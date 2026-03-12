package com.goldennova.upquest.data.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [NotificationHelper] 단위 테스트.
 *
 * [NotificationManagerCompat]을 MockK로 대체해
 * [NotificationHelper.showAlarmNotification] / [NotificationHelper.cancelAlarmNotification]의
 * notify / cancel 위임 동작을 검증한다.
 *
 * 단위 테스트 환경에서는 [android.os.Build.VERSION.SDK_INT] 가 0이므로
 * POST_NOTIFICATIONS 권한 체크(API 33+) 블록은 실행되지 않아 [notify]가 항상 호출된다.
 */
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var helper: NotificationHelper

    @BeforeEach
    fun setUp() {
        context = mockk()
        notificationManager = mockk()

        every { context.getString(any<Int>()) } returns "알람"
        every { context.packageName } returns "com.goldennova.upquest"

        // NotificationManagerCompat.from() 정적 메서드 대체
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManager
        justRun { notificationManager.notify(any<Int>(), any()) }
        justRun { notificationManager.cancel(any<Int>()) }

        // PendingIntent.getActivity() 정적 메서드 대체
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk()

        // Intent 생성자 및 메서드 대체
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Long>()) } returns mockk()
        every { anyConstructed<Intent>().setFlags(any<Int>()) } returns mockk()

        // NotificationCompat.Builder 생성자 및 플루언트 메서드 대체
        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().setSmallIcon(any<Int>()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setContentTitle(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setContentText(any()) } answers { self as NotificationCompat.Builder }
        every {
            anyConstructed<NotificationCompat.Builder>().setFullScreenIntent(
                any(),
                any()
            )
        } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setOngoing(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setCategory(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setPriority(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setAutoCancel(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk()

        helper = NotificationHelper(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region showAlarmNotification

    @Test
    fun `showAlarmNotification 호출 시 alarmId를 알림 ID로 사용해 notify가 호출된다`() {
        val alarmId = 42L

        helper.showAlarmNotification(alarmId, "기상 알람")

        verify(exactly = 1) { notificationManager.notify(alarmId.toInt(), any()) }
    }

    @Test
    fun `showAlarmNotification을 서로 다른 alarmId로 호출하면 각각 다른 알림 ID로 notify가 호출된다`() {
        helper.showAlarmNotification(1L, "알람 1")
        helper.showAlarmNotification(2L, "알람 2")

        verify(exactly = 1) { notificationManager.notify(1, any()) }
        verify(exactly = 1) { notificationManager.notify(2, any()) }
    }

    // endregion

    // region cancelAlarmNotification

    @Test
    fun `cancelAlarmNotification 호출 시 alarmId를 사용해 cancel이 호출된다`() {
        val alarmId = 42L

        helper.cancelAlarmNotification(alarmId)

        verify(exactly = 1) { notificationManager.cancel(alarmId.toInt()) }
    }

    @Test
    fun `cancelAlarmNotification을 서로 다른 alarmId로 호출하면 각각 다른 ID로 cancel이 호출된다`() {
        helper.cancelAlarmNotification(1L)
        helper.cancelAlarmNotification(2L)

        verify(exactly = 1) { notificationManager.cancel(1) }
        verify(exactly = 1) { notificationManager.cancel(2) }
    }

    // endregion
}
