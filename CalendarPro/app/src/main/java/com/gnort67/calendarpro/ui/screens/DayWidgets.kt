package com.gnort67.calendarpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.HolidayLogic
import com.gnort67.calendarpro.core.HuongXuatHanh
import com.gnort67.calendarpro.core.LunarCalendar
import com.gnort67.calendarpro.core.LunarDate
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.core.SaoNgay
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.ui.components.AppIcon
import com.gnort67.calendarpro.ui.components.BlobDecoration
import com.gnort67.calendarpro.ui.components.ShapeBadge
import com.gnort67.calendarpro.ui.components.holidayIconName
import com.gnort67.calendarpro.ui.components.pressable
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.extra
import java.time.LocalDate

/** Toàn bộ thông tin vạn niên của một ngày dương lịch. */
class DayInfo(
    val date: LocalDate,
    val lunar: LunarDate,
    val canChiDay: String,
    val canChiMonth: String,
    val canChiYear: String,
    val animal: String,
    val sao: SaoNgay,
    val huong: HuongXuatHanh,
    val gio: List<String>,
    val holidays: List<Holiday>,
    val myEvents: List<PersonalEvent>
) {
    val isFirst get() = lunar.day == 1
    val isFull get() = lunar.day == 15
    val isChay get() = lunar.day in HolidayLogic.NGAY_CHAY_NHI_TRAI
}

fun buildDayInfo(date: LocalDate, catalog: HolidayCatalog, events: List<PersonalEvent>, lang: String): DayInfo {
    val l = DateUtils.getLunarInfo(date)
    val (d, m, y) = Triple(date.dayOfMonth, date.monthValue, date.year)
    return DayInfo(
        date = date, lunar = l,
        canChiDay = LunarCalendar.canChiDay(d, m, y),
        canChiMonth = LunarCalendar.canChiMonth(l.month, l.year),
        canChiYear = LunarCalendar.canChiYear(l.year),
        animal = LunarCalendar.chiAnimalYear(l.year, lang == "en"),
        sao = LunarCalendar.saoNgay(d, m, y, l.month),
        huong = LunarCalendar.huongXuatHanh(d, m, y),
        gio = LunarCalendar.gioHoangDao(d, m, y),
        holidays = HolidayLogic.getHolidaysOnDate(catalog.holidays, date, l),
        myEvents = events.filter { it.isOn(date, l) }
    )
}

@Composable
fun categoryLabel(cat: String): String = stringResource(
    when (cat) {
        "quoc-le" -> R.string.category_quoc_le
        "truyen-thong" -> R.string.category_truyen_thong
        "ky-niem" -> R.string.category_ky_niem
        "quoc-te" -> R.string.category_quoc_te
        "ton-giao" -> R.string.category_ton_giao
        else -> R.string.category_ca_nhan
    }
)

@Composable
fun lunarMonthText(l: LunarDate, lang: String): String {
    val name = if (lang == "en") l.month.toString() else LunarCalendar.lunarMonthName(l.month)
    return (if (l.isLeap) stringResource(R.string.van_nien_detail_leap) else "") +
        stringResource(R.string.van_nien_detail_month_of, name)
}

fun direction(d: String, lang: String) = if (lang == "en") LunarCalendar.DIRECTION_EN[d] ?: d else d

