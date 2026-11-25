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

    fun file(ctx: Context, dark: Boolean, slot: Int = 0) =
        File(ctx.filesDir, "wp_${if (dark) "d" else "l"}_$slot.jpg")

    fun isSystemDark(): Boolean =
        Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    fun currentSlot(): Int {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when { h < 6 -> 4; h < 12 -> 1; h < 18 -> 2; h < 22 -> 3; else -> 4 }
    }

    fun nextBoundary(): Long {
        val c = Calendar.getInstance()
        val now = c.timeInMillis
        c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        for (h in BOUNDS) {
            c.set(Calendar.HOUR_OF_DAY, h)
            if (c.timeInMillis > now) return c.timeInMillis
        }
        c.add(Calendar.DAY_OF_YEAR, 1)
        c.set(Calendar.HOUR_OF_DAY, BOUNDS[0])
        return c.timeInMillis
    }

    fun stateKey(ctx: Context, dark: Boolean): Int =
        (if (dark) 10 else 0) + (if (Prefs.get(ctx).scheduleEnabled) currentSlot() else 0)

    private fun resolve(ctx: Context, dark: Boolean): File? {
        if (Prefs.get(ctx).scheduleEnabled) {
            val s = file(ctx, dark, currentSlot())
            if (s.exists()) return s
        }
        return file(ctx, dark, 0).takeIf { it.exists() }
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
