package com.goldennova.upquest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldennova.upquest.domain.repository.ThemeRepository
import com.goldennova.upquest.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    themeRepository: ThemeRepository,
) : ViewModel() {

    // 앱 시작 시 테마 깜빡임 방지를 위해 Eagerly로 즉시 수집 시작
    val themeMode: StateFlow<ThemeMode> = themeRepository
        .getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )
}
