package com.expenser.app.reminder

import android.Manifest
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expenser.app.ExpenserApp
import com.expenser.app.MainActivity
import com.expenser.app.R
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder"
    const val EXTRA_ADD = "add"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    /** Schedules the next reminder at [time] ("HH:mm"). The worker re-schedules itself after firing. */
    fun schedule(context: Context, time: String) {
        val (h, m) = time.split(":").map { it.toIntOrNull() ?: 0 }.let { (it.getOrNull(0) ?: 6) to (it.getOrNull(1) ?: 0) }
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val bodies = context.resources.getStringArray(R.array.reminder_bodies)
        val body = bodies[Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % bodies.size]
        fun intent(type: String, code: Int) = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ADD, type).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent("expense", 1))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.reminder_add_expense), intent("expense", 1))
            .addAction(0, context.getString(R.string.reminder_add_income), intent("income", 2))
            .build()
        NotificationManagerCompat.from(context).notify(1001, n)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = (applicationContext as ExpenserApp).settingsStore.flow.first()
        if (!settings.reminderEnabled) return Result.success()
        ReminderScheduler.show(applicationContext)
        ReminderScheduler.schedule(applicationContext, settings.reminderTime) // next day
        return Result.success()
    }
}
