package com.gnort67.wallpaperswitcher

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class ThemeWatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val syncRunnable = Runnable { sync() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = debounce()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sync()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        debounce()
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel("watcher")
        nm.createNotificationChannel(
            NotificationChannel(CH, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .setOngoing(true).setSilent(true).setOnlyAlertOnce(true).setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
        ServiceCompat.startForeground(
            this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(ACTION_SLOT)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        Prefs.get(this).serviceMarker = true
        running = true
    }

    /** Samsung often fires several config broadcasts in a row; wait until they settle. */
    private fun debounce() {
        handler.removeCallbacks(syncRunnable)
        handler.postDelayed(syncRunnable, 300)
    }

    private fun slotIntent() = PendingIntent.getBroadcast(
        this, 0, Intent(ACTION_SLOT).setPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Inexact alarm (no special permission) for the next time-of-day boundary. */
    private fun scheduleAlarm(p: Prefs) {
        val am = getSystemService(AlarmManager::class.java)
        if (p.scheduleEnabled) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, WallpaperApplier.nextBoundary(), slotIntent())
        } else am.cancel(slotIntent())
    }

    private fun sync() {
        val p = Prefs.get(this)
        scheduleAlarm(p)
        val dark = WallpaperApplier.isSystemDark()
        val key = WallpaperApplier.stateKey(this, dark)
        if (p.lastApplied == key) return
        scope.launch {
            if (WallpaperApplier.apply(applicationContext, dark)) {
                p.markApplied(key)
                // One cheap re-check in case the mode flipped again while applying.
                handler.postDelayed(syncRunnable, 1500)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(syncRunnable)
        unregisterReceiver(receiver)
        getSystemService(AlarmManager::class.java).cancel(slotIntent())
        scope.cancel()
        Prefs.get(this).serviceMarker = false
        running = false
        super.onDestroy()
    }

    companion object {
        private const val CH = "watcher_quiet"
        private const val ACTION_SLOT = "com.gnort67.wallpaperswitcher.SLOT"

        /** True while the service process is alive (Compose-observable). */
        var running by mutableStateOf(false)
            internal set

        fun start(c: Context) {
            runCatching { ContextCompat.startForegroundService(c, Intent(c, ThemeWatcherService::class.java)) }
        }
        fun stop(c: Context) { c.stopService(Intent(c, ThemeWatcherService::class.java)) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val p = Prefs.get(c)
        when (i.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                p.serviceMarker = false
                if (p.autoSwitch && p.startOnBoot) ThemeWatcherService.start(c)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                p.serviceMarker = false
                if (p.autoSwitch) ThemeWatcherService.start(c)
            }
        }
    }
}
