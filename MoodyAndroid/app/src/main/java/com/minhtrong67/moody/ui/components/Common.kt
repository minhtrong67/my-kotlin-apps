@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.MoodDef
import com.minhtrong67.moody.ui.theme.LocalIsDark
import com.minhtrong67.moody.ui.theme.PaletteDef
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Web `card-filled`: tinted lowest surface in light theme, highest container in dark theme. Clickable cards squish on press. */
@Composable
fun MoodyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalIsDark.current
    val cs = MaterialTheme.colorScheme
    val bg = containerColor ?: if (dark) cs.surfaceContainerHighest else lerp(cs.surfaceContainerLowest, cs.primary, 0.03f)
    val border = if (dark || containerColor != null) null else BorderStroke(1.dp, cs.primary.copy(alpha = 0.16f))
    val shape = RoundedCornerShape(28.dp)
    if (onClick != null) {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale = animateFloatAsState(if (pressed) 0.975f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
        // scale is read inside the layer lambda => animates on the draw phase only (no recomposition per frame)
        Surface(
            onClick = onClick, interactionSource = src, shape = shape, color = bg, border = border,
            modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        ) { Column(Modifier.padding(contentPadding), content = content) }
    } else {
        Surface(modifier = modifier, shape = shape, color = bg, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun MoodBubble(mood: MoodDef?, size: Dp = 48.dp, fontSize: Int = 24, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background((mood?.color ?: Color.Gray).copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) { Text(mood?.emoji ?: "", fontSize = fontSize.sp) }
}

/** Google avatar, or the first letter on a colored disc when there is no photo. */
@Composable
fun UserAvatar(user: AppUser?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val name = user?.displayName ?: user?.email ?: "?"
    if (user?.photoUrl != null) {
        AsyncImage(user.photoUrl, null, modifier.size(size).clip(CircleShape))
    } else {
        Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = (size.value * 0.45f).sp)
        }
    }
}

/** Dialog / sheet action: always label + icon + background colour (per the app's design rules). */
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (tonal) {
        val colors = if (danger) ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) else ButtonDefaults.filledTonalButtonColors()
        FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) { content() }
    } else {
        val colors = if (danger) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
        ) else ButtonDefaults.buttonColors()
        Button(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) { content() }
    }
}

/**
 * App dialog: roomy card (28dp corners, 24dp padding, near full width) with a centred header and TWO equal-width,
 * full-height action buttons side by side (label + icon + background colour).
 */
@Composable
fun MoodyDialog(
    onDismissRequest: () -> Unit,
    title: String,
    dismissLabel: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    headerIcon: ImageVector? = null,
    danger: Boolean = false,
    confirmIcon: ImageVector = if (danger) Icons.Rounded.Delete else Icons.Rounded.Check,
    confirmEnabled: Boolean = true,
    onDismiss: () -> Unit = onDismissRequest,
    centered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth().widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
                if (headerIcon != null) {
                    Icon(headerIcon, null, Modifier.size(32.dp), tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                }
                Text(
                    title, style = MaterialTheme.typography.headlineSmall,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                content()
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(dismissLabel, Icons.Rounded.Close, onDismiss, tonal = true, modifier = Modifier.weight(1f).height(52.dp))
                    ActionButton(confirmLabel, confirmIcon, onConfirm, danger = danger, enabled = confirmEnabled, modifier = Modifier.weight(1f).height(52.dp))
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    confirmIcon: ImageVector = if (danger) Icons.Rounded.Delete else Icons.Rounded.Check,
    dismissLabel: String = stringResource(R.string.common_cancel),
    headerIcon: ImageVector? = null,
) {
    MoodyDialog(
        onDismissRequest = onDismiss, title = title, dismissLabel = dismissLabel, confirmLabel = confirmLabel,
        onConfirm = onConfirm, headerIcon = headerIcon, danger = danger, confirmIcon = confirmIcon,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Material 3 has no TimePickerDialog, so wrap TimePicker in our dialog. */
@Composable
fun TimePickerDialog(initialHour: Int, initialMinute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val state = rememberTimePickerState(initialHour, initialMinute, is24Hour = DateFormat.is24HourFormat(ctx))
    MoodyDialog(
        onDismissRequest = onDismiss, title = stringResource(R.string.checkin_pickTime),
        dismissLabel = stringResource(R.string.common_cancel), confirmLabel = stringResource(R.string.common_ok),
        onConfirm = { onConfirm(state.hour, state.minute) },
    ) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } }
}

/**
 * Web `SegmentedButton`: pill group with small gaps. The selected item morphs into a full pill and fills with
 * the primary color; the others keep tighter inner corners.
 */
@Composable
fun SegmentedTabs(
    options: List<Pair<String, String>>,   // key to label
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    icons: Map<String, ImageVector> = emptyMap(),
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, (key, label) ->
            val sel = key == selected
            val cs = MaterialTheme.colorScheme
            val bg by animateColorAsState(if (sel) cs.primary else cs.secondaryContainer, tween(220), label = "segBg")
            val fg by animateColorAsState(if (sel) cs.onPrimary else cs.onSecondaryContainer, tween(220), label = "segFg")
            val outer = 20.dp
            val inner = 6.dp
            val tl by animateDpAsState(if (sel || i == 0) outer else inner, spring(stiffness = Spring.StiffnessMediumLow), label = "tl")
            val tr by animateDpAsState(if (sel || i == options.lastIndex) outer else inner, spring(stiffness = Spring.StiffnessMediumLow), label = "tr")
            Row(
                Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(topStart = tl, bottomStart = tl, topEnd = tr, bottomEnd = tr))
                    .background(bg)
                    .clickable { if (!sel) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(key) } },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons[key]?.let { Icon(it, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(6.dp)) }
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Select with the label above the field (as on the web). */
@Composable
fun DropdownField(
    label: String,
    options: List<Pair<String, String>>,   // key to display
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selectedKey }?.second ?: ""
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = current, onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (k, v) ->
                    DropdownMenuItem(text = { Text(v) }, onClick = { onSelect(k); expanded = false })
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 8.dp))
}

