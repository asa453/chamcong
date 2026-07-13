package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimekeeperRepository(private val attendanceDao: AttendanceDao) {

    // Default settings to return if none is saved yet
    private val defaultSettings = AttendanceSettings()

    fun getSettingsFlow(): Flow<AttendanceSettings> {
        return attendanceDao.getSettingsFlow().map { it ?: defaultSettings }
    }

    suspend fun getSettings(): AttendanceSettings {
        return attendanceDao.getSettings() ?: defaultSettings
    }

    suspend fun saveSettings(settings: AttendanceSettings) {
        attendanceDao.saveSettings(settings)
    }

    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAllRecordsFlow()
    }

    suspend fun getRecordForDate(dateString: String): AttendanceRecord? {
        return attendanceDao.getRecordForDate(dateString)
    }

    fun getRecordsForMonthFlow(month: String): Flow<List<AttendanceRecord>> {
        // month is expected to be in "yyyy-MM" format, so pattern is "yyyy-MM-%"
        return attendanceDao.getRecordsForMonthFlow("$month-%")
    }

    suspend fun saveRecord(record: AttendanceRecord) {
        attendanceDao.insertRecord(record)
    }

    suspend fun deleteRecordByDate(dateString: String) {
        attendanceDao.deleteRecordByDate(dateString)
    }
}
