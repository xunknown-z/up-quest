package com.goldennova.upquest.presentation.alarmdetail

import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek

data class AlarmDetailUiState(
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val dismissMode: DismissMode = DismissMode.Normal,
    val isLoading: Boolean = false,
)
