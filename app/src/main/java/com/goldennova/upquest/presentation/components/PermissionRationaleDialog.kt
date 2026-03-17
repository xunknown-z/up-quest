package com.goldennova.upquest.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.goldennova.upquest.R

/**
 * 권한이 거부된 뒤 재요청 이유를 설명하는 다이얼로그.
 *
 * @param title 다이얼로그 제목 (권한명)
 * @param description 권한이 필요한 이유 설명
 * @param onAllow 허용 버튼 클릭 — 권한 재요청 호출
 * @param onDismiss 취소 버튼 또는 외부 영역 클릭
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    description: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(text = stringResource(R.string.permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.permission_cancel))
            }
        },
    )
}
