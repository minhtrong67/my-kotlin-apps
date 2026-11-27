package com.gnort67.calendarpro.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.HolidayLogic
import com.gnort67.calendarpro.core.LunarCalendar
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.ui.components.AppIcon
import com.gnort67.calendarpro.ui.components.pressable
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.extra
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.absoluteValue

private const val BASE_YEAR = 1900
private const val PAGE_COUNT = 201 * 12

private fun pageOf(ym: YearMonth) = (ym.year - BASE_YEAR) * 12 + ym.monthValue - 1
private fun monthOf(page: Int) = YearMonth.of(BASE_YEAR + page / 12, page % 12 + 1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    catalog: HolidayCatalog,
    events: List<PersonalEvent>,
    lang: String,
    today: LocalDate,
    bottomInset: Dp,
    onOpenDay: (LocalDate) -> Unit
) {
    val todayPage = pageOf(YearMonth.from(today))
    val pager = rememberPagerState(initialPage = todayPage, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var picker by remember { mutableStateOf(false) }

    // Rung nhẹ mỗi khi vuốt sang tháng khác
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    val ym = monthOf(pager.currentPage)
    val mid = ym.atDay(15)
    val lunarMid = remember(mid) { DateUtils.getLunarInfo(mid) }
    val monthTitle = if (lang == "en") "${DateUtils.MONTH_NAMES_EN[ym.monthValue - 1]}\n${ym.year}" else "Tháng ${ym.monthValue}\n${ym.year}"

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = bottomInset + 96.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.pressable(RoundedCornerShape(16.dp)) { picker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(monthTitle, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, lineHeight = MaterialTheme.typography.displaySmall.lineHeight)
                        AppIcon("expand_more", Modifier.padding(start = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon("bedtime", tint = MaterialTheme.colorScheme.tertiary, size = 18.dp, filled = true)
                        Text(
                            stringResource(R.string.calendar_year_label, LunarCalendar.canChiYear(lunarMid.year), lunarMid.year),
                            Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavArrow("chevron_left", stringResource(R.string.calendar_prev_month)) {
                        scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                    }
                    Box(Modifier.size(1.dp, 26.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)))
                    NavArrow("chevron_right", stringResource(R.string.calendar_next_month)) {
                        scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(PAGE_COUNT - 1)) }
                    }
                }
            }

            Surface(
                Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(vertical = 18.dp)) {
                    WeekdayHeader(lang)
                    HorizontalPager(
                        state = pager, beyondViewportPageCount = 1, pageSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { page ->
                        val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        Box(
                            Modifier.graphicsLayer {
                                alpha = lerp(1f, 0.35f, off)
                                val s = lerp(1f, 0.92f, off)
                                scaleX = s; scaleY = s
                            }
                        ) {
                            MonthGrid(monthOf(page), today, catalog, events, onOpenDay)
                        }
                    }
                }
            }

            FlowRow(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ex = MaterialTheme.extra
                LegendChip(stringResource(R.string.calendar_legend_today), MaterialTheme.colorScheme.primaryContainer, null)
                LegendChip(stringResource(R.string.calendar_legend_lunar_mark), MaterialTheme.colorScheme.surfaceContainerHigh, null, bold = true)
                LegendChip(stringResource(R.string.calendar_legend_vegetarian), Color.Transparent, ex.chay, outlined = true)
                LegendChip(stringResource(R.string.calendar_legend_holiday), Color.Transparent, ex.holiday, outlined = true)
                LegendChip(stringResource(R.string.calendar_legend_sunday), Color.Transparent, null, textColor = MaterialTheme.colorScheme.error, outlined = false)
                if (events.isNotEmpty()) LegendChip(stringResource(R.string.calendar_legend_mine), MaterialTheme.colorScheme.secondaryContainer, null)
            }
        }

        FloatingActionButton(
            onClick = { scope.launch { pager.animateScrollToPage(todayPage, animationSpec = spring(stiffness = Spring.StiffnessLow)) } },
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomInset + 16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(22.dp)
        ) { AppIcon("calendar_today", filled = true) }
    }

    if (picker) MonthPicker(ym, lang, onDismiss = { picker = false }) { picked ->
        picker = false
        scope.launch { pager.animateScrollToPage(pageOf(picked)) }
    }
}

