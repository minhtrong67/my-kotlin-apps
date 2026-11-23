package com.expenser.app.data

import androidx.room.withTransaction
import com.expenser.app.util.DEFAULT_EXPENSE_CATEGORIES
import com.expenser.app.util.DEFAULT_INCOME_CATEGORIES
import com.expenser.app.util.DEFAULT_WALLETS

class Repository(private val db: AppDatabase, private val settings: SettingsStore) {
    private val dao = db.dao()

    val transactions = dao.observeTransactions()
    val categories = dao.observeCategories()
    val wallets = dao.observeWallets()
    val shops = dao.observeShops()
    val drinks = dao.observeDrinks()
    val logs = dao.observeLogs()

    private fun sign(type: String) = if (type == TxType.INCOME) 1L else -1L

    /** Seeds default categories + wallets exactly once (deleting them later does not bring them back). */
    suspend fun seedIfNeeded(english: Boolean, alreadySeeded: Boolean) {
        if (alreadySeeded) return
        db.withTransaction {
            if (dao.categoryCount() == 0) {
                var i = 0
                dao.insertCategories(DEFAULT_EXPENSE_CATEGORIES.map {
                    Category(name = if (english) it.en else it.vi, type = TxType.EXPENSE, emoji = it.emoji, color = it.color, sortOrder = i++)
                })
                i = 0
                dao.insertCategories(DEFAULT_INCOME_CATEGORIES.map {
                    Category(name = if (english) it.en else it.vi, type = TxType.INCOME, emoji = it.emoji, color = it.color, sortOrder = i++)
                })
            }
            if (dao.walletCount() == 0) {
                dao.insertWallets(DEFAULT_WALLETS.map {
                    Wallet(name = if (english) it.en else it.vi, icon = it.icon, color = it.color)
                })
            }
        }
        settings.markSeeded()
    }

    // ---------------- Transactions ----------------
    /** Inserts or updates a transaction and keeps the wallet balance + cafe details in step, atomically. */
    suspend fun saveTransaction(editing: Tx?, input: TxInput): Long = db.withTransaction {
        val id: Long
        if (editing == null) {
            id = dao.insertTx(
                Tx(type = input.type, amount = input.amount, categoryId = input.categoryId, walletId = input.walletId,
                    date = input.date, note = input.note.trim()),
            )
        } else {
            editing.walletId?.let { dao.addBalance(it, -sign(editing.type) * editing.amount) } // undo old effect
            dao.updateTx(
                editing.copy(type = input.type, amount = input.amount, categoryId = input.categoryId,
                    walletId = input.walletId, date = input.date, note = input.note.trim()),
            )
            id = editing.id
        }
        dao.addBalance(input.walletId, sign(input.type) * input.amount)
        val cafe = input.cafe
        if (cafe != null) dao.upsertLog(CafeLog(id, cafe.shopId, cafe.drinkId, cafe.imageUrl.trim())) else dao.deleteLog(id)
        id
    }

    suspend fun deleteTransaction(tx: Tx) = db.withTransaction {
        if (tx.type == TxType.TRANSFER) {
            tx.fromWalletId?.let { dao.addBalance(it, tx.amount) }
            tx.toWalletId?.let { dao.addBalance(it, -tx.amount) }
        } else {
            tx.walletId?.let { dao.addBalance(it, -sign(tx.type) * tx.amount) }
        }
        dao.deleteTx(tx.id)
        dao.deleteLog(tx.id)
    }

    suspend fun transfer(fromId: Long, toId: Long, amount: Long, note: String, date: Long) = db.withTransaction {
        dao.addBalance(fromId, -amount)
        dao.addBalance(toId, amount)
        dao.insertTx(Tx(type = TxType.TRANSFER, amount = amount, fromWalletId = fromId, toWalletId = toId, date = date, note = note.trim()))
    }

