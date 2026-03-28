package com.goldennova.upquest.data.alarm

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * [SystemVibrationPlayer] 단위 테스트.
 *
 * JVM 테스트 환경에서는 [android.os.Build.VERSION.SDK_INT] = 0이므로
 * API 26~30 경로(deprecated [Vibrator])가 항상 사용된다.
 * API 31+ ([android.os.VibratorManager]) 경로는 계측 테스트로 검증한다.
 *
 * 코루틴 루프 기반 반복 진동 검증을 위해 [UnconfinedTestDispatcher]를 사용한다.
 * [runTest] 완료 전 반드시 [SystemVibrationPlayer.cancel]을 호출해 코루틴을 종료한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemVibrationPlayerTest {

    private val context: Context = mockk()
    private val vibrator: Vibrator = mockk()
    private val mockEffect: VibrationEffect = mockk()

    private val expectedDuration = 1000L
    private val expectedAmplitude = VibrationEffect.DEFAULT_AMPLITUDE

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun buildPlayer() = SystemVibrationPlayer(
        context,
        dispatcher = UnconfinedTestDispatcher(/* testScheduler는 runTest 블록 안에서 전달 */),
    )

    // region vibrate()

    @Test
    fun `vibrate() 호출 시 createOneShot이 지정 duration과 amplitude로 호출된다`() = runTest {
        mockkStatic(VibrationEffect::class)
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        every { VibrationEffect.createOneShot(any(), any()) } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }
        justRun { vibrator.cancel() }

        val player = SystemVibrationPlayer(context, dispatcher = UnconfinedTestDispatcher(testScheduler))
        player.vibrate()

        verify(exactly = 1) { VibrationEffect.createOneShot(expectedDuration, expectedAmplitude) }
        player.cancel()
    }

    @Test
    fun `vibrate() 호출 시 vibrator vibrate가 createOneShot 반환값으로 호출된다`() = runTest {
        mockkStatic(VibrationEffect::class)
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        every { VibrationEffect.createOneShot(any(), any()) } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }
        justRun { vibrator.cancel() }

        val player = SystemVibrationPlayer(context, dispatcher = UnconfinedTestDispatcher(testScheduler))
        player.vibrate()

        verify(exactly = 1) { vibrator.vibrate(mockEffect) }
        player.cancel()
    }

    @Test
    fun `vibrate() 이후 패턴 주기(1500ms)가 경과하면 vibrate가 반복 호출된다`() = runTest {
        mockkStatic(VibrationEffect::class)
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        every { VibrationEffect.createOneShot(any(), any()) } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }
        justRun { vibrator.cancel() }

        val player = SystemVibrationPlayer(context, dispatcher = UnconfinedTestDispatcher(testScheduler))
        player.vibrate()
        // advanceTimeBy는 경계값 exclusive → 1ms 더해 t=1500ms 예약 태스크를 확실히 실행
        advanceTimeBy(1501L)
        player.cancel()  // verify 전에 취소해 runTest cleanup 시 무한 루프 방지
        verify(exactly = 2) { vibrator.vibrate(mockEffect) }
    }

    // endregion

    // region cancel()

    @Test
    fun `cancel() 호출 시 vibrator cancel이 호출된다`() = runTest {
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        justRun { vibrator.cancel() }

        SystemVibrationPlayer(context, dispatcher = UnconfinedTestDispatcher(testScheduler)).cancel()

        verify(exactly = 1) { vibrator.cancel() }
    }

    // endregion
}
