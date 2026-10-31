package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thuchanh2.th_mobile.data.model.StudySchedule
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {

    // ==================== CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: StudySchedule): Long

    @Update
    suspend fun updateSchedule(schedule: StudySchedule)

    @Delete
    suspend fun deleteSchedule(schedule: StudySchedule)

    @Query("SELECT * FROM study_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): StudySchedule?


    // ==================== GET SCHEDULES ====================

    // Lấy tất cả lịch học của một sinh viên
    @Query("""
        SELECT * FROM study_schedules
        WHERE studentId = :studentId
        ORDER BY studyDate ASC, startTime ASC
    """)
    fun getSchedulesByStudent(
        studentId: Long
    ): Flow<List<StudySchedule>>


    // Lấy lịch học trong một ngày, kèm thông tin môn học
    @Query("""
        SELECT s.id, s.studentId, s.subjectId,
               sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime,
               s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId
          AND s.studyDate = :date
        ORDER BY s.startTime ASC
    """)
    fun getSchedulesByDate(
        studentId: Long,
        date: String
    ): Flow<List<StudyScheduleWithSubject>>


    // Lấy lịch học trong một khoảng thời gian (Calendar)
    @Query("""
        SELECT s.id, s.studentId, s.subjectId,
               sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime,
               s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId
          AND s.studyDate BETWEEN :startDate AND :endDate
        ORDER BY s.studyDate ASC, s.startTime ASC
    """)
    fun getSchedulesByDateRange(
        studentId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<StudyScheduleWithSubject>>


    // Lấy tất cả lịch học kèm thông tin môn học
    @Query("""
        SELECT s.id, s.studentId, s.subjectId,
               sub.subjectCode, sub.subjectName,
               s.studyDate, s.startTime, s.endTime,
               s.location, s.status
        FROM study_schedules s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.studentId = :studentId
        ORDER BY s.studyDate ASC, s.startTime ASC
    """)
    fun getAllSchedulesWithSubject(
        studentId: Long
    ): Flow<List<StudyScheduleWithSubject>>


    // ==================== SEARCH ====================

    // Tìm kiếm theo địa điểm hoặc trạng thái
    @Query("""
        SELECT * FROM study_schedules
        WHERE studentId = :studentId
          AND (
              location LIKE '%' || :searchQuery || '%'
              OR status = :searchQuery
          )
        ORDER BY studyDate DESC, startTime DESC
    """)
    fun search(
        studentId: Long,
        searchQuery: String
    ): Flow<List<StudySchedule>>


    // ==================== CONFLICT VALIDATION ====================

    // Kiểm tra trùng lịch khi thêm mới hoặc cập nhật.
    // excludeScheduleId = 0: thêm mới.
    // excludeScheduleId = ID hiện tại: cập nhật.
    @Query("""
        SELECT COUNT(*) FROM study_schedules
        WHERE studentId = :studentId
          AND studyDate = :date
          AND status != 'CANCELLED'
          AND id != :excludeScheduleId
          AND (
              startTime < :endTime
              AND endTime > :startTime
          )
    """)
    suspend fun checkConflict(
        studentId: Long,
        date: String,
        startTime: String,
        endTime: String,
        excludeScheduleId: Long = 0
    ): Int

}