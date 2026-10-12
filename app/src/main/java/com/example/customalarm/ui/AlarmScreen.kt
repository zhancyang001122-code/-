package com.example.customalarm.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customalarm.data.AlarmStorage
import com.example.customalarm.model.AlarmItem
import com.example.customalarm.model.RepeatType
import com.example.customalarm.util.AlarmCalculator
import com.example.customalarm.util.AlarmScheduler
import com.example.customalarm.util.AlarmSoundPlayer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(storage: AlarmStorage, scheduler: AlarmScheduler) {
    var alarms by remember { mutableStateOf(storage.getAllAlarms()) }
    var historyList by remember { mutableStateOf(storage.getRecentHistory(8)) }
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<AlarmItem?>(null) }

    fun refresh() {
        alarms = storage.getAllAlarms()
        historyList = storage.getRecentHistory(8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("极简自定义闹钟", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("时区：北京时间 (UTC+8) | 支持法定节假日与本地音乐", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建闹钟", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 模块 1: 快捷历史记录卡片
            if (historyList.isNotEmpty()) {
                item {
                    Text(
                        text = "⚡ 快捷设置（点击直接套用记录）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(historyList) { historyItem ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier
                                    .clickable {
                                        editingItem = historyItem.copy(
                                            id = System.currentTimeMillis(),
                                            isEnabled = true
                                        )
                                        showDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = historyItem.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = formatRepeatText(historyItem),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 模块 2: 闹钟列表
            item {
                Text(
                    text = "⏰ 闹钟列表 (${alarms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (alarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无已设置闹钟，点击右下角 "+" 新建",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { isChecked ->
                            storage.updateAlarmStatus(alarm.id, isChecked)
                            if (isChecked) {
                                scheduler.schedule(alarm.copy(isEnabled = true))
                            } else {
                                scheduler.cancel(alarm.id)
                            }
                            refresh()
                        },
                        onDelete = {
                            scheduler.cancel(alarm.id)
                            storage.deleteAlarm(alarm.id)
                            refresh()
                        },
                        onClick = {
                            editingItem = alarm
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlarmEditDialog(
            initialItem = editingItem,
            onDismiss = {
                AlarmSoundPlayer.stop()
                showDialog = false
            },
            onConfirm = { savedItem ->
                AlarmSoundPlayer.stop()
                storage.saveAlarm(savedItem)
                if (savedItem.isEnabled) {
                    scheduler.schedule(savedItem)
                } else {
                    scheduler.cancel(savedItem.id)
                }
                refresh()
                showDialog = false
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatBeijingTime(alarm.targetTimestampMillis),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRepeatText(alarm) + " · 持续${alarm.ringDurationMinutes}分 · 🎵 ${alarm.ringtoneName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
fun AlarmEditDialog(
    initialItem: AlarmItem?,
    onDismiss: () -> Unit,
    onConfirm: (AlarmItem) -> Unit
) {
    val context = LocalContext.current
    val cal = remember {
        AlarmCalculator.getBeijingCalendar().apply {
            if (initialItem != null) {
                timeInMillis = initialItem.targetTimestampMillis
            } else {
                add(Calendar.MINUTE, 5)
            }
        }
    }

    var title by remember { mutableStateOf(initialItem?.title ?: "自定义闹钟") }
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }
    var repeatType by remember { mutableStateOf(initialItem?.repeatType ?: RepeatType.CHINA_WORKDAYS) }
    var intervalDays by remember { mutableStateOf(initialItem?.intervalDays ?: 2) }
    var ringDuration by remember { mutableStateOf(initialItem?.ringDurationMinutes ?: 5) }

    // 本地音频状态
    var ringtoneUri by remember { mutableStateOf(initialItem?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf(initialItem?.ringtoneName ?: "系统默认铃声") }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // 原生轻量文件选择器 (SAF GetContent, 零额外危险权限)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // 获取持久化读取权限，保证手机重启后依然可读
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 忽略非持久化 URI 异常
            }

            // 获取文件名
            var displayName = "本地音频"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex)
                }
            }
            ringtoneUri = uri.toString()
            ringtoneName = displayName
            AlarmSoundPlayer.stop()
            isPreviewPlaying = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            AlarmSoundPlayer.stop()
            onDismiss()
        },
        title = { Text(if (initialItem == null) "新建自定义闹钟 (北京时间)" else "编辑闹钟 (北京时间)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("闹钟名称 / 备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("北京时间日期与时刻：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { day += 1 }) {
                        Text(String.format(Locale.SIMPLIFIED_CHINESE, "%04d年%02d月%02d日", year, month + 1, day))
                    }
                    OutlinedButton(onClick = { minute = (minute + 5) % 60 }) {
                        Text(String.format(Locale.SIMPLIFIED_CHINESE, "%02d:%02d", hour, minute))
                    }
                }

                Text("循环模式：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        RepeatType.CHINA_WORKDAYS to "🇨🇳 法定工作日（跳过假期、调休上班响铃）",
                        RepeatType.ONCE to "仅此一次（指定具体年月日）",
                        RepeatType.DAILY to "每天响铃",
                        RepeatType.WEEKDAYS to "普通周一至周五",
                        RepeatType.INTERVAL_DAYS to "按间隔天数（每 $intervalDays 天）"
                    ).forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { repeatType = type }
                        ) {
                            RadioButton(
                                selected = repeatType == type,
                                onClick = { repeatType = type }
                            )
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                // 本地铃声选择板块（极简一键点击）
                Text("闹钟铃声：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ringtoneName,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // 试听 / 停止按钮
                            IconButton(onClick = {
                                if (isPreviewPlaying) {
                                    AlarmSoundPlayer.stop()
                                    isPreviewPlaying = false
                                } else {
                                    val sound = if (!ringtoneUri.isNullOrBlank()) Uri.parse(ringtoneUri) else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                                    AlarmSoundPlayer.play(context, sound)
                                    isPreviewPlaying = true
                                }
                            }) {
                                Icon(
                                    if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "试听"
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("选择本地音频", fontSize = 11.sp)
                            }
                            if (ringtoneUri != null) {
                                OutlinedButton(onClick = {
                                    ringtoneUri = null
                                    ringtoneName = "系统默认铃声"
                                    AlarmSoundPlayer.stop()
                                    isPreviewPlaying = false
                                }) {
                                    Text("重置默认", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 持续响铃时长选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("响铃时长：", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    listOf(1, 3, 5, 10).forEach { dur ->
                        FilterChip(
                            selected = ringDuration == dur,
                            onClick = { ringDuration = dur },
                            label = { Text("${dur}分钟") },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                AlarmSoundPlayer.stop()
                val targetCal = AlarmCalculator.getBeijingCalendar().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val newItem = AlarmItem(
                    id = initialItem?.id ?: System.currentTimeMillis(),
                    title = title.ifBlank { "自定义闹钟" },
                    targetTimestampMillis = targetCal.timeInMillis,
                    repeatType = repeatType,
                    intervalDays = intervalDays,
                    ringDurationMinutes = ringDuration,
                    ringtoneUri = ringtoneUri,
                    ringtoneName = ringtoneName,
                    isEnabled = true
                )
                onConfirm(newItem)
            }) {
                Text("保存并激活")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AlarmSoundPlayer.stop()
                onDismiss()
            }) {
                Text("取消")
            }
        }
    )
}

fun formatBeijingTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE).apply {
        timeZone = AlarmCalculator.BEIJING_TIME_ZONE
    }
    return sdf.format(Date(millis)) + " (北京时间)"
}

fun formatRepeatText(alarm: AlarmItem): String {
    val dateSdf = SimpleDateFormat("yyyy年MM月dd日", Locale.SIMPLIFIED_CHINESE).apply {
        timeZone = AlarmCalculator.BEIJING_TIME_ZONE
    }
    val dateStr = dateSdf.format(Date(alarm.targetTimestampMillis))
    return when (alarm.repeatType) {
        RepeatType.CHINA_WORKDAYS -> "法定工作日(智能调休)"
        RepeatType.ONCE -> "指定日期: $dateStr"
        RepeatType.DAILY -> "每天响铃"
        RepeatType.WEEKDAYS -> "工作日(周一至周五)"
        RepeatType.INTERVAL_DAYS -> "每隔 ${alarm.intervalDays} 天"
        RepeatType.CUSTOM_WEEKDAYS -> "自定义周几"
    }
}
