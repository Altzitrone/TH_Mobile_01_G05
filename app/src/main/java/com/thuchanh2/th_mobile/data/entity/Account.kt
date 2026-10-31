package com.thuchanh2.th_mobile.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object AccountRole {
    const val ADMIN = "Admin"
    const val STUDENT = "Student"
}

@Entity(tableName = "Account", indices = [Index(value = ["username"], unique = true)])
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val role: String
)
