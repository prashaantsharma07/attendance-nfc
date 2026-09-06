package com.attendance.nfc.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["rfidUid"], unique = true),
        Index(value = ["barcode"], unique = true)
    ]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rfidUid: String? = null,
    val barcode: String? = null,
    val name: String,
    val rollNo: String
)
