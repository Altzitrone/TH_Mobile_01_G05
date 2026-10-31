package com.thuchanh2.th_mobile.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.StudyApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarViewMode { DAILY, WEEKLY, MONTHLY }

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudyApplication
    private val dao = app.database.studyScheduleDao()
    private val accountId = MutableStateFlow(0L)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val selectedCalendar: Calendar = Calendar.getInstance()
    val viewMode = MutableStateFlow(CalendarViewMode.DAILY)
    private val date = MutableStateFlow(dateFormatter.format(selectedCalendar.time))
    private val title = MutableStateFlow("")
    val headerTitle = title.asStateFlow()

    val schedules = combine(accountId, viewMode, date) { id, mode, date -> Triple(id, mode, date) }
        .flatMapLatest { (id, mode, selectedDate) ->
            updateHeader(mode)
            val range = range(mode)
            if (mode == CalendarViewMode.DAILY) dao.getSchedulesByDate(id, selectedDate)
            else dao.getSchedulesByDateRange(id, dateFormatter.format(range.first.time), dateFormatter.format(range.second.time))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { accountId.value = app.session.currentAccount()?.id ?: 0L } }

    fun setDate(year: Int, month: Int, day: Int) {
        selectedCalendar.set(year, month, day)
        date.value = dateFormatter.format(selectedCalendar.time)
    }

    fun setViewModeByChipId(id: Int) {
        viewMode.value = when (id) {
            R.id.chipWeekly -> CalendarViewMode.WEEKLY
            R.id.chipMonthly -> CalendarViewMode.MONTHLY
            else -> CalendarViewMode.DAILY
        }
    }

    private fun range(mode: CalendarViewMode): Pair<Calendar, Calendar> {
        val start = selectedCalendar.clone() as Calendar
        val end = selectedCalendar.clone() as Calendar
        when (mode) {
            CalendarViewMode.WEEKLY -> {
                val offset = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                start.add(Calendar.DAY_OF_MONTH, -offset)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_MONTH, 6)
            }
            CalendarViewMode.MONTHLY -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            else -> Unit
        }
        return start to end
    }

    private fun updateHeader(mode: CalendarViewMode) {
        val (start, end) = range(mode)
        title.value = when (mode) {
            CalendarViewMode.DAILY -> app.getString(R.string.calendar_day_header, displayFormatter.format(start.time))
            CalendarViewMode.WEEKLY -> app.getString(R.string.calendar_week_header, displayFormatter.format(start.time), displayFormatter.format(end.time))
            CalendarViewMode.MONTHLY -> app.getString(R.string.calendar_month_header, start.get(Calendar.MONTH) + 1, start.get(Calendar.YEAR))
        }
    }
}
