package com.expenser.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.*
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.*
import java.util.Calendar

private const val PAGE = 60

private enum class TxView { LIST, CALENDAR }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(vm: MainViewModel, onEdit: (Tx) -> Unit) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val catById = remember(categories) { categories.associateBy { it.id } }
    val walletById = remember(wallets) { wallets.associateBy { it.id } }

    var view by rememberSaveable { mutableStateOf(TxView.LIST) }
    var search by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("all") }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var walletFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var from by rememberSaveable { mutableStateOf<Long?>(periodFor(Range.MONTH, System.currentTimeMillis()).start) }
    var to by rememberSaveable { mutableStateOf<Long?>(periodFor(Range.MONTH, System.currentTimeMillis()).fullEnd) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var limit by rememberSaveable { mutableIntStateOf(PAGE) }
    var deleteTarget by remember { mutableStateOf<Tx?>(null) }
    val listState0 = rememberLazyListState()

    val monthDefault = remember { periodFor(Range.MONTH, System.currentTimeMillis()) }
    val activeFilters = listOf(typeFilter != "all", categoryFilter != null, walletFilter != null, from != monthDefault.start || to != monthDefault.fullEnd, !newestFirst).count { it }

    val filtered = remember(transactions, search, typeFilter, categoryFilter, walletFilter, from, to, newestFirst, catById) {
        val q = search.trim().lowercase()
        transactions.filter { tx ->
            (typeFilter == "all" || tx.type == typeFilter) &&
                (categoryFilter == null || tx.categoryId == categoryFilter) &&
                (walletFilter == null || tx.walletId == walletFilter || tx.fromWalletId == walletFilter || tx.toWalletId == walletFilter) &&
                (from == null || tx.date >= from!!) && (to == null || tx.date <= to!!) &&
                (q.isEmpty() || tx.note.lowercase().contains(q) || (catById[tx.categoryId ?: ""]?.name?.lowercase()?.contains(q) == true))
        }.let { if (newestFirst) it else it.reversed() }
    }

    val monthStats = remember(transactions) {
        var inc = 0L; var exp = 0L
        val mk = monthKey(System.currentTimeMillis())
        transactions.forEach { if (monthKey(it.date) == mk) { if (it.type == TxType.INCOME) inc += it.amount else if (it.type == TxType.EXPENSE) exp += it.amount } }
        inc to exp
    }

    Column(Modifier.fillMaxSize()) {
        val inc = LocalIncomeColors.current
        LazyColumn(Modifier.weight(1f), state = listState0, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.tx_month_income), Format.money(monthStats.first), inc.container, inc.onContainer, Modifier.weight(1f))
                    StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.tx_month_expense), Format.money(monthStats.second), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f))
                }
            }
            item(key = "view") {
                PillSegmented(
                    listOf(Seg(TxView.LIST.name, stringResource(R.string.tx_view_list), Icons.AutoMirrored.Rounded.ViewList), Seg(TxView.CALENDAR.name, stringResource(R.string.tx_view_calendar), Icons.Rounded.CalendarMonth)),
                    view.name, { view = TxView.valueOf(it) },
                )
            }
            item(key = "filters") {
                AccordionCard(stringResource(R.string.filters_title), Icons.Rounded.FilterList, showFilters, { showFilters = !showFilters }, badge = activeFilters) {
                    FiltersPanel(
                        categories = categories, wallets = wallets, search = search, onSearch = { search = it; limit = PAGE },
                        type = typeFilter, onType = { typeFilter = it; categoryFilter = null; limit = PAGE },
                        categoryId = categoryFilter, onCategory = { categoryFilter = it; limit = PAGE },
                        walletId = walletFilter, onWallet = { walletFilter = it; limit = PAGE },
                        from = from, to = to, onRange = { f, t -> from = f; to = t; limit = PAGE },
                        newestFirst = newestFirst, onSort = { newestFirst = it },
                        onReset = { search = ""; typeFilter = "all"; categoryFilter = null; walletFilter = null; from = monthDefault.start; to = monthDefault.fullEnd; newestFirst = true; limit = PAGE },
                    )
                }
            }
            if (view == TxView.CALENDAR) {
                item(key = "calendar") { CalendarView(transactions, catById, walletById, onEdit) }
            } else if (loading) {
                item { ListSkeleton(rows = 7) }
            } else if (filtered.isEmpty()) {
                item { EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(if (transactions.isEmpty()) R.string.dashboard_no_tx else R.string.tx_no_match)) }
            } else {
                val visible = filtered.take(limit)
                val groups = visible.groupBy { dayKey(it.date) }.toList()
                item(key = "count") { Text(stringResource(R.string.tx_result_count, filtered.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp)) }
                groups.forEach { (day, dayItems) ->
                    item(key = "h$day") {
                        val di = dayItems.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                        val de = dayItems.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Format.dayHeader(dayItems.first().date), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (di > 0) Text("+" + Format.compactMoney(di), style = MaterialTheme.typography.labelMedium, color = inc.income, modifier = Modifier.padding(end = 8.dp))
                            if (de > 0) Text("-" + Format.compactMoney(de), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items(dayItems, key = { it.id }) { tx ->
                        SwipeRow(onEdit = if (tx.type != TxType.TRANSFER) ({ onEdit(tx) }) else null, onDelete = { deleteTarget = tx }, modifier = Modifier.animateItem()) {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                                TxRow(tx, catById[tx.categoryId ?: ""], walletById, showDate = false, onClick = if (tx.type != TxType.TRANSFER) ({ onEdit(tx) }) else null)
                            }
                        }
                    }
                }
            }
        }
    }
    // Infinite scroll: reveal the next page when the end is near.
    val nearEnd by remember { derivedStateOf { val last = listState0.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0; last >= listState0.layoutInfo.totalItemsCount - 8 } }
    LaunchedEffect(nearEnd, filtered.size) { if (nearEnd && limit < filtered.size) limit += PAGE }

    deleteTarget?.let { tx ->
        ConfirmDialog(stringResource(R.string.tx_confirm_delete), onConfirm = { vm.deleteTransaction(tx) }, onDismiss = { deleteTarget = null })
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersPanel(
    search: String, onSearch: (String) -> Unit,
    categories: List<Category>, wallets: List<Wallet>,
    type: String, onType: (String) -> Unit,
    categoryId: String?, onCategory: (String?) -> Unit,
    walletId: String?, onWallet: (String?) -> Unit,
    from: Long?, to: Long?, onRange: (Long?, Long?) -> Unit,
    newestFirst: Boolean, onSort: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchField(search, onSearch, stringResource(R.string.filters_search))
        OptionSelect(
            stringResource(R.string.filters_type),
            listOf("all" to stringResource(R.string.filters_all_types), TxType.INCOME to stringResource(R.string.tx_income), TxType.EXPENSE to stringResource(R.string.tx_expense), TxType.TRANSFER to stringResource(R.string.tx_transfer)),
            type, onType,
        )
        if (type == TxType.INCOME || type == TxType.EXPENSE) {
            OptionSelect(
                stringResource(R.string.tx_category),
                listOf("" to stringResource(R.string.filters_all_categories)) + categories.filter { it.type == type }.map { it.id to "${it.emoji} ${it.name}" },
                categoryId ?: "", { onCategory(it.ifEmpty { null }) },
            )
        }
        OptionSelect(
            stringResource(R.string.tx_wallet),
            listOf("" to stringResource(R.string.filters_all_wallets)) + wallets.map { it.id to it.name },
            walletId ?: "", { onWallet(it.ifEmpty { null }) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateSelect(stringResource(R.string.filters_from), from, { onRange(it?.let(::startOfDay), to) }, Modifier.weight(1f))
            DateSelect(stringResource(R.string.filters_to), to, { onRange(from, it?.let(::endOfDay)) }, Modifier.weight(1f))
        }
        OptionSelect(
            stringResource(R.string.tx_sort),
            listOf("new" to stringResource(R.string.tx_sort_newest), "old" to stringResource(R.string.tx_sort_oldest)),
            if (newestFirst) "new" else "old", { onSort(it == "new") },
        )
        Row {
            TextButton(onClick = { onRange(null, null) }) { Text(stringResource(R.string.tx_show_all)) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) { Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.filters_reset)) }
        }
    }
}

