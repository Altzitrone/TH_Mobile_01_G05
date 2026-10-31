package com.thuchanh2.th_mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_schedules",
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("subjectId")]
)
data class StudySchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val subjectId: Long,
    val studyDate: String, // Format: YYYY-MM-DD
    val startTime: String, // Format: HH:mm
    val endTime: String,   // Format: HH:mm
    val location: String,
    val status: String     // "Pending", "Confirmed", "Cancelled"
)

data class StudyScheduleWithSubject(
    val id: Long,
    val studentId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val studyDate: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val status: String
)
