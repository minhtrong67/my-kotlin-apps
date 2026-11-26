package com.minhtrong67.moody

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhtrong67.moody.ui.LocalActivityContext
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.MoodyRoot
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.theme.MoodyTheme
import com.minhtrong67.moody.ui.theme.resolveDark
import com.minhtrong67.moody.util.withLanguage
import java.io.File

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingRoute by mutableStateOf<String?>(null)
    private var lastCrash by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        // If the previous run crashed, MoodyApp saved the stack trace: show it once so it can be copied and reported.
        runCatching {
            val f = File(filesDir, MoodyApp.CRASH_FILE)
            if (f.exists()) { lastCrash = f.readText().take(6000); f.delete() }
        }
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings ?: return@setContent   // still reading DataStore: only the window background shows

            val dark = resolveDark(s.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            // In-app language: swap the resources Compose reads — instant, no activity recreation (no flash).
            // The localized context is NOT an Activity, so Compose can no longer find the activity-result registry /
            // back dispatcher through LocalContext: provide them explicitly (otherwise rememberLauncherForActivityResult crashes).
            val base = LocalContext.current
            val localized = remember(s.language, base) { base.withLanguage(s.language) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalActivityContext provides this@MainActivity,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity,
            ) {
                MoodyTheme(s.themeMode, s.themeColor, s.dynamicColor, s.fontScale) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        MoodyRoot(vm, s, pendingRoute) { pendingRoute = null }
                        lastCrash?.let { CrashDialog(it) { lastCrash = null } }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

@androidx.compose.runtime.Composable
private fun CrashDialog(trace: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_title)) },
        text = {
            Column {
                Text(stringResource(R.string.crash_hint), style = MaterialTheme.typography.bodyMedium)
                SelectionContainer(Modifier.heightIn(max = 280.dp)) {
                    Text(
                        trace, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 12.dp),
                    )
                }
            }
        },
        dismissButton = { ActionButton(stringResource(R.string.common_close), Icons.Rounded.Close, onDismiss, tonal = true) },
        confirmButton = {
            ActionButton(stringResource(R.string.crash_copy), Icons.Rounded.ContentCopy, {
                runCatching {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Moody crash", trace))
                }
            })
        },
    )
}
