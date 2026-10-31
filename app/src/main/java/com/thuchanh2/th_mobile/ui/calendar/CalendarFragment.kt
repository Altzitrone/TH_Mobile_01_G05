package com.thuchanh2.th_mobile.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.db.AppDatabase
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var chipGroupViewMode: ChipGroup
    private lateinit var tvSelectedDateHeader: TextView
    private lateinit var rvSchedule: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private val scheduleAdapter = ScheduleAdapter()
    private val selectedCalendar: Calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var currentStudentId: Long = 1L // Default student ID
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        seedSampleDataIfEmpty()
        loadScheduleForSelectedMode()
    }

    private fun initViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        chipGroupViewMode = view.findViewById(R.id.chipGroupViewMode)
        tvSelectedDateHeader = view.findViewById(R.id.tvSelectedDateHeader)
        rvSchedule = view.findViewById(R.id.rvSchedule)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
    }

    private fun setupRecyclerView() {
        rvSchedule.layoutManager = LinearLayoutManager(requireContext())
        rvSchedule.adapter = scheduleAdapter
    }

    private fun setupListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, month)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            loadScheduleForSelectedMode()
        }

        chipGroupViewMode.setOnCheckedStateChangeListener { _, _ ->
            loadScheduleForSelectedMode()
        }
    }

    private fun loadScheduleForSelectedMode() {
        val db = AppDatabase.getInstance(requireContext())
        val selectedDateStr = dateFormatter.format(selectedCalendar.time)
        val formattedDisplayDate = displayDateFormatter.format(selectedCalendar.time)

        val checkedChipId = chipGroupViewMode.checkedChipId

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            when (checkedChipId) {
                R.id.chipDaily -> {
                    tvSelectedDateHeader.text = "Danh sách lịch học ngày $formattedDisplayDate"
                    db.studyScheduleDao().getSchedulesByDate(currentStudentId, selectedDateStr)
                        .collectLatest { list -> updateUiList(list) }
                }
                R.id.chipWeekly -> {
                    val (startDate, endDate) = getWeekRange(selectedCalendar)
                    tvSelectedDateHeader.text = "Lịch học tuần (${displayDateFormatter.format(startDate.time)} - ${displayDateFormatter.format(endDate.time)})"
                    db.studyScheduleDao().getSchedulesByDateRange(
                        currentStudentId,
                        dateFormatter.format(startDate.time),
                        dateFormatter.format(endDate.time)
                    ).collectLatest { list -> updateUiList(list) }
                }
                R.id.chipMonthly -> {
                    val (startDate, endDate) = getMonthRange(selectedCalendar)
                    tvSelectedDateHeader.text = "Lịch học tháng ${selectedCalendar.get(Calendar.MONTH) + 1}/${selectedCalendar.get(Calendar.YEAR)}"
                    db.studyScheduleDao().getSchedulesByDateRange(
                        currentStudentId,
                        dateFormatter.format(startDate.time),
                        dateFormatter.format(endDate.time)
                    ).collectLatest { list -> updateUiList(list) }
                }
                else -> {
                    tvSelectedDateHeader.text = "Danh sách lịch học ngày $formattedDisplayDate"
                    db.studyScheduleDao().getSchedulesByDate(currentStudentId, selectedDateStr)
                        .collectLatest { list -> updateUiList(list) }
                }
            }
        }
    }

    private fun updateUiList(list: List<StudyScheduleWithSubject>) {
        scheduleAdapter.submitList(list)
        val isEmpty = list.isEmpty()
        rvSchedule.isVisible = !isEmpty
        layoutEmptyState.isVisible = isEmpty
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
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            val todayStr = dateFormatter.format(Calendar.getInstance().time)

            withContext(Dispatchers.IO) {
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
}
