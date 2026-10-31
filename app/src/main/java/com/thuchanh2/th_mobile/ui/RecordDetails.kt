package com.thuchanh2.th_mobile.ui

import androidx.appcompat.app.AppCompatActivity
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.entity.StudentRecord
import com.thuchanh2.th_mobile.data.entity.Subject

fun AppCompatActivity.studentDetail(record: StudentRecord) = record.student.let {
    getString(R.string.student_record_detail, it.studentCode, it.fullname, it.email, it.phone, record.username)
}

fun AppCompatActivity.subjectDetail(subject: Subject) = getString(
    R.string.subject_record_detail, subject.subjectCode, subject.subjectName, subject.credits, subject.description.ifBlank { "—" }
)
