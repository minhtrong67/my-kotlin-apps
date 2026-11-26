package com.expenser.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.aspectRatio
import com.expenser.app.R
import com.expenser.app.data.*
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.*
import java.util.Calendar
import kotlin.math.roundToInt

private data class CafeItem(val tx: Tx, val log: CafeLog?, val shop: CafeShop?, val drink: CafeDrink?, val wallet: Wallet?)

@Composable
private fun sessionLabel(s: Session) = stringResource(when (s) {
    Session.MORNING -> R.string.cafe_session_morning
    Session.NOON -> R.string.cafe_session_noon
    Session.AFTERNOON -> R.string.cafe_session_afternoon
    Session.EVENING -> R.string.cafe_session_evening
    Session.NIGHT -> R.string.cafe_session_night
})

@Composable
fun cafeCategoryId(categories: List<Category>): String? = remember(categories) { categories.firstOrNull { it.type == TxType.EXPENSE && isCafeCategory(it) }?.id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeScreen(vm: MainViewModel, onEdit: (Tx) -> Unit) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val shops by vm.shops.collectAsStateWithLifecycle()
    val drinks by vm.drinks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val tabScope = rememberCoroutineScope()

    val cafeIds = remember(categories) { categories.filter { it.type == TxType.EXPENSE && isCafeCategory(it) }.map { it.id }.toSet() }
    val logById = remember(logs) { logs.associateBy { it.txId } }
    val shopById = remember(shops) { shops.associateBy { it.id } }
    val drinkById = remember(drinks) { drinks.associateBy { it.id } }
    val walletById = remember(wallets) { wallets.associateBy { it.id } }
    val items = remember(transactions, cafeIds, logById, shopById, drinkById, walletById) {
        transactions.filter { it.type == TxType.EXPENSE && it.categoryId in cafeIds }.map { tx ->
            val log = logById[tx.id]
            CafeItem(tx, log, shopById[log?.shopId ?: ""], drinkById[log?.drinkId ?: ""], walletById[tx.walletId ?: ""])
        }
    }

    Column(Modifier.fillMaxSize()) {
        PillSegmented(
            listOf(Seg("0", stringResource(R.string.cafe_tab_transactions), Icons.AutoMirrored.Rounded.ReceiptLong), Seg("1", stringResource(R.string.cafe_tab_stats), Icons.Rounded.BarChart)),
            pagerState.currentPage.toString(), { tabScope.launch { pagerState.animateScrollToPage(it.toInt()) } }, Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp),
        )
        // Swiping drags the neighbouring tab into view; letting go past halfway settles on it.
        androidx.compose.foundation.pager.HorizontalPager(pagerState, Modifier.weight(1f).fillMaxWidth(), key = { it }) { t ->
            when {
                cafeIds.isEmpty() -> EmptyState(Icons.Rounded.LocalCafe, stringResource(R.string.cafe_no_category))
                t == 0 -> CafeList(items, shops, drinks, onEdit)
                else -> CafeStats(items)
            }
        }
    }
}

