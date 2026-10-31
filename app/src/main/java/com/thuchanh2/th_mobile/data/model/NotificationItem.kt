package com.thuchanh2.th_mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val title: String,
    val message: String,
    val type: String, // "UPCOMING", "CHANGE", "GENERAL"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
