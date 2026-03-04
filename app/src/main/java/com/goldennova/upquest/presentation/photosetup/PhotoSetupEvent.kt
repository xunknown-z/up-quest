package com.goldennova.upquest.presentation.photosetup

sealed interface PhotoSetupEvent {
    // UI(CameraX)가 촬영을 완료한 뒤 저장된 이미지 경로를 전달한다
    data class TakePhoto(val imagePath: String) : PhotoSetupEvent
    data object RetakePhoto : PhotoSetupEvent
    data object Confirm : PhotoSetupEvent
}
