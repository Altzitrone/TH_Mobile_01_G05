package com.thuchanh2.th_mobile.data.model

/** Query projection only. The single Room entity lives in data.entity. */
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
