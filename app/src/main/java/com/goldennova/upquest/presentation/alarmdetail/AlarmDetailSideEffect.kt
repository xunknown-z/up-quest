package com.goldennova.upquest.presentation.alarmdetail

sealed interface AlarmDetailSideEffect {
    data object NavigateBack : AlarmDetailSideEffect
    data class ShowError(val message: String) : AlarmDetailSideEffect
}
