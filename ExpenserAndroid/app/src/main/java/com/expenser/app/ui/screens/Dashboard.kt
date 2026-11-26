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
    val errorColor = com.expenser.app.ui.theme.LocalIncomeColors.current.expense

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
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.AccountBalanceWallet, stringResource(R.string.dashboard_total_balance), Format.money(totalBalance),
                        MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    StatTile(Icons.Rounded.Savings, stringResource(R.string.dashboard_remaining), Format.money(remaining),
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f),
                        valueColor = if (remaining < 0) errorColor else MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.dashboard_month_income), Format.money(monthIncome), inc.container, inc.onContainer, Modifier.weight(1f).fillMaxHeight(), badge = { ChangeBadge(monthIncome, sums[2]) })
                    StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.dashboard_month_expense), Format.money(monthExpense), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f).fillMaxHeight(), badge = { ChangeBadge(monthExpense, sums[3]) })
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dashboard_income_expense)) {
                if (monthIncome + monthExpense == 0L) {
                    EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.dashboard_no_month_tx))
                } else {
                    DonutWithLegend(
                        listOf(Slice(stringResource(R.string.tx_income), monthIncome, inc.income), Slice(stringResource(R.string.tx_expense), monthExpense, errorColor)),
                        centerTitle = stringResource(R.string.dashboard_remaining), centerValue = Format.compactMoney(remaining),
                    )
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dashboard_by_category)) {
                val rows = byCat.entries.sortedByDescending { it.value }
                if (rows.isEmpty()) EmptyState(Icons.Rounded.DonutLarge, stringResource(R.string.dashboard_no_expense))
                else {
                    val other = stringResource(R.string.common_other)
                    DonutWithLegend(
                        rows.mapIndexed { i, (id, v) -> Slice(catById[id]?.let { "${it.emoji} ${it.name}" } ?: other, v, chartColor(i, catById[id] != null)) },
                        centerTitle = stringResource(R.string.tx_expense), centerValue = Format.compactMoney(monthExpense),
                    )
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
        ScrollToTop(listState)
    }
}
