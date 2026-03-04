package com.goldennova.upquest.presentation.photosetup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.GetAlarmByIdUseCase
import com.goldennova.upquest.domain.usecase.SaveAlarmUseCase
import com.goldennova.upquest.presentation.navigation.PhotoSetup
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
class PhotoSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val saveAlarmUseCase: SaveAlarmUseCase,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.toRoute<PhotoSetup>().alarmId

    private val _uiState = MutableStateFlow(PhotoSetupUiState())
    val uiState: StateFlow<PhotoSetupUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<PhotoSetupSideEffect>()
    val sideEffect: SharedFlow<PhotoSetupSideEffect> = _sideEffect.asSharedFlow()

    fun onEvent(event: PhotoSetupEvent) {
        when (event) {
            is PhotoSetupEvent.TakePhoto -> onPhotoTaken(event.imagePath)
            PhotoSetupEvent.RetakePhoto -> retake()
            PhotoSetupEvent.Confirm -> confirm()
        }
    }

    // CameraX 촬영 완료 후 경로를 받아 UiState에 반영
    private fun onPhotoTaken(imagePath: String) {
        _uiState.update { it.copy(capturedImagePath = imagePath, isPhotoTaken = true) }
    }

    // 재촬영: 캡처 상태 초기화
    private fun retake() {
        _uiState.update { it.copy(capturedImagePath = null, isPhotoTaken = false) }
    }

    // 확인: 알람의 referencePhotoPath를 업데이트하고 뒤로 이동
    private fun confirm() {
        val path = _uiState.value.capturedImagePath ?: return
        viewModelScope.launch {
            getAlarmByIdUseCase(alarmId)
                .onSuccess { alarm ->
                    if (alarm == null) return@onSuccess
                    saveAlarmUseCase(alarm.copy(dismissMode = DismissMode.PhotoVerification(path)))
                        .onSuccess { _sideEffect.emit(PhotoSetupSideEffect.NavigateBackWithPath(path)) }
                }
        }
    }
}