@Composable
private fun SessionBadge(session: Session) {
    Surface(shape = CircleShape, color = session.color.copy(alpha = 0.15f), contentColor = session.color) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, Modifier.size(13.dp)); Spacer(Modifier.width(4.dp))
            Text(sessionLabel(session), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

private const val CAFE_PAGE = 20

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafeList(all: List<CafeItem>, shops: List<CafeShop>, drinks: List<CafeDrink>, onEdit: (Tx) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var session by rememberSaveable { mutableStateOf("all") }
    var shopId by rememberSaveable { mutableStateOf("all") }
    var drinkId by rememberSaveable { mutableStateOf("all") }
    var newest by rememberSaveable { mutableStateOf(true) }
    var from by rememberSaveable { mutableStateOf<Long?>(null) }
    var to by rememberSaveable { mutableStateOf<Long?>(null) }
    var open by rememberSaveable { mutableStateOf(false) }
    var limit by rememberSaveable { mutableIntStateOf(CAFE_PAGE) }
    var viewer by remember { mutableStateOf<Pair<List<LightboxItem>, Int>?>(null) }

    val active = listOf(search.isNotBlank(), session != "all", shopId != "all", drinkId != "all", !newest, from != null, to != null).count { it }
    val filtered = remember(all, search, session, shopId, drinkId, newest, from, to) {
        val q = normalizeText(search)
        all.filter {
            (session == "all" || sessionOf(it.tx.date).name == session) &&
                (shopId == "all" || (if (shopId == "none") it.shop == null else it.log?.shopId == shopId)) &&
                (drinkId == "all" || (if (drinkId == "none") it.drink == null else it.log?.drinkId == drinkId)) &&
                (from == null || it.tx.date >= from!!) && (to == null || it.tx.date <= to!!) &&
                (q.isEmpty() || normalizeText("${it.tx.note} ${it.shop?.name} ${it.drink?.name} ${it.shop?.address}").contains(q))
        }.let { if (newest) it.sortedByDescending { x -> x.tx.date } else it.sortedBy { x -> x.tx.date } }
    }
    val total = filtered.sumOf { it.tx.amount }
    val visible = filtered.take(limit)
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0; last >= listState.layoutInfo.totalItemsCount - 4 } }
    LaunchedEffect(nearEnd, filtered.size) { if (nearEnd && limit < filtered.size) limit += CAFE_PAGE }

    fun photoOf(it: CafeItem) = it.log?.imageUrl?.takeIf { u -> u.isNotBlank() } ?: it.drink?.imageUrl?.takeIf { u -> u.isNotBlank() } ?: it.shop?.imageUrl?.takeIf { u -> u.isNotBlank() }
    val none = stringResource(R.string.cafe_unspecified)
    val reset = { search = ""; session = "all"; shopId = "all"; drinkId = "all"; newest = true; from = null; to = null; limit = CAFE_PAGE }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "filters") {
            AccordionCard(stringResource(R.string.filters_title), Icons.Rounded.Tune, open, { open = !open }, badge = active) {
                SearchField(search, { search = it; limit = CAFE_PAGE }, stringResource(R.string.cafe_filter_search))
                OptionSelect(stringResource(R.string.cafe_shop), listOf("all" to stringResource(R.string.cafe_all_shops), "none" to none) + shops.map { it.id to it.name }, shopId, { shopId = it; limit = CAFE_PAGE })
                OptionSelect(stringResource(R.string.cafe_drink), listOf("all" to stringResource(R.string.cafe_all_drinks), "none" to none) + drinks.map { it.id to it.name }, drinkId, { drinkId = it; limit = CAFE_PAGE })
                OptionSelect(stringResource(R.string.cafe_session), listOf("all" to stringResource(R.string.cafe_all_sessions)) + Session.entries.map { it.name to stringResource(sessionRes(it)) }, session, { session = it; limit = CAFE_PAGE })
                OptionSelect(stringResource(R.string.tx_sort), listOf("new" to stringResource(R.string.tx_sort_newest), "old" to stringResource(R.string.tx_sort_oldest)), if (newest) "new" else "old", { newest = it == "new" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateSelect(stringResource(R.string.filters_from), from, { from = it?.let(::startOfDay); limit = CAFE_PAGE }, Modifier.weight(1f))
                    DateSelect(stringResource(R.string.filters_to), to, { to = it?.let(::endOfDay); limit = CAFE_PAGE }, Modifier.weight(1f))
                }
                TextButton(onClick = reset, enabled = active > 0, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.filters_reset)) }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyState(Icons.Rounded.LocalCafe, stringResource(if (all.isEmpty()) R.string.cafe_empty else R.string.tx_no_match), action = if (all.isNotEmpty()) stringResource(R.string.filters_reset) to reset else null) }
        } else {
            item(key = "count") { Text("${stringResource(R.string.cafe_moment_count, filtered.size)} · ${Format.money(total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(visible, key = { it.tx.id }) { item ->
                val photo = photoOf(item)
                val session0 = sessionOf(item.tx.date)
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, onClick = { onEdit(item.tx) }) {
                    Column {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            RemoteImage(photo, Modifier.fillMaxSize(), Icons.Rounded.LocalCafe)
                            if (photo != null) Box(Modifier.fillMaxSize().clickable {
                                val withPhotos = filtered.mapNotNull { f -> photoOf(f)?.let { u -> f to u } }
                                viewer = withPhotos.map { (f, u) -> LightboxItem(u, f.drink?.name ?: f.shop?.name ?: "") } to withPhotos.indexOfFirst { it.first.tx.id == item.tx.id }.coerceAtLeast(0)
                            })
                        }
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(item.drink?.name ?: stringResource(R.string.cafe_unspecified_drink), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(12.dp))
                                Text(Format.money(item.tx.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.expenser.app.ui.theme.LocalIncomeColors.current.expense)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Storefront, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(6.dp))
                                Text(item.shop?.name ?: stringResource(R.string.cafe_unspecified_shop), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${Format.date(item.tx.date)} · ${Format.time(item.tx.date)}" + (item.wallet?.let { " · ${it.name}" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SessionBadge(session0)
                            }
                            if (item.tx.note.isNotBlank()) Text(item.tx.note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
    ScrollToTop(listState)
    }
    viewer?.let { (list, idx) -> ImageLightbox(list, idx) { viewer = null } }
}

@Composable
private fun sessionRes(s: Session) = when (s) {
    Session.MORNING -> R.string.cafe_session_morning
    Session.NOON -> R.string.cafe_session_noon
    Session.AFTERNOON -> R.string.cafe_session_afternoon
    Session.EVENING -> R.string.cafe_session_evening
    Session.NIGHT -> R.string.cafe_session_night
}

private data class AggRow(val key: String, val name: String, val visits: Int, val amount: Long, val color: Color)

private fun aggregate(items: List<CafeItem>, keyFn: (CafeItem) -> String, labelFn: (String, CafeItem) -> String, colorFn: (String, Int) -> Color, otherLabel: String, maxSlices: Int = 8): List<AggRow> {
    val map = LinkedHashMap<String, AggRow>()
    items.forEach { it0 ->
        val k = keyFn(it0)
        val cur = map[k] ?: AggRow(k, labelFn(k, it0), 0, 0, Color.Gray)
        map[k] = cur.copy(visits = cur.visits + 1, amount = cur.amount + it0.tx.amount)
    }
    val rows = map.values.sortedWith(compareByDescending<AggRow> { it.visits }.thenByDescending { it.amount })
    val head = rows.take(maxSlices).mapIndexed { i, r -> r.copy(color = colorFn(r.key, i)) }.toMutableList()
    val tail = rows.drop(maxSlices)
    if (tail.isNotEmpty()) head.add(AggRow("__other", otherLabel, tail.sumOf { it.visits }, tail.sumOf { it.amount }, Color(0xFF94A3B8)))
    return head
}

@Composable
private fun PieWithBars(rows: List<AggRow>, byAmount: Boolean) {
    val total = rows.sumOf { if (byAmount) it.amount else it.visits.toLong() }
    if (rows.isEmpty() || total == 0L) { EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.common_no_data)); return }
    val ctx = LocalContext.current
    DonutWithLegend(
        rows.map { Slice(it.name, if (byAmount) it.amount else it.visits.toLong(), it.color) },
        size = 250.dp,
        formatValue = { v -> if (byAmount) Format.money(v) else ctx.getString(R.string.cafe_visits, v.toInt()) },
        centerValue = if (byAmount) Format.compactMoney(total) else "$total",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafeStats(items: List<CafeItem>) {
    var range by rememberSaveable { mutableStateOf(Range.MONTH) }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val period = remember(range, anchor) { periodFor(range, anchor) }
    val other = stringResource(R.string.common_other)
    val unspecified = stringResource(R.string.cafe_unspecified)

    val label = when (range) {
        Range.WEEK -> "${Format.dateShort(period.start)} – ${Format.dateShort(period.fullEnd)}/${period.fullEnd.field(Calendar.YEAR)}"
        Range.MONTH -> Format.monthYear(anchor)
        else -> stringResource(R.string.stats_year_label, anchor.field(Calendar.YEAR))
    }
    val inPeriod = remember(items, period) { items.filter { it.tx.date in period.start..period.fullEnd } }
    val total = inPeriod.sumOf { it.tx.amount }
    val days = inPeriod.map { dayKey(it.tx.date) }.toSet().size
    val avg = if (inPeriod.isEmpty()) 0L else Math.round(total.toDouble() / inPeriod.size)

    val sessionRows = remember(inPeriod) { aggregate(inPeriod, { sessionOf(it.tx.date).name }, { k, _ -> k }, { k, _ -> Session.valueOf(k).color }, other).sortedBy { r -> Session.entries.indexOfFirst { it.name == r.key }.let { if (it < 0) 99 else it } } }
    val drinkRows = remember(inPeriod) { aggregate(inPeriod, { it.log?.drinkId ?: "" }, { k, it0 -> if (k.isEmpty()) unspecified else it0.drink?.name ?: unspecified }, { k, i -> chartColor(i, k.isNotEmpty()) }, other) }
    val shopRows = remember(inPeriod) { aggregate(inPeriod, { it.log?.shopId ?: "" }, { k, it0 -> if (k.isEmpty()) unspecified else it0.shop?.name ?: unspecified }, { k, i -> chartColor(i, k.isNotEmpty()) }, other) }
    val topShop = shopRows.firstOrNull { it.key.isNotEmpty() && it.key != "__other" }
    val topDrink = drinkRows.firstOrNull { it.key.isNotEmpty() && it.key != "__other" }

    // Localise session names after aggregation (keys are enum names).
    val sessionNamed = sessionRows.map { r -> r }
    val statsState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = statsState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillSegmented(listOf(Seg("WEEK", stringResource(R.string.cafe_range_week)), Seg("MONTH", stringResource(R.string.cafe_range_month)), Seg("YEAR", stringResource(R.string.cafe_range_year))), range.name, { range = Range.valueOf(it); anchor = System.currentTimeMillis() })
                PeriodNavigator(label, period.isCurrent, { anchor = shiftAnchor(range, anchor, -1) }, { anchor = shiftAnchor(range, anchor, 1) }, { anchor = System.currentTimeMillis() })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Rounded.Payments, stringResource(R.string.cafe_stat_total), Format.money(total), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f).fillMaxHeight())
                    StatTile(Icons.Rounded.LocalCafe, stringResource(R.string.cafe_stat_count), "${inPeriod.size}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f).fillMaxHeight(), hint = stringResource(R.string.cafe_stat_days, days))
                }
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.cafe_stat_avg), Format.money(avg), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f).fillMaxHeight())
                    StatTile(Icons.Rounded.Storefront, stringResource(R.string.cafe_stat_top_shop), topShop?.name ?: "—", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f).fillMaxHeight(), hint = topShop?.let { stringResource(R.string.cafe_visits, it.visits) })
                }
            }
        }
        if (inPeriod.isEmpty()) {
            item { SectionCard(null) { EmptyState(Icons.Rounded.BarChart, stringResource(R.string.cafe_stats_empty)) } }
        } else {
            item {
                SectionCard(stringResource(R.string.cafe_chart_sessions)) {
                    PieWithBars(sessionNamed.map { r -> r.copy(name = if (r.key == "__other") r.name else sessionName(r.key)) }, false)
                }
            }
            item { SectionCard(stringResource(R.string.cafe_chart_drink_spend)) { PieWithBars(drinkRows.sortedByDescending { it.amount }, true) } }
            item { SectionCard(stringResource(R.string.cafe_chart_drinks)) { PieWithBars(drinkRows, false) } }
            item { SectionCard(stringResource(R.string.cafe_chart_shops)) { PieWithBars(shopRows, false) } }
        }
    }
    ScrollToTop(statsState)
    }
}

