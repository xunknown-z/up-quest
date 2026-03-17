package com.goldennova.upquest.data.repository

import com.goldennova.upquest.data.datasource.RoomAlarmDataSource
import com.goldennova.upquest.data.local.entity.AlarmEntity
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [AlarmRepositoryImpl] 단위 테스트.
 * [RoomAlarmDataSource]를 MockK로 mock 처리하여 Flow 변환 및 CRUD 위임 동작을 검증한다.
 * 매퍼(AlarmEntityMapper)는 실제 구현을 사용한다.
 */
class AlarmRepositoryImplTest {

    private lateinit var dataSource: RoomAlarmDataSource
    private lateinit var repository: AlarmRepositoryImpl

    private val sampleEntity = AlarmEntity(
        id = 1L,
        hour = 7,
        minute = 30,
        repeatDays = "MONDAY,WEDNESDAY",
        label = "기상",
        isEnabled = true,
        dismissMode = "NORMAL",
        referencePhotoPath = null,
    )

    private val sampleAlarm = Alarm(
        id = 1L,
        hour = 7,
        minute = 30,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        label = "기상",
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    @BeforeEach
    fun setUp() {
        dataSource = mockk()
        repository = AlarmRepositoryImpl(dataSource)
    }

    // region getAlarms

    /** getAlarms()가 DataSource Flow를 도메인 모델 리스트로 변환하여 방출하는지 검증한다. */
    @Test
    fun `getAlarms - Entity Flow를 Alarm Flow로 변환하여 반환한다`() = runTest {
        every { dataSource.getAll() } returns flowOf(listOf(sampleEntity))

        val result = repository.getAlarms().first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("기상", result[0].label)
        assertTrue(result[0].dismissMode is DismissMode.Normal)
    }

    /** getAlarms()가 빈 리스트 Flow를 그대로 반환하는지 검증한다. */
    @Test
    fun `getAlarms - 빈 목록 Flow를 그대로 반환한다`() = runTest {
        every { dataSource.getAll() } returns flowOf(emptyList())

        val result = repository.getAlarms().first()

        assertTrue(result.isEmpty())
    }

    // endregion

    // region getAlarmById

    /** getAlarmById()가 Entity를 도메인 모델로 변환하여 반환하는지 검증한다. */
    @Test
    fun `getAlarmById - Entity를 Alarm으로 변환하여 반환한다`() = runTest {
        coEvery { dataSource.getById(1L) } returns sampleEntity

        val result = repository.getAlarmById(1L)

        assertEquals(1L, result?.id)
        assertEquals("기상", result?.label)
    }

    /** getAlarmById()가 존재하지 않는 ID에 대해 null을 반환하는지 검증한다. */
    @Test
    fun `getAlarmById - 존재하지 않는 ID이면 null을 반환한다`() = runTest {
        coEvery { dataSource.getById(999L) } returns null

        val result = repository.getAlarmById(999L)

        assertNull(result)
    }

    // endregion

    // region insertAlarm

    /** insertAlarm()이 Alarm을 Entity로 변환하여 DataSource에 위임하는지 검증한다. */
    @Test
    fun `insertAlarm - Alarm을 Entity로 변환하여 insert를 위임한다`() = runTest {
        val entitySlot = slot<AlarmEntity>()
        coEvery { dataSource.insert(capture(entitySlot)) } returns 1L

        val result = repository.insertAlarm(sampleAlarm)

        assertEquals(1L, result)
        assertEquals("기상", entitySlot.captured.label)
        assertEquals("NORMAL", entitySlot.captured.dismissMode)
    }

    // endregion

    // region updateAlarm

    /** updateAlarm()이 Alarm을 Entity로 변환하여 DataSource에 위임하는지 검증한다. */
    @Test
    fun `updateAlarm - Alarm을 Entity로 변환하여 update를 위임한다`() = runTest {
        val entitySlot = slot<AlarmEntity>()
        coEvery { dataSource.update(capture(entitySlot)) } returns Unit

        repository.updateAlarm(sampleAlarm)

        assertEquals(1L, entitySlot.captured.id)
        assertEquals("기상", entitySlot.captured.label)
        coVerify(exactly = 1) { dataSource.update(any()) }
    }

    // endregion

    // region deleteAlarm

    /** deleteAlarm()이 DataSource의 deleteById()에 위임하는지 검증한다. */
    @Test
    fun `deleteAlarm - dataSource deleteById에 위임한다`() = runTest {
        coEvery { dataSource.deleteById(1L) } returns Unit

        repository.deleteAlarm(1L)

        coVerify(exactly = 1) { dataSource.deleteById(1L) }
    }

    // endregion

    // region toggleAlarm

    /** toggleAlarm()이 기존 Entity의 isEnabled만 변경하여 update하는지 검증한다. */
    @Test
    fun `toggleAlarm - isEnabled를 변경하여 update를 위임한다`() = runTest {
        val entitySlot = slot<AlarmEntity>()
        coEvery { dataSource.getById(1L) } returns sampleEntity
        coEvery { dataSource.update(capture(entitySlot)) } returns Unit

        repository.toggleAlarm(1L, isEnabled = false)

        assertEquals(false, entitySlot.captured.isEnabled)
        assertEquals(1L, entitySlot.captured.id)
    }

    /** toggleAlarm() 호출 시 존재하지 않는 ID이면 update를 호출하지 않는지 검증한다. */
    @Test
    fun `toggleAlarm - 존재하지 않는 ID이면 update를 호출하지 않는다`() = runTest {
        coEvery { dataSource.getById(999L) } returns null

        repository.toggleAlarm(999L, isEnabled = false)

        coVerify(exactly = 0) { dataSource.update(any()) }
    }

    // endregion
}
