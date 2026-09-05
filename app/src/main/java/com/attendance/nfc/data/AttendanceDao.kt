package com.attendance.nfc.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttendanceDao {

    /**
     * IGNORE, not REPLACE: if this (class, student, date) row already exists
     * (card tapped twice today) the insert silently no-ops instead of
     * creating a duplicate "present" record.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: AttendanceRecord): Long

    @Query("SELECT DISTINCT dateKey FROM attendance_records WHERE classId = :classId")
    suspend fun getSessionDates(classId: Long): List<String>

    @Query(
        "SELECT COUNT(*) FROM attendance_records WHERE classId = :classId AND studentId = :studentId"
    )
    suspend fun getPresentCount(classId: Long, studentId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM attendance_records WHERE classId = :classId AND studentId = :studentId AND dateKey = :dateKey"
    )
    suspend fun isPresentOn(classId: Long, studentId: Long, dateKey: String): Int
}
