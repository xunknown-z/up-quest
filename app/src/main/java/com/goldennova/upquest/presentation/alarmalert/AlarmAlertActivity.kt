package com.goldennova.upquest.presentation.alarmalert

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.goldennova.upquest.presentation.theme.UpQuestTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 알람 울림 화면 전용 Activity.
 *
 * AlarmManager → AlarmBroadcastReceiver가 Intent extra로 [EXTRA_ALARM_ID]를 전달하면
 * Hilt가 SavedStateHandle에 자동으로 주입하고, [AlarmAlertViewModel]이 이를 읽어 알람을 로드한다.
 *
 * 잠금 화면 위에 표시되어야 하므로 API 27 이상은 [setShowWhenLocked] / [setTurnScreenOn]을,
 * 그 이하는 WindowManager 플래그를 사용한다.
 */
@AndroidEntryPoint
class AlarmAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 잠금 화면 위에 표시 + 화면 깨우기 플래그 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UpQuestTheme {
                AlarmAlertRoot(
                    onDismiss = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    companion object {
        /** AlarmBroadcastReceiver가 Intent extra로 전달하는 알람 ID 키 */
        const val EXTRA_ALARM_ID = AlarmAlertViewModel.KEY_ALARM_ID
    }
}
