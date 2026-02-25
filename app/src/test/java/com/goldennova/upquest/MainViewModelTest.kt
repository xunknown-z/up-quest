package com.goldennova.upquest

import com.goldennova.upquest.domain.repository.ThemeRepository
import com.goldennova.upquest.presentation.theme.ThemeMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    // viewModelScope는 Dispatchers.Main을 사용하므로 테스트용으로 교체
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ThemeRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region 초기 상태

    @Test
    fun `repository가 SYSTEM을 방출하면 themeMode 초기값이 SYSTEM이다`() = runTest {
        every { repository.getThemeMode() } returns flowOf(ThemeMode.SYSTEM)

        val viewModel = MainViewModel(repository)

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    @Test
    fun `repository가 LIGHT를 방출하면 themeMode 초기값이 LIGHT이다`() = runTest {
        every { repository.getThemeMode() } returns flowOf(ThemeMode.LIGHT)

        val viewModel = MainViewModel(repository)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun `repository가 DARK를 방출하면 themeMode 초기값이 DARK이다`() = runTest {
        every { repository.getThemeMode() } returns flowOf(ThemeMode.DARK)

        val viewModel = MainViewModel(repository)

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    // endregion

    // region 상태 업데이트

    @Test
    fun `repository Flow가 변경되면 themeMode가 최신 값으로 업데이트된다`() = runTest {
        val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
        every { repository.getThemeMode() } returns themeFlow

        val viewModel = MainViewModel(repository)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        themeFlow.value = ThemeMode.DARK
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `repository Flow가 LIGHT → DARK 순으로 방출되면 최종값은 DARK이다`() = runTest {
        val themeFlow = MutableStateFlow(ThemeMode.LIGHT)
        every { repository.getThemeMode() } returns themeFlow

        val viewModel = MainViewModel(repository)
        themeFlow.value = ThemeMode.DARK

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    // endregion

    // region stateIn 초기값

    @Test
    fun `ViewModel 생성 직후 Flow 수집 전에도 초기값 SYSTEM이 제공된다`() = runTest {
        // stateIn initialValue = SYSTEM 이므로 Flow 방출 전에도 접근 가능
        val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
        every { repository.getThemeMode() } returns themeFlow

        val viewModel = MainViewModel(repository)

        // value 프로퍼티로 즉시 접근 가능해야 함
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    // endregion
}
