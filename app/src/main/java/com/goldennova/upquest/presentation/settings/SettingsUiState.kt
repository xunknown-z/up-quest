package com.goldennova.upquest.presentation.settings

import com.goldennova.upquest.presentation.theme.ThemeMode

data class SettingsUiState(
    val currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
)
