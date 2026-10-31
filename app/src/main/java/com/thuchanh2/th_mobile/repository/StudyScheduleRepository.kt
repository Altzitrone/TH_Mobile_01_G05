package com.thuchanh2.th_mobile.repository

import com.thuchanh2.th_mobile.data.dao.StudyScheduleDao
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import kotlinx.coroutines.flow.Flow

class StudyScheduleRepository(private val dao: StudyScheduleDao) {

    suspend fun insert(schedule: StudySchedule) = dao.insert(schedule)

    suspend fun update(schedule: StudySchedule) = dao.update(schedule)

    suspend fun delete(schedule: StudySchedule) = dao.delete(schedule)
    
    suspend fun getById(id: Long) = dao.getById(id)

    fun getAllByStudentId(studentId: Long): Flow<List<StudySchedule>> = dao.getAllByStudentId(studentId)

    fun search(studentId: Long, query: String): Flow<List<StudySchedule>> = dao.search(studentId, query)

    suspend fun checkConflict(studentId: Long, date: String, startTime: String, endTime: String): Boolean {
        val conflicts = dao.checkConflict(studentId, date, startTime, endTime)
        return conflicts.isNotEmpty()
    }
    
    suspend fun checkConflictForUpdate(scheduleId: Long, studentId: Long, date: String, startTime: String, endTime: String): Boolean {
        val conflicts = dao.checkConflictForUpdate(scheduleId, studentId, date, startTime, endTime)
        return conflicts.isNotEmpty()
    }
}
