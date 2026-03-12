package com.goldennova.upquest.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [AlarmManagerScheduler] 단위 테스트.
 *
 * Android 프레임워크 의존성([PendingIntent], [Intent], [AlarmManager])을 MockK로 대체하고,
 * [LocalDateTime.now]를 고정해 트리거 시각 계산 로직을 검증한다.
 */
class AlarmManagerSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var mockPendingIntent: PendingIntent
    private lateinit var scheduler: AlarmManagerScheduler

    @BeforeEach
    fun setUp() {
        context = mockk()
        alarmManager = mockk()
        mockPendingIntent = mockk()

        // Intent 생성자 및 putExtra 호출 대체
        // buildPendingIntent에서 alarmId(Long)와 label(String) 두 타입을 extra로 전달하므로 모두 스텁
        mockkConstructor(Intent::class)
        every {
            anyConstructed<Intent>().putExtra(any<String>(), any<Long>())
        } returns mockk()
        every {
            anyConstructed<Intent>().putExtra(any<String>(), any<String>())
        } returns mockk()

        // PendingIntent.getBroadcast 정적 메서드 대체
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any(), any())
        } returns mockPendingIntent

        // AlarmManager 메서드 스텁
        justRun { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        justRun { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
        justRun { alarmManager.cancel(any<PendingIntent>()) }

        scheduler = AlarmManagerScheduler(context, alarmManager)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region schedule — 기본 동작

    @Test
    fun `schedule 호출 시 setExactAndAllowWhileIdle이 RTC_WAKEUP 타입으로 호출된다`() {
        val alarm = createAlarm(isEnabled = true)

        scheduler.schedule(alarm)

        verify(exactly = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                mockPendingIntent,
            )
        }
    }

    @Test
    fun `schedule 호출 시 triggerTime이 현재 시각보다 미래이다`() {
        val beforeMs = System.currentTimeMillis()
        val alarm = createAlarm(isEnabled = true)

        scheduler.schedule(alarm)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                // triggerAtMillis > 현재 시각
                more(beforeMs),
                any(),
            )
        }
    }

    @Test
    fun `isEnabled가 false인 알람은 AlarmManager에 등록되지 않는다`() {
        val alarm = createAlarm(isEnabled = false)

        scheduler.schedule(alarm)

        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
    }

    // endregion

    // region schedule — triggerTime 계산

    @Test
    fun `반복 없는 알람에서 지정 시각이 이미 지난 경우 triggerTime이 내일 같은 시각이다`() {
        // 고정 현재 시각: 수요일 10:00
        val fixedNow = LocalDateTime.of(2026, 3, 4, 10, 0)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns fixedNow

        // 09:00 알람 → 오늘은 이미 지남 → 내일 09:00
        val alarm = createAlarm(hour = 9, minute = 0, repeatDays = emptySet())
        val tomorrowAt9 = LocalDateTime.of(2026, 3, 5, 9, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        scheduler.schedule(alarm)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                tomorrowAt9,
                any(),
            )
        }
    }

    @Test
    fun `반복 없는 알람에서 지정 시각이 미래인 경우 triggerTime이 오늘 같은 시각이다`() {
        // 고정 현재 시각: 수요일 10:00
        val fixedNow = LocalDateTime.of(2026, 3, 4, 10, 0)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns fixedNow

        // 11:00 알람 → 오늘 11:00
        val alarm = createAlarm(hour = 11, minute = 0, repeatDays = emptySet())
        val todayAt11 = LocalDateTime.of(2026, 3, 4, 11, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        scheduler.schedule(alarm)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                todayAt11,
                any(),
            )
        }
    }

    @Test
    fun `반복 요일 알람에서 오늘 요일이 포함되고 시각이 미래이면 오늘 triggerTime으로 설정된다`() {
        // 고정 현재 시각: 수요일(WEDNESDAY) 10:00
        val fixedNow = LocalDateTime.of(2026, 3, 4, 10, 0)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns fixedNow

        // 11:00, 수요일 반복 → 오늘 11:00
        val alarm = createAlarm(
            hour = 11,
            minute = 0,
            repeatDays = setOf(DayOfWeek.WEDNESDAY),
        )
        val todayAt11 = LocalDateTime.of(2026, 3, 4, 11, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        scheduler.schedule(alarm)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                todayAt11,
                any(),
            )
        }
    }

    @Test
    fun `반복 요일 알람에서 오늘 시각이 이미 지났으면 다음 해당 요일로 triggerTime이 설정된다`() {
        // 고정 현재 시각: 수요일(WEDNESDAY) 10:00
        val fixedNow = LocalDateTime.of(2026, 3, 4, 10, 0)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns fixedNow

        // 09:00, 수요일 반복 → 오늘은 지남 → 다음 수요일(3월 11일) 09:00
        val alarm = createAlarm(
            hour = 9,
            minute = 0,
            repeatDays = setOf(DayOfWeek.WEDNESDAY),
        )
        val nextWednesdayAt9 = LocalDateTime.of(2026, 3, 11, 9, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        scheduler.schedule(alarm)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextWednesdayAt9,
                any(),
            )
        }
    }

    // endregion

    // region cancel

    @Test
    fun `cancel 호출 시 alarmManager cancel이 호출된다`() {
        val alarm = createAlarm()

        scheduler.cancel(alarm)

        verify(exactly = 1) { alarmManager.cancel(mockPendingIntent) }
    }

    // endregion

    // region 헬퍼

    private fun createAlarm(
        id: Long = 1L,
        hour: Int = 23,
        minute: Int = 59,
        isEnabled: Boolean = true,
        repeatDays: Set<DayOfWeek> = emptySet(),
    ) = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        repeatDays = repeatDays,
        label = "테스트 알람",
        isEnabled = isEnabled,
        dismissMode = DismissMode.Normal,
    )

    // endregion
}
