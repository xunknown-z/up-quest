package com.goldennova.upquest.data.local.mapper

import com.goldennova.upquest.data.local.entity.AlarmEntity
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.AlarmSoundMode
import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek

private const val DISMISS_MODE_NORMAL = "NORMAL"
private const val DISMISS_MODE_PHOTO = "PHOTO_VERIFICATION"

/** [AlarmEntity]를 도메인 모델 [Alarm]으로 변환한다. */
fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    repeatDays = parseRepeatDays(repeatDays),
    label = label,
    isEnabled = isEnabled,
    dismissMode = parseDismissMode(dismissMode, referencePhotoPath),
    ringtoneUri = ringtoneUri,
    soundMode = parseSoundMode(soundMode),
)

/** 도메인 모델 [Alarm]을 [AlarmEntity]로 변환한다. */
fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    repeatDays = formatRepeatDays(repeatDays),
    label = label,
    isEnabled = isEnabled,
    dismissMode = formatDismissMode(dismissMode),
    referencePhotoPath = extractPhotoPath(dismissMode),
    ringtoneUri = ringtoneUri,
    soundMode = soundMode.name,
)

// region 내부 변환 헬퍼

/** 쉼표 구분 요일 문자열 → [Set]<[DayOfWeek]> */
private fun parseRepeatDays(value: String): Set<DayOfWeek> {
    if (value.isBlank()) return emptySet()
    return value.split(",")
        .mapNotNull { runCatching { DayOfWeek.valueOf(it.trim()) }.getOrNull() }
        .toSet()
}

/** [Set]<[DayOfWeek]> → 쉼표 구분 요일 문자열. 빈 세트는 빈 문자열. */
private fun formatRepeatDays(days: Set<DayOfWeek>): String =
    days.joinToString(",") { it.name }

/** dismissMode 문자열 + referencePhotoPath → [DismissMode] */
private fun parseDismissMode(mode: String, photoPath: String?): DismissMode =
    when (mode) {
        DISMISS_MODE_PHOTO -> DismissMode.PhotoVerification(photoPath)
        else -> DismissMode.Normal
    }

/** [DismissMode] → dismissMode 문자열 */
private fun formatDismissMode(mode: DismissMode): String =
    when (mode) {
        is DismissMode.PhotoVerification -> DISMISS_MODE_PHOTO
        DismissMode.Normal -> DISMISS_MODE_NORMAL
    }

/** soundMode 문자열 → [AlarmSoundMode]. 알 수 없는 값은 기본값으로 폴백. */
private fun parseSoundMode(value: String): AlarmSoundMode =
    runCatching { AlarmSoundMode.valueOf(value) }.getOrDefault(AlarmSoundMode.SOUND_AND_VIBRATION)

/** [DismissMode]에서 사진 경로 추출. Normal이면 null. */
private fun extractPhotoPath(mode: DismissMode): String? =
    when (mode) {
        is DismissMode.PhotoVerification -> mode.referencePhotoPath
        DismissMode.Normal -> null
    }

// endregion
