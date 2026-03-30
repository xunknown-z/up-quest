package com.goldennova.upquest.domain.usecase

import android.graphics.Bitmap
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * pHash(지각적 해시, Perceptual Hash) 계산 유틸리티.
 *
 * 이미지를 저주파 DCT 계수 기반의 64비트 Long 해시로 압축한다.
 * 두 해시 간 해밍 거리(비트 차이 수)가 작을수록 두 이미지가 유사함을 의미한다.
 *
 * 알고리즘 절차:
 * 1. 입력 Bitmap을 [DCT_SIZE] × [DCT_SIZE](32×32) 그레이스케일로 리사이즈.
 * 2. 32×32 픽셀 행렬에 2D DCT 적용.
 * 3. 상위 좌측 [HASH_SIZE] × [HASH_SIZE](8×8) 저주파 계수 64개 추출.
 * 4. 64개 계수의 평균값 계산 (DC 성분인 [0][0] 제외).
 * 5. 각 계수 ≥ 평균이면 1, 미만이면 0으로 64비트 Long 해시 생성.
 */
object PHashCalculator {

    // DCT 입력 해상도: 클수록 정밀하지만 연산 비용 증가
    private const val DCT_SIZE = 32

    // 저주파 계수 추출 크기: HASH_SIZE × HASH_SIZE = 64비트 해시
    private const val HASH_SIZE = 8

    // DCT 계수 캐시: 동일한 크기에 대해 반복 계산 방지
    private val cosCache: Array<DoubleArray> by lazy { buildCosCache() }

    /**
     * 주어진 Bitmap의 pHash를 64비트 Long으로 반환한다.
     *
     * @param bitmap 원본 이미지 (크기 무관)
     * @return 64비트 pHash 값
     */
    fun calculate(bitmap: Bitmap): Long {
        // 1. 32×32 그레이스케일 리사이즈
        val resized = Bitmap.createScaledBitmap(bitmap, DCT_SIZE, DCT_SIZE, true)
        val gray = toGrayscaleMatrix(resized)

        // 2. 2D DCT 적용
        val dct = applyDct2D(gray)

        // 3. 상위 좌측 8×8 저주파 계수 추출
        val lowFreq = extractLowFreq(dct)

        // 4. DC 성분([0][0]) 제외한 평균 계산
        val mean = computeMeanExcludingDc(lowFreq)

        // 5. 평균 기준으로 64비트 해시 생성
        return buildHash(lowFreq, mean)
    }

    /**
     * 두 pHash 간 해밍 거리(서로 다른 비트 수)를 반환한다.
     *
     * 0 = 완전 동일, 64 = 완전 상이.
     *
     * @param a 첫 번째 pHash
     * @param b 두 번째 pHash
     * @return 해밍 거리 (0~64)
     */
    fun hammingDistance(a: Long, b: Long): Int = (a xor b).countOneBits()

    // region 내부 구현

    /** Bitmap을 픽셀 휘도(luminance) 기반 2D Double 행렬로 변환한다. */
    private fun toGrayscaleMatrix(bitmap: Bitmap): Array<DoubleArray> {
        return Array(DCT_SIZE) { y ->
            DoubleArray(DCT_SIZE) { x ->
                val pixel = bitmap.getPixel(x, y)
                // ITU-R BT.601 휘도 공식 — Color.* 정적 메서드 대신 직접 비트 연산으로 추출
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                0.299 * r + 0.587 * g + 0.114 * b
            }
        }
    }

    /** 2D DCT-II 변환을 적용한다. 행 방향 → 열 방향 순서로 1D DCT를 두 번 적용한다. */
    private fun applyDct2D(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = DCT_SIZE
        // 행(row) 방향 1D DCT
        val rowDct = Array(n) { y -> applyDct1D(matrix[y]) }
        // 열(column) 방향 1D DCT
        val result = Array(n) { DoubleArray(n) }
        for (x in 0 until n) {
            val col = DoubleArray(n) { y -> rowDct[y][x] }
            val colDct = applyDct1D(col)
            for (y in 0 until n) result[y][x] = colDct[y]
        }
        return result
    }

    /**
     * 1D DCT-II 변환.
     * cosCache를 사용해 반복 cos 계산을 제거한다.
     */
    private fun applyDct1D(signal: DoubleArray): DoubleArray {
        val n = signal.size
        val output = DoubleArray(n)
        val scale = sqrt(2.0 / n)
        for (k in 0 until n) {
            var sum = 0.0
            for (i in 0 until n) sum += signal[i] * cosCache[k][i]
            // k=0 정규화 계수
            val normFactor = if (k == 0) 1.0 / sqrt(2.0) else 1.0
            output[k] = scale * normFactor * sum
        }
        return output
    }

    /** 2D DCT 결과에서 상위 좌측 [HASH_SIZE]×[HASH_SIZE] 계수를 추출한다. */
    private fun extractLowFreq(dct: Array<DoubleArray>): Array<DoubleArray> =
        Array(HASH_SIZE) { y -> DoubleArray(HASH_SIZE) { x -> dct[y][x] } }

    /**
     * DC 성분([0][0])을 제외한 나머지 계수의 평균을 반환한다.
     * DC 성분은 이미지 평균 밝기를 나타내며 해시 안정성에 불리하므로 제외한다.
     */
    private fun computeMeanExcludingDc(lowFreq: Array<DoubleArray>): Double {
        var sum = 0.0
        var count = 0
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                if (y == 0 && x == 0) continue
                sum += lowFreq[y][x]
                count++
            }
        }
        return if (count == 0) 0.0 else sum / count
    }

    /**
     * 각 계수가 평균 이상이면 1, 미만이면 0으로 bit를 설정하여 64비트 Long 해시를 반환한다.
     * 비트 순서: (y=0,x=0)이 최상위 비트(MSB).
     */
    private fun buildHash(lowFreq: Array<DoubleArray>, mean: Double): Long {
        var hash = 0L
        var bit = 63
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                if (lowFreq[y][x] >= mean) hash = hash or (1L shl bit)
                bit--
            }
        }
        return hash
    }

    /**
     * cos(π * k * (2i + 1) / (2N)) 값을 미리 계산하여 캐싱한다.
     * N = [DCT_SIZE].
     */
    private fun buildCosCache(): Array<DoubleArray> {
        val n = DCT_SIZE
        return Array(n) { k ->
            DoubleArray(n) { i ->
                cos(Math.PI * k * (2 * i + 1) / (2.0 * n))
            }
        }
    }

    // endregion
}
