package com.goldennova.upquest.data.alarm

import android.util.Log
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * dev 빌드용 AlarmScheduler — 시스템 AlarmManager를 사용하지 않고 로그만 출력한다.
 * AlarmAlertViewModel의 재등록 로직을 실제 알람 없이 검증할 수 있도록 한다.
 */
@Singleton
class FakeAlarmScheduler @Inject constructor() : AlarmScheduler {

    override fun schedule(alarm: Alarm) {
        Log.d(TAG, "schedule: id=${alarm.id}, ${alarm.hour}:${alarm.minute}, repeatDays=${alarm.repeatDays}")
    }

    override fun cancel(alarm: Alarm) {
        Log.d(TAG, "cancel: id=${alarm.id}")
    }

    private companion object {
        const val TAG = "FakeAlarmScheduler"
    }
}
