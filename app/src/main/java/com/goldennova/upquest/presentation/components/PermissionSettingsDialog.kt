package com.goldennova.upquest.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.goldennova.upquest.R

/**
 * 권한이 영구 거부된 경우 앱 설정으로 안내하는 다이얼로그.
 *
 * @param title 다이얼로그 제목 (권한명)
 * @param description 권한이 필요한 이유 설명
 * @param onGoToSettings 설정으로 이동 버튼 클릭
 * @param onDismiss 취소 버튼 또는 외부 영역 클릭
 */
@Composable
fun PermissionSettingsDialog(
    title: String,
    description: String,
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(text = stringResource(R.string.permission_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.permission_cancel))
            }
        },
    )
}
