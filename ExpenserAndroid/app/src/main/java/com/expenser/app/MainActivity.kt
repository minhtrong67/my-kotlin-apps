package com.expenser.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.reminder.ReminderScheduler
import com.expenser.app.ui.AppRoot
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.theme.ExpenserTheme
import com.expenser.app.util.Format

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingAdd by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Format.lang = if (resources.configuration.locales[0].language == "en") "en" else "vi"
        pendingAdd = intent?.getStringExtra(ReminderScheduler.EXTRA_ADD)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            ExpenserTheme(settings) {
                AppRoot(vm, pendingAdd) { pendingAdd = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAdd = intent.getStringExtra(ReminderScheduler.EXTRA_ADD)
    }
}
