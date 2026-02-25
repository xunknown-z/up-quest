package com.goldennova.upquest.data.repository

import com.goldennova.upquest.data.datasource.ThemePreferencesDataSource
import com.goldennova.upquest.presentation.theme.ThemeMode
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ThemeRepositoryImplTest {

    private lateinit var dataSource: ThemePreferencesDataSource
    private lateinit var repository: ThemeRepositoryImpl

    @BeforeEach
    fun setUp() {
        dataSource = mockk()
        repository = ThemeRepositoryImpl(dataSource)
    }

    // region getThemeMode

    @Test
    fun `getThemeMode는 DataSource의 themeMode Flow를 반환한다`() = runTest {
        every { dataSource.themeMode } returns flowOf(ThemeMode.SYSTEM)

        val result = repository.getThemeMode().first()

        assertEquals(ThemeMode.SYSTEM, result)
        verify(exactly = 1) { dataSource.themeMode }
    }

    @Test
    fun `getThemeMode는 LIGHT 모드를 그대로 방출한다`() = runTest {
        every { dataSource.themeMode } returns flowOf(ThemeMode.LIGHT)

        val result = repository.getThemeMode().first()

        assertEquals(ThemeMode.LIGHT, result)
    }

    @Test
    fun `getThemeMode는 DARK 모드를 그대로 방출한다`() = runTest {
        every { dataSource.themeMode } returns flowOf(ThemeMode.DARK)

        val result = repository.getThemeMode().first()

        assertEquals(ThemeMode.DARK, result)
    }

    // endregion

    // region setThemeMode

    @Test
    fun `setThemeMode는 DataSource의 setThemeMode에 위임한다`() = runTest {
        coJustRun { dataSource.setThemeMode(any()) }

        repository.setThemeMode(ThemeMode.DARK)

        coVerify(exactly = 1) { dataSource.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setThemeMode LIGHT 호출 시 DataSource에 LIGHT가 전달된다`() = runTest {
        coJustRun { dataSource.setThemeMode(any()) }

        repository.setThemeMode(ThemeMode.LIGHT)

        coVerify(exactly = 1) { dataSource.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `setThemeMode SYSTEM 호출 시 DataSource에 SYSTEM이 전달된다`() = runTest {
        coJustRun { dataSource.setThemeMode(any()) }

        repository.setThemeMode(ThemeMode.SYSTEM)

        coVerify(exactly = 1) { dataSource.setThemeMode(ThemeMode.SYSTEM) }
    }

    // endregion
}
