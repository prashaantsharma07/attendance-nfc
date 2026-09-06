package com.attendance.nfc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Student::class, SchoolClass::class, AttendanceRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun classDao(): ClassDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS students_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        rfidUid TEXT,
                        barcode TEXT,
                        name TEXT NOT NULL,
                        rollNo TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO students_new (id, rfidUid, barcode, name, rollNo)
                    SELECT id, rfidUid, NULL, name, rollNo FROM students
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE students")
                db.execSQL("ALTER TABLE students_new RENAME TO students")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_students_rfidUid ON students(rfidUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_students_barcode ON students(barcode)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_students_rollNo ON students(rollNo)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_students_rollNo ON students(rollNo)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
