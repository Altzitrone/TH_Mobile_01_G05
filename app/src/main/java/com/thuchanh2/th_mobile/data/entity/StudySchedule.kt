package com.thuchanh2.th_mobile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ScheduleStatus {
    PENDING, CONFIRMED, CANCELLED
}

@Entity(
    tableName = "StudySchedule",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("studentId"), Index("subjectId")]
)
data class StudySchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val subjectId: Long,
    val studyDate: String, // Format: yyyy-MM-dd
    val startTime: String, // Format: HH:mm
    val endTime: String,   // Format: HH:mm
    val location: String,
    val status: String = ScheduleStatus.PENDING.name
)
