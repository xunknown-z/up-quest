package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.repository.AlarmRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class GetAlarmsUseCaseTest {

    private lateinit var repository: AlarmRepository
    private lateinit var useCase: GetAlarmsUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetAlarmsUseCase(repository)
    }

    @Test
    fun `알람 목록을 Flow로 반환한다`() = runTest {
        // given
        val alarms = listOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatDays = emptySet(),
                label = "기상",
                isEnabled = true,
                dismissMode = DismissMode.Normal
            ),
            Alarm(
                id = 2L,
                hour = 8,
                minute = 30,
                repeatDays = emptySet(),
                label = "출근",
                isEnabled = false,
                dismissMode = DismissMode.Normal
            ),
        )
        every { repository.getAlarms() } returns flowOf(alarms)

        // when
        val result = useCase().toList()

        // then
        assertEquals(listOf(alarms), result)
    }

    @Test
    fun `알람이 없으면 빈 목록을 반환한다`() = runTest {
        // given
        every { repository.getAlarms() } returns flowOf(emptyList())

        // when
        val result = useCase().toList()

        // then
        assertEquals(listOf(emptyList<Alarm>()), result)
    }

    @Test
    fun `PhotoVerification 모드를 포함한 알람 목록을 반환한다`() = runTest {
        // given
        val alarms = listOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatDays = emptySet(),
                label = "기상",
                isEnabled = true,
                dismissMode = DismissMode.Normal
            ),
            Alarm(
                id = 2L,
                hour = 8,
                minute = 0,
                repeatDays = emptySet(),
                label = "출근",
                isEnabled = true,
                dismissMode = DismissMode.PhotoVerification("/storage/photo.jpg")
            ),
        )
        every { repository.getAlarms() } returns flowOf(alarms)

        // when
        val result = useCase().toList().first()

        // then
        assertEquals(2, result.size)
        assertTrue(result[1].dismissMode is DismissMode.PhotoVerification)
    }

    @Test
    fun `반복 요일이 설정된 알람 목록을 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            label = "평일 기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        )
        every { repository.getAlarms() } returns flowOf(listOf(alarm))

        // when
        val result = useCase().toList().first()

        // then
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            result.first().repeatDays
        )
    }

    @Test
    fun `Flow에서 에러 방출 시 예외가 전파된다`() = runTest {
        // given
        val exception = RuntimeException("DB 연결 오류")
        every { repository.getAlarms() } returns flow { throw exception }

        // when
        var caughtThrowable: Throwable? = null
        useCase().catch { caughtThrowable = it }.toList()

        // then
        assertTrue(caughtThrowable is RuntimeException)
        assertEquals("DB 연결 오류", caughtThrowable?.message)
    }

    @Test
    fun `Flow가 여러 번 방출될 때 모든 값을 수신한다`() = runTest {
        // given
        val firstEmission = listOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatDays = emptySet(),
                label = "기상",
                isEnabled = true,
                dismissMode = DismissMode.Normal
            ),
        )
        val secondEmission = listOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatDays = emptySet(),
                label = "기상",
                isEnabled = true,
                dismissMode = DismissMode.Normal
            ),
            Alarm(
                id = 2L,
                hour = 8,
                minute = 0,
                repeatDays = emptySet(),
                label = "출근",
                isEnabled = true,
                dismissMode = DismissMode.Normal
            ),
        )
        every { repository.getAlarms() } returns flow {
            emit(firstEmission)
            emit(secondEmission)
        }

        // when
        val result = useCase().toList()

        // then
        assertEquals(2, result.size)
        assertEquals(1, result[0].size)
        assertEquals(2, result[1].size)
    }
}