/** Small group heading used in Settings (primary, like the web). */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(start = 4.dp, bottom = 8.dp))
}

/** Friendly empty state: a big emoji over a short line of text. */
@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier, emoji: String? = "🌱") {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (emoji != null) Text(emoji, fontSize = 40.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Static activity chip for cards: fills its grid cell (see [Grid3]) with emoji + label centred. */
@Composable
fun StaticChip(emoji: String, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(17.dp)).background(cs.surfaceContainer).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** `grid-cols-3`: equal-width cells, last row padded so items never stretch. */
@Composable
fun <T> Grid3(items: List<T>, modifier: Modifier = Modifier, spacing: Dp = 8.dp, cell: @Composable (T) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Mood picker bubble: springs up when selected, gets a ring + tinted fill. Motion runs in graphicsLayer (no per-frame
 * recomposition) and the slot is bigger than the bubble so the scaled ring is never clipped.
 */
@Composable
fun MoodPickBubble(mood: MoodDef, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scale = animateFloatAsState(if (selected) 1.1f else 1f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "moodScale")
    val bg by animateColorAsState(if (selected) mood.color.copy(alpha = 0.38f) else MaterialTheme.colorScheme.surfaceContainerHigh, tween(200), label = "moodBg")
    val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.outline else Color.Transparent, tween(200), label = "moodRing")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {          // 64dp slot, 54dp bubble => room to grow
            Box(
                Modifier.size(54.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(CircleShape).background(bg).border(2.dp, ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(mood.emoji, fontSize = 26.sp) }
        }
        Text(
            mood.label, style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/**
 * Material 3 Expressive-style wavy progress: the bar fills in (staggered by [index]); the filled part is a wave,
 * the remaining track is flat with an end dot. Only the fill animates, so a screen full of bars costs nothing at rest.
 */
@Composable
fun WavyProgress(progress: Float, color: Color, modifier: Modifier = Modifier, index: Int = 0) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        delay(index * 80L)
        anim.animateTo(progress.coerceIn(0f, 1f), tween(750, easing = FastOutSlowInEasing))
    }
    val track = color.copy(alpha = 0.28f)
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val stroke = 4.dp.toPx()
        val amp = 2.6.dp.toPx()
        val wl = 20.dp.toPx()
        val cy = size.height / 2f
        val endX = ((size.width - stroke) * anim.value) + stroke / 2f
        if (anim.value > 0.001f) {
            val path = Path().apply {
                var x = stroke / 2f
                moveTo(x, cy + amp * sin(2.0 * PI * (x / wl)).toFloat())
                val step = 3.dp.toPx()
                while (x < endX) {
                    x = min(x + step, endX)
                    lineTo(x, cy + amp * sin(2.0 * PI * (x / wl)).toFloat())
                }
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val gapStart = endX + 6.dp.toPx()
        val trackEnd = size.width - stroke / 2f
        if (gapStart < trackEnd) drawLine(track, Offset(gapStart, cy), Offset(trackEnd, cy), stroke, StrokeCap.Round)
        drawCircle(color, 2.dp.toPx(), Offset(trackEnd, cy))
    }
}

/** 48dp "back to top" button that pops in once the page has been scrolled. */
@Composable
fun ScrollToTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.7f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
    ) {
        Surface(
            onClick = onClick, shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 4.dp, modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.common_scrollTop)) }
        }
    }
}

