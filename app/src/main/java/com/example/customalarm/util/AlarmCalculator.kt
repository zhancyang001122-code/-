package com.example.customalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.example.customalarm.model.AlarmItem
import com.example.customalarm.model.RepeatType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ChinaHolidayHelper {

    private val COMPENSATORY_WORKDAYS = hashSetOf(
        "20260104", "20260215", "20260228", "20260426", "20260509", "20260920", "20261010"
    )

    private val STATUTORY_HOLIDAYS = hashSetOf(
        "20260101", "20260102", "20260103",
        "20260217", "20260218", "20260219", "20260220", "20260221", "20260222", "20260223",
        "20260404", "20260405", "20260406",
        "20260501", "20260502", "20260503", "20260504", "20260505",
        "20260619", "20260620", "20260621",
        "20260925", "20260926", "20260927",
        "20261001", "20261002", "20261003", "20261004", "20261005", "20261006", "20261007"
    )

    fun isChinaWorkday(cal: Calendar): Boolean {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.SIMPLIFIED_CHINESE).apply {
            timeZone = AlarmCalculator.BEIJING_TIME_ZONE
        }
        val dateKey = sdf.format(cal.time)

        if (COMPENSATORY_WORKDAYS.contains(dateKey)) {
            return true
        }

        if (STATUTORY_HOLIDAYS.contains(dateKey)) {
            return false
        }

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }
}

object AlarmSoundPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, soundUri: Uri) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            playDefault(context)
        }
    }

    private fun playDefault(context: Context) {
        try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, defaultUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}

object AlarmCalculator {

    val BEIJING_TIME_ZONE: TimeZone = TimeZone.getTimeZone("GMT+8")
    val CHINESE_LOCALE: Locale = Locale.SIMPLIFIED_CHINESE

    fun getBeijingCalendar(): Calendar {
        return Calendar.getInstance(BEIJING_TIME_ZONE, CHINESE_LOCALE)
    }

    fun calculateNextTriggerTime(item: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long {
        if (item.repeatType == RepeatType.ONCE) {
            return item.targetTimestampMillis
        }

        val targetCal = getBeijingCalendar().apply {
            timeInMillis = item.targetTimestampMillis
        }
        val targetHour = targetCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = targetCal.get(Calendar.MINUTE)

        val nextCal = getBeijingCalendar().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (item.repeatType) {
            RepeatType.ONCE -> {
                return item.targetTimestampMillis
            }
            RepeatType.DAILY -> {
                if (nextCal.timeInMillis <= nowMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return nextCal.timeInMillis
            }
            RepeatType.WEEKDAYS -> {
                if (nextCal.timeInMillis <= nowMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (isWeekend(nextCal)) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return nextCal.timeInMillis
            }
            RepeatType.CHINA_WORKDAYS -> {
                if (nextCal.timeInMillis <= nowMillis) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (!ChinaHolidayHelper.isChinaWorkday(nextCal)) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return nextCal.timeInMillis
            }
            RepeatType.INTERVAL_DAYS -> {
                val interval = if (item.intervalDays > 0) item.intervalDays else 1
                var candidate = item.targetTimestampMillis
                val stepMillis = interval.toLong() * 24 * 60 * 60 * 1000L
                while (candidate <= nowMillis) {
                    candidate += stepMillis
                }
                return candidate
            }
            RepeatType.CUSTOM_WEEKDAYS -> {
                val allowedDays = if (item.selectedDaysOfWeek.isEmpty()) listOf(Calendar.MONDAY) else item.selectedDaysOfWeek
                if (nextCal.timeInMillis <= nowMillis || nextCal.get(Calendar.DAY_OF_WEEK) !in allowedDays) {
                    do {
                        nextCal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (nextCal.get(Calendar.DAY_OF_WEEK) !in allowedDays)
                }
                return nextCal.timeInMillis
            }
        }
    }

    private fun isWeekend(cal: Calendar): Boolean {
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }
}
