package com.goldennova.upquest.data.local.mapper

import com.goldennova.upquest.data.local.entity.AlarmEntity
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

/**
 * [AlarmEntityMapper] 단위 테스트.
 * Normal / PhotoVerification 각 DismissMode에 대해 양방향 변환 정확성을 검증하고,
 * repeatDays 직렬화/역직렬화를 검증한다.
 */
class AlarmEntityMapperTest {

    // region toDomain — Entity → 도메인 모델

    /** Normal DismissMode Entity → 도메인 변환 검증. */
    @Test
    fun `toDomain - Normal 모드 Entity를 도메인 모델로 정확히 변환한다`() {
        val entity = AlarmEntity(
            id = 1L,
            hour = 7,
            minute = 30,
            repeatDays = "MONDAY,WEDNESDAY,FRIDAY",
            label = "기상",
            isEnabled = true,
            dismissMode = "NORMAL",
            referencePhotoPath = null,
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(7, domain.hour)
        assertEquals(30, domain.minute)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            domain.repeatDays
        )
        assertEquals("기상", domain.label)
        assertEquals(true, domain.isEnabled)
        assertInstanceOf(DismissMode.Normal::class.java, domain.dismissMode)
    }

    /** PhotoVerification DismissMode Entity → 도메인 변환 검증. */
    @Test
    fun `toDomain - PhotoVerification 모드 Entity를 도메인 모델로 정확히 변환한다`() {
        val entity = AlarmEntity(
            id = 2L,
            hour = 22,
            minute = 0,
            repeatDays = "",
            label = "사진 알람",
            isEnabled = false,
            dismissMode = "PHOTO_VERIFICATION",
            referencePhotoPath = "/storage/photo.jpg",
        )

        val domain = entity.toDomain()

        val photoMode =
            assertInstanceOf(DismissMode.PhotoVerification::class.java, domain.dismissMode)
        assertEquals("/storage/photo.jpg", photoMode.referencePhotoPath)
        assertTrue(domain.repeatDays.isEmpty())
    }

    /** repeatDays 빈 문자열 → emptySet 변환 검증. */
    @Test
    fun `toDomain - repeatDays가 빈 문자열이면 빈 Set을 반환한다`() {
        val entity = AlarmEntity(
            id = 3L,
            hour = 8,
            minute = 0,
            repeatDays = "",
            label = "",
            isEnabled = true,
            dismissMode = "NORMAL",
            referencePhotoPath = null,
        )

        val domain = entity.toDomain()

        assertTrue(domain.repeatDays.isEmpty())
    }

    // endregion

    // region toEntity — 도메인 모델 → Entity

    /** Normal DismissMode 도메인 → Entity 변환 검증. */
    @Test
    fun `toEntity - Normal 모드 도메인 모델을 Entity로 정확히 변환한다`() {
        val domain = Alarm(
            id = 1L,
            hour = 7,
            minute = 30,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            label = "기상",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(7, entity.hour)
        assertEquals(30, entity.minute)
        assertEquals("NORMAL", entity.dismissMode)
        assertNull(entity.referencePhotoPath)
        // 요일 문자열에 MONDAY, FRIDAY 포함 여부 검증 (순서 무관)
        val days = entity.repeatDays.split(",").toSet()
        assertTrue(days.contains("MONDAY"))
        assertTrue(days.contains("FRIDAY"))
    }

    /** PhotoVerification DismissMode 도메인 → Entity 변환 검증. */
    @Test
    fun `toEntity - PhotoVerification 모드 도메인 모델을 Entity로 정확히 변환한다`() {
        val domain = Alarm(
            id = 2L,
            hour = 6,
            minute = 0,
            repeatDays = emptySet(),
            label = "사진 알람",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification("/storage/photo.jpg"),
        )

        val entity = domain.toEntity()

        assertEquals("PHOTO_VERIFICATION", entity.dismissMode)
        assertEquals("/storage/photo.jpg", entity.referencePhotoPath)
        assertEquals("", entity.repeatDays)
    }

    /** repeatDays emptySet → 빈 문자열 변환 검증. */
    @Test
    fun `toEntity - repeatDays가 빈 Set이면 빈 문자열로 직렬화된다`() {
        val domain = Alarm(
            id = 3L,
            hour = 8,
            minute = 0,
            repeatDays = emptySet(),
            label = "",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
        )

        val entity = domain.toEntity()

        assertEquals("", entity.repeatDays)
    }

    // endregion

    // region 양방향 변환 (왕복 검증)

    /** Normal 모드: Entity → Domain → Entity 왕복 변환 후 값이 동일한지 검증한다. */
    @Test
    fun `양방향 - Normal 모드 Entity를 왕복 변환해도 값이 동일하다`() {
        val original = AlarmEntity(
            id = 1L,
            hour = 7,
            minute = 30,
            repeatDays = "MONDAY,WEDNESDAY",
            label = "기상",
            isEnabled = true,
            dismissMode = "NORMAL",
            referencePhotoPath = null,
        )

        val roundTripped = original.toDomain().toEntity()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.hour, roundTripped.hour)
        assertEquals(original.minute, roundTripped.minute)
        assertEquals(original.label, roundTripped.label)
        assertEquals(original.isEnabled, roundTripped.isEnabled)
        assertEquals(original.dismissMode, roundTripped.dismissMode)
        assertEquals(original.referencePhotoPath, roundTripped.referencePhotoPath)
        // repeatDays는 순서가 다를 수 있으므로 Set으로 비교
        assertEquals(
            original.repeatDays.split(",").toSet(),
            roundTripped.repeatDays.split(",").toSet(),
        )
    }

