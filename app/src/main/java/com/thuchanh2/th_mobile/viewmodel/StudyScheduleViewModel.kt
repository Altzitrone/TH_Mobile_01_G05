package com.thuchanh2.th_mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.repository.StudyScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class StudyScheduleViewModel(private val repository: StudyScheduleRepository) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun getAllSchedules(studentId: Long): Flow<List<StudySchedule>> {
        return repository.getAllByStudentId(studentId)
    }

    fun searchSchedules(studentId: Long, query: String): Flow<List<StudySchedule>> {
        return repository.search(studentId, query)
    }

    fun bookSchedule(
        studentId: Long, 
        subjectId: Long, 
        date: String, 
        startTime: String, 
        endTime: String, 
        location: String
    ) {
        viewModelScope.launch {
            // Kiểm tra trùng lịch
            val isConflict = repository.checkConflict(studentId, date, startTime, endTime)
            
            if (isConflict) {
                _uiEvent.emit("Trùng lịch! Bạn đã có lịch học vào khoảng thời gian này.")
            } else {
                val newSchedule = StudySchedule(
                    studentId = studentId,
                    subjectId = subjectId,
                    studyDate = date,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    status = ScheduleStatus.PENDING.name
                )
                repository.insert(newSchedule)
                _uiEvent.emit("Đặt lịch thành công!")
            }
        }
    }

    fun updateSchedule(
        scheduleId: Long,
        studentId: Long, 
        subjectId: Long, 
        date: String, 
        startTime: String, 
        endTime: String, 
        location: String,
        status: String
    ) {
        viewModelScope.launch {
            val isConflict = repository.checkConflictForUpdate(scheduleId, studentId, date, startTime, endTime)
            if (isConflict) {
                _uiEvent.emit("Trùng lịch! Không thể cập nhật sang thời gian này.")
            } else {
                val updatedSchedule = StudySchedule(
                    id = scheduleId,
                    studentId = studentId,
                    subjectId = subjectId,
                    studyDate = date,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    status = status
                )
                repository.update(updatedSchedule)
                _uiEvent.emit("Cập nhật lịch thành công!")
            }
        }
    }

    fun updateStatus(schedule: StudySchedule, newStatus: ScheduleStatus) {
        viewModelScope.launch {
            repository.update(schedule.copy(status = newStatus.name))
            _uiEvent.emit("Đã thay đổi trạng thái thành ${newStatus.name}")
        }
    }

    fun deleteSchedule(schedule: StudySchedule) {
        viewModelScope.launch {
            repository.delete(schedule)
            _uiEvent.emit("Đã xóa lịch học!")
        }
    }
}

class StudyScheduleViewModelFactory(private val repository: StudyScheduleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
