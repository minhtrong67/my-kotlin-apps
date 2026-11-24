package com.minhtrong67.moody.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.minhtrong67.moody.MainActivity
import com.minhtrong67.moody.MoodyApp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.local.SettingsStore
import com.minhtrong67.moody.util.todayISO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import com.minhtrong67.moody.util.withLanguage

enum class ReminderKind(val code: Int, val route: String) {
    Daily(1001, "checkin"),
    Weekly(1002, "stats"),
    Monthly(1003, "stats"),
    Test(1099, "today"),
}

object Notifications {
    // v2: the first channel was DEFAULT importance; importance can't be raised after creation, so use a new id.
    const val CHANNEL_ID = "moody_reminders_v2"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = context.getString(R.string.settings_remindersHint) }
                )
            }
            runCatching { nm.deleteNotificationChannel("moody_reminders") }   // old channel from the first build
        }
    }

    /** Android 13+ runtime permission + the user not having blocked the app/channel. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(base: Context, kind: ReminderKind, language: String = "auto"): Boolean {
        val context = base.withLanguage(language)
        if (!canPost(context)) return false
        ensureChannel(context)
        val (title, body) = when (kind) {
            ReminderKind.Daily -> R.string.notif_daily_title to R.string.notif_daily_body
            ReminderKind.Weekly -> R.string.notif_weekly_title to R.string.notif_weekly_body
            ReminderKind.Monthly -> R.string.notif_monthly_title to R.string.notif_monthly_body
            ReminderKind.Test -> R.string.notif_test_title to R.string.notif_test_body
        }
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, kind.route)
        }
        val pi = PendingIntent.getActivity(context, kind.code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_moody)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(body)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(kind.code, n)
        return true
    }
}

/**
 * Reminders use AlarmManager (not WorkManager): WorkManager is deferrable and can be delayed by hours in Doze,
 * which is wrong for "remind me at 20:00". With the "Alarms & reminders" permission granted the alarm is exact;
 * without it we fall back to setAndAllowWhileIdle (still fires in Doze, only a few minutes late).
 */
object ReminderScheduler {
    private const val EXTRA_KIND = "kind"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun pending(context: Context, kind: ReminderKind): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction("com.minhtrong67.moody.REMINDER_${kind.name}").putExtra(EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(context, kind.code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun kindOf(intent: Intent): ReminderKind? = runCatching { ReminderKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: "") }.getOrNull()

    private fun scheduleAtMillis(context: Context, kind: ReminderKind, millis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, kind)
        try {
            if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (e: SecurityException) {
            Log.w("Moody", "Exact alarm denied, falling back", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancel(context: Context, kind: ReminderKind) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, kind))
    }

    fun nextDaily(hhmm: String, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val t = runCatching { LocalTime.parse(hhmm) }.getOrDefault(LocalTime.of(20, 0))
        val today = now.toLocalDate().atTime(t)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun nextWeekly(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val mon = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(9, 0)
        return if (mon.isAfter(now)) mon else mon.plusWeeks(1)
    }

    fun nextMonthly(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val first = now.toLocalDate().withDayOfMonth(1).atTime(9, 0)
        return if (first.isAfter(now)) first else first.plusMonths(1)
    }

    private fun LocalDateTime.millis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Re-registers every enabled reminder and cancels the disabled ones (idempotent, cheap). */
    suspend fun reschedule(context: Context, store: SettingsStore) {
        val s = store.settings.first()
        if (s.dailyEnabled) scheduleAtMillis(context, ReminderKind.Daily, nextDaily(s.dailyTime).millis()) else cancel(context, ReminderKind.Daily)
        if (s.weeklyEnabled) scheduleAtMillis(context, ReminderKind.Weekly, nextWeekly().millis()) else cancel(context, ReminderKind.Weekly)
        if (s.monthlyEnabled) scheduleAtMillis(context, ReminderKind.Monthly, nextMonthly().millis()) else cancel(context, ReminderKind.Monthly)
    }

    /** Fires the whole alarm → receiver → notification path N seconds from now (used by the "test" button). */
    fun scheduleTest(context: Context, seconds: Int = 10) {
        scheduleAtMillis(context, ReminderKind.Test, System.currentTimeMillis() + seconds * 1000L)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = ReminderScheduler.kindOf(intent) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val store = (context.applicationContext as MoodyApp).container.settingsStore
                val s = store.settings.first()
                val enabled = when (kind) {
                    ReminderKind.Daily -> s.dailyEnabled
                    ReminderKind.Weekly -> s.weeklyEnabled
                    ReminderKind.Monthly -> s.monthlyEnabled
                    ReminderKind.Test -> true
                }
                // Don't nag people who already checked in today.
                val skip = kind == ReminderKind.Daily && s.checkedInDate == todayISO()
                if (enabled && !skip) Notifications.show(context, kind, s.language)
                if (kind != ReminderKind.Test) ReminderScheduler.reschedule(context, store)   // chain the next occurrence
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are wiped by reboot / app update / clock or timezone changes / permission changes: put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.reschedule(context, (context.applicationContext as MoodyApp).container.settingsStore)
            } finally {
                pending.finish()
            }
        }
    }
}
