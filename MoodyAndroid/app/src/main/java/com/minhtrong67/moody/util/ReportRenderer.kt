package com.minhtrong67.moody.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

class ReportSlice(val emoji: String, val label: String, val color: Int, val count: Int)

class ReportColors(
    val bgTop: Int, val bgBottom: Int, val card: Int, val cardStroke: Int,
    val onSurface: Int, val onSurfaceVariant: Int,
    val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
    val track: Int,
)

/** Everything the poster shows, already localized (strings come from the app resources). */
class ReportData(
    val appName: String, val title: String, val periodLabel: String,
    val avg: Double?, val avgEmoji: String, val avgLabel: String, val avgCaption: String,
    val days: Int, val daysLabel: String, val streak: Int, val streakLabel: String,
    val topValue: String, val topLabel: String,
    val moodTitle: String, val moods: List<ReportSlice>,
    val trendTitle: String, val trend: List<Double?>?, val trendLabels: List<String>, val scoreColors: Map<Int, Int>,
    val activitiesTitle: String, val activities: List<ReportSlice>,
    val footer: String,
)

/**
 * Draws the statistics poster: 1080 x 1920 px (9:16), pure Canvas — independent from the on-screen UI, so it looks the
 * same on every phone and costs nothing while browsing. Call off the main thread.
 */
object ReportRenderer {
    const val W = 1080
    const val H = 1920
    private const val M = 64f           // outer margin
    private const val CARD_R = 56f

    fun render(ctx: Context, d: ReportData, c: ReportColors, logo: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // background + soft decorative glow
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), c.bgTop, c.bgBottom, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = android.graphics.RadialGradient(W * 0.95f, 40f, 620f, withAlpha(c.primary, 46), 0, Shader.TileMode.CLAMP)
        cv.drawCircle(W * 0.95f, 40f, 620f, p)
        p.shader = null

        header(cv, p, d, c, logo)
        var y = 250f

        // ---- hero: average mood ----
        val heroH = 270f
        card(cv, p, M, y, W - M, y + heroH, c.primaryContainer, null, c)
        val cx = M + 40f + 100f
        val cy = y + heroH / 2
        p.color = withAlpha(c.onPrimaryContainer, 28); cv.drawCircle(cx, cy, 100f, p)
        text(cv, p, d.avgEmoji, cx, cy + 44f, 120f, c.onPrimaryContainer, align = Paint.Align.CENTER)
        text(cv, p, d.avgCaption, M + 290f, y + 92f, 34f, withAlpha(c.onPrimaryContainer, 200))
        val big = d.avg?.let { "%.1f".format(java.util.Locale.US, it) } ?: "—"
        val bigW = text(cv, p, big, M + 290f, y + 205f, 150f, c.onPrimaryContainer, bold = true)
        text(cv, p, "/5", M + 290f + bigW + 8f, y + 205f, 56f, withAlpha(c.onPrimaryContainer, 190), bold = true)
        text(cv, p, d.avgLabel, M + 290f, y + 250f, 36f, c.onPrimaryContainer, bold = true)
        y += heroH + 25f

        // ---- KPI row ----
        val kpiH = 160f
        val gap = 24f
        val kw = (W - 2 * M - 2 * gap) / 3f
        listOf(
            Triple(d.days.toString(), d.daysLabel, 0),
            Triple(d.streak.toString(), d.streakLabel, 1),
            Triple(d.topValue, d.topLabel, 2),
        ).forEachIndexed { i, (v, label, _) ->
            val x0 = M + i * (kw + gap)
            card(cv, p, x0, y, x0 + kw, y + kpiH, c.card, c.cardStroke, c, radius = 44f)
            text(cv, p, v, x0 + kw / 2, y + 82f, if (v.length > 6) 44f else 64f, c.onSurface, bold = true, align = Paint.Align.CENTER, maxWidth = kw - 28f)
            text(cv, p, label, x0 + kw / 2, y + 128f, 26f, c.onSurfaceVariant, align = Paint.Align.CENTER, maxWidth = kw - 28f)
        }
        y += kpiH + 25f

