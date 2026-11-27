package com.gnort67.calendarpro.ui.screens

import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import java.time.LocalDateTime

class Upcoming(val holiday: Holiday, val at: LocalDateTime)

/** Mọi ngày lễ hệ thống + sự kiện cá nhân, kèm lần diễn ra kế tiếp, sắp xếp theo thời gian. */
fun upcomingAll(catalog: HolidayCatalog, events: List<PersonalEvent>, now: LocalDateTime): List<Upcoming> =
    (catalog.holidays + events.map { it.toHoliday() })
        .mapNotNull { h -> DateUtils.getNextOccurrence(h, now)?.let { Upcoming(h, it) } }
        .sortedBy { it.at }

/** Số ngày còn lại (0 nếu diễn ra trong hôm nay). */
fun daysLeft(u: Upcoming, now: LocalDateTime): Int = DateUtils.diffParts(u.at, now).days
