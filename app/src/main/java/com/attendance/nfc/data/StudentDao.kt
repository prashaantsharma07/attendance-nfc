package com.attendance.nfc.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: Student): Long

    @androidx.room.Update
    suspend fun update(student: Student)

    @Query("SELECT * FROM students WHERE rfidUid = :rfid LIMIT 1")
    suspend fun findByRfid(rfid: String): Student?

    @Query("SELECT * FROM students WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Student?

    @Query("SELECT * FROM students WHERE (rfidUid IS NOT NULL AND rfidUid = :identifier) OR (barcode IS NOT NULL AND barcode = :identifier) LIMIT 1")
    suspend fun findByIdentifier(identifier: String): Student?

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAll(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getById(id: Long): Student?
}
