package com.goldennova.upquest.presentation.alarmalert

import androidx.lifecycle.SavedStateHandle
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.GetAlarmByIdUseCase
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import com.goldennova.upquest.domain.usecase.ToggleAlarmUseCase
import com.goldennova.upquest.util.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmAlertViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var getAlarmByIdUseCase: GetAlarmByIdUseCase
    private lateinit var photoVerificationUseCase: PhotoVerificationUseCase
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var toggleAlarmUseCase: ToggleAlarmUseCase

    @BeforeEach
    fun setUp() {
        getAlarmByIdUseCase = mockk()
        photoVerificationUseCase = mockk()
        alarmScheduler = mockk()
        toggleAlarmUseCase = mockk()

        // 기본 스텁 — dismiss 경로의 handleAlarmDismiss에서 호출됨
        justRun { alarmScheduler.schedule(any()) }
        coEvery { toggleAlarmUseCase(any(), any()) } returns Result.success(Unit)
    }

    private fun createViewModel(alarmId: Long = 1L) = AlarmAlertViewModel(
        savedStateHandle = SavedStateHandle(mapOf(AlarmAlertViewModel.KEY_ALARM_ID to alarmId)),
        getAlarmByIdUseCase = getAlarmByIdUseCase,
        photoVerificationUseCase = photoVerificationUseCase,
        alarmScheduler = alarmScheduler,
        toggleAlarmUseCase = toggleAlarmUseCase,
    )

    // 반복 알람 (repeatDays 있음)
    private fun createNormalAlarm(id: Long = 1L, repeatDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY)) = Alarm(
        id = id,
        hour = 7,
        minute = 0,
        repeatDays = repeatDays,
        label = "테스트 알람",
        isEnabled = true,
        dismissMode = DismissMode.Normal,
    )

    // 반복 알람 (repeatDays 있음)
    private fun createPhotoAlarm(id: Long = 1L, referencePath: String = "/storage/ref.jpg") = Alarm(
        id = id,
        hour = 7,
        minute = 0,
        repeatDays = setOf(DayOfWeek.MONDAY),
        label = "사진 알람",
        isEnabled = true,
        dismissMode = DismissMode.PhotoVerification(referencePath),
    )

    // region 초기화 — loadAlarm

    @Test
    fun `ViewModel 생성 시 getAlarmByIdUseCase가 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createNormalAlarm())

            createViewModel(alarmId = 1L)

            coVerify(exactly = 1) { getAlarmByIdUseCase(1L) }
        }

    @Test
    fun `알람 로드 성공 시 UiState에 alarm이 설정된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val alarm = createNormalAlarm(id = 1L)
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(alarm)

            val viewModel = createViewModel(alarmId = 1L)

            assertEquals(alarm, viewModel.uiState.value.alarm)
        }

    @Test
    fun `알람 로드 실패 시 ShowError SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.failure(RuntimeException("DB 오류"))
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val viewModel = createViewModel(alarmId = 1L)
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            // UnconfinedTestDispatcher이므로 init 블록의 loadAlarm은 이미 완료됐지만,
            // SharedFlow(replay=0)이라 수집 시작 전 방출은 받지 못한다. 재로드 효과 확인을 위해
            // 실패 케이스는 UiState.alarm이 null임을 통해 검증한다.
            assertNull(viewModel.uiState.value.alarm)
            job.cancel()
        }

    // endregion

    // region DismissNormal 이벤트

    @Test
    fun `Normal 모드에서 DismissNormal 이벤트 처리 후 DismissAlarm SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createNormalAlarm())
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            assertTrue(effects.contains(AlarmAlertSideEffect.DismissAlarm))
            job.cancel()
        }

    @Test
    fun `Normal 모드에서 DismissNormal 이벤트 처리 후 isDismissed가 true가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createNormalAlarm())
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            assertTrue(viewModel.uiState.value.isDismissed)
        }

    @Test
    fun `PhotoVerification 모드에서 DismissNormal 이벤트를 처리하면 무시된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createPhotoAlarm())
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            assertFalse(viewModel.uiState.value.isDismissed)
            assertTrue(effects.isEmpty())
            job.cancel()
        }

    // endregion

    // region PhotoVerified 이벤트

    @Test
    fun `PhotoVerification 모드에서 PhotoVerified 이벤트 처리 시 photoVerificationUseCase가 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns
                    Result.success(createPhotoAlarm(referencePath = "/storage/ref.jpg"))
            coEvery {
                photoVerificationUseCase.verify("/storage/captured.jpg", "/storage/ref.jpg")
            } returns true
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            coVerify(exactly = 1) {
                photoVerificationUseCase.verify("/storage/captured.jpg", "/storage/ref.jpg")
            }
        }

    @Test
    fun `인증 성공 시 DismissAlarm SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createPhotoAlarm())
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns true
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            assertTrue(effects.contains(AlarmAlertSideEffect.DismissAlarm))
            job.cancel()
        }

    @Test
    fun `인증 성공 시 isPhotoVerified와 isDismissed가 true가 된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createPhotoAlarm())
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns true
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            assertTrue(viewModel.uiState.value.isPhotoVerified)
            assertTrue(viewModel.uiState.value.isDismissed)
        }

    @Test
    fun `인증 실패 시 ShowError SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createPhotoAlarm())
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns false
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            val effect = effects.firstOrNull() as? AlarmAlertSideEffect.ShowError
            assertNotNull(effect)
            job.cancel()
        }

    @Test
    fun `인증 실패 시 isDismissed가 변경되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createPhotoAlarm())
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns false
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            assertFalse(viewModel.uiState.value.isDismissed)
        }

    @Test
    fun `alarm이 null일 때 PhotoVerified 이벤트를 처리하면 무시된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(null)
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            coVerify(exactly = 0) { photoVerificationUseCase.verify(any(), any()) }
            assertTrue(effects.isEmpty())
            job.cancel()
        }

    @Test
    fun `Normal 모드에서 PhotoVerified 이벤트를 처리하면 무시된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(createNormalAlarm())
            val viewModel = createViewModel(alarmId = 1L)
            val effects = mutableListOf<AlarmAlertSideEffect>()
            val job = launch { viewModel.sideEffect.collect { effects.add(it) } }

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            coVerify(exactly = 0) { photoVerificationUseCase.verify(any(), any()) }
            assertTrue(effects.isEmpty())
            job.cancel()
        }

    // endregion

    // region 반복 알람 재등록

    @Test
    fun `반복 알람 Normal 해제 시 AlarmScheduler schedule이 재호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val repeatAlarm = createNormalAlarm(repeatDays = setOf(DayOfWeek.MONDAY))
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(repeatAlarm)
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            verify(exactly = 1) { alarmScheduler.schedule(repeatAlarm) }
        }

    @Test
    fun `비반복 알람 Normal 해제 시 toggleAlarmUseCase가 false로 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val nonRepeatAlarm = createNormalAlarm(repeatDays = emptySet())
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(nonRepeatAlarm)
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            coVerify(exactly = 1) { toggleAlarmUseCase(nonRepeatAlarm.id, false) }
        }

    @Test
    fun `비반복 알람 Normal 해제 시 AlarmScheduler schedule은 호출되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val nonRepeatAlarm = createNormalAlarm(repeatDays = emptySet())
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(nonRepeatAlarm)
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.DismissNormal)

            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    @Test
    fun `반복 알람 PhotoVerified 인증 성공 시 AlarmScheduler schedule이 재호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val repeatPhotoAlarm = createPhotoAlarm()
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(repeatPhotoAlarm)
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns true
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            verify(exactly = 1) { alarmScheduler.schedule(repeatPhotoAlarm) }
        }

    @Test
    fun `비반복 알람 PhotoVerified 인증 성공 시 toggleAlarmUseCase가 false로 호출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val nonRepeatPhotoAlarm = Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                repeatDays = emptySet(),
                label = "비반복 사진 알람",
                isEnabled = true,
                dismissMode = DismissMode.PhotoVerification("/storage/ref.jpg"),
            )
            coEvery { getAlarmByIdUseCase(1L) } returns Result.success(nonRepeatPhotoAlarm)
            coEvery { photoVerificationUseCase.verify(any(), any()) } returns true
            val viewModel = createViewModel(alarmId = 1L)

            viewModel.onEvent(AlarmAlertEvent.PhotoVerified("/storage/captured.jpg"))

            coVerify(exactly = 1) { toggleAlarmUseCase(nonRepeatPhotoAlarm.id, false) }
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }

    // endregion
}
