package com.expenser.app.data

import com.expenser.app.util.DEFAULT_EXPENSE_CATEGORIES
import com.expenser.app.util.DEFAULT_INCOME_CATEGORIES
import com.expenser.app.util.DEFAULT_WALLETS
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val BATCH_SIZE = 400

private fun DocumentSnapshot.long(f: String): Long = (get(f) as? Number)?.toLong() ?: 0L
private fun DocumentSnapshot.str(f: String): String = getString(f) ?: ""
private fun DocumentSnapshot.millis(f: String): Long = getTimestamp(f)?.toDate()?.time ?: 0L
private fun DocumentSnapshot.orderOrNull(): Int? = (get("order") as? Number)?.toInt()

/**
 * Firestore-backed data layer, document-compatible with the web app.
 * Writes are queued locally (offline persistence is on by default) and are never awaited, so the UI
 * never blocks while offline; local snapshot listeners fire immediately.
 */
class Repository(private val db: FirebaseFirestore, private val onWriteError: (Exception) -> Unit) {
    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun col(uid: String, name: String) = userDoc(uid).collection(name)

    private fun <T> listen(q: Query, map: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val reg = q.addSnapshotListener { snap, err ->
            if (snap != null) trySend(snap.documents.map(map)) else if (err != null) trySend(emptyList())
        }
        awaitClose { reg.remove() }
    }

    fun transactions(uid: String) = listen(col(uid, "transactions").orderBy("date", Query.Direction.DESCENDING)) { d ->
        Tx(
            id = d.id, type = d.str("type").ifBlank { TxType.EXPENSE }, amount = d.long("amount"),
            categoryId = d.getString("categoryId"), walletId = d.getString("walletId"),
            fromWalletId = d.getString("fromWalletId"), toWalletId = d.getString("toWalletId"),
            date = d.millis("date"), note = d.str("note"),
        )
    }

    fun categories(uid: String) = listen(col(uid, "categories")) { d ->
        Category(d.id, d.str("name"), d.str("type").ifBlank { TxType.EXPENSE }, d.str("emoji").ifBlank { "🏷️" }, d.str("color").ifBlank { "#64748B" }, d.orderOrNull(), d.millis("createdAt"))
    }

    fun wallets(uid: String) = listen(col(uid, "wallets")) { d ->
        Wallet(d.id, d.str("name"), d.str("icon").ifBlank { "account_balance_wallet" }, d.str("color").ifBlank { "#64748B" }, d.long("balance"), d.millis("createdAt"))
    }

    fun shops(uid: String) = listen(col(uid, "cafeShops")) { d ->
        CafeShop(d.id, d.str("name"), d.str("address"), d.str("mapUrl"), d.str("imageUrl"), d.str("description"), d.long("rating").toInt(), d.orderOrNull(), d.millis("createdAt"))
    }

    fun drinks(uid: String) = listen(col(uid, "cafeDrinks")) { d ->
        CafeDrink(d.id, d.str("name"), d.str("imageUrl"), d.str("description"), d.orderOrNull(), d.millis("createdAt"))
    }

    fun logs(uid: String) = listen(col(uid, "cafeLogs")) { d ->
        CafeLog(d.id, d.getString("shopId")?.ifBlank { null }, d.getString("drinkId")?.ifBlank { null }, d.str("imageUrl"))
    }

    private fun <T> queue(task: com.google.android.gms.tasks.Task<T>) { task.addOnFailureListener { onWriteError(it) } }

    private fun sign(type: String) = if (type == TxType.INCOME) 1L else -1L

    /** Seeds default categories + wallets once per account (a `seeded` flag on the user doc, as in the web app). */
    suspend fun seedIfNeeded(uid: String, english: Boolean) {
        try {
            val user = userDoc(uid).get().await()
            if (user.getBoolean("seeded") == true) return
            val hasCats = !col(uid, "categories").limit(1).get().await().isEmpty
            val hasWallets = !col(uid, "wallets").limit(1).get().await().isEmpty
            val batch = db.batch()
            if (!hasCats) {
                DEFAULT_EXPENSE_CATEGORIES.forEachIndexed { i, c ->
                    batch.set(col(uid, "categories").document(), mapOf("name" to if (english) c.en else c.vi, "type" to TxType.EXPENSE, "emoji" to c.emoji, "color" to c.color, "order" to i, "createdAt" to FieldValue.serverTimestamp()))
                }
                DEFAULT_INCOME_CATEGORIES.forEachIndexed { i, c ->
                    batch.set(col(uid, "categories").document(), mapOf("name" to if (english) c.en else c.vi, "type" to TxType.INCOME, "emoji" to c.emoji, "color" to c.color, "order" to i, "createdAt" to FieldValue.serverTimestamp()))
                }
            }
            if (!hasWallets) {
                DEFAULT_WALLETS.forEach { w ->
                    batch.set(col(uid, "wallets").document(), mapOf("name" to if (english) w.en else w.vi, "icon" to w.icon, "color" to w.color, "balance" to 0, "createdAt" to FieldValue.serverTimestamp()))
                }
            }
            batch.set(userDoc(uid), mapOf("seeded" to true), SetOptions.merge())
            batch.commit().await()
        } catch (_: Exception) { /* offline on first launch: try again next time */ }
    }

