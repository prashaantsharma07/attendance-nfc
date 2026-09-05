package com.attendance.nfc.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row = one student marked present in one class on one day.
 * The unique index on (classId, studentId, dateKey) is what stops a
 * second tap of the same card in the same class-day from double-counting.
 */
@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["classId", "studentId", "dateKey"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val studentId: Long,
    val dateKey: String, // yyyy-MM-dd, local date the session was held
    val timestamp: Long
)
