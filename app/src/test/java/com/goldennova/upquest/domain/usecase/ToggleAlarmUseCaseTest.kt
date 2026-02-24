package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.repository.AlarmRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class ToggleAlarmUseCaseTest {

    private lateinit var repository: AlarmRepository
    private lateinit var useCase: ToggleAlarmUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = ToggleAlarmUseCase(repository)
    }

    @Test
    fun `알람 활성화 시 repository toggleAlarm을 호출한다`() = runTest {
        // given
        coJustRun { repository.toggleAlarm(1L, true) }

        // when
        val result = useCase(1L, true)

        // then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.toggleAlarm(1L, true) }
    }

    @Test
    fun `알람 비활성화 시 repository toggleAlarm을 호출한다`() = runTest {
        // given
        coJustRun { repository.toggleAlarm(1L, false) }

        // when
        val result = useCase(1L, false)

        // then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.toggleAlarm(1L, false) }
    }

    @Test
    fun `이미 활성화된 알람을 다시 활성화해도 repository toggleAlarm을 호출한다`() = runTest {
        // given
        coJustRun { repository.toggleAlarm(1L, true) }

        // when
        val result = useCase(1L, true)

        // then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.toggleAlarm(1L, true) }
    }

    @Test
    fun `존재하지 않는 ID 토글 시 Repository 예외를 Result failure로 반환한다`() = runTest {
        // given
        coEvery { repository.toggleAlarm(999L, any()) } throws NoSuchElementException("알람을 찾을 수 없음")

        // when
        val result = useCase(999L, true)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(NoSuchElementException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `DB 오류 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.toggleAlarm(any(), any()) } throws RuntimeException("DB 오류")

        // when
        val result = useCase(1L, true)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `IO 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.toggleAlarm(any(), any()) } throws IOException("디스크 오류")

        // when
        val result = useCase(1L, false)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }
}
