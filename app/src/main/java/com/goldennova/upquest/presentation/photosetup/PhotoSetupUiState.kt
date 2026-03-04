package com.goldennova.upquest.presentation.photosetup

data class PhotoSetupUiState(
    val capturedImagePath: String? = null,
    val isCameraReady: Boolean = false,
    val isPhotoTaken: Boolean = false,
    val isCameraPermissionGranted: Boolean = false,
)
