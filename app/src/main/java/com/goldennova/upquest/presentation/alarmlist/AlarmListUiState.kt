package com.goldennova.upquest.presentation.alarmlist

import com.goldennova.upquest.domain.model.Alarm

data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val isLoading: Boolean = false,
    // 알람 목록 로드 실패 시 에러 화면 표시에 사용
    val errorMessage: String? = null,
)
