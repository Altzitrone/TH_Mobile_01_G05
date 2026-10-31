package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thuchanh2.th_mobile.data.model.StudySchedule
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: StudySchedule): Long

    @Update
    suspend fun updateSchedule(schedule: StudySchedule)

    @Delete
    suspend fun deleteSchedule(schedule: StudySchedule)

    @Query("SELECT * FROM study_schedules WHERE studentId = :studentId ORDER BY studyDate ASC, startTime ASC")
    fun getSchedulesByStudent(studentId: Long): Flow<List<StudySchedule>>

    @Query("""
        SELECT s.id, s.studentId, s.subjectId, sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime, s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId AND s.studyDate = :date
        ORDER BY s.startTime ASC
    """)
    fun getSchedulesByDate(studentId: Long, date: String): Flow<List<StudyScheduleWithSubject>>

    @Query("""
        SELECT s.id, s.studentId, s.subjectId, sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime, s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId AND s.studyDate BETWEEN :startDate AND :endDate
        ORDER BY s.studyDate ASC, s.startTime ASC
    """)
    fun getSchedulesByDateRange(
        studentId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<StudyScheduleWithSubject>>

    @Query("""
        SELECT s.id, s.studentId, s.subjectId, sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime, s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId
        ORDER BY s.studyDate ASC, s.startTime ASC
    """)
    fun getAllSchedulesWithSubject(studentId: Long): Flow<List<StudyScheduleWithSubject>>

    // Check conflict
    @Query("""
        SELECT COUNT(*) FROM study_schedules
        WHERE studentId = :studentId AND studyDate = :date AND status != 'Cancelled'
          AND id != :excludeScheduleId
          AND ((startTime < :endTime AND endTime > :startTime))
    """)
    suspend fun checkConflict(
        studentId: Long,
        date: String,
        startTime: String,
        endTime: String,
        excludeScheduleId: Long = 0
    ): Int
}
