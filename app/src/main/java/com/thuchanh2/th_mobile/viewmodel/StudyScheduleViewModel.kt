package com.thuchanh2.th_mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.repository.StudyScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class StudyScheduleViewModel(private val repository: StudyScheduleRepository) : ViewModel() {
    val keyword = MutableStateFlow("")
    val status = MutableStateFlow("")
    val schedules = combine(keyword, status) { query, status -> query.trim() to status }
        .flatMapLatest { (query, status) -> repository.search(query, status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun subjects() = repository.subjects()
    suspend fun studentId() = repository.currentStudentId()
    suspend fun getById(id: Long) = repository.getById(id)
    suspend fun save(schedule: StudySchedule) = repository.save(schedule)
    suspend fun delete(id: Long) = repository.delete(id)
    suspend fun changeStatus(id: Long, status: String) = repository.changeStatus(id, status)
}

class StudyScheduleViewModelFactory(private val repository: StudyScheduleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StudyScheduleViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return StudyScheduleViewModel(repository) as T
    }
}
