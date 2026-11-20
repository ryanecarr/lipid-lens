package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.DoseFrequency

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cached = ReminderCachePrefs.read(context)
        if (cached.enabled) {
            ReminderScheduler.showReminderNotification(
                context = context,
                activeMedName = cached.activeMedName,
                activeDoseMg = cached.activeDoseMg,
                frequencyKey = cached.frequencyKey
            )
            ReminderScheduler.scheduleReminder(
                context = context,
                hour = cached.hour,
                minute = cached.minute,
                frequency = DoseFrequency.fromKey(cached.frequencyKey),
                startedAtMillis = cached.regimenStartedAtMillis,
                afterAlarmFired = true
            )
        }
    }
}
