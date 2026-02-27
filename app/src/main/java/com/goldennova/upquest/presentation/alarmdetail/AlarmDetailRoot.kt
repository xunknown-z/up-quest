package com.goldennova.upquest.presentation.alarmdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AlarmDetailRoot(
    alarmId: Long = -1L,
    onNavigateBack: () -> Unit = {},
    onNavigateToPhotoSetup: (Long) -> Unit = {},
    viewModel: AlarmDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // SideEffect 수집 — 내비게이션 및 스낵바 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                AlarmDetailSideEffect.NavigateBack -> onNavigateBack()
                is AlarmDetailSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AlarmDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        isNewAlarm = alarmId == -1L,
        onNavigateBack = onNavigateBack,
        onNavigateToPhotoSetup = { onNavigateToPhotoSetup(alarmId) },
        snackbarHostState = snackbarHostState,
    )
}
