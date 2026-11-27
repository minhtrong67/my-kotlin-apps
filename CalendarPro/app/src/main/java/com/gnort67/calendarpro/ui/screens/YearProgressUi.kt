package com.gnort67.calendarpro.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.YearProgress
import com.gnort67.calendarpro.ui.components.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/** Tab "Tiến độ năm": vòng sóng, thống kê, các tháng và cột mốc. */
fun LazyListScope.yearProgressItems(lang: String, today: LocalDate) {
    item(key = "yp") { YearProgressBody(lang, today) }
}

@Composable
private fun YearProgressBody(lang: String, today: LocalDate) {
    var kind by rememberSaveable { mutableStateOf("solar") }
    val cs = MaterialTheme.colorScheme
    val range = remember(kind, today) { YearProgress.getRange(kind, today) }
    val nowMs by produceState(System.currentTimeMillis()) { while (true) { delay(30_000); value = System.currentTimeMillis() } }
    val st = remember(range, nowMs) { YearProgress.progressOf(range, nowMs) }
    val segs = remember(range) { YearProgress.monthSegments(range) }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(kind) { started = false; delay(60); started = true }
    val anim by animateFloatAsState(if (started) st.fraction.toFloat() else 0f, tween(1400, easing = FastOutSlowInEasing), label = "year-ring")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegmentedControl(
            listOf(SegOption(stringResource(R.string.year_progress_solar), "check"), SegOption(stringResource(R.string.year_progress_lunar))),
            if (kind == "solar") 0 else 1, { kind = if (it == 0) "solar" else "lunar" }
        )
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp), color = cs.surfaceContainerLow) {
            Box {
                BlobDecoration(Modifier.matchParentSize(), cs.primaryContainer)
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.year_progress_year_label, range.label).uppercase(),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant, letterSpacing = 1.sp
                    )
                    Box(Modifier.padding(top = 16.dp).size(290.dp), contentAlignment = Alignment.Center) {
                        WavyRing(anim, Modifier.fillMaxSize(), cs.primaryContainer, cs.surfaceContainerHighest, stroke = 16.dp, amplitude = 5.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val txt = String.format(Locale.US, "%.2f", anim * 100.0)
                            val sep = if (lang == "en") "." else ","
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(txt.substringBefore('.'), fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary, lineHeight = 76.sp)
                                Text(sep + txt.substringAfter('.') + "%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cs.primary, modifier = Modifier.padding(bottom = 10.dp))
                            }
                            Text(stringResource(R.string.year_progress_done), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(stringResource(R.string.year_progress_passed), stringResource(R.string.year_progress_days, st.passedDays), Modifier.weight(1f))
            Stat(stringResource(R.string.year_progress_left), stringResource(R.string.year_progress_days, st.leftDays), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(stringResource(R.string.year_progress_today), stringResource(R.string.year_progress_day_of, st.dayNumber, st.totalDays), Modifier.weight(1f))
            Stat(stringResource(R.string.year_progress_today), stringResource(R.string.year_progress_week_of, st.weekNumber, st.totalWeeks), Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.year_progress_per_day, String.format(Locale.US, "%.2f", st.perDay)),
            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)
        )

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = cs.surfaceContainer) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.year_progress_months), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    segs.forEach { s ->
                        val current = s.contains(nowMs)
                        val fill = s.fill(nowMs).toFloat()
                        val shown by animateFloatAsState(if (started) fill else 0f, tween(1100, easing = FastOutSlowInEasing), label = "bar")
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(120.dp).clip(CircleShape).background(if (current) cs.primaryContainer.copy(alpha = 0.35f) else cs.surfaceContainerHighest)) {
                                Box(
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(shown).clip(CircleShape)
                                        .background(if (current) cs.primaryContainer else cs.secondary.copy(alpha = 0.55f))
                                )
                            }
                            Text(
                                s.label + if (s.leap) "n" else "", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (current) FontWeight.ExtraBold else FontWeight.Normal, color = if (current) cs.primary else cs.onSurfaceVariant,
                                textAlign = TextAlign.Center, maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = cs.surfaceContainer) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.year_progress_milestones), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                listOf(0.25, 0.5, 0.75, 1.0).forEachIndexed { i, f ->
                    val date = YearProgress.dateAtFraction(range, if (f >= 1.0) 0.9999 else f).let { if (f >= 1.0) range.end.minusDays(1) else it }
                    val passed = st.fraction >= f || (f >= 1.0 && st.fraction >= 0.9999)
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (passed) cs.primaryContainer else cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                            AppIcon(if (passed) "check" else if (f >= 1.0) "flag" else "radio_button_unchecked", size = 22.dp, tint = if (passed) cs.onPrimaryContainer else cs.onSurfaceVariant)
                        }
                        Text("${(f * 100).toInt()}%", Modifier.padding(start = 14.dp).weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(DateUtils.formatShortDate(date), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        if (passed) Text(
                            stringResource(R.string.year_progress_reached), Modifier.padding(start = 10.dp).clip(CircleShape).background(cs.secondaryContainer).padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
