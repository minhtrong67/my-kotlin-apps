package com.expenser.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
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
fun LoadingBox(modifier: Modifier = Modifier) = DashboardSkeleton(modifier)

// ---------------------------------------------------------------- skeleton loading

/** Animated shimmer sweep used as the background of skeleton placeholders. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x = transition.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "shimmerX")
    drawBehind {
        val w = size.width.coerceAtLeast(1f)
        drawRect(Brush.horizontalGradient(listOf(base, hi, base), startX = x.value * w, endX = x.value * w + w * 0.6f))
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).shimmer())
}

@Composable
fun SkeletonRow(modifier: Modifier = Modifier, leadingSize: Dp = 44.dp) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        SkeletonBox(Modifier.size(leadingSize), CircleShape)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.55f).height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.3f).height(10.dp))
        }
        SkeletonBox(Modifier.width(64.dp).height(14.dp))
    }
}

@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 6) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { repeat(rows) { SkeletonRow() } }
}

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(Modifier.weight(1f).height(112.dp), RoundedCornerShape(22.dp))
                SkeletonBox(Modifier.weight(1f).height(112.dp), RoundedCornerShape(22.dp))
            }
        }
        SkeletonBox(Modifier.fillMaxWidth().height(260.dp), RoundedCornerShape(24.dp))
        ListSkeleton(rows = 3)
    }
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

/** Web-style floating action button: a rounded-square primary container with a plus. */
@Composable
fun ScreenFab(onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Add) {
    FloatingActionButton(onClick = onClick, modifier = modifier.padding(16.dp), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(18.dp)) {
        Icon(icon, null)
    }
}

data class Seg(val id: String, val label: String, val icon: ImageVector? = null, val container: Color? = null, val content: Color? = null)

