package com.expenser.app

import android.app.Application
import com.expenser.app.data.AuthRepository
import com.expenser.app.data.Repository
import com.expenser.app.data.SettingsStore
import com.expenser.app.reminder.ReminderScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow

/** Tiny hand-rolled service locator — keeps the project free of DI-framework setup. */
class ExpenserApp : Application() {
    val settingsStore by lazy { SettingsStore(this) }
    val auth by lazy { AuthRepository(this) }
    val writeErrors = MutableSharedFlow<Exception>(extraBufferCapacity = 4)
    val repo: Repository? by lazy {
        if (auth.configured) Repository(FirebaseFirestore.getInstance()) { writeErrors.tryEmit(it) } else null
    }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
