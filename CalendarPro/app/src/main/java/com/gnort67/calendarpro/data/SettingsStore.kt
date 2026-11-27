package com.gnort67.calendarpro.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gnort67.calendarpro.core.PersonalEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.store by preferencesDataStore(name = "calendar_pro")

data class AppSettings(
    val themeMode: String = "system", // light | dark | system
    val colorId: String = "blue",
    val dailyRotation: Boolean = false,
    val language: String = "vi", // vi | en
    val fontSize: String = "vua", // nho | vua | lon
    val fontFamily: String = "system"
) {
    val fontScale: Float get() = when (fontSize) { "nho" -> 0.9f; "lon" -> 1.1f; else -> 1f }
}

class SettingsStore(private val ctx: Context) {
    private object K {
        val mode = stringPreferencesKey("theme_mode")
        val color = stringPreferencesKey("color")
        val rotation = booleanPreferencesKey("daily_rotation")
        val lang = stringPreferencesKey("language")
        val size = stringPreferencesKey("font_size")
        val font = stringPreferencesKey("font_family")
        val events = stringPreferencesKey("personal_events")
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = this[K.mode] ?: d.themeMode, colorId = this[K.color] ?: d.colorId,
            dailyRotation = this[K.rotation] ?: d.dailyRotation, language = this[K.lang] ?: d.language,
            fontSize = this[K.size] ?: d.fontSize, fontFamily = this[K.font] ?: d.fontFamily
        )
    }

    val settings: Flow<AppSettings> = ctx.store.data.map { it.toSettings() }
    val events: Flow<List<PersonalEvent>> = ctx.store.data.map { decode(it[K.events]) }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        ctx.store.edit { p ->
            val n = block(p.toSettings())
            p[K.mode] = n.themeMode; p[K.color] = n.colorId; p[K.rotation] = n.dailyRotation
            p[K.lang] = n.language; p[K.size] = n.fontSize; p[K.font] = n.fontFamily
        }
    }

    suspend fun reset() {
        ctx.store.edit { p ->
            listOf(K.mode, K.color, K.size, K.font).forEach { p.remove(it) }
            p.remove(K.rotation)
        }
    }

    suspend fun updateEvents(block: (List<PersonalEvent>) -> List<PersonalEvent>) {
        ctx.store.edit { p -> p[K.events] = encode(block(decode(p[K.events]))) }
    }

    private fun encode(list: List<PersonalEvent>): String {
        val a = JSONArray()
        list.forEach { e ->
            a.put(JSONObject().put("id", e.id).put("name", e.name).put("type", e.type).put("day", e.day)
                .put("month", e.month).put("icon", e.icon).apply { e.year?.let { put("year", it) } })
        }
        return a.toString()
    }

    private fun decode(raw: String?): List<PersonalEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                PersonalEvent(
                    id = o.getString("id"), name = o.getString("name"), type = o.getString("type"),
                    day = o.getInt("day"), month = o.getInt("month"),
                    year = if (o.has("year")) o.getInt("year") else null, icon = o.optString("icon", "CakeSlice")
                )
            }.filter { PersonalEvent.isValid(it) }.take(PersonalEvent.MAX_EVENTS)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
