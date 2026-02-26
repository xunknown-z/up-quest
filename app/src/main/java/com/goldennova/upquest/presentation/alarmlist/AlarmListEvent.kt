package com.goldennova.upquest.presentation.alarmlist

sealed interface AlarmListEvent {
    data class ToggleAlarm(val id: Long, val enabled: Boolean) : AlarmListEvent
    data class DeleteAlarm(val id: Long) : AlarmListEvent
    data object AddAlarm : AlarmListEvent
    data class EditAlarm(val id: Long) : AlarmListEvent
}
