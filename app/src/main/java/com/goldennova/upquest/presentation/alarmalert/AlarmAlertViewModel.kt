package com.goldennova.upquest.presentation.alarmalert

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.GetAlarmByIdUseCase
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import com.goldennova.upquest.domain.usecase.ToggleAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmAlertViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val photoVerificationUseCase: PhotoVerificationUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
) : ViewModel() {

    // AlarmAlertActivity가 Intent extra로 전달하는 alarmId를 SavedStateHandle에서 읽는다
    private val alarmId: Long = savedStateHandle[KEY_ALARM_ID] ?: -1L

    private val _uiState = MutableStateFlow(AlarmAlertUiState())
    val uiState: StateFlow<AlarmAlertUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AlarmAlertSideEffect>()
    val sideEffect: SharedFlow<AlarmAlertSideEffect> = _sideEffect.asSharedFlow()

    init {
        loadAlarm()
    }

    fun onEvent(event: AlarmAlertEvent) {
        when (event) {
            AlarmAlertEvent.DismissNormal -> dismissNormal()
            // 촬영 트리거는 UI(CameraPreview)가 직접 처리하므로 ViewModel은 관여하지 않는다
            AlarmAlertEvent.TakeVerificationPhoto -> Unit
            is AlarmAlertEvent.PhotoVerified -> onPhotoVerified(event.capturedImagePath)
        }
    }

    private fun loadAlarm() {
        viewModelScope.launch {
            getAlarmByIdUseCase(alarmId)
                .onSuccess { alarm -> _uiState.update { it.copy(alarm = alarm) } }
                .onFailure { _sideEffect.emit(AlarmAlertSideEffect.ShowError("알람 정보를 불러올 수 없습니다.")) }
        }
    }

    // Normal 모드: 바로 알람 해제
    private fun dismissNormal() {
        val alarm = _uiState.value.alarm ?: return
        if (alarm.dismissMode !is DismissMode.Normal) return
        viewModelScope.launch {
            handleAlarmDismiss(alarm)
            _uiState.update { it.copy(isDismissed = true) }
            _sideEffect.emit(AlarmAlertSideEffect.DismissAlarm)
        }
    }

    // PhotoVerification 모드: 사진 비교 후 성공 시 해제, 실패 시 오류 전달
    private fun onPhotoVerified(capturedImagePath: String) {
        val alarm = _uiState.value.alarm ?: return
        val mode = alarm.dismissMode as? DismissMode.PhotoVerification ?: return
        viewModelScope.launch {
            val verified =
                photoVerificationUseCase.verify(capturedImagePath, mode.referencePhotoPath)
            if (verified) {
                handleAlarmDismiss(alarm)
                _uiState.update { it.copy(isPhotoVerified = true, isDismissed = true) }
                _sideEffect.emit(AlarmAlertSideEffect.DismissAlarm)
            } else {
                _sideEffect.emit(AlarmAlertSideEffect.ShowError("사진 인증에 실패했습니다. 다시 시도해 주세요."))
            }
        }
    }

    /**
     * 알람 해제 시 반복 여부에 따라 다음 회차 처리를 분기한다.
     *
     * - 반복 알람(`repeatDays.isNotEmpty`): 다음 요일 회차를 재등록
     * - 비반복 알람(`repeatDays.isEmpty`): 알람을 비활성화하여 재울리지 않도록 처리
     */
    private suspend fun handleAlarmDismiss(alarm: Alarm) {
        if (alarm.repeatDays.isNotEmpty()) {
            alarmScheduler.schedule(alarm)
        } else {
            toggleAlarmUseCase(alarm.id, false)
        }
    }

    companion object {
        const val KEY_ALARM_ID = "alarmId"
    }
}