@Composable
private fun sessionName(key: String): String = sessionLabel(Session.valueOf(key))

// ---------------------------------------------------------------- shops

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeShopsScreen(vm: MainViewModel) {
    val shops by vm.shops.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editor by remember { mutableStateOf<ShopEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<CafeShop?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var viewer by remember { mutableStateOf<Pair<List<LightboxItem>, Int>?>(null) }
    val usage = remember(logs) { logs.groupingBy { it.shopId }.eachCount() }
    val list = remember(shops, search) { val q = normalizeText(search); if (q.isEmpty()) shops else shops.filter { normalizeText(it.name).contains(q) || normalizeText(it.address).contains(q) } }
    val listState = rememberLazyListState()
    val reorder = rememberReorder(list.map { it.id }, listState) { ids -> vm.reorderShops(ids) }
    val byId = list.associateBy { it.id }
    val shown = reorder.display(list.map { it.id }).mapNotNull { byId[it] }

    var compact by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(search, { search = it }, stringResource(R.string.cafe_shop_search), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ViewToggleButton(if (compact) Icons.Rounded.ViewAgenda else Icons.AutoMirrored.Rounded.ViewList, stringResource(if (compact) R.string.view_cards else R.string.view_list), { compact = !compact })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
            if (list.isNotEmpty()) item { SwipeHint() }
            if (list.isEmpty()) item { EmptyState(Icons.Rounded.Storefront, if (search.isBlank()) stringResource(R.string.cafe_shop_empty) else stringResource(R.string.cafe_no_match, search)) }
            items(shown, key = { it.id }) { s ->
                SwipeRow(onEdit = { editor = ShopEditorState(s) }, onDelete = { deleteTarget = s }, shape = RoundedCornerShape(24.dp), modifier = reorderable(reorder, s.id)) {
                    if (compact) ShopCompactRow(
                        s, usage[s.id] ?: 0,
                        onImage = if (s.imageUrl.isNotBlank()) ({
                            val withImg = shown.filter { it.imageUrl.isNotBlank() }
                            viewer = withImg.map { LightboxItem(it.imageUrl, it.name) } to withImg.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
                        }) else null,
                        handle = if (search.isBlank()) ({ DragHandle(reorder, s.id) }) else null,
                    ) else Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).then(if (s.imageUrl.isNotBlank()) Modifier.clickable {
                                val withImg = shown.filter { it.imageUrl.isNotBlank() }
                                viewer = withImg.map { LightboxItem(it.imageUrl, it.name) } to withImg.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
                            } else Modifier)) {
                                RemoteImage(s.imageUrl, Modifier.fillMaxSize(), Icons.Rounded.Storefront)
                                if (s.rating > 0) Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Color(0xFFFBBF24)); Spacer(Modifier.width(4.dp))
                                        Text("${s.rating}/5", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                }
                            }
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { editor = ShopEditorState(s) }, Modifier.size(36.dp)) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), Modifier.size(18.dp)) }
                                    IconButton(onClick = { deleteTarget = s }, Modifier.size(36.dp)) { Icon(Icons.Rounded.Delete, stringResource(R.string.common_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (s.rating > 0) Row { repeat(5) { i -> Icon(if (i < s.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, Modifier.size(16.dp), tint = if (i < s.rating) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline) } }
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                        Row(Modifier.padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.LocalCafe, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.cafe_visits, usage[s.id] ?: 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (s.address.isNotBlank() || s.description.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    if (s.address.isNotBlank()) Row(verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Rounded.LocationOn, null, Modifier.size(18.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                                        Text(s.address, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (s.description.isNotBlank()) Text(s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                }
                                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (s.mapUrl.isNotBlank()) {
                                        val url = normalizeUrl(s.mapUrl)
                                        Button(
                                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { vm.toast(R.string.cafe_map_open_error) } },
                                            Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                        ) { Icon(Icons.Rounded.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.cafe_open_map), maxLines = 1) }
                                        CopyButton(url, onCopied = { vm.toast(R.string.cafe_map_copied) })
                                    } else {
                                        TextButton(onClick = { editor = ShopEditorState(s) }, Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cafe_add_map)) }
                                    }
                                    if (search.isBlank()) DragHandle(reorder, s.id)
                                }
                            }
                        }
                    }
                }
            }
        }
        ScrollToTop(listState)
        ScreenFab({ editor = ShopEditorState(null) }, Modifier.align(Alignment.BottomEnd))
        }
    }
    viewer?.let { (l, i) -> ImageLightbox(l, i) { viewer = null } }
    editor?.let { st -> ShopEditor(st, shops, vm) { editor = null } }
    deleteTarget?.let { s -> ConfirmDialog(stringResource(R.string.cafe_shop_confirm_delete, s.name), onConfirm = { vm.deleteShop(s) }, onDismiss = { deleteTarget = null }) }
}

