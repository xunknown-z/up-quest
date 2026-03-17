package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
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

class DeleteAlarmUseCaseTest {

    private lateinit var repository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: DeleteAlarmUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        alarmScheduler = mockk(relaxed = true)
        useCase = DeleteAlarmUseCase(repository, alarmScheduler)
        // 기본적으로 알람을 찾지 못하는 것으로 설정 (각 테스트에서 필요 시 재정의)
        coEvery { repository.getAlarmById(any()) } returns null
    }

    @Test
    fun `알람 삭제 시 repository deleteAlarm을 호출한다`() = runTest {
        // given
        coJustRun { repository.deleteAlarm(1L) }

        // when
        val result = useCase(1L)

        // then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteAlarm(1L) }
    }

    @Test
    fun `존재하지 않는 ID 삭제 시도 시 Repository 예외를 Result failure로 반환한다`() = runTest {
        // given
        coEvery { repository.deleteAlarm(999L) } throws NoSuchElementException("알람을 찾을 수 없음")

        // when
        val result = useCase(999L)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(NoSuchElementException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `DB 오류 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.deleteAlarm(any()) } throws RuntimeException("DB 오류")

        // when
        val result = useCase(1L)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `IO 예외 발생 시 Result failure를 반환한다`() = runTest {
        // given
        coEvery { repository.deleteAlarm(any()) } throws IOException("디스크 오류")

        // when
        val result = useCase(1L)

        // then
        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }
}
