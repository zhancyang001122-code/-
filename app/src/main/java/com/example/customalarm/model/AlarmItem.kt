package com.example.customalarm.model

enum class RepeatType {
    ONCE,               // 单次（指定具体年月日时分）
    DAILY,              // 每天
    WEEKDAYS,           // 普通周一至周五
    CHINA_WORKDAYS,     // 中国法定工作日（智能跳过假期，周末调休补班响铃）
    INTERVAL_DAYS,      // 每隔 N 天 (如每2天、每3天)
    CUSTOM_WEEKDAYS     // 自定义周几
}

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val title: String = "自定义闹钟",
    val targetTimestampMillis: Long,
    val repeatType: RepeatType = RepeatType.ONCE,
    val intervalDays: Int = 1,
    val selectedDaysOfWeek: List<Int> = emptyList(),
    val ringDurationMinutes: Int = 5,       // 响铃超时时长（1/3/5/10分钟）
    val snoozeMinutes: Int = 10,            // 贪睡稍后提醒时长（分钟）
    val ringtoneUri: String? = null,        // 本地音频 URI (null 表示系统默认闹钟铃声)
    val ringtoneName: String = "系统默认铃声", // 本地音频文件名展示
    val isEnabled: Boolean = true,
    val isFavoriteTemplate: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
