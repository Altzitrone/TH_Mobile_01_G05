package com.thuchanh2.th_mobile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StudySchedule::class, parentColumns = ["id"], childColumns = ["scheduleId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("studentId"), Index("scheduleId")]
)
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val scheduleId: Long? = null
)
