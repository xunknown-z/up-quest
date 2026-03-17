package com.goldennova.upquest.presentation.photosetup

sealed interface PhotoSetupSideEffect {
    data class NavigateBackWithPath(val path: String) : PhotoSetupSideEffect
}
