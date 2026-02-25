package com.goldennova.upquest.data.repository

import com.goldennova.upquest.data.datasource.ThemePreferencesDataSource
import com.goldennova.upquest.domain.repository.ThemeRepository
import com.goldennova.upquest.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val dataSource: ThemePreferencesDataSource,
) : ThemeRepository {

    override fun getThemeMode(): Flow<ThemeMode> = dataSource.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)
}
