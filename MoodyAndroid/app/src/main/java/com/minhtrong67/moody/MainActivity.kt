package com.minhtrong67.moody

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.MoodyRoot
import com.minhtrong67.moody.ui.theme.MoodyTheme
import com.minhtrong67.moody.ui.theme.resolveDark

/** AppCompatActivity (not ComponentActivity) so AppCompat per-app language switching works on all API levels. */
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings ?: return@setContent   // still reading DataStore: window background only

            val dark = resolveDark(s.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            LaunchedEffect(s.language) {
                AppCompatDelegate.setApplicationLocales(
                    if (s.language == "auto") LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(s.language)
                )
            }
            MoodyTheme(s.themeMode, s.themeColor, s.fontScale) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                    MoodyRoot(vm, s, pendingRoute) { pendingRoute = null }
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
