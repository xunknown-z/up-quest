package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.repository.AlarmRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.DayOfWeek

class DeleteAlarmUseCaseTest {

    @TempDir
    lateinit var tempDir: File

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

    @AfterEach
    fun tearDown() {
        unmockkAll()
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

    // region 참조 사진 파일 삭제

    @Test
    fun `PhotoVerification 알람 삭제 시 참조 사진 파일이 삭제된다`() = runTest {
        // given — 실제 임시 파일 생성
        val photoFile = File(tempDir, "ref.jpg").also { it.createNewFile() }
        val alarm = createAlarm(dismissMode = DismissMode.PhotoVerification(photoFile.absolutePath))
        coEvery { repository.getAlarmById(1L) } returns alarm
        coJustRun { repository.deleteAlarm(1L) }

        // when
        useCase(1L)

        // then — 파일이 실제로 삭제되었는지 확인
        assertFalse(photoFile.exists())
    }

    @Test
    fun `Normal 알람 삭제 시 참조 사진 파일이 삭제되지 않는다`() = runTest {
        // given
        val photoFile = File(tempDir, "ref.jpg").also { it.createNewFile() }
        val alarm = createAlarm(dismissMode = DismissMode.Normal)
        coEvery { repository.getAlarmById(1L) } returns alarm
        coJustRun { repository.deleteAlarm(1L) }

        // when
        useCase(1L)

        // then — Normal 모드이므로 파일은 그대로 남아 있어야 함
        assertTrue(photoFile.exists())
    }

    @Test
    fun `PhotoVerification 알람이지만 referencePhotoPath가 null이면 파일 삭제가 시도되지 않는다`() = runTest {
        // given
        val alarm = createAlarm(dismissMode = DismissMode.PhotoVerification(null))
        coEvery { repository.getAlarmById(1L) } returns alarm
        coJustRun { repository.deleteAlarm(1L) }

        // when & then — 예외 없이 정상 완료되어야 함
        assertTrue(useCase(1L).isSuccess)
    }

    // endregion

    // region 헬퍼

    private fun createAlarm(
        id: Long = 1L,
        dismissMode: DismissMode = DismissMode.Normal,
    ) = Alarm(
        id = id,
        hour = 7,
        minute = 0,
        repeatDays = emptySet<DayOfWeek>(),
        label = "테스트 알람",
        isEnabled = true,
        dismissMode = dismissMode,
    )

    // endregion
}
