package com.goldennova.upquest.presentation.alarmalert

sealed interface AlarmAlertEvent {
    // 일반 모드 알람 해제
    data object DismissNormal : AlarmAlertEvent
    // 사진 인증 화면으로 이동 요청
    data object TakeVerificationPhoto : AlarmAlertEvent
    // 사진 인증 완료 — 촬영된 이미지 경로 전달
    data class PhotoVerified(val capturedImagePath: String) : AlarmAlertEvent
    // 사진 인증 실패 후 재시도 — 비교 화면을 닫고 카메라 프리뷰로 복귀
    data object RetryPhotoVerification : AlarmAlertEvent
    // 기준 사진 오버레이 투명도 변경
    data class ChangeOverlayAlpha(val alpha: Float) : AlarmAlertEvent
}
