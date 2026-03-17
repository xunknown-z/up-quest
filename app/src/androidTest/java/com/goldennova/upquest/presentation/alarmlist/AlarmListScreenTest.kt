package com.goldennova.upquest.presentation.alarmlist

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AlarmListScreen] 컴포저블 UI 테스트.
 * ViewModel 없이 UiState를 직접 주입하여 렌더링과 이벤트 람다 호출을 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class AlarmListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 재사용되는 기본 샘플 알람 (isEnabled = true)
    private val sampleAlarm = Alarm(
        id = 1L,
        hour = 7,
        minute = 30,
        repeatDays = setOf(DayOfWeek.MONDAY),
        label = "기상 알람",
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    /** 알람 목록이 있을 때 각 카드의 시간·라벨이 화면에 표시되는지 검증한다. */
    @Test
    fun 알람_목록이_있을때_카드가_렌더링된다() {
        composeTestRule.setContent {
            AlarmListScreen(
                uiState = AlarmListUiState(
                    alarms = listOf(
                        sampleAlarm,
                        Alarm(
                            id = 2L,
                            hour = 22,
                            minute = 0,
                            repeatDays = emptySet(),
                            label = "취침 알람",
                            isEnabled = false,
                            dismissMode = DismissMode.PhotoVerification(""),
                        ),
                    ),
                ),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("07:30").assertIsDisplayed()
        composeTestRule.onNodeWithText("기상 알람").assertIsDisplayed()
        composeTestRule.onNodeWithText("22:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("취침 알람").assertIsDisplayed()
    }

    /** 알람이 없을 때 빈 상태 텍스트가 표시되는지 검증한다. */
    @Test
    fun 알람이_없을때_빈_상태_텍스트가_표시된다() {
        var emptyText = ""

        composeTestRule.setContent {
            emptyText = stringResource(R.string.alarm_list_empty)
            AlarmListScreen(
                uiState = AlarmListUiState(alarms = emptyList()),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(emptyText).assertIsDisplayed()
    }

    /** FAB 클릭 시 AddAlarm 이벤트 람다가 호출되는지 검증한다. */
    @Test
    fun FAB_클릭시_AddAlarm_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<AlarmListEvent>()
        var addAlarmDesc = ""

        composeTestRule.setContent {
            addAlarmDesc = stringResource(R.string.add_alarm)
            AlarmListScreen(
                uiState = AlarmListUiState(alarms = emptyList()),
                onEvent = { capturedEvents.add(it) },
            )
        }

        composeTestRule
            .onNodeWithContentDescription(addAlarmDesc)
            .performClick()

        assertTrue(capturedEvents.contains(AlarmListEvent.AddAlarm))
    }

    /**
     * Switch 클릭 시 ToggleAlarm 이벤트 람다가 올바른 인자로 호출되는지 검증한다.
     * sampleAlarm 의 isEnabled 가 true 이므로 토글 후 enabled = false 가 전달되어야 한다.
     */
    @Test
    fun 토글_클릭시_ToggleAlarm_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<AlarmListEvent>()

        composeTestRule.setContent {
            AlarmListScreen(
                uiState = AlarmListUiState(alarms = listOf(sampleAlarm)),
                onEvent = { capturedEvents.add(it) },
            )
        }

        // Switch 는 toggleable 시맨틱을 가지므로 isToggleable() 로 탐색한다
        composeTestRule.onNode(isToggleable()).performClick()

        val toggleEvent = capturedEvents
            .filterIsInstance<AlarmListEvent.ToggleAlarm>()
            .firstOrNull()
        assertEquals(1L, toggleEvent?.id)
        assertEquals(false, toggleEvent?.enabled)
    }
}
