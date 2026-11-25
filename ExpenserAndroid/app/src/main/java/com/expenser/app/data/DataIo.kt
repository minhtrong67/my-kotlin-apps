package com.expenser.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class Entity(val key: String) { TRANSACTIONS("transactions"), CATEGORIES("categories"), WALLETS("wallets"), SHOPS("cafeShops"), DRINKS("cafeDrinks") }

data class DataSnapshot(
    val transactions: List<Tx>, val categories: List<Category>, val wallets: List<Wallet>,
    val shops: List<CafeShop>, val drinks: List<CafeDrink>,
)

/** Localised labels for the transaction CSV (kept compatible with the web export). */
data class TxLabels(val headers: List<String>, val typeLabels: Map<String, String>)

class ExportFile(val name: String, val bytes: ByteArray)

/** Per-entity CSV / JSON export and import rows. Rows are plain string maps so both formats share the importer. */
object DataIo {
    private val COLUMNS = mapOf(
        Entity.CATEGORIES to listOf("name", "type", "emoji", "color"),
        Entity.WALLETS to listOf("name", "icon", "color", "balance"),
        Entity.SHOPS to listOf("name", "address", "mapUrl", "imageUrl", "description", "rating"),
        Entity.DRINKS to listOf("name", "imageUrl", "description"),
    )

    private fun esc(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    private fun rows(e: Entity, s: DataSnapshot): List<Map<String, String>> = when (e) {
        Entity.CATEGORIES -> s.categories.map { mapOf("name" to it.name, "type" to it.type, "emoji" to it.emoji, "color" to it.color) }
        Entity.WALLETS -> s.wallets.map { mapOf("name" to it.name, "icon" to it.icon, "color" to it.color, "balance" to it.balance.toString()) }
        Entity.SHOPS -> s.shops.map { mapOf("name" to it.name, "address" to it.address, "mapUrl" to it.mapUrl, "imageUrl" to it.imageUrl, "description" to it.description, "rating" to it.rating.toString()) }
        Entity.DRINKS -> s.drinks.map { mapOf("name" to it.name, "imageUrl" to it.imageUrl, "description" to it.description) }
        Entity.TRANSACTIONS -> emptyList()
    }

    private fun txRows(s: DataSnapshot): List<Map<String, String>> {
        val cats = s.categories.associateBy { it.id }; val wal = s.wallets.associateBy { it.id }
        return s.transactions.filter { it.type != TxType.TRANSFER }.map {
            mapOf(
                "date" to com.expenser.app.util.Format.date(it.date), "time" to com.expenser.app.util.Format.time(it.date), "type" to it.type,
                "category" to (cats[it.categoryId ?: ""]?.name ?: ""), "wallet" to (wal[it.walletId ?: ""]?.name ?: ""),
                "amount" to it.amount.toString(), "note" to it.note,
            )
        }
    }

    fun csv(e: Entity, s: DataSnapshot, labels: TxLabels): String {
        if (e == Entity.TRANSACTIONS) {
            val cats = s.categories.associateBy { it.id }; val wal = s.wallets.associateBy { it.id }
            return CsvIo.build(labels.headers, s.transactions, { cats[it ?: ""]?.name ?: "" }, { wal[it ?: ""]?.name ?: "" }, { labels.typeLabels[it] ?: it })
        }
        val cols = COLUMNS.getValue(e)
        val sb = StringBuilder("﻿").append(cols.joinToString(",")).append("\r\n")
        rows(e, s).forEach { r -> sb.append(cols.joinToString(",") { esc(r[it].orEmpty()) }).append("\r\n") }
        return sb.toString()
    }

    private fun jsonArray(e: Entity, s: DataSnapshot): JSONArray {
        val arr = JSONArray()
        val list = if (e == Entity.TRANSACTIONS) txRows(s) else rows(e, s)
        list.forEach { r ->
            val o = JSONObject()
            r.forEach { (k, v) -> if ((k == "amount" || k == "balance" || k == "rating") && v.toLongOrNull() != null) o.put(k, v.toLong()) else o.put(k, v) }
            arr.put(o)
        }
        return arr
    }

    fun json(e: Entity, s: DataSnapshot): String = jsonArray(e, s).toString(2)

    fun jsonAll(s: DataSnapshot): String {
        val o = JSONObject().put("app", "expenser").put("version", 1)
        Entity.entries.forEach { o.put(it.key, jsonArray(it, s)) }
        return o.toString(2)
    }

    fun zipAllCsv(s: DataSnapshot, labels: TxLabels): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            Entity.entries.forEach { e ->
                z.putNextEntry(ZipEntry("${e.key}.csv")); z.write(csv(e, s, labels).toByteArray(Charsets.UTF_8)); z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    // ---------------- import ----------------
    private fun csvRows(text: String): List<Map<String, String>> {
        val t = CsvIo.parse(text)
        if (t.size < 2) return emptyList()
        val head = t[0].map { it.trim().lowercase() }
        return t.drop(1).map { r -> head.indices.associate { head[it] to r.getOrNull(it).orEmpty().trim() } }
    }

    private fun jsonRows(arr: JSONArray): List<Map<String, String>> =
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { o -> o.keys().asSequence().associate { it.lowercase() to o.opt(it)?.toString().orEmpty() } }

    /** Plain rows for a single entity, from either a CSV or a JSON file (JSON may be an array or the "all" object). */
    fun parseRows(e: Entity, text: String): List<Map<String, String>> {
        val t = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        return try {
            when {
                t.startsWith("[") -> jsonRows(JSONArray(t))
                t.startsWith("{") -> JSONObject(t).optJSONArray(e.key)?.let { jsonRows(it) } ?: emptyList()
                else -> csvRows(t)
            }
        } catch (_: Exception) { emptyList() }
    }

    fun isJson(text: String) = text.trimStart('﻿', ' ', '\n', '\r', '\t').let { it.startsWith("[") || it.startsWith("{") }

    /** Transactions from CSV go through [CsvIo.parseImport]; JSON rows are converted to the same CSV shape first. */
    fun parseTransactions(text: String): ImportResult {
        if (!isJson(text)) return CsvIo.parseImport(text)
        val rows = parseRows(Entity.TRANSACTIONS, text)
        val sb = StringBuilder("date,time,type,category,wallet,amount,note\r\n")
        rows.forEach { r -> sb.append(listOf("date", "time", "type", "category", "wallet", "amount", "note").joinToString(",") { esc(r[it].orEmpty()) }).append("\r\n") }
        return CsvIo.parseImport(sb.toString())
    }
}
