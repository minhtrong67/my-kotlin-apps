@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalLocale
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.ConfirmDialog
import com.minhtrong67.moody.ui.components.DropdownField
import com.minhtrong67.moody.ui.components.EmptyText
import com.minhtrong67.moody.ui.components.MoodBubble
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SegmentedTabs
import com.minhtrong67.moody.ui.components.StaticChip
import com.minhtrong67.moody.ui.components.SwipeActionRow
import com.minhtrong67.moody.util.formatTime
import com.minhtrong67.moody.util.fullDate
import com.minhtrong67.moody.util.isoToDate
import com.minhtrong67.moody.util.monthTitle
import com.minhtrong67.moody.util.relativeDateLabel
import com.minhtrong67.moody.util.today
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HistoryScreen(
    entries: List<Entry>?,
    onEdit: (id: String) -> Unit,
    onNewForDate: (date: String) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable { mutableStateOf("list") }
    Column(modifier.padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        SegmentedTabs(
            options = listOf("list" to stringResource(R.string.history_list), "calendar" to stringResource(R.string.history_calendar)),
            selected = view, onSelect = { view = it },
            icons = mapOf("list" to Icons.AutoMirrored.Rounded.List, "calendar" to Icons.Rounded.CalendarMonth),
        )
        Spacer(Modifier.height(16.dp))
        if (entries == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (view == "list") {
            ListView(entries, onEdit, onDelete)
        } else {
            CalendarView(entries, onEdit, onNewForDate)
        }
    }
}

@Composable
private fun ListView(entries: List<Entry>, onEdit: (String) -> Unit, onDelete: (String) -> Unit) {
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val moodByKey = remember(moods) { moods.associateBy { it.key } }
    val activityById = remember(activities) { activities.associateBy { it.id } }

    var period by rememberSaveable { mutableStateOf("all") }
    var moodFilter by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("date_desc") }
    var search by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Entry?>(null) }

    val activeFilters = (if (period != "all") 1 else 0) + (if (moodFilter != "all") 1 else 0) + (if (sort != "date_desc") 1 else 0)

    val filtered = remember(entries, period, moodFilter, sort, search, activityById) {
        val now = today()
        val q = search.trim().lowercase()
        entries.filter { e ->
            if (moodFilter != "all" && e.mood != moodFilter) return@filter false
            val d = isoToDate(e.date)
            if (period == "week" && ChronoUnit.DAYS.between(d, now) !in 0..6) return@filter false
            if (period == "month" && !(d.month == now.month && d.year == now.year)) return@filter false
            if (q.isNotEmpty()) {
                val labels = e.activities.joinToString(" ") { activityById[it]?.label ?: "" }
                if (!"${e.note} $labels".lowercase().contains(q)) return@filter false
            }
            true
        }.let { list ->
            when (sort) {
                "date_asc" -> list.sortedWith(compareBy<Entry>({ it.date }, { it.instant }))
                "score_desc" -> list.sortedByDescending { it.score }
                "score_asc" -> list.sortedBy { it.score }
                else -> list.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.instant })
            }
        }
    }

    pendingDelete?.let { e ->
        ConfirmDialog(
            title = stringResource(R.string.history_deleteTitle),
            text = stringResource(R.string.common_irreversible),
            confirmLabel = stringResource(R.string.common_delete),
            danger = true,
            headerIcon = Icons.Rounded.WarningAmber,
            onConfirm = { onDelete(e.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text(stringResource(R.string.history_searchPlaceholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Rounded.Close, null) } },
            singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        MoodyCard(Modifier.fillMaxWidth(), onClick = { filtersOpen = !filtersOpen }, contentPadding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.history_filters), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (activeFilters > 0) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text(activeFilters.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Icon(if (filtersOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
        }
        AnimatedVisibility(filtersOpen) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    stringResource(R.string.history_period),
                    listOf("all" to stringResource(R.string.history_period_all), "week" to stringResource(R.string.history_period_week), "month" to stringResource(R.string.history_period_month)),
                    period, { period = it },
                    Modifier.fillMaxWidth(),
                )
                DropdownField(
                    stringResource(R.string.history_mood),
                    listOf("all" to stringResource(R.string.history_allMoods)) + moods.map { it.key to "${it.emoji} ${it.label}" },
                    moodFilter, { moodFilter = it },
                    Modifier.fillMaxWidth(),
                )
                DropdownField(
                    stringResource(R.string.history_sort),
                    listOf(
                        "date_desc" to stringResource(R.string.history_sort_date_desc),
                        "date_asc" to stringResource(R.string.history_sort_date_asc),
                        "score_desc" to stringResource(R.string.history_sort_score_desc),
                        "score_asc" to stringResource(R.string.history_sort_score_asc),
                    ),
                    sort, { sort = it },
                    Modifier.fillMaxWidth(),
                )
                ActionButton(
                    stringResource(R.string.history_resetFilters), Icons.Rounded.RestartAlt,
                    onClick = { period = "all"; moodFilter = "all"; sort = "date_desc" },
                    tonal = true, enabled = activeFilters > 0, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.history_count, filtered.size) + " · " + stringResource(R.string.history_swipeHint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        if (filtered.isEmpty()) {
            EmptyText(stringResource(R.string.history_empty))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { e ->
                    EntryCard(e, moodByKey[e.mood], activityById, onEdit = { onEdit(e.id) }, onDelete = { pendingDelete = e }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

/** Swipe right = edit, swipe left = delete (the card snaps back; delete asks for confirmation). */
@Composable
private fun EntryCard(
    e: Entry,
    mood: com.minhtrong67.moody.data.MoodDef?,
    activityById: Map<String, com.minhtrong67.moody.data.ActivityDef>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val locale = LocalLocale.current
    val cs = MaterialTheme.colorScheme
    SwipeActionRow(onEdit = onEdit, onDelete = onDelete, modifier = modifier) {
        MoodyCard(Modifier.fillMaxWidth(), onClick = onEdit) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoodBubble(mood)
                Column(Modifier.weight(1f)) {
                    Text(mood?.label ?: e.mood, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                        Text(
                            relativeDateLabel(ctx, e.date, locale) + ", " + formatTime(e, locale),
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            if (e.note.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(e.note, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            val acts = e.activities.mapNotNull { activityById[it] }
            if (acts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    acts.forEach { StaticChip(it.emoji, it.label) }
                }
            }
        }
    }
}

@Composable
private fun CalendarView(entries: List<Entry>, onEdit: (String) -> Unit, onNewForDate: (String) -> Unit) {
    val moods = LocalMoods.current
    val moodByKey = remember(moods) { moods.associateBy { it.key } }
    val locale = LocalLocale.current
    var cursorText by rememberSaveable { mutableStateOf(today().withDayOfMonth(1).toString()) }
    val cursor = LocalDate.parse(cursorText)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val byDate = remember(entries) {
        entries.groupBy { it.date }.mapValues { (_, l) -> l.sortedByDescending { it.instant } }
    }
    val ym = YearMonth.from(cursor)
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val cs = MaterialTheme.colorScheme

    selected?.let { sel ->
        val list = byDate[sel].orEmpty()
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheet) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
                Text(fullDate(isoToDate(sel), locale), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.history_count, list.size), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                list.forEach { e -> EntryDetail(e, onEdit = { selected = null; onEdit(e.id) }); Spacer(Modifier.height(12.dp)) }
                ActionButton(stringResource(R.string.common_close), Icons.Rounded.Close, { selected = null }, tonal = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { cursorText = cursor.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.history_prevMonth)) }
            AnimatedContent(
                targetState = cursor, modifier = Modifier.weight(1f),
                transitionSpec = {
                    val fwd = targetState > initialState
                    (slideInVertically(tween(240)) { if (fwd) it / 2 else -it / 2 } + fadeIn(tween(200))) togetherWith
                        (slideOutVertically(tween(180)) { if (fwd) -it / 2 else it / 2 } + fadeOut(tween(120)))
                },
                label = "monthTitle",
            ) { m -> Text(monthTitle(m, locale), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            IconButton(onClick = { cursorText = cursor.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.history_nextMonth)) }
        }
        Text(stringResource(R.string.history_calendarHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant) }
        }
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = cursor,
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(260)) { if (fwd) it / 4 else -it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { if (fwd) -it / 4 else it / 4 } + fadeOut(tween(120)))
            },
            label = "monthGrid",
        ) { month ->
            val first = month.withDayOfMonth(1).dayOfWeek.value - 1   // Monday = 0
            val cells: List<Int?> = List(first) { null } + (1..YearMonth.from(month).lengthOfMonth()).toList()
            Column {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until 7) {
                            val day = week.getOrNull(i)
                            if (day == null) { Spacer(Modifier.weight(1f)); continue }
                            val iso = month.withDayOfMonth(day).toString()
                            val m = byDate[iso]?.firstOrNull()?.let { moodByKey[it.mood] }
                            val isToday = iso == today().toString()
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                    .background(m?.color?.copy(alpha = 0.25f) ?: Color.Transparent)
                                    .clickable { if (byDate.containsKey(iso)) selected = iso else onNewForDate(iso) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (m != null) Text(m.emoji, fontSize = 20.sp)
                                else Text(day.toString(), style = MaterialTheme.typography.bodyMedium, color = if (isToday) cs.primary else cs.onSurface)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        MonthSummary(ym, byDate)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun EntryDetail(e: Entry, onEdit: () -> Unit) {
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val locale = LocalLocale.current
    val m = moods.firstOrNull { it.key == e.mood }
    val cs = MaterialTheme.colorScheme
    MoodyCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MoodBubble(m)
            Column(Modifier.weight(1f)) {
                Text(m?.label ?: e.mood, style = MaterialTheme.typography.titleMedium)
                Text(formatTime(e, locale), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Text(stringResource(R.string.history_score, e.score), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), tint = cs.primary) }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.history_note), style = MaterialTheme.typography.labelMedium, color = cs.primary)
        Text(if (e.note.isBlank()) stringResource(R.string.history_noNote) else e.note, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.history_activities), style = MaterialTheme.typography.labelMedium, color = cs.primary)
        val acts = e.activities.mapNotNull { id -> activities.firstOrNull { it.id == id } }
        if (acts.isEmpty()) Text(stringResource(R.string.history_noActivities), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            acts.forEach { StaticChip(it.emoji, it.label) }
        }
    }
}

@Composable
private fun MonthSummary(ym: YearMonth, byDate: Map<String, List<Entry>>) {
    val moods = LocalMoods.current
    val cs = MaterialTheme.colorScheme
    val prefix = ym.toString()   // yyyy-MM
    val month = byDate.filterKeys { it.startsWith(prefix) }.values.mapNotNull { it.firstOrNull() }
    val avg = if (month.isEmpty()) null else month.sumOf { it.score }.toDouble() / month.size
    val counts = moods.associate { m -> m.key to month.count { it.mood == m.key } }
    val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    MoodyCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(stringResource(R.string.history_monthSummary), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(month.size.toString(), stringResource(R.string.history_monthDays), Modifier.weight(1f))
            SummaryTile(avg?.let { "%.1f/5".format(Locale.US, it) } ?: "—", stringResource(R.string.history_monthAvg), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        moods.forEach { m ->
            val c = counts[m.key] ?: 0
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(m.emoji, fontSize = 20.sp, modifier = Modifier.size(28.dp), textAlign = TextAlign.Center)
                Text(m.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.size(width = 84.dp, height = 20.dp))
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(cs.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(c.toFloat() / max).height(10.dp).clip(CircleShape).background(m.color))
                }
                Text(c.toString(), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, modifier = Modifier.size(width = 24.dp, height = 20.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
