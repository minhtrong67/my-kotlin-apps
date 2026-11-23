package com.minhtrong67.moody

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.minhtrong67.moody.data.DataRepository
import com.minhtrong67.moody.data.auth.AuthManager
import com.minhtrong67.moody.data.local.AppDatabase
import com.minhtrong67.moody.data.local.SettingsStore
import com.minhtrong67.moody.data.remote.FirestoreStore
import com.minhtrong67.moody.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Tiny manual DI container. */
class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "moody.db").build()
    }
    val settingsStore by lazy { SettingsStore(context) }

    /** True only when google-services.json was present at build time. */
    val firebaseReady: Boolean by lazy {
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
    }
    val auth by lazy { AuthManager(context, firebaseReady) }
    private val remote by lazy { if (firebaseReady) FirestoreStore() else null }
    val repository by lazy { DataRepository(database.entryDao(), settingsStore, remote, auth) }
}

class MoodyApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        // Keep the background reminders in sync with the settings (survives reboots via WorkManager).
        appScope.launch {
            container.settingsStore.settings
                .map { listOf(it.dailyEnabled, it.dailyTime, it.weeklyEnabled, it.monthlyEnabled) }
                .distinctUntilChanged()
                .collect { ReminderScheduler.reschedule(applicationContext, container.settingsStore) }
        }
    }
}
