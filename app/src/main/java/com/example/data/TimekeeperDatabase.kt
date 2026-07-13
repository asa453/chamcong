package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "attendance_settings")
data class AttendanceSettings(
    @PrimaryKey val id: Int = 1,
    val basicSalary: Double = 6000000.0,
    val attendanceBonus: Double = 500000.0,
    val standardDays: Int = 26,
    val overtimeMultiplierWeekday: Double = 1.5,
    val overtimeMultiplierSunday: Double = 2.0,
    val alarmHour: Int = 7,
    val alarmMinute: Int = 30
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey val dateString: String, // format "yyyy-MM-dd"
    val status: String, // "DI_LAM", "NGHI_TUAN", "NGHI_CHO_VIEC", "NGHI_PHEP", "NGHI_VIEC_RIENG"
    val actualHours: Double = 8.0,
    val overtimeWeekday: Double = 0.0,
    val overtimeSunday: Double = 0.0,
    val note: String = ""
)

// 2. DAOs
@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AttendanceSettings?>

    @Query("SELECT * FROM attendance_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AttendanceSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AttendanceSettings)

    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString LIMIT 1")
    suspend fun getRecordForDate(dateString: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE dateString LIKE :monthPattern ORDER BY dateString ASC")
    fun getRecordsForMonthFlow(monthPattern: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE dateString = :dateString")
    suspend fun deleteRecordByDate(dateString: String)
}

// 3. Database
@Database(entities = [AttendanceSettings::class, AttendanceRecord::class], version = 1, exportSchema = false)
abstract class TimekeeperDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: TimekeeperDatabase? = null

        fun getDatabase(context: Context): TimekeeperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimekeeperDatabase::class.java,
                    "timekeeper_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
