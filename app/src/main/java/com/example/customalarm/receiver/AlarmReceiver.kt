package com.example.customalarm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.customalarm.data.AlarmStorage
import com.example.customalarm.model.AlarmItem
import com.example.customalarm.model.RepeatType
import com.example.customalarm.ui.MainActivity
import com.example.customalarm.util.AlarmCalculator
import com.example.customalarm.util.AlarmScheduler
import com.example.customalarm.util.AlarmSoundPlayer

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmTitle = intent.getStringExtra("ALARM_TITLE") ?: "自定义闹钟响铃"

        if (action == "com.example.customalarm.ALARM_SNOOZE") {
            AlarmSoundPlayer.stop()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(alarmId.toInt())

            val storage = AlarmStorage(context)
            val alarm = storage.getAllAlarms().find { it.id == alarmId }
            val snoozeMinutes = alarm?.snoozeMinutes ?: 10
            val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

            val snoozeAlarm = AlarmItem(
                id = System.currentTimeMillis(),
                title = "$alarmTitle (稍后提醒)",
                targetTimestampMillis = snoozeTime,
                repeatType = RepeatType.ONCE,
                ringtoneUri = alarm?.ringtoneUri,
                ringtoneName = alarm?.ringtoneName ?: "系统默认铃声",
                isEnabled = true
            )
            AlarmScheduler(context).schedule(snoozeAlarm)
            return
        }

        if (action == "com.example.customalarm.ALARM_DISMISS") {
            AlarmSoundPlayer.stop()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(alarmId.toInt())
            return
        }

        // 默认闹钟触发处理
        val storage = AlarmStorage(context)
        val alarm = storage.getAllAlarms().find { it.id == alarmId }
        val durationMinutes = alarm?.ringDurationMinutes ?: 5

        // 播放音频（支持本地自定义音频或系统默认）
        val soundUri = if (!alarm?.ringtoneUri.isNullOrBlank()) {
            Uri.parse(alarm?.ringtoneUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        AlarmSoundPlayer.play(context, soundUri)

        // 超时自动停止播放（防止手机电量耗尽）
        Handler(Looper.getMainLooper()).postDelayed({
            AlarmSoundPlayer.stop()
        }, durationMinutes.toLong() * 60 * 1000L)

        showNotification(context, alarmId, alarmTitle, durationMinutes)

        if (alarmId != -1L && alarm != null) {
            if (alarm.repeatType == RepeatType.ONCE) {
                storage.updateAlarmStatus(alarmId, isEnabled = false)
            } else {
                val nextTime = AlarmCalculator.calculateNextTriggerTime(alarm, System.currentTimeMillis() + 1000)
                storage.updateAlarmStatus(alarmId, isEnabled = true, nextTime = nextTime)
                AlarmScheduler(context).schedule(alarm.copy(targetTimestampMillis = nextTime))
            }
        }
    }

    private fun showNotification(context: Context, alarmId: Long, title: String, durationMinutes: Int) {
        val channelId = "custom_alarm_channel_beijing"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "自定义高优先级闹钟 (北京时间)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "精确按照北京时间触发，支持本地音频与静音穿透"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.customalarm.ALARM_DISMISS"
            putExtra("ALARM_ID", alarmId)
        }
        val pendingDismiss = PendingIntent.getBroadcast(
            context,
            (alarmId + 10000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.customalarm.ALARM_SNOOZE"
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", title)
        }
        val pendingSnooze = PendingIntent.getBroadcast(
            context,
            (alarmId + 20000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeoutMillis = durationMinutes.toLong() * 60 * 1000L

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ " + title)
            .setContentText("到达预定时间 (持续响铃${durationMinutes}分钟)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(timeoutMillis)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭闹钟", pendingDismiss)
            .addAction(android.R.drawable.ic_popup_reminder, "稍后提醒 (10分钟)", pendingSnooze)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }
}
