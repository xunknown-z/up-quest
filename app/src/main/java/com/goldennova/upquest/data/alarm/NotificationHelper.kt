package com.goldennova.upquest.data.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goldennova.upquest.R
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 알림 채널 생성 및 알림 발송/취소를 담당하는 헬퍼 클래스.
 *
 * Android 10+ 백그라운드 Activity 실행 제한 대응:
 * [showAlarmNotification]에서 [NotificationCompat.Builder.setFullScreenIntent]를 사용해
 * 잠금/백그라운드 상태에서도 [AlarmAlertActivity]가 전면에 표시되도록 한다.
 *
 * Android 14(API 34)+에서는 USE_FULL_SCREEN_INTENT 권한이 기본 거부 상태이므로
 * [canUseFullScreenIntent]로 상태를 확인하고, 미허용 시 시스템 설정으로 안내해야 한다.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
    }

    /**
     * 알람 알림 채널을 생성한다. 이미 존재하면 무시된다.
     * [UpQuestApplication.onCreate]에서 호출해야 한다.
     */
    fun createChannel() {
        // NotificationChannel은 API 26(Android 8.0)부터 지원
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            context.getString(R.string.notification_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            // 채널 수준 진동 활성화 및 반복 패턴 설정
            // [0ms OFF → 1000ms ON → 500ms OFF] 패턴으로 알람 알림 도착 시 진동
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 1000L, 500L)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * 알람 알림을 발송한다.
     *
     * [NotificationCompat.Builder.setFullScreenIntent]를 통해 기기가 잠긴 상태 또는
     * 앱이 백그라운드일 때도 [AlarmAlertActivity]가 전면에 표시된다.
     *
     * @param alarmId 알람 ID — 알림 ID 및 PendingIntent requestCode로 사용
     * @param label 알림에 표시될 알람 라벨
     */
    fun showAlarmNotification(alarmId: Long, label: String) {
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val displayLabel = label.ifBlank { context.getString(R.string.alarm_alert_title) }

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.alarm_alert_title))
            .setContentText(displayLabel)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()

        // Android 13(API 33)+에서는 POST_NOTIFICATIONS 런타임 권한이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context).notify(alarmId.toInt(), notification)
    }

    /**
     * 발송된 알람 알림을 취소한다.
     * [AlarmAlertActivity.onDestroy]에서 호출한다.
     *
     * @param alarmId [showAlarmNotification]에서 사용한 동일한 알람 ID
     */
    fun cancelAlarmNotification(alarmId: Long) {
        NotificationManagerCompat.from(context).cancel(alarmId.toInt())
    }

    /**
     * Android 14(API 34)+에서 USE_FULL_SCREEN_INTENT 권한 허용 여부를 반환한다.
     * API 34 미만에서는 항상 true를 반환한다.
     *
     * false인 경우 [android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT]
     * Intent로 시스템 설정 화면으로 이동하도록 사용자에게 안내해야 한다.
     */
    fun canUseFullScreenIntent(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java)
                .canUseFullScreenIntent()
        } else {
            true
        }
}
