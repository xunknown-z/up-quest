package com.goldennova.upquest.presentation.alarmalert

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AlarmAlertRoot(
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlarmAlertViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // SideEffect 수집 — 알람 해제 및 오류 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                AlarmAlertSideEffect.DismissAlarm -> onDismiss()
                is AlarmAlertSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AlarmAlertScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    )
}
