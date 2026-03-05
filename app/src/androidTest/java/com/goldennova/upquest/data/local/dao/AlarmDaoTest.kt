package com.goldennova.upquest.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.data.local.AppDatabase
import com.goldennova.upquest.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AlarmDao] 계측 테스트.
 * [Room.inMemoryDatabaseBuilder]로 생성한 인메모리 DB를 사용하여
 * insert / getAll / getById / update / deleteById 시나리오를 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class AlarmDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AlarmDao

    // 재사용되는 기본 샘플 엔티티
    private val sampleEntity = AlarmEntity(
        hour = 7,
        minute = 30,
        repeatDays = "MONDAY,WEDNESDAY",
        label = "기상 알람",
        isEnabled = true,
        dismissMode = "NORMAL",
        referencePhotoPath = null,
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.alarmDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** insert 후 getAll Flow에서 해당 항목이 방출되는지 검증한다. */
    @Test
    fun insert_후_getAll에서_항목이_조회된다() = runTest {
        dao.insert(sampleEntity)

        val alarms = dao.getAll().first()

        assertEquals(1, alarms.size)
        assertEquals("기상 알람", alarms[0].label)
        assertEquals(7, alarms[0].hour)
        assertEquals(30, alarms[0].minute)
    }

    /** insert가 자동 생성된 ID(양수)를 반환하는지 검증한다. */
    @Test
    fun insert_후_생성된_ID가_반환된다() = runTest {
        val insertedId = dao.insert(sampleEntity)

        assertTrue(insertedId > 0)
    }

    /** insert 후 getById로 동일 항목을 조회할 수 있는지 검증한다. */
    @Test
    fun insert_후_getById로_항목을_조회할_수_있다() = runTest {
        val insertedId = dao.insert(sampleEntity)

        val found = dao.getById(insertedId)

        assertNotNull(found)
        assertEquals(insertedId, found?.id)
        assertEquals("기상 알람", found?.label)
    }

    /** 존재하지 않는 ID로 getById 조회 시 null을 반환하는지 검증한다. */
    @Test
    fun 존재하지_않는_ID로_getById_조회시_null을_반환한다() = runTest {
        val found = dao.getById(999L)

        assertNull(found)
    }

    /** update 후 변경된 값이 getById에 반영되는지 검증한다. */
    @Test
    fun update_후_변경된_값이_조회된다() = runTest {
        val insertedId = dao.insert(sampleEntity)
        val updated = sampleEntity.copy(id = insertedId, label = "수정된 알람", isEnabled = false)

        dao.update(updated)
        val found = dao.getById(insertedId)

        assertEquals("수정된 알람", found?.label)
        assertEquals(false, found?.isEnabled)
    }

    /** deleteById 후 해당 항목이 getAll에서 제거되는지 검증한다. */
    @Test
    fun deleteById_후_항목이_목록에서_제거된다() = runTest {
        val insertedId = dao.insert(sampleEntity)

        dao.deleteById(insertedId)
        val alarms = dao.getAll().first()

        assertTrue(alarms.isEmpty())
    }

    /** deleteById 후 getById로 조회 시 null을 반환하는지 검증한다. */
    @Test
    fun deleteById_후_getById로_조회시_null을_반환한다() = runTest {
        val insertedId = dao.insert(sampleEntity)

        dao.deleteById(insertedId)
        val found = dao.getById(insertedId)

        assertNull(found)
    }

    /** 여러 항목 insert 후 getAll이 시/분 오름차순으로 반환하는지 검증한다. */
    @Test
    fun 여러_항목_insert_후_getAll이_시간_오름차순으로_반환한다() = runTest {
        dao.insert(sampleEntity.copy(hour = 22, minute = 0, label = "취침"))
        dao.insert(sampleEntity.copy(hour = 7, minute = 30, label = "기상"))
        dao.insert(sampleEntity.copy(hour = 12, minute = 0, label = "점심"))

        val alarms = dao.getAll().first()

        assertEquals("기상", alarms[0].label)
        assertEquals("점심", alarms[1].label)
        assertEquals("취침", alarms[2].label)
    }
}
