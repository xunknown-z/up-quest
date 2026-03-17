package com.goldennova.upquest.data.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * [SystemVibrationPlayer] 단위 테스트.
 *
 * [Build.VERSION_CODES.S] 미만은 [Vibrator]를 직접 사용하는 경로,
 * 이상은 [VibratorManager]를 통해 defaultVibrator를 얻는 경로를 각각 검증한다.
 * sdkVersion을 생성자로 주입해 분기를 제어한다.
 */
class SystemVibrationPlayerTest {

    private val context: Context = mockk()
    private val vibrator: Vibrator = mockk()
    private val vibratorManager: VibratorManager = mockk()
    private val mockEffect: VibrationEffect = mockk()

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region vibrate() — API < 31 경로 (Vibrator 직접 사용)

    @Test
    fun `vibrate() 호출 시 createWaveform이 올바른 패턴 파라미터로 호출된다`() {
        mockkStatic(VibrationEffect::class)
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        every {
            VibrationEffect.createWaveform(any(), any<IntArray>(), any())
        } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }

        // sdkVersion=0 → API < 31 경로
        SystemVibrationPlayer(context, sdkVersion = 0).vibrate()

        verify(exactly = 1) {
            VibrationEffect.createWaveform(
                longArrayOf(0L, 500L, 500L),
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0),
                1,
            )
        }
    }

    @Test
    fun `vibrate() 호출 시 vibrator vibrate가 createWaveform 반환값으로 호출된다`() {
        mockkStatic(VibrationEffect::class)
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        every {
            VibrationEffect.createWaveform(any(), any<IntArray>(), any())
        } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }

        SystemVibrationPlayer(context, sdkVersion = 0).vibrate()

        verify(exactly = 1) { vibrator.vibrate(mockEffect) }
    }

    // endregion

    // region cancel() — API < 31 경로

    @Test
    fun `cancel() 호출 시 vibrator cancel이 호출된다`() {
        every { context.getSystemService(Vibrator::class.java) } returns vibrator
        justRun { vibrator.cancel() }

        SystemVibrationPlayer(context, sdkVersion = 0).cancel()

        verify(exactly = 1) { vibrator.cancel() }
    }

    // endregion

    // region vibrate() — API 31+ 경로 (VibratorManager)

    @Test
    fun `API 31 이상에서 vibrate() 호출 시 VibratorManager defaultVibrator를 통해 진동한다`() {
        mockkStatic(VibrationEffect::class)
        every {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
        } returns vibratorManager
        every { vibratorManager.defaultVibrator } returns vibrator
        every {
            VibrationEffect.createWaveform(any(), any<IntArray>(), any())
        } returns mockEffect
        justRun { vibrator.vibrate(any<VibrationEffect>()) }

        // sdkVersion=31(S) → VibratorManager 경로
        SystemVibrationPlayer(context, sdkVersion = Build.VERSION_CODES.S).vibrate()

        verify(exactly = 1) { vibratorManager.defaultVibrator }
        verify(exactly = 1) { vibrator.vibrate(mockEffect) }
    }

    // endregion

    // region cancel() — API 31+ 경로 (VibratorManager)

    @Test
    fun `API 31 이상에서 cancel() 호출 시 VibratorManager defaultVibrator cancel이 호출된다`() {
        every {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
        } returns vibratorManager
        every { vibratorManager.defaultVibrator } returns vibrator
        justRun { vibrator.cancel() }

        SystemVibrationPlayer(context, sdkVersion = Build.VERSION_CODES.S).cancel()

        verify(exactly = 1) { vibrator.cancel() }
    }

    // endregion
}
