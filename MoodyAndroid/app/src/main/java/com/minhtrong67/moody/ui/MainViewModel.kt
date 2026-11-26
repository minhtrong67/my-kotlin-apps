package com.minhtrong67.moody.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minhtrong67.moody.MoodyApp
import com.minhtrong67.moody.data.ActivitiesLogic
import com.minhtrong67.moody.data.ActivitiesState
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.EntryData
import com.minhtrong67.moody.data.ImportItem
import com.minhtrong67.moody.data.MoodOverride
import com.minhtrong67.moody.data.local.Settings
import com.minhtrong67.moody.notify.Notifications
import com.minhtrong67.moody.notify.ReminderKind
import com.minhtrong67.moody.notify.ReminderScheduler
import com.minhtrong67.moody.util.todayISO
import kotlinx.coroutines.flow.filterNotNull
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as MoodyApp).container
    private val store = container.settingsStore
    private val repo = container.repository
    private val auth = container.auth

    val firebaseReady: Boolean get() = container.firebaseReady

    val settings: StateFlow<Settings?> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val user: StateFlow<AppUser?> = auth.user
    val authInitializing: StateFlow<Boolean> = auth.initializing

    /** null = still loading (switching account / first snapshot). */
    val entries: StateFlow<List<Entry>?> = repo.entries().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val activitiesState: StateFlow<ActivitiesState?> = repo.activitiesState().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val moodOverrides: StateFlow<Map<String, MoodOverride>?> = repo.moodOverrides().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Remember whether today already has a check-in so the daily reminder can skip people who did it.
        viewModelScope.launch {
            entries.filterNotNull().collect { list ->
                val today = todayISO()
                store.setCheckedInDate(if (list.any { it.date == today }) today else "")
            }
        }
    }

    private val _signInFailed = MutableStateFlow(false)
    val signInFailed: StateFlow<Boolean> = _signInFailed.asStateFlow()
    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    // ---------------- Settings ----------------
    fun setThemeMode(v: String) = viewModelScope.launch { store.setThemeMode(v) }
    fun setThemeColor(v: String) = viewModelScope.launch { store.setThemeColor(v) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { store.setDynamicColor(v) }
    fun setFontScale(v: Float) = viewModelScope.launch { store.setFontScale(v) }
    fun setLanguage(v: String) = viewModelScope.launch { store.setLanguage(v) }
    fun setFontFamily(v: String) = viewModelScope.launch { store.setFontFamily(v) }
    fun setStartPage(v: String) = viewModelScope.launch { store.setStartPage(v) }
    fun setDailyEnabled(v: Boolean) = viewModelScope.launch { store.setDailyEnabled(v) }
    fun setDailyTime(v: String) = viewModelScope.launch { store.setDailyTime(v) }
    fun setWeeklyEnabled(v: Boolean) = viewModelScope.launch { store.setWeeklyEnabled(v) }
    fun setMonthlyEnabled(v: Boolean) = viewModelScope.launch { store.setMonthlyEnabled(v) }
    fun sendTestNotification(language: String): Boolean = Notifications.show(getApplication(), ReminderKind.Test, language)
    fun scheduleTestAlarm() = ReminderScheduler.scheduleTest(getApplication(), 10)
    /** Called when returning from system settings (exact-alarm / notification permission may have changed). */
    fun rescheduleReminders() = viewModelScope.launch { ReminderScheduler.reschedule(getApplication(), store) }

    // ---------------- Auth ----------------
    fun continueAsGuest() = viewModelScope.launch { store.setGuest(true) }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _signingIn.value = true
            _signInFailed.value = false
            val r = auth.signInWithGoogle(activityContext)
            if (r.isSuccess) store.setGuest(false) else _signInFailed.value = true
            _signingIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            store.setGuest(false)
            auth.signOut()
        }
    }

    // ---------------- Entries ----------------
    fun addEntry(data: EntryData) = viewModelScope.launch { repo.addEntry(data) }
    fun updateEntry(id: String, data: EntryData) = viewModelScope.launch { repo.updateEntry(id, data) }
    fun deleteEntry(id: String) = viewModelScope.launch { repo.deleteEntry(id) }
    suspend fun importEntries(items: List<ImportItem>): Pair<Int, Int> = repo.importEntries(items)

    // ---------------- Activities / moods ----------------
    private suspend fun currentActivities(): ActivitiesState =
        activitiesState.value ?: ActivitiesState()

    /** Returns the new activity's id immediately (so the check-in form can select it right away). */
    fun addActivity(emoji: String, label: String): String {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch { repo.saveActivities(ActivitiesLogic.add(currentActivities(), emoji, label, id)) }
        return id
    }
    fun updateActivity(id: String, emoji: String, label: String) = viewModelScope.launch { repo.saveActivities(ActivitiesLogic.update(currentActivities(), id, emoji, label)) }
    fun removeActivity(id: String) = viewModelScope.launch { repo.saveActivities(ActivitiesLogic.remove(currentActivities(), id)) }
    fun reorderActivities(ids: List<String>) = viewModelScope.launch { repo.saveActivities(ActivitiesLogic.reorder(currentActivities(), ids)) }

    suspend fun importActivities(items: List<ActivitiesLogic.Imported>): Pair<Int, Int> {
        val (next, added, updated) = ActivitiesLogic.import(currentActivities(), items)
        repo.saveActivities(next)
        return added to updated
    }

    fun updateMood(key: String, emoji: String, label: String) = viewModelScope.launch {
        val cur = moodOverrides.value ?: emptyMap()
        repo.saveMoodOverrides(cur + (key to MoodOverride(emoji, label)))
    }

    fun resetMood(key: String) = viewModelScope.launch {
        repo.saveMoodOverrides((moodOverrides.value ?: emptyMap()) - key)
    }

    suspend fun importMoods(items: List<Pair<String, MoodOverride>>): Int {
        val next = (moodOverrides.value ?: emptyMap()).toMutableMap()
        for ((k, patch) in items) {
            val old = next[k]
            next[k] = MoodOverride(patch.emoji ?: old?.emoji, patch.label ?: old?.label)
        }
        repo.saveMoodOverrides(next)
        return items.size
    }
}
