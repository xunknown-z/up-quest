package com.goldennova.upquest.presentation.photosetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class PhotoSetupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoSetupUiState())
    val uiState: StateFlow<PhotoSetupUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<PhotoSetupSideEffect>()
    val sideEffect: SharedFlow<PhotoSetupSideEffect> = _sideEffect.asSharedFlow()

    fun onEvent(event: PhotoSetupEvent) {
        when (event) {
            is PhotoSetupEvent.TakePhoto -> onPhotoTaken(event.imagePath)
            PhotoSetupEvent.RetakePhoto -> retake()
            PhotoSetupEvent.Confirm -> confirm()
            is PhotoSetupEvent.UpdateCameraPermission ->
                _uiState.update { it.copy(isCameraPermissionGranted = event.granted) }
        }
    }

    // 권한 미허용 상태에서는 촬영 이벤트를 무시한다
    private fun onPhotoTaken(imagePath: String) {
        if (!_uiState.value.isCameraPermissionGranted) return
        _uiState.update { it.copy(capturedImagePath = imagePath, isPhotoTaken = true) }
    }

    // 재촬영: 캡처 상태 초기화
    private fun retake() {
        _uiState.update { it.copy(capturedImagePath = null, isPhotoTaken = false) }
    }

    // 확인: 촬영된 사진 경로를 AlarmDetail로 전달하고 뒤로 이동
    // 알람 저장은 AlarmDetail의 저장 버튼에서 일괄 처리한다
    private fun confirm() {
        val path = _uiState.value.capturedImagePath ?: return
        viewModelScope.launch {
            _sideEffect.emit(PhotoSetupSideEffect.NavigateBackWithPath(path))
        }
    }
}
