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
    var categoryFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var walletFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var from by rememberSaveable { mutableStateOf<Long?>(periodFor(Range.MONTH, System.currentTimeMillis()).start) }
    var to by rememberSaveable { mutableStateOf<Long?>(periodFor(Range.MONTH, System.currentTimeMillis()).fullEnd) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var limit by rememberSaveable { mutableIntStateOf(PAGE) }
    var deleteTarget by remember { mutableStateOf<Tx?>(null) }

    val monthDefault = remember { periodFor(Range.MONTH, System.currentTimeMillis()) }
    val activeFilters = listOf(typeFilter != "all", categoryFilter != null, walletFilter != null, from != monthDefault.start || to != monthDefault.fullEnd, !newestFirst).count { it }

    val filtered = remember(transactions, search, typeFilter, categoryFilter, walletFilter, from, to, newestFirst, catById) {
        val q = search.trim().lowercase()
        transactions.filter { tx ->
            (typeFilter == "all" || tx.type == typeFilter) &&
                (categoryFilter == null || tx.categoryId == categoryFilter) &&
                (walletFilter == null || tx.walletId == walletFilter || tx.fromWalletId == walletFilter || tx.toWalletId == walletFilter) &&
                (from == null || tx.date >= from!!) && (to == null || tx.date <= to!!) &&
                (q.isEmpty() || tx.note.lowercase().contains(q) || (catById[tx.categoryId]?.name?.lowercase()?.contains(q) == true))
        }.let { if (newestFirst) it else it.reversed() }
    }

    val monthStats = remember(transactions) {
        var inc = 0L; var exp = 0L
        val mk = monthKey(System.currentTimeMillis())
        transactions.forEach { if (monthKey(it.date) == mk) { if (it.type == TxType.INCOME) inc += it.amount else if (it.type == TxType.EXPENSE) exp += it.amount } }
        inc to exp
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val inc = LocalIncomeColors.current
                StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.tx_month_income), Format.money(monthStats.first), inc.container, inc.onContainer, Modifier.weight(1f))
                StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.tx_month_expense), Format.money(monthStats.second), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    SegmentedButton(view == TxView.LIST, { view = TxView.LIST }, SegmentedButtonDefaults.itemShape(0, 2), icon = {}) { Icon(Icons.AutoMirrored.Rounded.ViewList, stringResource(R.string.tx_view_list)) }
                    SegmentedButton(view == TxView.CALENDAR, { view = TxView.CALENDAR }, SegmentedButtonDefaults.itemShape(1, 2), icon = {}) { Icon(Icons.Rounded.CalendarMonth, stringResource(R.string.tx_view_calendar)) }
                }
                BadgedBox(badge = { if (activeFilters > 0) Badge { Text("$activeFilters") } }) {
                    FilledTonalIconButton(onClick = { showFilters = true }) { Icon(Icons.Rounded.FilterList, stringResource(R.string.filters_title)) }
                }
            }
            SearchField(search, { search = it; limit = PAGE }, stringResource(R.string.filters_search))
        }

        if (view == TxView.CALENDAR) {
            CalendarView(transactions, catById, walletById, onEdit)
        } else if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(if (transactions.isEmpty()) R.string.dashboard_no_tx else R.string.tx_no_match))
        } else {
            val visible = filtered.take(limit)
            val groups = remember(visible) { visible.groupBy { dayKey(it.date) }.toList() }
            val listState = rememberLazyListState()
            // Infinite scroll: reveal the next page when the end is near.
            val nearEnd by remember { derivedStateOf { val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0; last >= listState.layoutInfo.totalItemsCount - 8 } }
            LaunchedEffect(nearEnd, filtered.size) { if (nearEnd && limit < filtered.size) limit += PAGE }

            Text(stringResource(R.string.tx_result_count, filtered.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                groups.forEach { (day, dayItems) ->
                    item(key = "h$day") {
                        val inc = dayItems.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                        val exp = dayItems.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Format.dayHeader(dayItems.first().date), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (inc > 0) Text("+" + Format.compactMoney(inc), style = MaterialTheme.typography.labelMedium, color = LocalIncomeColors.current.income, modifier = Modifier.padding(end = 8.dp))
                            if (exp > 0) Text("-" + Format.compactMoney(exp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items(dayItems, key = { it.id }) { tx ->
                        SwipeableTx(tx, catById[tx.categoryId], walletById, onEdit = { onEdit(tx) }, onDelete = { deleteTarget = tx })
                    }
                }
            }
        }
    }

    deleteTarget?.let { tx ->
        ConfirmDialog(stringResource(R.string.tx_confirm_delete), onConfirm = { vm.deleteTransaction(tx) }, onDismiss = { deleteTarget = null })
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            FiltersSheet(
                categories = categories, wallets = wallets,
                type = typeFilter, onType = { typeFilter = it; categoryFilter = null; limit = PAGE },
                categoryId = categoryFilter, onCategory = { categoryFilter = it; limit = PAGE },
                walletId = walletFilter, onWallet = { walletFilter = it; limit = PAGE },
                from = from, to = to, onRange = { f, t -> from = f; to = t; limit = PAGE },
                newestFirst = newestFirst, onSort = { newestFirst = it },
                onReset = {
                    typeFilter = "all"; categoryFilter = null; walletFilter = null; from = monthDefault.start; to = monthDefault.fullEnd; newestFirst = true; limit = PAGE
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTx(tx: Tx, category: Category?, wallets: Map<Long, Wallet>, onEdit: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.StartToEnd -> { if (tx.type != TxType.TRANSFER) onEdit(); false } // snap back
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = tx.type != TxType.TRANSFER,
        backgroundContent = {
            val dir = state.dismissDirection
            val (bg, icon, align) = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(MaterialTheme.colorScheme.primaryContainer, Icons.Rounded.Edit, Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart -> Triple(MaterialTheme.colorScheme.errorContainer, Icons.Rounded.Delete, Alignment.CenterEnd)
                else -> Triple(Color.Transparent, Icons.Rounded.Edit, Alignment.CenterStart)
            }
            Box(Modifier.fillMaxSize().background(bg, MaterialTheme.shapes.medium).padding(horizontal = 20.dp), contentAlignment = align) {
                if (dir != SwipeToDismissBoxValue.Settled) Icon(icon, null)
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            TxRow(tx, category, wallets, showDate = false, onClick = if (tx.type != TxType.TRANSFER) onEdit else null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    categories: List<Category>, wallets: List<Wallet>,
    type: String, onType: (String) -> Unit,
    categoryId: Long?, onCategory: (Long?) -> Unit,
    walletId: Long?, onWallet: (Long?) -> Unit,
    from: Long?, to: Long?, onRange: (Long?, Long?) -> Unit,
    newestFirst: Boolean, onSort: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.filters_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReset) { Text(stringResource(R.string.filters_reset)) }
        }
        Text(stringResource(R.string.filters_type), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to R.string.filters_all_types, TxType.INCOME to R.string.tx_income, TxType.EXPENSE to R.string.tx_expense, TxType.TRANSFER to R.string.tx_transfer).forEach { (id, label) ->
                FilterChip(type == id, { onType(id) }, { Text(stringResource(label)) })
            }
        }
        if (type == TxType.INCOME || type == TxType.EXPENSE) {
            Text(stringResource(R.string.tx_category), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(categoryId == null, { onCategory(null) }, { Text(stringResource(R.string.filters_all_categories)) })
                categories.filter { it.type == type }.forEach { c -> FilterChip(categoryId == c.id, { onCategory(c.id) }, { Text("${c.emoji} ${c.name}") }) }
            }
        }
        Text(stringResource(R.string.tx_wallet), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(walletId == null, { onWallet(null) }, { Text(stringResource(R.string.filters_all_wallets)) })
            wallets.forEach { w -> FilterChip(walletId == w.id, { onWallet(w.id) }, { Text(w.name) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ pickFrom = true }, Modifier.weight(1f)) { Text(from?.let { stringResource(R.string.filters_from) + ": " + Format.dateShort(it) } ?: stringResource(R.string.filters_from)) }
            OutlinedButton({ pickTo = true }, Modifier.weight(1f)) { Text(to?.let { stringResource(R.string.filters_to) + ": " + Format.dateShort(it) } ?: stringResource(R.string.filters_to)) }
        }
        TextButton(onClick = { onRange(null, null) }) { Text(stringResource(R.string.tx_show_all)) }
        Text(stringResource(R.string.tx_sort), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(newestFirst, { onSort(true) }, { Text(stringResource(R.string.tx_sort_newest)) })
            FilterChip(!newestFirst, { onSort(false) }, { Text(stringResource(R.string.tx_sort_oldest)) })
        }
    }
    if (pickFrom) SimpleDatePicker(from ?: System.currentTimeMillis(), { onRange(startOfDay(it), to) }) { pickFrom = false }
    if (pickTo) SimpleDatePicker(to ?: System.currentTimeMillis(), { onRange(from, endOfDay(it)) }) { pickTo = false }
}

/** Month grid with net income / expense per day; tapping a day lists its transactions below. */
@Composable
private fun CalendarView(transactions: List<Tx>, catById: Map<Long, Category>, walletById: Map<Long, Wallet>, onEdit: (Tx) -> Unit) {
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val period = remember(anchor) { periodFor(Range.MONTH, anchor) }
    val inMonth = remember(transactions, period) { transactions.filter { it.date in period.start..period.fullEnd && it.type != TxType.TRANSFER } }
    val byDay = remember(inMonth) { inMonth.groupBy { dayKey(it.date) } }
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val income = LocalIncomeColors.current.income

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            else Column { list.forEach { TxRow(it, catById[it.categoryId], walletById, showDate = false, onClick = { onEdit(it) }) } }
        }
    }
}