        // ---- mood distribution (donut + legend) ----
        val moodH = 380f
        card(cv, p, M, y, W - M, y + moodH, c.card, c.cardStroke, c)
        text(cv, p, d.moodTitle, M + 40f, y + 68f, 36f, c.onSurface, bold = true)
        val total = d.moods.sumOf { it.count }
        val dcx = M + 40f + 150f
        val dcy = y + 70f + 150f + 20f
        donut(cv, p, dcx, dcy, 140f, 84f, d.moods, total, c)
        text(cv, p, total.toString(), dcx, dcy + 18f, 56f, c.onSurface, bold = true, align = Paint.Align.CENTER)
        val pcts = roundedPercents(d.moods.map { it.count })
        val lx = M + 40f + 330f
        d.moods.forEachIndexed { i, s ->
            val ry = y + 118f + i * 54f
            p.color = s.color; cv.drawCircle(lx + 10f, ry - 10f, 10f, p)
            text(cv, p, s.emoji, lx + 40f, ry + 2f, 34f, c.onSurface)
            text(cv, p, s.label, lx + 92f, ry, 30f, c.onSurface, maxWidth = 250f)
            text(cv, p, "${s.count}  ·  ${pcts[i]}%", W - M - 40f, ry, 28f, c.onSurfaceVariant, align = Paint.Align.RIGHT)
        }
        y += moodH + 25f

        // ---- trend ----
        if (d.trend != null && d.trend.any { it != null }) {
            val trendH = 290f
            card(cv, p, M, y, W - M, y + trendH, c.card, c.cardStroke, c)
            text(cv, p, d.trendTitle, M + 40f, y + 68f, 36f, c.onSurface, bold = true)
            trendChart(cv, p, d, c, M + 110f, y + 100f, W - M - 50f, y + trendH - 56f)
            y += trendH + 25f
        }

        // ---- activities ----
        if (d.activities.isNotEmpty()) {
            val n = min(d.activities.size, if (d.trend != null && d.trend.any { it != null }) 4 else 5)
            val actH = 100f + n * 64f
            card(cv, p, M, y, W - M, y + actH, c.card, c.cardStroke, c)
            text(cv, p, d.activitiesTitle, M + 40f, y + 68f, 36f, c.onSurface, bold = true)
            val topCount = max(1, d.activities.take(n).maxOf { it.count })
            val totalAct = max(1, d.activities.sumOf { it.count })
            d.activities.take(n).forEachIndexed { i, a ->
                val ry = y + 128f + i * 64f
                text(cv, p, a.emoji, M + 40f, ry + 4f, 34f, c.onSurface)
                text(cv, p, a.label, M + 92f, ry, 29f, c.onSurface, maxWidth = 210f)
                val bx0 = M + 330f; val bx1 = W - M - 150f
                p.color = withAlpha(a.color, 60); cv.drawRoundRect(RectF(bx0, ry - 22f, bx1, ry - 6f), 8f, 8f, p)
                p.color = a.color; cv.drawRoundRect(RectF(bx0, ry - 22f, bx0 + (bx1 - bx0) * a.count / topCount, ry - 6f), 8f, 8f, p)
                text(cv, p, "${a.count}  ·  ${(100f * a.count / totalAct).toInt()}%", W - M - 40f, ry, 27f, c.onSurfaceVariant, align = Paint.Align.RIGHT)
            }
            y += actH + 25f
        }