/** Thẻ lớn: dương lịch + âm lịch của một ngày. */
@Composable
fun DateHeroCard(info: DayInfo, lang: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
        Box {
            BlobDecoration(Modifier.matchParentSize(), cs.primary)
            Column(Modifier.padding(26.dp)) {
                Text(stringResource(R.string.van_nien_detail_solar), style = MaterialTheme.typography.bodyLarge)
                Gap(4)
                Text(DateUtils.formatVNDate(info.date, lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Gap(18)
                Text(stringResource(R.string.van_nien_detail_lunar), style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.lunar.day.toString(), fontSize = 76.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 84.sp)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(lunarMonthText(info.lunar, lang), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.van_nien_detail_year_of, info.canChiYear, info.lunar.year),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (info.isChay) {
                    Gap(8)
                    Pill(
                        stringResource(if (info.isFirst) R.string.van_nien_detail_vegetarian_first_day else R.string.van_nien_detail_vegetarian_full_moon),
                        MaterialTheme.extra.chayContainer, MaterialTheme.extra.onChayContainer, "eco"
                    )
                }
            }
        }
    }
}

// Một số tiện ích bố cục nhỏ để code gọn.
@Composable fun Gap(h: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(h.dp))
@Composable fun RowScope.HSpacer(w: Int) = androidx.compose.foundation.layout.Spacer(Modifier.width(w.dp))

@Composable
fun Pill(text: String, container: Color, content: Color, icon: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(container).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { AppIcon(icon, tint = content, size = 16.dp, filled = true); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)) }
        Text(text, color = content, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Thẻ ngày lễ rơi vào ngày đang xem. */
@Composable
fun HolidayBanner(h: Holiday, catalog: HolidayCatalog, lang: String, onClick: () -> Unit) {
    val ex = MaterialTheme.extra
    val t = catalog.texts(h, lang)
    Surface(
        Modifier.fillMaxWidth().pressable(RoundedCornerShape(32.dp), onClick = onClick),
        shape = RoundedCornerShape(32.dp), color = ex.holidayContainer, contentColor = ex.onHolidayContainer
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(holidayIconName(h.icon), ExpressiveShapes.Scallop, ex.holiday, ex.onHoliday, size = 56.dp, iconSize = 28.dp, filled = true)
                HSpacer(14)
                Column {
                    Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Gap(6)
                    Pill(categoryLabel(h.category), MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f), ex.onHolidayContainer)
                }
            }
            if (t.desc.isNotBlank()) { Gap(12); Text(t.desc, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
fun CanChiTiles(info: DayInfo, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoTile(R.string.van_nien_detail_day_can_chi, info.canChiDay, "wb_sunny", Modifier.weight(1f))
        InfoTile(R.string.van_nien_detail_month_can_chi, info.canChiMonth, "auto_awesome", Modifier.weight(1f))
        InfoTile(R.string.van_nien_detail_year_animal, info.animal, "cruelty_free", Modifier.weight(1f))
    }
}

@Composable
private fun InfoTile(label: Int, value: String, icon: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            ShapeBadge(icon, CircleShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, size = 40.dp, iconSize = 20.dp)
            Gap(12)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun SaoNgayCard(info: DayInfo, modifier: Modifier = Modifier) {
    val ex = MaterialTheme.extra
    val good = info.sao.good
    val container = if (good) ex.chayContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (good) ex.onChayContainer else MaterialTheme.colorScheme.onErrorContainer
    val badge = if (good) ex.chay else MaterialTheme.colorScheme.error
    val onBadge = if (good) ex.onChay else MaterialTheme.colorScheme.onError
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = container, contentColor = content) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge("auto_awesome", ExpressiveShapes.Flower, badge, onBadge, size = 60.dp, iconSize = 30.dp, filled = true)
            HSpacer(16)
            Column {
                Text(stringResource(R.string.van_nien_detail_day_star), style = MaterialTheme.typography.labelLarge)
                Text(info.sao.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(if (good) R.string.van_nien_detail_good_day else R.string.van_nien_detail_bad_day), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DirectionsCard(info: DayInfo, lang: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon("explore", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    HSpacer(10)
                    Text(stringResource(R.string.van_nien_detail_directions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Gap(10)
                Text("${stringResource(R.string.van_nien_detail_hy_than)}: ${direction(info.huong.hyThan, lang)}", style = MaterialTheme.typography.bodyLarge)
                Gap(4)
                Text("${stringResource(R.string.van_nien_detail_tai_than)}: ${direction(info.huong.taiThan, lang)}", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text(
            stringResource(R.string.van_nien_detail_direction_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LuckyHoursCard(info: DayInfo, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon("schedule", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                HSpacer(10)
                Text(stringResource(R.string.van_nien_detail_lucky_hours), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Gap(12)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                info.gio.forEach {
                    Text(
                        it, Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 14.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Danh sách sự kiện cá nhân rơi vào ngày đang xem. */
@Composable
fun MyEventsCard(info: DayInfo, modifier: Modifier = Modifier) {
    if (info.myEvents.isEmpty()) return
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.van_nien_detail_my_events), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            info.myEvents.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(holidayIconName(it.icon), size = 20.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    HSpacer(10)
                    Text(it.name, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Nội dung vạn niên đầy đủ của một ngày (dùng ở Trang chủ, Vạn niên và bottom sheet của Lịch). */
@Composable
fun DayDetailContent(
    info: DayInfo, catalog: HolidayCatalog, lang: String, onHoliday: (Holiday) -> Unit,
    modifier: Modifier = Modifier, showDirections: Boolean = true
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DateHeroCard(info, lang)
        info.holidays.forEach { HolidayBanner(it, catalog, lang) { onHoliday(it) } }
        MyEventsCard(info)
        CanChiTiles(info)
        SaoNgayCard(info)
        if (showDirections) DirectionsCard(info, lang)
        LuckyHoursCard(info)
    }
}
