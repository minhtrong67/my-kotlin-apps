package com.minhtrong67.moody.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.minhtrong67.moody.MainActivity
import com.minhtrong67.moody.MoodyApp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class ReminderKind(val workName: String, val notifId: Int) {
    Daily("moody_reminder_daily", 1001),
    Weekly("moody_reminder_weekly", 1002),
    Monthly("moody_reminder_monthly", 1003),
    Test("moody_reminder_test", 1099),
}

object Notifications {
    const val CHANNEL_ID = "moody_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, kind: ReminderKind) {
        if (!canPost(context)) return
        ensureChannel(context)
        val (title, body, route) = when (kind) {
            ReminderKind.Daily -> Triple(R.string.notif_daily_title, R.string.notif_daily_body, "checkin")
            ReminderKind.Weekly -> Triple(R.string.notif_weekly_title, R.string.notif_weekly_body, "stats")
            ReminderKind.Monthly -> Triple(R.string.notif_monthly_title, R.string.notif_monthly_body, "stats")
            ReminderKind.Test -> Triple(R.string.notif_test_title, R.string.notif_test_body, "today")
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        val pi = PendingIntent.getActivity(
            context, kind.notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_moody)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(body)))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(kind.notifId, n)
    }
}

/** Schedules the next occurrence of each enabled reminder with WorkManager (works with the app closed). */
object ReminderScheduler {
    private fun nextDaily(hhmm: String, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val t = runCatching { LocalTime.parse(hhmm) }.getOrDefault(LocalTime.of(20, 0))
        val today = now.toLocalDate().atTime(t)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun nextWeekly(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val thisMon = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(9, 0)
        return if (thisMon.isAfter(now)) thisMon else thisMon.plusWeeks(1)
    }

    private fun nextMonthly(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val first = now.toLocalDate().withDayOfMonth(1).atTime(9, 0)
        return if (first.isAfter(now)) first else first.plusMonths(1)
    }

    private fun enqueue(context: Context, kind: ReminderKind, at: LocalDateTime, policy: ExistingWorkPolicy) {
        val delay = Duration.between(LocalDateTime.now(), at).coerceAtLeast(Duration.ofSeconds(5))
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_KIND, kind.name).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(kind.workName, policy, req)
    }

    /** [runningKind]: the reminder whose worker is calling us (its own slot is appended, not replaced). */
    suspend fun reschedule(context: Context, store: SettingsStore, runningKind: ReminderKind? = null) {
        val s = store.settings.first()
        val wm = WorkManager.getInstance(context)
        fun policy(k: ReminderKind) = if (k == runningKind) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        if (s.dailyEnabled) enqueue(context, ReminderKind.Daily, nextDaily(s.dailyTime), policy(ReminderKind.Daily))
        else wm.cancelUniqueWork(ReminderKind.Daily.workName)
        if (s.weeklyEnabled) enqueue(context, ReminderKind.Weekly, nextWeekly(), policy(ReminderKind.Weekly))
        else wm.cancelUniqueWork(ReminderKind.Weekly.workName)
        if (s.monthlyEnabled) enqueue(context, ReminderKind.Monthly, nextMonthly(), policy(ReminderKind.Monthly))
        else wm.cancelUniqueWork(ReminderKind.Monthly.workName)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = runCatching { ReminderKind.valueOf(inputData.getString(KEY_KIND) ?: "") }.getOrNull() ?: return Result.success()
        val store = (applicationContext as MoodyApp).container.settingsStore
        val s = store.settings.first()
        val stillEnabled = when (kind) {
            ReminderKind.Daily -> s.dailyEnabled
            ReminderKind.Weekly -> s.weeklyEnabled
            ReminderKind.Monthly -> s.monthlyEnabled
            ReminderKind.Test -> true
        }
        if (stillEnabled) Notifications.show(applicationContext, kind)
        // Chain: schedule the next occurrence (a one-shot job re-enqueues itself).
        if (kind != ReminderKind.Test) ReminderScheduler.reschedule(applicationContext, store, kind)
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
    }
}
