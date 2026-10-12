package com.example.customalarm.util

import com.example.customalarm.model.AlarmItem
import com.example.customalarm.model.RepeatType
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
                // 中国法定工作日算法：跳过节假日，遇到调休补班周末响铃
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
