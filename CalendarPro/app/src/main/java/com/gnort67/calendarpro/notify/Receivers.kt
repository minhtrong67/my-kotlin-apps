package com.gnort67.calendarpro.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gnort67.calendarpro.data.HolidayCatalog
import com.gnort67.calendarpro.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Nhận báo thức: hiện thông báo của hôm nay rồi đặt lại cho ngày mai. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                val s = store.settings.first()
                if (s.notifyEnabled) {
                    Reminders.show(app, s.language, HolidayCatalog.load(app), store.events.first())
                    Reminders.schedule(app, s.notifyHour, s.notifyMinute)
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** Đặt lại báo thức sau khi khởi động máy, cập nhật app hoặc đổi múi giờ. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val s = SettingsStore(app).settings.first()
                if (s.notifyEnabled) Reminders.schedule(app, s.notifyHour, s.notifyMinute)
            } finally {
                result.finish()
            }
        }
    }
}
