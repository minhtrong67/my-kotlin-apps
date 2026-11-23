package com.expenser.app.data

import com.expenser.app.util.Format
import com.expenser.app.util.normalizeText
import java.util.Calendar

data class ImportRow(
    val date: Long,
    val type: String,
    val categoryName: String,
    val walletName: String,
    val amount: Long,
    val note: String,
)

data class ImportResult(val rows: List<ImportRow>, val skipped: Int)

/** CSV import/export compatible with the web app (columns: Date, Time, Type, Category, Wallet, Amount, Note). */
object CsvIo {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("﻿")
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes && c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && (c == ',' || c == ';') -> { row.add(cell.toString()); cell.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    private fun esc(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    fun build(
        headers: List<String>,
        txs: List<Tx>,
        categoryName: (Long?) -> String,
        walletName: (Long?) -> String,
        typeLabel: (String) -> String,
    ): String {
        val sb = StringBuilder("﻿")
        sb.append(headers.joinToString(",") { esc(it) }).append("\r\n")
        txs.forEach { tx ->
            val wallet = if (tx.type == TxType.TRANSFER) "${walletName(tx.fromWalletId)} → ${walletName(tx.toWalletId)}" else walletName(tx.walletId)
            val cols = listOf(
                Format.date(tx.date), Format.time(tx.date), typeLabel(tx.type),
                if (tx.type == TxType.TRANSFER) "" else categoryName(tx.categoryId),
                wallet, tx.amount.toString(), tx.note,
            )
            sb.append(cols.joinToString(",") { esc(it) }).append("\r\n")
        }
        return sb.toString()
    }

    private val ALIASES = mapOf(
        "date" to listOf("ngay", "date"),
        "time" to listOf("gio", "time"),
        "type" to listOf("loai", "type"),
        "category" to listOf("danh muc", "category"),
        "wallet" to listOf("vi", "wallet"),
        "amount" to listOf("so tien", "amount"),
        "note" to listOf("ghi chu", "note"),
    )

    private fun parseAmount(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0
        if (Regex("^\\d+[.,]\\d{1,2}$").matches(s)) return Math.round(s.replace(',', '.').toDouble())
        return s.filter { it.isDigit() }.take(15).toLongOrNull() ?: 0
    }

    private fun parseDateTime(dateVal: String, timeVal: String): Long? {
        val s = dateVal.trim()
        val cal = Calendar.getInstance().apply { clear() }
        val dmy = Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})").find(s)
        val ymd = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(s)
        when {
            dmy != null -> cal.set(dmy.groupValues[3].toInt(), dmy.groupValues[2].toInt() - 1, dmy.groupValues[1].toInt())
            ymd != null -> cal.set(ymd.groupValues[1].toInt(), ymd.groupValues[2].toInt() - 1, ymd.groupValues[3].toInt())
            else -> return null
        }
        Regex("(\\d{1,2}):(\\d{2})").find(timeVal)?.let {
            cal.set(Calendar.HOUR_OF_DAY, it.groupValues[1].toInt())
            cal.set(Calendar.MINUTE, it.groupValues[2].toInt())
        }
        return cal.timeInMillis
    }

    fun parseImport(text: String): ImportResult {
        val table = parse(text)
        if (table.size < 2) return ImportResult(emptyList(), 0)
        val index = mutableMapOf<String, Int>()
        table[0].forEachIndexed { i, h ->
            val key = normalizeText(h)
            ALIASES.entries.firstOrNull { key in it.value }?.let { index.putIfAbsent(it.key, i) }
        }
        val rows = mutableListOf<ImportRow>()
        var skipped = 0
        for (r in table.drop(1)) {
            fun col(name: String) = index[name]?.let { r.getOrNull(it) }?.trim().orEmpty()
            val typeKey = normalizeText(col("type"))
            if (typeKey == "chuyen khoan" || typeKey == "transfer") continue
            val category = col("category")
            val wallet = col("wallet")
            val amount = parseAmount(col("amount"))
            val date = parseDateTime(col("date"), col("time"))
            if (category.isEmpty() || wallet.isEmpty() || amount <= 0 || date == null) { skipped++; continue }
            val type = if (typeKey.startsWith("thu") || typeKey.startsWith("income") || typeKey == "+") TxType.INCOME else TxType.EXPENSE
            rows.add(ImportRow(date, type, category, wallet, amount, col("note").take(200)))
        }
        return ImportResult(rows, skipped)
    }
}