/** Pill-shaped two/three-way switch used across the web app (selected segment shows a check mark). */
@Composable
fun PillSegmented(options: List<Seg>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        options.forEach { o ->
            val on = o.id == selected
            val bg by animateColorAsState(if (on) (o.container ?: MaterialTheme.colorScheme.primary) else Color.Transparent, tween(150), label = "segBg")
            val fg by animateColorAsState(if (on) (o.content ?: MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurfaceVariant, tween(150), label = "segFg")
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
    Surface(modifier.fillMaxWidth().animateContentSize(tween(200)), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
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
                val rot = if (expanded) 180f else 0f
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.rotate(rot), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(160))) {
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

/**
 * Forms are drawn by [FormHost] inside the activity (not in a separate dialog window) so system-bar and keyboard insets
 * are applied correctly and the bottom action bar is never hidden.
 */
class FormEntry internal constructor(val id: Any, val content: MutableState<@Composable () -> Unit>) {
    /** Starts hidden and flips to visible right away, which plays the enter animation; flipping back plays the exit. */
    val visible = MutableTransitionState(false).apply { targetState = true }
}

class FormHostState {
    internal val entries = mutableStateListOf<FormEntry>()
}

val LocalFormHost = compositionLocalOf<FormHostState?> { null }

@Composable
fun FormHost(state: FormHostState) {
    state.entries.toList().forEach { entry ->
        key(entry.id) {
            AnimatedVisibility(
                visibleState = entry.visible, modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(180)),
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { entry.content.value() }
                }
            }
            LaunchedEffect(entry.visible.currentState, entry.visible.targetState) {
                if (!entry.visible.targetState && !entry.visible.currentState) state.entries.remove(entry)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(title: String, confirmLabel: String, onClose: () -> Unit, onConfirm: () -> Unit, confirmEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val host = LocalFormHost.current
    val body: @Composable () -> Unit = {
        BackHandler(onBack = onClose)
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
                Spacer(Modifier.height(16.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose, Modifier.weight(1f)) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1.6f).height(48.dp)) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(confirmLabel)
                }
            }
        }
    }
    if (host == null) return
    val id = remember { Any() }
    val holder = remember { mutableStateOf(body) }
    SideEffect { holder.value = body }
    DisposableEffect(id) {
        val entry = FormEntry(id, holder)
        host.entries.add(entry)
        onDispose { entry.visible.targetState = false }
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
            suffix = { Text("₫", style = MaterialTheme.typography.titleLarge) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            singleLine = true, isError = error != null,
            supportingText = error?.let { { Text(it) } },
            visualTransformation = ThousandsTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        val list = if (suggestions) Format.amountSuggestions(digits.toLongOrNull() ?: 0).filter { it <= max } else emptyList()
        if (list.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { amount ->
                    Surface(onClick = { onChange(amount.toString()) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(Format.number(amount), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

/** Read-only outlined field that opens a picker when tapped (category, wallet, date...). */
@Composable
fun SelectField(label: String, valueText: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, trailing: ImageVector = Icons.Rounded.KeyboardArrowDown, error: String? = null, textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default) {
    Box(modifier) {
        OutlinedTextField(
            value = valueText, onValueChange = {}, readOnly = true, label = { Text(label) },
            leadingIcon = leading, trailingIcon = { Icon(trailing, null) },
            isError = error != null, supportingText = error?.let { { Text(it) } },
            singleLine = true, textStyle = LocalTextStyle.current.merge(textStyle),
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().padding(bottom = if (error != null) 20.dp else 0.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick))
    }
}

/** Select field with a list of (id, label) options; id "" means "all / none". */
@Composable
fun OptionSelect(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    SelectField(label, options.firstOrNull { it.first == selected }?.second ?: "", { open = true }, modifier)
    if (open) ChoiceSheet(label, options, selected, { it.first }, { onSelect(it.first) }, { open = false }) { Text(it.second, style = MaterialTheme.typography.bodyLarge) }
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
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier, size: Dp = 220.dp, thickness: Dp = 38.dp, showLabels: Boolean = true, center: @Composable BoxScope.() -> Unit = {}) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val outsideColor = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
    // Plays once per screen: the flag survives the item leaving and re-entering the lazy list while scrolling.
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) { if (!played) { progress.animateTo(1f, tween(600)); played = true } }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val total = slices.sumOf { it.value }.toFloat()
            val visible = slices.filter { it.value > 0 }
            // Room around the ring for the percentage of slices too thin to hold a label.
            val pad = if (showLabels && total > 0f && visible.any { it.value * 100f / total < 7f }) 22.dp.toPx() else 0f
            val arcSize = Size(this.size.width - stroke - 2 * pad, this.size.height - stroke - 2 * pad)
            val topLeft = Offset(stroke / 2 + pad, stroke / 2 + pad)
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            } else {
                val gap = if (visible.size > 1) 3f else 0f
                var start = -90f
                visible.forEach { s ->
                    val full = s.value / total * 360f
                    drawArc(s.color, start + gap / 2, ((full - gap) * progress.value).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += full
                }
                if (showLabels && progress.value > 0.6f) {
                    val alpha = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    val cx = this.size.width / 2; val cy = this.size.height / 2
                    val r = arcSize.width / 2
                    var a0 = -90f
                    visible.forEach { s ->
                        val full = s.value / total * 360f
                        val pct = s.value * 100f / total
                        val mid = Math.toRadians((a0 + full / 2).toDouble())
                        a0 += full
                        if (pct < 1f) return@forEach
                        val text = "${Math.round(pct)}%"
                        val layout = measurer.measure(text, labelStyle)
                        val inside = pct >= 7f
                        val rr = if (inside) r else r + stroke / 2 + 11.dp.toPx()
                        val px = cx + (kotlin.math.cos(mid) * rr).toFloat()
                        val py = cy + (kotlin.math.sin(mid) * rr).toFloat()
                        val color = if (!inside) outsideColor else if (s.color.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White
                        drawText(layout, color = color, topLeft = Offset(px - layout.size.width / 2f, py - layout.size.height / 2f), alpha = alpha)
                    }
                }
            }
        }
        center()
    }
}

/** Donut + a complete legend: colour, name, amount and percentage for every slice. */
@Composable
fun DonutWithLegend(
    slices: List<Slice>, modifier: Modifier = Modifier, size: Dp = 220.dp, formatValue: (Long) -> String = { Format.money(it) },
    centerTitle: String? = null, centerValue: String? = null, onSliceClick: ((Int) -> Unit)? = null,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        DonutChart(slices, size = size) {
            if (centerValue != null) Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (centerTitle != null) Text(centerTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(centerValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(12.dp))
        slices.forEachIndexed { i, sl ->
            val pct = Math.round(sl.value * 100.0 / total).toInt()
            LegendRow(sl.color, sl.label, formatValue(sl.value), extra = "$pct%", modifier = if (onSliceClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSliceClick(i) } else Modifier)
        }
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
    val frac = (pct / 100f).coerceIn(0f, 1f)
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


// ---------------------------------------------------------------- images

/** Network image: shimmer skeleton while loading, cross-fade in, placeholder icon when the URL is empty, broken or unreachable. */
@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, fallbackIcon: ImageVector = Icons.Rounded.Image, contentScale: ContentScale = ContentScale.Crop) {
    val placeholder: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(fallbackIcon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), modifier = Modifier.fillMaxSize(0.4f))
        }
    }
    if (url.isNullOrBlank()) { Box(modifier) { placeholder() }; return }
    var loaded by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    val context = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(context).data(url).crossfade(180).build() }
    Box(modifier) {
        if (failed) placeholder()
        else {
            if (!loaded) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh).shimmer())
            AsyncImage(
                model = request, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = contentScale,
                onState = { st -> when (st) { is AsyncImagePainter.State.Success -> loaded = true; is AsyncImagePainter.State.Error -> failed = true; else -> {} } },
            )
        }
    }
}

data class LightboxItem(val url: String, val title: String)

/** Full-screen photo viewer: swipe or use the arrows for previous / next, double-tap or pinch to zoom. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageLightbox(items: List<LightboxItem>, startIndex: Int, onDismiss: () -> Unit) {
    if (items.isEmpty()) return
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, key = { items[it].url + it }) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                LaunchedEffect(pager.currentPage) { if (pager.currentPage != page) { scale = 1f; offset = Offset.Zero } }
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offset = if (scale > 1f) offset + pan else Offset.Zero
                            zoomed = scale > 1f
                        }
                    }.pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f; zoomed = scale > 1f }, onTap = { })
                    }.graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                    contentAlignment = Alignment.Center,
                ) { RemoteImage(items[page].url, Modifier.fillMaxSize(), Icons.Rounded.BrokenImage, ContentScale.Fit) }
            }
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close), tint = Color.White) }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(items[pager.currentPage].title, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (items.size > 1) Text("${pager.currentPage + 1} / ${items.size}", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (items.size > 1) {
                if (pager.currentPage > 0) FilledTonalIconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.stats_prev)) }
                if (pager.currentPage < items.lastIndex) FilledTonalIconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)) { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.stats_next)) }
            }
        }
    }
}

// ---------------------------------------------------------------- drag & drop reordering

/**
 * Drag-to-reorder state for a LazyColumn whose items are keyed by id (String). The order is kept locally while dragging and
 * while the write is in flight, then [onCommit] receives the final id order once, when the finger lifts.
 */
class ReorderState internal constructor(private val lazy: LazyListState, private val scope: CoroutineScope, source: List<String>) {
    var ids by mutableStateOf(source)
    var draggingKey by mutableStateOf<String?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    var pending by mutableStateOf<List<String>?>(null)
    internal var onCommit: (List<String>) -> Unit = {}
    private var expectedOffset: Int? = null

    /** Ids in display order: the local order while dragging / waiting for the write, otherwise the source order. */
    fun display(source: List<String>): List<String> {
        val local = if (draggingKey != null) ids else pending
        if (local == null) return source
        val set = source.toSet()
        val kept = local.filter { it in set }
        return kept + source.filter { it !in kept }
    }

    private var startIds: List<String> = emptyList()

    fun start(key: String) { startIds = ids; draggingKey = key; dragOffset = 0f; expectedOffset = null }

    fun drag(delta: Float) {
        val key = draggingKey ?: return
        dragOffset += delta
        val infos = lazy.layoutInfo.visibleItemsInfo
        val cur = infos.firstOrNull { (it.key as? String) == key } ?: return
        expectedOffset?.let { if (cur.offset == it) expectedOffset = null else return }
        val center = cur.offset + cur.size / 2f + dragOffset
        val target = infos.firstOrNull { (it.key as? String).let { k -> k != null && k != key && k in ids } && center >= it.offset && center <= it.offset + it.size }
        if (target != null) {
            val from = ids.indexOf(key); val to = ids.indexOf(target.key as String)
            if (from >= 0 && to >= 0) {
                val newOffset = if (target.offset > cur.offset) cur.offset + target.size else target.offset
                ids = ids.toMutableList().apply { add(to, removeAt(from)) }
                dragOffset += (cur.offset - newOffset)
                expectedOffset = newOffset
            }
            return
        }
        // Auto-scroll when the dragged card nears the top / bottom edge.
        val top = cur.offset + dragOffset
        val bottom = top + cur.size
        val edge = 72f
        val dy = when {
            top < lazy.layoutInfo.viewportStartOffset + edge -> -18f
            bottom > lazy.layoutInfo.viewportEndOffset - edge -> 18f
            else -> 0f
        }
        if (dy != 0f) scope.launch { dragOffset += lazy.scrollBy(dy) }
    }

    fun end() {
        val had = draggingKey != null
        val finalIds = ids
        draggingKey = null; dragOffset = 0f; expectedOffset = null
        if (had && finalIds != startIds) { pending = finalIds; onCommit(finalIds) }
    }
}

@Composable
fun rememberReorder(source: List<String>, lazy: LazyListState, onCommit: (List<String>) -> Unit): ReorderState {
    val scope = rememberCoroutineScope()
    val st = remember(lazy) { ReorderState(lazy, scope, source) }
    st.onCommit = onCommit
    LaunchedEffect(source) {
        if (st.draggingKey == null) {
            if (st.pending == source) st.pending = null
            if (st.pending == null) st.ids = source
        }
    }
    LaunchedEffect(st.pending) { if (st.pending != null) { delay(2500); st.pending = null } }
    return st
}

/** Item modifier: the dragged card floats above the others and follows the finger; the rest animate into their new slots. */
@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.reorderable(state: ReorderState, key: String): Modifier =
    if (state.draggingKey == key) Modifier.zIndex(1f).graphicsLayer { translationY = state.dragOffset; scaleX = 1.03f; scaleY = 1.03f; alpha = 0.96f }
    else Modifier.animateItem()

/** Grip icon: press and drag it to move the card. */
@Composable
fun DragHandle(state: ReorderState, key: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).pointerInput(key) {
            detectDragGestures(
                onDragStart = { state.start(key) },
                onDragEnd = { state.end() },
                onDragCancel = { state.end() },
            ) { change, amount -> change.consume(); state.drag(amount.y) }
        },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.DragHandle, stringResource(R.string.drag_to_reorder), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

// ---------------------------------------------------------------- misc helpers

/** Copies [text] to the clipboard; the icon turns into a check mark for two seconds. */
@Composable
fun CopyButton(text: String, modifier: Modifier = Modifier, onCopied: () -> Unit = {}) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(2000); copied = false } }
    FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(text)); copied = true; onCopied() }, modifier = modifier) {
        Icon(if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy, stringResource(R.string.cafe_copy_map))
    }
}

/** Horizontal swipe → previous / next (e.g. to move between tabs). Children that scroll or swipe horizontally win first. */
fun Modifier.swipeSwitch(onPrev: () -> Unit, onNext: () -> Unit): Modifier = composed {
    val prev by rememberUpdatedState(onPrev)
    val next by rememberUpdatedState(onNext)
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    pointerInput(Unit) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragCancel = { total = 0f },
            onDragEnd = { if (total > threshold) prev() else if (total < -threshold) next(); total = 0f },
        ) { _, d -> total += d }
    }
}

/** Date picker field with an optional value (filters): compact, one line; the trailing icon becomes a clear button when set. */
@Composable
fun DateSelect(label: String, millis: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectField(
            label, millis?.let { Format.date(it) } ?: "", { open = true }, Modifier.fillMaxWidth(),
            trailing = if (millis != null) Icons.Rounded.Close else Icons.Rounded.CalendarMonth,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        if (millis != null) Box(Modifier.align(Alignment.CenterEnd).size(56.dp).clip(CircleShape).clickable { onChange(null) })
    }
    if (open) SimpleDatePicker(millis ?: System.currentTimeMillis(), { onChange(it) }, { open = false })
}
