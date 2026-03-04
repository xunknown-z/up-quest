package com.goldennova.upquest.presentation.photosetup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun PhotoSetupRoot(
    alarmId: Long,
    onNavigateBack: () -> Unit = {},
    viewModel: PhotoSetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var hasRequestedPermission by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // 화면 진입 및 권한 상태 변화 시 처리
    // PermissionStatus는 sealed(Granted / Denied(shouldShowRationale))이므로 키로 활용 가능
    LaunchedEffect(cameraPermissionState.status) {
        when {
            cameraPermissionState.status.isGranted -> {
                showRationaleDialog = false
                showSettingsDialog = false
            }

            cameraPermissionState.status.shouldShowRationale ->
                showRationaleDialog = true

            !hasRequestedPermission -> {
                // 최초 진입 — 시스템 권한 다이얼로그 표시
                hasRequestedPermission = true
                cameraPermissionState.launchPermissionRequest()
            }

            else ->
                // 영구 거부 (shouldShowRationale=false + 이미 요청) — 설정 안내
                if (!showSettingsDialog) showSettingsDialog = true
        }
    }

    // 앱 설정에서 돌아왔을 때 권한 상태 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow
        .collectAsStateWithLifecycle()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED && hasRequestedPermission &&
            !cameraPermissionState.status.isGranted
        ) {
            if (cameraPermissionState.status.shouldShowRationale) {
                if (!showRationaleDialog) showRationaleDialog = true
            } else {
                if (!showSettingsDialog) showSettingsDialog = true
            }
        }
    }

    // SideEffect 수집 — 촬영 확인 완료 시 뒤로 이동
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is PhotoSetupSideEffect.NavigateBackWithPath -> onNavigateBack()
            }
        }
    }

    // 권한 거부 시 재요청 다이얼로그
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            title = stringResource(R.string.permission_camera_title),
            description = stringResource(R.string.permission_camera_description),
            onAllow = {
                showRationaleDialog = false
                cameraPermissionState.launchPermissionRequest()
            },
            onDismiss = { showRationaleDialog = false },
        )
    }

    // 영구 거부 시 앱 설정 안내 다이얼로그
    if (showSettingsDialog) {
        PermissionSettingsDialog(
            title = stringResource(R.string.permission_camera_title),
            description = stringResource(R.string.permission_camera_description),
            onGoToSettings = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (cameraPermissionState.status.isGranted) {
        // 권한 허용 — 정상 화면
        PhotoSetupScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onNavigateBack = onNavigateBack,
        )
    } else {
        // 권한 미허용 — 카메라 프리뷰 대신 권한 안내 UI
        PhotoSetupPermissionContent(
            onNavigateBack = onNavigateBack,
            onRequestPermission = {
                if (cameraPermissionState.status.shouldShowRationale) {
                    showRationaleDialog = true
                } else {
                    showSettingsDialog = true
                }
            },
        )
    }
}

/** 카메라 권한 미허용 상태에서 표시하는 안내 화면 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSetupPermissionContent(
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.photo_setup_title)) },
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                Text(text = stringResource(R.string.permission_camera_description))
                Button(onClick = onRequestPermission) {
                    Text(text = stringResource(R.string.permission_allow))
                }
            }
        }
    }
}
