package com.expenser.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

class RSlice(val label: String, val value: Long, val color: Int, val valueText: String, val pct: Int)
class RSection(val title: String, val centerLabel: String, val centerValue: String, val slices: List<RSlice>)

class ReportData(
    val appName: String, val title: String, val period: String, val generated: String,
    val incomeLabel: String, val incomeText: String, val incomeColor: Int,
    val expenseLabel: String, val expenseText: String, val expenseColor: Int,
    val balanceLabel: String, val balanceText: String, val balanceColor: Int,
    val topExpenseTitle: String, val topExpense: RSlice?,
    val topIncomeTitle: String, val topIncome: RSlice?,
    val expense: RSection, val income: RSection, val emptyText: String,
)

/** Draws the statistics report as a clean, fixed light-theme infographic (header, totals, top categories, donut + stat cards). */
object ReportImage {
    private const val W = 1080
    private const val M = 48f                 // outer margin
    private const val BG = 0xFFF3F6FB.toInt()
    private const val CARD = 0xFFFFFFFF.toInt()
    private const val INK = 0xFF0F172A.toInt()
    private const val MUTED = 0xFF64748B.toInt()
    private const val LINE = 0xFFE2E8F0.toInt()
    private const val H1 = 0xFF1E40AF.toInt()
    private const val H2 = 0xFF2563EB.toInt()
    private const val H3 = 0xFF38BDF8.toInt()

    private const val H = 1920 // 9:16 portrait

    fun render(d: ReportData): Bitmap {
        val natural = layout(null, d, 0f)
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (natural > H) {            // very unlikely: shrink to fit instead of cutting content
            val k = H / natural
            c.scale(k, k)
            layout(c, d, 0f)
        } else layout(c, d, (H - natural) / 4f) // spread the spare height evenly between the blocks
        return bmp
    }

