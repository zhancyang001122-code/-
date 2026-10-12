package com.example.customalarm.data

import android.content.Context
import android.content.SharedPreferences
import com.example.customalarm.model.AlarmItem
import com.example.customalarm.model.RepeatType
import org.json.JSONArray
import org.json.JSONObject

class AlarmStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("custom_alarms_prefs", Context.MODE_PRIVATE)

    fun getAllAlarms(): List<AlarmItem> {
        val jsonStr = prefs.getString("alarms_json", "[]") ?: "[]"
        return parseList(jsonStr)
    }

    fun saveAlarm(alarm: AlarmItem) {
        val list = getAllAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            list[index] = alarm.copy(lastUsedTimestamp = System.currentTimeMillis())
        } else {
            list.add(0, alarm.copy(lastUsedTimestamp = System.currentTimeMillis()))
        }
        saveList(list)
    }

    fun deleteAlarm(id: Long) {
        val list = getAllAlarms().filterNot { it.id == id }
        saveList(list)
    }

    fun updateAlarmStatus(id: Long, isEnabled: Boolean, nextTime: Long? = null) {
        val list = getAllAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = list[index]
            list[index] = old.copy(
                isEnabled = isEnabled,
                targetTimestampMillis = nextTime ?: old.targetTimestampMillis,
                lastUsedTimestamp = System.currentTimeMillis()
            )
            saveList(list)
        }
    }

    fun getRecentHistory(limit: Int = 10): List<AlarmItem> {
        return getAllAlarms()
            .sortedByDescending { it.lastUsedTimestamp }
            .take(limit)
    }

    private fun saveList(list: List<AlarmItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("targetTimestampMillis", item.targetTimestampMillis)
                put("repeatType", item.repeatType.name)
                put("intervalDays", item.intervalDays)
                put("selectedDaysOfWeek", JSONArray(item.selectedDaysOfWeek))
                put("ringDurationMinutes", item.ringDurationMinutes)
                put("snoozeMinutes", item.snoozeMinutes)
                put("ringtoneUri", item.ringtoneUri ?: "")
                put("ringtoneName", item.ringtoneName)
                put("isEnabled", item.isEnabled)
                put("isFavoriteTemplate", item.isFavoriteTemplate)
                put("lastUsedTimestamp", item.lastUsedTimestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("alarms_json", array.toString()).apply()
    }

    private fun parseList(jsonStr: String): List<AlarmItem> {
        val result = mutableListOf<AlarmItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArr = obj.optJSONArray("selectedDaysOfWeek") ?: JSONArray()
                val daysList = mutableListOf<Int>()
                for (j in 0 until daysArr.length()) {
                    daysList.add(daysArr.getInt(j))
                }

                val uriStr = obj.optString("ringtoneUri", "")
                val item = AlarmItem(
                    id = obj.getLong("id"),
                    title = obj.optString("title", "自定义闹钟"),
                    targetTimestampMillis = obj.getLong("targetTimestampMillis"),
                    repeatType = try {
                        RepeatType.valueOf(obj.optString("repeatType", "ONCE"))
                    } catch (e: Exception) {
                        RepeatType.ONCE
                    },
                    intervalDays = obj.optInt("intervalDays", 1),
                    selectedDaysOfWeek = daysList,
                    ringDurationMinutes = obj.optInt("ringDurationMinutes", 5),
                    snoozeMinutes = obj.optInt("snoozeMinutes", 10),
                    ringtoneUri = if (uriStr.isBlank()) null else uriStr,
                    ringtoneName = obj.optString("ringtoneName", "系统默认铃声"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    isFavoriteTemplate = obj.optBoolean("isFavoriteTemplate", false),
                    lastUsedTimestamp = obj.optLong("lastUsedTimestamp", System.currentTimeMillis())
                )
                result.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
