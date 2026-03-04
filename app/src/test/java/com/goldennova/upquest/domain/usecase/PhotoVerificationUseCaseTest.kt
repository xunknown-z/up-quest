package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.usecase.PhotoVerificationUseCaseImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [PhotoVerificationUseCaseImpl] (dev 구현체) 단위 테스트.
 *
 * dev 구현체는 항상 `true`를 반환하는 Mock이므로,
 * 어떤 경로 입력에도 인증 성공을 반환하는지 검증한다.
 */
class PhotoVerificationUseCaseTest {

    private lateinit var useCase: PhotoVerificationUseCase

    @BeforeEach
    fun setUp() {
        useCase = PhotoVerificationUseCaseImpl()
    }

    @Test
    fun `동일한 경로를 전달하면 true를 반환한다`() = runTest {
        val result = useCase.verify(
            capturedPath = "/storage/photo.jpg",
            referencePath = "/storage/photo.jpg",
        )

        assertTrue(result)
    }

    @Test
    fun `서로 다른 경로를 전달해도 true를 반환한다`() = runTest {
        val result = useCase.verify(
            capturedPath = "/storage/captured.jpg",
            referencePath = "/storage/reference.jpg",
        )

        assertTrue(result)
    }

    @Test
    fun `빈 경로를 전달해도 true를 반환한다`() = runTest {
        val result = useCase.verify(
            capturedPath = "",
            referencePath = "",
        )

        assertTrue(result)
    }
}
