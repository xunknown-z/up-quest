package com.goldennova.upquest.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.goldennova.upquest.R
import com.goldennova.upquest.presentation.theme.ThemeMode
import com.goldennova.upquest.presentation.theme.UpQuestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 테마 섹션 헤더
            Text(
                text = stringResource(R.string.theme_mode_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.height(4.dp))

            // 테마 RadioButton 그룹
            ThemeModeSelector(
                selectedMode = uiState.currentThemeMode,
                onModeSelected = { mode -> onEvent(SettingsEvent.ChangeThemeMode(mode)) },
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // selectableGroup: 접근성 그룹 처리
    Column(modifier = modifier.selectableGroup()) {
        ThemeModeItem(
            label = stringResource(R.string.theme_light),
            selected = selectedMode == ThemeMode.LIGHT,
            onClick = { onModeSelected(ThemeMode.LIGHT) },
        )
        ThemeModeItem(
            label = stringResource(R.string.theme_dark),
            selected = selectedMode == ThemeMode.DARK,
            onClick = { onModeSelected(ThemeMode.DARK) },
        )
        ThemeModeItem(
            label = stringResource(R.string.theme_system),
            selected = selectedMode == ThemeMode.SYSTEM,
            onClick = { onModeSelected(ThemeMode.SYSTEM) },
        )
    }
}

@Composable
private fun ThemeModeItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // selectable Row가 클릭 처리
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// region Preview

@Preview(showBackground = true, name = "SettingsScreen - 시스템 테마")
@Composable
private fun SettingsScreenSystemPreview() {
    UpQuestTheme {
        SettingsScreen(
            uiState = SettingsUiState(currentThemeMode = ThemeMode.SYSTEM),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsScreen - 라이트 테마")
@Composable
private fun SettingsScreenLightPreview() {
    UpQuestTheme {
        SettingsScreen(
            uiState = SettingsUiState(currentThemeMode = ThemeMode.LIGHT),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsScreen - 다크 테마")
@Composable
private fun SettingsScreenDarkPreview() {
    UpQuestTheme {
        SettingsScreen(
            uiState = SettingsUiState(currentThemeMode = ThemeMode.DARK),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

// endregion
