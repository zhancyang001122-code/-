package com.example.customalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.customalarm.data.AlarmStorage
import com.example.customalarm.util.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val storage = AlarmStorage(context)
            val scheduler = AlarmScheduler(context)
            val alarms = storage.getAllAlarms().filter { it.isEnabled }
            for (alarm in alarms) {
                scheduler.schedule(alarm)
            }
        }
    }
}
