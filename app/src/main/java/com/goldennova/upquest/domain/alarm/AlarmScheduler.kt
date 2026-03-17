package com.goldennova.upquest.domain.alarm

import com.goldennova.upquest.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
}
