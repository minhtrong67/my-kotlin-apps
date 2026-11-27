package com.gnort67.calendarpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.LunarCalendar
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.ui.components.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun VanNienScreen(
    catalog: HolidayCatalog,
    events: List<PersonalEvent>,
    lang: String,
    today: LocalDate,
    bottomInset: Dp,
    onOpenHoliday: (Holiday) -> Unit,
    onMessage: (String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = dương, 1 = âm
    var day by remember { mutableIntStateOf(today.dayOfMonth) }
    var month by remember { mutableIntStateOf(today.monthValue) }
    var year by remember { mutableIntStateOf(today.year) }
    var shown by remember { mutableStateOf(today) }
    val info = remember(shown, events, lang) { buildDayInfo(shown, catalog, events, lang) }
    val listState = rememberLazyListState()
    val rightState = rememberLazyListState()
    val landscape = isLandscape()
    val scope = rememberCoroutineScope()
    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 600 } }
    val invalidSolar = stringResource(R.string.van_nien_invalid_solar)
    val invalidLunar = stringResource(R.string.van_nien_invalid_lunar)
    val toLunarOk = stringResource(R.string.van_nien_converted_to_lunar)
    val toSolarOk = stringResource(R.string.van_nien_converted_to_solar)

    fun submit() {
        if (mode == 0) {
            val d = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (d == null) { onMessage(invalidSolar); return }
            shown = d; onMessage(toLunarOk)
        } else {
            val s = LunarCalendar.lunarToSolar(day, month, year, 0)
            val d = s?.let { runCatching { LocalDate.of(it[2], it[1], it[0]) }.getOrNull() }
            val back = d?.let { DateUtils.getLunarInfo(it) }
            if (d == null || back == null || back.day != day || back.month != month || back.year != year) { onMessage(invalidLunar); return }
            shown = d; onMessage(toSolarOk)
        }
        scope.launch { if (landscape) rightState.animateScrollToItem(0) else listState.animateScrollToItem(1) }
    }

    val leftItems: LazyListScope.() -> Unit = {
            item(key = "title") {
                Column(Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.van_nien_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.van_nien_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "form") {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SegmentedControl(
                            listOf(SegOption(stringResource(R.string.van_nien_from_solar)), SegOption(stringResource(R.string.van_nien_from_lunar))),
                            mode, { mode = it }, container = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickerField(stringResource(R.string.van_nien_day), day, (1..if (mode == 1) 30 else 31).toList(), { day = it }, Modifier.weight(1f))
                            PickerField(stringResource(R.string.van_nien_month), month, (1..12).toList(), { month = it }, Modifier.weight(1f))
                            PickerField(stringResource(R.string.van_nien_year), year, (1900..2100).toList(), { year = it }, Modifier.weight(1.15f))
                        }
                        Button(
                            onClick = { submit() }, Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            AppIcon("search", size = 20.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.van_nien_submit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
    }
    val rightItems: LazyListScope.() -> Unit = {
            item(key = "result-" + shown.toString()) {
                DayDetailContent(info, catalog, lang, onOpenHoliday, Modifier.animateItem())
            }
    }
    Box(Modifier.fillMaxSize()) {
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyColumn(
                    Modifier.weight(1f).fillMaxHeight(), state = listState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = bottomInset + 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { leftItems() }
                LazyColumn(
                    Modifier.weight(1.2f).fillMaxHeight(), state = rightState,
                    contentPadding = PaddingValues(end = 16.dp, top = 8.dp, bottom = bottomInset + 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { rightItems() }
            }
        } else {
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset + 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) { leftItems(); rightItems() }
        }
        ScrollTopButton(
            showTop, { scope.launch { listState.smoothScrollToTop() } },
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomInset + 16.dp)
        )
    }
}

/** Ô chọn dạng viên thuốc có nhãn nhỏ phía trên giá trị; mở menu cuộn tới đúng giá trị đang chọn. */
@Composable
private fun PickerField(label: String, value: Int, options: List<Int>, onPick: (Int) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).pressable(RoundedCornerShape(24.dp)) { open = true }
                .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AppIcon("expand_more", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val idx = options.indexOf(value).coerceAtLeast(0)
            val st = rememberLazyListState(initialFirstVisibleItemIndex = (idx - 2).coerceAtLeast(0))
            LazyColumn(Modifier.size(width = 150.dp, height = 280.dp), state = st) {
                items(options) { o ->
                    DropdownMenuItem(
                        text = { Text(o.toString(), fontWeight = if (o == value) FontWeight.Bold else FontWeight.Normal, color = if (o == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        onClick = { onPick(o); open = false }
                    )
                }
            }
        }
    }
}
