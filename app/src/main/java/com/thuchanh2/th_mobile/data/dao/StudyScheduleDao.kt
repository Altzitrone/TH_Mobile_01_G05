package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: StudySchedule): Long

    @Update
    suspend fun update(schedule: StudySchedule)

    @Delete
    suspend fun delete(schedule: StudySchedule)

    @Query("SELECT * FROM study_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): StudySchedule?

    // Lấy toàn bộ lịch học của 1 sinh viên, sắp xếp giảm dần theo ngày và giờ
    @Query("SELECT * FROM study_schedules WHERE studentId = :studentId ORDER BY studyDate DESC, startTime DESC")
    fun getAllByStudentId(studentId: Long): Flow<List<StudySchedule>>

    // Tìm kiếm lịch học theo địa điểm hoặc môn học (cần JOIN) hoặc trạng thái.
    // Ở đây search đơn giản theo location hoặc status.
    @Query("SELECT * FROM study_schedules WHERE studentId = :studentId AND (location LIKE '%' || :searchQuery || '%' OR status = :searchQuery) ORDER BY studyDate DESC")
    fun search(studentId: Long, searchQuery: String): Flow<List<StudySchedule>>

    // Kiểm tra trùng lịch: 
    // Trùng lịch khi: Ngày giống nhau, Trạng thái KHÔNG phải CANCELLED, và (StartTime_Mới < EndTime_Cũ VÀ EndTime_Mới > StartTime_Cũ)
    @Query("""
        SELECT * FROM study_schedules 
        WHERE studentId = :studentId 
        AND studyDate = :date 
        AND status != 'CANCELLED' 
        AND (startTime < :endTime AND endTime > :startTime)
    """)
    suspend fun checkConflict(studentId: Long, date: String, startTime: String, endTime: String): List<StudySchedule>
    
    // Kiểm tra trùng lịch khi Update (loại trừ chính id đang update)
    @Query("""
        SELECT * FROM study_schedules 
        WHERE studentId = :studentId 
        AND id != :scheduleId
        AND studyDate = :date 
        AND status != 'CANCELLED' 
        AND (startTime < :endTime AND endTime > :startTime)
    """)
    suspend fun checkConflictForUpdate(scheduleId: Long, studentId: Long, date: String, startTime: String, endTime: String): List<StudySchedule>
}
