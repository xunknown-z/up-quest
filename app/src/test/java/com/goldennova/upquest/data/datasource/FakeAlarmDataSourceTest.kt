package com.goldennova.upquest.data.datasource

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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAlarmDataSourceTest {

    private lateinit var dataSource: FakeAlarmDataSource

    @BeforeEach
    fun setUp() {
        dataSource = FakeAlarmDataSource()
    }

    // region 초기 데이터

    @Test
    fun `초기 알람 3개가 로드된다`() = runTest {
        val alarms = dataSource.alarms.first()
        assertEquals(3, alarms.size)
    }

    @Test
    fun `초기 알람 ID는 1부터 시작한다`() = runTest {
        val alarms = dataSource.alarms.first()
        assertEquals(1L, alarms[0].id)
        assertEquals(2L, alarms[1].id)
        assertEquals(3L, alarms[2].id)
    }

    @Test
    fun `초기 알람 중 PhotoVerification 모드가 포함된다`() = runTest {
        val alarms = dataSource.alarms.first()
        assertTrue(alarms.any { it.dismissMode is DismissMode.PhotoVerification })
    }

    @Test
    fun `초기 알람 중 비활성화된 알람이 포함된다`() = runTest {
        val alarms = dataSource.alarms.first()
        assertTrue(alarms.any { !it.isEnabled })
    }

    // endregion

    // region getAlarmById

    @Test
    fun `존재하는 ID로 조회하면 알람을 반환한다`() = runTest {
        val alarm = dataSource.getAlarmById(1L)
        assertNotNull(alarm)
        assertEquals(1L, alarm?.id)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        val alarm = dataSource.getAlarmById(999L)
        assertNull(alarm)
    }

    // endregion

    // region insertAlarm

    @Test
    fun `알람 insert 시 새 ID가 발급되고 목록에 추가된다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            label = "새 알람",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )

        val newId = dataSource.insertAlarm(newAlarm)
        val alarms = dataSource.alarms.first()

        assertEquals(4L, newId)
        assertEquals(4, alarms.size)
        assertNotNull(alarms.find { it.id == newId })
    }

    @Test
    fun `알람 insert 시 발급된 ID로 저장된다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 10,
            minute = 30,
            repeatDays = emptySet(),
            label = "추가 알람",
            isEnabled = false,
            dismissMode = DismissMode.Normal
        )

        val newId = dataSource.insertAlarm(newAlarm)
        val inserted = dataSource.getAlarmById(newId)

        assertNotNull(inserted)
        assertEquals("추가 알람", inserted?.label)
        assertEquals(10, inserted?.hour)
        assertEquals(30, inserted?.minute)
    }

    @Test
    fun `알람을 여러 번 insert하면 ID가 순차적으로 증가한다`() = runTest {
        val alarm = Alarm(
            id = 0L,
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            label = "테스트",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )

        val id1 = dataSource.insertAlarm(alarm)
        val id2 = dataSource.insertAlarm(alarm)

        assertEquals(4L, id1)
        assertEquals(5L, id2)
    }

    @Test
    fun `insert 시 Flow가 갱신된다`() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 5,
            minute = 0,
            repeatDays = emptySet(),
            label = "새벽 알람",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )

        val emissions = mutableListOf<List<Alarm>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataSource.alarms.take(2).toList(emissions)
        }
        dataSource.insertAlarm(newAlarm)
        advanceUntilIdle()

        assertEquals(2, emissions.size)
        assertEquals(3, emissions[0].size)
        assertEquals(4, emissions[1].size)
    }

    // endregion

    // region updateAlarm

    @Test
    fun `알람 update 시 해당 알람이 변경된다`() = runTest {
        val updated = dataSource.getAlarmById(1L)!!.copy(label = "수정된 알람", hour = 9)

        dataSource.updateAlarm(updated)
        val alarm = dataSource.getAlarmById(1L)

        assertEquals("수정된 알람", alarm?.label)
        assertEquals(9, alarm?.hour)
    }

    @Test
    fun `알람 update 시 다른 알람은 변경되지 않는다`() = runTest {
        val before2 = dataSource.getAlarmById(2L)!!
        val updated = dataSource.getAlarmById(1L)!!.copy(label = "수정")

        dataSource.updateAlarm(updated)

        assertEquals(before2, dataSource.getAlarmById(2L))
    }

    @Test
    fun `존재하지 않는 ID로 update해도 목록 크기는 변하지 않는다`() = runTest {
        val nonExistent = Alarm(
            id = 999L,
            hour = 1,
            minute = 0,
            repeatDays = emptySet(),
            label = "유령",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )

        dataSource.updateAlarm(nonExistent)

        assertEquals(3, dataSource.alarms.first().size)
    }

    @Test
    fun `update 시 Flow가 갱신된다`() = runTest {
        val updated = dataSource.getAlarmById(1L)!!.copy(label = "수정된 알람")

        val emissions = mutableListOf<List<Alarm>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataSource.alarms.take(2).toList(emissions)
        }
        dataSource.updateAlarm(updated)
        advanceUntilIdle()

        assertEquals("수정된 알람", emissions[1].find { it.id == 1L }?.label)
    }

    // endregion

    // region deleteAlarm

    @Test
    fun `알람 삭제 시 목록에서 제거된다`() = runTest {
        dataSource.deleteAlarm(1L)
        val alarms = dataSource.alarms.first()

        assertEquals(2, alarms.size)
        assertNull(dataSource.getAlarmById(1L))
    }

    @Test
    fun `알람 삭제 시 다른 알람은 유지된다`() = runTest {
        dataSource.deleteAlarm(1L)
        val alarms = dataSource.alarms.first()

        assertTrue(alarms.any { it.id == 2L })
        assertTrue(alarms.any { it.id == 3L })
    }

    @Test
    fun `존재하지 않는 ID 삭제 시 목록이 변경되지 않는다`() = runTest {
        dataSource.deleteAlarm(999L)
        assertEquals(3, dataSource.alarms.first().size)
    }

    @Test
    fun `삭제 시 Flow가 갱신된다`() = runTest {
        val emissions = mutableListOf<List<Alarm>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataSource.alarms.take(2).toList(emissions)
        }
        dataSource.deleteAlarm(1L)
        advanceUntilIdle()

        assertEquals(3, emissions[0].size)
        assertEquals(2, emissions[1].size)
    }

    // endregion

    // region toggleAlarm

    @Test
    fun `활성화된 알람을 비활성화할 수 있다`() = runTest {
        dataSource.toggleAlarm(1L, false)
        assertFalse(dataSource.getAlarmById(1L)!!.isEnabled)
    }

    @Test
    fun `비활성화된 알람을 활성화할 수 있다`() = runTest {
        dataSource.toggleAlarm(3L, true)
        assertTrue(dataSource.getAlarmById(3L)!!.isEnabled)
    }

    @Test
    fun `이미 같은 상태로 toggle해도 다른 알람은 영향받지 않는다`() = runTest {
        val before2 = dataSource.getAlarmById(2L)!!

        dataSource.toggleAlarm(1L, true)

        assertEquals(before2, dataSource.getAlarmById(2L))
    }

    @Test
    fun `존재하지 않는 ID toggle 시 목록이 변경되지 않는다`() = runTest {
        val before = dataSource.alarms.first()

        dataSource.toggleAlarm(999L, true)

        assertEquals(before, dataSource.alarms.first())
    }

    @Test
    fun `toggle 시 Flow가 갱신된다`() = runTest {
        val emissions = mutableListOf<List<Alarm>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataSource.alarms.take(2).toList(emissions)
        }
        dataSource.toggleAlarm(1L, false)
        advanceUntilIdle()

        assertTrue(emissions[0].find { it.id == 1L }!!.isEnabled)
        assertFalse(emissions[1].find { it.id == 1L }!!.isEnabled)
    }

    // endregion

    // region DismissMode

    @Test
    fun `PhotoVerification 알람을 insert하면 dismissMode가 유지된다`() = runTest {
        val alarm = Alarm(
            id = 0L, hour = 7, minute = 0, repeatDays = emptySet(),
            label = "사진 알람", isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
        )

        val newId = dataSource.insertAlarm(alarm)
        val inserted = dataSource.getAlarmById(newId)

        assertInstanceOf(DismissMode.PhotoVerification::class.java, inserted?.dismissMode)
        assertEquals(
            "/storage/ref.jpg",
            (inserted?.dismissMode as DismissMode.PhotoVerification).referencePhotoPath
        )
    }

    @Test
    fun `Normal → PhotoVerification으로 update할 수 있다`() = runTest {
        val updated = dataSource.getAlarmById(1L)!!.copy(
            dismissMode = DismissMode.PhotoVerification("/storage/new_ref.jpg")
        )

        dataSource.updateAlarm(updated)
        val alarm = dataSource.getAlarmById(1L)

        assertInstanceOf(DismissMode.PhotoVerification::class.java, alarm?.dismissMode)
    }

    @Test
    fun `PhotoVerification → Normal로 update할 수 있다`() = runTest {
        val updated = dataSource.getAlarmById(2L)!!.copy(dismissMode = DismissMode.Normal)

        dataSource.updateAlarm(updated)
        val alarm = dataSource.getAlarmById(2L)

        assertInstanceOf(DismissMode.Normal::class.java, alarm?.dismissMode)
    }

    // endregion
}
