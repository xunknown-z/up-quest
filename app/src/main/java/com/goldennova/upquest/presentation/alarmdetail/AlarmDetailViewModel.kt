package com.goldennova.upquest.presentation.alarmdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.usecase.DeleteAlarmUseCase
import com.goldennova.upquest.domain.usecase.GetAlarmByIdUseCase
import com.goldennova.upquest.domain.usecase.SaveAlarmUseCase
import com.goldennova.upquest.presentation.navigation.AlarmDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
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
class AlarmDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val saveAlarmUseCase: SaveAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.toRoute<AlarmDetail>().alarmId

    // alarmId == -1L이면 신규 생성 모드
    private val isNewAlarm: Boolean = alarmId == -1L

    private val _uiState = MutableStateFlow(AlarmDetailUiState())
    val uiState: StateFlow<AlarmDetailUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AlarmDetailSideEffect>()
    val sideEffect: SharedFlow<AlarmDetailSideEffect> = _sideEffect.asSharedFlow()

    init {
        if (!isNewAlarm) loadAlarm()
    }

    fun onEvent(event: AlarmDetailEvent) {
        when (event) {
            is AlarmDetailEvent.ChangeHour -> _uiState.update { it.copy(hour = event.hour) }
            is AlarmDetailEvent.ChangeMinute -> _uiState.update { it.copy(minute = event.minute) }
            is AlarmDetailEvent.ChangeLabel -> _uiState.update { it.copy(label = event.label) }
            is AlarmDetailEvent.ToggleDay -> toggleDay(event.day)
            is AlarmDetailEvent.ChangeDismissMode -> _uiState.update { it.copy(dismissMode = event.mode) }
            is AlarmDetailEvent.ChangeRingtone -> _uiState.update { it.copy(ringtoneUri = event.uri) }
            is AlarmDetailEvent.ChangeSoundMode -> _uiState.update { it.copy(soundMode = event.mode) }
            is AlarmDetailEvent.Save -> save(event.defaultLabel)
            is AlarmDetailEvent.Delete -> delete()
        }
    }

    // 기존 알람 데이터를 로드하여 UiState에 반영
    private fun loadAlarm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAlarmByIdUseCase(alarmId)
                .onSuccess { alarm ->
                    if (alarm != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hour = alarm.hour,
                                minute = alarm.minute,
                                repeatDays = alarm.repeatDays,
                                label = alarm.label,
                                dismissMode = alarm.dismissMode,
                                ringtoneUri = alarm.ringtoneUri,
                                soundMode = alarm.soundMode,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        _sideEffect.emit(AlarmDetailSideEffect.ShowError("알람을 찾을 수 없습니다."))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(AlarmDetailSideEffect.ShowError(throwable.message ?: ""))
                }
        }
    }

    // 선택된 요일 토글 — 이미 있으면 제거, 없으면 추가
    private fun toggleDay(day: DayOfWeek) {
        _uiState.update { state ->
            val updated = if (day in state.repeatDays) state.repeatDays - day
                          else state.repeatDays + day
            state.copy(repeatDays = updated)
        }
    }

    // 알람 저장 — 신규면 insert, 수정이면 update
    private fun save(defaultLabel: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val alarm = Alarm(
                id = if (isNewAlarm) 0L else alarmId,
                hour = state.hour,
                minute = state.minute,
                repeatDays = state.repeatDays,
                label = state.label.ifBlank { defaultLabel }, // 빈 라벨이면 기본값 사용
                isEnabled = true,
                dismissMode = state.dismissMode,
                ringtoneUri = state.ringtoneUri,
                soundMode = state.soundMode,
            )
            _uiState.update { it.copy(isLoading = true) }
            saveAlarmUseCase(alarm)
                .onSuccess { _sideEffect.emit(AlarmDetailSideEffect.NavigateBack) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(AlarmDetailSideEffect.ShowError(throwable.message ?: ""))
                }
        }
    }

    // 알람 삭제 — 성공 시 뒤로 이동
    private fun delete() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteAlarmUseCase(alarmId)
                .onSuccess { _sideEffect.emit(AlarmDetailSideEffect.NavigateBack) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(AlarmDetailSideEffect.ShowError(throwable.message ?: ""))
                }
        }
    }
}
