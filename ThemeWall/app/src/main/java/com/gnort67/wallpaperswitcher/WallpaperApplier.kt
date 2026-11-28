package com.gnort67.wallpaperswitcher

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File
import java.util.Calendar

object WallpaperApplier {
    /** Slot 0 = main image, 1..4 = morning / afternoon / evening / night. */
    private val BOUNDS = intArrayOf(6, 12, 18, 22)

    fun file(ctx: Context, setId: String, dark: Boolean, slot: Int = 0) =
        File(ctx.filesDir, "wp_${setId}_${if (dark) "d" else "l"}_$slot.jpg")

    fun deleteSet(ctx: Context, setId: String) {
        ctx.filesDir.listFiles { f -> f.name.startsWith("wp_${setId}_") }?.forEach { it.delete() }
    }

    fun isSystemDark(): Boolean =
        Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    fun currentSlot(): Int {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when { h < 6 -> 4; h < 12 -> 1; h < 18 -> 2; h < 22 -> 3; else -> 4 }
    }

    fun minutesNow(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** Dark from [darkFrom] until [lightFrom] (both minutes after midnight, wraps over midnight). */
    fun isDarkAt(lightFrom: Int, darkFrom: Int, m: Int): Boolean = when {
        lightFrom == darkFrom -> false
        darkFrom > lightFrom -> m >= darkFrom || m < lightFrom
        else -> m >= darkFrom && m < lightFrom
    }

    /** Effective mode: system theme, or the user's own clock times. */
    fun isDarkNow(ctx: Context): Boolean {
        val p = Prefs.get(ctx)
        return if (p.modeSource == 1) isDarkAt(p.lightFrom, p.darkFrom, minutesNow()) else isSystemDark()
    }

    fun needsAlarm(ctx: Context): Boolean {
        val p = Prefs.get(ctx)
        return p.scheduleEnabled || p.modeSource == 1
    }

    /** Next moment (time-of-day slot edge or custom light/dark time) at which the wallpaper may change. */
    fun nextBoundary(ctx: Context): Long {
        val p = Prefs.get(ctx)
        val marks = buildList {
            if (p.scheduleEnabled) BOUNDS.forEach { add(it * 60) }
            if (p.modeSource == 1) { add(p.lightFrom); add(p.darkFrom) }
        }
        val now = System.currentTimeMillis()
        var best = Long.MAX_VALUE
        for (m in marks) {
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
            best = minOf(best, c.timeInMillis)
        }
        return best
    }

    fun stateKey(ctx: Context, dark: Boolean): Int =
        (if (dark) 10 else 0) + (if (Prefs.get(ctx).scheduleEnabled) currentSlot() else 0)

    private fun resolve(ctx: Context, dark: Boolean): File? {
        val p = Prefs.get(ctx)
        if (p.scheduleEnabled) {
            val s = file(ctx, p.activeId, dark, currentSlot())
            if (s.exists()) return s
        }
        return file(ctx, p.activeId, dark, 0).takeIf { it.exists() }
    }

    @Synchronized
    fun apply(ctx: Context, dark: Boolean): Boolean {
        val f = resolve(ctx, dark) ?: return false
        val p = Prefs.get(ctx)
        val which = when (p.target) {
            1 -> WallpaperManager.FLAG_SYSTEM
            2 -> WallpaperManager.FLAG_LOCK
            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        val wm = WallpaperManager.getInstance(ctx)
        val dim = if (dark) p.dim else 0f
        return runCatching {
            if (dim > 0.01f) {
                val src = BitmapFactory.decodeFile(f.path) ?: return@runCatching false
                val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(1f - dim, 1f - dim, 1f - dim, 1f) })
                }
                Canvas(out).drawBitmap(src, 0f, 0f, paint)
                src.recycle()
                wm.setBitmap(out, null, true, which)
                out.recycle()
            } else {
                f.inputStream().use { wm.setStream(it, null, true, which) }
            }
            true
        }.getOrDefault(false)
    }
}