    // ---------------- Transactions ----------------
    fun saveTransaction(uid: String, editing: Tx?, input: TxInput) {
        val batch = db.batch()
        val txRef = if (editing == null) col(uid, "transactions").document() else col(uid, "transactions").document(editing.id)
        val data = mapOf(
            "type" to input.type, "amount" to input.amount, "categoryId" to input.categoryId, "walletId" to input.walletId,
            "date" to Timestamp(java.util.Date(input.date)), "note" to input.note.trim(),
        )
        if (editing == null) batch.set(txRef, data + ("createdAt" to FieldValue.serverTimestamp())) else batch.update(txRef, data)
        val logRef = col(uid, "cafeLogs").document(txRef.id)
        val cafe = input.cafe
        if (cafe != null) batch.set(logRef, mapOf("shopId" to (cafe.shopId ?: ""), "drinkId" to (cafe.drinkId ?: ""), "imageUrl" to cafe.imageUrl.trim(), "updatedAt" to FieldValue.serverTimestamp()))
        else if (editing != null) batch.delete(logRef)
        // Net the balance change per wallet.
        val deltas = mutableMapOf<String, Long>()
        if (editing?.walletId != null) deltas[editing.walletId] = (deltas[editing.walletId] ?: 0) - sign(editing.type) * editing.amount
        deltas[input.walletId] = (deltas[input.walletId] ?: 0) + sign(input.type) * input.amount
        deltas.forEach { (id, d) -> if (d != 0L) batch.update(col(uid, "wallets").document(id), "balance", FieldValue.increment(d)) }
        queue(batch.commit())
    }

    fun deleteTransaction(uid: String, tx: Tx) {
        val batch = db.batch()
        batch.delete(col(uid, "transactions").document(tx.id))
        batch.delete(col(uid, "cafeLogs").document(tx.id))
        if (tx.type == TxType.TRANSFER) {
            tx.fromWalletId?.let { batch.update(col(uid, "wallets").document(it), "balance", FieldValue.increment(tx.amount)) }
            tx.toWalletId?.let { batch.update(col(uid, "wallets").document(it), "balance", FieldValue.increment(-tx.amount)) }
        } else {
            tx.walletId?.let { batch.update(col(uid, "wallets").document(it), "balance", FieldValue.increment(-sign(tx.type) * tx.amount)) }
        }
        queue(batch.commit())
    }

    fun transfer(uid: String, fromId: String, toId: String, amount: Long, note: String, date: Long) {
        val batch = db.batch()
        batch.update(col(uid, "wallets").document(fromId), "balance", FieldValue.increment(-amount))
        batch.update(col(uid, "wallets").document(toId), "balance", FieldValue.increment(amount))
        batch.set(col(uid, "transactions").document(), mapOf(
            "type" to TxType.TRANSFER, "amount" to amount, "fromWalletId" to fromId, "toWalletId" to toId,
            "date" to Timestamp(java.util.Date(date)), "note" to note.trim(), "createdAt" to FieldValue.serverTimestamp(),
        ))
        queue(batch.commit())
    }

