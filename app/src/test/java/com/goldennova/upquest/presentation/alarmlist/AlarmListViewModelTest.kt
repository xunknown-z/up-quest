package com.goldennova.upquest.presentation.alarmlist

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.usecase.DeleteAlarmUseCase
import com.goldennova.upquest.domain.usecase.GetAlarmsUseCase
import com.goldennova.upquest.domain.usecase.ToggleAlarmUseCase
import com.goldennova.upquest.util.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {

    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var getAlarmsUseCase: GetAlarmsUseCase
    private lateinit var toggleAlarmUseCase: ToggleAlarmUseCase
    private lateinit var deleteAlarmUseCase: DeleteAlarmUseCase

    @BeforeEach
    fun setUp() {
        getAlarmsUseCase = mockk()
        toggleAlarmUseCase = mockk()
        deleteAlarmUseCase = mockk()
    }

    // 테스트용 알람 픽스처
    private fun createAlarm(
        id: Long = 1L,
        hour: Int = 7,
        minute: Int = 0,
        label: String = "기상",
        isEnabled: Boolean = true,
    ) = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        repeatDays = emptySet(),
        label = label,
        isEnabled = isEnabled,
        dismissMode = DismissMode.Normal,
    )

    private fun createViewModel() = AlarmListViewModel(
        getAlarmsUseCase = getAlarmsUseCase,
        toggleAlarmUseCase = toggleAlarmUseCase,
        deleteAlarmUseCase = deleteAlarmUseCase,
    )

    // region 알람 목록 로드

    @Test
    fun `알람 목록 로드 성공 시 alarms가 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            val alarms = listOf(createAlarm(id = 1L), createAlarm(id = 2L))
            every { getAlarmsUseCase() } returns flowOf(alarms)

            // when
            val viewModel = createViewModel()

            // then
            assertEquals(alarms, viewModel.uiState.value.alarms)
        }

    @Test
    fun `알람 목록 로드 성공 시 isLoading이 false이고 errorMessage가 null이다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())

            // when
            val viewModel = createViewModel()

            // then
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `알람 목록이 비어있으면 alarms가 빈 목록이다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())

            // when
            val viewModel = createViewModel()

            // then
            assertTrue(viewModel.uiState.value.alarms.isEmpty())
        }

    @Test
    fun `알람 목록 로드 실패 시 errorMessage가 설정된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            val errorMessage = "DB 연결 오류"
            every { getAlarmsUseCase() } returns flow { throw RuntimeException(errorMessage) }

            // when
            val viewModel = createViewModel()

            // then
            assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `Repository Flow가 변경되면 alarms가 최신 값으로 업데이트된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            val alarmFlow = MutableStateFlow(listOf(createAlarm(id = 1L)))
            every { getAlarmsUseCase() } returns alarmFlow

            val viewModel = createViewModel()
            assertEquals(1, viewModel.uiState.value.alarms.size)

            // when
            alarmFlow.value = listOf(createAlarm(id = 1L), createAlarm(id = 2L))

            // then
            assertEquals(2, viewModel.uiState.value.alarms.size)
        }

    // endregion

    // region 토글 이벤트

    @Test
    fun `ToggleAlarm 성공 시 ShowError가 방출되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())
            coEvery { toggleAlarmUseCase(any(), any()) } returns Result.success(Unit)

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.ToggleAlarm(id = 1L, enabled = true))

            // then
            assertTrue(collectedEffects.isEmpty())
            job.cancel()
        }

    @Test
    fun `ToggleAlarm 실패 시 ShowError SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())
            coEvery {
                toggleAlarmUseCase(
                    any(),
                    any()
                )
            } returns Result.failure(RuntimeException("토글 실패"))

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.ToggleAlarm(id = 1L, enabled = false))

            // then
            assertEquals(1, collectedEffects.size)
            val effect = collectedEffects[0] as AlarmListSideEffect.ShowError
            assertEquals("토글 실패", effect.message)
            job.cancel()
        }

    // endregion

    // region 삭제 이벤트

    @Test
    fun `DeleteAlarm 성공 시 ShowError가 방출되지 않는다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())
            coEvery { deleteAlarmUseCase(any()) } returns Result.success(Unit)

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.DeleteAlarm(id = 1L))

            // then
            assertTrue(collectedEffects.isEmpty())
            job.cancel()
        }

    @Test
    fun `DeleteAlarm 실패 시 ShowError SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())
            coEvery { deleteAlarmUseCase(any()) } returns Result.failure(RuntimeException("삭제 실패"))

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.DeleteAlarm(id = 1L))

            // then
            assertEquals(1, collectedEffects.size)
            val effect = collectedEffects[0] as AlarmListSideEffect.ShowError
            assertEquals("삭제 실패", effect.message)
            job.cancel()
        }

    // endregion

    // region 내비게이션 SideEffect

    @Test
    fun `AddAlarm 이벤트 시 NavigateToNewAlarm SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.AddAlarm)

            // then
            assertEquals(1, collectedEffects.size)
            assertTrue(collectedEffects[0] is AlarmListSideEffect.NavigateToNewAlarm)
            job.cancel()
        }

    @Test
    fun `EditAlarm 이벤트 시 alarmId를 포함한 NavigateToDetail SideEffect가 방출된다`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            // given
            every { getAlarmsUseCase() } returns flowOf(emptyList())

            val viewModel = createViewModel()
            val collectedEffects = mutableListOf<AlarmListSideEffect>()
            val job = launch { viewModel.sideEffect.collect { collectedEffects.add(it) } }

            // when
            viewModel.onEvent(AlarmListEvent.EditAlarm(id = 42L))

            // then
            assertEquals(1, collectedEffects.size)
            val effect = collectedEffects[0] as AlarmListSideEffect.NavigateToDetail
            assertEquals(42L, effect.alarmId)
            job.cancel()
        }

    // endregion
}