data class ShopEditorState(val editing: CafeShop?)

/** Slim horizontal card used by the list view (easier to drag-sort than the tall photo card). */
@Composable
private fun ShopCompactRow(s: CafeShop, visits: Int, onImage: (() -> Unit)?, handle: (@Composable () -> Unit)?) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).then(if (onImage != null) Modifier.clickable(onClick = onImage) else Modifier)) {
                RemoteImage(s.imageUrl, Modifier.fillMaxSize(), Icons.Rounded.Storefront)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.rating > 0) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Color(0xFFF59E0B)); Spacer(Modifier.width(2.dp))
                        Text("${s.rating}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.cafe_visits, visits), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s.address.isNotBlank()) Text(s.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            handle?.invoke()
        }
    }
}

@Composable
private fun ShopEditor(state: ShopEditorState, all: List<CafeShop>, vm: MainViewModel, onDismiss: () -> Unit) {
    val e = state.editing
    var name by remember { mutableStateOf(e?.name ?: "") }
    var address by remember { mutableStateOf(e?.address ?: "") }
    var mapUrl by remember { mutableStateOf(e?.mapUrl ?: "") }
    var imageUrl by remember { mutableStateOf(e?.imageUrl ?: "") }
    var description by remember { mutableStateOf(e?.description ?: "") }
    var rating by remember { mutableIntStateOf(e?.rating ?: 0) }
    var submitted by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    val err = when {
        trimmed.isEmpty() -> stringResource(R.string.validation_shop_name_required)
        trimmed.length > 60 -> stringResource(R.string.validation_shop_name_max)
        all.any { it.id != e?.id && it.name.equals(trimmed, ignoreCase = true) } -> stringResource(R.string.validation_shop_exists)
        else -> null
    }
    val mapErr = if (mapUrl.isNotBlank() && !(mapUrl.trim().startsWith("http://") || mapUrl.trim().startsWith("https://"))) stringResource(R.string.validation_map_url_invalid) else null
    FormScreen(
        stringResource(if (e == null) R.string.cafe_shop_add_title else R.string.cafe_shop_edit_title), stringResource(R.string.common_save), onClose = onDismiss,
        onConfirm = { submitted = true; if (err == null && mapErr == null) vm.saveShop(e, trimmed, address, mapUrl, imageUrl, description, rating, onDismiss) },
    ) {
        run {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.cafe_shop_name)) }, singleLine = true, isError = submitted && err != null, supportingText = { if (submitted && err != null) Text(err) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                OutlinedTextField(address, { address = it.take(200) }, label = { Text(stringResource(R.string.cafe_address)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(mapUrl, { mapUrl = it }, label = { Text(stringResource(R.string.cafe_map_url)) }, supportingText = { Text(if (submitted && mapErr != null) mapErr else stringResource(R.string.cafe_map_url_hint)) }, isError = submitted && mapErr != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text(stringResource(R.string.cafe_photo)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it.take(500) }, label = { Text(stringResource(R.string.cafe_description)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.cafe_rating), style = MaterialTheme.typography.titleSmall)
                Row { (1..5).forEach { i -> IconButton(onClick = { rating = if (rating == i) 0 else i }) { Icon(if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.cafe_stars_n, i), tint = if (i <= rating) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline) } } }
            }
        }
    }
}

