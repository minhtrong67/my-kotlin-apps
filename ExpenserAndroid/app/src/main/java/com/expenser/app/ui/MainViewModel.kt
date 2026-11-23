package com.expenser.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenser.app.ExpenserApp
import com.expenser.app.R
import com.expenser.app.data.*
import com.expenser.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as ExpenserApp
    val repo = container.repo
    private val store = container.settingsStore

    val settings = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val wallets = repo.wallets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val shops = repo.shops.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val drinks = repo.drinks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs = repo.logs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** False until the first database snapshot has arrived (drives skeleton / loading UI). */
    val loading = MutableStateFlow(true)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    fun toast(@StringRes res: Int, vararg args: Any) { _messages.tryEmit(UiMessage(res, args.toList())) }

    init {
        viewModelScope.launch {
            val s = store.flow.first()
            val english = app.resources.configuration.locales[0].language == "en"
            repo.seedIfNeeded(english, s.seeded)
            combine(repo.transactions, repo.categories, repo.wallets) { _, _, _ -> }.first()
            loading.value = false
            if (s.reminderEnabled) ReminderScheduler.schedule(app, s.reminderTime)
        }
    }

    private fun launchOp(@StringRes error: Int, onDone: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                onDone()
            } catch (e: Exception) {
                toast(error)
            }
        }
    }

    // ---------- Transactions ----------
    fun saveTransaction(editing: Tx?, input: TxInput, onDone: () -> Unit) =
        launchOp(R.string.tx_save_error, onDone = { toast(if (editing == null) R.string.tx_added else R.string.tx_updated); onDone() }) {
            repo.saveTransaction(editing, input)
        }

    fun deleteTransaction(tx: Tx) = launchOp(R.string.tx_delete_error, onDone = { toast(R.string.tx_deleted) }) { repo.deleteTransaction(tx) }

    fun transfer(from: Long, to: Long, amount: Long, note: String, date: Long, onDone: () -> Unit) =
        launchOp(R.string.transfer_error, onDone = { toast(R.string.transfer_success); onDone() }) { repo.transfer(from, to, amount, note, date) }

    // ---------- Categories ----------
    fun addCategory(name: String, type: String, emoji: String, color: String, onDone: () -> Unit) =
        launchOp(R.string.cat_save_error, onDone = { toast(R.string.cat_added); onDone() }) { repo.addCategory(name, type, emoji, color) }

    fun updateCategory(c: Category, onDone: () -> Unit) =
        launchOp(R.string.cat_save_error, onDone = { toast(R.string.cat_updated); onDone() }) { repo.updateCategory(c) }

    fun deleteCategory(c: Category) = viewModelScope.launch {
        try {
            val n = repo.deleteCategory(c)
            if (n > 0) toast(R.string.cat_in_use, n) else toast(R.string.cat_deleted)
        } catch (e: Exception) { toast(R.string.cat_delete_error) }
    }

    fun reorderCategories(ids: List<Long>) = launchOp(R.string.reorder_error) { repo.reorderCategories(ids) }

    // ---------- Wallets ----------
    fun addWallet(name: String, icon: String, color: String, balance: Long, onDone: () -> Unit) =
        launchOp(R.string.wallet_save_error, onDone = { toast(R.string.wallet_added); onDone() }) { repo.addWallet(name, icon, color, balance) }

    fun updateWallet(w: Wallet, onDone: () -> Unit) =
        launchOp(R.string.wallet_save_error, onDone = { toast(R.string.wallet_updated); onDone() }) { repo.updateWallet(w) }

    fun deleteWallet(w: Wallet) = viewModelScope.launch {
        if (wallets.value.size <= 1) { toast(R.string.wallet_keep_one); return@launch }
        try {
            val n = repo.deleteWallet(w)
            if (n > 0) toast(R.string.wallet_in_use, n) else toast(R.string.wallet_deleted)
        } catch (e: Exception) { toast(R.string.wallet_delete_error) }
    }

    // ---------- Cafe ----------
    fun saveShop(editing: CafeShop?, name: String, address: String, mapUrl: String, imageUrl: String, description: String, rating: Int, onDone: () -> Unit) =
        launchOp(R.string.cafe_shop_save_error, onDone = onDone) { repo.saveShop(editing, name, address, mapUrl, imageUrl, description, rating) }

    fun deleteShop(s: CafeShop) = viewModelScope.launch {
        try {
            val n = repo.deleteShop(s)
            if (n > 0) toast(R.string.cafe_shop_in_use, n)
        } catch (e: Exception) { toast(R.string.cafe_delete_error) }
    }

    fun saveDrink(editing: CafeDrink?, name: String, imageUrl: String, description: String, onDone: () -> Unit) =
        launchOp(R.string.cafe_drink_save_error, onDone = onDone) { repo.saveDrink(editing, name, imageUrl, description) }

    fun deleteDrink(d: CafeDrink) = viewModelScope.launch {
        try {
            val n = repo.deleteDrink(d)
            if (n > 0) toast(R.string.cafe_drink_in_use, n)
        } catch (e: Exception) { toast(R.string.cafe_delete_error) }
    }

    fun reorderShops(ids: List<Long>) = launchOp(R.string.reorder_error) { repo.reorderShops(ids) }
    fun reorderDrinks(ids: List<Long>) = launchOp(R.string.reorder_error) { repo.reorderDrinks(ids) }

    // ---------- Settings ----------
    fun setThemeMode(v: String) = viewModelScope.launch { store.setThemeMode(v) }
    fun setAccent(v: String) = viewModelScope.launch { store.setAccent(v) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { store.setDynamicColor(v) }
    fun setFontSize(v: String) = viewModelScope.launch { store.setFontSize(v) }
    fun resetAppearance() = viewModelScope.launch { store.resetAppearance(); toast(R.string.settings_reset_done) }

    fun setReminder(enabled: Boolean, time: String) = viewModelScope.launch {
        store.setReminder(enabled, time)
        if (enabled) ReminderScheduler.schedule(getApplication(), time) else ReminderScheduler.cancel(getApplication())
    }

    fun sendTestReminder() = ReminderScheduler.show(getApplication())

    // ---------- Import / export ----------
    fun importCsv(text: String) = viewModelScope.launch {
        try {
            val result = CsvIo.parseImport(text)
            if (result.rows.isEmpty()) { toast(R.string.import_empty); return@launch }
            val n = repo.importTransactions(result.rows)
            toast(R.string.imported, n)
            if (result.skipped > 0) toast(R.string.import_skipped, result.skipped)
        } catch (e: Exception) { toast(R.string.import_error) }
    }
}
