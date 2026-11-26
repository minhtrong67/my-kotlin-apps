package com.expenser.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expenser.app.ExpenserApp
import com.expenser.app.MainActivity
import com.expenser.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Daily reminder driven by AlarmManager (set-and-allow-while-idle: fires near the chosen time even in Doze, needs no special
 * permission). The alarm re-arms itself every time it fires, after a reboot / app update / time-zone change.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "daily_reminder"
    const val EXTRA_ADD = "add"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_CODE = 7001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, ALARM_CODE, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Arms the next alarm at [time] ("HH:mm"), today if still ahead, otherwise tomorrow. Replaces any earlier alarm. */
    fun schedule(context: Context, time: String) {
        val parts = time.split(":").map { it.toIntOrNull() }
        val h = (parts.getOrNull(0) ?: 6).coerceIn(0, 23)
        val m = (parts.getOrNull(1) ?: 0).coerceIn(0, 59)
        val now = System.currentTimeMillis()
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now + 1000) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, alarmIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Posts the reminder now. Returns false when notifications are blocked. */
    fun show(context: Context): Boolean {
        if (!canNotify(context)) return false
        createChannel(context)
        val bodies = context.resources.getStringArray(R.array.reminder_bodies)
        val body = bodies[Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % bodies.size]
        fun open(type: String, code: Int) = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ADD, type).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF0061A4.toInt())
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open("expense", 1))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.reminder_add_expense), open("expense", 1))
            .addAction(0, context.getString(R.string.reminder_add_income), open("income", 2))
            .build()
        return try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n); true } catch (e: SecurityException) { false }
    }

    /** Re-arms the alarm from the saved settings (used after boot / update / app start). */
    suspend fun restore(context: Context) {
        val s = (context.applicationContext as ExpenserApp).settingsStore.flow.first()
        if (s.reminderEnabled) schedule(context, s.reminderTime) else cancel(context)
    }
}

/** Fires at the scheduled time: shows the notification and arms tomorrow's alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val s = (context.applicationContext as ExpenserApp).settingsStore.flow.first()
                if (s.reminderEnabled) {
                    ReminderScheduler.show(context)
                    ReminderScheduler.schedule(context, s.reminderTime)
                }
            } finally { pending.finish() }
        }
    }
}

/** Alarms are wiped by a reboot / app update, and shift with the time zone: re-arm them. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try { ReminderScheduler.restore(context) } finally { pending.finish() }
        }
    }
}
