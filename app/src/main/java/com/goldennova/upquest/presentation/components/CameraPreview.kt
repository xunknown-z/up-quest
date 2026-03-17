package com.goldennova.upquest.presentation.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * CameraX PreviewView를 AndroidView로 래핑한 공용 컴포저블.
 *
 * @param onPhotoTaken 촬영 완료 후 저장된 이미지의 절대 경로를 전달하는 콜백
 * @param onCaptureFunctionReady 카메라 바인딩 완료 시 촬영 트리거 함수를 외부로 노출하는 콜백
 * @param modifier Modifier
 */
@Composable
fun CameraPreview(
    onPhotoTaken: (imagePath: String) -> Unit,
    onCaptureFunctionReady: (capturePhoto: () -> Unit) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ImageCapture UseCase — 컴포저블 생명주기 동안 동일 인스턴스를 유지
    val imageCapture = remember { ImageCapture.Builder().build() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )

                    // 카메라 바인딩 완료 — 촬영 트리거 함수를 부모에게 전달
                    onCaptureFunctionReady {
                        val photoDir = File(ctx.filesDir, "photos").also { it.mkdirs() }
                        val photoFile = File(photoDir, "capture_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions
                            .Builder(photoFile)
                            .build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(ctx),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onPhotoTaken(photoFile.absolutePath)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    // 촬영 오류 — 콜백을 호출하지 않아 UiState는 변경되지 않는다
                                }
                            },
                        )
                    }
                } catch (exc: Exception) {
                    // 카메라 바인딩 실패 — 권한 거부 또는 기기 미지원 시 발생 가능
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier,
    )
}
