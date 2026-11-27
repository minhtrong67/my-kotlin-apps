package com.expenser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastKind { SUCCESS, ERROR, INFO, WARNING }

data class ToastData(val text: String, val kind: ToastKind, val id: Long = System.nanoTime())

/** Picks a variant from a string resource name, e.g. "tx_added" -> success, "import_error" -> error. */
fun toastKindFor(entryName: String): ToastKind {
    val n = entryName.lowercase()
    return when {
        listOf("error", "denied", "failed", "invalid", "insufficient").any { n.contains(it) } -> ToastKind.ERROR
        listOf("empty", "skipped", "in_use", "same_wallet", "no_match").any { n.contains(it) } -> ToastKind.WARNING
        listOf("added", "updated", "deleted", "saved", "exported", "imported", "copied", "success", "_on", "created", "changed").any { n.contains(it) } -> ToastKind.SUCCESS
        else -> ToastKind.INFO
    }
}

/** Compact toast pinned under the status bar; readable on light and dark themes. Disappears by itself. */
@Composable
fun BoxScope.AppToastHost(data: ToastData?, onDone: (Long) -> Unit) {
    var shown by remember { mutableStateOf<ToastData?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(data?.id) {
        if (data != null) {
            shown = data; visible = true
            delay(if (data.kind == ToastKind.ERROR || data.kind == ToastKind.WARNING) 3000 else 2200)
            visible = false
            delay(300)
            onDone(data.id)
        }
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    AnimatedVisibility(
        visible && shown != null,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        enter = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(180)),
    ) {
        val t = shown ?: return@AnimatedVisibility
        val (bg, icon) = when (t.kind) {
            ToastKind.SUCCESS -> (if (dark) Color(0xFF2E7D32) else Color(0xFF2E7D32)) to Icons.Rounded.CheckCircle
            ToastKind.ERROR -> (if (dark) Color(0xFFD32F2F) else Color(0xFFC62828)) to Icons.Rounded.ErrorOutline
            ToastKind.WARNING -> (if (dark) Color(0xFFF57C00) else Color(0xFFEF6C00)) to Icons.Rounded.WarningAmber
            ToastKind.INFO -> (if (dark) Color(0xFF1976D2) else Color(0xFF1565C0)) to Icons.Rounded.Info
        }
        Surface(shape = RoundedCornerShape(16.dp), color = bg, contentColor = Color.White, shadowElevation = 8.dp, modifier = Modifier.widthIn(max = 520.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(t.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}
