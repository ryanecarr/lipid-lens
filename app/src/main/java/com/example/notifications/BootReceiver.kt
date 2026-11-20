package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.DoseFrequency

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val cached = ReminderCachePrefs.read(context)
        if (cached.enabled) {
            val now = System.currentTimeMillis()
            if (cached.nextTriggerAtMillis > now) {
                ReminderScheduler.scheduleAtExactTrigger(context, cached.nextTriggerAtMillis)
            } else {
                ReminderScheduler.scheduleReminder(
                    context = context,
                    hour = cached.hour,
                    minute = cached.minute,
                    frequency = DoseFrequency.fromKey(cached.frequencyKey),
                    startedAtMillis = cached.regimenStartedAtMillis
                )
            }
        }
    }
}
