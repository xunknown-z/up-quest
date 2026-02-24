package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.repository.AlarmRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.DayOfWeek

class SaveAlarmUseCaseTest {

    private lateinit var repository: AlarmRepository
    private lateinit var useCase: SaveAlarmUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SaveAlarmUseCase(repository)
    }

    @Test
    fun `id가 0이면 insertAlarm을 호출하고 새 ID를 반환한다`() = runTest {
        // given
        val newAlarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.insertAlarm(newAlarm) } returns 1L

        // when
        val result = useCase(newAlarm)

        // then
        assertEquals(1L, result.getOrNull())
        coVerify(exactly = 1) { repository.insertAlarm(newAlarm) }
        coVerify(exactly = 0) { repository.updateAlarm(any()) }
    }

    @Test
    fun `id가 0이 아니면 updateAlarm을 호출하고 기존 ID를 반환한다`() = runTest {
        // given
        val existingAlarm = Alarm(
            id = 5L,
            hour = 8,
            minute = 30,
            repeatDays = emptySet(),
            label = "출근",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coJustRun { repository.updateAlarm(existingAlarm) }

        // when
        val result = useCase(existingAlarm)

        // then
        assertEquals(5L, result.getOrNull())
        coVerify(exactly = 1) { repository.updateAlarm(existingAlarm) }
        coVerify(exactly = 0) { repository.insertAlarm(any()) }
    }

    @Test
    fun `PhotoVerification 모드 알람을 신규 저장한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "사진 알람",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
        )
        coEvery { repository.insertAlarm(alarm) } returns 3L

        // when
        val result = useCase(alarm)

        // then
        assertEquals(3L, result.getOrNull())
        coVerify(exactly = 1) { repository.insertAlarm(alarm) }
    }

    @Test
    fun `반복 요일이 설정된 알람을 저장한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 0L,
            hour = 6,
            minute = 30,
            repeatDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            label = "평일 기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        )
        coEvery { repository.insertAlarm(alarm) } returns 5L

        // when
        val result = useCase(alarm)

        // then
        assertEquals(5L, result.getOrNull())
    }

    @Test
    fun `비활성화 상태 알람을 저장한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "비활성",
            isEnabled = false,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.insertAlarm(alarm) } returns 6L

        // when
        val result = useCase(alarm)

        // then
        assertEquals(6L, result.getOrNull())
    }

    @Test
    fun `insert 중 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.insertAlarm(any()) } throws RuntimeException("DB 오류")

        // when
        val result = useCase(alarm)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `update 중 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 5L,
            hour = 8,
            minute = 0,
            repeatDays = emptySet(),
            label = "출근",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.updateAlarm(any()) } throws RuntimeException("DB 오류")

        // when
        val result = useCase(alarm)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `IO 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 0L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.insertAlarm(any()) } throws IOException("디스크 쓰기 오류")

        // when
        val result = useCase(alarm)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }
}
