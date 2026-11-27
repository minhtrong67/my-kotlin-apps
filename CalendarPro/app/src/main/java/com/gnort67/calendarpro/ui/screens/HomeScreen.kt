package com.gnort67.calendarpro.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.LunarCalendar
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.ui.components.AppIcon
import com.gnort67.calendarpro.ui.components.ScrollTopButton
import com.gnort67.calendarpro.ui.components.ShapeBadge
import com.gnort67.calendarpro.ui.components.holidayIconName
import com.gnort67.calendarpro.ui.components.pressable
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.extra
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalog: HolidayCatalog,
    events: List<PersonalEvent>,
    lang: String,
    today: LocalDate,
    bottomInset: androidx.compose.ui.unit.Dp,
    onOpenDay: (LocalDate) -> Unit,
    onOpenHoliday: (Holiday) -> Unit,
    onSeeAll: () -> Unit,
    onRefreshed: () -> Unit
) {
    var tick by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    val info = remember(today, events, lang, tick) { buildDayInfo(today, catalog, events, lang) }
    val upcoming = remember(today, events, tick) {
        val now = LocalDateTime.now()
        upcomingAll(catalog, events, now).take(8).map { it to daysLeft(it, now) }
    }
    val weekStart = today.minusDays(DateUtils.mondayIndex(today).toLong())

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                delay(700)
                tick++
                refreshing = false
                onRefreshed()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "week") { WeekStrip(weekStart, today, lang, onOpenDay) }
            item(key = "hero") { DateHeroCard(info, lang) }
            items(info.holidays, key = { "h-" + it.id }) { HolidayBanner(it, catalog, lang) { onOpenHoliday(it) } }
            item(key = "mine") { MyEventsCard(info) }
            item(key = "tiles") { CanChiTiles(info) }
            item(key = "sao") { SaoNgayCard(info) }
            item(key = "dir") { DirectionsCard(info, lang) }
            item(key = "gio") { LuckyHoursCard(info) }
            item(key = "up-head") {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_upcoming_events), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                    )
                    Row(
                        Modifier.pressable(RoundedCornerShape(20.dp), onClick = onSeeAll).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.home_view_all), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        AppIcon("chevron_right", tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    }
                }
            }
            item(key = "up-row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
                    items(upcoming, key = { it.first.holiday.id }) { (u, left) ->
                        UpcomingCard(u, left, catalog, lang, today) { onOpenHoliday(u.holiday) }
                    }
                }
            }
            item(key = "chay") { VegetarianSection(info) }
        }
        ScrollTopButton(
            showTop, { scope.launch { listState.animateScrollToItem(0) } },
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomInset + 16.dp)
        )
    }
}

@Composable
private fun WeekStrip(start: LocalDate, today: LocalDate, lang: String, onOpenDay: (LocalDate) -> Unit) {
    val labels = DateUtils.weekdaysShort(lang)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 7) {
            val d = start.plusDays(i.toLong())
            val isToday = d == today
            val lunar = DateUtils.getLunarInfo(d)
            val shape = RoundedCornerShape(26.dp)
            val bg = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            val fg = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            val sun = i == 6
            Column(
                Modifier.weight(1f).height(if (isToday) 104.dp else 96.dp).pressable(shape) { onOpenDay(d) }
                    .background(bg).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(labels[i], style = MaterialTheme.typography.labelMedium, color = if (sun) MaterialTheme.colorScheme.error else fg.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = fg)
                Text(lunar.day.toString(), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun UpcomingCard(u: Upcoming, left: Int, catalog: HolidayCatalog, lang: String, today: LocalDate, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val isToday = left == 0 && u.at.toLocalDate() == today
    Surface(
        Modifier.width(250.dp).pressable(shape, onClick = onClick), shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(20.dp).height(150.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Top) {
                ShapeBadge(
                    holidayIconName(u.holiday.icon), ExpressiveShapes.Scallop,
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                    size = 48.dp, iconSize = 24.dp, filled = true
                )
                Text(
                    catalog.name(u.holiday, lang), Modifier.padding(start = 12.dp), maxLines = 2,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                )
            }
            Column {
                Text(
                    if (isToday) stringResource(R.string.common_today) else stringResource(R.string.countdown_days_short, left),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary
                )
                Text(DateUtils.formatShortDate(u.at.toLocalDate()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VegetarianSection(info: DayInfo) {
    val ex = MaterialTheme.extra
    val l = info.lunar
    val leap = if (l.isLeap) 1 else 0
    fun solar(day: Int): String? = LunarCalendar.lunarToSolar(day, l.month, l.year, leap)?.let { "${DateUtils.pad2(it[0])}/${DateUtils.pad2(it[1])}" }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge("eco", ExpressiveShapes.Cookie, ex.chayContainer, ex.onChayContainer, size = 52.dp, iconSize = 26.dp, filled = true)
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.home_vegetarian_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_vegetarian_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChayTile(stringResource(R.string.home_first_day), stringResource(R.string.home_first_day_desc), solar(1), Modifier.weight(1f))
            ChayTile(stringResource(R.string.home_full_moon), stringResource(R.string.home_full_moon_desc), solar(15), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChayTile(title: String, desc: String, date: String?, modifier: Modifier) {
    val ex = MaterialTheme.extra
    Surface(modifier, shape = RoundedCornerShape(30.dp), color = ex.chayContainer, contentColor = ex.onChayContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
            if (date != null) Text(date, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
