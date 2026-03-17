package com.goldennova.upquest.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.goldennova.upquest.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 파일 I/O 없이 테스트하기 위한 인메모리 DataStore 구현체.
 * 실제 DataStore의 파일 쓰기/읽기 동작은 DataStore 라이브러리 책임이므로 여기서는 검증하지 않는다.
 */
private class InMemoryDataStore : DataStore<Preferences> {

    private val _data = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(_data.value)
        _data.value = updated
        return updated
    }
}

class ThemePreferencesDataSourceTest {

    private lateinit var dataSource: ThemePreferencesDataSource

    @BeforeEach
    fun setUp() {
        dataSource = ThemePreferencesDataSource(InMemoryDataStore())
    }

    // region 기본값

    @Test
    fun `저장된 값이 없으면 기본값 SYSTEM을 반환한다`() = runTest {
        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.SYSTEM, result)
    }

    // endregion

    // region setThemeMode — 쓰기 후 읽기

    @Test
    fun `LIGHT 저장 후 LIGHT를 반환한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.LIGHT)

        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.LIGHT, result)
    }

    @Test
    fun `DARK 저장 후 DARK를 반환한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.DARK)

        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.DARK, result)
    }

    @Test
    fun `SYSTEM 명시 저장 후 SYSTEM을 반환한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.SYSTEM)

        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.SYSTEM, result)
    }

    // endregion

    // region 테마 변경

    @Test
    fun `DARK로 변경 후 LIGHT로 재변경하면 LIGHT를 반환한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.DARK)
        dataSource.setThemeMode(ThemeMode.LIGHT)

        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.LIGHT, result)
    }

    @Test
    fun `LIGHT로 변경 후 SYSTEM으로 재변경하면 SYSTEM을 반환한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.LIGHT)
        dataSource.setThemeMode(ThemeMode.SYSTEM)

        val result = dataSource.themeMode.first()
        assertEquals(ThemeMode.SYSTEM, result)
    }

    @Test
    fun `테마 변경 시 Flow가 최신 값을 방출한다`() = runTest {
        dataSource.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, dataSource.themeMode.first())

        dataSource.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, dataSource.themeMode.first())
    }

    // endregion
}
