package com.gnort67.calendarpro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Vòng tiến độ dạng sóng (Material 3 Expressive) — sóng trôi chậm quanh vòng. */
@Composable
fun WavyRing(
    progress: Float,
    modifier: Modifier,
    color: Color,
    track: Color,
    stroke: Dp = 14.dp,
    amplitude: Dp = 5.dp,
    waves: Int = 18
) {
    val t = rememberInfiniteTransition(label = "wavy-ring")
    val phase by t.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )
    Canvas(modifier) {
        val sw = stroke.toPx()
        val amp = amplitude.toPx()
        val r = min(size.width, size.height) / 2f - sw / 2f - amp
        val c = Offset(size.width / 2f, size.height / 2f)
        // vòng nền (trơn)
        drawCircle(track, r, c, style = Stroke(sw, cap = StrokeCap.Round))
        val p = progress.coerceIn(0f, 1f)
        if (p <= 0f) return@Canvas
        val sweep = 2 * PI * p
        val steps = max(2, (sweep / (2 * PI) * 480).toInt())
        val path = Path()
        for (i in 0..steps) {
            val a = -PI / 2 + sweep * i / steps
            val rr = (r + amp * sin(waves * (a + PI / 2) + phase)).toFloat()
            val x = c.x + rr * cos(a).toFloat()
            val y = c.y + rr * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Thanh tiến độ ngang dạng sóng. */
@Composable
fun WavyLine(
    progress: Float,
    modifier: Modifier = Modifier.fillMaxWidth().height(16.dp),
    color: Color,
    track: Color,
    stroke: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 22.dp
) {
    val t = rememberInfiniteTransition(label = "wavy-line")
    val phase by t.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )
    Canvas(modifier) {
        val sw = stroke.toPx()
        val amp = amplitude.toPx()
        val cy = size.height / 2f
        val w = size.width - sw
        val end = sw / 2f + w * progress.coerceIn(0f, 1f)
        drawLine(track, Offset(end, cy), Offset(size.width - sw / 2f, cy), sw, StrokeCap.Round)
        if (progress <= 0f) return@Canvas
        val k = (2 * PI / wavelength.toPx()).toFloat()
        val path = Path()
        var x = sw / 2f
        path.moveTo(x, cy + amp * sin(phase))
        while (x < end) {
            x = min(end, x + 3f)
            path.lineTo(x, cy + amp * sin(k * x + phase))
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Hai "đốm màu" mờ trang trí ở góc thẻ (giống các blob trong bản web). */
@Composable
fun BlobDecoration(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        drawCircle(color.copy(alpha = 0.35f), radius = size.minDimension * 0.55f, center = Offset(size.width * 0.92f, size.height * 0.08f))
        drawCircle(color.copy(alpha = 0.25f), radius = size.minDimension * 0.4f, center = Offset(size.width * 0.9f, size.height * 0.95f))
    }
}
