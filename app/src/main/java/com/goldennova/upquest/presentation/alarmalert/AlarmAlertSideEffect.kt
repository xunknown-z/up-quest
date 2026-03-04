package com.goldennova.upquest.presentation.alarmalert

sealed interface AlarmAlertSideEffect {
    // 알람 해제 완료 — 화면 종료 신호
    data object DismissAlarm : AlarmAlertSideEffect
    // 오류 발생 — 사용자에게 메시지 노출
    data class ShowError(val message: String) : AlarmAlertSideEffect
}
