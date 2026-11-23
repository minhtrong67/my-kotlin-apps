package com.expenser.app.util

import java.util.Calendar

enum class Range { TODAY, WEEK, MONTH, YEAR, ALL }

data class Period(val start: Long, val end: Long, val fullEnd: Long, val isCurrent: Boolean)

private fun cal(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }

fun startOfDay(ms: Long): Long = cal(ms).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun endOfDay(ms: Long): Long = startOfDay(ms) + 24L * 3600_000 - 1 // DST-safe enough for display ranges

private fun endOfDayCal(c: Calendar): Long = c.apply {
    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
}.timeInMillis

/** The period (day / Mon–Sun week / month / year / everything) containing [anchor]. */
fun periodFor(range: Range, anchor: Long, now: Long = System.currentTimeMillis()): Period {
    val a = cal(anchor)
    val start: Long
    val fullEnd: Long
    when (range) {
        Range.TODAY -> {
            start = startOfDay(anchor); fullEnd = endOfDayCal(cal(anchor))
        }
        Range.WEEK -> {
            val dow = (a.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
            val s = cal(startOfDay(anchor)).apply { add(Calendar.DAY_OF_MONTH, -dow) }
            start = s.timeInMillis
            fullEnd = endOfDayCal(cal(start).apply { add(Calendar.DAY_OF_MONTH, 6) })
        }
        Range.MONTH -> {
            val s = cal(anchor).apply { set(Calendar.DAY_OF_MONTH, 1) }
            start = startOfDay(s.timeInMillis)
            fullEnd = endOfDayCal(cal(start).apply { add(Calendar.MONTH, 1); add(Calendar.DAY_OF_MONTH, -1) })
        }
        Range.YEAR -> {
            val s = cal(anchor).apply { set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1) }
            start = startOfDay(s.timeInMillis)
            fullEnd = endOfDayCal(cal(start).apply { add(Calendar.YEAR, 1); add(Calendar.DAY_OF_MONTH, -1) })
        }
        Range.ALL -> {
            start = 0L; fullEnd = now
        }
    }
    return Period(start, if (fullEnd > now) now else fullEnd, fullEnd, start <= now && fullEnd >= now)
}

fun shiftAnchor(range: Range, anchor: Long, delta: Int): Long {
    val c = cal(anchor)
    when (range) {
        Range.TODAY -> c.add(Calendar.DAY_OF_MONTH, delta)
        Range.WEEK -> c.add(Calendar.DAY_OF_MONTH, 7 * delta)
        Range.MONTH -> { c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.MONTH, delta) }
        Range.YEAR -> { c.set(Calendar.MONTH, 0); c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.YEAR, delta) }
        Range.ALL -> {}
    }
    return c.timeInMillis
}

fun Long.field(field: Int): Int = cal(this).get(field)
fun dayKey(ms: Long): Long = startOfDay(ms)
fun monthKey(ms: Long): Int = cal(ms).let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) }
