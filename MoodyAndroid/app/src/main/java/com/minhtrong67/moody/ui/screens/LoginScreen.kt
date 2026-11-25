package com.minhtrong67.moody.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.MoodConfig
import com.minhtrong67.moody.ui.LocalActivityContext
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.theme.LocalIsDark
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sign-in: soft tonal hero (logo, wordmark, five bobbing mood faces) over a bottom sheet holding the actions.
 * Everything uses theme colors, so it follows the palette / Material You choice and dark mode.
 */
@Composable
fun LoginScreen(vm: MainViewModel) {
    val activity = LocalActivityContext.current
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val failed by vm.signInFailed.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val dark = LocalIsDark.current

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); shown = true }

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        // Decorative tonal background (drawn once; no animation)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(cs.primaryContainer.copy(alpha = if (dark) 0.75f else 0.95f), cs.surface), 0f, size.height * 0.7f))
            drawCircle(Brush.radialGradient(listOf(cs.primary.copy(alpha = 0.22f), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.06f), size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.95f, size.height * 0.06f))
            drawCircle(Brush.radialGradient(listOf(cs.tertiary.copy(alpha = 0.18f), Color.Transparent), Offset(size.width * 0.05f, size.height * 0.32f), size.width * 0.55f), size.width * 0.55f, Offset(size.width * 0.05f, size.height * 0.32f))
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenH = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // ---------------- hero ----------------
                Column(
                    Modifier.fillMaxWidth().heightIn(min = screenH * 0.44f).statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(600, easing = FastOutSlowInEasing)) { it / 6 }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MoodFaces()
                            Spacer(Modifier.height(28.dp))
                            Image(
                                painterResource(R.mipmap.ic_logo), null,
                                Modifier.size(88.dp).shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = cs.primary, spotColor = cs.primary).clip(RoundedCornerShape(26.dp)),
                            )
                            Spacer(Modifier.height(18.dp))
                            Text("Moody", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer.takeIf { dark } ?: cs.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.login_tagline), style = MaterialTheme.typography.bodyLarge,
                                color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }

                // ---------------- action sheet ----------------
                AnimatedVisibility(shown, enter = fadeIn(tween(400, 120)) + slideInVertically(tween(550, 120, FastOutSlowInEasing)) { it / 3 }) {
                    Surface(
                        Modifier.fillMaxWidth(), color = cs.surfaceContainerLow,
                        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp), shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.login_welcomeSub), style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(24.dp))

                            if (vm.firebaseReady) {
                                GoogleButton(loading = signingIn) { vm.signInWithGoogle(activity) }
                                if (failed) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(stringResource(R.string.login_error), color = cs.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HorizontalDivider(Modifier.weight(1f), color = cs.outlineVariant)
                                    Text(stringResource(R.string.login_or), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                                    HorizontalDivider(Modifier.weight(1f), color = cs.outlineVariant)
                                }
                                Spacer(Modifier.height(16.dp))
                            } else {
                                Text(stringResource(R.string.login_firebaseError), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(16.dp))
                            }

                            ActionButton(
                                stringResource(R.string.login_guest), Icons.Rounded.PhoneAndroid,
                                onClick = { vm.continueAsGuest() }, tonal = true, enabled = !signingIn,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.login_guestHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)

                            Spacer(Modifier.height(28.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Feature(Icons.Rounded.Bolt, R.string.login_f1_title)
                                Feature(Icons.Rounded.BarChart, R.string.login_f2_title)
                                Feature(Icons.Rounded.Lock, R.string.login_f3_title)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(stringResource(R.string.login_footer), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, enabled = !loading, shape = CircleShape,
        color = cs.surface, contentColor = cs.onSurface, border = BorderStroke(1.dp, cs.outline),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            else Image(painterResource(R.drawable.ic_google_g), null, Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(if (loading) R.string.login_signingIn else R.string.login_googleContinue),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            )
        }
    }
}

/** Five mood faces that bob in a gentle wave. One shared clock; motion is applied in graphicsLayer (draw phase only). */
@Composable
private fun MoodFaces() {
    val phase = rememberInfiniteTransition(label = "faces").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "facesPhase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MoodConfig.MOODS.forEachIndexed { i, m ->
            Box(
                Modifier.size(46.dp)
                    .graphicsLayer {
                        val a = sin(2.0 * PI * (phase.value - i * 0.12)).toFloat()
                        translationY = -6.dp.toPx() * a
                        val sc = 1f + 0.06f * a
                        scaleX = sc; scaleY = sc
                    }
                    .clip(CircleShape).background(Color(m.color).copy(alpha = 0.30f)),
                contentAlignment = Alignment.Center,
            ) { Text(m.emoji, fontSize = 24.sp) }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: Int) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = cs.onPrimaryContainer)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(title), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
    }
}
