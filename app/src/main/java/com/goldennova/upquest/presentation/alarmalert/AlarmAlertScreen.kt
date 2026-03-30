package com.goldennova.upquest.presentation.alarmalert

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

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
                val referencePath =
                    (alarm?.dismissMode as? DismissMode.PhotoVerification)?.referencePhotoPath

                CameraPreview(
                    onPhotoTaken = { path -> onEvent(AlarmAlertEvent.PhotoVerified(path)) },
                    onCaptureFunctionReady = { captureAction = it },
                    modifier = Modifier.fillMaxSize(),
                )

                // 기준 사진 오버레이: 카메라 프리뷰 위에 반투명하게 표시하여 구도 가이드 제공
                if (referencePath != null) {
                    AsyncImage(
                        model = File(referencePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = uiState.overlayAlpha,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            when {
                isComparingPhotos -> {
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
                            capturedPath = uiState.capturedImagePath ?: return@Column,
                            isVerifying = uiState.isVerifying,
                            verificationFailed = uiState.verificationFailed,
                            onRetry = { onEvent(AlarmAlertEvent.RetryPhotoVerification) },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                isPhotoMode && uiState.hasReferencePhoto -> {
                    // 사진 모드 전용 레이아웃 — 5초 후 카드가 좌상단으로 이동
                    PhotoModeLayout(
                        alarm = alarm,
                        captureAction = captureAction,
                    )
                }

                else -> {
                    // 일반 모드 또는 참조 사진 미등록 상태
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

                        if (isPhotoModeWithoutReference) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.alarm_alert_no_reference_photo_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { onEvent(AlarmAlertEvent.DismissNormal) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dismiss_normal))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 사진 인증 모드 전용 레이아웃.
 *
 * 5초 동안은 알람 카드가 화면 중앙에서 흔들리고,
 * 5초 경과 후에는 카드가 좌상단으로 fade되어 작게 표시된다.
 * 촬영 버튼은 항상 하단에 고정된다.
 */
@Composable
private fun PhotoModeLayout(
    alarm: Alarm?,
    captureAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var isCardMinimized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(5_000L)
        isCardMinimized = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        // 카드 영역: 5초 전 중앙 ↔ 5초 후 좌상단
        AnimatedContent(
            targetState = isCardMinimized,
            transitionSpec = {
                fadeIn(tween(800)) togetherWith fadeOut(tween(800))
            },
            modifier = Modifier.fillMaxSize(),
            label = "cardPosition",
        ) { minimized ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (minimized) {
                    // 좌상단 — 시간만 표시하는 작은 카드
                    AlarmInfoCardMini(
                        alarm = alarm,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp),
                    )
                } else {
                    // 중앙 — 기존 전체 크기 카드
                    AlarmInfoCard(
                        alarm = alarm,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        // 촬영 버튼은 항상 하단 고정
        Button(
            onClick = { captureAction?.invoke() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(text = stringResource(R.string.alarm_alert_take_photo))
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

/** 화면 중앙에 크게 표시되는 흔들리는 알람 카드 */
@Composable
private fun AlarmInfoCard(
    alarm: Alarm?,
    modifier: Modifier = Modifier,
) {
    val rotation by shakeRotation()
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
                    text = alarm.label,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** 좌상단에 작게 표시되는 흔들리는 알람 카드 — 시간만 노출 */
@Composable
private fun AlarmInfoCardMini(
    alarm: Alarm?,
    modifier: Modifier = Modifier,
) {
    val rotation by shakeRotation()
    Card(
        modifier = modifier.graphicsLayer { rotationZ = rotation },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.alarm_alert_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = alarm?.let { formatTime(it.hour, it.minute) } ?: "--:--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 좌우 반복 흔들림 회전값을 반환하는 공통 헬퍼 */
@Composable
private fun shakeRotation(): androidx.compose.runtime.State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    return infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotation",
    )
}

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)