    /** Bulk import. Missing categories / wallets are created first, then balances are updated once per wallet. */
    suspend fun importTransactions(rows: List<ImportRow>): Int {
        if (rows.isEmpty()) return 0
        return db.withTransaction {
            val catMap = dao.getCategories().associate { "${it.type}:${it.name.lowercase()}" to it.id }.toMutableMap()
            val walletMap = dao.getWallets().associate { it.name.lowercase() to it.id }.toMutableMap()
            val deltas = mutableMapOf<Long, Long>()
            val txs = rows.map { row ->
                val ck = "${row.type}:${row.categoryName.lowercase()}"
                val catId = catMap.getOrPut(ck) {
                    dao.insertCategory(Category(name = row.categoryName, type = row.type, sortOrder = dao.maxCategoryOrder(row.type) + 1))
                }
                val walletId = walletMap.getOrPut(row.walletName.lowercase()) {
                    dao.insertWallet(Wallet(name = row.walletName))
                }
                deltas[walletId] = (deltas[walletId] ?: 0) + sign(row.type) * row.amount
                Tx(type = row.type, amount = row.amount, categoryId = catId, walletId = walletId, date = row.date, note = row.note)
            }
            dao.insertTxs(txs)
            deltas.forEach { (id, d) -> dao.addBalance(id, d) }
            txs.size
        }
    }

    // ---------------- Categories ----------------
    suspend fun addCategory(name: String, type: String, emoji: String, color: String) {
        dao.insertCategory(Category(name = name.trim(), type = type, emoji = emoji.ifBlank { "🏷️" }, color = color, sortOrder = dao.maxCategoryOrder(type) + 1))
    }

    suspend fun updateCategory(c: Category) = dao.updateCategory(c.copy(name = c.name.trim(), emoji = c.emoji.ifBlank { "🏷️" }))

    /** Returns 0 when deleted, otherwise the number of transactions still using the category. */
    suspend fun deleteCategory(c: Category): Int = db.withTransaction {
        val n = dao.countTxByCategory(c.id)
        if (n == 0) dao.deleteCategory(c)
        n
    }

    /** Item i of [ids] (one type's categories) gets sortOrder i. */
    suspend fun reorderCategories(ids: List<Long>) = db.withTransaction { ids.forEachIndexed { i, id -> dao.setCategoryOrder(id, i) } }

    // ---------------- Wallets ----------------
    suspend fun addWallet(name: String, icon: String, color: String, balance: Long) {
        dao.insertWallet(Wallet(name = name.trim(), icon = icon, color = color, balance = balance))
    }

    suspend fun updateWallet(w: Wallet) = dao.updateWallet(w.copy(name = w.name.trim()))

    suspend fun deleteWallet(w: Wallet): Int = db.withTransaction {
        val n = dao.countTxByWallet(w.id)
        if (n == 0) dao.deleteWallet(w)
        n
    }

    // ---------------- Cafe ----------------
    suspend fun saveShop(editing: CafeShop?, name: String, address: String, mapUrl: String, imageUrl: String, description: String, rating: Int) {
        if (editing == null) {
            dao.insertShop(CafeShop(name = name.trim(), address = address.trim(), mapUrl = mapUrl.trim(), imageUrl = imageUrl.trim(),
                description = description.trim(), rating = rating, sortOrder = dao.maxShopOrder() + 1))
        } else {
            dao.updateShop(editing.copy(name = name.trim(), address = address.trim(), mapUrl = mapUrl.trim(),
                imageUrl = imageUrl.trim(), description = description.trim(), rating = rating))
        }
    }

    suspend fun deleteShop(s: CafeShop): Int = db.withTransaction {
        val n = dao.countLogsByShop(s.id)
        if (n == 0) dao.deleteShop(s)
        n
    }

    suspend fun saveDrink(editing: CafeDrink?, name: String, imageUrl: String, description: String) {
        if (editing == null) {
            dao.insertDrink(CafeDrink(name = name.trim(), imageUrl = imageUrl.trim(), description = description.trim(), sortOrder = dao.maxDrinkOrder() + 1))
        } else {
            dao.updateDrink(editing.copy(name = name.trim(), imageUrl = imageUrl.trim(), description = description.trim()))
        }
    }

    suspend fun deleteDrink(d: CafeDrink): Int = db.withTransaction {
        val n = dao.countLogsByDrink(d.id)
        if (n == 0) dao.deleteDrink(d)
        n
    }

    suspend fun reorderShops(ids: List<Long>) = db.withTransaction { ids.forEachIndexed { i, id -> dao.setShopOrder(id, i) } }
    suspend fun reorderDrinks(ids: List<Long>) = db.withTransaction { ids.forEachIndexed { i, id -> dao.setDrinkOrder(id, i) } }
}
