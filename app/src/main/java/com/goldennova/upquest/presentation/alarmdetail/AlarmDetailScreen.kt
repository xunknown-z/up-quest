package com.goldennova.upquest.presentation.alarmdetail

import android.media.RingtoneManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.AlarmSoundMode
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.presentation.theme.UpQuestTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    uiState: AlarmDetailUiState,
    onEvent: (AlarmDetailEvent) -> Unit,
    isNewAlarm: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToPhotoSetup: () -> Unit,
    onPickRingtone: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val title = stringResource(
        if (isNewAlarm) R.string.alarm_detail_title_new else R.string.alarm_detail_title_edit
    )
    val defaultLabel = stringResource(R.string.alarm_label_hint)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AlarmDetailBottomBar(
                isNewAlarm = isNewAlarm,
                isLoading = uiState.isLoading,
                onSave = { onEvent(AlarmDetailEvent.Save(defaultLabel)) },
                onDelete = { onEvent(AlarmDetailEvent.Delete) },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            AlarmDetailForm(
                uiState = uiState,
                onEvent = onEvent,
                onNavigateToPhotoSetup = onNavigateToPhotoSetup,
                onPickRingtone = onPickRingtone,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmDetailForm(
    uiState: AlarmDetailUiState,
    onEvent: (AlarmDetailEvent) -> Unit,
    onNavigateToPhotoSetup: () -> Unit,
    onPickRingtone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.hour,
        initialMinute = uiState.minute,
        is24Hour = true,
    )
    // TimePicker 상태 변경 시 ViewModel에 이벤트 전달
    LaunchedEffect(timePickerState.hour) {
        onEvent(AlarmDetailEvent.ChangeHour(timePickerState.hour))
    }
    LaunchedEffect(timePickerState.minute) {
        onEvent(AlarmDetailEvent.ChangeMinute(timePickerState.minute))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TimePicker(state = timePickerState)

        OutlinedTextField(
            value = uiState.label,
            onValueChange = { onEvent(AlarmDetailEvent.ChangeLabel(it)) },
            label = { Text(stringResource(R.string.alarm_label_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        RepeatDaysRow(
            selectedDays = uiState.repeatDays,
            onToggleDay = { onEvent(AlarmDetailEvent.ToggleDay(it)) },
        )

        DismissModeSection(
            dismissMode = uiState.dismissMode,
            onChangeDismissMode = { onEvent(AlarmDetailEvent.ChangeDismissMode(it)) },
            onNavigateToPhotoSetup = onNavigateToPhotoSetup,
        )

        SoundModeSection(
            soundMode = uiState.soundMode,
            onChangeSoundMode = { onEvent(AlarmDetailEvent.ChangeSoundMode(it)) },
        )

        // 진동만 모드에서는 알람음 선택 불필요
        if (uiState.soundMode == AlarmSoundMode.SOUND_AND_VIBRATION) {
            RingtoneRow(
                ringtoneUri = uiState.ringtoneUri,
                onPickRingtone = onPickRingtone,
            )
        }
    }
}

@Composable
private fun RepeatDaysRow(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.repeat_days_label),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayOfWeek.values().forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = { onToggleDay(day) },
                    label = {
                        Text(text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DismissModeSection(
    dismissMode: DismissMode,
    onChangeDismissMode: (DismissMode) -> Unit,
    onNavigateToPhotoSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // selectableGroup으로 RadioButton 그룹의 접근성 시맨틱을 구성한다
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.dismiss_mode_label),
            style = MaterialTheme.typography.titleSmall,
        )
        // Row 전체를 클릭 영역으로 지정 — M3 권장 RadioButton 패턴
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = dismissMode is DismissMode.Normal,
                    onClick = { onChangeDismissMode(DismissMode.Normal) },
                    role = Role.RadioButton,
                ),
        ) {
            RadioButton(
                selected = dismissMode is DismissMode.Normal,
                onClick = null,
            )
            Text(
                text = stringResource(R.string.dismiss_normal),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = dismissMode is DismissMode.PhotoVerification,
                    onClick = { onChangeDismissMode(DismissMode.PhotoVerification(null)) },
                    role = Role.RadioButton,
                ),
        ) {
            RadioButton(
                selected = dismissMode is DismissMode.PhotoVerification,
                onClick = null,
            )
            Text(
                text = stringResource(R.string.dismiss_photo),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (dismissMode is DismissMode.PhotoVerification) {
            Button(
                onClick = onNavigateToPhotoSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(text = stringResource(R.string.photo_setup_title))
            }
        }
    }
}

@Composable
private fun SoundModeSection(
    soundMode: AlarmSoundMode,
    onChangeSoundMode: (AlarmSoundMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.sound_mode_label),
            style = MaterialTheme.typography.titleSmall,
        )
        AlarmSoundMode.entries.forEach { mode ->
            val label = stringResource(
                when (mode) {
                    AlarmSoundMode.SOUND_AND_VIBRATION -> R.string.sound_mode_sound_and_vibration
                    AlarmSoundMode.VIBRATION_ONLY -> R.string.sound_mode_vibration_only
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = soundMode == mode,
                        onClick = { onChangeSoundMode(mode) },
                        role = Role.RadioButton,
                    ),
            ) {
                RadioButton(
                    selected = soundMode == mode,
                    onClick = null,
                )
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RingtoneRow(
    ringtoneUri: String?,
    onPickRingtone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // URI가 변경될 때마다 링톤 표시 이름을 조회
    val ringtoneName = remember(ringtoneUri) {
        ringtoneUri?.let { uriString ->
            RingtoneManager.getRingtone(context, uriString.toUri())?.getTitle(context)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.ringtone_label),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                // URI가 null이거나 이름 조회 실패 시 "기본 알람음" 표시
                text = ringtoneName ?: stringResource(R.string.ringtone_default),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onPickRingtone) {
            Text(text = stringResource(R.string.ringtone_change))
        }
    }
}

@Composable
private fun AlarmDetailBottomBar(
    isNewAlarm: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isNewAlarm) {
            OutlinedButton(
                onClick = onDelete,
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.alarm_delete))
            }
        }
        Button(
            onClick = onSave,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.alarm_save))
        }
    }
}

// region Preview

@Preview(showBackground = true, name = "AlarmDetailScreen - 신규 생성")
@Composable
private fun AlarmDetailScreenNewPreview() {
    UpQuestTheme {
        AlarmDetailScreen(
            uiState = AlarmDetailUiState(),
            onEvent = {},
            isNewAlarm = true,
            onNavigateBack = {},
            onNavigateToPhotoSetup = {},
            onPickRingtone = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmDetailScreen - 수정 (사진 인증 모드)")
@Composable
private fun AlarmDetailScreenEditPreview() {
    UpQuestTheme {
        AlarmDetailScreen(
            uiState = AlarmDetailUiState(
                hour = 8,
                minute = 30,
                repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                label = "기상 알람",
                dismissMode = DismissMode.PhotoVerification("/storage/photo.jpg"),
            ),
            onEvent = {},
            isNewAlarm = false,
            onNavigateBack = {},
            onNavigateToPhotoSetup = {},
            onPickRingtone = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmDetailScreen - 로딩")
@Composable
private fun AlarmDetailScreenLoadingPreview() {
    UpQuestTheme {
        AlarmDetailScreen(
            uiState = AlarmDetailUiState(isLoading = true),
            onEvent = {},
            isNewAlarm = false,
            onNavigateBack = {},
            onNavigateToPhotoSetup = {},
            onPickRingtone = {},
        )
    }
}

// endregion
