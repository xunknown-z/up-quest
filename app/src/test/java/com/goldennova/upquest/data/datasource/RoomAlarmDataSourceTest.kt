package com.goldennova.upquest.data.datasource

import com.goldennova.upquest.data.local.dao.AlarmDao
import com.goldennova.upquest.data.local.entity.AlarmEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [RoomAlarmDataSource] 단위 테스트.
 * [AlarmDao]를 MockK로 mock 처리하여 각 메서드가 DAO에 올바르게 위임되는지 검증한다.
 */
class RoomAlarmDataSourceTest {

    private lateinit var dao: AlarmDao
    private lateinit var dataSource: RoomAlarmDataSource

    private val sampleEntity = AlarmEntity(
        id = 1L,
        hour = 7,
        minute = 30,
        repeatDays = "MONDAY",
        label = "기상",
        isEnabled = true,
        dismissMode = "NORMAL",
        referencePhotoPath = null,
    )

    @BeforeEach
    fun setUp() {
        dao = mockk()
        dataSource = RoomAlarmDataSource(dao)
    }

    /** getAll() 호출 시 dao.getAll()에 위임하고 동일한 Flow를 반환하는지 검증한다. */
    @Test
    fun `getAll - dao getAll에 위임한다`() = runTest {
        val expected = listOf(sampleEntity)
        every { dao.getAll() } returns flowOf(expected)

        val result = dataSource.getAll().first()

        assertEquals(expected, result)
        verify(exactly = 1) { dao.getAll() }
    }

    /** getById() 호출 시 dao.getById()에 위임하고 동일한 결과를 반환하는지 검증한다. */
    @Test
    fun `getById - dao getById에 위임한다`() = runTest {
        coEvery { dao.getById(1L) } returns sampleEntity

        val result = dataSource.getById(1L)

        assertEquals(sampleEntity, result)
        coVerify(exactly = 1) { dao.getById(1L) }
    }

    /** 존재하지 않는 ID로 getById() 호출 시 null을 반환하는지 검증한다. */
    @Test
    fun `getById - 존재하지 않는 ID이면 null을 반환한다`() = runTest {
        coEvery { dao.getById(999L) } returns null

        val result = dataSource.getById(999L)

        assertNull(result)
    }

    /** insert() 호출 시 dao.insert()에 위임하고 생성된 ID를 반환하는지 검증한다. */
    @Test
    fun `insert - dao insert에 위임하고 생성된 ID를 반환한다`() = runTest {
        coEvery { dao.insert(sampleEntity) } returns 1L

        val result = dataSource.insert(sampleEntity)

        assertEquals(1L, result)
        coVerify(exactly = 1) { dao.insert(sampleEntity) }
    }

    /** update() 호출 시 dao.update()에 위임하는지 검증한다. */
    @Test
    fun `update - dao update에 위임한다`() = runTest {
        coEvery { dao.update(sampleEntity) } returns Unit

        dataSource.update(sampleEntity)

        coVerify(exactly = 1) { dao.update(sampleEntity) }
    }

    /** deleteById() 호출 시 dao.deleteById()에 위임하는지 검증한다. */
    @Test
    fun `deleteById - dao deleteById에 위임한다`() = runTest {
        coEvery { dao.deleteById(1L) } returns Unit

        dataSource.deleteById(1L)

        coVerify(exactly = 1) { dao.deleteById(1L) }
    }
}
