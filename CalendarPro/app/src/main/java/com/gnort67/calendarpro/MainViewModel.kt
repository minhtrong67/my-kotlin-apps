package com.gnort67.calendarpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.AppSettings
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    val catalog: HolidayCatalog = HolidayCatalog.load(app)

    /** null cho tới khi đọc xong DataStore (giữ splash screen). */
    val settings: StateFlow<AppSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val events: StateFlow<List<PersonalEvent>> = store.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun update(block: (AppSettings) -> AppSettings) { viewModelScope.launch { store.update(block) } }
    fun resetSettings() { viewModelScope.launch { store.reset() } }

    /** Trả về false nếu vượt giới hạn số sự kiện. */
    fun saveEvent(e: PersonalEvent): Boolean {
        val exists = events.value.any { it.id == e.id }
        if (!exists && events.value.size >= PersonalEvent.MAX_EVENTS) return false
        viewModelScope.launch {
            store.updateEvents { list -> if (exists) list.map { if (it.id == e.id) e else it } else list + e }
        }
        return true
    }

    fun deleteEvent(id: String) { viewModelScope.launch { store.updateEvents { l -> l.filterNot { it.id == id } } } }

    fun newEventId(): String = UUID.randomUUID().toString().take(8)
}