// ---------------------------------------------------------------- drinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeDrinksScreen(vm: MainViewModel) {
    val drinks by vm.drinks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<DrinkEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<CafeDrink?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var viewer by remember { mutableStateOf<Pair<List<LightboxItem>, Int>?>(null) }
    val usage = remember(logs) { logs.groupingBy { it.drinkId }.eachCount() }
    val list = remember(drinks, search) { val q = normalizeText(search); if (q.isEmpty()) drinks else drinks.filter { normalizeText(it.name).contains(q) } }
    val listState = rememberLazyListState()
    val reorder = rememberReorder(list.map { it.id }, listState) { ids -> vm.reorderDrinks(ids) }
    val byId = list.associateBy { it.id }
    val shown = reorder.display(list.map { it.id }).mapNotNull { byId[it] }

    var grid by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    fun openViewer(d: CafeDrink) {
        val withImg = list.filter { it.imageUrl.isNotBlank() }
        viewer = withImg.map { LightboxItem(it.imageUrl, it.name) } to withImg.indexOfFirst { it.id == d.id }.coerceAtLeast(0)
    }
    val emptyText = if (search.isBlank()) stringResource(R.string.cafe_drink_empty) else stringResource(R.string.cafe_no_match, search)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(search, { search = it }, stringResource(R.string.cafe_drink_search), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ViewToggleButton(if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView, stringResource(if (grid) R.string.view_list else R.string.view_grid), { grid = !grid })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (grid) {
                if (list.isEmpty()) EmptyState(Icons.Rounded.Coffee, emptyText)
                else LazyVerticalGrid(
                    GridCells.Fixed(2), state = gridState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(list, key = { it.id }) { d ->
                        DrinkGridCard(d, usage[d.id] ?: 0, onImage = { openViewer(d) }, onEdit = { editor = DrinkEditorState(d) }, onDelete = { deleteTarget = d })
                    }
                }
                ScrollToTop(gridState)
            } else {
                LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (list.isEmpty()) item { EmptyState(Icons.Rounded.Coffee, emptyText) } else item { SwipeHint() }
                    items(shown, key = { it.id }) { d ->
                        SwipeRow(onEdit = { editor = DrinkEditorState(d) }, onDelete = { deleteTarget = d }, modifier = reorderable(reorder, d.id)) {
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).then(if (d.imageUrl.isNotBlank()) Modifier.clickable { openViewer(d) } else Modifier)) {
                                        RemoteImage(d.imageUrl, Modifier.fillMaxSize(), Icons.Rounded.Coffee)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(d.name, style = MaterialTheme.typography.titleSmall)
                                        Text(stringResource(R.string.cafe_drink_usage, usage[d.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (search.isBlank()) DragHandle(reorder, d.id)
                                }
                            }
                        }
                    }
                }
                ScrollToTop(listState)
            }
            ScreenFab({ editor = DrinkEditorState(null) }, Modifier.align(Alignment.BottomEnd))
        }
    }
    viewer?.let { (l, i) -> ImageLightbox(l, i) { viewer = null } }
    editor?.let { st -> DrinkEditor(st, drinks, vm) { editor = null } }
    deleteTarget?.let { d -> ConfirmDialog(stringResource(R.string.cafe_drink_confirm_delete, d.name), onConfirm = { vm.deleteDrink(d) }, onDismiss = { deleteTarget = null }) }
}

