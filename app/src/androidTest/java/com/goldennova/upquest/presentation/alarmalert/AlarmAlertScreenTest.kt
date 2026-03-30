package com.goldennova.upquest.presentation.alarmalert

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AlarmAlertScreen] 컴포저블 UI 테스트.
 * ViewModel 없이 [AlarmAlertUiState]를 직접 주입하여
 * Normal / PhotoVerification 모드별 버튼 노출 상태와 이벤트 람다 호출을 검증한다.
 *
 * CameraPreview는 실제 카메라 없이는 동작하지 않으므로
 * PhotoVerification 모드에서는 버튼 노출 여부만 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class AlarmAlertScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createNormalAlarm() = Alarm(
        id = 1L,
        hour = 7,
        minute = 0,
        repeatDays = setOf(DayOfWeek.MONDAY),
        label = "기상 알람",
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    private fun createPhotoAlarm() = Alarm(
        id = 2L,
        hour = 8,
        minute = 30,
        repeatDays = emptySet(),
        label = "사진 알람",
        isEnabled = true,
        dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
    )

    // region Normal 모드

    @Test
    fun Normal_모드에서_알람_해제_버튼이_표시된다() {
        var dismissText = ""

        composeTestRule.setContent {
            dismissText = stringResource(R.string.dismiss_normal)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(dismissText).assertIsDisplayed()
    }

    @Test
    fun Normal_모드에서_사진_해제_버튼이_표시되지_않는다() {
        var takePhotoText = ""

        composeTestRule.setContent {
            takePhotoText = stringResource(R.string.alarm_alert_take_photo)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(takePhotoText).assertDoesNotExist()
    }

    @Test
    fun Normal_모드에서_알람_해제_버튼_클릭_시_DismissNormal_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<AlarmAlertEvent>()
        var dismissText = ""

        composeTestRule.setContent {
            dismissText = stringResource(R.string.dismiss_normal)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = { capturedEvents.add(it) },
            )
        }

        composeTestRule.onNodeWithText(dismissText).performClick()

        assertTrue(capturedEvents.contains(AlarmAlertEvent.DismissNormal))
    }

    // endregion

    // region PhotoVerification 모드

    @Test
    fun PhotoVerification_모드에서_사진_해제_버튼이_표시된다() {
        var takePhotoText = ""

        composeTestRule.setContent {
            takePhotoText = stringResource(R.string.alarm_alert_take_photo)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createPhotoAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(takePhotoText).assertIsDisplayed()
    }

    @Test
    fun PhotoVerification_모드에서_알람_해제_버튼이_표시되지_않는다() {
        var dismissText = ""

        composeTestRule.setContent {
            dismissText = stringResource(R.string.dismiss_normal)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createPhotoAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(dismissText).assertDoesNotExist()
    }

    // endregion

    // region 오버레이 슬라이더

    @Test
    fun PhotoVerification_모드에서_referencePhotoPath가_non_null이면_오버레이_슬라이더가_표시된다() {
        var alphaLabel = ""

        composeTestRule.setContent {
            alphaLabel = stringResource(R.string.alarm_alert_overlay_alpha_label)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createPhotoAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(alphaLabel).assertIsDisplayed()
    }

    @Test
    fun Normal_모드에서는_오버레이_슬라이더가_표시되지_않는다() {
        var alphaLabel = ""

        composeTestRule.setContent {
            alphaLabel = stringResource(R.string.alarm_alert_overlay_alpha_label)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(alphaLabel).assertDoesNotExist()
    }

    @Test
    fun PhotoVerification_모드에서_referencePhotoPath가_null이면_오버레이_슬라이더가_표시되지_않는다() {
        var alphaLabel = ""
        val noRefAlarm = Alarm(
            id = 3L,
            hour = 9,
            minute = 0,
            repeatDays = emptySet(),
            label = "기준사진 없는 알람",
            isEnabled = true,
            dismissMode = DismissMode.PhotoVerification(referencePhotoPath = null),
        )

        composeTestRule.setContent {
            alphaLabel = stringResource(R.string.alarm_alert_overlay_alpha_label)
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = noRefAlarm),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(alphaLabel).assertDoesNotExist()
    }

    // endregion

    // region 알람 정보 카드

    @Test
    fun 알람_시간이_카드에_표시된다() {
        composeTestRule.setContent {
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = {},
            )
        }

        // createNormalAlarm()의 시간: 07:00
        composeTestRule.onNodeWithText("07:00").assertIsDisplayed()
    }

    @Test
    fun 알람_라벨이_카드에_표시된다() {
        composeTestRule.setContent {
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = createNormalAlarm()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("기상 알람").assertIsDisplayed()
    }

    @Test
    fun alarm이_null이면_시간_자리에_대시가_표시된다() {
        composeTestRule.setContent {
            AlarmAlertScreen(
                uiState = AlarmAlertUiState(alarm = null),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("--:--").assertIsDisplayed()
    }

    // endregion
}
