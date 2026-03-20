package com.goldennova.upquest.presentation.alarmdetail

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.AlarmSoundMode
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.DeleteAlarmUseCase
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek

/**
 * [AlarmDetailViewModel] 단위 테스트.
 *
 * [savedStateHandle.toRoute<AlarmDetail>()][androidx.navigation.SavedStateHandleKt.toRoute]가
 * 내부적으로 [BundleKt.bundleOf] → [android.os.BaseBundle.putLong]을 호출한다.
 * [mockkStatic]으로 [BundleKt.bundleOf]를 가로채 Map 기반 mock [Bundle]을 반환하면
 * Android 프레임워크(Robolectric) 없이 JUnit5 환경에서 테스트할 수 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlarmDetailViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var getAlarmByIdUseCase: GetAlarmByIdUseCase
    private lateinit var saveAlarmUseCase: SaveAlarmUseCase
    private lateinit var deleteAlarmUseCase: DeleteAlarmUseCase

    @BeforeEach
    fun setUp() {
        getAlarmByIdUseCase = mockk()
        saveAlarmUseCase = mockk()
        deleteAlarmUseCase = mockk()
        stubBundleOf()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * [BundleKt.bundleOf]를 mock해 Android 프레임워크 없이 동작하는 Map 기반 Bundle을 반환한다.
     *
     * Navigation의 [RouteDecoder]는 SavedStateHandle에서 값을 읽을 때 중간 단계로
     * `bundleOf(key to value)`를 호출하고, [NavType]이 해당 Bundle에서 값을 꺼낸다.
     * Long 타입의 경우 `Bundle.putLong` / `Bundle.getLong`이 호출된다.
     */
    private fun stubBundleOf() {
        mockkStatic("androidx.core.os.BundleKt")
        every { bundleOf(*anyVararg<Pair<String, Any?>>()) } answers {
            @Suppress("UNCHECKED_CAST")
            val pairs = (args[0] as? Array<*>)
                ?.filterIsInstance<Pair<String, Any?>>()
                ?.toMap()
                ?: emptyMap()
            // mockk {} 람다 안에서 answers{}를 쓰면 타입 추론 문제가 발생하므로,
            // 인스턴스를 먼저 생성한 뒤 외부에서 every {} 를 설정한다.
            val bundle = mockk<Bundle>()
            every { bundle.containsKey(any()) } answers { firstArg<String>() in pairs }
            every { bundle.getLong(any(), any<Long>()) } answers {
                pairs[firstArg<String>()] as? Long ?: secondArg<Long>()
            }
            bundle
        }
    }

    private fun createSavedStateHandle(alarmId: Long) =
        SavedStateHandle(mapOf("alarmId" to alarmId))

    private fun createViewModel(alarmId: Long = -1L) = AlarmDetailViewModel(
        savedStateHandle = createSavedStateHandle(alarmId),
        getAlarmByIdUseCase = getAlarmByIdUseCase,
        saveAlarmUseCase = saveAlarmUseCase,
        deleteAlarmUseCase = deleteAlarmUseCase,
    )

    private fun createAlarm(
        id: Long = 1L,
        hour: Int = 8,
        minute: Int = 30,
        label: String = "테스트 알람",
    ) = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        repeatDays = setOf(DayOfWeek.MONDAY),
        label = label,
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    // region 신규 생성 모드

    @Test
    fun `신규 생성 모드에서는 getAlarmByIdUseCase가 호출되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            createViewModel(alarmId = -1L)

            coVerify(exactly = 0) { getAlarmByIdUseCase(any()) }
        }

    @Test
    fun `신규 생성 모드에서 초기 UiState는 기본값이다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel(alarmId = -1L)

            val state = viewModel.uiState.value
            assertEquals(7, state.hour)
            assertEquals(0, state.minute)
            assertTrue(state.repeatDays.isEmpty())
            assertEquals("", state.label)
            assertEquals(DismissMode.Normal, state.dismissMode)
            assertFalse(state.isLoading)
        }

    // endregion

    // region 수정 모드 — loadAlarm

    @Test
    fun `수정 모드에서는 getAlarmByIdUseCase가 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)

            createViewModel(alarmId = 1L)

            coVerify(exactly = 1) { getAlarmByIdUseCase(1L) }
        }

    @Test
    fun `수정 모드에서 알람 로드 성공 시 UiState가 알람 데이터로 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L, hour = 8, minute = 30, label = "테스트 알람")
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)

            val viewModel = createViewModel(alarmId = 1L)

            val state = viewModel.uiState.value
            assertEquals(8, state.hour)
            assertEquals(30, state.minute)
            assertEquals("테스트 알람", state.label)
            assertEquals(setOf(DayOfWeek.MONDAY), state.repeatDays)
            assertFalse(state.isLoading)
        }

    @Test
    fun `수정 모드에서 알람 로드 실패 시 isLoading이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // UnconfinedTestDispatcher로 loadAlarm()이 ViewModel 생성 시점에 즉시 완료됨.
            // SharedFlow(replay=0)이므로 init에서 방출된 SideEffect는 수집 불가 — isLoading으로 검증.
            coEvery { getAlarmByIdUseCase(1L) } returns Result.failure(RuntimeException("로드 실패"))

            val viewModel = createViewModel(alarmId = 1L)

            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `수정 모드에서 알람이 null이면 isLoading이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(99L) } returns Result.success(null)

            val viewModel = createViewModel(alarmId = 99L)

            assertFalse(viewModel.uiState.value.isLoading)
        }

    // endregion

    // region Event — ChangeHour / ChangeMinute / ToggleDay / ChangeDismissMode

    @Test
    fun `ChangeHour 이벤트 처리 시 hour가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(AlarmDetailEvent.ChangeHour(9))

            assertEquals(9, viewModel.uiState.value.hour)
        }

    @Test
    fun `ChangeMinute 이벤트 처리 시 minute가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(AlarmDetailEvent.ChangeMinute(45))

            assertEquals(45, viewModel.uiState.value.minute)
        }

    @Test
    fun `ToggleDay 이벤트로 선택되지 않은 요일을 토글하면 추가된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(AlarmDetailEvent.ToggleDay(DayOfWeek.WEDNESDAY))

            assertTrue(DayOfWeek.WEDNESDAY in viewModel.uiState.value.repeatDays)
        }

    @Test
    fun `ToggleDay 이벤트로 이미 선택된 요일을 토글하면 제거된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(AlarmDetailEvent.ToggleDay(DayOfWeek.WEDNESDAY))

            viewModel.onEvent(AlarmDetailEvent.ToggleDay(DayOfWeek.WEDNESDAY))

            assertFalse(DayOfWeek.WEDNESDAY in viewModel.uiState.value.repeatDays)
        }

    @Test
    fun `ChangeDismissMode 이벤트 처리 시 dismissMode가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            val photoMode = DismissMode.PhotoVerification("/path/photo.jpg")

            viewModel.onEvent(AlarmDetailEvent.ChangeDismissMode(photoMode))

            assertEquals(photoMode, viewModel.uiState.value.dismissMode)
        }

    // endregion

    // region Event — ChangeSoundMode

    @Test
    fun `ChangeSoundMode 이벤트 처리 시 soundMode가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(AlarmDetailEvent.ChangeSoundMode(AlarmSoundMode.VIBRATION_ONLY))

            assertEquals(AlarmSoundMode.VIBRATION_ONLY, viewModel.uiState.value.soundMode)
        }

    // endregion

    // region Event — ChangeRingtone

    @Test
    fun `ChangeRingtone 이벤트 처리 시 ringtoneUri가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val uri = "content://media/internal/audio/media/12"
            val viewModel = createViewModel()

            viewModel.onEvent(AlarmDetailEvent.ChangeRingtone(uri))

            assertEquals(uri, viewModel.uiState.value.ringtoneUri)
        }

    @Test
    fun `ChangeRingtone 이벤트에 null 전달 시 ringtoneUri가 null이 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(AlarmDetailEvent.ChangeRingtone("content://media/internal/audio/media/12"))

            viewModel.onEvent(AlarmDetailEvent.ChangeRingtone(null))

            assertEquals(null, viewModel.uiState.value.ringtoneUri)
        }

    // endregion

    // region Event — Save

    @Test
    fun `Save 이벤트 성공 시 NavigateBack SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = -1L)
            val effects = mutableListOf<AlarmDetailSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            assertTrue(effects.contains(AlarmDetailSideEffect.NavigateBack))
            job.cancel()
        }

    @Test
    fun `Save 이벤트 실패 시 ShowError SideEffect가 방출되고 isLoading이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { saveAlarmUseCase(any()) } returns Result.failure(RuntimeException("저장 실패"))
            val viewModel = createViewModel(alarmId = -1L)
            val effects = mutableListOf<AlarmDetailSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            val effect = effects.firstOrNull() as? AlarmDetailSideEffect.ShowError
            assertEquals("저장 실패", effect?.message)
            assertFalse(viewModel.uiState.value.isLoading)
            job.cancel()
        }

    @Test
    fun `신규 생성 모드에서 Save 시 id가 0인 Alarm으로 saveAlarmUseCase가 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = -1L)

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            coVerify { saveAlarmUseCase(match { it.id == 0L }) }
        }

    @Test
    fun `수정 모드에서 Save 시 기존 alarmId로 saveAlarmUseCase가 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 5L)
            coEvery { getAlarmByIdUseCase(5L) } returns Result.success(alarm)
            coEvery { saveAlarmUseCase(any()) } returns Result.success(5L)
            val viewModel = createViewModel(alarmId = 5L)

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            coVerify { saveAlarmUseCase(match { it.id == 5L }) }
        }

    @Test
    fun `라벨이 비어 있을 때 Save 시 defaultLabel이 사용된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = -1L)
            // 라벨을 입력하지 않은 상태(기본값 빈 문자열)로 저장

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            coVerify { saveAlarmUseCase(match { it.label == "알람" }) }
        }

    @Test
    fun `Save 시 soundMode가 saveAlarmUseCase 파라미터에 포함된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = -1L)
            viewModel.onEvent(AlarmDetailEvent.ChangeSoundMode(AlarmSoundMode.VIBRATION_ONLY))

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            coVerify { saveAlarmUseCase(match { it.soundMode == AlarmSoundMode.VIBRATION_ONLY }) }
        }

    @Test
    fun `Save 시 ringtoneUri가 saveAlarmUseCase 파라미터에 포함된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val uri = "content://media/internal/audio/media/12"
            coEvery { saveAlarmUseCase(any()) } returns Result.success(1L)
            val viewModel = createViewModel(alarmId = -1L)
            viewModel.onEvent(AlarmDetailEvent.ChangeRingtone(uri))

            viewModel.onEvent(AlarmDetailEvent.Save("알람"))

            coVerify { saveAlarmUseCase(match { it.ringtoneUri == uri }) }
        }

    // endregion

    // region Event — Delete

    @Test
    fun `Delete 이벤트 성공 시 NavigateBack SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)
            coEvery { deleteAlarmUseCase(1L) } returns Result.success(Unit)
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmDetailSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmDetailEvent.Delete)

            assertTrue(effects.contains(AlarmDetailSideEffect.NavigateBack))
            job.cancel()
        }

    @Test
    fun `Delete 이벤트 실패 시 ShowError SideEffect가 방출되고 isLoading이 false가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)
            coEvery { deleteAlarmUseCase(1L) } returns Result.failure(RuntimeException("삭제 실패"))
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmDetailSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmDetailEvent.Delete)

            val effect = effects.firstOrNull() as? AlarmDetailSideEffect.ShowError
            assertEquals("삭제 실패", effect?.message)
            assertFalse(viewModel.uiState.value.isLoading)
            job.cancel()
        }

    // endregion
}
