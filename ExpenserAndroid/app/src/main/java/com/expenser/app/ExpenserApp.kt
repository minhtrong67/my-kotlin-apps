package com.expenser.app

import android.app.Application
import com.expenser.app.data.AppDatabase
import com.expenser.app.data.Repository
import com.expenser.app.data.SettingsStore
import com.expenser.app.reminder.ReminderScheduler

/** Tiny hand-rolled service locator — keeps the project free of DI-framework setup. */
class ExpenserApp : Application() {
    val settingsStore by lazy { SettingsStore(this) }
    val repo by lazy { Repository(AppDatabase.create(this), settingsStore) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
