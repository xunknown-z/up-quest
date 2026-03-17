package com.goldennova.upquest.presentation.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldennova.upquest.domain.usecase.DeleteAlarmUseCase
import com.goldennova.upquest.domain.usecase.GetAlarmsUseCase
import com.goldennova.upquest.domain.usecase.ToggleAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val getAlarmsUseCase: GetAlarmsUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmListUiState())
    val uiState: StateFlow<AlarmListUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AlarmListSideEffect>()
    val sideEffect: SharedFlow<AlarmListSideEffect> = _sideEffect.asSharedFlow()

    init {
        loadAlarms()
    }

    fun onEvent(event: AlarmListEvent) {
        when (event) {
            is AlarmListEvent.ToggleAlarm -> toggleAlarm(event.id, event.enabled)
            is AlarmListEvent.DeleteAlarm -> deleteAlarm(event.id)
            is AlarmListEvent.AddAlarm -> onAddAlarm()
            is AlarmListEvent.EditAlarm -> onEditAlarm(event.id)
        }
    }

    // 알람 목록을 Flow로 수집 — 로드 실패 시 errorMessage 업데이트
    private fun loadAlarms() {
        viewModelScope.launch {
            getAlarmsUseCase()
                .onStart {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message)
                    }
                }
                .collect { alarms ->
                    _uiState.update {
                        it.copy(isLoading = false, alarms = alarms, errorMessage = null)
                    }
                }
        }
    }

    // 알람 활성화/비활성화 — 실패 시 ShowError SideEffect 방출
    private fun toggleAlarm(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(id, enabled)
                .onFailure { throwable ->
                    _sideEffect.emit(AlarmListSideEffect.ShowError(throwable.message ?: ""))
                }
        }
    }

    // 알람 삭제 — 실패 시 ShowError SideEffect 방출
    private fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            deleteAlarmUseCase(id)
                .onFailure { throwable ->
                    _sideEffect.emit(AlarmListSideEffect.ShowError(throwable.message ?: ""))
                }
        }
    }

    // 신규 알람 생성 화면으로 이동
    private fun onAddAlarm() {
        viewModelScope.launch {
            _sideEffect.emit(AlarmListSideEffect.NavigateToNewAlarm)
        }
    }

    // 기존 알람 수정 화면으로 이동
    private fun onEditAlarm(id: Long) {
        viewModelScope.launch {
            _sideEffect.emit(AlarmListSideEffect.NavigateToDetail(id))
        }
    }
}
