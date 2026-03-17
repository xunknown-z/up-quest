package com.goldennova.upquest.data.alarm

import android.net.Uri
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * dev flavor용 AlarmSoundPlayer 구현체.
 * 실제 알람음 재생 없이 인터페이스만 충족한다.
 */
@Singleton
class NoOpAlarmSoundPlayer @Inject constructor() : AlarmSoundPlayer {
    override fun play(uri: Uri?) = Unit
    override fun stop() = Unit
}
