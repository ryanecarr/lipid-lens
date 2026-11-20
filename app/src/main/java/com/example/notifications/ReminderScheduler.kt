package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.StatinRegimen
import java.util.Calendar

object ReminderScheduler {
    private const val CHANNEL_ID = "statin_daily_reminders"
    private const val REQUEST_CODE_ALARM = 4001
    private const val NOTIFICATION_ID = 4002
    private val EPOCH_2000_MILLIS by lazy { DateTimeUtils.utcMillisFromDateKey("2000-01-01") }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        frequency: DoseFrequency,
        startedAtMillis: Long = -1L,
        nowMillis: Long = System.currentTimeMillis(),
        afterAlarmFired: Boolean = false
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val threshold = if (afterAlarmFired) nowMillis + 60_000L else nowMillis

        if (frequency == DoseFrequency.ONCE_DAILY_EVENING ||
            frequency == DoseFrequency.ONCE_DAILY_MORNING ||
            frequency == DoseFrequency.CUSTOM
        ) {
            val trigger = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, safeHour)
                set(Calendar.MINUTE, safeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                while (timeInMillis <= threshold) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return trigger.timeInMillis
        }

        // Periodic schedule (every other day, every 2 weeks, once monthly)
        val anchor = Calendar.getInstance().apply {
            timeInMillis = if (startedAtMillis > EPOCH_2000_MILLIS) startedAtMillis else nowMillis
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetDayOfMonth = anchor.get(Calendar.DAY_OF_MONTH)
        var guard = 0
        while (anchor.timeInMillis <= threshold && guard < 500) {
            guard++
            when (frequency) {
                DoseFrequency.EVERY_OTHER_DAY -> anchor.add(Calendar.DAY_OF_YEAR, 2)
                DoseFrequency.EVERY_2_WEEKS -> anchor.add(Calendar.DAY_OF_YEAR, 14)
                DoseFrequency.ONCE_MONTHLY -> {
                    anchor.add(Calendar.MONTH, 1)
                    val maxDay = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
                    anchor.set(Calendar.DAY_OF_MONTH, minOf(targetDayOfMonth, maxDay))
                }
                else -> anchor.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return anchor.timeInMillis
    }

    fun scheduleReminder(
        context: Context,
        hour: Int,
        minute: Int,
        frequency: DoseFrequency = DoseFrequency.ONCE_DAILY_EVENING,
        startedAtMillis: Long = -1L,
        afterAlarmFired: Boolean = false
    ): Long {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)
        val triggerAt = computeNextTriggerMillis(
            hour = hour,
            minute = minute,
            frequency = frequency,
            startedAtMillis = startedAtMillis,
            afterAlarmFired = afterAlarmFired
        )
        ReminderCachePrefs.saveNextTriggerAtMillis(context, triggerAt)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        return triggerAt
    }

    fun scheduleAtExactTrigger(context: Context, triggerAtMillis: Long) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)
        ReminderCachePrefs.saveNextTriggerAtMillis(context, triggerAtMillis)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        scheduleReminder(context, hour, minute, DoseFrequency.ONCE_DAILY_EVENING, -1L)
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmPendingIntent(context))
        ReminderCachePrefs.saveNextTriggerAtMillis(context, -1L)
    }

    fun showReminderNotification(context: Context, activeRegimen: StatinRegimen?): Boolean {
        return showReminderNotification(
            context = context,
            activeMedName = activeRegimen?.statinName,
            activeDoseMg = activeRegimen?.doseMg,
            frequencyKey = activeRegimen?.frequencyKey
        )
    }

    fun showReminderNotification(
        context: Context,
        activeMedName: String?,
        activeDoseMg: Int?,
        frequencyKey: String? = null
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (!activeMedName.isNullOrBlank() && activeDoseMg != null && activeDoseMg > 0) {
            context.getString(
                R.string.reminder_notif_title,
                activeMedName,
                activeDoseMg
            )
        } else {
            context.getString(R.string.reminder_notif_title_generic)
        }

        val bodyText = if (!frequencyKey.isNullOrBlank()) {
            val freq = DoseFrequency.fromKey(frequencyKey)
            context.getString(R.string.reminder_notif_body_cadence, freq.reminderSummary)
        } else {
            context.getString(R.string.reminder_notif_body)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
