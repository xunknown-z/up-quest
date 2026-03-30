package com.goldennova.upquest.domain.usecase

import android.graphics.BitmapFactory
import javax.inject.Inject

/**
 * prod 빌드 전용 pHash(지각적 해시) 기반 사진 인증 구현체.
 *
 * 두 이미지에서 각각 64비트 pHash를 계산한 뒤 해밍 거리를 구한다.
 * 해밍 거리가 [HAMMING_THRESHOLD] 이하이면 동일 피사체로 판단하여 true를 반환한다.
 *
 * 기존 ML Kit Jaccard 유사도 방식 대비 장점:
 * - 외부 라이브러리 의존성 없음.
 * - 동일 피사체를 다른 조명·각도로 찍어도 안정적으로 유사 판정.
 */
class PhotoVerificationUseCaseImpl @Inject constructor() : PhotoVerificationUseCase {

    companion object {
        // 해밍 거리 허용 상한 (0 = 완전 동일, 64 = 완전 상이)
        // 10 이하 = 동일 피사체로 판단 (조명·각도 차이 허용)
        const val HAMMING_THRESHOLD = 10
    }

    override suspend fun verify(capturedPath: String, referencePath: String): Boolean {
        val captured = BitmapFactory.decodeFile(capturedPath) ?: return false
        val reference = BitmapFactory.decodeFile(referencePath) ?: return false

        val capturedHash = PHashCalculator.calculate(captured)
        val referenceHash = PHashCalculator.calculate(reference)

        return PHashCalculator.hammingDistance(capturedHash, referenceHash) <= HAMMING_THRESHOLD
    }
}
