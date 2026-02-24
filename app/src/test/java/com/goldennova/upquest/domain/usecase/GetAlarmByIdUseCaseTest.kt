package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.repository.AlarmRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.DayOfWeek

class GetAlarmByIdUseCaseTest {

    private lateinit var repository: AlarmRepository
    private lateinit var useCase: GetAlarmByIdUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetAlarmByIdUseCase(repository)
    }

    @Test
    fun `존재하는 ID로 조회하면 알람을 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal
        )
        coEvery { repository.getAlarmById(1L) } returns alarm

        // when
        val result = useCase(1L)

        // then
        assertEquals(alarm, result.getOrNull())
    }

    @Test
    fun `PhotoVerification 모드 알람을 정확히 반환한다`() = runTest {
        // given
        val alarm = Alarm(
            id = 2L,
            hour = 8,
            minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            label = "사진 알람",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
        )
        coEvery { repository.getAlarmById(2L) } returns alarm

        // when
        val result = useCase(2L)

        // then
        val fetchedAlarm = result.getOrNull()
        assertTrue(fetchedAlarm?.dismissMode is DismissMode.PhotoVerification)
        assertEquals(
            "/storage/ref.jpg",
            (fetchedAlarm?.dismissMode as DismissMode.PhotoVerification).referencePhotoPath
        )
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        // given
        coEvery { repository.getAlarmById(999L) } returns null

        // when
        val result = useCase(999L)

        // then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `Repository에서 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.getAlarmById(any()) } throws RuntimeException("DB 오류")

        // when
        val result = useCase(1L)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
        assertEquals("DB 오류", result.exceptionOrNull()?.message)
    }

    @Test
    fun `IO 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.getAlarmById(any()) } throws IOException("디스크 읽기 오류")

        // when
        val result = useCase(1L)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }
}
