package com.goldennova.upquest.presentation.photosetup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PhotoSetupRoot(
    alarmId: Long,
    onNavigateBack: () -> Unit = {},
    viewModel: PhotoSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // SideEffect 수집 — 촬영 확인 완료 시 뒤로 이동
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is PhotoSetupSideEffect.NavigateBackWithPath -> onNavigateBack()
            }
        }
    }

    PhotoSetupScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}
