package com.attendance.nfc.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {

    @Insert
    suspend fun insert(schoolClass: SchoolClass): Long

    @Query("SELECT * FROM classes ORDER BY name ASC")
    fun getAll(): Flow<List<SchoolClass>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getById(id: Long): SchoolClass?
}
