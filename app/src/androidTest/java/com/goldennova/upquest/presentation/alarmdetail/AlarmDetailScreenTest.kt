package com.goldennova.upquest.presentation.alarmdetail

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.DismissMode
import java.time.DayOfWeek
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AlarmDetailScreen] 컴포저블 UI 테스트.
 * ViewModel 없이 [AlarmDetailUiState]를 직접 주입하여 렌더링과 이벤트 람다 호출을 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class AlarmDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region 신규 생성 모드 / 수정 모드 렌더링

    /** 신규 생성 모드에서 "새 알람" 제목과 저장 버튼이 표시되고 삭제 버튼은 없다. */
    @Test
    fun 신규_생성_모드에서_제목과_저장_버튼이_표시되고_삭제_버튼은_없다() {
        var titleNew = ""
        var saveText = ""
        var deleteText = ""

        composeTestRule.setContent {
            titleNew = stringResource(R.string.alarm_detail_title_new)
            saveText = stringResource(R.string.alarm_save)
            deleteText = stringResource(R.string.alarm_delete)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(),
                onEvent = {},
                isNewAlarm = true,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText(titleNew).assertIsDisplayed()
        composeTestRule.onNodeWithText(saveText).assertIsDisplayed()
        composeTestRule.onNodeWithText(deleteText).assertDoesNotExist()
    }

    /** 수정 모드에서 "알람 편집" 제목, 저장 버튼, 삭제 버튼이 모두 표시된다. */
    @Test
    fun 수정_모드에서_제목과_저장_삭제_버튼이_표시된다() {
        var titleEdit = ""
        var saveText = ""
        var deleteText = ""

        composeTestRule.setContent {
            titleEdit = stringResource(R.string.alarm_detail_title_edit)
            saveText = stringResource(R.string.alarm_save)
            deleteText = stringResource(R.string.alarm_delete)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(),
                onEvent = {},
                isNewAlarm = false,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText(titleEdit).assertIsDisplayed()
        composeTestRule.onNodeWithText(saveText).assertIsDisplayed()
        composeTestRule.onNodeWithText(deleteText).assertIsDisplayed()
    }

    /** 수정 모드에서 UiState의 라벨이 텍스트 필드에 표시된다. */
    @Test
    fun 수정_모드에서_기존_라벨이_입력_필드에_표시된다() {
        composeTestRule.setContent {
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(label = "기상 알람"),
                onEvent = {},
                isNewAlarm = false,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText("기상 알람").assertIsDisplayed()
    }

    /** 반복 요일 섹션 라벨이 표시된다. */
    @Test
    fun 반복_요일_섹션_라벨이_표시된다() {
        var repeatLabel = ""

        composeTestRule.setContent {
            repeatLabel = stringResource(R.string.repeat_days_label)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(
                    repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                ),
                onEvent = {},
                isNewAlarm = true,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText(repeatLabel).assertIsDisplayed()
    }

    // endregion

    // region 해제 모드 — 사진 등록 버튼 노출/숨김

    /** DismissMode.PhotoVerification 상태에서 사진 등록 버튼이 표시된다. */
    @Test
    fun 사진_인증_모드에서_사진_등록_버튼이_표시된다() {
        var photoSetupText = ""

        composeTestRule.setContent {
            photoSetupText = stringResource(R.string.photo_setup_title)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(
                    dismissMode = DismissMode.PhotoVerification("/path/photo.jpg"),
                ),
                onEvent = {},
                isNewAlarm = true,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText(photoSetupText).assertIsDisplayed()
    }

    /** DismissMode.Normal 상태에서 사진 등록 버튼이 표시되지 않는다. */
    @Test
    fun 일반_모드에서_사진_등록_버튼이_표시되지_않는다() {
        var photoSetupText = ""

        composeTestRule.setContent {
            photoSetupText = stringResource(R.string.photo_setup_title)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(dismissMode = DismissMode.Normal),
                onEvent = {},
                isNewAlarm = true,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        composeTestRule.onNodeWithText(photoSetupText).assertDoesNotExist()
    }

    /** 사진 인증 라디오 버튼 행 클릭 시 ChangeDismissMode 이벤트가 발생한다. */
    @Test
    fun 사진_인증_라디오_클릭시_ChangeDismissMode_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<AlarmDetailEvent>()
        var dismissPhotoText = ""

        composeTestRule.setContent {
            dismissPhotoText = stringResource(R.string.dismiss_photo)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(dismissMode = DismissMode.Normal),
                onEvent = { capturedEvents.add(it) },
                isNewAlarm = true,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        // selectable Row가 자식의 시맨틱을 병합하므로, 텍스트로 해당 행을 탐색·클릭할 수 있다
        composeTestRule.onNodeWithText(dismissPhotoText).performClick()

        val event = capturedEvents
            .filterIsInstance<AlarmDetailEvent.ChangeDismissMode>()
            .firstOrNull()
        assertTrue(event?.mode is DismissMode.PhotoVerification)
    }

    // endregion

    // region 로딩 상태

    /** isLoading = true 일 때 폼 콘텐츠(해제 방식 라벨)가 표시되지 않는다. */
    @Test
    fun 로딩_상태에서_폼이_표시되지_않는다() {
        var dismissModeLabel = ""

        composeTestRule.setContent {
            dismissModeLabel = stringResource(R.string.dismiss_mode_label)
            AlarmDetailScreen(
                uiState = AlarmDetailUiState(isLoading = true),
                onEvent = {},
                isNewAlarm = false,
                onNavigateBack = {},
                onNavigateToPhotoSetup = {},
            )
        }

        // AlarmDetailForm은 isLoading = true 일 때 컴포즈되지 않는다
        composeTestRule.onNodeWithText(dismissModeLabel).assertDoesNotExist()
    }

    // endregion
}
