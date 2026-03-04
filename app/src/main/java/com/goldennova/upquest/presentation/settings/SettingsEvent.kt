package com.goldennova.upquest.presentation.settings

import com.goldennova.upquest.presentation.theme.ThemeMode

sealed interface SettingsEvent {
    data class ChangeThemeMode(val mode: ThemeMode) : SettingsEvent
}
