package com.goldennova.upquest.domain.model

sealed class DismissMode {
    data object Normal : DismissMode()
    data class PhotoVerification(val referencePhotoPath: String?) : DismissMode()
}
