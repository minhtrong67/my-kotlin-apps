package com.expenser.app.ui

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenser.app.ExpenserApp
import com.expenser.app.R
import com.expenser.app.data.*
import com.expenser.app.reminder.ReminderScheduler
import com.expenser.app.util.sortCategories
import com.expenser.app.util.sortDrinks
import com.expenser.app.util.sortShops
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val container = app as ExpenserApp
    private val auth = container.auth
    private val repo = container.repo
    private val store = container.settingsStore

    val configured = auth.configured
    val user: StateFlow<UserProfile?> = auth.userFlow.stateIn(viewModelScope, SharingStarted.Eagerly, auth.profile(auth.currentUser))
    private val uidFlow = user.map { it?.uid }.distinctUntilChanged()
    private fun uid(): String? = user.value?.uid

    val settings = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private fun <T> source(f: (Repository, String) -> Flow<List<T>>): StateFlow<List<T>?> =
        uidFlow.flatMapLatest { id -> if (id == null || repo == null) flowOf(null) else f(repo, id) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rawTx = source { r, id -> r.transactions(id) }
    private val rawCat = source { r, id -> r.categories(id) }
    private val rawWal = source { r, id -> r.wallets(id) }
    private val rawShops = source { r, id -> r.shops(id) }
    private val rawDrinks = source { r, id -> r.drinks(id) }
    private val rawLogs = source { r, id -> r.logs(id) }

    val transactions = rawTx.map { it ?: emptyList() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = rawCat.map { sortCategories(it ?: emptyList()) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val wallets = rawWal.map { (it ?: emptyList()).sortedBy { w -> w.createdAt } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val shops = rawShops.map { sortShops(it ?: emptyList()) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val drinks = rawDrinks.map { sortDrinks(it ?: emptyList()) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs = rawLogs.map { it ?: emptyList() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** True until the first snapshot of transactions, categories and wallets has arrived. */
    val loading = combine(rawTx, rawCat, rawWal) { a, b, c -> a == null || b == null || c == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    fun toast(@StringRes res: Int, vararg args: Any) { _messages.tryEmit(UiMessage(res, args.toList())) }

    val signingIn = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val english = app.resources.configuration.locales[0].language == "en"
            uidFlow.filterNotNull().collectLatest { id ->
                repo?.seedIfNeeded(id, english)
                val s = store.flow.first()
                if (s.reminderEnabled) ReminderScheduler.schedule(app, s.reminderTime)
            }
        }
        viewModelScope.launch { container.writeErrors.collect { toast(R.string.write_error) } }
    }

    // ---------- Auth ----------
    fun signIn(activityContext: Context) {
        if (signingIn.value) return
        viewModelScope.launch {
            signingIn.value = true; loginError.value = null
            when (val r = auth.signInWithGoogle(activityContext)) {
                is SignInResult.Failure -> loginError.value = r.message
                else -> {}
            }
            signingIn.value = false
        }
    }

    fun signOut(activityContext: Context) { viewModelScope.launch { auth.signOut(activityContext) } }

    private inline fun withRepo(block: (Repository, String) -> Unit) {
        val r = repo; val id = uid()
        if (r != null && id != null) block(r, id)
    }

    // ---------- Transactions ----------
    fun saveTransaction(editing: Tx?, input: TxInput, onDone: () -> Unit) = withRepo { r, id ->
        r.saveTransaction(id, editing, input)
        toast(if (editing == null) R.string.tx_added else R.string.tx_updated)
        onDone()
    }

    fun deleteTransaction(tx: Tx) = withRepo { r, id -> r.deleteTransaction(id, tx); toast(R.string.tx_deleted) }

    fun transfer(from: String, to: String, amount: Long, note: String, date: Long, onDone: () -> Unit) = withRepo { r, id ->
        r.transfer(id, from, to, amount, note, date); toast(R.string.transfer_success); onDone()
    }

    // ---------- Categories ----------
    fun addCategory(name: String, type: String, emoji: String, color: String, onDone: () -> Unit) = withRepo { r, id ->
        r.addCategory(id, categories.value.filter { it.type == type }, name, type, emoji, color); toast(R.string.cat_added); onDone()
    }

    fun updateCategory(c: Category, name: String, emoji: String, color: String, onDone: () -> Unit) = withRepo { r, id ->
        r.updateCategory(id, c, name, emoji, color); toast(R.string.cat_updated); onDone()
    }

    fun deleteCategory(c: Category) = withRepo { r, id ->
        val n = transactions.value.count { it.categoryId == c.id }
        if (n > 0) toast(R.string.cat_in_use, n) else { r.deleteCategory(id, c.id); toast(R.string.cat_deleted) }
    }

    fun reorderCategories(ids: List<String>) = withRepo { r, id -> r.reorder(id, "categories", ids) }

    // ---------- Wallets ----------
    fun addWallet(name: String, icon: String, color: String, balance: Long, onDone: () -> Unit) = withRepo { r, id ->
        r.addWallet(id, name, icon, color, balance); toast(R.string.wallet_added); onDone()
    }

    fun updateWallet(w: Wallet, name: String, icon: String, color: String, onDone: () -> Unit) = withRepo { r, id ->
        r.updateWallet(id, w.id, name, icon, color); toast(R.string.wallet_updated); onDone()
    }

    fun deleteWallet(w: Wallet) = withRepo { r, id ->
        val n = transactions.value.count { it.walletId == w.id || it.fromWalletId == w.id || it.toWalletId == w.id }
        when {
            wallets.value.size <= 1 -> toast(R.string.wallet_keep_one)
            n > 0 -> toast(R.string.wallet_in_use, n)
            else -> { r.deleteWallet(id, w.id); toast(R.string.wallet_deleted) }
        }
    }

    // ---------- Cafe ----------
    private fun nextOrder(list: List<Int?>) = (list.filterNotNull().maxOrNull() ?: -1) + 1

    fun saveShop(editing: CafeShop?, name: String, address: String, mapUrl: String, imageUrl: String, description: String, rating: Int, onDone: () -> Unit) = withRepo { r, id ->
        r.saveShop(id, editing, nextOrder(shops.value.map { it.order }), name, address, mapUrl, imageUrl, description, rating); onDone()
    }

    fun deleteShop(s: CafeShop) = withRepo { r, id ->
        val n = logs.value.count { it.shopId == s.id }
        if (n > 0) toast(R.string.cafe_shop_in_use, n) else r.deleteShop(id, s.id)
    }

    fun saveDrink(editing: CafeDrink?, name: String, imageUrl: String, description: String, onDone: () -> Unit) = withRepo { r, id ->
        r.saveDrink(id, editing, nextOrder(drinks.value.map { it.order }), name, imageUrl, description); onDone()
    }

    fun deleteDrink(d: CafeDrink) = withRepo { r, id ->
        val n = logs.value.count { it.drinkId == d.id }
        if (n > 0) toast(R.string.cafe_drink_in_use, n) else r.deleteDrink(id, d.id)
    }

    fun reorderShops(ids: List<String>) = withRepo { r, id -> r.reorder(id, "cafeShops", ids) }
    fun reorderDrinks(ids: List<String>) = withRepo { r, id -> r.reorder(id, "cafeDrinks", ids) }

    // ---------- Settings ----------
    fun setThemeMode(v: String) = viewModelScope.launch { store.setThemeMode(v) }
    fun setAccent(v: String) = viewModelScope.launch { store.setAccent(v) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { store.setDynamicColor(v) }
    fun setFontSize(v: String) = viewModelScope.launch { store.setFontSize(v) }
    fun resetAppearance() = viewModelScope.launch { store.resetAppearance(); toast(R.string.settings_reset_done) }

    fun setReminder(enabled: Boolean, time: String) = viewModelScope.launch {
        store.setReminder(enabled, time)
        if (enabled) ReminderScheduler.schedule(app, time) else ReminderScheduler.cancel(app)
    }

    fun sendTestReminder() { if (!ReminderScheduler.show(app)) toast(R.string.reminder_denied) }

    // ---------- Import / export ----------
    fun snapshot() = DataSnapshot(transactions.value, categories.value, wallets.value, shops.value, drinks.value)

    fun importEntity(entity: Entity, text: String) = withRepo { r, id ->
        try {
            when (entity) {
                Entity.TRANSACTIONS -> {
                    val result = DataIo.parseTransactions(text)
                    if (result.rows.isEmpty()) { toast(R.string.import_empty); return@withRepo }
                    val n = r.importTransactions(id, result.rows, categories.value, wallets.value)
                    toast(R.string.imported, n)
                    if (result.skipped > 0) toast(R.string.import_skipped, result.skipped)
                }
                else -> {
                    val rows = DataIo.parseRows(entity, text)
                    var added = 0
                    when (entity) {
                        Entity.CATEGORIES -> {
                            val known = categories.value.map { "${it.type}:${it.name.lowercase()}" }.toMutableSet()
                            val counts = categories.value.groupingBy { it.type }.eachCount().toMutableMap()
                            rows.forEach { row ->
                                val name = row["name"].orEmpty().trim().take(40)
                                val type = if (row["type"]?.lowercase()?.startsWith("inc") == true || row["type"]?.lowercase()?.startsWith("thu") == true) TxType.INCOME else TxType.EXPENSE
                                if (name.isEmpty() || !known.add("$type:${name.lowercase()}")) return@forEach
                                val n = counts[type] ?: 0
                                r.addCategory(id, List(n) { Category(order = it) }, name, type, row["emoji"].orEmpty(), row["color"].orEmpty().ifBlank { "#64748B" })
                                counts[type] = n + 1; added++
                            }
                        }
                        Entity.WALLETS -> {
                            val known = wallets.value.map { it.name.lowercase() }.toMutableSet()
                            rows.forEach { row ->
                                val name = row["name"].orEmpty().trim().take(40)
                                if (name.isEmpty() || !known.add(name.lowercase())) return@forEach
                                r.addWallet(id, name, row["icon"].orEmpty().ifBlank { "account_balance_wallet" }, row["color"].orEmpty().ifBlank { "#64748B" }, row["balance"]?.toLongOrNull() ?: 0L)
                                added++
                            }
                        }
                        Entity.SHOPS -> {
                            val known = shops.value.map { it.name.lowercase() }.toMutableSet()
                            var order = nextOrder(shops.value.map { it.order })
                            rows.forEach { row ->
                                val name = row["name"].orEmpty().trim().take(60)
                                if (name.isEmpty() || !known.add(name.lowercase())) return@forEach
                                r.saveShop(id, null, order++, name, row["address"].orEmpty(), row["mapurl"].orEmpty(), row["imageurl"].orEmpty(), row["description"].orEmpty(), (row["rating"]?.toIntOrNull() ?: 0).coerceIn(0, 5))
                                added++
                            }
                        }
                        Entity.DRINKS -> {
                            val known = drinks.value.map { it.name.lowercase() }.toMutableSet()
                            var order = nextOrder(drinks.value.map { it.order })
                            rows.forEach { row ->
                                val name = row["name"].orEmpty().trim().take(60)
                                if (name.isEmpty() || !known.add(name.lowercase())) return@forEach
                                r.saveDrink(id, null, order++, name, row["imageurl"].orEmpty(), row["description"].orEmpty())
                                added++
                            }
                        }
                        else -> {}
                    }
                    if (added == 0) toast(R.string.import_empty) else toast(R.string.imported, added)
                }
            }
        } catch (e: Exception) { toast(R.string.import_error) }
    }

    /** Imports every entity found in an "export all" JSON file (wallets and categories first so transactions can link to them). */
    fun importAll(text: String) {
        if (!DataIo.isJson(text)) { toast(R.string.import_error); return }
        viewModelScope.launch {
            listOf(Entity.WALLETS, Entity.CATEGORIES, Entity.SHOPS, Entity.DRINKS).forEach { importEntity(it, text) }
            // Give the local cache a moment so imported wallets / categories are matched, not duplicated.
            kotlinx.coroutines.delay(800)
            importEntity(Entity.TRANSACTIONS, text)
        }
    }
}
