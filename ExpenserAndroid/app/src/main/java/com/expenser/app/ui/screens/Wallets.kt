package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.Wallet
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(vm: MainViewModel) {
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<WalletEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<Wallet?>(null) }
    var showTransfer by remember { mutableStateOf(false) }

    val usage = remember(transactions) {
        val m = mutableMapOf<String, Int>()
        transactions.forEach { tx -> listOfNotNull(tx.walletId, tx.fromWalletId, tx.toWalletId).forEach { m[it] = (m[it] ?: 0) + 1 } }
        m
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                StatTile(Icons.Rounded.AccountBalanceWallet, stringResource(R.string.wallet_total_all), Format.money(wallets.sumOf { it.balance }),
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.fillMaxWidth(),
                    badge = {
                        FilledTonalButton(onClick = { showTransfer = true }, enabled = wallets.size >= 2, contentPadding = PaddingValues(horizontal = 14.dp)) {
                            Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.wallet_transfer))
                        }
                    })
            }
            if (wallets.isEmpty()) item { EmptyState(Icons.Rounded.AccountBalanceWallet, stringResource(R.string.wallet_empty), action = stringResource(R.string.wallet_add_first) to { editor = WalletEditorState(null) }) }
            items(wallets, key = { it.id }) { w ->
                SwipeRow(onEdit = { editor = WalletEditorState(w) }, onDelete = { deleteTarget = w }, shape = RoundedCornerShape(24.dp)) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconAvatar(walletIcon(w.icon), w.color.toColor(), 48.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(w.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.wallet_usage, usage[w.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(Format.money(w.balance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (w.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        ScreenFab({ editor = WalletEditorState(null) }, Modifier.align(Alignment.BottomEnd))
    }

    editor?.let { st -> WalletEditor(st, wallets, onDismiss = { editor = null }, vm = vm) }
    deleteTarget?.let { w -> ConfirmDialog(stringResource(R.string.wallet_confirm_delete, w.name), onConfirm = { vm.deleteWallet(w) }, onDismiss = { deleteTarget = null }) }
    if (showTransfer) TransferDialog(wallets, vm) { showTransfer = false }
}

data class WalletEditorState(val editing: Wallet?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletEditor(state: WalletEditorState, all: List<Wallet>, onDismiss: () -> Unit, vm: MainViewModel) {
    val editing = state.editing
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var balance by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(editing?.icon ?: WALLET_ICON_KEYS.first()) }
    var color by remember { mutableStateOf(editing?.color ?: CATEGORY_COLORS[5]) }
    var submitted by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    val err = when {
        trimmed.isEmpty() -> stringResource(R.string.validation_wallet_name_required)
        trimmed.length > 40 -> stringResource(R.string.validation_wallet_name_max)
        all.any { it.id != editing?.id && it.name.equals(trimmed, ignoreCase = true) } -> stringResource(R.string.validation_wallet_exists)
        else -> null
    }
    FormScreen(
        stringResource(if (editing == null) R.string.wallet_add_title else R.string.wallet_edit_title), stringResource(R.string.common_save), onClose = onDismiss,
        onConfirm = {
            submitted = true
            if (err == null) {
                if (editing == null) vm.addWallet(trimmed, icon, color, balance.toLongOrNull() ?: 0L) { onDismiss() }
                else vm.updateWallet(editing, trimmed, icon, color) { onDismiss() }
            }
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.wallet_name)) }, singleLine = true, isError = submitted && err != null,
            supportingText = { if (submitted && err != null) Text(err) }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        if (editing == null) MoneyField(balance, { balance = it }, stringResource(R.string.wallet_initial_balance), suggestions = false)
        Text(stringResource(R.string.wallet_icon), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WALLET_ICON_KEYS.forEach { k -> FilterChip(selected = icon == k, onClick = { icon = k }, label = { Icon(walletIcon(k), null, Modifier.size(20.dp)) }) }
        }
        Text(stringResource(R.string.common_color), style = MaterialTheme.typography.titleSmall)
        ColorPicker(color, { color = it }, CATEGORY_COLORS)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferDialog(wallets: List<Wallet>, vm: MainViewModel, onDismiss: () -> Unit) {
    var from by remember { mutableStateOf<String?>(wallets.firstOrNull()?.id) }
    var to by remember { mutableStateOf<String?>(wallets.getOrNull(1)?.id) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var submitted by remember { mutableStateOf(false) }

    val value = amount.toLongOrNull() ?: 0L
    val fromWallet = wallets.firstOrNull { it.id == from }
    val err = when {
        from == null -> stringResource(R.string.transfer_pick_from)
        to == null -> stringResource(R.string.transfer_pick_to)
        from == to -> stringResource(R.string.transfer_same_wallet)
        value <= 0 -> stringResource(R.string.validation_amount_positive)
        fromWallet != null && value > fromWallet.balance -> stringResource(R.string.transfer_insufficient)
        else -> null
    }
    var sheet by remember { mutableStateOf("") }
    FormScreen(
        stringResource(R.string.transfer_title), stringResource(R.string.transfer_submit), onClose = onDismiss,
        onConfirm = { submitted = true; if (err == null) vm.transfer(from!!, to!!, value, note, date) { onDismiss() } },
    ) {
        SelectField(stringResource(R.string.transfer_from), fromWallet?.name ?: "", { sheet = "from" }, leading = { Icon(walletIcon(fromWallet?.icon ?: ""), null) })
        SelectField(stringResource(R.string.transfer_to), wallets.firstOrNull { it.id == to }?.name ?: "", { sheet = "to" }, leading = { Icon(walletIcon(wallets.firstOrNull { it.id == to }?.icon ?: ""), null) })
        MoneyField(amount, { amount = it }, stringResource(R.string.tx_amount), max = fromWallet?.balance ?: Long.MAX_VALUE)
        DateTimeField(date, { date = it }, stringResource(R.string.tx_date))
        OutlinedTextField(note, { note = it.take(200) }, label = { Text(stringResource(R.string.tx_note)) }, supportingText = { Text(stringResource(R.string.transfer_note_hint)) }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        if (submitted && err != null) Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (sheet.isNotEmpty()) ChoiceSheet(stringResource(if (sheet == "from") R.string.transfer_from else R.string.transfer_to), wallets, if (sheet == "from") from else to, { it.id }, { if (sheet == "from") from = it.id else to = it.id }, { sheet = "" }) { w ->
        Row(verticalAlignment = Alignment.CenterVertically) { IconAvatar(walletIcon(w.icon), w.color.toColor(), 36.dp); Spacer(Modifier.width(12.dp)); Text(w.name) }
    }
}
