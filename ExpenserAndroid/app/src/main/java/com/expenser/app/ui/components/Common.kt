package com.expenser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// ---------------------------------------------------------------- motion helpers

/** Press feedback: the element shrinks slightly with a spring, then clicks. */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press")
    this.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(source, LocalIndication.current, enabled = enabled, onClick = onClick)
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
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun SectionCard(title: String?, modifier: Modifier = Modifier, subtitle: String? = null, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); action?.invoke() }
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(14.dp))
            }
            content()
        }
    }
}

@Composable
fun CategoryAvatar(emoji: String, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.46f).sp)
    }
}

@Composable
fun IconAvatar(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun ConfirmDialog(text: String, confirmLabel: String = stringResource(R.string.common_delete), onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
        title = { Text(stringResource(R.string.common_confirm)) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Web-style floating action button: a rounded-square primary container with a plus. Pops in when the screen opens. */
@Composable
fun ScreenFab(onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Add) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown, modifier = modifier.padding(16.dp), enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(), exit = scaleOut() + fadeOut()) {
        FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(18.dp)) {
            Icon(icon, null)
        }
    }
}

data class Seg(val id: String, val label: String, val icon: ImageVector? = null, val container: Color? = null, val content: Color? = null)

/** Pill-shaped two/three-way switch used across the web app (selected segment shows a check mark). */
@Composable
fun PillSegmented(options: List<Seg>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        options.forEach { o ->
            val on = o.id == selected
            val bg by animateColorAsState(if (on) (o.container ?: MaterialTheme.colorScheme.primary) else Color.Transparent, tween(220), label = "segBg")
            val fg by animateColorAsState(if (on) (o.content ?: MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurfaceVariant, tween(220), label = "segFg")
            Row(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(bg).clickable { onSelect(o.id) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = if (on) Icons.Rounded.Check else o.icon
                if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
                Text(o.label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
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

/** Header card that expands in place (Filters, Settings groups). */
@Composable
fun AccordionCard(
    title: String, icon: ImageVector, expanded: Boolean, onToggle: () -> Unit,
    modifier: Modifier = Modifier, subtitle: String? = null, badge: Int = 0, content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Row(Modifier.clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (badge > 0) { Badge { Text("$badge") }; Spacer(Modifier.width(8.dp)) }
                val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "chev")
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.rotate(rot), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded, enter = expandVertically(tween(250)) + fadeIn(tween(250)), exit = shrinkVertically(tween(200)) + fadeOut(tween(150))) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
            }
        }
    }
}

/** Swipe right to edit, left to delete (the card snaps back; the action runs through the callbacks). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(onEdit: (() -> Unit)?, onDelete: () -> Unit, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp), content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.StartToEnd -> { onEdit?.invoke(); false }
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state, modifier = modifier,
        enableDismissFromStartToEnd = onEdit != null,
        backgroundContent = {
            val dir = state.dismissDirection
            val bg by animateColorAsState(
                when (dir) { SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer; SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer; else -> Color.Transparent },
                label = "swipeBg",
            )
            Box(Modifier.fillMaxSize().clip(shape).background(bg).padding(horizontal = 24.dp), contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                when (dir) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    SwipeToDismissBoxValue.EndToStart -> Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    else -> {}
                }
            }
        },
    ) { content() }
}

// ---------------------------------------------------------------- full-screen form

/** Multi-field forms open as a full-screen sheet: close button + title on top, Cancel / confirm at the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(title: String, confirmLabel: String, onClose: () -> Unit, onConfirm: () -> Unit, confirmEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    content()
                    Spacer(Modifier.height(16.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose, Modifier.weight(1f)) { Text(stringResource(R.string.common_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1.6f).height(48.dp)) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(confirmLabel)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- inputs

/** Shows digits grouped by thousands ("1.500.000") while the state stays plain digits. */
private class ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val sep = if (Format.lang == "en") ',' else '.'
        val sb = StringBuilder()
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
                val seps = out.take(offset).count { it == sep }
                return (offset - seps).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyField(digits: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, error: String? = null, suggestions: Boolean = true, max: Long = Long.MAX_VALUE) {
    Column(modifier) {
        OutlinedTextField(
            value = digits,
            onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(12).trimStart('0')) },
            label = { Text("$label *") },
            suffix = { Text("₫") },
            singleLine = true, isError = error != null,
            supportingText = error?.let { { Text(it) } },
            visualTransformation = ThousandsTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        val list = if (suggestions) Format.amountSuggestions(digits.toLongOrNull() ?: 0).filter { it <= max } else emptyList()
        AnimatedVisibility(list.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                list.forEach { amount -> SuggestionChip(onClick = { onChange(amount.toString()) }, label = { Text(Format.number(amount)) }) }
            }
        }
    }
}

