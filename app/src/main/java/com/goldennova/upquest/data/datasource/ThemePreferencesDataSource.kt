package com.goldennova.upquest.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.goldennova.upquest.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // 저장 키
    private val themeModeKey = stringPreferencesKey("theme_mode")

    // 저장된 ThemeMode를 Flow로 노출, 값 없으면 SYSTEM 반환
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        when (preferences[themeModeKey]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }
}