    // ---- drawing helpers (all no-ops when c == null, so the same code measures and draws) ----
    private fun p(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; textAlign = align
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }
    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun card(c: Canvas?, l: Float, t: Float, r: Float, b: Float, color: Int = CARD, radius: Float = 32f) {
        c ?: return
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0x14102A56; maskFilter = null }
        c.drawRoundRect(RectF(l, t + 4f, r, b + 6f), radius, radius, shadow)
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, fill(color))
    }

    private fun fit(s: String, paint: Paint, max: Float): String {
        if (paint.measureText(s) <= max) return s
        var t = s
        while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun layout(c: Canvas?, d: ReportData, extra: Float): Float {
        c?.drawColor(BG)
        // ---------- header ----------
        val headerH = 260f
        if (c != null) {
            val g = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, W.toFloat(), headerH, intArrayOf(H1, H2, H3), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP) }
            c.drawRect(0f, 0f, W.toFloat(), headerH + 40f, g)
            c.drawCircle(W - 90f, 30f, 170f, fill(0x22FFFFFF))
            c.drawCircle(W - 260f, headerH - 10f, 100f, fill(0x16FFFFFF))
            c.drawRoundRect(RectF(M, 44f, M + 64f, 108f), 20f, 20f, fill(0x33FFFFFF))
            c.drawText("₫", M + 32f, 90f, p(0xFFFFFFFF.toInt(), 40f, true, Paint.Align.CENTER))
            c.drawText(d.appName, M + 84f, 88f, p(0xFFFFFFFF.toInt(), 34f, true))
            c.drawText(d.generated, W - M, 88f, p(0xBBFFFFFF.toInt(), 24f, false, Paint.Align.RIGHT))
            c.drawText(d.title, M, 176f, p(0xFFFFFFFF.toInt(), 54f, true))
            c.drawText(d.period, M, 222f, p(0xCCFFFFFF.toInt(), 30f))
        }
        var y = headerH - 36f

        // ---------- totals ----------
        val gap = 18f
        val cw = (W - 2 * M - 2 * gap) / 3f
        val ch = 150f
        listOf(
            Triple(d.incomeLabel, d.incomeText, d.incomeColor),
            Triple(d.expenseLabel, d.expenseText, d.expenseColor),
            Triple(d.balanceLabel, d.balanceText, d.balanceColor),
        ).forEachIndexed { i, (label, value, color) ->
            val l = M + i * (cw + gap)
            card(c, l, y, l + cw, y + ch)
            if (c != null) {
                c.drawRoundRect(RectF(l + 22f, y + 26f, l + 32f, y + 54f), 5f, 5f, fill(color))
                c.drawText(fit(label, p(MUTED, 24f, true), cw - 70f), l + 42f, y + 50f, p(MUTED, 24f, true))
                var size = 38f
                val vp = p(color, size, true)
                while (vp.measureText(value) > cw - 40f && size > 20f) { size -= 2f; vp.textSize = size }
                c.drawText(value, l + 22f, y + 112f, vp)
            }
        }
        y += ch + 24f + extra

        // ---------- top categories ----------
        val tw = (W - 2 * M - gap) / 2f
        val th = 172f
        listOf(Triple(d.topExpenseTitle, d.topExpense, d.expenseColor), Triple(d.topIncomeTitle, d.topIncome, d.incomeColor)).forEachIndexed { i, (title, s, color) ->
            val l = M + i * (tw + gap)
            card(c, l, y, l + tw, y + th)
            if (c != null) {
                c.drawRoundRect(RectF(l, y, l + tw, y + th), 32f, 32f, fill(alpha(color, 0x10)))
                c.drawText(title.uppercase(), l + 26f, y + 44f, p(color, 21f, true).apply { letterSpacing = 0.06f })
                if (s == null) c.drawText("—", l + 26f, y + 110f, p(MUTED, 38f, true))
                else {
                    c.drawText(fit(s.label, p(INK, 32f, true), tw - 52f), l + 26f, y + 92f, p(INK, 32f, true))
                    c.drawText(s.valueText, l + 26f, y + 142f, p(color, 36f, true))
                    val chip = "${s.pct}%"
                    val cp = p(color, 22f, true, Paint.Align.CENTER)
                    val cwid = cp.measureText(chip) + 32f
                    c.drawRoundRect(RectF(l + tw - 26f - cwid, y + 112f, l + tw - 26f, y + 148f), 18f, 18f, fill(alpha(color, 0x26)))
                    c.drawText(chip, l + tw - 26f - cwid / 2f, y + 138f, cp)
                }
            }
        }
        y += th + 30f + extra

        y = section(c, d.expense, d.expenseColor, d.emptyText, y)
        y += extra
        y = section(c, d.income, d.incomeColor, d.emptyText, y)
        y += extra

        if (c != null) {
            c.drawLine(M, y + 4f, W - M, y + 4f, Paint().apply { color = LINE; strokeWidth = 2f })
            c.drawText("${d.appName} · ${d.generated}", W / 2f, y + 48f, p(MUTED, 22f, false, Paint.Align.CENTER))
        }
        return y + 80f
    }

    /** Section: title, then ONE card with the donut on the left and up to six stat cards (2 x 3) on the right. */
    private fun section(c: Canvas?, s: RSection, accent: Int, emptyText: String, top: Float): Float {
        var y = top
        c?.let {
            it.drawRoundRect(RectF(M, y + 4f, M + 10f, y + 42f), 5f, 5f, fill(accent))
            it.drawText(s.title, M + 26f, y + 38f, p(INK, 34f, true))
        }
        y += 64f
        val inner = W - 2 * M
        val slices = s.slices.filter { it.value > 0 }.take(6)
        val cardH = 410f
        card(c, M, y, M + inner, y + cardH)
        if (slices.isEmpty()) {
            c?.drawText(emptyText, W / 2f, y + cardH / 2f + 10f, p(MUTED, 28f, false, Paint.Align.CENTER))
            return y + cardH + 16f
        }
        if (c != null) {
            val cx = M + 24f + 150f; val cy = y + cardH / 2f; val r = 104f; val stroke = 50f
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke }
            val total = slices.sumOf { it.value }.toFloat()
            var start = -90f
            val gapDeg = if (slices.size > 1) 2.2f else 0f
            slices.forEach { sl ->
                val sweep = sl.value / total * 360f
                ring.color = sl.color
                c.drawArc(rect, start + gapDeg / 2, (sweep - gapDeg).coerceAtLeast(0.6f), false, ring)
                start += sweep
            }
            var a = -90f
            val lp = p(0xFFFFFFFF.toInt(), 19f, true, Paint.Align.CENTER)
            slices.forEach { sl ->
                val sweep = sl.value / total * 360f
                if (sweep >= 26f) {
                    val mid = Math.toRadians((a + sweep / 2).toDouble())
                    c.drawText("${sl.pct}%", cx + (r * Math.cos(mid)).toFloat(), cy + (r * Math.sin(mid)).toFloat() + 7f, lp)
                }
                a += sweep
            }
            c.drawText(s.centerLabel, cx, cy - 4f, p(MUTED, 21f, false, Paint.Align.CENTER))
            c.drawText(s.centerValue, cx, cy + 30f, p(INK, 30f, true, Paint.Align.CENTER))
        }
        // stat cards
        val left = M + 24f + 300f + 8f
        val gap = 12f
        val cw = (M + inner - 24f - left - gap) / 2f
        val chh = (cardH - 48f - 2 * gap) / 3f
        slices.forEachIndexed { i, sl ->
            val col = i % 2; val row = i / 2
            val l = left + col * (cw + gap); val t = y + 24f + row * (chh + gap)
            if (c != null) {
                c.drawRoundRect(RectF(l, t, l + cw, t + chh), 22f, 22f, fill(alpha(sl.color, 0x14)))
                c.drawCircle(l + 24f, t + 30f, 9f, fill(sl.color))
                c.drawText("${sl.pct}%", l + cw - 18f, t + 38f, p(sl.color, 22f, true, Paint.Align.RIGHT))
                c.drawText(fit(sl.label, p(INK, 24f, true), cw - 110f), l + 44f, t + 38f, p(INK, 24f, true))
                var size = 27f
                val vp = p(INK, size, true)
                while (vp.measureText(sl.valueText) > cw - 36f && size > 18f) { size -= 1f; vp.textSize = size }
                c.drawText(sl.valueText, l + 18f, t + 78f, vp)
                val bl = l + 18f; val br = l + cw - 18f; val bt = t + chh - 18f
                c.drawRoundRect(RectF(bl, bt, br, bt + 8f), 4f, 4f, fill(alpha(sl.color, 0x2A)))
                c.drawRoundRect(RectF(bl, bt, bl + (br - bl) * (sl.pct / 100f).coerceIn(0.02f, 1f), bt + 8f), 4f, 4f, fill(sl.color))
            }
        }
        return y + cardH + 16f
    }
}
