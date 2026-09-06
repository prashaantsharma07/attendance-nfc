package com.attendance.nfc.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudentAttendanceSummary(
    val student: Student,
    val presentCount: Int,
    val totalSessions: Int
) {
    val percentage: Float
        get() = if (totalSessions == 0) 0f else (presentCount.toFloat() / totalSessions) * 100f
}

sealed class MarkResult {
    data class Success(
        val student: Student,
        val alreadyMarkedToday: Boolean,
        val method: String = "NFC"
    ) : MarkResult()
    object UnknownCard : MarkResult()
    data class UnknownBarcode(val barcode: String) : MarkResult()
}

class AttendanceRepository(db: AppDatabase) {

    private val studentDao = db.studentDao()
    private val classDao = db.classDao()
    private val attendanceDao = db.attendanceDao()

    fun getAllClasses(): Flow<List<SchoolClass>> = classDao.getAll()

    fun getAllStudents(): Flow<List<Student>> = studentDao.getAll()

    suspend fun getClassById(classId: Long): SchoolClass? = classDao.getById(classId)

    suspend fun createClass(name: String): Long = classDao.insert(SchoolClass(name = name.trim()))

    suspend fun findStudentByRfid(rfid: String): Student? = studentDao.findByRfid(rfid.trim())

    suspend fun findStudentByBarcode(barcode: String): Student? = studentDao.findByBarcode(barcode.trim())

    suspend fun findStudentByIdentifier(identifier: String): Student? =
        studentDao.findByIdentifier(identifier.trim())

    suspend fun registerStudent(
        name: String,
        rollNo: String,
        rfid: String? = null,
        barcode: String? = null
    ): Long = studentDao.insert(
        Student(
            rfidUid = rfid?.trim()?.ifBlank { null },
            barcode = barcode?.trim()?.ifBlank { null },
            name = name.trim(),
            rollNo = rollNo.trim()
        )
    )

    suspend fun registerStudent(rfid: String, name: String, rollNo: String): Long =
        registerStudent(name = name, rollNo = rollNo, rfid = rfid, barcode = null)

    /**
     * Looks up the scanned RFID. If the student exists, marks them present
     * for [classId] today (no-op if already marked). If the card is unknown,
     * returns UnknownCard so the UI can route to registration.
     */
    suspend fun scanAndMark(classId: Long, rfid: String): MarkResult {
        val student = studentDao.findByRfid(rfid.trim()) ?: return MarkResult.UnknownCard
        val today = todayKey()
        val alreadyMarked = attendanceDao.isPresentOn(classId, student.id, today) > 0
        if (!alreadyMarked) {
            attendanceDao.insert(
                AttendanceRecord(
                    classId = classId,
                    studentId = student.id,
                    dateKey = today,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return MarkResult.Success(student, alreadyMarked, method = "NFC")
    }

    /**
     * Looks up the scanned barcode. If the student exists, marks them present
     * for [classId] today (no-op if already marked). If the barcode is unknown,
     * returns UnknownBarcode so the UI can route to registration.
     */
    suspend fun scanAndMarkBarcode(classId: Long, barcode: String): MarkResult {
        val cleanBarcode = barcode.trim()
        val student = studentDao.findByBarcode(cleanBarcode)
            ?: studentDao.findByRfid(cleanBarcode)
            ?: return MarkResult.UnknownBarcode(cleanBarcode)

        val today = todayKey()
        val alreadyMarked = attendanceDao.isPresentOn(classId, student.id, today) > 0
        if (!alreadyMarked) {
            attendanceDao.insert(
                AttendanceRecord(
                    classId = classId,
                    studentId = student.id,
                    dateKey = today,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return MarkResult.Success(student, alreadyMarked, method = "Barcode")
    }

    /** Registers a student with RFID and/or Barcode, and marks present for [classId]. */
    suspend fun registerAndMark(
        classId: Long,
        name: String,
        rollNo: String,
        rfid: String? = null,
        barcode: String? = null
    ): Student {
        val id = registerStudent(name = name, rollNo = rollNo, rfid = rfid, barcode = barcode)
        val student = Student(
            id = id,
            rfidUid = rfid?.trim()?.ifBlank { null },
            barcode = barcode?.trim()?.ifBlank { null },
            name = name.trim(),
            rollNo = rollNo.trim()
        )
        attendanceDao.insert(
            AttendanceRecord(
                classId = classId,
                studentId = id,
                dateKey = todayKey(),
                timestamp = System.currentTimeMillis()
            )
        )
        return student
    }

    /** Registers a brand-new card and immediately marks it present for [classId]. */
    suspend fun registerAndMark(classId: Long, rfid: String, name: String, rollNo: String): Student =
        registerAndMark(classId = classId, name = name, rollNo = rollNo, rfid = rfid, barcode = null)

    /**
     * Attendance % per student for a class = sessions they were present for,
     * divided by total distinct days attendance was ever taken for that class.
     * Roster is global across all classes (a student registers once); this is
     * a simplification worth revisiting if you need per-class rosters.
     */
    suspend fun getAttendanceSummary(classId: Long): List<StudentAttendanceSummary> {
        val students = studentDao.getAll().first()
        val totalSessions = attendanceDao.getSessionDates(classId).size
        return students.map { student ->
            StudentAttendanceSummary(
                student = student,
                presentCount = attendanceDao.getPresentCount(classId, student.id),
                totalSessions = totalSessions
            )
        }.sortedByDescending { it.percentage }
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
