package com.goldennova.upquest.presentation.photosetup

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.GetAlarmByIdUseCase
import com.goldennova.upquest.domain.usecase.SaveAlarmUseCase
import com.goldennova.upquest.util.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek

/**
 * [PhotoSetupViewModel] 단위 테스트.
 *
 * [AlarmDetailViewModelTest]와 동일하게 [mockkStatic]으로 [BundleKt.bundleOf]를 가로채
 * Android 프레임워크 없이 [SavedStateHandle.toRoute] 호출을 처리한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoSetupViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var getAlarmByIdUseCase: GetAlarmByIdUseCase
    private lateinit var saveAlarmUseCase: SaveAlarmUseCase

    @BeforeEach
    fun setUp() {
        getAlarmByIdUseCase = mockk()
        saveAlarmUseCase = mockk()
        stubBundleOf()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun stubBundleOf() {
        mockkStatic("androidx.core.os.BundleKt")
        every { bundleOf(*anyVararg<Pair<String, Any?>>()) } answers {
            @Suppress("UNCHECKED_CAST")
            val pairs = (args[0] as? Array<*>)
                ?.filterIsInstance<Pair<String, Any?>>()
                ?.toMap()
                ?: emptyMap()
            val bundle = mockk<Bundle>()
            every { bundle.containsKey(any()) } answers { firstArg<String>() in pairs }
            every { bundle.getLong(any(), any<Long>()) } answers {
                pairs[firstArg<String>()] as? Long ?: secondArg<Long>()
            }
            bundle
        }
    }

    private fun createViewModel(alarmId: Long = 1L) = PhotoSetupViewModel(
        savedStateHandle = SavedStateHandle(mapOf("alarmId" to alarmId)),
        getAlarmByIdUseCase = getAlarmByIdUseCase,
        saveAlarmUseCase = saveAlarmUseCase,
    )

    private fun createAlarm(id: Long = 1L) = Alarm(
        id = id,
        hour = 7,
        minute = 0,
        repeatDays = setOf(DayOfWeek.MONDAY),
        label = "테스트 알람",
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    // region 초기 상태

    @Test
    fun `초기 UiState는 기본값이다`() = runTest(mainDispatcherExtension.testDispatcher) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertNull(state.capturedImagePath)
        assertFalse(state.isPhotoTaken)
        assertFalse(state.isCameraReady)
    }

    // endregion

    // region TakePhoto 이벤트

    @Test
    fun `TakePhoto 이벤트 처리 후 capturedImagePath가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertEquals("/storage/photo.jpg", viewModel.uiState.value.capturedImagePath)
        }

    @Test
    fun `TakePhoto 이벤트 처리 후 isPhotoTaken이 true가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            assertTrue(viewModel.uiState.value.isPhotoTaken)
        }

    // endregion

    // region RetakePhoto 이벤트

    @Test
    fun `RetakePhoto 이벤트 처리 후 capturedImagePath가 초기화된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            viewModel.onEvent(PhotoSetupEvent.RetakePhoto)

            assertNull(viewModel.uiState.value.capturedImagePath)
        }

    @Test
    fun `RetakePhoto 이벤트 처리 후 isPhotoTaken이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            viewModel.onEvent(PhotoSetupEvent.RetakePhoto)

            assertFalse(viewModel.uiState.value.isPhotoTaken)
        }

    // endregion

    // region Confirm 이벤트

    @Test
    fun `Confirm 이벤트 성공 시 NavigateBackWithPath SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = 1L)
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))
            val effects = mutableListOf<PhotoSetupSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(PhotoSetupEvent.Confirm)

            val effect = effects.firstOrNull() as? PhotoSetupSideEffect.NavigateBackWithPath
            assertEquals("/storage/photo.jpg", effect?.path)
            job.cancel()
        }

    @Test
    fun `Confirm 이벤트 시 saveAlarmUseCase가 PhotoVerification 모드로 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = 1L)
            viewModel.onEvent(PhotoSetupEvent.TakePhoto("/storage/photo.jpg"))

            viewModel.onEvent(PhotoSetupEvent.Confirm)

            coVerify {
                saveAlarmUseCase(match {
                    val mode = it.dismissMode
                    mode is DismissMode.PhotoVerification &&
                            mode.referencePhotoPath == "/storage/photo.jpg"
                })
            }
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
