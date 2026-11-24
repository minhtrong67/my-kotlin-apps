package com.expenser.app.data

object TxType {
    const val INCOME = "income"
    const val EXPENSE = "expense"
    const val TRANSFER = "transfer"
}

// Same field names as the web app's Firestore documents (users/{uid}/<collection>/{id}).
data class Category(
    val id: String = "",
    val name: String = "",
    val type: String = TxType.EXPENSE,
    val emoji: String = "🏷️",
    val color: String = "#64748B",
    val order: Int? = null,
    val createdAt: Long = 0,
)

data class Wallet(
    val id: String = "",
    val name: String = "",
    val icon: String = "account_balance_wallet",
    val color: String = "#64748B",
    val balance: Long = 0,
    val createdAt: Long = 0,
)

/** income / expense use walletId + categoryId; transfer uses fromWalletId + toWalletId. */
data class Tx(
    val id: String = "",
    val type: String = TxType.EXPENSE,
    val amount: Long = 0,
    val categoryId: String? = null,
    val walletId: String? = null,
    val fromWalletId: String? = null,
    val toWalletId: String? = null,
    val date: Long = 0,
    val note: String = "",
)

data class CafeShop(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val mapUrl: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val rating: Int = 0,
    val order: Int? = null,
    val createdAt: Long = 0,
)

data class CafeDrink(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val order: Int? = null,
    val createdAt: Long = 0,
)

/** Cafe details of a transaction (document id = transaction id). */
data class CafeLog(val txId: String = "", val shopId: String? = null, val drinkId: String? = null, val imageUrl: String = "")

data class CafeInput(val shopId: String?, val drinkId: String?, val imageUrl: String)

data class TxInput(
    val type: String,
    val amount: Long,
    val categoryId: String,
    val walletId: String,
    val date: Long,
    val note: String,
    val cafe: CafeInput? = null,
)

data class UserProfile(val uid: String, val name: String, val email: String, val photoUrl: String?)
