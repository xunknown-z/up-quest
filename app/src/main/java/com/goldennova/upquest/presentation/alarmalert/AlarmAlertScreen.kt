package com.goldennova.upquest.presentation.alarmalert

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.presentation.components.CameraPreview
import java.io.File

@Composable
fun AlarmAlertScreen(
    uiState: AlarmAlertUiState,
    onEvent: (AlarmAlertEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    val alarm = uiState.alarm
    val isPhotoMode = alarm?.dismissMode is DismissMode.PhotoVerification
    val isPhotoModeWithoutReference = isPhotoMode && !uiState.hasReferencePhoto
    // 촬영된 사진이 있으면 비교 화면 표시
    val isComparingPhotos = uiState.capturedImagePath != null

    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 카메라 프리뷰: 사진 모드이고 참조 사진 있고 비교 화면이 아닐 때만 표시
            if (isPhotoMode && uiState.hasReferencePhoto && !isComparingPhotos) {
                CameraPreview(
                    onPhotoTaken = { path -> onEvent(AlarmAlertEvent.PhotoVerified(path)) },
                    onCaptureFunctionReady = { captureAction = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isComparingPhotos) {
                // 비교 화면: 등록된 사진 vs 현재 사진
                val referencePath =
                    (alarm?.dismissMode as? DismissMode.PhotoVerification)?.referencePhotoPath
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AlarmInfoCard(
                        alarm = alarm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PhotoComparisonSection(
                        referencePath = referencePath,
                        capturedPath = uiState.capturedImagePath!!,
                        isVerifying = uiState.isVerifying,
                        verificationFailed = uiState.verificationFailed,
                        onRetry = { onEvent(AlarmAlertEvent.RetryPhotoVerification) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // 기본 화면: 알람 카드 + 액션 버튼
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    AlarmInfoCard(
                        alarm = alarm,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 참조 사진 미등록 경고 메시지
                    if (isPhotoModeWithoutReference) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.alarm_alert_no_reference_photo_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 해제 방식에 따른 액션 버튼
                    if (isPhotoMode && uiState.hasReferencePhoto) {
                        Button(
                            onClick = { captureAction?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.alarm_alert_take_photo))
                        }
                    } else {
                        Button(
                            onClick = { onEvent(AlarmAlertEvent.DismissNormal) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dismiss_normal))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/** 등록된 기준 사진과 촬영된 사진을 나란히 보여주는 비교 섹션 */
@Composable
private fun PhotoComparisonSection(
    referencePath: String?,
    capturedPath: String,
    isVerifying: Boolean,
    verificationFailed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 두 사진 나란히 표시
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ComparisonPhotoCard(
                title = stringResource(R.string.photo_comparison_reference),
                photoPath = referencePath,
                modifier = Modifier.weight(1f),
            )
            ComparisonPhotoCard(
                title = stringResource(R.string.photo_comparison_captured),
                photoPath = capturedPath,
                modifier = Modifier.weight(1f),
            )
        }

        // 분석 상태 표시
        if (isVerifying) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.photo_comparison_analyzing),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (verificationFailed) {
            Text(
                text = stringResource(R.string.photo_comparison_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.photo_comparison_retry))
            }
        }
    }
}

/** 사진 한 장을 제목과 함께 카드로 표시 */
@Composable
private fun ComparisonPhotoCard(
    title: String,
    photoPath: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (photoPath != null) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** InfiniteTransition으로 좌우 흔들림 애니메이션이 적용된 알람 정보 카드 */
@Composable
private fun AlarmInfoCard(
    alarm: Alarm?,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotation",
    )

    Card(
        modifier = modifier.graphicsLayer { rotationZ = rotation },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.alarm_alert_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = alarm?.let { formatTime(it.hour, it.minute) } ?: "--:--",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            if (!alarm?.label.isNullOrBlank()) {
                Text(
                    text = alarm!!.label,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)
