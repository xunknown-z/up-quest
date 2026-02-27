package com.goldennova.upquest.presentation.alarmlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.goldennova.upquest.R
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.model.DismissMode
import com.goldennova.upquest.presentation.components.AlarmCard
import com.goldennova.upquest.presentation.theme.UpQuestTheme
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    uiState: AlarmListUiState,
    onEvent: (AlarmListEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.alarm_list_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(AlarmListEvent.AddAlarm) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_alarm),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        AlarmListContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AlarmListContent(
    uiState: AlarmListUiState,
    onEvent: (AlarmListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            uiState.alarms.isEmpty() -> {
                Text(
                    text = stringResource(R.string.alarm_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            else -> {
                AlarmList(alarms = uiState.alarms, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onEvent: (AlarmListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onToggle = { enabled ->
                    onEvent(AlarmListEvent.ToggleAlarm(id = alarm.id, enabled = enabled))
                },
                onEdit = { onEvent(AlarmListEvent.EditAlarm(id = alarm.id)) },
                onDelete = { onEvent(AlarmListEvent.DeleteAlarm(id = alarm.id)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// region Preview

@Preview(showBackground = true, name = "AlarmListScreen - 알람 목록")
@Composable
private fun AlarmListScreenPreview() {
    UpQuestTheme {
        AlarmListScreen(
            uiState = AlarmListUiState(
                alarms = listOf(
                    Alarm(
                        id = 1L,
                        hour = 7,
                        minute = 30,
                        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        label = "기상",
                        isEnabled = true,
                        dismissMode = DismissMode.PhotoVerification("/storage/photo.jpg"),
                    ),
                    Alarm(
                        id = 2L,
                        hour = 22,
                        minute = 0,
                        repeatDays = emptySet(),
                        label = "취침 알람",
                        isEnabled = false,
                        dismissMode = DismissMode.Normal,
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmListScreen - 빈 상태")
@Composable
private fun AlarmListScreenEmptyPreview() {
    UpQuestTheme {
        AlarmListScreen(
            uiState = AlarmListUiState(alarms = emptyList()),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmListScreen - 로딩")
@Composable
private fun AlarmListScreenLoadingPreview() {
    UpQuestTheme {
        AlarmListScreen(
            uiState = AlarmListUiState(isLoading = true),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, name = "AlarmListScreen - 에러")
@Composable
private fun AlarmListScreenErrorPreview() {
    UpQuestTheme {
        AlarmListScreen(
            uiState = AlarmListUiState(errorMessage = "DB 연결 오류"),
            onEvent = {},
        )
    }
}

// endregion
