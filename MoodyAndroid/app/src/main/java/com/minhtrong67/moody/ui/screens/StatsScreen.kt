@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.lerp
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
import com.minhtrong67.moody.ui.LocalActivityContext
import com.minhtrong67.moody.ui.LocalLocale
import com.minhtrong67.moody.util.ReportColors
import com.minhtrong67.moody.util.ReportData
import com.minhtrong67.moody.util.ReportRenderer
import com.minhtrong67.moody.util.ReportSlice
import com.minhtrong67.moody.util.daysOfWeek
import com.minhtrong67.moody.util.isoToDate
import androidx.compose.ui.res.stringArrayResource
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.LocalToaster
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.EmptyText
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SegmentedTabs
import com.minhtrong67.moody.ui.components.ScrollToTopButton
import com.minhtrong67.moody.ui.components.WavyProgress
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

    val savedMsg = stringResource(R.string.report_saved)
    val openLabel = stringResource(R.string.report_open)
    val reportTitle = stringResource(R.string.report_title)
    val topActivitiesTitle = stringResource(R.string.report_topActivities)
    val weekdayLabels = stringArrayResource(R.array.weekdays_short).toList()
    val activityCtx = LocalActivityContext.current
    val generatedFooter = stringResource(R.string.report_generated, java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
    val labels = ReportLabels(
        avgMood = stringResource(R.string.stats_avgMood), days = stringResource(R.string.stats_checkinDays),
        streak = stringResource(R.string.stats_streak), topMood = stringResource(R.string.stats_topMood),
        moodShare = stringResource(R.string.stats_share), trend = stringResource(R.string.report_trend),
    )

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
            com.minhtrong67.moody.ui.components.StatsSkeleton()
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
        ActionButton(
            if (exporting != null) stringResource(R.string.stats_creating) else stringResource(R.string.stats_png), Icons.Rounded.Image,
            onClick = {
                scope.launch {
                    exporting = "png"
                    runCatching {
                        val data = buildReportData(
                            labels, reportTitle, topActivitiesTitle, generatedFooter, label, period, cursor, byDate,
                            periodEntries, distribution, activitySlices, moods, weekdayLabels, calcStreak(byDate),
                        )
                        val colors = reportColors(cs, dark)
                        val logo = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.mipmap.ic_logo)
                        val bitmap = withContext(Dispatchers.Default) { ReportRenderer.render(ctx, data, colors, logo) }
                        val safe = label.replace(Regex("[\\s/,→]+"), "-")
                        ReportRenderer.saveToGallery(ctx, bitmap, "Moody-$safe.png")
                    }.onSuccess { uri ->
                        if (uri != null) toaster.show(savedMsg, openLabel) {
                            runCatching {
                                activityCtx.startActivity(
                                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/png").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    }.onFailure { toaster.show(failedMsg) }
                    exporting = null
                }
            },
            enabled = hasData && exporting == null, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Column(Modifier.padding(vertical = 12.dp)) {
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
                WavyProgress(progress = it.count.toFloat() / total, color = it.color, index = i)
            }
        }
    }
}

private class ReportLabels(val avgMood: String, val days: String, val streak: String, val topMood: String, val moodShare: String, val trend: String)

private fun reportColors(cs: androidx.compose.material3.ColorScheme, dark: Boolean) = ReportColors(
    bgTop = lerp(cs.surface, cs.primaryContainer, if (dark) 0.55f else 0.7f).toArgb(),
    bgBottom = cs.surface.toArgb(),
    card = (if (dark) cs.surfaceContainerHighest else lerp(cs.surfaceContainerLowest, cs.primary, 0.03f)).toArgb(),
    cardStroke = (if (dark) cs.outlineVariant.copy(alpha = 0.4f) else cs.primary.copy(alpha = 0.16f)).toArgb(),
    onSurface = cs.onSurface.toArgb(), onSurfaceVariant = cs.onSurfaceVariant.toArgb(),
    primary = cs.primary.toArgb(), onPrimary = cs.onPrimary.toArgb(),
    primaryContainer = cs.primaryContainer.toArgb(), onPrimaryContainer = cs.onPrimaryContainer.toArgb(),
    track = cs.surfaceContainerHighest.toArgb(),
)

private fun buildReportData(
    l: ReportLabels, title: String, activitiesTitle: String, footer: String, label: String,
    period: Period, cursor: LocalDate, byDate: Map<String, Entry>, periodEntries: List<Entry>,
    distribution: List<Slice>, activitySlices: List<Slice>, moods: List<com.minhtrong67.moody.data.MoodDef>,
    weekdayLabels: List<String>, streak: Int,
): ReportData {
    val avg = if (periodEntries.isEmpty()) null else periodEntries.sumOf { it.score }.toDouble() / periodEntries.size
    val nearest = avg?.let { a -> moods.minByOrNull { kotlin.math.abs(it.score - a) } }
    val top = distribution.maxByOrNull { it.count }?.takeIf { it.count > 0 }

    // trend: week/month = one point per day, year = one point per month, day = none
    val (values, labelsX) = when (period) {
        Period.Day -> null to emptyList()
        Period.Week -> daysOfWeek(cursor).let { days -> days.map { byDate[it.toString()]?.score?.toDouble() } to days.indices.map { weekdayLabels.getOrElse(it) { "" } } }
        Period.Month -> (1..cursor.lengthOfMonth()).let { r -> r.map { byDate[cursor.withDayOfMonth(it).toString()]?.score?.toDouble() } to r.map { it.toString() } }
        Period.Year -> (1..12).let { r ->
            val byMonth = byDate.values.filter { isoToDate(it.date).year == cursor.year }.groupBy { isoToDate(it.date).monthValue }
            r.map { m -> byMonth[m]?.let { list -> list.sumOf { e -> e.score }.toDouble() / list.size } } to r.map { it.toString() }
        }
    }
    return ReportData(
        appName = "Moody", title = title, periodLabel = label,
        avg = avg, avgEmoji = nearest?.emoji ?: "🙂", avgLabel = nearest?.label ?: "—", avgCaption = l.avgMood,
        days = periodEntries.size, daysLabel = l.days, streak = streak, streakLabel = l.streak,
        topValue = top?.let { "${it.emoji} ${it.label}" } ?: "—", topLabel = l.topMood,
        moodTitle = l.moodShare, moods = distribution.map { ReportSlice(it.emoji, it.label, it.color.toArgb(), it.count) },
        trendTitle = l.trend, trend = values, trendLabels = labelsX, scoreColors = moods.associate { it.score to it.color.toArgb() },
        activitiesTitle = activitiesTitle, activities = activitySlices.take(5).map { ReportSlice(it.emoji, it.label, it.color.toArgb(), it.count) },
        footer = footer,
    )
}
