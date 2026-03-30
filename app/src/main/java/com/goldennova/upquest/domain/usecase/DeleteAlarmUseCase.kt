package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.domain.repository.AlarmRepository
import java.io.File
import javax.inject.Inject

class DeleteAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        val alarm = repository.getAlarmById(id)

        // 삭제 전 스케줄러 취소
        alarm?.let { alarmScheduler.cancel(it) }

        // PhotoVerification 모드이고 참조 사진 경로가 있으면 파일 삭제
        val photoPath = (alarm?.dismissMode as? DismissMode.PhotoVerification)?.referencePhotoPath
        if (photoPath != null) {
            File(photoPath).delete()
        }

        repository.deleteAlarm(id)
    }
}
