package com.gnort67.wallpaperswitcher

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = Prefs.get(this)
        if (p.autoSwitch) {
            // Marker still set but no live service = the system killed it earlier.
            if (p.serviceMarker && !ThemeWatcherService.running) p.updateKilledHint(true)
            ThemeWatcherService.start(this)
        }
        setContent { AppTheme(p) { AppRoot(p) } }
    }
}
