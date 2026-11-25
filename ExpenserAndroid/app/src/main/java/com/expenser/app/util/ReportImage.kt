package com.expenser.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

class ImgSlice(val label: String, val value: Long, val color: Int, val valueText: String, val pctText: String)
class ImgSection(val title: String, val centerTitle: String, val centerValue: String, val slices: List<ImgSlice>)
class ImgSummary(val label: String, val value: String, val color: Int)

/** Draws the statistics report (summary + one donut with a full legend per section) into a PNG-ready bitmap. */
object ReportImage {
    private const val W = 1080
    private const val PAD = 56f

    fun render(appName: String, period: String, summary: List<ImgSummary>, sections: List<ImgSection>, primary: Int, text: Int, muted: Int, bg: Int, track: Int): Bitmap {
        val title = paint(text, 44f, true)
        val sub = paint(muted, 30f)
        val body = paint(text, 32f)
        val bodyBold = paint(text, 32f, true)
        val small = paint(muted, 28f)
        val h2 = paint(text, 38f, true)
        val legendRow = 52f
        val donutBlock = 420f

        var height = PAD + 60f + 40f + 40f + summary.size * 64f + 30f
        sections.forEach { height += 30f + 60f + donutBlock + 20f + it.slices.size * legendRow + 40f }
        height += 70f
        val bmp = Bitmap.createBitmap(W, height.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bg)

        var y = PAD + 44f
        title.color = primary
        c.drawText(appName, PAD, y, title)
        y += 46f
        c.drawText(period, PAD, y, sub)
        y += 50f
        summary.forEach { s ->
            c.drawText(s.label, PAD, y, body)
            val p = paint(s.color, 34f, true); p.textAlign = Paint.Align.RIGHT
            c.drawText(s.value, W - PAD, y, p)
            y += 64f
        }
        y += 10f
        sections.forEach { sec ->
            y += 30f
            c.drawText(sec.title, PAD, y + 20f, h2)
            y += 60f
            val cx = W / 2f; val cy = y + donutBlock / 2f; val r = 150f; val stroke = 56f
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke }
            val total = sec.slices.sumOf { it.value }.toFloat()
            if (total <= 0f) { ring.color = track; c.drawArc(rect, 0f, 360f, false, ring) }
            else {
                var start = -90f
                val visible = sec.slices.filter { it.value > 0 }
                val gap = if (visible.size > 1) 2f else 0f
                visible.forEach { s ->
                    val sweep = s.value / total * 360f
                    ring.color = s.color
                    c.drawArc(rect, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, ring)
                    start += sweep
                }
            }
            val ct = paint(muted, 26f); ct.textAlign = Paint.Align.CENTER
            val cv = paint(text, 36f, true); cv.textAlign = Paint.Align.CENTER
            c.drawText(sec.centerTitle, cx, cy - 8f, ct)
            c.drawText(sec.centerValue, cx, cy + 34f, cv)
            y += donutBlock + 20f
            sec.slices.forEach { s ->
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color }
                c.drawCircle(PAD + 12f, y - 10f, 12f, dot)
                val name = ellipsize(s.label, body, 470f)
                c.drawText(name, PAD + 40f, y, body)
                val pct = paint(muted, 28f); pct.textAlign = Paint.Align.RIGHT
                c.drawText(s.pctText, W - PAD - 220f, y, pct)
                val v = paint(text, 30f, true); v.textAlign = Paint.Align.RIGHT
                c.drawText(s.valueText, W - PAD, y, v)
                y += legendRow
            }
            y += 40f
        }
        val foot = paint(muted, 24f); foot.textAlign = Paint.Align.CENTER
        c.drawText("Expenser", W / 2f, height - 28f, foot)
        return bmp
    }

    private fun ellipsize(s: String, p: Paint, max: Float): String {
        if (p.measureText(s) <= max) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
