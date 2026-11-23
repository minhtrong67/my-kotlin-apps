package com.expenser.app.util

import androidx.compose.ui.graphics.Color
import com.expenser.app.data.Category
import java.text.Normalizer
import java.util.Calendar

/** Lower-case and strip Vietnamese diacritics so "Cà phê", "CAFE" and "Coffee" compare alike. */
fun normalizeText(s: String?): String =
    Normalizer.normalize((s ?: "").lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
        .trim()

/** A category counts as the "cafe" category when its name mentions cafe / cà phê / coffee. */
fun isCafeCategory(c: Category?): Boolean {
    if (c == null) return false
    val n = normalizeText(c.name).replace(Regex("[^a-z0-9]+"), " ")
    return "cafe" in n || "caphe" in n || "ca phe" in n || "coffee" in n
}

enum class Session(val color: Color) {
    MORNING(Color(0xFFF59E0B)),
    NOON(Color(0xFFF97316)),
    AFTERNOON(Color(0xFF8B5CF6)),
    EVENING(Color(0xFF3B82F6)),
    NIGHT(Color(0xFF6366F1)),
}

/** Part of the day: morning 05–11, noon 11–14, afternoon 14–18, evening 18–22, else night. */
fun sessionOf(ms: Long): Session {
    val h = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..10 -> Session.MORNING
        h in 11..13 -> Session.NOON
        h in 14..17 -> Session.AFTERNOON
        h in 18..21 -> Session.EVENING
        else -> Session.NIGHT
    }
}

val CHART_PALETTE = listOf(
    "#B45309", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6",
    "#EC4899", "#06B6D4", "#EF4444", "#84CC16", "#64748B",
).map { Color(android.graphics.Color.parseColor(it)) }
