package com.goldennova.upquest.presentation.alarmlist

sealed interface AlarmListSideEffect {
    data class NavigateToDetail(val alarmId: Long) : AlarmListSideEffect
    data object NavigateToNewAlarm : AlarmListSideEffect
    // 토글/삭제 등 사용자 액션 실패 시 스낵바로 표시
    data class ShowError(val message: String) : AlarmListSideEffect
}
