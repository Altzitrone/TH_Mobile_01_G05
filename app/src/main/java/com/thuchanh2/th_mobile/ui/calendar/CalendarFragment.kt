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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var chipGroupViewMode: ChipGroup
    private lateinit var tvSelectedDateHeader: TextView
    private lateinit var rvSchedule: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private val viewModel: CalendarViewModel by viewModels()
    private val scheduleAdapter = ScheduleAdapter()

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
        calendarView.date = viewModel.selectedCalendar.timeInMillis
        chipGroupViewMode.check(when (viewModel.viewMode.value) {
            CalendarViewMode.WEEKLY -> R.id.chipWeekly
            CalendarViewMode.MONTHLY -> R.id.chipMonthly
            else -> R.id.chipDaily
        })
        setupListeners()
        observeViewModel()
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
            viewModel.setDate(year, month, dayOfMonth)
        }

        chipGroupViewMode.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                viewModel.setViewModeByChipId(checkedIds.first())
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.headerTitle.collectLatest { title ->
                        tvSelectedDateHeader.text = title
                    }
                }
                launch {
                    viewModel.schedules.collectLatest { list ->
                        updateUiList(list)
                    }
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
}
