package com.gnort67.calendarpro.core

import java.time.DayOfWeek
import java.time.LocalDate

/** Ngày lễ theo quy tắc từng năm (Phục sinh, thứ N của tháng). Port từ `lib/holidayRules.js`. */
object HolidayRules {
    /** Chủ nhật Phục sinh (Meeus/Jones/Butcher). */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** weekday theo kiểu JS: 0 = Chủ nhật ... 6 = Thứ bảy. */
    fun nthWeekday(year: Int, month: Int, weekday: Int, nth: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        val firstJs = jsWeekday(first)
        val offset = (weekday - firstJs + 7) % 7
        return first.plusDays((offset + (nth - 1) * 7).toLong())
    }

    fun jsWeekday(d: LocalDate): Int = if (d.dayOfWeek == DayOfWeek.SUNDAY) 0 else d.dayOfWeek.value

    fun isRule(type: String) = type == "easter" || type == "nth-weekday"
}
