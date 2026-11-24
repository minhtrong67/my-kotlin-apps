package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.TxType
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
private fun ChangeBadge(current: Long, previous: Long) {
    if (previous == 0L) return
    val pct = (((current - previous).toDouble() / previous) * 100).roundToInt()
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (pct >= 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, null, Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text("${abs(pct)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel, onAdd: () -> Unit, onEdit: (com.expenser.app.data.Tx) -> Unit, onViewAll: () -> Unit) {
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    val catById = remember(categories) { categories.associateBy { it.id } }
    val walletById = remember(wallets) { wallets.associateBy { it.id } }
    val inc = LocalIncomeColors.current
    val errorColor = MaterialTheme.colorScheme.error

    val stats = remember(transactions) {
        val now = System.currentTimeMillis()
        val cur = monthKey(now)
        val last = cur - 1
        var mi = 0L; var me = 0L; var li = 0L; var le = 0L
        val byCat = mutableMapOf<String, Long>()
        transactions.forEach { tx ->
            if (tx.type == TxType.TRANSFER) return@forEach
            val mk = monthKey(tx.date)
            val isIncome = tx.type == TxType.INCOME
            if (mk == cur) {
                if (isIncome) mi += tx.amount else { me += tx.amount; tx.categoryId?.let { byCat[it] = (byCat[it] ?: 0) + tx.amount } }
            } else if (mk == last) { if (isIncome) li += tx.amount else le += tx.amount }
        }
        arrayOf(mi, me, li, le) to byCat
    }
    val (sums, byCat) = stats
    val monthIncome = sums[0]; val monthExpense = sums[1]
    val remaining = monthIncome - monthExpense
    val totalBalance = wallets.sumOf { it.balance }

    if (loading) { LoadingBox(); return }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.AccountBalanceWallet, stringResource(R.string.dashboard_total_balance), Format.money(totalBalance),
                        MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    StatTile(Icons.Rounded.Savings, stringResource(R.string.dashboard_remaining), Format.money(remaining),
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f),
                        valueColor = if (remaining < 0) errorColor else MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.dashboard_month_income), Format.money(monthIncome), inc.container, inc.onContainer, Modifier.weight(1f), badge = { ChangeBadge(monthIncome, sums[2]) })
                    StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.dashboard_month_expense), Format.money(monthExpense), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f), badge = { ChangeBadge(monthExpense, sums[3]) })
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dashboard_income_expense)) {
                if (monthIncome + monthExpense == 0L) {
                    EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.dashboard_no_month_tx))
                } else {
                    val total = (monthIncome + monthExpense).toDouble()
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(listOf(Slice(stringResource(R.string.tx_income), monthIncome, inc.income), Slice(stringResource(R.string.tx_expense), monthExpense, errorColor))) {
                            Column(Modifier.padding(horizontal = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.dashboard_remaining), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Text(Format.compactMoney(remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (remaining < 0) errorColor else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LegendRow(inc.income, stringResource(R.string.tx_income), Format.money(monthIncome), extra = "${(monthIncome / total * 100).roundToInt()}%")
                    LegendRow(errorColor, stringResource(R.string.tx_expense), Format.money(monthExpense), extra = "${(monthExpense / total * 100).roundToInt()}%")
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dashboard_by_category)) {
                val rows = byCat.entries.sortedByDescending { it.value }.take(6)
                if (rows.isEmpty()) EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.dashboard_no_expense))
                else {
                    val slices = rows.map { (id, v) -> Slice(catById[id]?.name ?: "", v, catById[id]?.color?.toColor() ?: Color(0xFF94A3B8)) }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DonutChart(slices, size = 170.dp) }
                    Spacer(Modifier.height(8.dp))
                    slices.forEach { LegendRow(it.color, it.label.ifBlank { stringResource(R.string.common_other) }, Format.money(it.value)) }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dashboard_recent), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onViewAll) { Text(stringResource(R.string.dashboard_view_all)) }
                    }
                    if (transactions.isEmpty()) {
                        EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.dashboard_no_tx), action = stringResource(R.string.tx_add_title) to onAdd)
                    } else {
                        transactions.take(6).forEach { tx -> TxRow(tx, catById[tx.categoryId ?: ""], walletById, onClick = if (tx.type != TxType.TRANSFER) ({ onEdit(tx) }) else null) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dashboard_wallet_balances)) {
                wallets.forEach { w ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconAvatar(walletIcon(w.icon), w.color.toColor(), 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(w.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Format.money(w.balance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
