package com.minhtrong67.moody.util

import android.content.Context
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.Entry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun today(): LocalDate = LocalDate.now()
fun todayISO(): String = today().toString()
fun isoToDate(iso: String): LocalDate = LocalDate.parse(iso)

fun startOfWeek(date: LocalDate = today()): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Monday .. Sunday of the week containing [date]. */
fun daysOfWeek(date: LocalDate = today()): List<LocalDate> = startOfWeek(date).let { s -> (0..6).map { s.plusDays(it.toLong()) } }

/** Latest entry of each day (map date -> entry). */
fun latestByDate(entries: List<Entry>): Map<String, Entry> {
    val map = HashMap<String, Entry>()
    for (e in entries) {
        val cur = map[e.date]
        if (cur == null || e.instant > cur.instant) map[e.date] = e
    }
    return map
}

fun calcStreak(byDate: Map<String, Entry>): Int {
    var streak = 0
    var cursor = today()
    while (byDate.containsKey(cursor.toString())) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun relativeDateLabel(context: Context, iso: String, locale: Locale): String {
    val diff = ChronoUnit.DAYS.between(isoToDate(iso), today())
    return when (diff) {
        0L -> context.getString(R.string.common_today)
        1L -> context.getString(R.string.common_yesterday)
        else -> isoToDate(iso).format(DateTimeFormatter.ofPattern("EEE, dd/MM", locale))
    }
}

fun formatTime(entry: Entry, locale: Locale): String =
    entry.instant.atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

fun monthTitle(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

fun fullDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
