package com.gnort67.calendarpro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gnort67.calendarpro.ui.AppRoot
import com.gnort67.calendarpro.ui.theme.CalendarProTheme

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Giữ splash tới khi đọc xong cài đặt để không bị nháy sai màu / sai ngôn ngữ.
        splash.setKeepOnScreenCondition { vm.settings.value == null }
        enableEdgeToEdge()
        setContent {
            val s by vm.settings.collectAsState()
            val settings = s
            if (settings != null) {
                CalendarProTheme(settings) { AppRoot(vm, settings) }
            }
        }
    }
}
