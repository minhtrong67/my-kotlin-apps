package com.expenser.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenser.app.R
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.EMOJI_GROUPS
import com.expenser.app.util.Format
import com.expenser.app.util.toColor
import java.util.Calendar
import java.util.TimeZone

// ---------------------------------------------------------------- icons

fun walletIcon(key: String): ImageVector = when (key) {
    "payments" -> Icons.Rounded.Payments
    "account_balance" -> Icons.Rounded.AccountBalance
    "qr_code_2" -> Icons.Rounded.QrCode2
    "credit_card" -> Icons.Rounded.CreditCard
    "savings" -> Icons.Rounded.Savings
    "currency_exchange" -> Icons.Rounded.CurrencyExchange
    "diamond" -> Icons.Rounded.Diamond
    "smartphone" -> Icons.Rounded.Smartphone
    "attach_money" -> Icons.Rounded.AttachMoney
    "account_box" -> Icons.Rounded.AccountBox
    "real_estate_agent" -> Icons.Rounded.RealEstateAgent
    "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "business_center" -> Icons.Rounded.BusinessCenter
    else -> Icons.Rounded.AccountBalanceWallet
}

// ---------------------------------------------------------------- basic building blocks

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) FilledTonalButton(onClick = action.second) { Text(action.first) }
    }
}

@Composable
fun SectionCard(title: String?, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun CategoryAvatar(emoji: String, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.46f).sp)
    }
}

@Composable
fun IconAvatar(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun ConfirmDialog(text: String, confirmLabel: String = stringResource(R.string.common_delete), onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_confirm)) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) } },
        actions = actions,
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), singleLine = true,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.common_clear)) } },
        shape = RoundedCornerShape(28.dp),
    )
}

// ---------------------------------------------------------------- inputs

/** Shows digits grouped by thousands ("1.500.000") while the state stays plain digits. */
private class ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val sb = StringBuilder()
        val sep = if (Format.lang == "en") ',' else '.'
        digits.forEachIndexed { i, ch ->
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(sep)
            sb.append(ch)
        }
        val out = sb.toString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val seps = (1 until offset).count { (digits.length - it) % 3 == 0 }
                return (offset + seps).coerceIn(0, out.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val sepsBefore = out.take(offset).count { it == sep }
                return (offset - sepsBefore).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyField(
    digits: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    suggestions: Boolean = true,
    max: Long = Long.MAX_VALUE,
) {
    Column(modifier) {
        OutlinedTextField(
            value = digits,
            onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(12).trimStart('0')) },
            label = { Text(label) },
            suffix = { Text("₫") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            visualTransformation = ThousandsTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        val list = if (suggestions) Format.amountSuggestions(digits.toLongOrNull() ?: 0).filter { it <= max } else emptyList()
        if (list.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                list.forEach { amount ->
                    SuggestionChip(onClick = { onChange(amount.toString()) }, label = { Text(Format.number(amount)) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(millis: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier, error: String? = null) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1.4f).height(56.dp), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(Format.date(millis))
            }
            OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f).height(56.dp), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(Format.time(millis))
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }

    if (showDate) {
        // DatePicker works in UTC midnight: convert the local calendar day to/from it.
        val local = Calendar.getInstance().apply { timeInMillis = millis }
        val utcInitial = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val state = rememberDatePickerState(initialSelectedDateMillis = utcInitial)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                        val result = Calendar.getInstance().apply {
                            timeInMillis = millis
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }
                        onChange(result.timeInMillis)
                    }
                    showDate = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val state = rememberTimePickerState(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(Calendar.getInstance().apply {
                        timeInMillis = millis
                        set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute); set(Calendar.SECOND, 0)
                    }.timeInMillis)
                    showTime = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.common_cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: String, onSelect: (String) -> Unit, colors: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { hex ->
            val on = hex.equals(selected, ignoreCase = true)
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(hex.toColor())
                    .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

// ---------------------------------------------------------------- charts

data class Slice(val label: String, val value: Long, val color: Color)

@Composable
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier, size: Dp = 180.dp, thickness: Dp = 26.dp, center: @Composable BoxScope.() -> Unit = {}) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            val total = slices.sumOf { it.value }.toFloat()
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            } else {
                val visible = slices.filter { it.value > 0 }
                val gap = if (visible.size > 1) 2.5f else 0f
                var start = -90f
                visible.forEach { s ->
                    val sweep = s.value / total * 360f
                    drawArc(s.color, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += sweep
                }
            }
        }
        center()
    }
}

@Composable
fun LegendRow(color: Color, name: String, value: String, modifier: Modifier = Modifier, extra: String? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (extra != null) Text(extra, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

/** Dot + name on the left, amount + % on the right, a thin progress bar underneath. */
@Composable
fun ProgressRow(name: String, color: Color, valueText: String, pct: Int, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) } else {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(8.dp))
            }
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$valueText · $pct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

data class Bucket(val label: String, val income: Long, val expense: Long)

/** Grouped income / expense bars, scrolls horizontally when there are many buckets. */
@Composable
fun TrendBars(buckets: List<Bucket>, modifier: Modifier = Modifier, showIncome: Boolean = true, barAreaHeight: Dp = 130.dp) {
    val max = buckets.maxOfOrNull { maxOf(if (showIncome) it.income else 0L, it.expense) }?.coerceAtLeast(1L) ?: 1L
    val income = LocalIncomeColors.current.income
    val expense = MaterialTheme.colorScheme.error
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        buckets.forEach { b ->
            Column(Modifier.width(if (showIncome) 38.dp else 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(barAreaHeight), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (showIncome) Box(Modifier.width(14.dp).fillMaxHeight((b.income / max.toFloat()).coerceIn(0.01f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (b.income > 0) income else Color.Transparent))
                    Box(Modifier.width(if (showIncome) 14.dp else 18.dp).fillMaxHeight((b.expense / max.toFloat()).coerceIn(0.01f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (b.expense > 0) expense else Color.Transparent))
                }
                Spacer(Modifier.height(4.dp))
                Text(b.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- period navigation

@Composable
fun PeriodNavigator(label: String, isCurrent: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onPrev, enabled = enabled) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.stats_prev)) }
            Column(Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(enabled = enabled && !isCurrent, onClick = onToday).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = TextAlign.Center)
                if (!isCurrent && enabled) Text(stringResource(R.string.stats_back_to_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            FilledTonalIconButton(onClick = onNext, enabled = enabled && !isCurrent) { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.stats_next)) }
        }
    }
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, container: Color, onContainer: Color, modifier: Modifier = Modifier, hint: String? = null, valueColor: Color = onContainer) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = container, contentColor = onContainer) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = valueColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Stand-alone date picker dialog (DatePicker works in UTC midnight, so convert the local day). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePicker(millis: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    val utcInitial = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
    val state = rememberDatePickerState(initialSelectedDateMillis = utcInitial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { utc ->
                    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                    onPick(Calendar.getInstance().apply { clear(); set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH)) }.timeInMillis)
                }
                onDismiss()
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val names = stringArrayResource(R.array.emoji_group_names)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = selected, onValueChange = { onSelect(it.take(8)) }, singleLine = true,
            label = { Text(stringResource(R.string.emoji_placeholder)) }, modifier = Modifier.fillMaxWidth(),
        )
        EMOJI_GROUPS.forEachIndexed { i, (_, list) ->
            Text(names.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            FlowRow {
                list.forEach { e ->
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(if (e == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelect(e) },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 22.sp) }
                }
            }
        }
    }
}
