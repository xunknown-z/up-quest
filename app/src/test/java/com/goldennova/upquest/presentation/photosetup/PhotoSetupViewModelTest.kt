package com.goldennova.upquest.presentation.photosetup

import com.goldennova.upquest.util.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoSetupViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private fun createViewModel() = PhotoSetupViewModel()

    // region 초기 상태

    @Test
    fun `초기 UiState는 기본값이다`() = runTest(mainDispatcherExtension.testDispatcher) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertNull(state.capturedImagePath)
        assertFalse(state.isPhotoTaken)
        assertFalse(state.isCameraReady)
        assertFalse(state.isCameraPermissionGranted)
    }

    // endregion

    // region UpdateCameraPermission 이벤트

    @Test
    fun `UpdateCameraPermission(true) 이벤트 처리 후 isCameraPermissionGranted가 true가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))

            assertTrue(viewModel.uiState.value.isCameraPermissionGranted)
        }

    @Test
    fun `UpdateCameraPermission(false) 이벤트 처리 후 isCameraPermissionGranted가 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))

            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(false))

            assertFalse(viewModel.uiState.value.isCameraPermissionGranted)
        }

    // endregion

    // region TakePhoto 이벤트 — 권한 허용 상태

    @Test
    fun `권한 허용 상태에서 TakePhoto 이벤트 처리 후 capturedImagePath가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertEquals("/storage/photo.jpg", viewModel.uiState.value.capturedImagePath)
        }

    @Test
    fun `권한 허용 상태에서 TakePhoto 이벤트 처리 후 isPhotoTaken이 true가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertTrue(viewModel.uiState.value.isPhotoTaken)
        }

    // endregion

    // region TakePhoto 이벤트 — 권한 미허용 상태 (guard)

    @Test
    fun `권한 미허용 상태에서 TakePhoto 이벤트를 처리하면 capturedImagePath가 변경되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel() // isCameraPermissionGranted = false (기본값)

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertNull(viewModel.uiState.value.capturedImagePath)
        }

    @Test
    fun `권한 미허용 상태에서 TakePhoto 이벤트를 처리하면 isPhotoTaken이 변경되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel() // isCameraPermissionGranted = false (기본값)

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertFalse(viewModel.uiState.value.isPhotoTaken)
        }

    // endregion

    // region RetakePhoto 이벤트

    @Test
    fun `RetakePhoto 이벤트 처리 후 capturedImagePath가 초기화된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            viewModel.onEvent(PhotoSetupEvent.RetakePhoto)

            assertNull(viewModel.uiState.value.capturedImagePath)
        }

    @Test
    fun `RetakePhoto 이벤트 처리 후 isPhotoTaken이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            viewModel.onEvent(PhotoSetupEvent.RetakePhoto)

            assertFalse(viewModel.uiState.value.isPhotoTaken)
        }

    // endregion

    // region Confirm 이벤트

    @Test
    fun `Confirm 이벤트 시 NavigateBackWithPath SideEffect가 촬영 경로와 함께 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.UpdateCameraPermission(true))
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))
            val effects = mutableListOf<PhotoSetupSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(PhotoSetupEvent.Confirm)

            val effect = effects.firstOrNull() as? PhotoSetupSideEffect.NavigateBackWithPath
            assertEquals("/storage/photo.jpg", effect?.path)
            job.cancel()
        }

    @Test
    fun `capturedImagePath가 null일 때 Confirm 이벤트를 처리하면 SideEffect가 방출되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            val effects = mutableListOf<PhotoSetupSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(PhotoSetupEvent.Confirm)

            assertTrue(effects.isEmpty())
            job.cancel()
        }

    // endregion
}
