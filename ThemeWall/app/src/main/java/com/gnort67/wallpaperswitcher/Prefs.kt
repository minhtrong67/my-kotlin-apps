package com.gnort67.wallpaperswitcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A named pair of wallpapers (light + dark, plus optional time-of-day slots). */
@Immutable
class WallSet(val id: String, val name: String)

class Prefs private constructor(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init { migrateLegacy(ctx.applicationContext.filesDir) }

    var themeMode by mutableIntStateOf(sp.getInt("theme", 0)); private set      // 0 system, 1 light, 2 dark
    var dynamicColor by mutableStateOf(sp.getBoolean("dynamic", true)); private set
    var language by mutableIntStateOf(sp.getInt("lang", 0)); private set        // 0 system, 1 en, 2 vi
    var target by mutableIntStateOf(sp.getInt("target", 0)); private set        // 0 both, 1 home, 2 lock
    var autoSwitch by mutableStateOf(sp.getBoolean("auto", false)); private set
    var startOnBoot by mutableStateOf(sp.getBoolean("boot", true)); private set
    var scheduleEnabled by mutableStateOf(sp.getBoolean("schedule", false)); private set
    var dim by mutableFloatStateOf(sp.getFloat("dim", 0f)); private set          // 0..0.6, dark wallpaper only
    var modeSource by mutableIntStateOf(sp.getInt("msrc", 0)); private set        // 0 follow system, 1 custom times
    var lightFrom by mutableIntStateOf(sp.getInt("lightFrom", 6 * 60)); private set  // minutes after midnight
    var darkFrom by mutableIntStateOf(sp.getInt("darkFrom", 18 * 60)); private set
    var lastAppliedAt by mutableLongStateOf(sp.getLong("at", 0L)); private set
    var killedHint by mutableStateOf(sp.getBoolean("killed", false)); private set
    var sets by mutableStateOf(loadSets()); private set
    var activeId by mutableStateOf(sp.getString("active", DEFAULT_ID) ?: DEFAULT_ID); private set

    val activeSet: WallSet get() = sets.firstOrNull { it.id == activeId } ?: sets.first()

    /** Set while the watcher service is alive; still set at next launch = it was killed. */
    var serviceMarker: Boolean
        get() = sp.getBoolean("marker", false)
        set(v) { sp.edit().putBoolean("marker", v).apply() }

    /** Key of the state (mode + slot) whose wallpaper was applied last; -1 = unknown. */
    var lastApplied: Int
        get() = sp.getInt("last", -1)
        set(v) { sp.edit().putInt("last", v).apply() }

    fun markApplied(key: Int) {
        lastApplied = key
        lastAppliedAt = System.currentTimeMillis()
        sp.edit().putLong("at", lastAppliedAt).apply()
    }
    fun updateKilledHint(v: Boolean) { killedHint = v; sp.edit().putBoolean("killed", v).apply() }
    fun updateTheme(v: Int) { themeMode = v; sp.edit().putInt("theme", v).apply() }
    fun updateDynamic(v: Boolean) { dynamicColor = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun updateTarget(v: Int) { target = v; lastApplied = -1; sp.edit().putInt("target", v).apply() }
    fun updateAuto(v: Boolean) { autoSwitch = v; lastApplied = -1; sp.edit().putBoolean("auto", v).apply() }
    fun updateBoot(v: Boolean) { startOnBoot = v; sp.edit().putBoolean("boot", v).apply() }
    fun updateSchedule(v: Boolean) { scheduleEnabled = v; lastApplied = -1; sp.edit().putBoolean("schedule", v).apply() }
    fun updateModeSource(v: Int) { modeSource = v; lastApplied = -1; sp.edit().putInt("msrc", v).apply() }
    fun updateLightFrom(m: Int) { lightFrom = m; lastApplied = -1; sp.edit().putInt("lightFrom", m).apply() }
    fun updateDarkFrom(m: Int) { darkFrom = m; lastApplied = -1; sp.edit().putInt("darkFrom", m).apply() }
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

    // ── Wallpaper sets ──
    fun addSet(name: String): String {
        val id = UUID.randomUUID().toString().take(8)
        sets = sets + WallSet(id, name.trim())
        saveSets(sets)
        return id
    }
    fun renameSet(id: String, name: String) {
        sets = sets.map { if (it.id == id) WallSet(id, name.trim()) else it }
        saveSets(sets)
    }
    fun removeSet(id: String) {
        if (sets.size <= 1) return
        sets = sets.filterNot { it.id == id }
        saveSets(sets)
        if (activeId == id) selectSet(sets.first().id)
    }
    fun selectSet(id: String) {
        activeId = id
        lastApplied = -1
        sp.edit().putString("active", id).apply()
    }

    private fun loadSets(): List<WallSet> {
        val raw = sp.getString("sets", null)
        if (raw != null) {
            val parsed = runCatching {
                val a = JSONArray(raw)
                List(a.length()) { i -> a.getJSONObject(i).let { WallSet(it.getString("id"), it.optString("name")) } }
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }
        return listOf(WallSet(DEFAULT_ID, ""))
    }

    private fun saveSets(l: List<WallSet>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        sp.edit().putString("sets", a.toString()).apply()
    }

    /** Pre-sets versions stored files as wp_<l|d>_<slot>.jpg; move them into the default set. */
    private fun migrateLegacy(dir: File) {
        for (m in listOf("l", "d")) for (s in 0..4) {
            val old = File(dir, "wp_${m}_$s.jpg")
            if (old.exists()) old.renameTo(File(dir, "wp_${DEFAULT_ID}_${m}_$s.jpg"))
        }
    }

    companion object {
        const val DEFAULT_ID = "default"
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) {
            inst ?: Prefs(ctx).also { inst = it }
        }
    }
}
