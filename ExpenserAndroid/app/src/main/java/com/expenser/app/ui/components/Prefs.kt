package com.expenser.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.reflect.KProperty

/** A small UI preference (e.g. "cards vs list") that survives leaving the screen and restarting the app. */
class PersistedState<T>(private val key: String, initial: T, private val write: (android.content.SharedPreferences.Editor, String, T) -> Unit, private val prefs: android.content.SharedPreferences) {
    private var state by mutableStateOf(initial)
    var value: T
        get() = state
        set(v) { state = v; prefs.edit().also { write(it, key, v) }.apply() }
    operator fun getValue(thisRef: Any?, p: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, p: KProperty<*>, v: T) { value = v }
}

@Composable
fun rememberBoolPref(key: String, default: Boolean): PersistedState<Boolean> {
    val prefs = LocalContext.current.getSharedPreferences("view_prefs", Context.MODE_PRIVATE)
    return remember(key) { PersistedState(key, prefs.getBoolean(key, default), { e, k, v -> e.putBoolean(k, v) }, prefs) }
}

@Composable
fun rememberStringPref(key: String, default: String): PersistedState<String> {
    val prefs = LocalContext.current.getSharedPreferences("view_prefs", Context.MODE_PRIVATE)
    return remember(key) { PersistedState(key, prefs.getString(key, default) ?: default, { e, k, v -> e.putString(k, v) }, prefs) }
}
