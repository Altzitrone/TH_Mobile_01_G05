package com.thuchanh2.th_mobile

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thuchanh2.th_mobile.data.entity.AccountRole
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.databinding.ActivityStudyScheduleBinding
import com.thuchanh2.th_mobile.databinding.DialogBookScheduleBinding
import com.thuchanh2.th_mobile.notifications.ScheduleReminders
import com.thuchanh2.th_mobile.repository.StudyScheduleRepository
import com.thuchanh2.th_mobile.ui.*
import com.thuchanh2.th_mobile.ui.adapter.StudyScheduleAdapter
import com.thuchanh2.th_mobile.ui.calendar.CalendarFragment
import com.thuchanh2.th_mobile.ui.notification.NotificationFragment
import com.thuchanh2.th_mobile.viewmodel.StudyScheduleViewModel
import com.thuchanh2.th_mobile.viewmodel.StudyScheduleViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StudyScheduleActivity : ProtectedActivity() {
    override val adminOnly = false
    private lateinit var binding: ActivityStudyScheduleBinding
    private lateinit var model: StudyScheduleViewModel
    private var studentId = 0L
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) notifyUser(getString(R.string.notifications_disabled))
    }

    override fun prepareContent(savedInstanceState: Bundle?) {
        binding = ActivityStudyScheduleBinding.inflate(layoutInflater)
        installContent(binding.root)
    }

    override fun onAuthorized(savedInstanceState: Bundle?) {
        if (account.role != AccountRole.STUDENT) {
            startActivity(Intent(this, AdminDashboardActivity::class.java)); finish(); return
        }
        setupToolbar(binding.toolbar, getString(R.string.study_schedules_title))
        model = ViewModelProvider(this, StudyScheduleViewModelFactory(
            StudyScheduleRepository(studyApp.database.studyScheduleDao(), account.id)
        ))[StudyScheduleViewModel::class.java]
        val adapter = StudyScheduleAdapter(::viewBooking, ::editBooking,
            { confirmStatus(it, ScheduleStatus.CONFIRMED) }, { confirmStatus(it, ScheduleStatus.CANCELLED) }, ::confirmDelete)
        binding.recyclerViewSchedules.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSchedules.adapter = adapter
        binding.searchInput.fill(model.keyword.value)
        binding.searchInput.editText?.doAfterTextChanged { model.keyword.value = it?.toString().orEmpty() }
        binding.statusChips.check(when (model.status.value) {
            "PENDING" -> R.id.filterPending; "CONFIRMED" -> R.id.filterConfirmed
            "CANCELLED" -> R.id.filterCancelled; else -> R.id.filterAll
        })
        binding.statusChips.setOnCheckedStateChangeListener { _, ids ->
            model.status.value = when (ids.firstOrNull()) {
                R.id.filterPending -> "PENDING"; R.id.filterConfirmed -> "CONFIRMED"
                R.id.filterCancelled -> "CANCELLED"; else -> ""
            }
        }
        binding.addScheduleButton.isEnabled = false
        runAction {
            studentId = model.studentId() ?: error(getString(R.string.profile_missing))
            binding.addScheduleButton.isEnabled = true
        }
        binding.addScheduleButton.setOnClickListener { editBooking(0) }
        binding.bottomNavigation.setOnItemSelectedListener { showTab(it.itemId); true }
        val initialTab = savedInstanceState?.getInt("tab") ?: intent.getIntExtra("tab", R.id.navigation_schedules)
        binding.bottomNavigation.selectedItemId = initialTab
        showTab(initialTab)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.schedules.collect {
                    adapter.submitList(it)
                    binding.emptyState.isVisible = it.isEmpty()
                    binding.resultCount.text = getString(R.string.results_count, it.size)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            savedInstanceState == null) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showTab(id: Int) {
        val bookings = id == R.id.navigation_schedules
        binding.bookingContent.isVisible = bookings
        binding.fragmentContainer.isVisible = !bookings
        binding.toolbar.setTitle(when (id) {
            R.id.navigation_calendar -> R.string.title_calendar
            R.id.navigation_notification -> R.string.title_notification
            else -> R.string.study_schedules_title
        })
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (bookings) {
            current?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
        } else if (current?.tag != id.toString()) {
            val fragment = if (id == R.id.navigation_calendar) CalendarFragment() else NotificationFragment()
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment, id.toString()).commit()
        }
    }

    private fun editBooking(id: Long) = runAction {
        val subjects = model.subjects()
        if (subjects.isEmpty()) { notifyUser(getString(R.string.no_subjects)); return@runAction }
        val old = if (id == 0L) null else model.getById(id) ?: error(getString(R.string.not_found))
        val form = DialogBookScheduleBinding.inflate(layoutInflater)
        listOf(form.dateValue, form.startValue, form.endValue).forEach {
            it.keyListener = null
            it.isFocusable = true
            it.showSoftInputOnFocus = false
        }
        var subjectId = old?.subjectId ?: subjects.first().id
        val labels = subjects.map { "${it.subjectCode} · ${it.subjectName}" }
        form.subjectDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels))
        form.subjectDropdown.setText(labels[subjects.indexOfFirst { it.id == subjectId }.coerceAtLeast(0)], false)
        form.subjectDropdown.setOnItemClickListener { _, _, position, _ -> subjectId = subjects[position].id }
        form.dateInput.fill(old?.studyDate.orEmpty())
        form.startInput.fill(old?.startTime.orEmpty())
        form.endInput.fill(old?.endTime.orEmpty())
        form.locationInput.fill(old?.location.orEmpty())
        form.dateValue.setOnClickListener {
            val calendar = Calendar.getInstance()
            runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(form.dateInput.value()) }.getOrNull()?.let { calendar.time = it }
            DatePickerDialog(this, { _, year, month, day ->
                form.dateInput.fill(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                form.dateInput.error = null
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        fun pickTime(start: Boolean) {
            val input = if (start) form.startInput else form.endInput
            val parts = input.value().split(":").mapNotNull { it.toIntOrNull() }
            TimePickerDialog(this, { _, hour, minute ->
                input.fill(String.format(Locale.US, "%02d:%02d", hour, minute)); input.error = null
            }, parts.getOrElse(0) { 8 }, parts.getOrElse(1) { 0 }, true).show()
        }
        form.startValue.setOnClickListener { pickTime(true) }
        form.endValue.setOnClickListener { pickTime(false) }
        val dialog = MaterialAlertDialogBuilder(this).setTitle(if (old == null) R.string.book_schedule else R.string.edit_schedule)
            .setView(form.root).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener {
            val button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val checks = listOf(form.dateInput, form.startInput, form.endInput, form.locationInput).map {
                    it.validate(getString(R.string.required), it.value().isNotEmpty())
                }
                if (checks.all { it }) runAction(button) {
                    val newId = model.save(StudySchedule(id, old?.studentId ?: studentId, subjectId,
                        form.dateInput.value(), form.startInput.value(), form.endInput.value(), form.locationInput.value(),
                        old?.status ?: ScheduleStatus.PENDING.name))
                    ScheduleReminders.reschedule(this, newId)
                    ScheduleReminders.notify(this, "change-$newId", getString(R.string.title_notification), getString(R.string.booking_saved))
                    dialog.dismiss()
                    notifyUser(getString(R.string.booking_saved))
                }
            }
        }
        dialog.show()
    }

    private fun viewBooking(id: Long) = runAction {
        val row = model.schedules.value.firstOrNull { it.id == id } ?: return@runAction
        val status = getString(when (row.status) {
            "CONFIRMED" -> R.string.status_confirmed; "CANCELLED" -> R.string.status_cancelled; else -> R.string.status_pending
        })
        MaterialAlertDialogBuilder(this).setTitle(R.string.study_schedules_title)
            .setMessage(getString(R.string.booking_detail, row.subjectCode, row.subjectName, row.studyDate,
                row.startTime, row.endTime, row.location, status))
            .setPositiveButton(R.string.edit) { _, _ -> editBooking(id) }.setNegativeButton(R.string.close, null).show()
    }

    private fun confirmStatus(id: Long, status: ScheduleStatus) {
        MaterialAlertDialogBuilder(this).setTitle(if (status == ScheduleStatus.CANCELLED) R.string.cancel_booking else R.string.confirm_booking)
            .setMessage(if (status == ScheduleStatus.CANCELLED) R.string.cancel_booking_question else R.string.confirm_booking_question)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.confirm_booking) { _, _ ->
                runAction {
                    model.changeStatus(id, status.name)
                    ScheduleReminders.reschedule(this, id)
                    ScheduleReminders.notify(this, "change-$id", getString(R.string.title_notification), getString(R.string.booking_status_changed))
                    notifyUser(getString(R.string.booking_status_changed))
                }
            }.show()
    }

    private fun confirmDelete(id: Long) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.delete_booking).setMessage(R.string.delete_booking_question)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete) { _, _ ->
                runAction {
                    model.delete(id)
                    ScheduleReminders.cancel(this, id)
                    ScheduleReminders.notify(this, "change-$id", getString(R.string.title_notification), getString(R.string.deleted))
                    notifyUser(getString(R.string.deleted))
                }
            }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized) outState.putInt("tab", binding.bottomNavigation.selectedItemId)
        super.onSaveInstanceState(outState)
    }
}
