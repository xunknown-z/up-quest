package com.goldennova.upquest.domain.model

import java.time.DayOfWeek

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,
    val label: String,
    val isEnabled: Boolean,
    val dismissMode: DismissMode,
)
