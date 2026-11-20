package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

    fun todayDateKey(): String = dateKeyForMillis(System.currentTimeMillis())

    fun dateKeyForMillis(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(millis))
    }

    fun monthsAgoDateKey(months: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.MONTH, -months)
        }
        return dateKeyForMillis(cal.timeInMillis)
    }

    /**
     * Converts a calendar date key ("yyyy-MM-dd") to UTC midnight epoch millis,
     * matching Material 3 DatePicker's internal UTC representation and keeping
     * calendar-date timestamps invariant across timezone changes.
     */
    fun utcMillisFromDateKey(dateKey: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC_TIME_ZONE
        }
        return fmt.parse(dateKey)?.time ?: utcMillisFromDateKey(todayDateKey())
    }

    /**
     * Converts UTC midnight epoch millis (as emitted by Material 3 DatePicker)
     * back to a canonical calendar date key ("yyyy-MM-dd").
     */
    fun dateKeyFromUtcMillis(utcMillis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC_TIME_ZONE
        }
        return fmt.format(Date(utcMillis))
    }

    fun formatDateKeyShort(dateKey: String): String {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC_TIME_ZONE
        }
        val outFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
            timeZone = UTC_TIME_ZONE
        }
        val parsed = inFmt.parse(dateKey) ?: return dateKey
        return outFmt.format(parsed)
    }

    fun formatDateMillis(millis: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun formatDateTimeMillis(millis: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun formatMinutesOfDay(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 23 * 60 + 59)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, clamped / 60)
            set(Calendar.MINUTE, clamped % 60)
        }
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun lastNDaysDateKeys(days: Int): List<String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (days - 1 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            fmt.format(c.time)
        }
    }

    fun dayShortLetter(dateKey: String): String {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC_TIME_ZONE
        }
        val outFmt = SimpleDateFormat("EEEEE", Locale.getDefault()).apply {
            timeZone = UTC_TIME_ZONE
        }
        val parsed = inFmt.parse(dateKey) ?: return ""
        return outFmt.format(parsed)
    }

    fun epochDaysFromDateKey(dateKey: String): Long =
        utcMillisFromDateKey(dateKey) / MILLIS_PER_DAY

    fun dateKeyFromEpochDays(epochDays: Long): String =
        dateKeyFromUtcMillis(epochDays * MILLIS_PER_DAY)

    fun addDaysToDateKey(dateKey: String, days: Int): String =
        dateKeyFromEpochDays(epochDaysFromDateKey(dateKey) + days)

    fun dayOfMonthFromDateKey(dateKey: String): Int {
        val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
            timeInMillis = utcMillisFromDateKey(dateKey)
        }
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun daysInMonthForDateKey(dateKey: String): Int {
        val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
            timeInMillis = utcMillisFromDateKey(dateKey)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
}
