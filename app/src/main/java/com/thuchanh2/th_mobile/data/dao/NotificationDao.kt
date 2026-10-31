package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thuchanh2.th_mobile.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("SELECT * FROM notifications WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getNotificationsByStudent(studentId: Long): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE studentId = :studentId AND type = :type ORDER BY timestamp DESC")
    fun getNotificationsByType(studentId: Long, type: String): Flow<List<NotificationItem>>

    @Query("UPDATE notifications SET isRead = 1 WHERE studentId = :studentId")
    suspend fun markAllAsRead(studentId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)
}