/** Month grid with net income / expense per day; tapping a day lists its transactions below. */
@Composable
private fun CalendarView(transactions: List<Tx>, catById: Map<String, Category>, walletById: Map<String, Wallet>, onEdit: (Tx) -> Unit) {
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val period = remember(anchor) { periodFor(Range.MONTH, anchor) }
    val inMonth = remember(transactions, period) { transactions.filter { it.date in period.start..period.fullEnd && it.type != TxType.TRANSFER } }
    val byDay = remember(inMonth) { inMonth.groupBy { dayKey(it.date) } }
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val income = LocalIncomeColors.current.income

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PeriodNavigator(Format.monthYear(anchor), period.isCurrent, { anchor = shiftAnchor(Range.MONTH, anchor, -1); selected = null }, { anchor = shiftAnchor(Range.MONTH, anchor, 1); selected = null }, { anchor = System.currentTimeMillis(); selected = null })
        Row { weekdays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        val first = Calendar.getInstance().apply { timeInMillis = period.start }
        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = (offset + days + 6) / 7
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    if (dayNum < 1 || dayNum > days) { Spacer(Modifier.weight(1f)) } else {
                        val dayStart = Calendar.getInstance().apply { timeInMillis = period.start; set(Calendar.DAY_OF_MONTH, dayNum) }.timeInMillis.let { startOfDay(it) }
                        val list = byDay[dayStart].orEmpty()
                        val net = list.sumOf { if (it.type == TxType.INCOME) it.amount else -it.amount }
                        val isSel = selected == dayStart
                        Surface(
                            modifier = Modifier.weight(1f).height(54.dp), onClick = { selected = if (isSel) null else dayStart },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(Modifier.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("$dayNum", style = MaterialTheme.typography.labelMedium, fontWeight = if (dayKey(System.currentTimeMillis()) == dayStart) FontWeight.Bold else FontWeight.Normal)
                                if (list.isNotEmpty()) Text(Format.compactMoney(net), style = MaterialTheme.typography.labelSmall, color = if (net >= 0) income else MaterialTheme.colorScheme.error, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        val sel = selected
        if (sel != null) {
            val list = byDay[sel].orEmpty()
            if (list.isEmpty()) Text(stringResource(R.string.calendar_no_tx), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Column { list.forEach { TxRow(it, catById[it.categoryId ?: ""], walletById, showDate = false, onClick = { onEdit(it) }) } }
        }
    }
}
