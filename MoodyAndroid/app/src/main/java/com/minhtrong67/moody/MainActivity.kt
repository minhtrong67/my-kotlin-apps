package com.minhtrong67.moody

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhtrong67.moody.ui.LocalActivityContext
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.MoodyRoot
import com.minhtrong67.moody.ui.theme.MoodyTheme
import com.minhtrong67.moody.ui.theme.resolveDark
import com.minhtrong67.moody.util.withLanguage

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
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
            val base = LocalContext.current
            val localized = remember(s.language, base) { base.withLanguage(s.language) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalActivityContext provides this@MainActivity,
            ) {
                MoodyTheme(s.themeMode, s.themeColor, s.dynamicColor, s.fontScale, s.fontFamily) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        MoodyRoot(vm, s, pendingRoute) { pendingRoute = null }
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
