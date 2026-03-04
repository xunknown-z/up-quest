package com.goldennova.upquest.presentation.photosetup

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goldennova.upquest.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [PhotoSetupScreen] 컴포저블 UI 테스트.
 * ViewModel 없이 [PhotoSetupUiState]를 직접 주입하여 촬영 전/후 버튼 노출 상태와
 * 이벤트 람다 호출을 검증한다.
 *
 * CameraPreview는 AndroidView(PreviewView) 기반으로 실제 카메라 없이는 동작하지 않으므로
 * 촬영 전 상태에서는 버튼 노출만 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class PhotoSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region TopAppBar

    /** 화면 제목이 TopAppBar에 표시된다. */
    @Test
    fun 화면_제목이_표시된다() {
        var title = ""

        composeTestRule.setContent {
            title = stringResource(R.string.photo_setup_title)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    // endregion

    // region 촬영 전 상태 (isPhotoTaken = false)

    /** 촬영 전 상태에서 촬영 버튼이 표시된다. */
    @Test
    fun 촬영_전_상태에서_촬영_버튼이_표시된다() {
        var takePhotoText = ""

        composeTestRule.setContent {
            takePhotoText = stringResource(R.string.photo_take)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(isPhotoTaken = false),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(takePhotoText).assertIsDisplayed()
    }

    /** 촬영 전 상태에서 재촬영 버튼이 표시되지 않는다. */
    @Test
    fun 촬영_전_상태에서_재촬영_버튼이_표시되지_않는다() {
        var retakeText = ""

        composeTestRule.setContent {
            retakeText = stringResource(R.string.photo_retake)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(isPhotoTaken = false),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(retakeText).assertDoesNotExist()
    }

    /** 촬영 전 상태에서 확인 버튼이 표시되지 않는다. */
    @Test
    fun 촬영_전_상태에서_확인_버튼이_표시되지_않는다() {
        var confirmText = ""

        composeTestRule.setContent {
            confirmText = stringResource(R.string.photo_confirm)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(isPhotoTaken = false),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(confirmText).assertDoesNotExist()
    }

    // endregion

    // region 촬영 완료 상태 (isPhotoTaken = true)

    /** 촬영 완료 상태에서 재촬영 버튼이 표시된다. */
    @Test
    fun 촬영_완료_상태에서_재촬영_버튼이_표시된다() {
        var retakeText = ""

        composeTestRule.setContent {
            retakeText = stringResource(R.string.photo_retake)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(
                    isPhotoTaken = true,
                    capturedImagePath = "/storage/photo.jpg",
                ),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(retakeText).assertIsDisplayed()
    }

    /** 촬영 완료 상태에서 확인 버튼이 표시된다. */
    @Test
    fun 촬영_완료_상태에서_확인_버튼이_표시된다() {
        var confirmText = ""

        composeTestRule.setContent {
            confirmText = stringResource(R.string.photo_confirm)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(
                    isPhotoTaken = true,
                    capturedImagePath = "/storage/photo.jpg",
                ),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(confirmText).assertIsDisplayed()
    }

    /** 촬영 완료 상태에서 촬영 버튼이 표시되지 않는다. */
    @Test
    fun 촬영_완료_상태에서_촬영_버튼이_표시되지_않는다() {
        var takePhotoText = ""

        composeTestRule.setContent {
            takePhotoText = stringResource(R.string.photo_take)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(
                    isPhotoTaken = true,
                    capturedImagePath = "/storage/photo.jpg",
                ),
                onEvent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(takePhotoText).assertDoesNotExist()
    }

    // endregion

    // region 이벤트 람다 호출

    /** 재촬영 버튼 클릭 시 RetakePhoto 이벤트가 발생한다. */
    @Test
    fun 재촬영_버튼_클릭시_RetakePhoto_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<PhotoSetupEvent>()
        var retakeText = ""

        composeTestRule.setContent {
            retakeText = stringResource(R.string.photo_retake)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(
                    isPhotoTaken = true,
                    capturedImagePath = "/storage/photo.jpg",
                ),
                onEvent = { capturedEvents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(retakeText).performClick()

        assertTrue(capturedEvents.contains(PhotoSetupEvent.RetakePhoto))
    }

    /** 확인 버튼 클릭 시 Confirm 이벤트가 발생한다. */
    @Test
    fun 확인_버튼_클릭시_Confirm_이벤트가_발생한다() {
        val capturedEvents = mutableListOf<PhotoSetupEvent>()
        var confirmText = ""

        composeTestRule.setContent {
            confirmText = stringResource(R.string.photo_confirm)
            PhotoSetupScreen(
                uiState = PhotoSetupUiState(
                    isPhotoTaken = true,
                    capturedImagePath = "/storage/photo.jpg",
                ),
                onEvent = { capturedEvents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText(confirmText).performClick()

        assertTrue(capturedEvents.contains(PhotoSetupEvent.Confirm))
    }

    // endregion
}
