package com.example.notifications

import android.content.Context
import com.example.data.model.DoseFrequency
import com.example.data.model.ReminderSettings
import com.example.data.model.StatinRegimen

data class CachedReminderState(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val activeMedName: String?,
    val activeDoseMg: Int?,
    val frequencyKey: String = DoseFrequency.ONCE_DAILY_EVENING.storageKey,
    val regimenStartedAtMillis: Long = -1L,
    val nextTriggerAtMillis: Long = -1L
)

object ReminderCachePrefs {
    private const val PREFS_NAME = "statinlog_reminder_cache"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_MED_NAME = "active_med_name"
    private const val KEY_DOSE_MG = "active_dose_mg"
    private const val KEY_FREQUENCY = "frequency_key"
    private const val KEY_STARTED_AT = "regimen_started_at"
    private const val KEY_NEXT_TRIGGER = "next_trigger_at"

    fun saveSettings(context: Context, settings: ReminderSettings) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour.coerceIn(0, 23))
            .putInt(KEY_MINUTE, settings.minute.coerceIn(0, 59))
            .apply()
    }

    fun saveActiveRegimen(context: Context, regimen: StatinRegimen?) {
        val editor = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
        if (regimen == null) {
            editor.remove(KEY_MED_NAME)
                .remove(KEY_DOSE_MG)
                .remove(KEY_FREQUENCY)
                .remove(KEY_STARTED_AT)
        } else {
            editor.putString(KEY_MED_NAME, regimen.statinName)
                .putInt(KEY_DOSE_MG, regimen.doseMg)
                .putString(KEY_FREQUENCY, regimen.frequencyKey)
                .putLong(KEY_STARTED_AT, regimen.startedAtMillis)
        }
        editor.apply()
    }

    fun saveNextTriggerAtMillis(context: Context, nextTriggerAtMillis: Long) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_NEXT_TRIGGER, nextTriggerAtMillis)
            .apply()
    }

    fun clearOnSignOut(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun read(context: Context): CachedReminderState {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val medName = prefs.getString(KEY_MED_NAME, null)
        val dose = prefs.getInt(KEY_DOSE_MG, -1).takeIf { it > 0 }
        val freqKey = prefs.getString(KEY_FREQUENCY, DoseFrequency.ONCE_DAILY_EVENING.storageKey)
            ?: DoseFrequency.ONCE_DAILY_EVENING.storageKey
        return CachedReminderState(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 20).coerceIn(0, 23),
            minute = prefs.getInt(KEY_MINUTE, 0).coerceIn(0, 59),
            activeMedName = medName,
            activeDoseMg = dose,
            frequencyKey = freqKey,
            regimenStartedAtMillis = prefs.getLong(KEY_STARTED_AT, -1L),
            nextTriggerAtMillis = prefs.getLong(KEY_NEXT_TRIGGER, -1L)
        )
    }
}
