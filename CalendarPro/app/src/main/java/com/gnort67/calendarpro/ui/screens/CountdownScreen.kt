package com.gnort67.calendarpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.HolidayLogic
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.data.HolidayTheme
import com.gnort67.calendarpro.ui.components.*
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun CountdownScreen(
    catalog: HolidayCatalog,
    events: List<PersonalEvent>,
    lang: String,
    today: LocalDate,
    bottomInset: Dp,
    onOpenHoliday: (Holiday) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (PersonalEvent) -> Unit,
    onDeleteEvent: (PersonalEvent) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }

    // Đồng hồ chạy mỗi giây — chỉ các ô đếm ngược đọc State này nên chỉ chúng được vẽ lại.
    val now = produceState(LocalDateTime.now()) { while (true) { delay(1000); value = LocalDateTime.now() } }
    val list = remember(today, events, filter, tab) {
        val n = LocalDateTime.now()
        upcomingAll(catalog, events, n).filter { filter == "all" || it.holiday.category == filter }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "head") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.countdown_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(if (tab == 0) R.string.countdown_desc else R.string.year_progress_subtitle),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "tabs") {
                SegmentedControl(
                    listOf(SegOption(stringResource(R.string.year_progress_tab_events), "hourglass_top"), SegOption(stringResource(R.string.year_progress_tab_year), "donut_large")),
                    tab, { tab = it }, height = 60.dp
                )
            }
            if (tab == 0) {
                item(key = "filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val cats = listOf("all") + HolidayLogic.CATEGORIES
                        items(cats) { c ->
                            FilterChip(
                                selected = filter == c, onClick = { filter = c },
                                label = { Text(if (c == "all") stringResource(R.string.countdown_filter_all) else categoryLabel(c)) },
                                leadingIcon = if (filter == c) ({ AppIcon("check", size = 18.dp) }) else null,
                                shape = CircleShape, modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
                if (list.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(if (filter == "ca-nhan") R.string.countdown_empty_mine else R.string.countdown_empty),
                            Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(list, key = { it.holiday.id }) { u ->
                    val card: @Composable () -> Unit = { CountdownCard(u, now, catalog, lang, today) { onOpenHoliday(u.holiday) } }
                    if (u.holiday.personal) {
                        val ev = events.firstOrNull { it.id == u.holiday.eventId }
                        if (ev != null) SwipeableEvent(ev, onEditEvent, onDeleteEvent, Modifier.animateItem()) { card() } else card()
                    } else Box(Modifier.animateItem()) { card() }
                }
                if (filter == "ca-nhan" && events.isNotEmpty()) item(key = "hint") {
                    Text(stringResource(R.string.countdown_mine_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                }
            } else {
                yearProgressItems(lang, today)
            }
        }
        ScrollTopButton(
            showTop, { scope.launch { listState.animateScrollToItem(0) } },
            Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = bottomInset + 92.dp)
        )
        if (tab == 0) {
            FloatingActionButton(
                onClick = onAddEvent, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomInset + 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(22.dp)
            ) { AppIcon("add") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEvent(
    ev: PersonalEvent, onEdit: (PersonalEvent) -> Unit, onDelete: (PersonalEvent) -> Unit,
    modifier: Modifier, content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.EndToStart -> onDelete(ev)
            SwipeToDismissBoxValue.StartToEnd -> onEdit(ev)
            else -> {}
        }
        false // luôn bật lại — thao tác thật diễn ra qua hộp thoại / bottom sheet
    })
    SwipeToDismissBox(
        state = state, modifier = modifier,
        backgroundContent = {
            val end = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val start = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val bg = when { end -> MaterialTheme.colorScheme.errorContainer; start -> MaterialTheme.colorScheme.secondaryContainer; else -> Color.Transparent }
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(bg).padding(horizontal = 28.dp)) {
                if (start) AppIcon("edit", Modifier.align(Alignment.CenterStart), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                if (end) AppIcon("delete", Modifier.align(Alignment.CenterEnd), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) { content() }
}

@Composable
private fun CountdownCard(u: Upcoming, now: State<LocalDateTime>, catalog: HolidayCatalog, lang: String, today: LocalDate, onDetail: () -> Unit) {
    val h = u.holiday
    val theme = catalog.theme(h)
    val t = catalog.texts(h, lang)
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(36.dp)
    val fg = if (theme != null) Color.White else cs.onSurface
    val sub = if (theme != null) Color.White.copy(alpha = 0.85f) else cs.onSurfaceVariant
    val date = u.at.toLocalDate()
    val desc = if (h.personal) stringResource(R.string.holiday_detail_personal_note) else t.desc

    Box(
        Modifier.fillMaxWidth().clip(shape).let {
            if (theme != null) it.background(themeBrush(theme)) else it.background(cs.surfaceContainerLow)
        }
    ) {
        if (theme != null) ThemeDecor(theme, Modifier.matchParentSize())
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (theme != null) ShapeBadge(holidayIconName(h.icon), ExpressiveShapes.Scallop, Color.White.copy(alpha = 0.22f), Color.White, size = 60.dp, iconSize = 30.dp, filled = true)
                else ShapeBadge(holidayIconName(h.icon), ExpressiveShapes.Scallop, cs.primaryContainer, cs.onPrimaryContainer, size = 60.dp, iconSize = 30.dp, filled = true)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(t.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = fg)
                    Text(DateUtils.formatVNDate(date, lang), style = MaterialTheme.typography.bodyMedium, color = sub)
                }
            }
            Pill(
                categoryLabel(h.category),
                if (theme != null) Color.White.copy(alpha = 0.22f) else cs.surfaceContainerHighest,
                if (theme != null) Color.White else cs.onSurfaceVariant
            )
            CountdownPills(u, now, today, theme)
            if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodyLarge, color = sub)
            Button(
                onClick = onDetail, Modifier.fillMaxWidth().height(54.dp), shape = CircleShape,
                colors = if (theme != null) ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = theme.strong)
                else ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(stringResource(R.string.countdown_view_detail), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                AppIcon("arrow_forward", size = 18.dp)
            }
        }
    }
}

@Composable
private fun CountdownPills(u: Upcoming, now: State<LocalDateTime>, today: LocalDate, theme: HolidayTheme?) {
    val n = now.value // chỉ composable này đọc State đồng hồ
    val p = DateUtils.diffParts(u.at, n)
    val cs = MaterialTheme.colorScheme
    if (p.days == 0 && u.at.toLocalDate() == n.toLocalDate() && !u.holiday.isEve) {
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(if (theme != null) Color.White else cs.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.countdown_occurs_today), fontWeight = FontWeight.Bold, color = if (theme != null) theme.strong else cs.onPrimaryContainer)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            p.days to R.string.common_day, p.hours to R.string.common_hour,
            p.minutes to R.string.common_minute, p.seconds to R.string.common_second
        ).forEachIndexed { i, (v, label) ->
            val first = i == 0
            val bg = when {
                theme != null -> if (first) Color.White else Color.White.copy(alpha = 0.22f)
                else -> if (first) cs.primaryContainer else cs.surfaceContainerHighest
            }
            val c = when {
                theme != null -> if (first) theme.strong else Color.White
                else -> if (first) cs.onPrimaryContainer else cs.onSurface
            }
            Column(
                Modifier.weight(1f).height(72.dp).clip(CircleShape).background(bg),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text(if (first) v.toString() else DateUtils.pad2(v), color = c, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(stringResource(label), color = c.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    val prog = 1f - (p.days.coerceAtMost(365) / 365f)
    WavyLine(
        prog, Modifier.fillMaxWidth().height(16.dp),
        color = if (theme != null) Color.White else cs.primary,
        track = if (theme != null) Color.White.copy(alpha = 0.3f) else cs.surfaceContainerHighest
    )
}

private fun themeBrush(t: HolidayTheme): Brush =
    Brush.linearGradient(
        *t.stops.map { it.first to it.second }.toTypedArray(),
        start = Offset(0f, 0f), end = Offset(1400f, 1800f)
    )

/** Emoji trôi nhẹ trong nền thẻ chủ đề (Giáng sinh ❄️🎄, Tết 🧧...). */
@Composable
private fun ThemeDecor(theme: HolidayTheme, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "emoji")
    BoxWithConstraints(modifier) {
        theme.emojis.forEachIndexed { i, e ->
            val bob by inf.animateFloat(
                -6f, 6f, infiniteRepeatable(tween(2600 + i * 350, easing = FastOutSlowInEasing, delayMillis = (e.delay * 300).toInt()), RepeatMode.Reverse), label = "bob"
            )
            val size = (e.size * 18).sp
            val sizeDp = (e.size * 18 * 1.3f).dp
            val x = when { e.left != null -> maxWidth * e.left; e.right != null -> maxWidth * (1f - e.right) - sizeDp; else -> 0.dp }
            val y = when { e.top != null -> maxHeight * e.top; e.bottom != null -> maxHeight * (1f - e.bottom) - sizeDp; else -> 0.dp }
            Text(
                e.e, Modifier.offset(x, y).graphicsLayer { translationY = bob; alpha = 0.5f }, fontSize = size
            )
        }
    }
}
