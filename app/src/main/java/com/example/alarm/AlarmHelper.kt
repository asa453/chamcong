package com.example.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AlarmHelper {
    private const val TAG = "AlarmHelper"

    const val REQ_CODE_MORNING = 9981
    const val REQ_CODE_EVENING = 9982
    const val REQ_CODE_SNOOZE_CHECK = 9983
    const val REQ_CODE_SNOOZE_REMINDER = 9984

    const val ACTION_MORNING_ALARM = "com.example.alarm.ACTION_MORNING_ALARM"
    const val ACTION_EVENING_ALARM = "com.example.alarm.ACTION_EVENING_ALARM"
    const val ACTION_SNOOZE_CHECK = "com.example.alarm.ACTION_SNOOZE_CHECK"
    const val ACTION_SNOOZE_REMINDER = "com.example.alarm.ACTION_SNOOZE_REMINDER"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyAlarm(context: Context, hour: Int = 7, minute: Int = 30) {
        // Schedule both Morning (7:30 AM or customized) and Evening (20:00)
        scheduleAlarm(context, ACTION_MORNING_ALARM, REQ_CODE_MORNING, hour, minute)
        scheduleAlarm(context, ACTION_EVENING_ALARM, REQ_CODE_EVENING, 20, 0)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, action: String, requestCode: Int, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed for today, set it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d(TAG, "Scheduling alarm for $action at $hour:$minute. Next trigger: ${calendar.time}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for action $action, fallback", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleTestAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_EVENING_ALARM
            putExtra("is_test_alarm", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999, // use a different request code for testing to avoid conflicts
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 5 * 1000 // 5 seconds from now
        Log.d(TAG, "Scheduling test alarm in 5 seconds.")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnoozeCheck(context: Context) {
        // Schedule a check 1 minute from now
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_SNOOZE_CHECK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 60 * 1000 // 1 minute in ms
        Log.d(TAG, "Scheduling snooze check in 1 minute.")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnoozeReminder(context: Context) {
        // Schedule a reminder 30 minutes from now
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_SNOOZE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 30 * 60 * 1000 // 30 minutes in ms
        Log.d(TAG, "Scheduling snooze reminder in 30 minutes.")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelSnoozeAndCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel Snooze Check
        val checkIntent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SNOOZE_CHECK }
        val checkPending = PendingIntent.getBroadcast(
            context, REQ_CODE_SNOOZE_CHECK, checkIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (checkPending != null) {
            alarmManager.cancel(checkPending)
            checkPending.cancel()
        }

        // Cancel Snooze Reminder
        val reminderIntent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SNOOZE_REMINDER }
        val reminderPending = PendingIntent.getBroadcast(
            context, REQ_CODE_SNOOZE_REMINDER, reminderIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (reminderPending != null) {
            alarmManager.cancel(reminderPending)
            reminderPending.cancel()
        }
        Log.d(TAG, "Cancelled snooze checks and snooze reminders")
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        cancelSpecific(context, alarmManager, ACTION_MORNING_ALARM, REQ_CODE_MORNING)
        cancelSpecific(context, alarmManager, ACTION_EVENING_ALARM, REQ_CODE_EVENING)
        cancelSnoozeAndCheck(context)
    }

    private fun cancelSpecific(context: Context, alarmManager: AlarmManager, action: String, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
