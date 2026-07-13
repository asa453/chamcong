package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AttendanceRecord
import com.example.data.AttendanceSettings
import com.example.data.TimekeeperDatabase
import com.example.data.TimekeeperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Alarm received with action: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TimekeeperDatabase.getDatabase(context)
                val repository = TimekeeperRepository(db.attendanceDao())
                val settings = repository.getSettings() ?: AttendanceSettings()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // Reschedule for next days on morning/evening triggers
                if (action == AlarmHelper.ACTION_MORNING_ALARM || action == AlarmHelper.ACTION_EVENING_ALARM) {
                    AlarmHelper.scheduleDailyAlarm(context, settings.alarmHour, settings.alarmMinute)
                }

                when (action) {
                    AlarmHelper.ACTION_MORNING_ALARM -> {
                        // Save morning start status
                        val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("morning_started_$today", true).apply()

                        // Post informative notification only! No UI launch or overlay!
                        showInformativeNotification(
                            context = context,
                            title = "⏰ BẮT ĐẦU TÍNH GIỜ LÀM VIỆC",
                            content = "Giờ làm việc đã bắt đầu lúc 7:30 sáng hôm nay!",
                            notificationId = 4847
                        )
                    }

                    AlarmHelper.ACTION_EVENING_ALARM, AlarmHelper.ACTION_SNOOZE_REMINDER -> {
                        // 2. Evening Clock-out alarm
                        // Check if they already clocked out manually
                        val isTestAlarm = intent.getBooleanExtra("is_test_alarm", false)
                        val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)
                        val isSubmitted = prefs.getBoolean("evening_submitted_$today", false)

                        if (isTestAlarm || !isSubmitted) {
                            // Prepare intent to launch MainActivity in attendance overlay mode
                            val mainIntent = Intent(context, MainActivity::class.java).apply {
                                this.action = MainActivity.ACTION_TRIGGER_ATTENDANCE
                                putExtra(MainActivity.EXTRA_TRIGGER_ATTENDANCE, true)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }

                            // Launch the app
                            try {
                                context.startActivity(mainIntent)
                            } catch (e: Exception) {
                                Log.e("AlarmReceiver", "Failed to launch main activity: ${e.message}")
                            }

                            // Show high-priority overlay notification
                            showFullScreenNotification(
                                context = context,
                                title = "⏰ ĐẾN GIỜ CHẤM CÔNG TỐI (20h00)!",
                                content = "Vui lòng chọn mốc thời gian kết thúc làm việc ngày hôm nay.",
                                fullScreenIntent = mainIntent,
                                notificationId = 4848
                            )

                            // Schedule 1-minute Snooze Check
                            AlarmHelper.scheduleSnoozeCheck(context)
                        }
                    }

                    AlarmHelper.ACTION_SNOOZE_CHECK -> {
                        // 3. Snooze Check: After 1 minute, check if they did attendance
                        val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)
                        val isSubmitted = prefs.getBoolean("evening_submitted_$today", false)

                        if (!isSubmitted) {
                            Log.d("AlarmReceiver", "User did not submit attendance in 1 minute. Snoozing for 30 minutes.")
                            
                            // Post snooze reminder notification
                            showInformativeNotification(
                                context = context,
                                title = "⚠️ CHƯA CHẤM CÔNG HÔM NAY",
                                content = "Bạn chưa chấm công tối nay! Hệ thống sẽ nhắc lại sau 30 phút nữa.",
                                notificationId = 4849
                            )

                            // Clear original full-screen notification to avoid confusion
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(4848)

                            // Schedule Snooze Reminder 30 minutes later
                            AlarmHelper.scheduleSnoozeReminder(context)
                        } else {
                            Log.d("AlarmReceiver", "User has already submitted attendance. No snooze needed.")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error in background processing", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showInformativeNotification(context: Context, title: String, content: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "attendance_info_channel"
        val channelName = "Thông Báo Chấm Công"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh thông báo thông tin chấm công"
                enableLights(true)
                enableVibration(true)
                
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun showFullScreenNotification(
        context: Context,
        title: String,
        content: String,
        fullScreenIntent: Intent,
        notificationId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "attendance_alarm_channel"
        val channelName = "Báo Thức Chấm Công"

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            2424,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo khẩn cấp chấm công tối"
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))

        notificationManager.notify(notificationId, builder.build())
    }
}
