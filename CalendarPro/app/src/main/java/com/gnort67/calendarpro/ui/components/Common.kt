package com.gnort67.calendarpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gnort67.calendarpro.R

private val HOLIDAY_ICON_MAP = mapOf(
    "Baby" to "child_care", "CakeSlice" to "cake", "Egg" to "egg", "Flag" to "flag",
    "Flame" to "local_fire_department", "Flower" to "local_florist", "Flower2" to "local_florist",
    "Ghost" to "skull", "GraduationCap" to "school", "HardHat" to "engineering", "Heart" to "favorite",
    "HeartHandshake" to "volunteer_activism", "Home" to "home", "Landmark" to "account_balance",
    "Moon" to "bedtime", "PartyPopper" to "celebration", "Ribbon" to "military_tech", "Shield" to "shield",
    "Sparkles" to "auto_awesome", "Sun" to "wb_sunny", "TreePine" to "park", "Turkey" to "dinner_dining"
)

/** Tên icon dữ liệu (kiểu lucide trong holidays.json) -> tên Material Symbols. */
fun holidayIconName(key: String) = HOLIDAY_ICON_MAP[key] ?: "celebration"

@Composable
fun AppIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Dp = 24.dp
) {
    val ctx = LocalContext.current
    val id = remember(name, filled) {
        val n = "ic_$name" + if (filled) "_fill" else ""
        ctx.resources.getIdentifier(n, "drawable", ctx.packageName).takeIf { it != 0 } ?: R.drawable.ic_celebration
    }
    Icon(painter = painterResource(id), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

/** Nhấn có hiệu ứng co giãn kiểu lò xo + ripple + haptic khi nhấn giữ. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press"
    )
    val haptic = LocalHapticFeedback.current
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .combinedClickable(
            interactionSource = source, indication = LocalIndication.current, enabled = enabled,
            onLongClick = onLongClick?.let { l -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); l() } },
            onClick = onClick
        )
}

@Composable
fun ShapeBadge(
    icon: String, shape: Shape, container: Color, content: Color,
    modifier: Modifier = Modifier, size: Dp = 48.dp, iconSize: Dp = 24.dp, filled: Boolean = false
) {
    Box(modifier.size(size).clip(shape).background(container), contentAlignment = Alignment.Center) {
        AppIcon(icon, tint = content, size = iconSize, filled = filled)
    }
}

class SegOption(val label: String, val icon: String? = null)

/** Bộ chọn dạng viên thuốc: chỉ báo trượt bằng lò xo. */
@Composable
fun SegmentedControl(
    options: List<SegOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    indicator: Color = MaterialTheme.colorScheme.primaryContainer,
    onIndicator: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onContainer: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    height: Dp = 52.dp
) {
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(container).padding(4.dp)
    ) {
        val itemW = maxWidth / options.size
        val x by animateDpAsState(
            itemW * selected, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "seg"
        )
        Box(Modifier.offset(x = x).width(itemW).fillMaxHeight().clip(CircleShape).background(indicator))
        Row(Modifier.fillMaxHeight()) {
            options.forEachIndexed { i, o ->
                val on = i == selected
                Box(
                    Modifier.width(itemW).fillMaxHeight().clip(CircleShape).clickable {
                        if (!on) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(i) }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val c = if (on) onIndicator else onContainer
                        if (o.icon != null) AppIcon(o.icon, tint = c, size = 18.dp, filled = on)
                        Text(
                            o.label, color = c, maxLines = 1, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/** Nút "lên đầu trang": nảy vào bằng lò xo, to 56dp dễ chạm. */
@Composable
fun ScrollTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(
        visible, modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut(tween(180)) + fadeOut(tween(180))
    ) {
        FloatingActionButton(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) { AppIcon("arrow_upward", size = 28.dp) }
    }
}

/** Đang xoay ngang (chiều rộng lớn hơn chiều cao). */
@Composable
fun isLandscape(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp > c.screenHeightDp
}

private fun scrollDuration(distancePx: Float): Int = (500 + distancePx / 4f).coerceIn(500f, 1000f).toInt()

/** Cuộn lên đầu mượt: hoạt ảnh một mạch theo khoảng cách ước lượng, rồi chốt đúng vị trí 0. */
suspend fun LazyListState.smoothScrollToTop() {
    val vis = layoutInfo.visibleItemsInfo
    if (vis.isEmpty()) return
    val avg = vis.sumOf { it.size } / vis.size
    val dist = (firstVisibleItemIndex * avg + firstVisibleItemScrollOffset).toFloat()
    if (dist > 0f) animateScrollBy(-dist, tween(scrollDuration(dist), easing = FastOutSlowInEasing))
    scrollToItem(0)
}

suspend fun LazyGridState.smoothScrollToTop(columns: Int) {
    val vis = layoutInfo.visibleItemsInfo
    if (vis.isEmpty()) return
    val avg = vis.sumOf { it.size.height } / vis.size
    val dist = (firstVisibleItemIndex / columns.coerceAtLeast(1) * avg + firstVisibleItemScrollOffset).toFloat()
    if (dist > 0f) animateScrollBy(-dist, tween(scrollDuration(dist), easing = FastOutSlowInEasing))
    scrollToItem(0)
}

/** Vùng an toàn cho bottom sheet: tránh camera đục lỗ / thanh trạng thái phía trên và tai thỏ hai bên khi xoay ngang. */
val sheetInsets: WindowInsets
    @Composable get() = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary
    )
}
