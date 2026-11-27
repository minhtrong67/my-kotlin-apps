package com.expenser.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.ui.MainViewModel

/** The four-colour Google "G" drawn with arcs (no bitmap asset needed). */
@Composable
private fun GoogleG(size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val stroke = w * 0.2f
        val inset = stroke / 2
        val arcSize = Size(w - stroke, w - stroke); val tl = Offset(inset, inset)
        fun arc(c: Long, start: Float, sweep: Float) = drawArc(Color(c), start, sweep, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        arc(0xFFEA4335, 200f, 90f)   // red (top)
        arc(0xFFFBBC05, 140f, 60f)   // yellow (left)
        arc(0xFF34A853, 50f, 90f)    // green (bottom)
        arc(0xFF4285F4, -20f, 70f)   // blue (right)
        drawRect(Color(0xFF4285F4), Offset(w / 2, w / 2 - stroke / 2), Size(w / 2 - inset, stroke))
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Decorative mini dashboard (ring + bars) shown in the hero; carries no real data. */
@Composable
private fun PreviewCard(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.16f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(72.dp)) {
                val stroke = size.width * 0.22f; val inset = stroke / 2
                val sz = Size(size.width - stroke, size.height - stroke)
                fun arc(c: Long, start: Float, sweep: Float) = drawArc(Color(c), start, sweep, false, Offset(inset, inset), sz, style = Stroke(stroke, cap = StrokeCap.Round))
                arc(0xFF22C55E, -90f, 150f); arc(0xFFEF4444, 70f, 110f); arc(0xFFFBBF24, 190f, 70f)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0.9f to 0xFF22C55E, 0.62f to 0xFFEF4444, 0.38f to 0xFFFBBF24).forEach { (w, c) ->
                    Box(Modifier.fillMaxWidth(w).height(10.dp).background(Color(c).copy(alpha = 0.9f), CircleShape))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val error by vm.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val c1 = Color(0xFF1E40AF); val c2 = Color(0xFF2563EB); val c3 = Color(0xFF38BDF8)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2, c3), start = Offset(0f, 0f), end = Offset(900f, 1900f)))) {
        val compact = maxHeight < 700.dp
        // soft decorative circles
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.55f, center = Offset(size.width * 0.95f, size.height * 0.02f))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.38f, center = Offset(size.width * 0.05f, size.height * 0.34f))
        }
        val wide = maxWidth > maxHeight
        @Composable
        fun HeroPane(modifier: Modifier) {
            Column(modifier.statusBarsPadding().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AnimatedVisibility(shown, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 6 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(Modifier.size(84.dp), shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 12.dp) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(46.dp), tint = c2) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                        if (!compact && !wide) { Spacer(Modifier.height(28.dp)); PreviewCard(Modifier.fillMaxWidth(0.82f)) }
                    }
                }
            }
        }
        @Composable
        fun SheetPane(modifier: Modifier) {
            AnimatedVisibility(shown, modifier = modifier, enter = fadeIn(tween(500, 150)) + slideInVertically(tween(550, 100)) { it / 3 }) {
                Surface(
                    Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = cs.surface,
                    shape = if (wide) RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp) else RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                ) {
                    Column(
                        Modifier.then(if (wide) Modifier.fillMaxHeight().verticalScroll(rememberScrollState()) else Modifier).navigationBarsPadding().padding(horizontal = 28.dp).padding(top = 28.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.login_welcome_sub), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FeatureChip(Icons.Rounded.Sync, stringResource(R.string.login_f1_title), Modifier.weight(1f))
                            FeatureChip(Icons.Rounded.DonutLarge, stringResource(R.string.login_f2_title), Modifier.weight(1f))
                            FeatureChip(Icons.Rounded.LocalCafe, stringResource(R.string.login_f3_title), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(24.dp))
                        AnimatedVisibility(error != null) {
                            Surface(Modifier.padding(bottom = 14.dp), shape = RoundedCornerShape(16.dp), color = cs.errorContainer, contentColor = cs.onErrorContainer) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ErrorOutline, null); Spacer(Modifier.width(10.dp))
                                    Text(error ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Surface(
                            onClick = { vm.signIn(context) }, enabled = !signingIn, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
                            color = cs.surfaceContainerLowest, border = BorderStroke(1.dp, cs.outline), shadowElevation = 2.dp,
                        ) {
                            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                if (signingIn) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp) else GoogleG()
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(if (signingIn) R.string.login_signing_in else R.string.login_google), style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.login_same_account), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (wide) Row(Modifier.fillMaxSize()) { HeroPane(Modifier.weight(1f).fillMaxHeight()); SheetPane(Modifier.weight(1f).fillMaxHeight()) }
        else Column(Modifier.fillMaxSize()) { HeroPane(Modifier.weight(1f).fillMaxWidth()); SheetPane(Modifier.fillMaxWidth()) }
    }
}

/** Shown when app/google-services.json was not present at build time. */
@Composable
fun SetupScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SettingsSuggest, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.setup_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
