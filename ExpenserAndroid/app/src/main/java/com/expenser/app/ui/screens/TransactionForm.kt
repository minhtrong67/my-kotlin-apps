package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.expenser.app.util.toColor
import com.expenser.app.ui.theme.LocalIncomeColors
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


data class TxFormRequest(val editing: Tx? = null, val initialType: String = TxType.EXPENSE, val initialCategoryId: String? = null)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormDialog(request: TxFormRequest, vm: MainViewModel, onDismiss: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val shops by vm.shops.collectAsStateWithLifecycle()
    val drinks by vm.drinks.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val history by vm.transactions.collectAsStateWithLifecycle()

    val editing = request.editing
    val log = remember(editing, logs) { editing?.let { e -> logs.firstOrNull { it.txId == e.id } } }

    var type by rememberSaveable { mutableStateOf(editing?.type ?: request.initialType) }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toString() ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(editing?.categoryId ?: request.initialCategoryId) }
    var walletId by rememberSaveable { mutableStateOf(editing?.walletId) }
    var date by rememberSaveable { mutableLongStateOf(editing?.date ?: System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var shopId by rememberSaveable { mutableStateOf<String?>(null) }
    var drinkId by rememberSaveable { mutableStateOf<String?>(null) }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var cafeLoaded by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

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

    val context = androidx.compose.ui.platform.LocalContext.current
    // Strings are constant per locale, so they are safe to capture in the click handler.
    val msgAmount = stringResource(R.string.validation_amount_positive); val msgAmountBig = stringResource(R.string.validation_amount_large)
    val msgCategory = stringResource(R.string.validation_category_required); val msgWallet = stringResource(R.string.validation_wallet_required)
    val msgDate = stringResource(R.string.validation_date_future); val msgNote = stringResource(R.string.validation_note_max)
    val msgShop = stringResource(R.string.validation_shop_required); val msgDrink = stringResource(R.string.validation_drink_required)

    // The save handler re-reads every piece of state at the moment of the tap, so it can never act on an out-of-date composition.
    fun submit() {
        submitted = true
        if (saving) return
        val cat = categories.firstOrNull { it.id == categoryId && it.type == type }
        val cafe = type == TxType.EXPENSE && isCafeCategory(cat)
        val amt = amount.toLongOrNull() ?: 0L
        val problem = when {
            amt <= 0 -> msgAmount
            amt > 999_999_999_999L -> msgAmountBig
            cat == null -> msgCategory
            wallets.none { it.id == walletId } -> msgWallet
            date > endOfDay(System.currentTimeMillis()) -> msgDate
            note.length > 200 -> msgNote
            cafe && shopId == null -> msgShop
            cafe && drinkId == null -> msgDrink
            else -> null
        }
        if (problem != null) { android.widget.Toast.makeText(context, problem, android.widget.Toast.LENGTH_SHORT).show(); return }
        saving = true
        vm.saveTransaction(
            editing,
            TxInput(type, amt, cat!!.id, walletId!!, date, note, if (cafe) CafeInput(shopId, drinkId, imageUrl) else null),
        ) { onDismiss() }
    }

    var sheet by remember { mutableStateOf("") } // "", "category", "wallet", "shop", "drink"
    val income = LocalIncomeColors.current.income
    val expenseColor = com.expenser.app.ui.theme.LocalIncomeColors.current.expense

    FormScreen(
        title = stringResource(if (editing == null) R.string.tx_add_title else R.string.tx_edit_title),
        confirmLabel = stringResource(R.string.common_save),
        onClose = onDismiss, onConfirm = { submit() }, confirmEnabled = !saving && !uploading,
    ) {
        PillSegmented(
            listOf(
                Seg(TxType.EXPENSE, stringResource(R.string.tx_expense), container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer),
                Seg(TxType.INCOME, stringResource(R.string.tx_income), container = LocalIncomeColors.current.container, content = LocalIncomeColors.current.onContainer),
            ),
            type, { if (type != it) { type = it; categoryId = null } },
        )

        MoneyField(amount, { amount = it }, stringResource(R.string.tx_amount), error = if (submitted) errAmount else null)

        // Quick pick: past (category, amount) pairs of this type, most used first. Typing an amount narrows them to matching amounts.
        val pairs = remember(history, categories, type) {
            val catIds = categories.filter { it.type == type }.map { it.id }.toSet()
            history.filter { it.type == type && it.categoryId != null && catIds.contains(it.categoryId!!) && it.amount > 0 }
                .groupBy { it.categoryId!! to it.amount }
                .entries.sortedWith(compareByDescending<Map.Entry<Pair<String, Long>, List<Tx>>> { it.value.size }.thenByDescending { e -> e.value.maxOf { it.date } })
                .map { it.key }
        }
        val quick = remember(pairs, amount) { (if (amount.isEmpty()) pairs else pairs.filter { it.second.toString().startsWith(amount) }).take(if (amount.isEmpty()) 15 else 20) }
        if (editing == null && quick.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tx_quick_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quick.forEach { (catId, amt) ->
                        val cat = categories.firstOrNull { it.id == catId } ?: return@forEach
                        Surface(
                            onClick = { categoryId = catId; amount = amt.toString(); date = System.currentTimeMillis() },
                            shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text("${cat.emoji} ${cat.name} · ${Format.money(amt)}", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }

        SelectField(
            stringResource(R.string.tx_category), selectedCategory?.takeIf { it.type == type }?.name ?: "", { sheet = "category" },
            leading = { Text(selectedCategory?.takeIf { it.type == type }?.emoji ?: "🏷️", style = MaterialTheme.typography.titleMedium) },
            error = if (submitted) (errCategory ?: null) else null,
        )
        SelectField(
            stringResource(R.string.tx_wallet), wallets.firstOrNull { it.id == walletId }?.name ?: "", { sheet = "wallet" },
            leading = { Icon(walletIcon(wallets.firstOrNull { it.id == walletId }?.icon ?: ""), null) },
            error = if (submitted) errWallet else null,
        )
        DateTimeField(date, { date = it }, stringResource(R.string.tx_date), error = errDate)

        OutlinedTextField(
            value = note, onValueChange = { note = it.take(220) }, label = { Text(stringResource(R.string.tx_note)) },
            supportingText = { Text(errNote ?: stringResource(R.string.tx_note_hint)) }, isError = errNote != null,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
        )

        if (showCafe) {
            SectionCard(stringResource(R.string.cafe_form_section)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectField(stringResource(R.string.cafe_shop), shops.firstOrNull { it.id == shopId }?.name ?: "", { sheet = "shop" }, error = if (submitted) errShop else null,
                        leading = { RemoteImage(shops.firstOrNull { it.id == shopId }?.imageUrl, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), Icons.Rounded.Storefront) })
                    SelectField(stringResource(R.string.cafe_drink), drinks.firstOrNull { it.id == drinkId }?.name ?: "", { sheet = "drink" }, error = if (submitted) errDrink else null,
                        leading = { RemoteImage(drinks.firstOrNull { it.id == drinkId }?.imageUrl, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), Icons.Rounded.Coffee) })
                    ImageUploadField(imageUrl, { imageUrl = it }, Modifier.fillMaxWidth(), onBusyChange = { uploading = it })
                }
            }
        }
    }

    when (sheet) {
        "category" -> ChoiceSheet(stringResource(R.string.tx_category), typeCategories, categoryId, { it.id }, { categoryId = it.id }, { sheet = "" }) { c ->
            Row(verticalAlignment = Alignment.CenterVertically) { CategoryAvatar(c.emoji, c.color.toColor(), 36.dp); Spacer(Modifier.width(12.dp)); Text(c.name, style = MaterialTheme.typography.bodyLarge) }
        }
        "wallet" -> ChoiceSheet(stringResource(R.string.tx_wallet), wallets, walletId, { it.id }, { walletId = it.id }, { sheet = "" }) { w ->
            Row(verticalAlignment = Alignment.CenterVertically) { IconAvatar(walletIcon(w.icon), w.color.toColor(), 36.dp); Spacer(Modifier.width(12.dp)); Column { Text(w.name, style = MaterialTheme.typography.bodyLarge); Text(Format.money(w.balance), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        "shop" -> MediaChoiceSheet(stringResource(R.string.cafe_shop), shops.map { PickItem(it.id, it.name, it.address.ifBlank { null }, it.imageUrl) }, shopId, Icons.Rounded.Storefront, { shopId = it }, { sheet = "" })
        "drink" -> MediaChoiceSheet(stringResource(R.string.cafe_drink), drinks.map { PickItem(it.id, it.name, null, it.imageUrl) }, drinkId, Icons.Rounded.Coffee, { drinkId = it }, { sheet = "" })
    }
}

