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

@Composable
private fun BrandMark() {
    Surface(Modifier.size(88.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimary) }
    }
}

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
private fun Feature(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val error by vm.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val top = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    Surface(Modifier.fillMaxSize(), color = surface) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(top, surface))).statusBarsPadding().padding(start = 28.dp, end = 28.dp, top = 56.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandMark()
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
            // Features
            Column(Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Feature(Icons.Rounded.Sync, stringResource(R.string.login_f1_title), stringResource(R.string.login_f1_desc))
                Feature(Icons.Rounded.DonutLarge, stringResource(R.string.login_f2_title), stringResource(R.string.login_f2_desc))
                Feature(Icons.Rounded.LocalCafe, stringResource(R.string.login_f3_title), stringResource(R.string.login_f3_desc))
            }
            Spacer(Modifier.height(40.dp))
            // Action
            Column(Modifier.navigationBarsPadding().padding(horizontal = 28.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(error != null) {
                    Surface(Modifier.padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, null); Spacer(Modifier.width(10.dp))
                            Text(error ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Surface(
                    onClick = { vm.signIn(context) }, enabled = !signingIn, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (signingIn) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp) else GoogleG()
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(if (signingIn) R.string.login_signing_in else R.string.login_google), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.login_same_account), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
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
