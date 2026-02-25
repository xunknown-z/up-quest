package com.goldennova.upquest.presentation.alarmdetail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// TODO: Phase 7에서 구현 예정
@Composable
fun AlarmDetailRoot(
    alarmId: Long = -1L,
    onNavigateBack: () -> Unit = {},
    onNavigateToPhotoSetup: (Long) -> Unit = {},
) {
    Text(text = "AlarmDetail(alarmId=$alarmId) — 구현 예정")
}
