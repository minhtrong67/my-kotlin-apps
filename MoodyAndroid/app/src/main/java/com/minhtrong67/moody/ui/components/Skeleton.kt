package com.minhtrong67.moody.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One shimmer clock per skeleton screen (every placeholder reads it in the DRAW phase: no recomposition per frame). */
private val LocalShimmer = staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val phase = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shimmerPhase")
    CompositionLocalProvider(LocalShimmer provides phase, content = content)
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    val phase = LocalShimmer.current
    val cs = MaterialTheme.colorScheme
    val base = cs.surfaceContainerHigh
    val highlight = lerp(base, cs.onSurface, 0.10f)
    Box(
        modifier.clip(shape).drawWithCache {
            onDrawBehind {
                val w = size.width.coerceAtLeast(1f)
                val x = if (phase != null) -w + 2f * w * phase.value else 0f
                drawRect(Brush.linearGradient(listOf(base, highlight, base), Offset(x, 0f), Offset(x + w, 0f)))
            }
        },
    )
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    MoodyCard(modifier.fillMaxWidth()) { content() }
}

@Composable
private fun EntrySkeleton() {
    SkeletonCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SkeletonBox(Modifier.size(48.dp), CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.width(120.dp).height(16.dp))
                SkeletonBox(Modifier.width(80.dp).height(12.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { SkeletonBox(Modifier.weight(1f).height(34.dp), CircleShape) }
        }
    }
}

/** History list placeholder. */
@Composable
fun ListSkeleton(count: Int = 4, modifier: Modifier = Modifier) {
    ShimmerHost { Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) { repeat(count) { EntrySkeleton() } } }
}

/** Today screen placeholder: hero card, three tiles, week strip. */
@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    ShimmerHost {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SkeletonBox(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonBox(Modifier.weight(1f).height(110.dp), RoundedCornerShape(28.dp)) }
            }
            SkeletonBox(Modifier.width(110.dp).height(18.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(86.dp), RoundedCornerShape(28.dp))
            EntrySkeleton()
        }
    }
}

/** Statistics placeholder: 2x2 tiles + chart + legend. */
@Composable
fun StatsSkeleton(modifier: Modifier = Modifier) {
    ShimmerHost {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(2) { SkeletonBox(Modifier.weight(1f).height(112.dp), RoundedCornerShape(28.dp)) } } }
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.width(180.dp).height(18.dp))
            SkeletonCard {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { SkeletonBox(Modifier.size(170.dp), CircleShape) }
            }
            SkeletonCard {
                repeat(4) {
                    SkeletonBox(Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(10.dp))
                    SkeletonBox(Modifier.fillMaxWidth().height(8.dp), CircleShape)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** Check-in form placeholder. */
@Composable
fun CheckInSkeleton(modifier: Modifier = Modifier) {
    ShimmerHost {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SkeletonBox(Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { repeat(5) { SkeletonBox(Modifier.size(56.dp), CircleShape) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { SkeletonBox(Modifier.weight(1f).height(44.dp), RoundedCornerShape(12.dp)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { SkeletonBox(Modifier.weight(1f).height(44.dp), RoundedCornerShape(12.dp)) } }
            SkeletonBox(Modifier.fillMaxWidth().height(110.dp), RoundedCornerShape(16.dp))
        }
    }
}
