package com.goldennova.upquest.data.repository

import com.goldennova.upquest.data.datasource.FakeAlarmDataSource
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAlarmRepositoryTest {

    private lateinit var dataSource: FakeAlarmDataSource
    private lateinit var repository: FakeAlarmRepository

    @BeforeEach
    fun setUp() {
        dataSource = FakeAlarmDataSource()
        repository = FakeAlarmRepository(dataSource)
    }

    // region getAlarms

    @Test
    fun `getAlarms는 DataSource의 알람 Flow를 반환한다`() = runTest {
        val alarms = repository.getAlarms().first()
        assertEquals(3, alarms.size)
    }

    @Test
    fun `DataSource가 변경되면 getAlarms Flow에 반영된다`() = runTest {
        val emissions = mutableListOf<List<Alarm>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getAlarms().take(2).toList(emissions)
        }

        val newAlarm = Alarm(
            id = 0L,
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            label = "새 알람",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        repository.insertAlarm(newAlarm)
        advanceUntilIdle()

        assertEquals(2, emissions.size)
        assertEquals(3, emissions[0].size)
        assertEquals(4, emissions[1].size)
    }

    // endregion

    // region getAlarmById

    @Test
    fun `getAlarmById는 존재하는 알람을 반환한다`() = runTest {
        val alarm = repository.getAlarmById(1L)
        assertNotNull(alarm)
        assertEquals(1L, alarm?.id)
    }

    @Test
    fun `getAlarmById는 존재하지 않는 ID에 대해 null을 반환한다`() = runTest {
        val alarm = repository.getAlarmById(999L)
        assertNull(alarm)
    }

    // endregion

    // region insertAlarm

    @Test
    fun `insertAlarm은 새 알람을 추가하고 ID를 반환한다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 5,
            minute = 30,
            repeatDays = emptySet(),
            label = "새벽 알람",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )

        val newId = repository.insertAlarm(newAlarm)
        val alarms = repository.getAlarms().first()

        assertEquals(4L, newId)
        assertEquals(4, alarms.size)
        assertNotNull(alarms.find { it.id == newId })
    }

    @Test
    fun `insertAlarm은 PhotoVerification 알람을 정확히 저장한다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "사진 알람",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
        )

        val newId = repository.insertAlarm(newAlarm)
        val inserted = repository.getAlarmById(newId)

        assertEquals(
            DismissMode.PhotoVerification("/storage/ref.jpg"),
            inserted?.dismissMode
        )
    }

    @Test
    fun `insertAlarm은 반복 요일이 설정된 알람을 저장한다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            label = "반복 알람",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        )

        val newId = repository.insertAlarm(newAlarm)
        val inserted = repository.getAlarmById(newId)

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), inserted?.repeatDays)
    }

    // endregion

    // region updateAlarm

    @Test
    fun `updateAlarm은 기존 알람을 수정한다`() = runTest {
        val original = repository.getAlarmById(1L)!!
        val updated = original.copy(label = "수정된 알람", hour = 9)

        repository.updateAlarm(updated)
        val result = repository.getAlarmById(1L)

        assertEquals("수정된 알람", result?.label)
        assertEquals(9, result?.hour)
    }

    @Test
    fun `updateAlarm은 다른 알람에 영향을 주지 않는다`() = runTest {
        val before2 = repository.getAlarmById(2L)!!
        val updated = repository.getAlarmById(1L)!!.copy(label = "수정")

        repository.updateAlarm(updated)

        assertEquals(before2, repository.getAlarmById(2L))
    }

    @Test
    fun `updateAlarm으로 dismissMode를 변경할 수 있다`() = runTest {
        val updated = repository.getAlarmById(1L)!!.copy(
            dismissMode = DismissMode.PhotoVerification("/storage/new_ref.jpg")
        )

        repository.updateAlarm(updated)
        val result = repository.getAlarmById(1L)

        assertEquals(
            DismissMode.PhotoVerification("/storage/new_ref.jpg"),
            result?.dismissMode
        )
    }

    // endregion

    // region deleteAlarm

    @Test
    fun `deleteAlarm은 알람을 목록에서 제거한다`() = runTest {
        repository.deleteAlarm(1L)

        val alarms = repository.getAlarms().first()
        assertEquals(2, alarms.size)
        assertNull(repository.getAlarmById(1L))
    }

    @Test
    fun `deleteAlarm은 다른 알람을 유지한다`() = runTest {
        repository.deleteAlarm(1L)

        assertNotNull(repository.getAlarmById(2L))
        assertNotNull(repository.getAlarmById(3L))
    }

    @Test
    fun `존재하지 않는 ID 삭제 시 목록이 변경되지 않는다`() = runTest {
        repository.deleteAlarm(999L)
        assertEquals(3, repository.getAlarms().first().size)
    }

    // endregion

    // region toggleAlarm

    @Test
    fun `toggleAlarm으로 알람을 비활성화할 수 있다`() = runTest {
        repository.toggleAlarm(1L, false)
        assertFalse(repository.getAlarmById(1L)!!.isEnabled)
    }

    @Test
    fun `toggleAlarm으로 알람을 활성화할 수 있다`() = runTest {
        repository.toggleAlarm(3L, true)
        assertTrue(repository.getAlarmById(3L)!!.isEnabled)
    }

    @Test
    fun `toggleAlarm은 다른 알람에 영향을 주지 않는다`() = runTest {
        val before2 = repository.getAlarmById(2L)!!

        repository.toggleAlarm(1L, false)

        assertEquals(before2, repository.getAlarmById(2L))
    }

    @Test
    fun `존재하지 않는 ID toggle 시 목록이 변경되지 않는다`() = runTest {
        val before = repository.getAlarms().first()

        repository.toggleAlarm(999L, true)

        assertEquals(before, repository.getAlarms().first())
    }

    // endregion
}
