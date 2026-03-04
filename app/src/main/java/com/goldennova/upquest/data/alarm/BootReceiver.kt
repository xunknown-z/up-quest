package com.goldennova.upquest.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.usecase.GetAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 기기 재부팅 후 AlarmManager에 등록된 알람이 초기화되므로,
 * [BOOT_COMPLETED] 수신 시 활성화된 알람을 모두 재등록한다.
 *
 * DB 조회가 필요하므로 [goAsync]로 실행 시간을 연장하고
 * IO 코루틴에서 작업 후 반드시 [BroadcastReceiver.PendingResult.finish]를 호출한다.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var getAlarmsUseCase: GetAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                getAlarmsUseCase()
                    .first()
                    .filter { it.isEnabled }
                    .forEach { alarmScheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
