@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.MoodConfig
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalLocale
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.LocalToaster
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.EmptyText
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SegmentedTabs
import com.minhtrong67.moody.ui.components.ScrollToTopButton
import com.minhtrong67.moody.ui.components.WavyProgress
import com.minhtrong67.moody.ui.components.rememberWavePhase
import com.minhtrong67.moody.ui.theme.LocalIsDark
import com.minhtrong67.moody.util.Period
import com.minhtrong67.moody.util.Slice
import com.minhtrong67.moody.util.calcStreak
import com.minhtrong67.moody.util.entriesInPeriod
import com.minhtrong67.moody.util.fullDate
import com.minhtrong67.moody.util.latestByDate
import com.minhtrong67.moody.util.moodDistribution
import com.minhtrong67.moody.util.roundedPercents
import com.minhtrong67.moody.util.shiftCursor
import com.minhtrong67.moody.util.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StatsScreen(entries: List<Entry>?, modifier: Modifier = Modifier) {
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val locale = LocalLocale.current
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val dark = LocalIsDark.current
    val cs = MaterialTheme.colorScheme

    var periodKey by rememberSaveable { mutableStateOf("month") }
    var cursorText by rememberSaveable { mutableStateOf(today().toString()) }
    var exporting by remember { mutableStateOf<String?>(null) }
    val period = Period.entries.first { it.key == periodKey }
    val cursor = LocalDate.parse(cursorText)
    val layer = rememberGraphicsLayer()

    val label = when (period) {
        Period.Day -> fullDate(cursor, locale)
        Period.Week -> com.minhtrong67.moody.util.daysOfWeek(cursor).let { "${it.first()} → ${it.last()}" }
        Period.Month -> stringResource(R.string.stats_monthLabel, cursor.monthValue, cursor.year)
        Period.Year -> stringResource(R.string.stats_yearLabel, cursor.year)
    }
    val periodWord = stringResource(when (period) {
        Period.Day -> R.string.stats_word_day; Period.Week -> R.string.stats_word_week
        Period.Month -> R.string.stats_word_month; Period.Year -> R.string.stats_word_year
    })
    val failedMsg = stringResource(R.string.stats_exportFailed)

    // The report layer is recorded ONLY while exporting. Recording it permanently would re-draw this whole tall page twice
    // on every frame (the wave animation invalidates it 60x/s) and starve the RenderThread.
    var capturing by remember { mutableStateOf(false) }
    suspend fun captureAndExport(pdf: Boolean) {
        try {
            capturing = true
            withFrameNanos { }            // frame 1: draw pass records the layer
            withFrameNanos { }            // frame 2: recorded content is ready
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            capturing = false
            exportReport(ctx, bitmap, label, pdf)
        } finally {
            capturing = false
        }
    }

    val scroll = rememberScrollState()
    val showTop by remember { androidx.compose.runtime.derivedStateOf { scroll.value > 500 } }
    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        SegmentedTabs(
            options = Period.entries.map { it.key to stringResource(when (it) {
                Period.Day -> R.string.stats_period_day; Period.Week -> R.string.stats_period_week
                Period.Month -> R.string.stats_period_month; Period.Year -> R.string.stats_period_year
            }) },
            selected = periodKey, onSelect = { periodKey = it },
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { cursorText = shiftCursor(period, cursor, -1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.stats_prev)) }
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { cursorText = shiftCursor(period, cursor, 1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.stats_next)) }
        }

        if (entries == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val byDate = remember(entries) { latestByDate(entries) }
        val periodEntries = remember(period, cursor, byDate) { entriesInPeriod(period, cursor, byDate) }
        val hasData = periodEntries.isNotEmpty()
        val distribution = remember(periodEntries, moods) { moodDistribution(periodEntries, moods) }
        val palette = (if (dark) MoodConfig.CATEGORY_DARK else MoodConfig.CATEGORY_LIGHT).map { Color(it) }
        val activitySlices = remember(periodEntries, activities, dark) {
            val counts = HashMap<String, Int>()
            periodEntries.forEach { e -> e.activities.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
            counts.entries.sortedByDescending { it.value }.mapIndexed { i, (id, c) ->
                val a = activities.firstOrNull { it.id == id }
                Slice(id, a?.label ?: "—", a?.emoji ?: "❔", palette[i % palette.size], c)
            }
        }
        val avg = if (periodEntries.isEmpty()) null else periodEntries.sumOf { it.score }.toDouble() / periodEntries.size
        val topMood = distribution.maxByOrNull { it.count }?.takeIf { it.count > 0 }

        // Export buttons
        Text(stringResource(R.string.stats_export, periodWord), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val busy = exporting != null
            ActionButton(
                if (exporting == "pdf") stringResource(R.string.stats_creating) else "PDF", Icons.Rounded.PictureAsPdf,
                onClick = {
                    scope.launch {
                        exporting = "pdf"
                        runCatching { captureAndExport(pdf = true) }
                            .onFailure { toaster.show(failedMsg) }
                        exporting = null
                    }
                },
                enabled = hasData && !busy, modifier = Modifier.weight(1f),
            )
            ActionButton(
                if (exporting == "png") stringResource(R.string.stats_creating) else stringResource(R.string.stats_png), Icons.Rounded.Image,
                onClick = {
                    scope.launch {
                        exporting = "png"
                        runCatching { captureAndExport(pdf = false) }
                            .onFailure { toaster.show(failedMsg) }
                        exporting = null
                    }
                },
                tonal = true, enabled = hasData && !busy, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))

        // Captured area (exactly what is exported): stat tiles + 2 charts with legends.
        Column(
            Modifier
                .drawWithContent {
                    if (capturing) {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    } else {
                        drawContent()
                    }
                }
                .background(cs.surface)
                .padding(vertical = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Icons.Rounded.SentimentSatisfied, null, stringResource(R.string.stats_avgMood), avg?.let { "%.1f/5".format(Locale.US, it) } ?: "—", Color(0xFF8FBF7F), Modifier.weight(1f))
                Tile(Icons.Rounded.CalendarMonth, null, stringResource(R.string.stats_checkinDays), periodEntries.size.toString(), Color(0xFF9FB8C8), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Icons.Rounded.LocalFireDepartment, null, stringResource(R.string.stats_streak), calcStreak(byDate).toString(), Color(0xFFF0A34E), Modifier.weight(1f))
                Tile(Icons.Rounded.Favorite, topMood?.emoji, stringResource(R.string.stats_topMood), topMood?.label ?: "—", topMood?.color ?: Color(0xFF7D8CB0), Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            if (!hasData) {
                EmptyText(stringResource(R.string.stats_noData))
            } else {
                ChartSection("${stringResource(R.string.stats_share)} ($label)", distribution.filter { it.count > 0 }, distribution)
                Spacer(Modifier.height(24.dp))
                ChartSection("${stringResource(R.string.stats_activitiesShare)} ($label)", activitySlices, activitySlices)
            }
        }
        Spacer(Modifier.height(96.dp))
    }
    ScrollToTopButton(showTop, { scope.launch { scroll.animateScrollTo(0) } }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun Tile(icon: ImageVector, emoji: String?, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    MoodyCard(modifier, contentPadding = 12.dp) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            if (emoji != null) Text(emoji, fontSize = 20.sp) else Icon(icon, null, tint = color)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChartSection(title: String, pieSlices: List<Slice>, legend: List<Slice>) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    MoodyCard(Modifier.fillMaxWidth()) {
        PieChart(pieSlices, Modifier.fillMaxWidth().height(232.dp))
    }
    Spacer(Modifier.height(12.dp))
    Legend(legend)
}

/** Pie that sweeps in, with "emoji + %" labels outside each slice joined by a short leader line (slices under 3%: emoji only). */
@Composable
private fun PieChart(slices: List<Slice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    val stroke = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = MaterialTheme.colorScheme.onSurface.toArgb()
    val line = MaterialTheme.colorScheme.outline
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(slices.map { it.count }) {
        progress.snapTo(0f)
        progress.animateTo(1f, androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val radius = size.minDimension * 0.33f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 12.sp.toPx(); alpha = (255 * progress.value).toInt() }
        var start = -90f
        slices.forEach { s ->
            val full = 360f * s.count / total
            val sweep = full * progress.value
            drawArc(s.color, start, sweep, true, topLeft, arcSize)
            drawArc(stroke, start, sweep, true, topLeft, arcSize, style = Stroke(2.dp.toPx()))
            if (progress.value > 0.6f) {
                val mid = Math.toRadians((start + full / 2).toDouble())
                val r0 = radius + 4.dp.toPx()
                val r1 = radius + 14.dp.toPx()
                val a = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                drawLine(line.copy(alpha = a), Offset(center.x + (r0 * cos(mid)).toFloat(), center.y + (r0 * sin(mid)).toFloat()),
                    Offset(center.x + (r1 * cos(mid)).toFloat(), center.y + (r1 * sin(mid)).toFloat()), 1.dp.toPx())
                val x = center.x + ((r1 + 3.dp.toPx()) * cos(mid)).toFloat()
                val y = center.y + ((r1 + 3.dp.toPx()) * sin(mid)).toFloat()
                val pct = 100f * s.count / total
                val text = if (pct < 3f) s.emoji else "${s.emoji} ${pct.toInt()}%"
                paint.alpha = (255 * a).toInt()
                paint.textAlign = if (x > center.x) Paint.Align.LEFT else Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(text, x, y + paint.textSize / 3f, paint)
            }
            start += full
        }
    }
}

@Composable
private fun Legend(items: List<Slice>) {
    val pcts = roundedPercents(items.map { it.count })
    val total = items.sumOf { it.count }.coerceAtLeast(1)
    val cs = MaterialTheme.colorScheme
    val phase = rememberWavePhase()
    MoodyCard(Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, it ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(it.color))
                    Text(it.emoji, fontSize = 16.sp)
                    Text(it.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(it.count.toString(), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    Text("${pcts[i]}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.size(width = 44.dp, height = 20.dp), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(6.dp))
                WavyProgress(progress = it.count.toFloat() / total, color = it.color, phase = phase, index = i)
            }
        }
    }
}

/** Saves the captured bitmap as PNG or single-page PDF in the cache and opens the share sheet. */
private suspend fun exportReport(context: Context, captured: Bitmap, label: String, pdf: Boolean) {
    val uriFile = withContext(Dispatchers.IO) {
        val bmp = if (captured.config == Bitmap.Config.HARDWARE) captured.copy(Bitmap.Config.ARGB_8888, false) else captured
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safe = label.replace(Regex("[\\s/,→]+"), "-")
        val file = File(dir, "moody-stats-$safe.${if (pdf) "pdf" else "png"}")
        if (pdf) {
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create())
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
        } else {
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        file
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", uriFile)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = if (pdf) "application/pdf" else "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.stats_shareFile)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
