package com.goldennova.upquest.presentation.photosetup

sealed interface PhotoSetupEvent {
    // UI(CameraX)가 촬영을 완료한 뒤 저장된 이미지 경로를 전달한다
    data class TakePhoto(val imagePath: String) : PhotoSetupEvent
    data object RetakePhoto : PhotoSetupEvent
    data object Confirm : PhotoSetupEvent
    // Root(Accompanist)가 권한 상태 변화를 ViewModel에 동기화
    data class UpdateCameraPermission(val granted: Boolean) : PhotoSetupEvent
}
