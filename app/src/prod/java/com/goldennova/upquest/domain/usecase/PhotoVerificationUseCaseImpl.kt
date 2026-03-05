package com.goldennova.upquest.domain.usecase

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * prod 빌드 전용 ML Kit ImageLabeling 기반 사진 인증 구현체.
 *
 * 두 이미지에서 각각 레이블을 추출한 뒤,
 * 공통 레이블 수 / 전체 레이블 수(합집합)로 Jaccard 유사도를 계산한다.
 * 유사도가 [SIMILARITY_THRESHOLD] 이상이면 동일 피사체로 판단하여 true를 반환한다.
 */
class PhotoVerificationUseCaseImpl @Inject constructor() : PhotoVerificationUseCase {

    companion object {
        // 레이블 신뢰도 최소 기준 (0.0 ~ 1.0)
        private const val CONFIDENCE_THRESHOLD = 0.7f
        // 유사 피사체 판단 Jaccard 유사도 임계값
        private const val SIMILARITY_THRESHOLD = 0.3f
    }

    override suspend fun verify(capturedPath: String, referencePath: String): Boolean {
        val capturedLabels = extractLabels(capturedPath)
        val referenceLabels = extractLabels(referencePath)

        if (capturedLabels.isEmpty() || referenceLabels.isEmpty()) return false

        // Jaccard 유사도: 교집합 / 합집합
        val intersection = capturedLabels.intersect(referenceLabels).size
        val union = capturedLabels.union(referenceLabels).size

        return (intersection.toFloat() / union) >= SIMILARITY_THRESHOLD
    }

    /** 이미지 경로에서 ML Kit ImageLabeler로 레이블 Set을 추출한다. */
    private suspend fun extractLabels(imagePath: String): Set<String> {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return emptySet()
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                .build(),
        )

        return suspendCoroutine { continuation ->
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    continuation.resume(labels.map { it.text }.toSet())
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }
}
