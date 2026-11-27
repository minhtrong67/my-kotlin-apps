package com.gnort67.calendarpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.DateUtils
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.ui.components.*
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.extra
import com.gnort67.calendarpro.util.Sharing
import java.time.LocalDateTime

/** Bottom sheet chi tiết ngày lễ / sự kiện. Vuốt xuống hoặc chạm nền để đóng. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HolidayDetailSheet(
    holiday: Holiday, catalog: HolidayCatalog, events: List<PersonalEvent>, lang: String,
    onDismiss: () -> Unit, onEdit: (PersonalEvent) -> Unit, onDelete: (PersonalEvent) -> Unit, onMessage: (String) -> Unit
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val theme = catalog.theme(holiday)
    val t = catalog.texts(holiday, lang)
    val now = remember { LocalDateTime.now() }
    val next = remember(holiday) { DateUtils.getNextOccurrence(holiday, now) }
    val left = next?.let { DateUtils.diffParts(it, now).days }
    val date = next?.toLocalDate()
    val ev = holiday.eventId?.let { id -> events.firstOrNull { it.id == id } }
    val noCalendar = stringResource(R.string.no_calendar_app)
    val todayText = stringResource(R.string.holiday_detail_share_today)
    val unitText = stringResource(R.string.holiday_detail_share_unit)
    val imageFailed = stringResource(R.string.holiday_detail_image_failed)

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow, shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        contentWindowInsets = { sheetInsets }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bg = theme?.strong ?: cs.primaryContainer
                ShapeBadge(holidayIconName(holiday.icon), ExpressiveShapes.Scallop, bg, if (theme != null) Color.White else cs.onPrimaryContainer, size = 68.dp, iconSize = 34.dp, filled = true)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(t.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(categoryLabel(holiday.category), cs.surfaceContainerHighest, cs.onSurfaceVariant)
                        if (holiday.personal) Pill(stringResource(R.string.holiday_detail_repeats_yearly), cs.secondaryContainer, cs.onSecondaryContainer)
                    }
                }
            }

            if (date != null && left != null) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
                    Column(Modifier.padding(22.dp)) {
                        Text(stringResource(R.string.holiday_detail_last_occurrence), style = MaterialTheme.typography.labelLarge)
                        Text(DateUtils.formatVNDate(date, lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Gap(10)
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (left == 0 && date == now.toLocalDate()) {
                                Text(stringResource(R.string.holiday_detail_share_today), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                            } else {
                                Text(left.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
                                Text(" " + stringResource(R.string.holiday_detail_share_unit), Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (ev != null && holiday.year != null) {
                            val n = DateUtils.anniversaryNumber(holiday, date)
                            if (n > 0) Text(stringResource(R.string.holiday_detail_anniversary, n), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            InfoRow(
                stringResource(R.string.holiday_detail_calendar_type),
                stringResource(when (holiday.type) { "solar" -> R.string.holiday_detail_by_solar; "lunar" -> R.string.holiday_detail_by_lunar; else -> R.string.holiday_detail_by_rule }) +
                    (catalog.rule(holiday, lang)?.let { " — $it" } ?: "")
            )

            if (holiday.personal) {
                Text(stringResource(R.string.holiday_detail_personal_note), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            } else {
                if (t.meaning.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.holiday_detail_meaning))
                    t.meaning.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
                if (t.customs.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.holiday_detail_customs))
                    t.customs.forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 9.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(cs.primary))
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                t.fact?.let { f ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon("auto_awesome", size = 20.dp, filled = true)
                                Text(stringResource(R.string.holiday_detail_fact), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Gap(8)
                            Text(f, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (date != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { if (!Sharing.addToCalendar(ctx, t.name, date, holiday.type == "solar")) onMessage(noCalendar) },
                        Modifier.weight(1f).height(52.dp)
                    ) { AppIcon("event", size = 18.dp); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.holiday_detail_add_to_calendar), maxLines = 1) }
                    FilledTonalButton(
                        onClick = {
                            val c1 = theme?.stops?.first()?.second?.toArgb() ?: cs.primary.toArgb()
                            val c2 = theme?.stops?.last()?.second?.toArgb() ?: cs.tertiary.toArgb()
                            val fg = if (theme != null) Color.White.toArgb() else cs.onPrimary.toArgb()
                            val big = if (left == 0 && date == now.toLocalDate()) todayText else left.toString()
                            val small = if (left == 0 && date == now.toLocalDate()) "" else unitText
                            if (!Sharing.shareCard(ctx, t.name, DateUtils.formatVNDate(date, lang), big, small, "Calendar Pro", c1, c2, fg)) onMessage(imageFailed)
                        },
                        Modifier.weight(1f).height(52.dp)
                    ) { AppIcon("share", size = 18.dp); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.holiday_detail_share_image), maxLines = 1) }
                }
            }
            if (ev != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onEdit(ev) }, Modifier.weight(1f).height(52.dp)) {
                        AppIcon("edit", size = 18.dp); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.holiday_detail_edit))
                    }
                    OutlinedButton(
                        onClick = { onDelete(ev) }, Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error)
                    ) { AppIcon("delete", size = 18.dp); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.holiday_detail_remove)) }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** Bottom sheet thêm / sửa sự kiện cá nhân. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormSheet(initial: PersonalEvent?, onDismiss: () -> Unit, onSave: (PersonalEvent) -> Unit, onMessage: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableIntStateOf(if (initial?.type == "lunar") 1 else 0) }
    var day by remember { mutableStateOf(initial?.day?.toString() ?: "") }
    var month by remember { mutableStateOf(initial?.month?.toString() ?: "") }
    var year by remember { mutableStateOf(initial?.year?.toString() ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "CakeSlice") }
    val needName = stringResource(R.string.events_name_required)
    val badSolar = stringResource(R.string.van_nien_invalid_solar)
    val badLunar = stringResource(R.string.van_nien_invalid_lunar)
    val id = remember { initial?.id ?: java.util.UUID.randomUUID().toString().take(8) }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow, shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        contentWindowInsets = { sheetInsets }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(if (initial == null) R.string.events_new_title else R.string.events_edit_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            OutlinedTextField(
                name, { name = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(20.dp),
                label = { Text(stringResource(R.string.events_name)) }, placeholder = { Text(stringResource(R.string.events_name_placeholder)) }
            )
            Text(stringResource(R.string.events_calendar_type), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            SegmentedControl(listOf(SegOption(stringResource(R.string.events_solar)), SegOption(stringResource(R.string.events_lunar))), type, { type = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumField(stringResource(R.string.events_day), day, Modifier.weight(1f)) { day = it.filter(Char::isDigit).take(2) }
                NumField(stringResource(R.string.events_month), month, Modifier.weight(1f)) { month = it.filter(Char::isDigit).take(2) }
                NumField(stringResource(R.string.events_year), year, Modifier.weight(1.4f)) { year = it.filter(Char::isDigit).take(4) }
            }
            Text(stringResource(R.string.events_year_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            if (type == 1) Text(stringResource(R.string.events_lunar_note), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text(stringResource(R.string.events_icon), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonalEvent.ICONS.forEach { key ->
                    val sel = key == icon
                    Box(
                        Modifier.weight(1f).height(52.dp).pressable(CircleShape) { icon = key }
                            .background(if (sel) cs.primaryContainer else cs.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) { AppIcon(holidayIconName(key), tint = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant, filled = sel) }
                }
            }
            Button(
                onClick = {
                    val d = day.toIntOrNull(); val m = month.toIntOrNull()
                    val kind = if (type == 1) "lunar" else "solar"
                    when {
                        name.isBlank() -> onMessage(needName)
                        d == null || m == null || m !in 1..12 || d !in 1..PersonalEvent.maxDayOf(kind, m) -> onMessage(if (type == 1) badLunar else badSolar)
                        else -> onSave(PersonalEvent(id, name.trim(), kind, d, m, year.toIntOrNull()?.takeIf { it in 1..9999 }, icon))
                    }
                },
                Modifier.fillMaxWidth().height(56.dp), shape = CircleShape
            ) { Text(stringResource(R.string.events_save), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, modifier, singleLine = true, shape = RoundedCornerShape(20.dp),
        label = { Text(label, maxLines = 1) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
