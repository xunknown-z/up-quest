package com.goldennova.upquest.presentation.settings

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.R
import com.goldennova.upquest.presentation.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [SettingsScreen] 컴포저블 UI 테스트.
 * ViewModel 없이 [SettingsUiState]를 직접 주입하여 렌더링과 이벤트 람다 호출을 검증한다.
 * selectable Row가 자식의 시맨틱을 병합하므로, onNodeWithText 로 항목을 탐색하고
 * assertIsSelected / assertIsNotSelected 로 선택 상태를 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region 테마 모드 선택 상태 표시 검증

    /** LIGHT 모드 UiState → 라이트 항목만 선택 상태로 표시된다. */
    @Test
    fun 라이트_모드_UiState일때_라이트_항목이_선택_상태로_표시된다() {
        var lightText = ""
        var darkText = ""
        var systemText = ""

        composeTestRule.setContent {
            lightText = stringResource(R.string.theme_light)
            darkText = stringResource(R.string.theme_dark)
            systemText = stringResource(R.string.theme_system)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.LIGHT),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(lightText).assertIsSelected()
        composeTestRule.onNodeWithText(darkText).assertIsNotSelected()
        composeTestRule.onNodeWithText(systemText).assertIsNotSelected()
    }

    /** DARK 모드 UiState → 다크 항목만 선택 상태로 표시된다. */
    @Test
    fun 다크_모드_UiState일때_다크_항목이_선택_상태로_표시된다() {
        var lightText = ""
        var darkText = ""
        var systemText = ""

        composeTestRule.setContent {
            lightText = stringResource(R.string.theme_light)
            darkText = stringResource(R.string.theme_dark)
            systemText = stringResource(R.string.theme_system)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.DARK),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(darkText).assertIsSelected()
        composeTestRule.onNodeWithText(lightText).assertIsNotSelected()
        composeTestRule.onNodeWithText(systemText).assertIsNotSelected()
    }

    /** SYSTEM 모드 UiState → 시스템 항목만 선택 상태로 표시된다. */
    @Test
    fun 시스템_모드_UiState일때_시스템_항목이_선택_상태로_표시된다() {
        var lightText = ""
        var darkText = ""
        var systemText = ""

        composeTestRule.setContent {
            lightText = stringResource(R.string.theme_light)
            darkText = stringResource(R.string.theme_dark)
            systemText = stringResource(R.string.theme_system)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.SYSTEM),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(systemText).assertIsSelected()
        composeTestRule.onNodeWithText(lightText).assertIsNotSelected()
        composeTestRule.onNodeWithText(darkText).assertIsNotSelected()
    }

    // endregion

    // region 항목 클릭 시 이벤트 호출 검증

    /** 라이트 항목 클릭 시 ChangeThemeMode(LIGHT) 이벤트가 발생한다. */
    @Test
    fun 라이트_항목_클릭시_ChangeThemeMode_LIGHT_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<SettingsEvent>()
        var lightText = ""

        composeTestRule.setContent {
            lightText = stringResource(R.string.theme_light)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.SYSTEM),
                onEvent = { capturedEvents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(lightText).performClick()

        val event = capturedEvents
            .filterIsInstance<SettingsEvent.ChangeThemeMode>()
            .firstOrNull()
        assertEquals(ThemeMode.LIGHT, event?.mode)
    }

    /** 다크 항목 클릭 시 ChangeThemeMode(DARK) 이벤트가 발생한다. */
    @Test
    fun 다크_항목_클릭시_ChangeThemeMode_DARK_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<SettingsEvent>()
        var darkText = ""

        composeTestRule.setContent {
            darkText = stringResource(R.string.theme_dark)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.SYSTEM),
                onEvent = { capturedEvents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(darkText).performClick()

        val event = capturedEvents
            .filterIsInstance<SettingsEvent.ChangeThemeMode>()
            .firstOrNull()
        assertEquals(ThemeMode.DARK, event?.mode)
    }

    /** 시스템 항목 클릭 시 ChangeThemeMode(SYSTEM) 이벤트가 발생한다. */
    @Test
    fun 시스템_항목_클릭시_ChangeThemeMode_SYSTEM_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<SettingsEvent>()
        var systemText = ""

        composeTestRule.setContent {
            systemText = stringResource(R.string.theme_system)
            SettingsScreen(
                uiState = SettingsUiState(currentThemeMode = ThemeMode.LIGHT),
                onEvent = { capturedEvents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(systemText).performClick()

        val event = capturedEvents
            .filterIsInstance<SettingsEvent.ChangeThemeMode>()
            .firstOrNull()
        assertEquals(ThemeMode.SYSTEM, event?.mode)
    }

    // endregion
}
