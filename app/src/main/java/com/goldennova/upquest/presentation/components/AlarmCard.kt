package com.goldennova.upquest.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.presentation.theme.UpQuestTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 왼쪽으로 스와이프하면 삭제 버튼이 노출되는 알람 카드.
 *
 * AnchoredDraggableState(@ExperimentalFoundationApi) 대신
 * stable API인 Animatable + Modifier.draggable 로 구현하여
 * interactive preview에서도 동작한다.
 */
@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { 65.dp.toPx() }

    // 카드 수평 오프셋 — 0f(닫힘) ~ -revealWidthPx(열림)
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val clamped = (offsetX.value + delta).coerceIn(-revealWidthPx, 0f)
            offsetX.snapTo(clamped)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        // 카드 뒤에 위치하는 원형 삭제 버튼
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.alarm_delete),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 카드 본문 — 드래그에 따라 좌측으로 이동
        AlarmCardContent(
            alarm = alarm,
            onToggle = onToggle,
            onEdit = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            // 절반 이상 열렸거나 빠르게 스와이프했으면 완전히 열기
                            val shouldExpand =
                                offsetX.value < -revealWidthPx / 2 || velocity < -500f
                            offsetX.animateTo(
                                targetValue = if (shouldExpand) -revealWidthPx else 0f,
                                animationSpec = spring(),
                            )
                        }
                    },
                ),
        )
    }
}

@Composable
private fun AlarmCardContent(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (alarm.isEnabled) 1f else 0.4f

    Card(
        onClick = onEdit,
        modifier = modifier,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = contentAlpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DismissModeIcon(dismissMode = alarm.dismissMode)

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (alarm.repeatDays.isNotEmpty()) {
                        Text(
                            text = formatRepeatDays(alarm.repeatDays),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

@Composable
private fun DismissModeIcon(dismissMode: DismissMode) {
    when (dismissMode) {
        is DismissMode.Normal -> Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = stringResource(R.string.dismiss_normal),
        )
        is DismissMode.PhotoVerification -> Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = stringResource(R.string.dismiss_photo),
        )
    }
}

/** 시간을 "HH:MM" 형식으로 포맷한다. */
private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

/** 반복 요일을 로케일에 맞는 약자로 포맷한다 (예: "Mon Wed Fri" / "월 수 금"). */
private fun formatRepeatDays(days: Set<DayOfWeek>): String =
    days.sortedBy { it.value }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

// region Preview

@Preview(showBackground = true, name = "AlarmCard - 활성화 / 사진 인증")
@Composable
private fun AlarmCardPhotoPreview() {
    // interactive preview에서 스위치 토글이 동작하도록 상태를 로컬로 관리
    var alarm = remember {
        androidx.compose.runtime.mutableStateOf(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 30,
                repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                label = "기상",
                isEnabled = true,
                dismissMode = DismissMode.PhotoVerification("/storage/photo.jpg"),
            )
        )
    }
    UpQuestTheme {
        AlarmCard(
            alarm = alarm.value,
            onToggle = { alarm.value = alarm.value.copy(isEnabled = it) },
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmCard - 비활성화 / 일반 해제")
@Composable
private fun AlarmCardDisabledPreview() {
    var alarm = remember {
        androidx.compose.runtime.mutableStateOf(
            Alarm(
                id = 2L,
                hour = 22,
                minute = 0,
                repeatDays = emptySet(),
                label = "취침 알람",
                isEnabled = false,
                dismissMode = DismissMode.Normal,
            )
        )
    }
    UpQuestTheme {
        AlarmCard(
            alarm = alarm.value,
            onToggle = { alarm.value = alarm.value.copy(isEnabled = it) },
            onEdit = {},
            onDelete = {},
        )
    }
}

// endregion