@Composable
private fun NavArrow(icon: String, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).pressable(CircleShape, onClick = onClick), contentAlignment = Alignment.Center) {
        AppIcon(icon, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun LegendChip(text: String, bg: Color, dot: Color?, bold: Boolean = false, outlined: Boolean = false, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
            Box(Modifier.width(8.dp))
        }
        Text(text, color = textColor, style = MaterialTheme.typography.labelLarge, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun WeekdayHeader(lang: String) {
    val labels = DateUtils.weekdaysShort(lang)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        labels.forEachIndexed { i, l ->
            Text(
                l, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = if (i == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MonthGrid(ym: YearMonth, today: LocalDate, catalog: HolidayCatalog, events: List<PersonalEvent>, onOpenDay: (LocalDate) -> Unit) {
    val cells = remember(ym, events) {
        val first = ym.atDay(1)
        val start = first.minusDays(DateUtils.mondayIndex(first).toLong())
        (0 until 42).map { i ->
            val d = start.plusDays(i.toLong())
            val l = DateUtils.getLunarInfo(d)
            CellData(
                d, d.monthValue == ym.monthValue, l.day, l.month,
                HolidayLogic.getHolidaysOnDate(catalog.holidays, d, l).isNotEmpty(),
                l.day in HolidayLogic.NGAY_CHAY_NHI_TRAI, events.any { it.isOn(d, l) }
            )
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cell = cells[r * 7 + c]
                    DayCell(cell, cell.date == today, c == 6, Modifier.weight(1f)) { onOpenDay(cell.date) }
                }
            }
        }
    }
}

private class CellData(
    val date: LocalDate, val inMonth: Boolean, val lunarDay: Int, val lunarMonth: Int,
    val holiday: Boolean, val chay: Boolean, val mine: Boolean
)

@Composable
private fun DayCell(c: CellData, isToday: Boolean, sunday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ex = MaterialTheme.extra
    val cs = MaterialTheme.colorScheme
    val shape: Shape = if (isToday) ExpressiveShapes.Scallop else CircleShape
    val bg = when {
        isToday -> cs.primaryContainer
        c.holiday && c.inMonth -> ex.holidayContainer
        c.chay && c.inMonth -> ex.chayContainer
        else -> Color.Transparent
    }
    val fg = when {
        isToday -> cs.onPrimaryContainer
        c.holiday && c.inMonth -> ex.onHolidayContainer
        c.chay && c.inMonth -> ex.onChayContainer
        sunday -> cs.error
        else -> cs.onSurface
    }
    val alpha = if (c.inMonth) 1f else 0.38f
    Box(modifier.aspectRatio(0.86f).padding(1.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(shape).background(bg).pressable(shape, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.date.dayOfMonth.toString(), color = fg.copy(alpha = alpha), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (c.lunarDay == 1) "1/${c.lunarMonth}" else DateUtils.pad2(c.lunarDay),
                    color = fg.copy(alpha = alpha * 0.75f), style = MaterialTheme.typography.labelSmall
                )
            }
            if (c.mine) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(7.dp).clip(CircleShape).background(cs.tertiary))
        }
    }
}

@Composable
private fun MonthPicker(current: YearMonth, lang: String, onDismiss: () -> Unit, onPick: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(current.year) }
    val names = DateUtils.monthNames(lang)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_pick_month)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).pressable(CircleShape) { if (year > BASE_YEAR) year-- }, contentAlignment = Alignment.Center) { AppIcon("chevron_left") }
                    Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(44.dp).pressable(CircleShape) { if (year < BASE_YEAR + 200) year++ }, contentAlignment = Alignment.Center) { AppIcon("chevron_right") }
                }
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until 4) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 3) {
                            val m = r * 3 + c + 1
                            val sel = year == current.year && m == current.monthValue
                            Box(
                                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .pressable(RoundedCornerShape(16.dp)) { onPick(YearMonth.of(year, m)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (lang == "en") names[m - 1].take(3) else m.toString(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } }
    )
}

