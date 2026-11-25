package com.gnort67.wallpaperswitcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat

class Prefs private constructor(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableIntStateOf(sp.getInt("theme", 0)); private set      // 0 system, 1 light, 2 dark
    var dynamicColor by mutableStateOf(sp.getBoolean("dynamic", true)); private set
    var language by mutableIntStateOf(sp.getInt("lang", 0)); private set        // 0 system, 1 en, 2 vi
    var target by mutableIntStateOf(sp.getInt("target", 0)); private set        // 0 both, 1 home, 2 lock
    var autoSwitch by mutableStateOf(sp.getBoolean("auto", false)); private set
    var startOnBoot by mutableStateOf(sp.getBoolean("boot", true)); private set
    var scheduleEnabled by mutableStateOf(sp.getBoolean("schedule", false)); private set
    var dim by mutableFloatStateOf(sp.getFloat("dim", 0f)); private set          // 0..0.6, dark wallpaper only

    var lastAppliedAt by mutableLongStateOf(sp.getLong("at", 0L)); private set
    var killedHint by mutableStateOf(sp.getBoolean("killed", false)); private set

    /** Set while the watcher service is alive; still set at next launch = it was killed. */
    var serviceMarker: Boolean
        get() = sp.getBoolean("marker", false)
        set(v) { sp.edit().putBoolean("marker", v).apply() }

    fun markApplied(key: Int) {
        lastApplied = key
        lastAppliedAt = System.currentTimeMillis()
        sp.edit().putLong("at", lastAppliedAt).apply()
    }
    fun updateKilledHint(v: Boolean) { killedHint = v; sp.edit().putBoolean("killed", v).apply() }

    /** Key of the state (mode + slot) whose wallpaper was applied last; -1 = unknown. */
    var lastApplied: Int
        get() = sp.getInt("last", -1)
        set(v) { sp.edit().putInt("last", v).apply() }

    fun updateTheme(v: Int) { themeMode = v; sp.edit().putInt("theme", v).apply() }
    fun updateDynamic(v: Boolean) { dynamicColor = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun updateTarget(v: Int) { target = v; lastApplied = -1; sp.edit().putInt("target", v).apply() }
    fun updateAuto(v: Boolean) { autoSwitch = v; lastApplied = -1; sp.edit().putBoolean("auto", v).apply() }
    fun updateBoot(v: Boolean) { startOnBoot = v; sp.edit().putBoolean("boot", v).apply() }
    fun updateSchedule(v: Boolean) { scheduleEnabled = v; lastApplied = -1; sp.edit().putBoolean("schedule", v).apply() }
    fun updateDim(v: Float) { dim = v; lastApplied = -1; sp.edit().putFloat("dim", v).apply() }
    fun updateLanguage(v: Int) {
        language = v
        sp.edit().putInt("lang", v).apply()
        AppCompatDelegate.setApplicationLocales(
            when (v) {
                1 -> LocaleListCompat.forLanguageTags("en")
                2 -> LocaleListCompat.forLanguageTags("vi")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
        )
    }

    companion object {
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) {
            inst ?: Prefs(ctx).also { inst = it }
        }
    }
}