    /** PhotoVerification 모드: Domain → Entity → Domain 왕복 변환 후 값이 동일한지 검증한다. */
    @Test
    fun `양방향 - PhotoVerification 모드 도메인 모델을 왕복 변환해도 값이 동일하다`() {
        val original = Alarm(
            id = 2L,
            hour = 22,
            minute = 0,
            repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            label = "주말 알람",
            isEnabled = false,
            dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.repeatDays, roundTripped.repeatDays)
        val photoMode =
            assertInstanceOf(DismissMode.PhotoVerification::class.java, roundTripped.dismissMode)
        assertEquals("/storage/ref.jpg", photoMode.referencePhotoPath)
    }

    // endregion

    // region ringtoneUri 매핑 검증

    /** toDomain: ringtoneUri가 null인 Entity → 도메인 모델 변환 시 null 유지 검증. */
    @Test
    fun `toDomain - ringtoneUri가 null이면 도메인 모델의 ringtoneUri도 null이다`() {
        val entity = AlarmEntity(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatDays = "",
            label = "",
            isEnabled = true,
            dismissMode = "NORMAL",
            referencePhotoPath = null,
            ringtoneUri = null,
        )

        val domain = entity.toDomain()

        assertNull(domain.ringtoneUri)
    }

    /** toDomain: ringtoneUri가 non-null인 Entity → 도메인 모델 변환 시 값 유지 검증. */
    @Test
    fun `toDomain - ringtoneUri가 non-null이면 도메인 모델의 ringtoneUri에 값이 복원된다`() {
        val uri = "content://media/internal/audio/media/12"
        val entity = AlarmEntity(
            id = 2L,
            hour = 8,
            minute = 0,
            repeatDays = "",
            label = "",
            isEnabled = true,
            dismissMode = "NORMAL",
            referencePhotoPath = null,
            ringtoneUri = uri,
        )

        val domain = entity.toDomain()

        assertEquals(uri, domain.ringtoneUri)
    }

    /** toEntity: ringtoneUri가 null인 도메인 모델 → Entity 변환 시 null 유지 검증. */
    @Test
    fun `toEntity - ringtoneUri가 null이면 Entity의 ringtoneUri도 null이다`() {
        val domain = Alarm(
            id = 1L,
            hour = 7,
            minute = 0,
            repeatDays = emptySet(),
            label = "",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
            ringtoneUri = null,
        )

        val entity = domain.toEntity()

        assertNull(entity.ringtoneUri)
    }

    /** toEntity: ringtoneUri가 non-null인 도메인 모델 → Entity 변환 시 값 유지 검증. */
    @Test
    fun `toEntity - ringtoneUri가 non-null이면 Entity의 ringtoneUri에 값이 저장된다`() {
        val uri = "content://media/internal/audio/media/12"
        val domain = Alarm(
            id = 2L,
            hour = 8,
            minute = 0,
            repeatDays = emptySet(),
            label = "",
            isEnabled = true,
            dismissMode = DismissMode.Normal,
            ringtoneUri = uri,
        )

        val entity = domain.toEntity()

        assertEquals(uri, entity.ringtoneUri)
    }

    // endregion
}
