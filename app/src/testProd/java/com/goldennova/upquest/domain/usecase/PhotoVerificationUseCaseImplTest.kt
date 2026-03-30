package com.goldennova.upquest.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PhotoVerificationUseCaseImpl] 단위 테스트.
 *
 * [PHashCalculator]를 MockK로 대체하여 해밍 거리 반환값을 직접 제어한다.
 * [BitmapFactory.decodeFile]은 정적 메서드이므로 mockkStatic으로 대체한다.
 */
class PhotoVerificationUseCaseImplTest {

    private lateinit var useCase: PhotoVerificationUseCaseImpl

    @BeforeEach
    fun setUp() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)
        mockkObject(PHashCalculator)
        useCase = PhotoVerificationUseCaseImpl()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(BitmapFactory::class)
        unmockkStatic(Bitmap::class)
        unmockkObject(PHashCalculator)
    }

    @Test
    fun `해밍 거리가 임계값 이하이면 true를 반환한다`() = runTest {
        stubBitmaps()
        stubHammingDistance(PhotoVerificationUseCaseImpl.HAMMING_THRESHOLD)

        assertTrue(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `해밍 거리가 0이면 true를 반환한다`() = runTest {
        stubBitmaps()
        stubHammingDistance(0)

        assertTrue(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `해밍 거리가 임계값과 동일하면 true를 반환한다`() = runTest {
        stubBitmaps()
        stubHammingDistance(PhotoVerificationUseCaseImpl.HAMMING_THRESHOLD)

        assertTrue(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `해밍 거리가 임계값보다 크면 false를 반환한다`() = runTest {
        stubBitmaps()
        stubHammingDistance(PhotoVerificationUseCaseImpl.HAMMING_THRESHOLD + 1)

        assertFalse(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `캡처 이미지 파일이 없으면 false를 반환한다`() = runTest {
        every { BitmapFactory.decodeFile("/captured.jpg") } returns null
        every { BitmapFactory.decodeFile("/reference.jpg") } returns mockk()

        assertFalse(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `기준 이미지 파일이 없으면 false를 반환한다`() = runTest {
        every { BitmapFactory.decodeFile("/captured.jpg") } returns mockk()
        every { BitmapFactory.decodeFile("/reference.jpg") } returns null

        assertFalse(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    @Test
    fun `두 이미지 파일이 모두 없으면 false를 반환한다`() = runTest {
        every { BitmapFactory.decodeFile(any()) } returns null

        assertFalse(useCase.verify("/captured.jpg", "/reference.jpg"))
    }

    // region 헬퍼

    /** BitmapFactory.decodeFile이 유효한 Bitmap mock을 반환하도록 stub한다. */
    private fun stubBitmaps() {
        val capturedBitmap = mockk<Bitmap>()
        val referenceBitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeFile("/captured.jpg") } returns capturedBitmap
        every { BitmapFactory.decodeFile("/reference.jpg") } returns referenceBitmap
        every { PHashCalculator.calculate(capturedBitmap) } returns 0L
        every { PHashCalculator.calculate(referenceBitmap) } returns 0L
    }

    /** PHashCalculator.hammingDistance가 [distance]를 반환하도록 stub한다. */
    private fun stubHammingDistance(distance: Int) {
        every { PHashCalculator.hammingDistance(any(), any()) } returns distance
    }

    // endregion
}
