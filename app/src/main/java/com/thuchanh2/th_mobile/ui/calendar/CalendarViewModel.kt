package com.thuchanh2.th_mobile.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.db.AppDatabase
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarViewMode {
    DAILY, WEEKLY, MONTHLY
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.studyScheduleDao()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val selectedCalendar: Calendar = Calendar.getInstance()

    private val _viewMode = MutableStateFlow(CalendarViewMode.DAILY)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _selectedDateStr = MutableStateFlow(dateFormatter.format(selectedCalendar.time))

    private val _headerTitle = MutableStateFlow("")
    val headerTitle: StateFlow<String> = _headerTitle.asStateFlow()

    private var currentStudentId: Long = 1L

    @OptIn(ExperimentalCoroutinesApi::class)
    val schedules: StateFlow<List<StudyScheduleWithSubject>> = combine(
        _viewMode,
        _selectedDateStr
    ) { mode, dateStr ->
        Pair(mode, dateStr)
    }.flatMapLatest { (mode, dateStr) ->
        updateHeaderTitle(mode)
        when (mode) {
            CalendarViewMode.DAILY -> {
                dao.getSchedulesByDate(currentStudentId, dateStr)
            }
            CalendarViewMode.WEEKLY -> {
                val (startDate, endDate) = getWeekRange(selectedCalendar)
                dao.getSchedulesByDateRange(
                    currentStudentId,
                    dateFormatter.format(startDate.time),
                    dateFormatter.format(endDate.time)
                )
            }
            CalendarViewMode.MONTHLY -> {
                val (startDate, endDate) = getMonthRange(selectedCalendar)
                dao.getSchedulesByDateRange(
                    currentStudentId,
                    dateFormatter.format(startDate.time),
                    dateFormatter.format(endDate.time)
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        seedSampleDataIfEmpty()
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        selectedCalendar.set(Calendar.YEAR, year)
        selectedCalendar.set(Calendar.MONTH, month)
        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        _selectedDateStr.value = dateFormatter.format(selectedCalendar.time)
    }

    fun setViewModeByChipId(chipId: Int) {
        val newMode = when (chipId) {
            R.id.chipWeekly -> CalendarViewMode.WEEKLY
            R.id.chipMonthly -> CalendarViewMode.MONTHLY
            else -> CalendarViewMode.DAILY
        }
        _viewMode.value = newMode
    }

    private fun updateHeaderTitle(mode: CalendarViewMode) {
        val formattedDisplayDate = displayDateFormatter.format(selectedCalendar.time)
        _headerTitle.value = when (mode) {
            CalendarViewMode.DAILY -> "Danh sách lịch học ngày $formattedDisplayDate"
            CalendarViewMode.WEEKLY -> {
                val (startDate, endDate) = getWeekRange(selectedCalendar)
                "Lịch học tuần (${displayDateFormatter.format(startDate.time)} - ${displayDateFormatter.format(endDate.time)})"
            }
            CalendarViewMode.MONTHLY -> {
                "Lịch học tháng ${selectedCalendar.get(Calendar.MONTH) + 1}/${selectedCalendar.get(Calendar.YEAR)}"
            }
        }
    }

    private fun getWeekRange(cal: Calendar): Pair<Calendar, Calendar> {
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_WEEK, 6)
        return Pair(start, end)
    }

    private fun getMonthRange(cal: Calendar): Pair<Calendar, Calendar> {
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        val end = cal.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return Pair(start, end)
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = dateFormatter.format(Calendar.getInstance().time)
            try {
                val conn = db.openHelper.writableDatabase
                conn.execSQL("INSERT OR IGNORE INTO accounts (id, username, password, role) VALUES (1, 'student', '123', 'STUDENT')")
                conn.execSQL("INSERT OR IGNORE INTO students (id, studentCode, fullname, email, phone, accountId) VALUES (1, 'SV001', 'Nguyễn Văn A', 'a@student.edu.vn', '0912345678', 1)")
                conn.execSQL("INSERT OR IGNORE INTO subjects (id, subjectCode, subjectName, description, credits) VALUES (1, 'MOB204', 'Lập trình Android', 'Android UI & Room', 3)")
                conn.execSQL("INSERT OR IGNORE INTO subjects (id, subjectCode, subjectName, description, credits) VALUES (2, 'WEB205', 'Lập trình Web', 'ReactJS & REST API', 3)")
                conn.execSQL("INSERT OR IGNORE INTO study_schedules (id, studentId, subjectId, studyDate, startTime, endTime, location, status) VALUES (1, 1, 1, '$todayStr', '08:00', '10:30', 'Phòng A302', 'Confirmed')")
                conn.execSQL("INSERT OR IGNORE INTO study_schedules (id, studentId, subjectId, studyDate, startTime, endTime, location, status) VALUES (2, 1, 2, '$todayStr', '13:30', '16:00', 'Phòng B105', 'Pending')")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
