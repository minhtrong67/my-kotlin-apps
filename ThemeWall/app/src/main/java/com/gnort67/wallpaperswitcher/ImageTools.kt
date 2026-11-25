package com.gnort67.wallpaperswitcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object ImageTools {
    /** Decodes [uri] down to at most ~[maxEdge] px on the long edge and applies EXIF rotation. */
    fun loadWorking(ctx: Context, uri: Uri, maxEdge: Int = 2600): Bitmap? = runCatching {
        val cr = ctx.contentResolver
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, b) }
        val long = max(b.outWidth, b.outHeight)
        if (long <= 0) return@runCatching null
        var s = 1
        while (long / (s * 2) >= maxEdge) s *= 2
        val o = BitmapFactory.Options().apply {
            inSampleSize = s
            val l = long / s
            if (l > maxEdge) { inScaled = true; inDensity = l; inTargetDensity = maxEdge }
        }
        val bmp = cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o) }
            ?: return@runCatching null
        val orientation = runCatching {
            cr.openInputStream(uri)!!.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { m.setRotate(180f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.setRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.setRotate(-90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(-90f)
            else -> return@runCatching bmp
        }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (rotated !== bmp) bmp.recycle()
        rotated
    }.getOrNull()

    /** Crops [r] out of [src], scales to [outW]x[outH] and writes a JPEG atomically to [dst]. */
    fun cropAndSave(src: Bitmap, r: RectF, outW: Int, outH: Int, dst: File): Boolean = runCatching {
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val from = Rect(
            floor(r.left).toInt().coerceIn(0, src.width - 1),
            floor(r.top).toInt().coerceIn(0, src.height - 1),
            ceil(r.right).toInt().coerceIn(1, src.width),
            ceil(r.bottom).toInt().coerceIn(1, src.height)
        )
        Canvas(out).drawBitmap(src, from, Rect(0, 0, outW, outH), Paint(Paint.FILTER_BITMAP_FLAG))
        val tmp = File(dst.parentFile, dst.name + ".tmp")
        tmp.outputStream().buffered().use { out.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        out.recycle()
        dst.delete()
        tmp.renameTo(dst)
    }.getOrDefault(false)
}
