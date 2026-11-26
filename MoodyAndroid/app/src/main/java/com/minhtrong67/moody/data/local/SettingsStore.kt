package com.minhtrong67.moody.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minhtrong67.moody.data.ActivitiesState
import com.minhtrong67.moody.data.MoodOverride
import com.minhtrong67.moody.ui.theme.Palettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.moodyDataStore by preferencesDataStore(name = "moody_settings")

/** Start pages the user can pick (web: START_PAGES). */
val START_PAGES = listOf("today", "history", "stats", "checkin", "activities", "moods", "settings")

data class Settings(
    val themeMode: String = "system",              // system | light | dark
    val themeColor: String = Palettes.DEFAULT_ID,  // palette id (teal, blue, ...)
    val dynamicColor: Boolean = false,             // Material You: follow the wallpaper (Android 12+)
    val fontScale: Float = 1f,                     // 0.9 | 1 | 1.15
    val language: String = "auto",                 // auto | vi | en
    val startPage: String = "today",
    val guest: Boolean = false,
    val dailyEnabled: Boolean = false,
    val dailyTime: String = "20:00",
    val weeklyEnabled: Boolean = false,
    val monthlyEnabled: Boolean = false,
    val checkedInDate: String = "",                // yyyy-MM-dd of the last known check-in "today" (skips the daily nudge)
    val fontFamily: String = "system",
)

class SettingsStore(private val context: Context) {
    private val ds get() = context.moodyDataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val moodsSerializer = MapSerializer(String.serializer(), MoodOverride.serializer())

    private object K {
        val themeMode = stringPreferencesKey("theme_mode")
        val themeColor = stringPreferencesKey("theme_color")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val fontScale = stringPreferencesKey("font_scale")
        val language = stringPreferencesKey("language")
        val startPage = stringPreferencesKey("start_page")
        val guest = booleanPreferencesKey("guest")
        val dailyEnabled = booleanPreferencesKey("notif_daily_enabled")
        val dailyTime = stringPreferencesKey("notif_daily_time")
        val weeklyEnabled = booleanPreferencesKey("notif_weekly_enabled")
        val monthlyEnabled = booleanPreferencesKey("notif_monthly_enabled")
        val checkedInDate = stringPreferencesKey("checked_in_date")
        val fontFamily = stringPreferencesKey("font_family")
        val localActivities = stringPreferencesKey("local_activities")
        val localMoods = stringPreferencesKey("local_moods")
    }

    val settings: Flow<Settings> = ds.data.map { p: Preferences ->
        Settings(
            themeMode = p[K.themeMode] ?: "system",
            themeColor = p[K.themeColor] ?: Palettes.DEFAULT_ID,
            dynamicColor = p[K.dynamicColor] ?: false,
            fontScale = p[K.fontScale]?.toFloatOrNull() ?: 1f,
            language = p[K.language] ?: "auto",
            startPage = p[K.startPage]?.takeIf { it in START_PAGES } ?: "today",
            guest = p[K.guest] ?: false,
            dailyEnabled = p[K.dailyEnabled] ?: false,
            dailyTime = p[K.dailyTime] ?: "20:00",
            weeklyEnabled = p[K.weeklyEnabled] ?: false,
            monthlyEnabled = p[K.monthlyEnabled] ?: false,
            checkedInDate = p[K.checkedInDate] ?: "",
            fontFamily = p[K.fontFamily] ?: "system",
        )
    }

    suspend fun setThemeMode(v: String) { ds.edit { it[K.themeMode] = v } }
    suspend fun setThemeColor(v: String) { ds.edit { it[K.themeColor] = v } }
    suspend fun setDynamicColor(v: Boolean) { ds.edit { it[K.dynamicColor] = v } }
    suspend fun setFontScale(v: Float) { ds.edit { it[K.fontScale] = v.toString() } }
    suspend fun setLanguage(v: String) { ds.edit { it[K.language] = v } }
    suspend fun setStartPage(v: String) { ds.edit { it[K.startPage] = v } }
    suspend fun setGuest(v: Boolean) { ds.edit { it[K.guest] = v } }
    suspend fun setDailyEnabled(v: Boolean) { ds.edit { it[K.dailyEnabled] = v } }
    suspend fun setDailyTime(v: String) { ds.edit { it[K.dailyTime] = v } }
    suspend fun setWeeklyEnabled(v: Boolean) { ds.edit { it[K.weeklyEnabled] = v } }
    suspend fun setMonthlyEnabled(v: Boolean) { ds.edit { it[K.monthlyEnabled] = v } }
    suspend fun setCheckedInDate(v: String) { ds.edit { it[K.checkedInDate] = v } }
    suspend fun setFontFamily(v: String) { ds.edit { it[K.fontFamily] = v } }

    // ---- Guest-mode copies of the "meta" documents (activities, mood overrides) ----
    val localActivities: Flow<ActivitiesState> = ds.data.map { p ->
        p[K.localActivities]?.let { runCatching { json.decodeFromString(ActivitiesState.serializer(), it) }.getOrNull() }
            ?: ActivitiesState()
    }

    suspend fun setLocalActivities(state: ActivitiesState) {
        ds.edit { it[K.localActivities] = json.encodeToString(ActivitiesState.serializer(), state) }
    }

    val localMoods: Flow<Map<String, MoodOverride>> = ds.data.map { p ->
        p[K.localMoods]?.let { runCatching { json.decodeFromString(moodsSerializer, it) }.getOrNull() } ?: emptyMap()
    }

    suspend fun setLocalMoods(map: Map<String, MoodOverride>) {
        ds.edit { it[K.localMoods] = json.encodeToString(moodsSerializer, map) }
    }
}
