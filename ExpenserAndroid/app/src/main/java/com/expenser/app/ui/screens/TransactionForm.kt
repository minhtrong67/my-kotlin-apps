package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expenser.app.R
import com.expenser.app.data.*
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.util.Format
import com.expenser.app.util.endOfDay
import com.expenser.app.util.isCafeCategory


data class TxFormRequest(val editing: Tx? = null, val initialType: String = TxType.EXPENSE, val initialCategoryId: Long? = null)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormDialog(request: TxFormRequest, vm: MainViewModel, onDismiss: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val shops by vm.shops.collectAsStateWithLifecycle()
    val drinks by vm.drinks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()

    val editing = request.editing
    val log = remember(editing, logs) { editing?.let { e -> logs.firstOrNull { it.txId == e.id } } }

    var type by rememberSaveable { mutableStateOf(editing?.type ?: request.initialType) }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toString() ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(editing?.categoryId ?: request.initialCategoryId) }
    var walletId by rememberSaveable { mutableStateOf(editing?.walletId) }
    var date by rememberSaveable { mutableLongStateOf(editing?.date ?: System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var shopId by rememberSaveable { mutableStateOf<Long?>(null) }
    var drinkId by rememberSaveable { mutableStateOf<Long?>(null) }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var cafeLoaded by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Cafe details of an edited transaction arrive with the logs flow.
    LaunchedEffect(log) {
        if (!cafeLoaded && log != null) {
            shopId = log.shopId; drinkId = log.drinkId; imageUrl = log.imageUrl; cafeLoaded = true
        }
    }
    // Default wallet = first one.
    LaunchedEffect(wallets) { if (walletId == null && wallets.isNotEmpty()) walletId = wallets.first().id }

    val typeCategories = categories.filter { it.type == type }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val showCafe = type == TxType.EXPENSE && isCafeCategory(selectedCategory)

    val amountValue = amount.toLongOrNull() ?: 0L
    val errAmount = when { amountValue <= 0 -> stringResource(R.string.validation_amount_positive); amountValue > 999_999_999_999L -> stringResource(R.string.validation_amount_large); else -> null }
    val errCategory = if (typeCategories.none { it.id == categoryId }) stringResource(R.string.validation_category_required) else null
    val errWallet = if (wallets.none { it.id == walletId }) stringResource(R.string.validation_wallet_required) else null
    val errDate = if (date > endOfDay(System.currentTimeMillis())) stringResource(R.string.validation_date_future) else null
    val errNote = if (note.length > 200) stringResource(R.string.validation_note_max) else null
    val errShop = if (showCafe && shopId == null) stringResource(R.string.validation_shop_required) else null
    val errDrink = if (showCafe && drinkId == null) stringResource(R.string.validation_drink_required) else null
    val valid = listOf(errAmount, errCategory, errWallet, errDate, errNote, errShop, errDrink).all { it == null }

    fun submit() {
        submitted = true
        if (!valid || saving) return
        saving = true
        vm.saveTransaction(
            editing,
            TxInput(type, amountValue, categoryId!!, walletId!!, date, note,
                if (showCafe) CafeInput(shopId, drinkId, imageUrl) else null),
        ) { onDismiss() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(if (editing == null) R.string.tx_add_title else R.string.tx_edit_title)) },
                        navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close)) } },
                        actions = {
                            Button(onClick = ::submit, enabled = !saving, modifier = Modifier.padding(end = 8.dp)) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.common_save))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(TxType.EXPENSE to R.string.tx_expense, TxType.INCOME to R.string.tx_income).forEachIndexed { i, (t, label) ->
                            SegmentedButton(
                                selected = type == t, onClick = { if (type != t) { type = t; categoryId = null } },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                            ) { Text(stringResource(label)) }
                        }
                    }

                    MoneyField(amount, { amount = it }, stringResource(R.string.tx_amount), error = if (submitted) errAmount else null)

                    // ----- category -----
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.tx_category), style = MaterialTheme.typography.titleSmall)
                        if (typeCategories.isEmpty()) {
                            Text(stringResource(R.string.tx_no_category), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                typeCategories.forEach { c ->
                                    FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id }, label = { Text("${c.emoji} ${c.name}") })
                                }
                            }
                        }
                        if (submitted && errCategory != null && typeCategories.isNotEmpty()) Text(errCategory, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // ----- wallet -----
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.tx_wallet), style = MaterialTheme.typography.titleSmall)
                        if (wallets.isEmpty()) Text(stringResource(R.string.tx_no_wallet), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            wallets.forEach { w ->
                                FilterChip(
                                    selected = walletId == w.id, onClick = { walletId = w.id }, label = { Text(w.name) },
                                    leadingIcon = { Icon(walletIcon(w.icon), null, Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }

                    DateTimeField(date, { date = it }, error = errDate)

                    OutlinedTextField(
                        value = note, onValueChange = { note = it.take(220) }, label = { Text(stringResource(R.string.tx_note)) },
                        supportingText = { Text(errNote ?: stringResource(R.string.tx_note_hint)) }, isError = errNote != null,
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                    )

                    if (showCafe) {
                        SectionCard(stringResource(R.string.cafe_form_section)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CafePicker(stringResource(R.string.cafe_shop), shops.map { it.id to it.name }, shopId, { shopId = it },
                                    stringResource(R.string.cafe_no_shops), if (submitted) errShop else null)
                                CafePicker(stringResource(R.string.cafe_drink), drinks.map { it.id to it.name }, drinkId, { drinkId = it },
                                    stringResource(R.string.cafe_no_drinks), if (submitted) errDrink else null)
                                OutlinedTextField(
                                    value = imageUrl, onValueChange = { imageUrl = it }, label = { Text(stringResource(R.string.cafe_photo)) },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafePicker(label: String, options: List<Pair<Long, String>>, selected: Long?, onSelect: (Long?) -> Unit, emptyText: String, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        if (options.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, name) -> FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(name) }) }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
