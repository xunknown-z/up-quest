package com.goldennova.upquest.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldennova.upquest.domain.repository.ThemeRepository
import com.goldennova.upquest.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeThemeMode()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ChangeThemeMode -> setThemeMode(event.mode)
        }
    }

    // ThemeRepository에서 테마 모드를 Flow로 수집 → uiState 업데이트
    private fun observeThemeMode() {
        viewModelScope.launch {
            themeRepository.getThemeMode()
                .catch { /* 수집 실패 시 기본값(SYSTEM) 유지 */ }
                .collect { mode ->
                    _uiState.update { it.copy(currentThemeMode = mode) }
                }
        }
    }

    // 테마 모드 변경 → DataStore에 저장
    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }
}