/** Fades + slides a section in shortly after the screen appears (staggered by [index]). Cheap: one state flip per section. */
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 45L); shown = true }
    AnimatedVisibility(
        visible = shown, modifier = modifier,
        enter = fadeIn(tween(260)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 10 },
    ) { content() }
}

/** Four-quadrant swatch of a theme palette (primary / secondary / tertiary / container). */
@Composable
fun PalettePreview(p: PaletteDef, dark: Boolean, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val a = if (dark) p.dark else p.light
    val colors = listOf(Color(a[0]), Color(a[4]), Color(a[8]), Color(a[2]))   // primary, secondary, tertiary, primary-container
    Canvas(modifier.size(size).clip(CircleShape)) {
        val s = Size(this.size.width, this.size.height)
        colors.forEachIndexed { i, c -> drawArc(c, -90f + 90f * i, 90f, true, Offset.Zero, s) }
    }
}

/** Swipe right = edit, swipe left = delete. The row always springs back; the callbacks decide what happens next. */
@Composable
fun SwipeActionRow(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val editCb by androidx.compose.runtime.rememberUpdatedState(onEdit)
    val deleteCb by androidx.compose.runtime.rememberUpdatedState(onDelete)
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.45f }, confirmValueChange = { v ->
        when (v) {
            androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> editCb()
            androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> deleteCb()
            else -> {}
        }
        false
    })
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.SwipeToDismissBox(
        state = state, modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val edit = dir == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
            val del = dir == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(if (edit) cs.primaryContainer else if (del) cs.errorContainer else Color.Transparent, tween(120), label = "swipeBg")
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(bg).padding(horizontal = 24.dp),
                contentAlignment = if (del) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (edit) Icon(Icons.Rounded.Edit, null, tint = cs.onPrimaryContainer)
                if (del) Icon(Icons.Rounded.Delete, null, tint = cs.onErrorContainer)
            }
        },
    ) { content() }
}

/** Compact "value ▾" button with a dropdown menu, for rows like Font / Language / Start page. */
@Composable
fun ValueMenu(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    Box(modifier) {
        Surface(onClick = { open = true }, shape = CircleShape, color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
            Row(Modifier.padding(start = 14.dp, end = 8.dp).height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
                Icon(androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, Modifier.size(18.dp))
            }
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (k, v) ->
                DropdownMenuItem(
                    text = { Text(v, fontWeight = if (k == selected) androidx.compose.ui.text.font.FontWeight.Bold else null) },
                    leadingIcon = if (k == selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                    onClick = { onSelect(k); open = false },
                )
            }
        }
    }
}

/** Swatch for the "Dynamic (Material You)" option: four colors from the current wallpaper scheme. */
@Composable
fun DynamicPreview(dark: Boolean, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val colors = remember(dark) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val cs = if (dark) androidx.compose.material3.dynamicDarkColorScheme(ctx) else androidx.compose.material3.dynamicLightColorScheme(ctx)
            listOf(cs.primary, cs.secondary, cs.tertiary, cs.primaryContainer)
        } else listOf(Color.Gray, Color.LightGray, Color.DarkGray, Color.Gray)
    }
    Canvas(modifier.size(size).clip(CircleShape)) {
        val s = Size(this.size.width, this.size.height)
        colors.forEachIndexed { i, c -> drawArc(c, -90f + 90f * i, 90f, true, Offset.Zero, s) }
    }
}
