package com.expenser.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---- observe ----
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun observeTransactions(): Flow<List<Tx>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, name COLLATE NOCASE") fun observeCategories(): Flow<List<Category>>
    @Query("SELECT * FROM wallets ORDER BY createdAt, id") fun observeWallets(): Flow<List<Wallet>>
    @Query("SELECT * FROM cafe_shops ORDER BY sortOrder, id") fun observeShops(): Flow<List<CafeShop>>
    @Query("SELECT * FROM cafe_drinks ORDER BY sortOrder, id") fun observeDrinks(): Flow<List<CafeDrink>>
    @Query("SELECT * FROM cafe_logs") fun observeLogs(): Flow<List<CafeLog>>

    // ---- one-shot reads ----
    @Query("SELECT * FROM categories") suspend fun getCategories(): List<Category>
    @Query("SELECT * FROM wallets") suspend fun getWallets(): List<Wallet>
    @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int
    @Query("SELECT COUNT(*) FROM wallets") suspend fun walletCount(): Int

    // ---- transactions ----
    @Insert suspend fun insertTx(tx: Tx): Long
    @Update suspend fun updateTx(tx: Tx)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTx(id: Long)
    @Insert suspend fun insertTxs(list: List<Tx>)

    // ---- cafe logs ----
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLog(log: CafeLog)
    @Query("DELETE FROM cafe_logs WHERE txId = :txId") suspend fun deleteLog(txId: Long)

    // ---- wallets ----
    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :id") suspend fun addBalance(id: Long, delta: Long)
    @Insert suspend fun insertWallet(w: Wallet): Long
    @Insert suspend fun insertWallets(list: List<Wallet>)
    @Update suspend fun updateWallet(w: Wallet)
    @Delete suspend fun deleteWallet(w: Wallet)
    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :id OR fromWalletId = :id OR toWalletId = :id")
    suspend fun countTxByWallet(id: Long): Int

    // ---- categories ----
    @Insert suspend fun insertCategory(c: Category): Long
    @Insert suspend fun insertCategories(list: List<Category>)
    @Update suspend fun updateCategory(c: Category)
    @Delete suspend fun deleteCategory(c: Category)
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id") suspend fun countTxByCategory(id: Long): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories WHERE type = :type") suspend fun maxCategoryOrder(type: String): Int
    @Query("UPDATE categories SET sortOrder = :order WHERE id = :id") suspend fun setCategoryOrder(id: Long, order: Int)

    // ---- cafe shops / drinks ----
    @Insert suspend fun insertShop(s: CafeShop): Long
    @Update suspend fun updateShop(s: CafeShop)
    @Delete suspend fun deleteShop(s: CafeShop)
    @Query("SELECT COUNT(*) FROM cafe_logs WHERE shopId = :id") suspend fun countLogsByShop(id: Long): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM cafe_shops") suspend fun maxShopOrder(): Int
    @Query("UPDATE cafe_shops SET sortOrder = :order WHERE id = :id") suspend fun setShopOrder(id: Long, order: Int)

    @Insert suspend fun insertDrink(d: CafeDrink): Long
    @Update suspend fun updateDrink(d: CafeDrink)
    @Delete suspend fun deleteDrink(d: CafeDrink)
    @Query("SELECT COUNT(*) FROM cafe_logs WHERE drinkId = :id") suspend fun countLogsByDrink(id: Long): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM cafe_drinks") suspend fun maxDrinkOrder(): Int
    @Query("UPDATE cafe_drinks SET sortOrder = :order WHERE id = :id") suspend fun setDrinkOrder(id: Long, order: Int)
}
