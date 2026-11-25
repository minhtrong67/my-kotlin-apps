package com.expenser.app

import android.app.Application
import com.expenser.app.data.AuthRepository
import com.expenser.app.data.Repository
import com.expenser.app.data.SettingsStore
import com.expenser.app.reminder.ReminderScheduler
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow

/** Tiny hand-rolled service locator — keeps the project free of DI-framework setup. */
class ExpenserApp : Application(), ImageLoaderFactory {
    val settingsStore by lazy { SettingsStore(this) }
    val auth by lazy { AuthRepository(this) }
    val writeErrors = MutableSharedFlow<Exception>(extraBufferCapacity = 4)
    val repo: Repository? by lazy {
        if (auth.configured) Repository(FirebaseFirestore.getInstance()) { writeErrors.tryEmit(it) } else null
    }

    /** One shared image loader: 25 % RAM + 150 MB disk cache, cache headers ignored so photos from any host stay cached. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(150L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .allowRgb565(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
