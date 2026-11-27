package com.gnort67.calendarpro

import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.HolidayRules
import com.gnort67.calendarpro.core.LunarCalendar
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Các giá trị kỳ vọng được lấy từ chính bản web (lunar.js) cho cùng ngày. */
class CoreTest {
    @Test fun solarToLunar_today_screenshot() {
        val l = LunarCalendar.solarToLunar(26, 11, 2026)
        assertEquals(18, l.day); assertEquals(10, l.month); assertEquals(2026, l.year)
        assertEquals("Giáp Thìn", LunarCalendar.canChiDay(26, 11, 2026))
        assertEquals("Kỷ Hợi", LunarCalendar.canChiMonth(l.month, l.year))
        assertEquals("Bính Ngọ", LunarCalendar.canChiYear(l.year))
        assertEquals("Tư Mệnh", LunarCalendar.saoNgay(26, 11, 2026, l.month).name)
        val h = LunarCalendar.huongXuatHanh(26, 11, 2026)
        assertEquals("Đông Bắc", h.hyThan); assertEquals("Đông Nam", h.taiThan)
    }

    @Test fun tet_dates() {
        assertArrayEquals(intArrayOf(17, 2, 2026), LunarCalendar.lunarToSolar(1, 1, 2026))
        assertArrayEquals(intArrayOf(6, 2, 2027), LunarCalendar.lunarToSolar(1, 1, 2027))
    }

    @Test fun invalid_leap_month_returns_null() {
        assertNull(LunarCalendar.lunarToSolar(1, 4, 2025, 1))
    }

    @Test fun thanksgiving_2026() {
        assertEquals(LocalDate.of(2026, 11, 26), HolidayRules.nthWeekday(2026, 11, 4, 4))
    }

    @Test fun next_occurrence_of_christmas() {
        val xmas = Holiday(id = "giang-sinh", type = "solar", day = 25, month = 12, category = "quoc-te", icon = "TreePine")
        val n = DateUtils.getNextOccurrence(xmas, LocalDateTime.of(2026, 11, 26, 15, 30, 10))!!
        assertEquals(LocalDate.of(2026, 12, 25), n.toLocalDate())
        assertEquals(28, DateUtils.diffParts(n, LocalDateTime.of(2026, 11, 26, 15, 30, 10)).days)
    }
}
