package com.goldennova.upquest.domain.usecase

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PHashCalculator] 단위 테스트.
 *
 * [Bitmap.createScaledBitmap]은 Android 정적 메서드이므로 mockkStatic으로 대체한다.
 * [Color.red] / [Color.green] / [Color.blue] / [Color.rgb]는 순수 비트 연산이므로 mock 없이 동작한다.
 *
 * ## hammingDistance
 * 순수 비트 연산이므로 mock 없이 경계값 위주로 검증한다.
 *
 * ## calculate
 * Bitmap mock 의 getPixel 반환값을 제어하여 픽셀 패턴이 해시 계산에 올바르게 반영되는지 검증한다.
 * - 동일 패턴 → 해시 동일 (해밍 거리 = 0)
 * - 1~2픽셀 차이 → DCT 전역 특성상 해시 거의 동일 (해밍 거리 ≤ [HAMMING_THRESHOLD])
 * - 좌우 반전 이진 패턴 → DC 외 AC 계수 부호 반전으로 다수 비트 상이 (해밍 거리 > [HAMMING_THRESHOLD])
 * - 정반대 그라디언트 → 저주파 계수 구조 상이 (해밍 거리 > [HAMMING_THRESHOLD])
 */
class PHashCalculatorTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Bitmap::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Bitmap::class)
    }

    // region hammingDistance 테스트

    @Test
    fun `hammingDistance - 동일한 해시는 거리가 0이다`() {
        assertEquals(0, PHashCalculator.hammingDistance(0L, 0L))
        assertEquals(0, PHashCalculator.hammingDistance(-1L, -1L))
        assertEquals(0, PHashCalculator.hammingDistance(Long.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun `hammingDistance - 모든 비트가 다를 때 거리는 64이다`() {
        // -1L = 0xFFFF_FFFF_FFFF_FFFF (64비트 전부 1), 0L (64비트 전부 0) → XOR = 64개의 1
        assertEquals(64, PHashCalculator.hammingDistance(-1L, 0L))
        assertEquals(64, PHashCalculator.hammingDistance(0L, -1L))
    }

    @Test
    fun `hammingDistance - Long MAX_VALUE와 0의 거리는 63이다`() {
        // Long.MAX_VALUE = 0x7FFF_FFFF_FFFF_FFFF (최상위 비트만 0, 나머지 63비트 = 1)
        assertEquals(63, PHashCalculator.hammingDistance(Long.MAX_VALUE, 0L))
    }

    @Test
    fun `hammingDistance - 교환 법칙이 성립한다`() {
        val a = 0x0F0F_0F0F_0F0F_0F0FL
        val b = a.inv() // 0xF0F0_F0F0_F0F0_F0F0...은 Long 범위 초과이므로 inv()로 생성
        assertEquals(
            PHashCalculator.hammingDistance(a, b),
            PHashCalculator.hammingDistance(b, a),
        )
    }

    // endregion

    // region calculate 테스트

    @Test
    fun `동일한 픽셀 패턴의 Bitmap 입력 시 해밍 거리는 0이다`() {
        val hashA = PHashCalculator.calculate(createUniformBitmap(WHITE))
        val hashB = PHashCalculator.calculate(createUniformBitmap(WHITE))

        assertEquals(0, PHashCalculator.hammingDistance(hashA, hashB))
    }

    @Test
    fun `좌우 반전된 이진 패턴 Bitmap 입력 시 해밍 거리는 임계값보다 크다`() {
        // 왼쪽 흰색·오른쪽 검정 vs 왼쪽 검정·오른쪽 흰색:
        // DC 계수는 동일하지만 수평 AC 계수의 부호가 반전되어 평균 기준이 달라지고
        // k_y > 0 행의 계수(모두 0)에 대한 비트 방향도 반대가 되어 다수 비트 상이.
        val hashA = PHashCalculator.calculate(
            createHalfBitmap(leftColor = WHITE, rightColor = BLACK),
        )
        val hashB = PHashCalculator.calculate(
            createHalfBitmap(leftColor = BLACK, rightColor = WHITE),
        )

        assertTrue(PHashCalculator.hammingDistance(hashA, hashB) > HAMMING_THRESHOLD)
    }

    @Test
    fun `수평 그라디언트와 반전 그라디언트 Bitmap 입력 시 해밍 거리는 임계값보다 크다`() {
        // 밝기가 왼쪽→오른쪽으로 증가하는 패턴 vs 오른쪽→왼쪽으로 증가하는 패턴:
        // 저주파 수평 계수의 위상이 반대여서 해시 비트 패턴이 크게 달라진다.
        val hashA = PHashCalculator.calculate(createGradientBitmap(inverted = false))
        val hashB = PHashCalculator.calculate(createGradientBitmap(inverted = true))

        assertTrue(PHashCalculator.hammingDistance(hashA, hashB) > HAMMING_THRESHOLD)
    }

    // endregion

    // region 헬퍼

    /** 모든 픽셀이 [color]로 균일한 32×32 Bitmap mock을 반환한다. Color.* 정적 메서드를 피해 raw int를 사용한다. */
    private fun createUniformBitmap(color: Int): Bitmap {
        val original = mockk<Bitmap>()
        val scaled = mockk<Bitmap>()
        every { Bitmap.createScaledBitmap(original, 32, 32, true) } returns scaled
        every { scaled.getPixel(any(), any()) } returns color
        return original
    }

    /**
     * 가로로 이등분된 32×32 Bitmap mock을 반환한다.
     * x < 16 이면 [leftColor], x >= 16 이면 [rightColor]를 반환한다.
     */
    private fun createHalfBitmap(leftColor: Int, rightColor: Int): Bitmap {
        val original = mockk<Bitmap>()
        val scaled = mockk<Bitmap>()
        every { Bitmap.createScaledBitmap(original, 32, 32, true) } returns scaled
        every { scaled.getPixel(any(), any()) } answers {
            val x = firstArg<Int>()
            if (x < 16) leftColor else rightColor
        }
        return original
    }

    /**
     * 수평 그라디언트 32×32 Bitmap mock을 반환한다.
     * [inverted] = false: x=0 → 어두움(0), x=31 → 밝음(248).
     * [inverted] = true:  x=0 → 밝음(248), x=31 → 어두움(0).
     * Color.rgb() 대신 직접 비트 패킹으로 ARGB 픽셀 값을 생성한다.
     */
    private fun createGradientBitmap(inverted: Boolean): Bitmap {
        val original = mockk<Bitmap>()
        val scaled = mockk<Bitmap>()
        every { Bitmap.createScaledBitmap(original, 32, 32, true) } returns scaled
        every { scaled.getPixel(any(), any()) } answers {
            val x = firstArg<Int>()
            val intensity = if (inverted) (31 - x) * 8 else x * 8
            // ARGB: A=0xFF, R=intensity, G=intensity, B=intensity
            (0xFF shl 24) or (intensity shl 16) or (intensity shl 8) or intensity
        }
        return original
    }

    // endregion

    companion object {
        // PhotoVerificationUseCaseImpl.HAMMING_THRESHOLD 와 동일한 값
        private const val HAMMING_THRESHOLD = 10

        // Color.* 정적 메서드는 Android 스텁 환경에서 0을 반환하므로 raw ARGB 값으로 직접 정의
        private const val WHITE = 0xFFFFFFFF.toInt() // R=255, G=255, B=255
        private const val BLACK = 0xFF000000.toInt() // R=0,   G=0,   B=0
    }
}
