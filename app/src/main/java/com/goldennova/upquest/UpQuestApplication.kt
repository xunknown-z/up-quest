package com.goldennova.upquest

import android.app.Application
import com.goldennova.upquest.data.alarm.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UpQuestApplication : Application() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        // 알람 알림 채널 등록 — 앱 최초 실행 시 한 번만 생성되며, 이미 존재하면 무시된다
        notificationHelper.createChannel()
    }
}
