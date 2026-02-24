package com.goldennova.upquest.data.datasource

import android.os.Build
import androidx.annotation.RequiresApi
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class FakeAlarmDataSource @Inject constructor() {

    private val _alarms = MutableStateFlow(initialAlarms())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    suspend fun getAlarmById(id: Long): Alarm? =
        _alarms.value.find { it.id == id }

    suspend fun insertAlarm(alarm: Alarm): Long {
        val newId = (_alarms.value.maxOfOrNull { it.id } ?: 0L) + 1L
        _alarms.value += alarm.copy(id = newId)
        return newId
    }

    suspend fun updateAlarm(alarm: Alarm) {
        _alarms.value = _alarms.value.map { if (it.id == alarm.id) alarm else it }
    }

    suspend fun deleteAlarm(id: Long) {
        _alarms.value = _alarms.value.filter { it.id != id }
    }

    suspend fun toggleAlarm(id: Long, isEnabled: Boolean) {
        _alarms.value =
            _alarms.value.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
    }

    private fun initialAlarms(): List<Alarm> = listOf(
        Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        ),
        Alarm(
            id = 2L,
            hour = 8,
            minute = 30,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            label = "출근",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/emulated/0/Pictures/ref_office.jpg"),
        ),
        Alarm(
            id = 3L,
            hour = 9,
            minute = 0,
            repeatDays = emptySet(),
            label = "주말 기상",
            isEnabled = false,
            dismissMode = DismissMode.Normal,
        ),
    )
}
