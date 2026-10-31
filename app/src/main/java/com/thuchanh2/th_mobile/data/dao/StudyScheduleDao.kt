package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus
import com.thuchanh2.th_mobile.data.entity.Subject
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

private const val SCHEDULE_SELECT = """SELECT s.*, sub.subjectCode, sub.subjectName
    FROM StudySchedule s JOIN Subject sub ON s.subjectId = sub.id
    JOIN Student st ON st.id = s.studentId JOIN Account a ON a.id = st.accountId """
private const val OWNER = " a.id = :accountId AND a.role = 'Student' "

@Dao
abstract class StudyScheduleDao {
    @Query("SELECT s.id FROM Student s JOIN Account a ON a.id = s.accountId WHERE a.id = :accountId AND a.role = 'Student'")
    abstract suspend fun studentIdForAccount(accountId: Long): Long?

    @Query("""SELECT * FROM Subject WHERE EXISTS(SELECT 1 FROM Student s JOIN Account a
        ON a.id = s.accountId WHERE a.id = :accountId AND a.role = 'Student') ORDER BY subjectName COLLATE NOCASE""")
    abstract suspend fun availableSubjects(accountId: Long): List<Subject>

    @Query(SCHEDULE_SELECT + " WHERE " + OWNER + """ AND
        (:keyword = '' OR instr(lower(sub.subjectName), lower(:keyword)) > 0
        OR instr(lower(sub.subjectCode), lower(:keyword)) > 0 OR instr(lower(s.location), lower(:keyword)) > 0
        OR instr(s.studyDate, :keyword) > 0 OR instr(lower(s.status), lower(:keyword)) > 0)
        AND (:status = '' OR s.status = :status) ORDER BY s.studyDate, s.startTime, s.id""")
    abstract fun search(accountId: Long, keyword: String = "", status: String = ""): Flow<List<StudyScheduleWithSubject>>

    @Query(SCHEDULE_SELECT + " WHERE " + OWNER + " AND s.studyDate = :date ORDER BY s.startTime")
    abstract fun getSchedulesByDate(accountId: Long, date: String): Flow<List<StudyScheduleWithSubject>>

    @Query(SCHEDULE_SELECT + " WHERE " + OWNER + " AND s.studyDate BETWEEN :startDate AND :endDate ORDER BY s.studyDate, s.startTime")
    abstract fun getSchedulesByDateRange(accountId: Long, startDate: String, endDate: String): Flow<List<StudyScheduleWithSubject>>

    @Query("""SELECT s.* FROM StudySchedule s JOIN Student st ON st.id = s.studentId
        JOIN Account a ON a.id = st.accountId WHERE s.id = :id AND a.id = :accountId AND a.role = 'Student'""")
    abstract suspend fun getById(accountId: Long, id: Long): StudySchedule?

    @Query("""SELECT COUNT(*) FROM StudySchedule WHERE studentId = :studentId AND studyDate = :date
        AND status != 'CANCELLED' AND id != :excludeId AND startTime < :endTime AND endTime > :startTime""")
    protected abstract suspend fun countConflicts(studentId: Long, date: String, startTime: String, endTime: String, excludeId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM Subject WHERE id = :id)")
    protected abstract suspend fun subjectExists(id: Long): Boolean

    @Query("""INSERT INTO StudySchedule(studentId, subjectId, studyDate, startTime, endTime, location, status)
        VALUES(:studentId, :subjectId, :date, :start, :end, :location, :status)""")
    protected abstract suspend fun insertRow(studentId: Long, subjectId: Long, date: String, start: String, end: String, location: String, status: String): Long

    @Query("""UPDATE StudySchedule SET subjectId = :subjectId, studyDate = :date, startTime = :start,
        endTime = :end, location = :location, status = :status WHERE id = :id""")
    protected abstract suspend fun updateRow(id: Long, subjectId: Long, date: String, start: String, end: String, location: String, status: String): Int

    @Query("DELETE FROM StudySchedule WHERE id = :id")
    protected abstract suspend fun deleteRow(id: Long): Int

    @Query("DELETE FROM notifications WHERE scheduleId = :id AND type = 'UPCOMING'")
    protected abstract suspend fun clearReminder(id: Long)

