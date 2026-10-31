package com.thuchanh2.th_mobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String, // Lưu ý: trong thực tế nên mã hóa password
    val role: String // "Admin" hoặc "Student"
)
