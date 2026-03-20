package com.goldennova.upquest.presentation.alarmalert

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode

data class AlarmAlertUiState(
    val alarm: Alarm? = null,
    val isDismissed: Boolean = false,
    val isPhotoVerified: Boolean = false,
) {
    /** PhotoVerification 모드이고 참조 사진 경로가 설정되어 있으면 true */
    val hasReferencePhoto: Boolean
        get() = (alarm?.dismissMode as? DismissMode.PhotoVerification)?.referencePhotoPath != null
}
