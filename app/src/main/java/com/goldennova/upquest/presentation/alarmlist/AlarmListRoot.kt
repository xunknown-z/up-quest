package com.goldennova.upquest.presentation.alarmlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AlarmListRoot(
    onNavigateToNewAlarm: () -> Unit = {},
    onNavigateToAlarm: (Long) -> Unit = {},
    viewModel: AlarmListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // SideEffect 수집 — 내비게이션 및 스낵바 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AlarmListSideEffect.NavigateToNewAlarm -> onNavigateToNewAlarm()
                is AlarmListSideEffect.NavigateToDetail -> onNavigateToAlarm(effect.alarmId)
                is AlarmListSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AlarmListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}
