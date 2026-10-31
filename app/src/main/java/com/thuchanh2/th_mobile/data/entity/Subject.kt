package com.thuchanh2.th_mobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectCode: String,
    val subjectName: String,
    val description: String,
    val credits: Int
)
