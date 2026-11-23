package com.expenser.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import java.util.Calendar
import kotlin.math.roundToInt

private data class CatRow(val id: Long, val category: Category?, val value: Long, val count: Int, val pct: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(vm: MainViewModel, onEdit: (Tx) -> Unit) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val catById = remember(categories) { categories.associateBy { it.id } }
    val walletById = remember(wallets) { wallets.associateBy { it.id } }

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

    val categoryRows = remember(filtered) {
        val expenses = filtered.filter { it.type == TxType.EXPENSE }
        val total = expenses.sumOf { it.amount }.coerceAtLeast(1)
        expenses.groupBy { it.categoryId ?: -1L }.map { (id, list) ->
            val v = list.sumOf { it.amount }
            CatRow(id, catById[id], v, list.size, (v * 100.0 / total).roundToInt())
        }.sortedByDescending { it.value }
    }

    val buckets = remember(filtered, range, period, anchor) { buildBuckets(filtered, range, period) }
    val inc = LocalIncomeColors.current
    val errorColor = MaterialTheme.colorScheme.error

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Range.TODAY to R.string.stats_range_today, Range.WEEK to R.string.stats_range_week, Range.MONTH to R.string.stats_range_month, Range.YEAR to R.string.stats_range_year, Range.ALL to R.string.stats_range_all).forEach { (r, res) ->
                        FilterChip(range == r, { range = r; anchor = System.currentTimeMillis() }, { Text(stringResource(res)) })
                    }
                }
                PeriodNavigator(label, period.isCurrent || range == Range.ALL, { anchor = shiftAnchor(range, anchor, -1) }, { anchor = shiftAnchor(range, anchor, 1) }, { anchor = System.currentTimeMillis() }, enabled = range != Range.ALL)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.stats_total_income), Format.money(income), inc.container, inc.onContainer, Modifier.weight(1f))
                StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.stats_total_expense), Format.money(expense), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            StatTile(Icons.Rounded.Balance, stringResource(R.string.stats_difference), Format.money(income - expense), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.fillMaxWidth(),
                hint = stringResource(R.string.stats_transactions_count, filtered.size))
        }

        if (filtered.isEmpty()) {
            item { SectionCard(null) { EmptyState(Icons.Rounded.BarChart, stringResource(R.string.stats_no_data)) } }
        } else {
            item {
                SectionCard(stringResource(R.string.stats_trend)) {
                    TrendBars(buckets)
                    Spacer(Modifier.height(8.dp))
                    LegendRow(inc.income, stringResource(R.string.tx_income), Format.money(income))
                    LegendRow(errorColor, stringResource(R.string.tx_expense), Format.money(expense))
                }
            }
            item {
                SectionCard(stringResource(R.string.stats_pie), action = { Text(stringResource(R.string.stats_pie_subtitle, Format.compactMoney(expense), categoryRows.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }) {
                    if (categoryRows.isEmpty()) EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.stats_no_expense))
                    else {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DonutChart(categoryRows.map { Slice(it.category?.name ?: "", it.value, it.category?.color?.toColor() ?: Color(0xFF94A3B8)) }, size = 200.dp) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(Format.compactMoney(expense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.tx_expense), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.stats_highest, categoryRows.first().category?.name ?: stringResource(R.string.common_other), categoryRows.first().pct), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (categoryRows.isNotEmpty()) {
                item {
                    SectionCard(stringResource(R.string.stats_category_pct)) {
                        categoryRows.forEach { row ->
                            ProgressRow(
                                name = (row.category?.let { "${it.emoji} ${it.name}" } ?: stringResource(R.string.common_other)),
                                color = row.category?.color?.toColor() ?: Color(0xFF94A3B8), valueText = Format.money(row.value), pct = row.pct,
                                modifier = Modifier.clickable { sheetCategory = row },
                            )
                        }
                    }
                }
            }
        }
    }

    sheetCategory?.let { row ->
        ModalBottomSheet(onDismissRequest = { sheetCategory = null }) {
            val list = filtered.filter { it.type == TxType.EXPENSE && (it.categoryId ?: -1L) == row.id }
            Column(Modifier.padding(bottom = 24.dp)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(row.category?.emoji ?: "🏷️", row.category?.color?.toColor() ?: Color(0xFF94A3B8))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(row.category?.name ?: stringResource(R.string.common_other), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.stats_cat_summary, row.count, row.pct), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(Format.money(row.value), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = errorColor)
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

/** Buckets for the trend chart: hours (today), days (week / month), months (year / all). */
private fun buildBuckets(list: List<Tx>, range: Range, period: Period): List<Bucket> {
    fun sum(items: List<Tx>, type: String) = items.filter { it.type == type }.sumOf { it.amount }
    fun bucket(label: String, items: List<Tx>) = Bucket(label, sum(items, TxType.INCOME), sum(items, TxType.EXPENSE))
    return when (range) {
        Range.TODAY -> (0 until 24 step 3).map { h ->
            bucket("${h}h", list.filter { val x = it.date.field(Calendar.HOUR_OF_DAY); x >= h && x < h + 3 })
        }
        Range.WEEK, Range.MONTH -> {
            val byDay = list.groupBy { dayKey(it.date) }
            val days = if (range == Range.WEEK) 7 else Calendar.getInstance().apply { timeInMillis = period.start }.getActualMaximum(Calendar.DAY_OF_MONTH)
            (0 until days).map { i ->
                val d = Calendar.getInstance().apply { timeInMillis = period.start; add(Calendar.DAY_OF_MONTH, i) }.timeInMillis
                bucket("${d.field(Calendar.DAY_OF_MONTH)}", byDay[startOfDay(d)].orEmpty())
            }
        }
        Range.YEAR -> {
            val byMonth = list.groupBy { it.date.field(Calendar.MONTH) }
            (0 until 12).map { m -> bucket("${m + 1}", byMonth[m].orEmpty()) }
        }
        Range.ALL -> {
            val byMonth = list.groupBy { monthKey(it.date) }.toSortedMap()
            byMonth.entries.toList().takeLast(12).map { (k, items) -> bucket("${k % 12 + 1}/${(k / 12) % 100}", items) }
        }
    }
}