        // ---- footer ----
        text(cv, p, d.footer, W / 2f, H - 62f, 28f, withAlpha(c.onSurfaceVariant, 220), align = Paint.Align.CENTER)
        return bmp
    }

    // ------------------------------------------------------------------ pieces

    private fun header(cv: Canvas, p: Paint, d: ReportData, c: ReportColors, logo: Bitmap?) {
        if (logo != null) {
            val r = RectF(M, 84f, M + 100f, 184f)
            val path = Path().apply { addRoundRect(r, 30f, 30f, Path.Direction.CW) }
            cv.save(); cv.clipPath(path); cv.drawBitmap(logo, null, r, Paint(Paint.FILTER_BITMAP_FLAG)); cv.restore()
        }
        text(cv, p, d.appName, M + 124f, 140f, 54f, c.onSurface, bold = true)
        text(cv, p, d.title, M + 124f, 182f, 30f, c.onSurfaceVariant)
        // period pill (right)
        p.textSize = 32f; p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val tw = min(p.measureText(d.periodLabel), 360f)
        val pill = RectF(W - M - tw - 56f, 98f, W - M, 170f)
        p.color = c.primary; cv.drawRoundRect(pill, 36f, 36f, p)
        text(cv, p, d.periodLabel, pill.centerX(), 144f, 32f, c.onPrimary, bold = true, align = Paint.Align.CENTER, maxWidth = 360f)
    }

    private fun card(cv: Canvas, p: Paint, l: Float, t: Float, r: Float, b: Float, fill: Int, stroke: Int?, c: ReportColors, radius: Float = CARD_R) {
        val rect = RectF(l, t, r, b)
        p.style = Paint.Style.FILL; p.color = fill; cv.drawRoundRect(rect, radius, radius, p)
        if (stroke != null) { p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = stroke; cv.drawRoundRect(rect, radius, radius, p); p.style = Paint.Style.FILL }
    }

    /** Draws [s] at (x, baseline y). Returns the drawn width. Long text is shrunk to fit [maxWidth]. */
    private fun text(
        cv: Canvas, p: Paint, s: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, maxWidth: Float = Float.MAX_VALUE,
    ): Float {
        p.style = Paint.Style.FILL; p.shader = null
        p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        p.textSize = size; p.color = color; p.textAlign = align
        var w = p.measureText(s)
        if (w > maxWidth) { p.textSize = size * maxWidth / w; w = maxWidth }
        cv.drawText(s, x, y, p)
        p.textAlign = Paint.Align.LEFT
        return w
    }

    private fun donut(cv: Canvas, p: Paint, cx: Float, cy: Float, outer: Float, inner: Float, slices: List<ReportSlice>, total: Int, c: ReportColors) {
        val oval = RectF(cx - outer, cy - outer, cx + outer, cy + outer)
        p.style = Paint.Style.FILL
        if (total <= 0) { p.color = c.track; cv.drawCircle(cx, cy, outer, p) }
        else {
            var start = -90f
            slices.forEach { s ->
                if (s.count > 0) {
                    val sweep = 360f * s.count / total
                    p.color = s.color; cv.drawArc(oval, start, sweep, true, p)
                    start += sweep
                }
            }
            // thin separators
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = c.card
            start = -90f
            slices.forEach { s -> if (s.count > 0) { cv.drawArc(oval, start, 360f * s.count / total, true, p); start += 360f * s.count / total } }
            p.style = Paint.Style.FILL
        }
        p.color = c.card; cv.drawCircle(cx, cy, inner, p)
    }

    private fun trendChart(cv: Canvas, p: Paint, d: ReportData, c: ReportColors, l: Float, t: Float, r: Float, b: Float) {
        val values = d.trend ?: return
        val n = values.size
        fun xAt(i: Int) = if (n <= 1) (l + r) / 2 else l + (r - l) * i / (n - 1)
        fun yAt(v: Double) = (b - (b - t) * ((v - 1.0) / 4.0)).toFloat()
        // grid + y labels (1 / 3 / 5)
        for (g in listOf(1, 3, 5)) {
            val gy = yAt(g.toDouble())
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = withAlpha(c.onSurfaceVariant, 60); cv.drawLine(l, gy, r, gy, p)
            text(cv, p, g.toString(), l - 18f, gy + 10f, 26f, c.onSurfaceVariant, align = Paint.Align.RIGHT)
        }
        // line
        val pts = values.mapIndexedNotNull { i, v -> v?.let { Triple(xAt(i), yAt(it), it) } }
        if (pts.size >= 2) {
            val line = Path().apply { moveTo(pts[0].first, pts[0].second); pts.drop(1).forEach { lineTo(it.first, it.second) } }
            val area = Path(line).apply { lineTo(pts.last().first, b); lineTo(pts.first().first, b); close() }
            p.style = Paint.Style.FILL; p.shader = LinearGradient(0f, t, 0f, b, withAlpha(c.primary, 90), withAlpha(c.primary, 0), Shader.TileMode.CLAMP)
            cv.drawPath(area, p); p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = c.primary; p.strokeJoin = Paint.Join.ROUND; p.strokeCap = Paint.Cap.ROUND
            cv.drawPath(line, p)
        }
        p.style = Paint.Style.FILL
        pts.forEach { (x, y, v) ->
            p.color = c.card; cv.drawCircle(x, y, if (n > 14) 8f else 13f, p)
            p.color = d.scoreColors[Math.round(v).toInt().coerceIn(1, 5)] ?: c.primary; cv.drawCircle(x, y, if (n > 14) 5f else 9f, p)
        }
        // x labels: first / middle / last (or every point when few)
        val idxs = if (n <= 7) (0 until n).toList() else listOf(0, n / 2, n - 1)
        idxs.forEach { i -> text(cv, p, d.trendLabels.getOrElse(i) { "" }, xAt(i), b + 42f, 25f, c.onSurfaceVariant, align = Paint.Align.CENTER) }
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    // ------------------------------------------------------------------ saving

    /** Saves to Pictures/Moody (visible in the gallery). Returns the Uri, or null when it could only be cached (Android 9-). */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Moody")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("insert failed")
            try {
                resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("no stream")
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null); throw e
            }
        } else {
            // Android 8/9 would need the legacy storage permission: fall back to the share sheet.
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val f = File(dir, fileName).also { file -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        }
    }
}
