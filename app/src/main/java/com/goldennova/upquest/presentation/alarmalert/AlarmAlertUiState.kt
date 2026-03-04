package com.goldennova.upquest.presentation.alarmalert

import com.goldennova.upquest.domain.model.Alarm

data class AlarmAlertUiState(
    val alarm: Alarm? = null,
    val isDismissed: Boolean = false,
    val isPhotoVerified: Boolean = false,
)
