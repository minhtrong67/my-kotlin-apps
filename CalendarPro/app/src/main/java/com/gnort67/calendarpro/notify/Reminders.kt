package com.gnort67.calendarpro.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gnort67.calendarpro.MainActivity
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.HolidayLogic
import com.gnort67.calendarpro.core.LunarCalendar
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

/** Thông báo nhắc hằng ngày: lên lịch bằng AlarmManager, tự đặt lại sau mỗi lần phát và sau khi khởi động máy. */
object Reminders {
    const val ACTION = "com.gnort67.calendarpro.DAILY_REMINDER"
    private const val CHANNEL = "daily_reminder"
    private const val REQUEST_CODE = 1001
    private const val ID_DAILY = 1
    private const val ID_TEST = 2

    fun canNotify(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST_CODE, Intent(ctx, ReminderReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Đặt báo thức cho lần kế tiếp lúc [hour]:[minute] (hôm nay nếu chưa qua, ngược lại là ngày mai). */
    fun schedule(ctx: Context, hour: Int, minute: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        // Không cần quyền báo thức chính xác: sai lệch vài phút là chấp nhận được cho lời nhắc hằng ngày.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), pending(ctx))
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pending(ctx))
    }

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = ctx.getString(R.string.notif_channel_desc)
            }
        )
    }

    private fun localized(ctx: Context, lang: String): Context {
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(lang))
        return ctx.createConfigurationContext(cfg)
    }

    /** Trả về false nếu hệ thống đang chặn thông báo. */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, lang: String, catalog: HolidayCatalog, events: List<PersonalEvent>, test: Boolean = false): Boolean {
        if (!canNotify(ctx)) return false
        ensureChannel(ctx)
        val lc = localized(ctx, lang)
        val (title, body) = if (test) {
            lc.getString(R.string.notif_test_title) to lc.getString(R.string.notif_test_body)
        } else compose(lc, lang, catalog, events, LocalDate.now())

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(ctx).notify(if (test) ID_TEST else ID_DAILY, n)
        return true
    }

    /** Nội dung ngắn: "Hôm nay 18/10 âm lịch" + (ngày lễ/sự kiện | sao ngày) + ngày chay + sự kiện ngày mai. */
    private fun compose(lc: Context, lang: String, catalog: HolidayCatalog, events: List<PersonalEvent>, date: LocalDate): Pair<String, String> {
        val l = DateUtils.getLunarInfo(date)
        val names = HolidayLogic.getHolidaysOnDate(catalog.holidays, date, l).map { catalog.name(it, lang) } +
            events.filter { it.isOn(date, l) }.map { it.name }
        val parts = ArrayList<String>()
        if (names.isNotEmpty()) {
            parts += names.joinToString(", ")
        } else {
            val sao = LunarCalendar.saoNgay(date.dayOfMonth, date.monthValue, date.year, l.month)
            parts += lc.getString(if (sao.good) R.string.notif_star_good else R.string.notif_star_bad, sao.name)
        }
        if (l.day in HolidayLogic.NGAY_CHAY_NHI_TRAI) parts += lc.getString(R.string.notif_chay)
        val next = date.plusDays(1)
        val nl = DateUtils.getLunarInfo(next)
        val tomorrow = HolidayLogic.getHolidaysOnDate(catalog.holidays, next, nl).map { catalog.name(it, lang) } +
            events.filter { it.isOn(next, nl) }.map { it.name }
        if (tomorrow.isNotEmpty()) parts += lc.getString(R.string.notif_tomorrow, tomorrow.first())
        return lc.getString(R.string.notif_title, l.day, l.month) to parts.joinToString(" · ")
    }
}
