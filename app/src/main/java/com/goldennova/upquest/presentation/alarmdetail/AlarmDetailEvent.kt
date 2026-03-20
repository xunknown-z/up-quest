package com.goldennova.upquest.presentation.alarmdetail

import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek

sealed interface AlarmDetailEvent {
    data class ChangeHour(val hour: Int) : AlarmDetailEvent
    data class ChangeMinute(val minute: Int) : AlarmDetailEvent
    data class ChangeLabel(val label: String) : AlarmDetailEvent
    data class ToggleDay(val day: DayOfWeek) : AlarmDetailEvent
    data class ChangeDismissMode(val mode: DismissMode) : AlarmDetailEvent
    data class ChangeRingtone(val uri: String?) : AlarmDetailEvent
    data object Save : AlarmDetailEvent
    data object Delete : AlarmDetailEvent
}
