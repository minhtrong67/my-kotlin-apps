package com.expenser.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Locale-aware helpers. `lang` is set from the active app language (see MainActivity). */
object Format {
    @Volatile var lang: String = "vi"

    val locale: Locale get() = if (lang == "en") Locale.UK else Locale.forLanguageTag("vi-VN")

    fun money(amount: Long): String {
        val nf = NumberFormat.getCurrencyInstance(locale)
        nf.currency = Currency.getInstance("VND")
        nf.maximumFractionDigits = 0
        nf.minimumFractionDigits = 0
        return nf.format(amount)
    }

    fun number(n: Long): String = NumberFormat.getInstance(locale).format(n)

    private fun fmt(pattern: String, ms: Long) = SimpleDateFormat(pattern, locale).format(Date(ms))

    fun date(ms: Long) = fmt("dd/MM/yyyy", ms)
    fun dateShort(ms: Long) = fmt("dd/MM", ms)
    fun time(ms: Long) = fmt("HH:mm", ms)
    fun dayHeader(ms: Long) = "${fmt("EEEE", ms).replaceFirstChar { it.titlecase(locale) }}, ${date(ms)}"

    fun monthYear(ms: Long): String =
        if (lang == "vi") {
            val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
            "Tháng ${c.get(java.util.Calendar.MONTH) + 1}/${c.get(java.util.Calendar.YEAR)}"
        } else fmt("MMMM yyyy", ms)

    /** 1,5tr / 200k (vi) or 1.5M / 200K (en) — for compact chart labels. */
    fun compactMoney(n: Long): String {
        val abs = kotlin.math.abs(n)
        val en = lang == "en"
        fun trim(v: Double): String {
            val s = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
            return if (en) s else s.replace('.', ',')
        }
        return when {
            abs >= 1_000_000_000 -> trim(n / 1_000_000_000.0) + if (en) "B" else "tỷ"
            abs >= 1_000_000 -> trim(n / 1_000_000.0) + if (en) "M" else "tr"
            abs >= 1_000 -> "${Math.round(n / 1000.0)}${if (en) "K" else "k"}"
            else -> n.toString()
        }
    }

    /** "1.500.000" -> 1500000 */
    fun parseMoney(s: String): Long = s.filter { it.isDigit() }.take(15).toLongOrNull() ?: 0L

    /**
     * Quick-pick amounts for a short typed number: "2" offers 2.000 / 20.000 / 200.000 / 2.000.000.
     * Only offered while the number is still short (< 1.000).
     */
    fun amountSuggestions(value: Long): List<Long> {
        if (value <= 0 || value >= 1000) return emptyList()
        return listOf(1_000L, 10_000L, 100_000L, 1_000_000L).map { it * value }
    }
}