/** Read-only outlined field that opens a picker when tapped (category, wallet, date...). */
@Composable
fun SelectField(label: String, valueText: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, trailing: ImageVector = Icons.Rounded.KeyboardArrowDown, error: String? = null) {
    Box(modifier) {
        OutlinedTextField(
            value = valueText, onValueChange = {}, readOnly = true, label = { Text(label) },
            leadingIcon = leading, trailingIcon = { Icon(trailing, null) },
            isError = error != null, supportingText = error?.let { { Text(it) } },
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().padding(bottom = if (error != null) 20.dp else 0.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceSheet(title: String, items: List<T>, selectedId: String?, id: (T) -> String, onSelect: (T) -> Unit, onDismiss: () -> Unit, row: @Composable (T) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(Modifier.heightIn(max = 460.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(items, key = { id(it) }) { item ->
                val sel = id(item) == selectedId
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(item); onDismiss() }.background(if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { row(item) }
                    if (sel) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** One field showing "dd/MM/yyyy HH:mm"; tap → date picker, then time picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(millis: Long, onChange: (Long) -> Unit, label: String, modifier: Modifier = Modifier, error: String? = null) {
    var step by remember { mutableIntStateOf(0) } // 0 closed, 1 date, 2 time
    var pending by remember { mutableLongStateOf(millis) }
    SelectField(label, "${Format.date(millis)}  ${Format.time(millis)}", { pending = millis; step = 1 }, modifier, leading = { Icon(Icons.Rounded.Event, null) }, trailing = Icons.Rounded.CalendarMonth, error = error)
    if (step == 1) SimpleDatePicker(pending, { pending = it; }, { step = 0 }, onConfirmed = { step = 2 })
    if (step == 2) {
        val c = Calendar.getInstance().apply { timeInMillis = pending }
        val state = rememberTimePickerState(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    onChange(Calendar.getInstance().apply { timeInMillis = pending; set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
                    step = 0
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.common_cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

/** Stand-alone date picker (DatePicker works in UTC midnight, so the local calendar day is converted). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePicker(millis: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit, onConfirmed: (() -> Unit)? = null) {
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
                    onPick(Calendar.getInstance().apply {
                        timeInMillis = millis
                        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                    }.timeInMillis)
                }
                if (onConfirmed != null) onConfirmed() else onDismiss()
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: String, onSelect: (String) -> Unit, colors: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { hex ->
            val on = hex.equals(selected, ignoreCase = true)
            val ring by animateDpAsState(if (on) 3.dp else 0.dp, label = "ring")
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(hex.toColor())
                    .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val names = stringArrayResource(R.array.emoji_group_names)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest), contentAlignment = Alignment.Center) { Text(selected.ifBlank { "🏷️" }, fontSize = 24.sp) }
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(value = selected, onValueChange = { onSelect(it.take(8)) }, singleLine = true, placeholder = { Text(stringResource(R.string.emoji_placeholder)) }, shape = CircleShape, modifier = Modifier.weight(1f))
        }
        EMOJI_GROUPS.forEachIndexed { i, (_, list) ->
            Text(names.getOrElse(i) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            FlowRow {
                list.forEach { e ->
                    val on = e == selected
                    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "emojiBg")
                    Box(Modifier.size(44.dp).clip(CircleShape).background(bg).clickable { onSelect(e) }, contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- charts

data class Slice(val label: String, val value: Long, val color: Color)

@Composable
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier, size: Dp = 200.dp, thickness: Dp = 28.dp, center: @Composable BoxScope.() -> Unit = {}) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(800)) }
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
                val gap = if (visible.size > 1) 3f else 0f
                var start = -90f
                visible.forEach { s ->
                    val sweep = s.value / total * 360f * progress.value
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
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (extra != null) Text("  ($extra)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProgressRow(name: String, color: Color, valueText: String, pct: Int, modifier: Modifier = Modifier) {
    val frac by animateFloatAsState((pct / 100f).coerceIn(0f, 1f), tween(600), label = "bar")
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(8.dp))
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$valueText · $pct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

data class Bucket(val label: String, val income: Long, val expense: Long)

/** Grouped income / expense bars that grow in; scrolls horizontally when there are many buckets. */
@Composable
fun TrendBars(buckets: List<Bucket>, modifier: Modifier = Modifier, barAreaHeight: Dp = 140.dp) {
    val max = buckets.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
    val income = LocalIncomeColors.current.income
    val expense = MaterialTheme.colorScheme.error
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        buckets.forEach { b ->
            val hi by animateFloatAsState(b.income / max.toFloat(), tween(600), label = "hi")
            val he by animateFloatAsState(b.expense / max.toFloat(), tween(600), label = "he")
            Column(Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(barAreaHeight), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.width(14.dp).fillMaxHeight(hi.coerceIn(0.005f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (b.income > 0) income else Color.Transparent))
                    Box(Modifier.width(14.dp).fillMaxHeight(he.coerceIn(0.005f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (b.expense > 0) expense else Color.Transparent))
                }
                Spacer(Modifier.height(4.dp))
                Text(b.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- period navigation + tiles

@Composable
fun PeriodNavigator(label: String, isCurrent: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onPrev, enabled = enabled) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.stats_prev)) }
            Column(Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(enabled = enabled && !isCurrent, onClick = onToday).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (!isCurrent && enabled) Text(stringResource(R.string.stats_back_to_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            FilledTonalIconButton(onClick = onNext, enabled = enabled && !isCurrent) { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.stats_next)) }
        }
    }
}

/** Tonal summary tile: rounded-square icon, optional badge on the right, label, big value. Bordered like the web cards. */
@Composable
fun StatTile(
    icon: ImageVector, label: String, value: String, container: Color, onContainer: Color, modifier: Modifier = Modifier,
    hint: String? = null, valueColor: Color = onContainer, badge: (@Composable () -> Unit)? = null, compact: Boolean = false,
) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = container, contentColor = onContainer, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (compact) 28.dp else 44.dp).clip(RoundedCornerShape(if (compact) 8.dp else 14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(if (compact) 16.dp else 22.dp))
                }
                Spacer(Modifier.weight(1f))
                badge?.invoke()
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.85f), maxLines = 2)
            Text(value, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 2)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.85f))
        }
    }
}
