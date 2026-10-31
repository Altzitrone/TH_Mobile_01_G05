package com.thuchanh2.th_mobile.repository

import com.thuchanh2.th_mobile.data.dao.StudyScheduleDao
import com.thuchanh2.th_mobile.data.entity.StudySchedule

/** Identity is the authenticated Account ID, not a fixed Student ID. */
class StudyScheduleRepository(private val dao: StudyScheduleDao, private val accountId: Long) {
    fun search(query: String, status: String) = dao.search(accountId, query, status)
    suspend fun subjects() = dao.availableSubjects(accountId)
    suspend fun currentStudentId() = dao.studentIdForAccount(accountId)
    suspend fun getById(id: Long) = dao.getById(accountId, id)
    suspend fun save(schedule: StudySchedule): Long = if (schedule.id == 0L) dao.create(accountId, schedule)
        else { dao.update(accountId, schedule); schedule.id }
    suspend fun delete(id: Long) = dao.delete(accountId, id)
    suspend fun changeStatus(id: Long, status: String) = dao.changeStatus(accountId, id, status)
}
