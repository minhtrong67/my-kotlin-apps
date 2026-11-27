package com.gnort67.calendarpro.core

import java.time.LocalDate

/**
 * Một ngày lễ / sự kiện. `type`: "solar" | "lunar" | "easter" | "nth-weekday".
 * Sự kiện cá nhân (personal = true) giữ tên người dùng nhập trong [customName].
 */
data class Holiday(
    val id: String,
    val type: String,
    val day: Int = 0,
    val month: Int = 0,
    val weekday: Int = 0,
    val nth: Int = 0,
    val isEve: Boolean = false,
    val category: String,
    val icon: String,
    val ruleVi: String? = null,
    val ruleEn: String? = null,
    val personal: Boolean = false,
    val eventId: String? = null,
    val year: Int? = null,
    val customName: String? = null
)

object HolidayLogic {
    val CATEGORIES = listOf("quoc-le", "truyen-thong", "ky-niem", "quoc-te", "ton-giao", "ca-nhan")

    /** Thập trai / Nhị trai. */
    val NGAY_CHAY_NHI_TRAI = listOf(1, 15)

    /** Ngày lễ hệ thống rơi vào [date] (cần kèm thông tin âm lịch của ngày đó). */
    fun getHolidaysOnDate(all: List<Holiday>, date: LocalDate, lunar: LunarDate): List<Holiday> {
        var tomorrow: LunarDate? = null
        if (lunar.month == 12 && lunar.day >= 29) tomorrow = DateUtils.getLunarInfo(date.plusDays(1))
        return all.filter { h ->
            when {
                HolidayRules.isRule(h.type) -> {
                    val d = if (h.type == "easter") HolidayRules.easterSunday(date.year)
                    else HolidayRules.nthWeekday(date.year, h.month, h.weekday, h.nth)
                    d.monthValue == date.monthValue && d.dayOfMonth == date.dayOfMonth
                }
                h.type == "solar" -> h.day == date.dayOfMonth && h.month == date.monthValue
                h.isEve -> tomorrow != null && tomorrow.day == h.day && tomorrow.month == h.month && !tomorrow.isLeap
                else -> h.day == lunar.day && h.month == lunar.month && !lunar.isLeap
            }
        }
    }
}

/** Sự kiện cá nhân (sinh nhật, giỗ, kỷ niệm...). `type`: "solar" | "lunar". */
data class PersonalEvent(
    val id: String,
    val name: String,
    val type: String,
    val day: Int,
    val month: Int,
    val year: Int? = null,
    val icon: String = "CakeSlice"
) {
    fun toHoliday() = Holiday(
        id = "my-$id", type = type, day = day, month = month, category = "ca-nhan", icon = icon,
        personal = true, eventId = id, year = year, customName = name
    )

    fun isOn(date: LocalDate, lunar: LunarDate): Boolean =
        if (type == "solar") day == date.dayOfMonth && month == date.monthValue
        else day == lunar.day && month == lunar.month && !lunar.isLeap

    companion object {
        const val MAX_EVENTS = 100
        val ICONS = listOf("CakeSlice", "Heart", "PartyPopper", "GraduationCap", "Home", "Baby")

        fun maxDayOf(type: String, month: Int): Int =
            if (type == "lunar") 30 else listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31).getOrElse(month - 1) { 31 }

        fun isValid(e: PersonalEvent): Boolean =
            e.name.isNotBlank() && (e.type == "solar" || e.type == "lunar") &&
                e.day in 1..(if (e.type == "lunar") 30 else 31) && e.month in 1..12
    }
}
