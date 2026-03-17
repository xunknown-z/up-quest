package com.goldennova.upquest.domain.repository

import com.goldennova.upquest.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
