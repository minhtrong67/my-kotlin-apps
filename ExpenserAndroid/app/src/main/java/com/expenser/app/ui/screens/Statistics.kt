package com.expenser.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.Category
import com.expenser.app.data.Tx
import com.expenser.app.data.TxType
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.*
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class CatRow(val id: String, val category: Category?, val value: Long, val count: Int, val pct: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(vm: MainViewModel, onEdit: (Tx) -> Unit) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val catById = remember(categories) { categories.associateBy { it.id } }
    val walletById = remember(wallets) { wallets.associateBy { it.id } }

    if (loading) { StatsSkeleton(); return }
    var range by rememberSaveable { mutableStateOf(Range.MONTH) }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var sheetCategory by remember { mutableStateOf<CatRow?>(null) }

    val period = remember(range, anchor) { periodFor(range, anchor) }
    val label = when (range) {
        Range.TODAY -> if (period.isCurrent) stringResource(R.string.stats_today_label, Format.dateShort(anchor)) else Format.dayHeader(anchor)
        Range.WEEK -> "${Format.dateShort(period.start)} – ${Format.dateShort(period.fullEnd)}/${period.fullEnd.field(Calendar.YEAR)}"
        Range.MONTH -> Format.monthYear(anchor)
        Range.YEAR -> stringResource(R.string.stats_year_label, anchor.field(Calendar.YEAR))
        Range.ALL -> stringResource(R.string.stats_range_all)
    }

    val filtered = remember(transactions, period) { transactions.filter { it.type != TxType.TRANSFER && it.date in period.start..period.end } }
    val income = filtered.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

    fun rowsFor(type: String): List<CatRow> {
        val list0 = filtered.filter { it.type == type }
        val total = list0.sumOf { it.amount }.coerceAtLeast(1)
        return list0.groupBy { it.categoryId ?: "" }.map { (id, list) ->
            val v = list.sumOf { it.amount }
            CatRow(id, catById[id], v, list.size, (v * 100.0 / total).roundToInt())
        }.sortedByDescending { it.value }
    }
    val categoryRows = remember(filtered, catById) { rowsFor(TxType.EXPENSE) }
    val incomeRows = remember(filtered, catById) { rowsFor(TxType.INCOME) }
    var sheetType by remember { mutableStateOf(TxType.EXPENSE) }
    val inc = LocalIncomeColors.current
    val errorColor = com.expenser.app.ui.theme.LocalIncomeColors.current.expense

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val appName = stringResource(R.string.app_name)
    val colors = MaterialTheme.colorScheme
    val other = stringResource(R.string.common_other)
    val incLabel = stringResource(R.string.tx_income); val expLabel = stringResource(R.string.tx_expense)
    val totalLabels = listOf(stringResource(R.string.stats_total_income), stringResource(R.string.stats_total_expense), stringResource(R.string.stats_difference))
    val catSection = stringResource(R.string.stats_pie)
    val incSection = stringResource(R.string.stats_pie_income)
    val ieSection = stringResource(R.string.dashboard_income_expense)
    val exporter = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                fun slices(rows: List<CatRow>): List<RSlice> {
                    val mk = { i: Int, r: CatRow -> RSlice(r.category?.let { c -> "${c.emoji} ${c.name}" } ?: other, r.value, chartColor(i, r.category != null).toArgb(), Format.money(r.value), r.pct) }
                    val head = rows.take(5).mapIndexed(mk)
                    val rest = rows.drop(5)
                    if (rest.isEmpty()) return head
                    val v = rest.sumOf { it.value }
                    return head + RSlice(other, v, chartColor(5, false).toArgb(), Format.money(v), rest.sumOf { it.pct })
                }
                val data = ReportData(
                    appName = appName, title = ctx.getString(R.string.report_title), period = label,
                    generated = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    incomeLabel = totalLabels[0], incomeText = Format.money(income), incomeColor = inc.income.toArgb(),
                    expenseLabel = totalLabels[1], expenseText = Format.money(expense), expenseColor = errorColor.toArgb(),
                    balanceLabel = totalLabels[2], balanceText = Format.money(income - expense), balanceColor = (if (income - expense >= 0) inc.income else errorColor).toArgb(),
                    topExpenseTitle = ctx.getString(R.string.report_top_expense),
                    topExpense = categoryRows.firstOrNull()?.let { r -> RSlice(r.category?.let { c -> "${c.emoji} ${c.name}" } ?: other, r.value, errorColor.toArgb(), Format.money(r.value), r.pct) },
                    topIncomeTitle = ctx.getString(R.string.report_top_income),
                    topIncome = incomeRows.firstOrNull()?.let { r -> RSlice(r.category?.let { c -> "${c.emoji} ${c.name}" } ?: other, r.value, inc.income.toArgb(), Format.money(r.value), r.pct) },
                    expense = RSection(catSection, expLabel, Format.compactMoney(expense), slices(categoryRows)),
                    income = RSection(incSection, incLabel, Format.compactMoney(income), slices(incomeRows)),
                    emptyText = ctx.getString(R.string.report_no_data),
                )
                val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { ReportImage.render(data) }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
                vm.toast(R.string.settings_exported)
            } catch (e: Exception) { vm.toast(R.string.settings_export_error) }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Range.TODAY to R.string.stats_range_today, Range.WEEK to R.string.stats_range_week, Range.MONTH to R.string.stats_range_month, Range.YEAR to R.string.stats_range_year, Range.ALL to R.string.stats_range_all).forEach { (r, res) ->
                        FilterChip(range == r, { range = r; anchor = System.currentTimeMillis() }, { Text(stringResource(res)) })
                    }
                }
                PeriodNavigator(label, period.isCurrent || range == Range.ALL, { anchor = shiftAnchor(range, anchor, -1) }, { anchor = shiftAnchor(range, anchor, 1) }, { anchor = System.currentTimeMillis() }, enabled = range != Range.ALL)
            }
        }

        item {
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.stats_total_income), Format.money(income), inc.container, inc.onContainer, Modifier.weight(1f).fillMaxHeight())
                StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.stats_total_expense), Format.money(expense), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(10.dp))
            val topE = categoryRows.firstOrNull(); val topI = incomeRows.firstOrNull()
            fun topName(r: CatRow?) = r?.let { x -> (x.category?.let { c -> "${c.emoji} ${c.name}" } ?: other) + " · ${x.pct}%" } ?: "—"
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.AutoMirrored.Rounded.TrendingDown, stringResource(R.string.report_top_expense), Format.money(topE?.value ?: 0), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f).fillMaxHeight(), hint = topName(topE))
                StatTile(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.report_top_income), Format.money(topI?.value ?: 0), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f).fillMaxHeight(), hint = topName(topI))
            }
        }

        item {
            OutlinedButton(onClick = { if (filtered.isEmpty()) vm.toast(R.string.settings_export_empty) else exporter.launch("expenser-report-${Format.date(period.start).replace('/', '-')}.png") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.stats_export))
            }
        }

        if (filtered.isEmpty()) {
            item { SectionCard(null) { EmptyState(Icons.Rounded.BarChart, stringResource(R.string.stats_no_data)) } }
        } else {
            item {
                SectionCard(stringResource(R.string.dashboard_income_expense)) {
                    DonutWithLegend(
                        listOf(Slice(incLabel, income, inc.income), Slice(expLabel, expense, errorColor)),
                        centerTitle = stringResource(R.string.dashboard_remaining), centerValue = Format.compactMoney(income - expense),
                    )
                }
            }
            item {
                SectionCard(stringResource(R.string.stats_pie), action = { Text(stringResource(R.string.stats_pie_subtitle, Format.compactMoney(expense), categoryRows.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }) {
                    if (categoryRows.isEmpty()) EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.stats_no_expense))
                    else {
                        DonutWithLegend(
                            categoryRows.mapIndexed { i, it -> Slice(it.category?.let { c -> "${c.emoji} ${c.name}" } ?: other, it.value, chartColor(i, it.category != null)) },
                            centerTitle = expLabel, centerValue = Format.compactMoney(expense),
                            onSliceClick = { sheetType = TxType.EXPENSE; sheetCategory = categoryRows[it] },
                        )
                        Text(stringResource(R.string.stats_highest, categoryRows.first().category?.name ?: other, categoryRows.first().pct), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            if (incomeRows.isNotEmpty()) item {
                SectionCard(stringResource(R.string.stats_pie_income)) {
                    DonutWithLegend(
                        incomeRows.mapIndexed { i, it -> Slice(it.category?.let { c -> "${c.emoji} ${c.name}" } ?: other, it.value, chartColor(i, it.category != null)) },
                        centerTitle = incLabel, centerValue = Format.compactMoney(income),
                        onSliceClick = { sheetType = TxType.INCOME; sheetCategory = incomeRows[it] },
                    )
                }
            }
        }
    }
        ScrollToTop(listState, aboveFab = false)
    }

    sheetCategory?.let { row ->
        ModalBottomSheet(onDismissRequest = { sheetCategory = null }) {
            val list = filtered.filter { it.type == sheetType && (it.categoryId ?: "") == row.id }
            Column(Modifier.padding(bottom = 24.dp)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(row.category?.emoji ?: "🏷️", row.category?.color?.toColor() ?: Color(0xFF94A3B8))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(row.category?.name ?: stringResource(R.string.common_other), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.stats_cat_summary, row.count, row.pct), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(Format.money(row.value), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (sheetType == TxType.INCOME) inc.income else errorColor)
                }
                HorizontalDivider()
                if (list.isEmpty()) EmptyState(Icons.Rounded.Inbox, stringResource(R.string.stats_cat_empty))
                else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(list, key = { it.id }) { tx -> TxRow(tx, row.category, walletById, onClick = { sheetCategory = null; onEdit(tx) }) }
                }
            }
        }
    }
}

