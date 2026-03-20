package com.goldennova.upquest.presentation.alarmdetail

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.os.PowerManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldennova.upquest.R
import com.goldennova.upquest.presentation.components.PermissionRationaleDialog
import com.goldennova.upquest.presentation.components.PermissionSettingsDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AlarmDetailRoot(
    alarmId: Long = -1L,
    onNavigateBack: () -> Unit = {},
    onNavigateToPhotoSetup: (Long) -> Unit = {},
    viewModel: AlarmDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 링톤 선택 결과 처리 — 선택된 URI를 ChangeRingtone 이벤트로 전달
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pickedUri = result.data?.let { data ->
            IntentCompat.getParcelableExtra(
                data,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java,
            )
        }
        viewModel.onEvent(AlarmDetailEvent.ChangeRingtone(pickedUri?.toString()))
    }

    // SideEffect 수집 — 내비게이션 및 스낵바 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                AlarmDetailSideEffect.NavigateBack -> onNavigateBack()
                is AlarmDetailSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // ① POST_NOTIFICATIONS 권한 처리 (API 33+, 일반 런타임 권한)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(notificationPermissionState?.status) {
        val pState = notificationPermissionState ?: return@LaunchedEffect
        when {
            pState.status.isGranted -> {
                showNotificationRationaleDialog = false
                showNotificationSettingsDialog = false
            }

            pState.status.shouldShowRationale -> showNotificationRationaleDialog = true
            !hasRequestedNotificationPermission -> {
                hasRequestedNotificationPermission = true
                pState.launchPermissionRequest()
            }

            else -> if (!showNotificationSettingsDialog) showNotificationSettingsDialog = true
        }
    }

    // ② SCHEDULE_EXACT_ALARM 권한 처리 (API 31+, 특수 권한)
    val alarmManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember { context.getSystemService(AlarmManager::class.java) }
    } else null

    var showExactAlarmSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == false) {
                showExactAlarmSettingsDialog = true
            }
        }
    }

    // ③ 배터리 최적화 예외 처리 — 도즈 모드에서 알람 신뢰성 보장
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationDialog = true
        }
    }

    // ④ USE_FULL_SCREEN_INTENT 권한 처리 (API 34+, 특수 권한)
    // 미허용 시 잠금 화면에서 알람 화면이 표시되지 않으므로 설정으로 안내한다
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }

    var showFullScreenIntentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!notificationManager.canUseFullScreenIntent()) {
                showFullScreenIntentDialog = true
            }
        }
    }

    // 앱 설정에서 돌아왔을 때 권한 상태 재확인 (RESUMED)
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow
        .collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect

        // POST_NOTIFICATIONS 재확인
        notificationPermissionState?.let { pState ->
            if (!pState.status.isGranted && hasRequestedNotificationPermission) {
                if (pState.status.shouldShowRationale) {
                    if (!showNotificationRationaleDialog) showNotificationRationaleDialog = true
                } else {
                    if (!showNotificationSettingsDialog) showNotificationSettingsDialog = true
                }
            }
        }

        // SCHEDULE_EXACT_ALARM 재확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == false && !showExactAlarmSettingsDialog) {
                showExactAlarmSettingsDialog = true
            }
        }

        // 배터리 최적화 재확인 — 허용됐으면 다이얼로그 닫힘
        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationDialog = false
        } else if (!showBatteryOptimizationDialog) {
            showBatteryOptimizationDialog = true
        }

        // USE_FULL_SCREEN_INTENT 재확인 — 허용됐으면 다이얼로그 닫힘
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (notificationManager.canUseFullScreenIntent()) {
                showFullScreenIntentDialog = false
            } else if (!showFullScreenIntentDialog) {
                showFullScreenIntentDialog = true
            }
        }
    }

    // POST_NOTIFICATIONS — 권한 재요청 다이얼로그
    if (showNotificationRationaleDialog) {
        PermissionRationaleDialog(
            title = context.getString(R.string.permission_notification_title),
            description = context.getString(R.string.permission_notification_description),
            onAllow = {
                showNotificationRationaleDialog = false
                notificationPermissionState?.launchPermissionRequest()
            },
            onDismiss = { showNotificationRationaleDialog = false },
        )
    }

    // POST_NOTIFICATIONS — 영구 거부 시 앱 설정 안내 다이얼로그
    if (showNotificationSettingsDialog) {
        PermissionSettingsDialog(
            title = context.getString(R.string.permission_notification_title),
            description = context.getString(R.string.permission_notification_description),
            onGoToSettings = {
                showNotificationSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showNotificationSettingsDialog = false },
        )
    }

    // USE_FULL_SCREEN_INTENT — 시스템 설정 안내 다이얼로그 (API 34+)
    if (showFullScreenIntentDialog) {
        PermissionSettingsDialog(
            title = context.getString(R.string.permission_full_screen_intent_title),
            description = context.getString(R.string.permission_full_screen_intent_description),
            onGoToSettings = {
                showFullScreenIntentDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
            onDismiss = { showFullScreenIntentDialog = false },
        )
    }

    // 배터리 최적화 — 시스템 설정 안내 다이얼로그
    if (showBatteryOptimizationDialog) {
        PermissionSettingsDialog(
            title = context.getString(R.string.permission_battery_optimization_title),
            description = context.getString(R.string.permission_battery_optimization_description),
            onGoToSettings = {
                showBatteryOptimizationDialog = false
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showBatteryOptimizationDialog = false },
        )
    }

    // SCHEDULE_EXACT_ALARM — 시스템 설정 안내 다이얼로그
    if (showExactAlarmSettingsDialog) {
        PermissionSettingsDialog(
            title = context.getString(R.string.permission_exact_alarm_title),
            description = context.getString(R.string.permission_exact_alarm_description),
            onGoToSettings = {
                showExactAlarmSettingsDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
            onDismiss = { showExactAlarmSettingsDialog = false },
        )
    }

    AlarmDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        isNewAlarm = alarmId == -1L,
        onNavigateBack = onNavigateBack,
        onNavigateToPhotoSetup = { onNavigateToPhotoSetup(alarmId) },
        onPickRingtone = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                // 현재 선택된 URI를 picker 초기값으로 설정
                uiState.ringtoneUri?.let {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                }
            }
            ringtoneLauncher.launch(intent)
        },
        snackbarHostState = snackbarHostState,
    )
}
