@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.MoodDef
import com.minhtrong67.moody.ui.theme.LocalIsDark

/** Web `card-filled`: tinted lowest surface in light theme, highest container in dark theme. */
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
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = bg, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
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
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) else ButtonDefaults.filledTonalButtonColors()
        FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) { content() }
    } else {
        val colors = if (danger) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) else ButtonDefaults.buttonColors()
        Button(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) { content() }
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
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        dismissButton = { ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, onDismiss, tonal = true) },
        confirmButton = { ActionButton(confirmLabel, confirmIcon, onConfirm, danger = danger) },
    )
}

/** Material 3 has no TimePickerDialog, so wrap TimePicker in an AlertDialog. */
@Composable
fun TimePickerDialog(initialHour: Int, initialMinute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val state = rememberTimePickerState(initialHour, initialMinute, is24Hour = DateFormat.is24HourFormat(ctx))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checkin_pickTime)) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
        dismissButton = { ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, onDismiss, tonal = true) },
        confirmButton = { ActionButton(stringResource(R.string.common_ok), Icons.Rounded.Check, { onConfirm(state.hour, state.minute) }) },
    )
}

@Composable
fun SegmentedTabs(
    options: List<Pair<String, String>>,   // key to label
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    icons: Map<String, ImageVector> = emptyMap(),
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (key, label) ->
            SegmentedButton(
                selected = key == selected,
                onClick = { onSelect(key) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {
                    val ic = icons[key]
                    if (ic != null && key != selected) Icon(ic, null, Modifier.size(18.dp))
                    else SegmentedButtonDefaults.Icon(key == selected)
                },
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (k, v) ->
                DropdownMenuItem(text = { Text(v) }, onClick = { onSelect(k); expanded = false })
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 8.dp))
}

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small static chip used for activities inside cards. */
@Composable
fun StaticChip(emoji: String, label: String, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier.clip(CircleShape).background(if (dark) cs.surfaceContainerHigh else cs.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (dark) cs.onSurfaceVariant else cs.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mood picker bubble: scales up and morphs towards a squircle when selected (web check-in animation). */
@Composable
fun MoodPickBubble(mood: MoodDef, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.1f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "moodScale")
    val corner by animateFloatAsState(if (selected) 34f else 50f, spring(stiffness = 400f), label = "moodCorner")
    val bg by animateColorAsState(
        if (selected) mood.color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "moodBg",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick,
    )) {
        Box(
            Modifier.size(58.dp).scale(scale).clip(RoundedCornerShape(percent = corner.toInt())).background(bg),
            contentAlignment = Alignment.Center,
        ) { Text(mood.emoji, fontSize = 26.sp) }
        Spacer(Modifier.size(6.dp))
        Text(
            mood.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(62.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