data class DrinkEditorState(val editing: CafeDrink?)

@Composable
private fun DrinkGridCard(d: CafeDrink, uses: Int, onImage: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).then(if (d.imageUrl.isNotBlank()) Modifier.clickable(onClick = onImage) else Modifier)) {
                RemoteImage(d.imageUrl, Modifier.fillMaxSize(), Icons.Rounded.Coffee)
            }
            Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.cafe_drink_usage, uses), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onEdit, Modifier.size(32.dp)) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, Modifier.size(32.dp)) { Icon(Icons.Rounded.Delete, stringResource(R.string.common_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DrinkEditor(state: DrinkEditorState, all: List<CafeDrink>, vm: MainViewModel, onDismiss: () -> Unit) {
    val e = state.editing
    var name by remember { mutableStateOf(e?.name ?: "") }
    var imageUrl by remember { mutableStateOf(e?.imageUrl ?: "") }
    var description by remember { mutableStateOf(e?.description ?: "") }
    var submitted by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    val err = when {
        trimmed.isEmpty() -> stringResource(R.string.validation_drink_name_required)
        trimmed.length > 60 -> stringResource(R.string.validation_drink_name_max)
        all.any { it.id != e?.id && it.name.equals(trimmed, ignoreCase = true) } -> stringResource(R.string.validation_drink_exists)
        else -> null
    }
    FormScreen(
        stringResource(if (e == null) R.string.cafe_drink_add_title else R.string.cafe_drink_edit_title), stringResource(R.string.common_save), onClose = onDismiss,
        onConfirm = { submitted = true; if (err == null) vm.saveDrink(e, trimmed, imageUrl, description, onDismiss) },
    ) {
        run {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.cafe_drink_name)) }, singleLine = true, isError = submitted && err != null, supportingText = { if (submitted && err != null) Text(err) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text(stringResource(R.string.cafe_photo)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it.take(500) }, label = { Text(stringResource(R.string.cafe_description)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun normalizeUrl(u: String): String = u.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
