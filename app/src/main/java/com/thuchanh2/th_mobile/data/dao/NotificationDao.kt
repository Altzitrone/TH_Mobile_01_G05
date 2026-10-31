package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.thuchanh2.th_mobile.data.entity.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("""SELECT n.* FROM notifications n JOIN Student s ON s.id = n.studentId
        JOIN Account a ON a.id = s.accountId
        WHERE a.id = :accountId AND a.role = 'Student' AND (:type IS NULL OR n.type = :type)
        ORDER BY n.timestamp DESC, n.id DESC""")
    fun observe(accountId: Long, type: String? = null): Flow<List<NotificationItem>>

    @Query("""UPDATE notifications SET isRead = 1 WHERE studentId IN
        (SELECT s.id FROM Student s JOIN Account a ON a.id = s.accountId WHERE a.id = :accountId AND a.role = 'Student')""")
    suspend fun markAllAsRead(accountId: Long): Int

    @Query("""UPDATE notifications SET isRead = 1 WHERE id = :notificationId AND studentId IN
        (SELECT s.id FROM Student s JOIN Account a ON a.id = s.accountId WHERE a.id = :accountId AND a.role = 'Student')""")
    suspend fun markAsRead(accountId: Long, notificationId: Long): Int

    @Query("""INSERT INTO notifications(studentId, title, message, type, timestamp, isRead, scheduleId)
        SELECT studentId, :title, :message, 'UPCOMING', :timestamp, 0, id FROM StudySchedule
        WHERE id = :scheduleId AND status != 'CANCELLED'
        AND NOT EXISTS(SELECT 1 FROM notifications WHERE scheduleId = :scheduleId AND type = 'UPCOMING')""")
    suspend fun recordUpcoming(scheduleId: Long, title: String, message: String, timestamp: Long): Long
}