    /** Bulk import: creates missing categories / wallets, then writes transactions in batches with one balance update per wallet per batch. */
    fun importTransactions(uid: String, rows: List<ImportRow>, categories: List<Category>, wallets: List<Wallet>): Int {
        if (rows.isEmpty()) return 0
        val catMap = categories.associate { "${it.type}:${it.name.lowercase()}" to it.id }.toMutableMap()
        val walletMap = wallets.associate { it.name.lowercase() to it.id }.toMutableMap()
        val setup = db.batch()
        var needsSetup = false
        rows.forEach { r ->
            val ck = "${r.type}:${r.categoryName.lowercase()}"
            if (ck !in catMap) {
                val ref = col(uid, "categories").document()
                setup.set(ref, mapOf("name" to r.categoryName, "type" to r.type, "emoji" to "🏷️", "color" to "#64748B", "createdAt" to FieldValue.serverTimestamp()))
                catMap[ck] = ref.id; needsSetup = true
            }
            val wk = r.walletName.lowercase()
            if (wk !in walletMap) {
                val ref = col(uid, "wallets").document()
                setup.set(ref, mapOf("name" to r.walletName, "icon" to "account_balance_wallet", "color" to "#64748B", "balance" to 0, "createdAt" to FieldValue.serverTimestamp()))
                walletMap[wk] = ref.id; needsSetup = true
            }
        }
        if (needsSetup) queue(setup.commit())
        rows.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = db.batch()
            val deltas = mutableMapOf<String, Long>()
            chunk.forEach { r ->
                val walletId = walletMap[r.walletName.lowercase()]!!
                batch.set(col(uid, "transactions").document(), mapOf(
                    "type" to r.type, "amount" to r.amount, "categoryId" to catMap["${r.type}:${r.categoryName.lowercase()}"],
                    "walletId" to walletId, "date" to Timestamp(java.util.Date(r.date)), "note" to r.note, "createdAt" to FieldValue.serverTimestamp(),
                ))
                deltas[walletId] = (deltas[walletId] ?: 0) + sign(r.type) * r.amount
            }
            deltas.forEach { (id, d) -> batch.update(col(uid, "wallets").document(id), "balance", FieldValue.increment(d)) }
            queue(batch.commit())
        }
        return rows.size
    }

    // ---------------- Categories ----------------
    /** The new category goes to the END of its type's list; never-ordered siblings get their current position first. */
    fun addCategory(uid: String, siblings: List<Category>, name: String, type: String, emoji: String, color: String) {
        val batch = db.batch()
        siblings.forEachIndexed { i, c -> if (c.order != i) batch.update(col(uid, "categories").document(c.id), "order", i) }
        batch.set(col(uid, "categories").document(), mapOf(
            "name" to name.trim(), "type" to type, "emoji" to emoji.ifBlank { "🏷️" }, "color" to color, "order" to siblings.size, "createdAt" to FieldValue.serverTimestamp(),
        ))
        queue(batch.commit())
    }

    fun updateCategory(uid: String, c: Category, name: String, emoji: String, color: String) {
        queue(col(uid, "categories").document(c.id).update(mapOf("name" to name.trim(), "type" to c.type, "emoji" to emoji.ifBlank { "🏷️" }, "color" to color)))
    }

    fun deleteCategory(uid: String, id: String) { queue(col(uid, "categories").document(id).delete()) }

    fun reorder(uid: String, collection: String, ids: List<String>) {
        val batch = db.batch()
        ids.forEachIndexed { i, id -> batch.update(col(uid, collection).document(id), "order", i) }
        queue(batch.commit())
    }

    // ---------------- Wallets ----------------
    fun addWallet(uid: String, name: String, icon: String, color: String, balance: Long) {
        queue(col(uid, "wallets").document().set(mapOf("name" to name.trim(), "icon" to icon, "color" to color, "balance" to balance, "createdAt" to FieldValue.serverTimestamp())))
    }

    fun updateWallet(uid: String, id: String, name: String, icon: String, color: String) {
        queue(col(uid, "wallets").document(id).update(mapOf("name" to name.trim(), "icon" to icon, "color" to color)))
    }

    fun deleteWallet(uid: String, id: String) { queue(col(uid, "wallets").document(id).delete()) }

    // ---------------- Cafe ----------------
    fun saveShop(uid: String, editing: CafeShop?, nextOrder: Int, name: String, address: String, mapUrl: String, imageUrl: String, description: String, rating: Int) {
        val data = mutableMapOf<String, Any>("name" to name.trim(), "address" to address.trim(), "mapUrl" to mapUrl.trim(), "imageUrl" to imageUrl.trim(), "description" to description.trim(), "rating" to rating)
        if (editing == null) queue(col(uid, "cafeShops").document().set(data + mapOf("order" to nextOrder, "createdAt" to FieldValue.serverTimestamp())))
        else queue(col(uid, "cafeShops").document(editing.id).update(data))
    }

    fun deleteShop(uid: String, id: String) { queue(col(uid, "cafeShops").document(id).delete()) }

    fun saveDrink(uid: String, editing: CafeDrink?, nextOrder: Int, name: String, imageUrl: String, description: String) {
        val data = mutableMapOf<String, Any>("name" to name.trim(), "imageUrl" to imageUrl.trim(), "description" to description.trim())
        if (editing == null) queue(col(uid, "cafeDrinks").document().set(data + mapOf("order" to nextOrder, "createdAt" to FieldValue.serverTimestamp())))
        else queue(col(uid, "cafeDrinks").document(editing.id).update(data))
    }

    fun deleteDrink(uid: String, id: String) { queue(col(uid, "cafeDrinks").document(id).delete()) }
}
