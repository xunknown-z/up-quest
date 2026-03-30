package com.goldennova.upquest.data.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.goldennova.upquest.domain.alarm.VibrationPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 시스템 진동기([Vibrator])를 사용하는 [VibrationPlayer] 구현체.
 *
 * [VibrationEffect.createWaveform]의 repeat 파라미터를 사용하면
 * [android.media.AudioManager.STREAM_ALARM] 오디오 재생 중 시스템이 진동을
 * 단발로 제한하는 문제가 있어, 코루틴 루프로 직접 반복 진동을 구현한다.
 *
 * API 버전별 분기:
 * - API 31+ : [VibratorManager].defaultVibrator 사용
 * - API 26~30 : deprecated [Vibrator] 직접 사용
 *
 * [vibrate] 호출 시 ON 1000ms → OFF 500ms 패턴을 코루틴으로 반복한다.
 * [cancel] 호출 시 코루틴을 중단하고 진동을 즉시 멈춘다.
 */
class SystemVibrationPlayer(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : VibrationPlayer {

    private val onDuration = 1000L
    private val offDuration = 500L

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var vibrationJob: Job? = null

    // Build.VERSION.SDK_INT를 직접 사용해 lint가 API 레벨 체크를 인식한다
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    override fun vibrate() {
        vibrationJob?.cancel()
        vibrationJob = scope.launch {
            while (isActive) {
                // 시스템 반복 기능 대신 코루틴 루프로 직접 반복해
                // STREAM_ALARM 재생 중에도 안정적인 반복 진동을 보장한다
                vibrator.vibrate(
                    VibrationEffect.createOneShot(onDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
                delay(onDuration + offDuration)
            }
        }
    }

    override fun cancel() {
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator.cancel()
    }
}
