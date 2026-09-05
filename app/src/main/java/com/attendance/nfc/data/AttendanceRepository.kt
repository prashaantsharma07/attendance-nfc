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
    data class Success(val student: Student, val alreadyMarkedToday: Boolean) : MarkResult()
    object UnknownCard : MarkResult()
}

class AttendanceRepository(db: AppDatabase) {

    private val studentDao = db.studentDao()
    private val classDao = db.classDao()
    private val attendanceDao = db.attendanceDao()

    fun getAllClasses(): Flow<List<SchoolClass>> = classDao.getAll()

    fun getAllStudents(): Flow<List<Student>> = studentDao.getAll()

    suspend fun getClassById(classId: Long): SchoolClass? = classDao.getById(classId)

    suspend fun createClass(name: String): Long = classDao.insert(SchoolClass(name = name.trim()))

    suspend fun findStudentByRfid(rfid: String): Student? = studentDao.findByRfid(rfid)

    suspend fun registerStudent(rfid: String, name: String, rollNo: String): Long =
        studentDao.insert(Student(rfidUid = rfid, name = name.trim(), rollNo = rollNo.trim()))

    /**
     * Looks up the scanned RFID. If the student exists, marks them present
     * for [classId] today (no-op if already marked). If the card is unknown,
     * returns UnknownCard so the UI can route to registration.
     */
    suspend fun scanAndMark(classId: Long, rfid: String): MarkResult {
        val student = studentDao.findByRfid(rfid) ?: return MarkResult.UnknownCard
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
        return MarkResult.Success(student, alreadyMarked)
    }

    /** Registers a brand-new card and immediately marks it present for [classId]. */
    suspend fun registerAndMark(classId: Long, rfid: String, name: String, rollNo: String): Student {
        val id = registerStudent(rfid, name, rollNo)
        val student = Student(id = id, rfidUid = rfid, name = name.trim(), rollNo = rollNo.trim())
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
