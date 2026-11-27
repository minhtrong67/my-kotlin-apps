package com.gnort67.calendarpro.core

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Các hàm tiện ích ngày tháng — port từ `lib/dateUtils.js`. */
object DateUtils {
    val WEEKDAYS_SHORT_VI = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val WEEKDAYS_SHORT_EN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    /** Chỉ số theo kiểu JS getDay(): 0 = Chủ nhật. */
    val WEEKDAYS_FULL_VI = listOf("Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy")
    val WEEKDAYS_FULL_EN = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val MONTH_NAMES_VI = (1..12).map { "Tháng $it" }
    val MONTH_NAMES_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun weekdaysShort(lang: String) = if (lang == "en") WEEKDAYS_SHORT_EN else WEEKDAYS_SHORT_VI
    fun monthNames(lang: String) = if (lang == "en") MONTH_NAMES_EN else MONTH_NAMES_VI

    /** 0 = Thứ Hai ... 6 = Chủ Nhật. */
    fun mondayIndex(d: LocalDate): Int = d.dayOfWeek.value - 1

    fun pad2(n: Int) = n.toString().padStart(2, '0')

    /** Giống `new Date(y, m-1, d)` của JS: ngày vượt quá tháng sẽ tràn sang tháng sau (29/2 năm thường -> 1/3). */
    fun safeDate(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, 1).plusDays((day - 1).toLong())

    fun getLunarInfo(d: LocalDate): LunarDate = LunarCalendar.solarToLunar(d.dayOfMonth, d.monthValue, d.year)

    private fun lunarDateInYear(day: Int, month: Int, lunarYear: Int): LocalDate? =
        LunarCalendar.lunarToSolar(day, month, lunarYear, 0)?.let { LocalDate.of(it[2], it[1], it[0]) }

    /**
     * Lần diễn ra kế tiếp (từ [from]) của một ngày lễ / sự kiện.
     * Giao thừa (`isEve`) trả về 23:59:59 của ngày liền trước mùng 1 Tết.
     */
    fun getNextOccurrence(h: Holiday, from: LocalDateTime): LocalDateTime? {
        val today = from.toLocalDate()
        if (HolidayRules.isRule(h.type)) {
            for (y in today.year..today.year + 1) {
                val d = if (h.type == "easter") HolidayRules.easterSunday(y) else HolidayRules.nthWeekday(y, h.month, h.weekday, h.nth)
                if (!d.isBefore(today)) return d.atStartOfDay()
            }
            return null
        }
        if (h.type == "solar") {
            var candidate = safeDate(today.year, h.month, h.day)
            if (candidate.isBefore(today)) candidate = safeDate(today.year + 1, h.month, h.day)
            return candidate.atStartOfDay()
        }
        for (ly in today.year - 1..today.year + 2) {
            val base = lunarDateInYear(h.day, h.month, ly) ?: continue
            if (h.isEve) {
                val eve = base.minusDays(1).atTime(23, 59, 59)
                if (!eve.toLocalDate().isBefore(today)) return eve
            } else if (!base.isBefore(today)) {
                return base.atStartOfDay()
            }
        }
        return null
    }

    fun formatVNDate(d: LocalDate, lang: String = "vi"): String {
        val js = HolidayRules.jsWeekday(d)
        return if (lang == "en") {
            "${WEEKDAYS_FULL_EN[js]}, ${MONTH_NAMES_EN[d.monthValue - 1]} ${d.dayOfMonth}, ${d.year}"
        } else {
            "${WEEKDAYS_FULL_VI[js]}, ngày ${pad2(d.dayOfMonth)} tháng ${pad2(d.monthValue)} năm ${d.year}"
        }
    }

    fun formatShortDate(d: LocalDate) = "${pad2(d.dayOfMonth)}/${pad2(d.monthValue)}/${d.year}"

    data class Parts(val days: Int, val hours: Int, val minutes: Int, val seconds: Int)

    fun diffParts(target: LocalDateTime, from: LocalDateTime): Parts {
        var ms = Duration.between(from.toLocalDate().atStartOfDay(), target).toMillis()
        if (ms < 0) ms = 0
        val days = (ms / 86_400_000L).toInt()
        val clamped = maxOf(0L, Duration.between(from, target).toMillis())
        return Parts(
            days = days,
            hours = ((clamped / 3_600_000L) % 24).toInt(),
            minutes = ((clamped / 60_000L) % 60).toInt(),
            seconds = ((clamped / 1_000L) % 60).toInt()
        )
    }

    /** Lần kỷ niệm của sự kiện cá nhân (cần có `year` gốc); 0 nếu không có. */
    fun anniversaryNumber(h: Holiday, date: LocalDate): Int {
        val y0 = h.year ?: return 0
        if (y0 == 0) return 0
        val y = if (h.type == "lunar") getLunarInfo(date).year else date.year
        return maxOf(0, y - y0)
    }
}
