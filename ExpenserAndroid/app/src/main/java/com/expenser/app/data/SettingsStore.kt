package com.expenser.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: String = "system", // light | dark | system
    val accent: String = "", // "" = default Material 3 blue, otherwise #RRGGBB
    val dynamicColor: Boolean = false, // Material You wallpaper colours (Android 12+)
    val fontSize: String = "md", // sm | md | lg
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "06:00",
    val reportWeekly: Boolean = false,
    val reportMonthly: Boolean = false,
    val seeded: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object K {
        val themeMode = stringPreferencesKey("themeMode")
        val accent = stringPreferencesKey("accent")
        val dynamic = booleanPreferencesKey("dynamicColor")
        val fontSize = stringPreferencesKey("fontSize")
        val reminderEnabled = booleanPreferencesKey("reminderEnabled")
        val reminderTime = stringPreferencesKey("reminderTime")
        val reportWeekly = booleanPreferencesKey("reportWeekly")
        val reportMonthly = booleanPreferencesKey("reportMonthly")
        val seeded = booleanPreferencesKey("seeded")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            themeMode = p[K.themeMode] ?: d.themeMode,
            accent = p[K.accent] ?: d.accent,
            dynamicColor = p[K.dynamic] ?: d.dynamicColor,
            fontSize = p[K.fontSize] ?: d.fontSize,
            reminderEnabled = p[K.reminderEnabled] ?: d.reminderEnabled,
            reminderTime = p[K.reminderTime] ?: d.reminderTime,
            reportWeekly = p[K.reportWeekly] ?: d.reportWeekly,
            reportMonthly = p[K.reportMonthly] ?: d.reportMonthly,
            seeded = p[K.seeded] ?: d.seeded,
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setThemeMode(v: String) = edit { it[K.themeMode] = v }
    suspend fun setAccent(v: String) = edit { it[K.accent] = v }
    suspend fun setDynamicColor(v: Boolean) = edit { it[K.dynamic] = v }
    suspend fun setFontSize(v: String) = edit { it[K.fontSize] = v }
    suspend fun setReminder(enabled: Boolean, time: String) = edit {
        it[K.reminderEnabled] = enabled
        it[K.reminderTime] = time
    }
    suspend fun setReports(weekly: Boolean, monthly: Boolean) = edit {
        it[K.reportWeekly] = weekly
        it[K.reportMonthly] = monthly
    }
    suspend fun markSeeded() = edit { it[K.seeded] = true }

    /** Resets appearance only (data and language are kept), like the web app's "Restore settings". */
    suspend fun resetAppearance() = edit {
        it.remove(K.themeMode); it.remove(K.accent); it.remove(K.dynamic); it.remove(K.fontSize)
    }
}
