package com.goldennova.upquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room DB에 저장되는 알람 엔티티.
 *
 * - [repeatDays]: "MONDAY,WEDNESDAY,FRIDAY" 형식으로 직렬화된 요일 목록. 빈 문자열은 반복 없음.
 * - [dismissMode]: "NORMAL" 또는 "PHOTO_VERIFICATION" 중 하나.
 * - [referencePhotoPath]: 사진 인증 모드일 때만 경로가 저장되며, 일반 모드는 null.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: String,
    val label: String,
    val isEnabled: Boolean,
    val dismissMode: String,
    val referencePhotoPath: String?,
    val ringtoneUri: String? = null, // null = 시스템 기본 알람음
    val soundMode: String = "SOUND_AND_VIBRATION",
)
