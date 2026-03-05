package com.goldennova.upquest.presentation.settings

import com.goldennova.upquest.domain.repository.ThemeRepository
import com.goldennova.upquest.presentation.theme.ThemeMode
import com.goldennova.upquest.util.MainDispatcherExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SettingsViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var themeRepository: ThemeRepository

    @BeforeEach
    fun setUp() {
        themeRepository = mockk(relaxed = true)
    }

    // region 초기 UiState

    @Test
    fun `repository가 SYSTEM을 방출하면 초기 uiState가 SYSTEM이다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)

        val viewModel = SettingsViewModel(themeRepository)

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.currentThemeMode)
    }

    @Test
    fun `repository가 LIGHT를 방출하면 초기 uiState가 LIGHT이다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.LIGHT)

        val viewModel = SettingsViewModel(themeRepository)

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.currentThemeMode)
    }

    @Test
    fun `repository가 DARK를 방출하면 초기 uiState가 DARK이다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.DARK)

        val viewModel = SettingsViewModel(themeRepository)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.currentThemeMode)
    }

    // endregion

    // region Flow 업데이트

    @Test
    fun `repository Flow가 변경되면 uiState가 최신 테마 모드로 업데이트된다`() = runTest {
        val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
        every { themeRepository.getThemeMode() } returns themeFlow

        val viewModel = SettingsViewModel(themeRepository)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.currentThemeMode)

        themeFlow.value = ThemeMode.DARK
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.currentThemeMode)
    }

    // endregion

    // region ChangeThemeMode 이벤트

    @Test
    fun `ChangeThemeMode 이벤트 발생 시 setThemeMode를 호출한다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(themeRepository)

        viewModel.onEvent(SettingsEvent.ChangeThemeMode(ThemeMode.DARK))

        coVerify(exactly = 1) { themeRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `ChangeThemeMode(LIGHT) 이벤트 발생 시 setThemeMode(LIGHT)를 호출한다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(themeRepository)

        viewModel.onEvent(SettingsEvent.ChangeThemeMode(ThemeMode.LIGHT))

        coVerify(exactly = 1) { themeRepository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `ChangeThemeMode 이벤트를 여러 번 발생시키면 매번 setThemeMode를 호출한다`() = runTest {
        every { themeRepository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(themeRepository)

        viewModel.onEvent(SettingsEvent.ChangeThemeMode(ThemeMode.LIGHT))
        viewModel.onEvent(SettingsEvent.ChangeThemeMode(ThemeMode.DARK))
        viewModel.onEvent(SettingsEvent.ChangeThemeMode(ThemeMode.SYSTEM))

        coVerify(exactly = 1) { themeRepository.setThemeMode(ThemeMode.LIGHT) }
        coVerify(exactly = 1) { themeRepository.setThemeMode(ThemeMode.DARK) }
        coVerify(exactly = 1) { themeRepository.setThemeMode(ThemeMode.SYSTEM) }
    }

    // endregion
}
