package com.minhtrong67.moody.util

import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.MoodDef
import java.time.LocalDate

enum class Period(val key: String) { Day("day"), Week("week"), Month("month"), Year("year") }

/** Entries (latest per day) that fall into the period containing [cursor]. */
fun entriesInPeriod(period: Period, cursor: LocalDate, byDate: Map<String, Entry>): List<Entry> = when (period) {
    Period.Day -> listOfNotNull(byDate[cursor.toString()])
    Period.Week -> daysOfWeek(cursor).mapNotNull { byDate[it.toString()] }
    Period.Month -> (1..cursor.lengthOfMonth()).mapNotNull { byDate[cursor.withDayOfMonth(it).toString()] }
    Period.Year -> byDate.values.filter { isoToDate(it.date).year == cursor.year }
}

fun shiftCursor(period: Period, cursor: LocalDate, dir: Int): LocalDate = when (period) {
    Period.Day -> cursor.plusDays(dir.toLong())
    Period.Week -> cursor.plusWeeks(dir.toLong())
    Period.Month -> cursor.plusMonths(dir.toLong())
    Period.Year -> cursor.plusYears(dir.toLong())
}

class Slice(val key: String, val label: String, val emoji: String, val color: androidx.compose.ui.graphics.Color, val count: Int)

fun moodDistribution(entries: List<Entry>, moods: List<MoodDef>): List<Slice> {
    val counts = HashMap<String, Int>()
    for (e in entries) counts[e.mood] = (counts[e.mood] ?: 0) + 1
    return moods.map { Slice(it.key, it.label, it.emoji, it.color, counts[it.key] ?: 0) }
}

/** Percentages rounded by largest remainder so they always add up to 100 (as in the web legend). */
fun roundedPercents(counts: List<Int>): List<Int> {
    val total = counts.sum().coerceAtLeast(1)
    val base = counts.map { (it * 100) / total }.toMutableList()
    var left = (if (counts.any { it > 0 }) 100 else 0) - base.sum()
    counts.indices
        .filter { counts[it] > 0 }
        .sortedByDescending { ((counts[it] * 100.0) / total) % 1.0 }
        .forEach { if (left > 0) { base[it] = base[it] + 1; left-- } }
    return base
}
