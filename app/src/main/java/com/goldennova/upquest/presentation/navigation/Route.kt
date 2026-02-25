package com.goldennova.upquest.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object AlarmList

// alarmId = -1 : 신규 생성, 그 외 : 기존 알람 수정
@Serializable
data class AlarmDetail(val alarmId: Long = -1L)

@Serializable
object AlarmAlert

@Serializable
data class PhotoSetup(val alarmId: Long)

@Serializable
object Settings
