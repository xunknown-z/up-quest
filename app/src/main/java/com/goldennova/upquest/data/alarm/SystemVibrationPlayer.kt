package com.goldennova.upquest.data.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.goldennova.upquest.domain.alarm.VibrationPlayer

@RequiresApi(Build.VERSION_CODES.S)
class SystemVibrationPlayer(
    private val context: Context,
    private val sdkVersion: Int = Build.VERSION.SDK_INT,
) : VibrationPlayer {

    // 0ms 대기 → 500ms 진동 → 500ms 정지 반복 패턴
    private val timings = longArrayOf(0L, 500L, 500L)
    private val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
    private val repeatIndex = 1

    private val vibrator: Vibrator by lazy {
        if (sdkVersion >= Build.VERSION_CODES.S) {
            getVibratorFromManager()
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    private fun getVibratorFromManager(): Vibrator {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        return manager.defaultVibrator
    }

    override fun vibrate() {
        val effect = VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
        vibrator.vibrate(effect)
    }

    override fun cancel() {
        vibrator.cancel()
    }
}
