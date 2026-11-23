package com.expenser.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object TxType {
    const val INCOME = "income"
    const val EXPENSE = "expense"
    const val TRANSFER = "transfer"
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // income | expense
    val emoji: String = "🏷️",
    val color: String = "#64748B",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "account_balance_wallet",
    val color: String = "#64748B",
    val balance: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** income / expense use walletId + categoryId; transfer uses fromWalletId + toWalletId. */
@Entity(tableName = "transactions")
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val categoryId: Long? = null,
    val walletId: Long? = null,
    val fromWalletId: Long? = null,
    val toWalletId: Long? = null,
    val date: Long, // epoch millis
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cafe_shops")
data class CafeShop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val mapUrl: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val rating: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cafe_drinks")
data class CafeDrink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUrl: String = "",
    val description: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Extra cafe details of a transaction (primary key = the transaction id). */
@Entity(tableName = "cafe_logs")
data class CafeLog(
    @PrimaryKey val txId: Long,
    val shopId: Long? = null,
    val drinkId: Long? = null,
    val imageUrl: String = "",
)

data class CafeInput(val shopId: Long?, val drinkId: Long?, val imageUrl: String)

data class TxInput(
    val type: String,
    val amount: Long,
    val categoryId: Long,
    val walletId: Long,
    val date: Long,
    val note: String,
    val cafe: CafeInput? = null,
)
