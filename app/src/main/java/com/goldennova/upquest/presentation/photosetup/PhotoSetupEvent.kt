package com.goldennova.upquest.presentation.photosetup

sealed interface PhotoSetupEvent {
    data object TakePhoto : PhotoSetupEvent
    data object RetakePhoto : PhotoSetupEvent
    data object Confirm : PhotoSetupEvent
}
