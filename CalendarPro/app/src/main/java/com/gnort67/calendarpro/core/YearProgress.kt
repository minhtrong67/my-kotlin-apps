package com.gnort67.calendarpro.core

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Tiến độ năm (dương lịch / âm lịch) — port từ `lib/yearProgress.js`. */
object YearProgress {
    private const val DAY_MS = 86_400_000L

    data class Range(val kind: String, val label: String, val start: LocalDate, val end: LocalDate) {
        val startMs: Long get() = ms(start)
        val endMs: Long get() = ms(end)
    }

    data class Stats(
        val fraction: Double, val totalDays: Int, val passedDays: Int, val leftDays: Int,
        val dayNumber: Int, val weekNumber: Int, val totalWeeks: Int, val perDay: Double
    )

    data class Segment(val label: String, val startMs: Long, val endMs: Long, val leap: Boolean) {
        fun fill(nowMs: Long): Double = min(1.0, max(0.0, (nowMs - startMs).toDouble() / (endMs - startMs)))
        fun contains(nowMs: Long) = nowMs >= startMs && nowMs < endMs
    }

    fun ms(d: LocalDate): Long = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun solarYearRange(now: LocalDate) =
        Range("solar", now.year.toString(), LocalDate.of(now.year, 1, 1), LocalDate.of(now.year + 1, 1, 1))

    private fun tetOf(lunarYear: Int): LocalDate {
        val s = LunarCalendar.lunarToSolar(1, 1, lunarYear, 0)!!
        return LocalDate.of(s[2], s[1], s[0])
    }

    fun lunarYearRange(now: LocalDate): Range {
        val ly = DateUtils.getLunarInfo(now).year
        return Range("lunar", LunarCalendar.canChiYear(ly), tetOf(ly), tetOf(ly + 1))
    }

    fun getRange(kind: String, now: LocalDate) = if (kind == "lunar") lunarYearRange(now) else solarYearRange(now)

    fun progressOf(range: Range, nowMs: Long): Stats {
        val total = (range.endMs - range.startMs).toDouble()
        val elapsed = min(max((nowMs - range.startMs).toDouble(), 0.0), total)
        val totalDays = (total / DAY_MS).roundToInt()
        val passedDays = floor(elapsed / DAY_MS).toInt()
        return Stats(
            fraction = elapsed / total, totalDays = totalDays, passedDays = passedDays,
            leftDays = totalDays - passedDays - 1, dayNumber = passedDays + 1,
            weekNumber = passedDays / 7 + 1, totalWeeks = ceil(totalDays / 7.0).toInt(),
            perDay = 100.0 / totalDays
        )
    }

    fun monthSegments(range: Range): List<Segment> {
        if (range.kind == "solar") {
            val y = range.start.year
            return (0 until 12).map { m ->
                Segment((m + 1).toString(), ms(LocalDate.of(y, m + 1, 1)), ms(LocalDate.of(y, m + 1, 1).plusMonths(1)), false)
            }
        }
        data class S(val date: LocalDate, val label: String, val leap: Boolean)
        val starts = ArrayList<S>()
        var d = range.start
        while (d.isBefore(range.end)) {
            val l = DateUtils.getLunarInfo(d)
            if (l.day == 1) starts.add(S(d, l.month.toString(), l.isLeap))
            d = d.plusDays(1)
        }
        return starts.mapIndexed { i, s ->
            Segment(s.label, ms(s.date), if (i + 1 < starts.size) ms(starts[i + 1].date) else range.endMs, s.leap)
        }
    }

    fun dateAtFraction(range: Range, fraction: Double): LocalDate {
        val t = range.startMs + ((range.endMs - range.startMs) * fraction).toLong()
        return java.time.Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
