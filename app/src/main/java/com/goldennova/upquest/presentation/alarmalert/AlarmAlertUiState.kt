package com.goldennova.upquest.presentation.alarmalert

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode

data class AlarmAlertUiState(
    val alarm: Alarm? = null,
    val isDismissed: Boolean = false,
    val isPhotoVerified: Boolean = false,
    /** 촬영된 사진 경로 — null이면 카메라 프리뷰 표시, non-null이면 비교 화면 표시 */
    val capturedImagePath: String? = null,
    /** 사진 비교 분석 진행 중 여부 */
    val isVerifying: Boolean = false,
    /** 사진 인증 실패 여부 */
    val verificationFailed: Boolean = false,
) {
    /** PhotoVerification 모드이고 참조 사진 경로가 설정되어 있으면 true */
    val hasReferencePhoto: Boolean
        get() = (alarm?.dismissMode as? DismissMode.PhotoVerification)?.referencePhotoPath != null
}
