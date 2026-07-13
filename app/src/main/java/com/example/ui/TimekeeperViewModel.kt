package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmHelper
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TimekeeperViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TimekeeperDatabase.getDatabase(application)
    private val repository = TimekeeperRepository(database.attendanceDao())

    // 1. App State
    val settings: StateFlow<AttendanceSettings> = repository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttendanceSettings()
        )

    private val _currentMonth = MutableStateFlow(getCurrentMonthString())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    // 2. Records for the selected month
    val records: StateFlow<List<AttendanceRecord>> = _currentMonth
        .flatMapLatest { month ->
            repository.getRecordsForMonthFlow(month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 3. UI Overlay control
    var isOverlayShowing by mutableStateOf(false)
        private set
    var overlayDateString by mutableStateOf<String?>(null)
        private set

    fun showOverlay(dateString: String? = null) {
        overlayDateString = dateString
        isOverlayShowing = true
    }

    fun dismissOverlay() {
        isOverlayShowing = false
        overlayDateString = null
    }

    // 4. Calculations derived from current state
    val salaryCalculation: StateFlow<SalarySummary> = combine(settings, records) { currentSettings, list ->
        calculateSalary(currentSettings, list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalarySummary()
    )

    // 5. Actions
    fun setMonth(month: String) {
        _currentMonth.value = month
    }

    fun saveSettings(
        basicSalary: Double,
        attendanceBonus: Double,
        standardDays: Int,
        alarmHour: Int,
        alarmMinute: Int
    ) {
        viewModelScope.launch {
            val updatedSettings = AttendanceSettings(
                id = 1,
                basicSalary = basicSalary,
                attendanceBonus = attendanceBonus,
                standardDays = standardDays,
                overtimeMultiplierWeekday = 1.5,
                overtimeMultiplierSunday = 2.0,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute
            )
            repository.saveSettings(updatedSettings)
            
            // Re-schedule alarm with new time
            AlarmHelper.cancelAlarm(getApplication())
            AlarmHelper.scheduleDailyAlarm(getApplication(), alarmHour, alarmMinute)
        }
    }

    fun saveAttendance(
        dateString: String,
        status: String,
        actualHours: Double,
        note: String
    ) {
        viewModelScope.launch {
            val isDaySunday = isSunday(dateString)
            var otWeekday = 0.0
            var otSunday = 0.0

            if (status == "DI_LAM") {
                if (isDaySunday) {
                    // All hours on Sunday are Sunday overtime
                    otSunday = actualHours
                } else {
                    // Regular weekday overtime
                    if (actualHours > 8.0) {
                        otWeekday = actualHours - 8.0
                    }
                }
            }

            val record = AttendanceRecord(
                dateString = dateString,
                status = status,
                actualHours = actualHours,
                overtimeWeekday = otWeekday,
                overtimeSunday = otSunday,
                note = note
            )
            repository.saveRecord(record)
        }
    }

    fun deleteAttendance(dateString: String) {
        viewModelScope.launch {
            repository.deleteRecordByDate(dateString)
        }
    }

    fun ensureAlarmScheduled() {
        viewModelScope.launch {
            val s = repository.getSettings()
            AlarmHelper.scheduleDailyAlarm(getApplication(), s.alarmHour, s.alarmMinute)
        }
    }

    fun triggerTestAlarm() {
        AlarmHelper.scheduleTestAlarm(getApplication())
    }

    // --- Helpers ---
    private fun getCurrentMonthString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun isSunday(dateString: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateString)
            val cal = Calendar.getInstance()
            if (date != null) {
                cal.time = date
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateSalary(settings: AttendanceSettings, list: List<AttendanceRecord>): SalarySummary {
        val lcb = settings.basicSalary
        val cc = settings.attendanceBonus
        val n = settings.standardDays.toDouble()
        
        val dailyStandardPay = if (n > 0) lcb / n else 0.0
        val hourlyStandardPay = dailyStandardPay / 8.0

        var daysWorked = 0.0
        var paidLeaveDays = 0.0
        var waitingForWorkDays = 0.0
        var regularDayOffDays = 0.0
        var unpaidLeaveDays = 0.0

        var totalWeekdayOvertime = 0.0
        var totalSundayOvertime = 0.0
        var hasPersonalLeave = false

        for (record in list) {
            when (record.status) {
                "DI_LAM" -> {
                    if (isSunday(record.dateString)) {
                        totalSundayOvertime += record.overtimeSunday
                    } else {
                        daysWorked += 1.0
                        totalWeekdayOvertime += record.overtimeWeekday
                    }
                }
                "NGHI_PHEP" -> {
                    paidLeaveDays += 1.0
                }
                "NGHI_CHO_VIEC" -> {
                    waitingForWorkDays += 1.0
                }
                "NGHI_TUAN" -> {
                    regularDayOffDays += 1.0
                }
                "NGHI_VIEC_RIENG" -> {
                    unpaidLeaveDays += 1.0
                    hasPersonalLeave = true
                }
            }
        }

        val standardPay = (daysWorked + paidLeaveDays) * dailyStandardPay
        val otWeekdayPay = totalWeekdayOvertime * hourlyStandardPay * settings.overtimeMultiplierWeekday
        val otSundayPay = totalSundayOvertime * hourlyStandardPay * settings.overtimeMultiplierSunday
        val finalAttendanceBonus = if (hasPersonalLeave) 0.0 else cc

        // Work day allowances
        val physicalDaysWorked = list.count { it.status == "DI_LAM" }.toDouble()
        val gasolineAllowanceValue = physicalDaysWorked * (200000.0 / 26.0)
        val mealAllowanceValue = physicalDaysWorked * 20000.0

        val totalSalary = standardPay + otWeekdayPay + otSundayPay + finalAttendanceBonus + gasolineAllowanceValue + mealAllowanceValue

        return SalarySummary(
            totalSalary = totalSalary,
            daysWorked = daysWorked,
            paidLeaveDays = paidLeaveDays,
            waitingForWorkDays = waitingForWorkDays,
            regularDayOffDays = regularDayOffDays,
            unpaidLeaveDays = unpaidLeaveDays,
            totalWeekdayOvertimeHours = totalWeekdayOvertime,
            totalSundayOvertimeHours = totalSundayOvertime,
            weekdayOvertimePay = otWeekdayPay,
            sundayOvertimePay = otSundayPay,
            standardPay = standardPay,
            isAttendanceBonusActive = !hasPersonalLeave,
            attendanceBonusValue = finalAttendanceBonus,
            physicalDaysWorked = physicalDaysWorked,
            gasolineAllowance = gasolineAllowanceValue,
            mealAllowance = mealAllowanceValue
        )
    }
}

data class SalarySummary(
    val totalSalary: Double = 0.0,
    val daysWorked: Double = 0.0,
    val paidLeaveDays: Double = 0.0,
    val waitingForWorkDays: Double = 0.0,
    val regularDayOffDays: Double = 0.0,
    val unpaidLeaveDays: Double = 0.0,
    val totalWeekdayOvertimeHours: Double = 0.0,
    val totalSundayOvertimeHours: Double = 0.0,
    val weekdayOvertimePay: Double = 0.0,
    val sundayOvertimePay: Double = 0.0,
    val standardPay: Double = 0.0,
    val isAttendanceBonusActive: Boolean = true,
    val attendanceBonusValue: Double = 0.0,
    val physicalDaysWorked: Double = 0.0,
    val gasolineAllowance: Double = 0.0,
    val mealAllowance: Double = 0.0
)
