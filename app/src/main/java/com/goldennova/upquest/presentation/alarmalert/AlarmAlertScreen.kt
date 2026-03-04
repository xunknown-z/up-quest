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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.presentation.components.CameraPreview

@Composable
fun AlarmAlertScreen(
    uiState: AlarmAlertUiState,
    onEvent: (AlarmAlertEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alarm = uiState.alarm
    val isPhotoMode = alarm?.dismissMode is DismissMode.PhotoVerification

    // CameraX 바인딩 완료 후 전달받은 촬영 트리거 함수
    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // PhotoVerification 모드: 전체 배경을 카메라 프리뷰로 채운다
        if (isPhotoMode) {
            CameraPreview(
                onPhotoTaken = { path -> onEvent(AlarmAlertEvent.PhotoVerified(path)) },
                onCaptureFunctionReady = { captureAction = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 흔들림 애니메이션이 적용된 알람 정보 카드
            AlarmInfoCard(
                alarm = alarm,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))

            // 해제 방식에 따른 액션 버튼
            if (isPhotoMode) {
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
