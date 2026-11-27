package com.gnort67.calendarpro.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.CalendarContract
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

object Sharing {
    /** Mở màn hình "Thêm sự kiện" của ứng dụng Lịch hệ thống (cả ngày, lặp hằng năm cho ngày dương lịch). */
    fun addToCalendar(ctx: Context, title: String, date: LocalDate, yearly: Boolean): Boolean {
        val begin = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val i = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 86_400_000L)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            if (yearly) putExtra(CalendarContract.Events.RRULE, "FREQ=YEARLY")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try { ctx.startActivity(i); true } catch (e: ActivityNotFoundException) { false }
    }

    /** Vẽ thẻ ảnh 1080x1350 và mở hộp thoại chia sẻ. Trả về false nếu lỗi. */
    fun shareCard(
        ctx: Context, title: String, dateText: String, big: String, small: String, footer: String,
        c1: Int, c2: Int, fg: Int
    ): Boolean = try {
        val w = 1080
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), c1, c2, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; alpha = 28 }
        c.drawCircle(w * 0.88f, h * 0.08f, 360f, deco)
        c.drawCircle(w * 0.1f, h * 0.95f, 300f, deco)

        fun text(s: String, size: Float, bold: Boolean, alpha: Int = 255): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fg; this.alpha = alpha; textSize = size
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }
        fun draw(s: String, p: TextPaint, top: Float): Float {
            val l = StaticLayout.Builder.obtain(s, 0, s.length, p, 900).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            c.save(); c.translate(90f, top); l.draw(c); c.restore()
            return top + l.height
        }
        var y = 170f
        y = draw(title, text(title, 88f, true), y) + 36f
        y = draw(dateText, text(dateText, 46f, false, 220), y) + 110f
        y = draw(big, text(big, 300f, true), y) + 10f
        draw(small, text(small, 64f, false, 230), y)
        val f = text(footer, 38f, true, 180)
        draw(footer, f, h - 140f)

        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "calendar-pro-${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$title — $dateText")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
