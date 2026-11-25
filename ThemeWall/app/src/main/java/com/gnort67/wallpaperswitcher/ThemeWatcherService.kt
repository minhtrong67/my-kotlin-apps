package com.gnort67.wallpaperswitcher

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class ThemeWatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = sync()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sync()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CH, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open).setOngoing(true).setSilent(true).build()
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
        scope.launch { if (WallpaperApplier.apply(applicationContext, dark)) p.lastApplied = key }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        getSystemService(AlarmManager::class.java).cancel(slotIntent())
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CH = "watcher"
        private const val ACTION_SLOT = "com.gnort67.wallpaperswitcher.SLOT"
        fun start(c: Context) =
            ContextCompat.startForegroundService(c, Intent(c, ThemeWatcherService::class.java))
        fun stop(c: Context) { c.stopService(Intent(c, ThemeWatcherService::class.java)) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val p = Prefs.get(c)
        if (i.action == Intent.ACTION_BOOT_COMPLETED && p.autoSwitch && p.startOnBoot) {
            ThemeWatcherService.start(c)
        }
    }
}
