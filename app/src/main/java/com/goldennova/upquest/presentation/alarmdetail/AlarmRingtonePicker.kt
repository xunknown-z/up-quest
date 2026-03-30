package com.goldennova.upquest.presentation.alarmdetail

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.goldennova.upquest.R

/**
 * 알람 전용 링톤 선택 바텀 시트.
 *
 * 시스템 링톤 피커([RingtoneManager.ACTION_RINGTONE_PICKER])는 OEM(삼성 등)에 따라
 * 진동·무음 모드에서 미리 듣기 소리가 나지 않는 문제가 있다.
 * 이 컴포저블은 [AudioAttributes.USAGE_ALARM]을 직접 지정해 링톤을 재생하므로
 * 진동·무음 모드와 무관하게 알람 스트림으로 소리를 확인할 수 있다.
 *
 * @param currentUri 현재 선택된 링톤 URI 문자열 (null이면 선택 없음)
 * @param onConfirm 확인 버튼 클릭 시 선택된 URI 문자열 전달 (null이면 선택 없음)
 * @param onDismiss 취소 또는 바깥 영역 클릭 시 호출
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmRingtonePicker(
    currentUri: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 기기에 설치된 알람 링톤 목록 구성
    val ringtones: List<Pair<String, Uri>> = remember {
        buildList {
            val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            val cursor = manager.cursor
            var pos = 0
            while (cursor.moveToNext()) {
                val uri = manager.getRingtoneUri(pos)
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                add(title to uri)
                pos++
            }
        }
    }

    var selectedUri by remember { mutableStateOf(currentUri?.let { Uri.parse(it) }) }
    var playingRingtone by remember { mutableStateOf<Ringtone?>(null) }

    // 컴포저블이 사라질 때 재생 중인 링톤 정지
    DisposableEffect(Unit) {
        onDispose { playingRingtone?.stop() }
    }

    ModalBottomSheet(
        onDismissRequest = {
            playingRingtone?.stop()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.ringtone_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(ringtones) { (title, uri) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUri = uri
                                playingRingtone?.stop()
                                // USAGE_ALARM으로 재생 — 진동·무음 모드에서도 알람 스트림 사용
                                val ringtone = RingtoneManager.getRingtone(context, uri)
                                ringtone.audioAttributes = AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                                ringtone.play()
                                playingRingtone = ringtone
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedUri == uri,
                            onClick = null,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    playingRingtone?.stop()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.permission_cancel))
                }
                TextButton(onClick = {
                    playingRingtone?.stop()
                    onConfirm(selectedUri?.toString())
                }) {
                    Text(stringResource(R.string.photo_confirm))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
