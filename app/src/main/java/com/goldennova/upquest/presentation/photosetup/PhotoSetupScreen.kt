package com.goldennova.upquest.presentation.photosetup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.goldennova.upquest.R
import com.goldennova.upquest.presentation.components.CameraPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSetupScreen(
    uiState: PhotoSetupUiState,
    onEvent: (PhotoSetupEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // CameraX 바인딩 완료 후 전달받은 촬영 트리거 함수
    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
        bottomBar = {
            PhotoSetupBottomBar(
                isPhotoTaken = uiState.isPhotoTaken,
                onTakePhoto = { captureAction?.invoke() },
                onRetake = { onEvent(PhotoSetupEvent.RetakePhoto) },
                onConfirm = { onEvent(PhotoSetupEvent.Confirm) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isPhotoTaken && uiState.capturedImagePath != null) {
                // 촬영 완료 — Coil로 캡처 이미지 미리보기
                AsyncImage(
                    model = uiState.capturedImagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // 촬영 전 — CameraX 라이브 프리뷰
                CameraPreview(
                    onPhotoTaken = { path -> onEvent(PhotoSetupEvent.TakePhoto(path)) },
                    onCaptureFunctionReady = { captureAction = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PhotoSetupBottomBar(
    isPhotoTaken: Boolean,
    onTakePhoto: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPhotoTaken) {
            // 촬영 완료 상태 — 재촬영 + 확인 버튼
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
            ) {
                Text(text = stringResource(R.string.photo_retake))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            ) {
                Text(text = stringResource(R.string.photo_confirm))
            }
        } else {
            // 촬영 전 상태 — 촬영 버튼
            Button(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.photo_take))
            }
        }
    }
}