    @Query("""INSERT INTO notifications(studentId, title, message, type, timestamp, isRead, scheduleId)
        VALUES(:studentId, :title, :message, 'CHANGE', :timestamp, 0, :scheduleId)""")
    protected abstract suspend fun recordChange(studentId: Long, scheduleId: Long?, title: String, message: String, timestamp: Long): Long

    @Transaction
    open suspend fun create(accountId: Long, schedule: StudySchedule): Long {
        require(schedule.id == 0L) { "Lịch mới không được có ID cũ." }
        validate(accountId, schedule)
        val id = insertRow(schedule.studentId, schedule.subjectId, schedule.studyDate, schedule.startTime, schedule.endTime, schedule.location.trim(), schedule.status)
        recordChange(schedule.studentId, id, "Đặt lịch thành công",
            "${schedule.studyDate} · ${schedule.startTime}–${schedule.endTime} · ${schedule.location}", System.currentTimeMillis())
        return id
    }

    @Transaction
    open suspend fun update(accountId: Long, schedule: StudySchedule) {
        val old = getById(accountId, schedule.id) ?: error("Không tìm thấy lịch học của bạn.")
        require(old.studentId == schedule.studentId) { "Không được đổi chủ sở hữu lịch học." }
        validate(accountId, schedule)
        check(updateRow(schedule.id, schedule.subjectId, schedule.studyDate, schedule.startTime, schedule.endTime, schedule.location.trim(), schedule.status) == 1)
        clearReminder(schedule.id)
        recordChange(schedule.studentId, schedule.id, "Lịch học đã thay đổi",
            "${schedule.studyDate} · ${schedule.startTime}–${schedule.endTime} · ${schedule.location} · ${statusText(schedule.status)}", System.currentTimeMillis())
    }

    @Transaction
    open suspend fun delete(accountId: Long, id: Long) {
        val old = getById(accountId, id) ?: error("Không tìm thấy lịch học của bạn.")
        clearReminder(id)
        check(deleteRow(id) == 1)
        recordChange(old.studentId, null, "Đã xóa lịch học",
            "${old.studyDate} · ${old.startTime}–${old.endTime}", System.currentTimeMillis())
    }

    @Transaction
    open suspend fun changeStatus(accountId: Long, id: Long, status: String) {
        val old = getById(accountId, id) ?: error("Không tìm thấy lịch học của bạn.")
        update(accountId, old.copy(status = status))
    }

    // Used only by the Android reminder receiver, never to authorize a UI action.
    @Query(SCHEDULE_SELECT + " WHERE s.id = :id AND a.role = 'Student'")
    abstract suspend fun forReminder(id: Long): StudyScheduleWithSubject?

    @Query("SELECT * FROM StudySchedule WHERE status != 'CANCELLED'")
    abstract suspend fun activeReminders(): List<StudySchedule>

    private fun statusText(status: String) = when (status) {
        "CONFIRMED" -> "Đã xác nhận"
        "CANCELLED" -> "Đã hủy"
        else -> "Chờ xác nhận"
    }

    private suspend fun validate(accountId: Long, schedule: StudySchedule) {
        require(studentIdForAccount(accountId) == schedule.studentId) { "Bạn chỉ được quản lý lịch học cá nhân." }
        require(subjectExists(schedule.subjectId)) { "Môn học không còn tồn tại." }
        require(schedule.status in ScheduleStatus.entries.map { it.name }) { "Trạng thái lịch học không hợp lệ." }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        require(schedule.studyDate.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) &&
            runCatching { formatter.parse(schedule.studyDate) }.getOrNull() != null) { "Ngày học không hợp lệ." }
        val timePattern = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")
        require(timePattern.matches(schedule.startTime) && timePattern.matches(schedule.endTime)) { "Giờ học không hợp lệ." }
        require(schedule.startTime < schedule.endTime) { "Giờ kết thúc phải sau giờ bắt đầu." }
        require(schedule.location.isNotBlank()) { "Vui lòng nhập địa điểm học." }
        if (schedule.status != ScheduleStatus.CANCELLED.name) {
            require(countConflicts(schedule.studentId, schedule.studyDate, schedule.startTime, schedule.endTime, schedule.id) == 0) {
                "Trùng lịch! Bạn đã có buổi học trong khoảng thời gian này."
            }
        }
    }
}
