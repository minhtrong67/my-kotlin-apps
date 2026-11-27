package com.gnort67.calendarpro.core

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Thuật toán chuyển đổi Dương lịch <-> Âm lịch Việt Nam (Hồ Ngọc Đức), múi giờ GMT+7.
 * Port từ `lib/lunar.js` của bản web — giữ nguyên công thức để kết quả khớp tuyệt đối.
 */
data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean)

data class SaoNgay(val name: String, val good: Boolean)

data class HuongXuatHanh(val hyThan: String, val taiThan: String)

object LunarCalendar {
    private const val TZ = 7.0
    private const val SYNODIC = 29.530588853
    private const val EPOCH = 2415021.076998695

    fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        return jd
    }

    /** Trả về [ngày, tháng, năm] */
    fun jdToDate(jd: Int): IntArray {
        val a: Int
        val b: Int
        val c: Int
        if (jd > 2299160) {
            a = jd + 32044
            b = (4 * a + 3) / 146097
            c = a - (b * 146097) / 4
        } else {
            b = 0
            c = jd + 32082
        }
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = b * 100 + d - 4800 + m / 10
        return intArrayOf(day, month, year)
    }

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 -= 0.4068 * sin(mpr * dr)
        c1 += 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 += 0.0104 * sin(dr * 2 * f)
        c1 -= 0.0051 * sin(dr * (m + mpr))
        c1 -= 0.0074 * sin(dr * (m - mpr))
        c1 += 0.0004 * sin(dr * (2 * f + m))
        c1 -= 0.0004 * sin(dr * (2 * f - m))
        c1 -= 0.0006 * sin(dr * (2 * f + mpr))
        c1 += 0.0010 * sin(dr * (2 * f - mpr))
        c1 += 0.0005 * sin(dr * (2 * mpr + m))
        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltat
    }

    private fun sunLongitude(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * floor(l / (PI * 2))
        return l
    }

    private fun getSunLongitude(dayNumber: Int, tz: Double): Int =
        floor(sunLongitude(dayNumber - 0.5 - tz / 24) / PI * 6).toInt()

    private fun getNewMoonDay(k: Int, tz: Double): Int = floor(newMoon(k) + 0.5 + tz / 24).toInt()

    private fun getLunarMonth11(yy: Int, tz: Double): Int {
        val off = jdFromDate(31, 12, yy) - EPOCH
        val k = floor(off / SYNODIC).toInt()
        var nm = getNewMoonDay(k, tz)
        val sunLong = getSunLongitude(nm, tz)
        if (sunLong >= 9) nm = getNewMoonDay(k - 1, tz)
        return nm
    }

    private fun getLeapMonthOffset(a11: Int, tz: Double): Int {
        val k = floor((a11 - EPOCH) / SYNODIC + 0.5).toInt()
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, tz), tz)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, tz), tz)
        } while (arc != last && i < 14)
        return i - 1
    }

    /** Dương lịch -> âm lịch. */
    fun solarToLunar(dd: Int, mm: Int, yy: Int, tz: Double = TZ): LunarDate {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - EPOCH) / SYNODIC).toInt()
        var monthStart = getNewMoonDay(k + 1, tz)
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k, tz)
        var a11 = getLunarMonth11(yy, tz)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1, tz)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1, tz)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = (monthStart - a11) / 29
        var lunarLeap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, tz)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) lunarLeap = true
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1
        return LunarDate(lunarDay, lunarMonth, lunarYear, lunarLeap)
    }

    /** Âm lịch -> dương lịch. Trả về [ngày, tháng, năm] hoặc null nếu ngày âm không tồn tại. */
    fun lunarToSolar(lunarDay: Int, lunarMonth: Int, lunarYear: Int, lunarLeap: Int = 0, tz: Double = TZ): IntArray? {
        val a11: Int
        val b11: Int
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1, tz)
            b11 = getLunarMonth11(lunarYear, tz)
        } else {
            a11 = getLunarMonth11(lunarYear, tz)
            b11 = getLunarMonth11(lunarYear + 1, tz)
        }
        val k = floor(0.5 + (a11 - EPOCH) / SYNODIC).toInt()
        var off = lunarMonth - 11
        if (off < 0) off += 12
        if (b11 - a11 > 365) {
            val leapOff = getLeapMonthOffset(a11, tz)
            var leapMonth = leapOff - 2
            if (leapMonth < 0) leapMonth += 12
            if (lunarLeap != 0 && lunarMonth != leapMonth) return null
            else if (lunarLeap != 0 || off >= leapOff) off += 1
        }
        val monthStart = getNewMoonDay(k + off, tz)
        return jdToDate(monthStart + lunarDay - 1)
    }

    // ---- Can Chi ----
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val CHI_ANIMAL = arrayOf("Chuột", "Trâu", "Hổ", "Mèo", "Rồng", "Rắn", "Ngựa", "Dê", "Khỉ", "Gà", "Chó", "Lợn")
    private val CHI_ANIMAL_EN = arrayOf("Rat", "Ox", "Tiger", "Cat", "Dragon", "Snake", "Horse", "Goat", "Monkey", "Rooster", "Dog", "Pig")

    fun canChiYear(lunarYear: Int) = "${CAN[(lunarYear + 6) % 10]} ${CHI[(lunarYear + 8) % 12]}"

    fun chiAnimalYear(lunarYear: Int, english: Boolean = false) =
        (if (english) CHI_ANIMAL_EN else CHI_ANIMAL)[(lunarYear + 8) % 12]

    fun canChiMonth(lunarMonth: Int, lunarYear: Int) =
        "${CAN[(lunarYear * 12 + lunarMonth + 3) % 10]} ${CHI[(lunarMonth + 1) % 12]}"

    fun canChiDay(dd: Int, mm: Int, yy: Int): String {
        val jd = jdFromDate(dd, mm, yy)
        return "${CAN[(jd + 9) % 10]} ${CHI[(jd + 1) % 12]}"
    }

    private val HOANG_DAO_BY_PAIR = arrayOf(
        intArrayOf(0, 1, 3, 6, 8, 9), // Tý – Ngọ
        intArrayOf(2, 3, 5, 8, 10, 11), // Sửu – Mùi
        intArrayOf(0, 1, 4, 5, 7, 10), // Dần – Thân
        intArrayOf(0, 2, 3, 6, 7, 9), // Mão – Dậu
        intArrayOf(2, 4, 5, 8, 9, 11), // Thìn – Tuất
        intArrayOf(1, 4, 6, 7, 10, 11) // Tỵ – Hợi
    )

    private val GIO_CHI_LABEL = arrayOf(
        "23h-1h (Tý)", "1h-3h (Sửu)", "3h-5h (Dần)", "5h-7h (Mão)",
        "7h-9h (Thìn)", "9h-11h (Tỵ)", "11h-13h (Ngọ)", "13h-15h (Mùi)",
        "15h-17h (Thân)", "17h-19h (Dậu)", "19h-21h (Tuất)", "21h-23h (Hợi)"
    )

    fun gioHoangDao(dd: Int, mm: Int, yy: Int): List<String> {
        val chiIndex = (jdFromDate(dd, mm, yy) + 1) % 12
        return HOANG_DAO_BY_PAIR[chiIndex % 6].map { GIO_CHI_LABEL[it] }
    }

    private val SAO_NGAY = arrayOf(
        SaoNgay("Thanh Long", true), SaoNgay("Minh Đường", true), SaoNgay("Thiên Hình", false),
        SaoNgay("Chu Tước", false), SaoNgay("Kim Quỹ", true), SaoNgay("Bảo Quang", true),
        SaoNgay("Bạch Hổ", false), SaoNgay("Ngọc Đường", true), SaoNgay("Thiên Lao", false),
        SaoNgay("Nguyên Vũ", false), SaoNgay("Tư Mệnh", true), SaoNgay("Câu Trần", false)
    )
    private val SAO_START_CHI = intArrayOf(0, 2, 4, 6, 8, 10)

    fun saoNgay(dd: Int, mm: Int, yy: Int, lunarMonth: Int): SaoNgay {
        val dayChi = (jdFromDate(dd, mm, yy) + 1) % 12
        val start = SAO_START_CHI[(lunarMonth - 1) % 6]
        return SAO_NGAY[(dayChi - start + 12) % 12]
    }

    private const val DONG_BAC = "Đông Bắc"
    private const val DONG = "Chính Đông"
    private const val DONG_NAM = "Đông Nam"
    private const val NAM = "Chính Nam"
    private const val TAY_NAM = "Tây Nam"
    private const val TAY = "Chính Tây"
    private const val TAY_BAC = "Tây Bắc"
    private const val BAC = "Chính Bắc"
    private val HY_THAN = arrayOf(DONG_BAC, TAY_BAC, TAY_NAM, NAM, DONG_NAM)
    private val TAI_THAN = arrayOf(DONG_NAM, DONG_NAM, DONG, DONG, BAC, NAM, TAY_NAM, TAY_NAM, TAY, TAY_BAC)

    val DIRECTION_EN = mapOf(
        DONG_BAC to "Northeast", DONG to "East", DONG_NAM to "Southeast", NAM to "South",
        TAY_NAM to "Southwest", TAY to "West", TAY_BAC to "Northwest", BAC to "North"
    )

    fun huongXuatHanh(dd: Int, mm: Int, yy: Int): HuongXuatHanh {
        val can = (jdFromDate(dd, mm, yy) + 9) % 10
        return HuongXuatHanh(HY_THAN[can % 5], TAI_THAN[can])
    }

    private val MONTH_NAME_LUNAR = arrayOf(
        "Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy", "Tám", "Chín", "Mười", "Mười Một", "Chạp"
    )

    fun lunarMonthName(m: Int) = MONTH_NAME_LUNAR[m - 1]
}
