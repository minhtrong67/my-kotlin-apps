package com.expenser.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var tab by rememberSaveable { mutableIntStateOf(0) }

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
            tab.toString(), { tab = it.toInt() }, Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp),
        )
        when {
            cafeIds.isEmpty() -> EmptyState(Icons.Rounded.LocalCafe, stringResource(R.string.cafe_no_category))
            tab == 0 -> CafeList(items, shops, drinks, onEdit)
            else -> CafeStats(items)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafeList(all: List<CafeItem>, shops: List<CafeShop>, drinks: List<CafeDrink>, onEdit: (Tx) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var session by rememberSaveable { mutableStateOf<Session?>(null) }
    var shopId by rememberSaveable { mutableStateOf<String?>(null) }
    var drinkId by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = remember(all, search, session, shopId, drinkId) {
        val q = normalizeText(search)
        all.filter {
            (session == null || sessionOf(it.tx.date) == session) &&
                (shopId == null || it.log?.shopId == shopId) && (drinkId == null || it.log?.drinkId == drinkId) &&
                (q.isEmpty() || normalizeText(it.tx.note).contains(q) || normalizeText(it.shop?.name).contains(q) || normalizeText(it.drink?.name).contains(q))
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(search, { search = it }, stringResource(R.string.cafe_filter_search))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(session == null, { session = null }, { Text(stringResource(R.string.cafe_all_sessions)) })
                    Session.entries.forEach { s -> FilterChip(session == s, { session = if (session == s) null else s }, { Text(sessionLabel(s)) }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(shopId == null, { shopId = null }, { Text(stringResource(R.string.cafe_all_shops)) })
                    shops.forEach { s -> FilterChip(shopId == s.id, { shopId = if (shopId == s.id) null else s.id }, { Text(s.name) }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(drinkId == null, { drinkId = null }, { Text(stringResource(R.string.cafe_all_drinks)) })
                    drinks.forEach { d -> FilterChip(drinkId == d.id, { drinkId = if (drinkId == d.id) null else d.id }, { Text(d.name) }) }
                }
            }
        }
        if (filtered.isEmpty()) item { EmptyState(Icons.Rounded.LocalCafe, stringResource(if (all.isEmpty()) R.string.cafe_empty else R.string.tx_no_match)) }
        items(filtered, key = { it.tx.id }) { item ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, onClick = { onEdit(item.tx) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val photo = item.log?.imageUrl.orEmpty()
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).then(Modifier), contentAlignment = Alignment.Center) {
                        if (photo.isNotBlank()) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.LocalCafe, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) } }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.drink?.name ?: stringResource(R.string.cafe_unspecified_drink), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.shop?.name ?: stringResource(R.string.cafe_unspecified_shop), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${Format.dateShort(item.tx.date)} · ${Format.time(item.tx.date)} · ${sessionLabel(sessionOf(item.tx.date))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.tx.note.isNotBlank()) Text(item.tx.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(Format.money(item.tx.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DonutChart(rows.map { Slice(it.name, if (byAmount) it.amount else it.visits.toLong(), it.color) }, size = 180.dp)
    }
    Spacer(Modifier.height(8.dp))
    rows.forEach { r ->
        val v = if (byAmount) r.amount else r.visits.toLong()
        ProgressRow(r.name, r.color, if (byAmount) Format.money(r.amount) else stringResource(R.string.cafe_visits, r.visits), (v * 100.0 / total).roundToInt())
    }
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
    val drinkRows = remember(inPeriod) { aggregate(inPeriod, { it.log?.drinkId ?: "" }, { k, it0 -> if (k.isEmpty()) unspecified else it0.drink?.name ?: unspecified }, { k, i -> if (k.isEmpty()) Color(0xFF94A3B8) else CHART_PALETTE[i % CHART_PALETTE.size] }, other) }
    val shopRows = remember(inPeriod) { aggregate(inPeriod, { it.log?.shopId ?: "" }, { k, it0 -> if (k.isEmpty()) unspecified else it0.shop?.name ?: unspecified }, { k, i -> if (k.isEmpty()) Color(0xFF94A3B8) else CHART_PALETTE[i % CHART_PALETTE.size] }, other) }
    val topShop = shopRows.firstOrNull { it.key.isNotEmpty() && it.key != "__other" }
    val topDrink = drinkRows.firstOrNull { it.key.isNotEmpty() && it.key != "__other" }

    // Localise session names after aggregation (keys are enum names).
    val sessionNamed = sessionRows.map { r -> r }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillSegmented(listOf(Seg("WEEK", stringResource(R.string.cafe_range_week)), Seg("MONTH", stringResource(R.string.cafe_range_month)), Seg("YEAR", stringResource(R.string.cafe_range_year))), range.name, { range = Range.valueOf(it); anchor = System.currentTimeMillis() })
                PeriodNavigator(label, period.isCurrent, { anchor = shiftAnchor(range, anchor, -1) }, { anchor = shiftAnchor(range, anchor, 1) }, { anchor = System.currentTimeMillis() })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Rounded.Payments, stringResource(R.string.cafe_stat_total), Format.money(total), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f))
                    StatTile(Icons.Rounded.LocalCafe, stringResource(R.string.cafe_stat_count), "${inPeriod.size}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f), hint = stringResource(R.string.cafe_stat_days, days))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.cafe_stat_avg), Format.money(avg), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                    StatTile(Icons.Rounded.Storefront, stringResource(R.string.cafe_stat_top_shop), topShop?.name ?: "—", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f), hint = topShop?.let { stringResource(R.string.cafe_visits, it.visits) })
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
    val usage = remember(logs) { logs.groupingBy { it.shopId }.eachCount() }
    val list = remember(shops, search) { val q = normalizeText(search); if (q.isEmpty()) shops else shops.filter { normalizeText(it.name).contains(q) || normalizeText(it.address).contains(q) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SearchField(search, { search = it }, stringResource(R.string.cafe_shop_search)) }
            if (list.isEmpty()) item { EmptyState(Icons.Rounded.Storefront, if (search.isBlank()) stringResource(R.string.cafe_shop_empty) else stringResource(R.string.cafe_no_match, search)) }
            itemsIndexed(list, key = { _, s -> s.id }) { index, s ->
                SwipeRow(onEdit = { editor = ShopEditorState(s) }, onDelete = { deleteTarget = s }, shape = RoundedCornerShape(24.dp), modifier = Modifier.animateItem()) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            if (s.imageUrl.isNotBlank()) AsyncImage(s.imageUrl, null, Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(s.name, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { i -> Icon(if (i < s.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, Modifier.size(18.dp), tint = if (i < s.rating) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline) }
                                    if (s.rating == 0) Text(stringResource(R.string.cafe_no_rating), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (s.address.isNotBlank()) Text(s.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (s.description.isNotBlank()) Text(s.description, style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.cafe_visits, usage[s.id] ?: 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (s.mapUrl.isNotBlank()) TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.mapUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                                        Icon(Icons.Rounded.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cafe_open_map))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (search.isBlank()) {
                                        IconButton(enabled = index > 0, onClick = { vm.reorderShops(shops.map { it.id }.toMutableList().apply { add(index - 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
                                        IconButton(enabled = index < shops.lastIndex, onClick = { vm.reorderShops(shops.map { it.id }.toMutableList().apply { add(index + 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ScreenFab({ editor = ShopEditorState(null) }, Modifier.align(Alignment.BottomEnd))
    }
    editor?.let { st -> ShopEditor(st, shops, vm) { editor = null } }
    deleteTarget?.let { s -> ConfirmDialog(stringResource(R.string.cafe_shop_confirm_delete, s.name), onConfirm = { vm.deleteShop(s) }, onDismiss = { deleteTarget = null }) }
}

data class ShopEditorState(val editing: CafeShop?)

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
    val usage = remember(logs) { logs.groupingBy { it.drinkId }.eachCount() }
    val list = remember(drinks, search) { val q = normalizeText(search); if (q.isEmpty()) drinks else drinks.filter { normalizeText(it.name).contains(q) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SearchField(search, { search = it }, stringResource(R.string.cafe_drink_search)) }
            if (list.isEmpty()) item { EmptyState(Icons.Rounded.Coffee, if (search.isBlank()) stringResource(R.string.cafe_drink_empty) else stringResource(R.string.cafe_no_match, search)) }
            itemsIndexed(list, key = { _, d -> d.id }) { index, d ->
                SwipeRow(onEdit = { editor = DrinkEditorState(d) }, onDelete = { deleteTarget = d }, modifier = Modifier.animateItem()) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                if (d.imageUrl.isNotBlank()) AsyncImage(d.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Coffee, null) } }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.cafe_drink_usage, usage[d.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (search.isBlank()) {
                                IconButton(enabled = index > 0, onClick = { vm.reorderDrinks(drinks.map { it.id }.toMutableList().apply { add(index - 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
                                IconButton(enabled = index < drinks.lastIndex, onClick = { vm.reorderDrinks(drinks.map { it.id }.toMutableList().apply { add(index + 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
                            }
                        }
                    }
                }
            }
        }
        ScreenFab({ editor = DrinkEditorState(null) }, Modifier.align(Alignment.BottomEnd))
    }
    editor?.let { st -> DrinkEditor(st, drinks, vm) { editor = null } }
    deleteTarget?.let { d -> ConfirmDialog(stringResource(R.string.cafe_drink_confirm_delete, d.name), onConfirm = { vm.deleteDrink(d) }, onDismiss = { deleteTarget = null }) }
}

data class DrinkEditorState(val editing: CafeDrink?)

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
