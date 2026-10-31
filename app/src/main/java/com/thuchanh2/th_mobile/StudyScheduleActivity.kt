package com.thuchanh2.th_mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.thuchanh2.th_mobile.data.database.AppDatabase
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus
import com.thuchanh2.th_mobile.repository.StudyScheduleRepository
import com.thuchanh2.th_mobile.ui.adapter.StudyScheduleAdapter
import com.thuchanh2.th_mobile.viewmodel.StudyScheduleViewModel
import com.thuchanh2.th_mobile.viewmodel.StudyScheduleViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StudyScheduleActivity : AppCompatActivity() {

    private lateinit var viewModel: StudyScheduleViewModel
    private lateinit var adapter: StudyScheduleAdapter
    
    // Giả lập Student ID đăng nhập. Khi ghép code sẽ lấy từ SharedPreferences
    private val CURRENT_STUDENT_ID = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study_schedule)

        // 1. Khởi tạo ViewModel và Database
        val database = AppDatabase.getDatabase(this)
        val repository = StudyScheduleRepository(database.studyScheduleDao())
        val factory = StudyScheduleViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StudyScheduleViewModel::class.java]

        // 2. Cài đặt RecyclerView và Adapter
        adapter = StudyScheduleAdapter(
            onCancelClick = { schedule -> 
                viewModel.updateStatus(schedule, ScheduleStatus.CANCELLED)
            },
            onDeleteClick = { schedule ->
                viewModel.deleteSchedule(schedule)
            }
        )
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSchedules)
        recyclerView.adapter = adapter

        // 3. Lắng nghe dữ liệu (Read/Search)
        lifecycleScope.launch {
            viewModel.getAllSchedules(CURRENT_STUDENT_ID).collect { schedules ->
                adapter.submitList(schedules)
            }
        }

        // 4. Lắng nghe thông báo lỗi / thành công (Validation)
        lifecycleScope.launch {
            viewModel.uiEvent.collect { message ->
                Toast.makeText(this@StudyScheduleActivity, message, Toast.LENGTH_LONG).show()
            }
        }

        // 5. Nút Thêm lịch học
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddSchedule)
        fabAdd.setOnClickListener {
            showBookScheduleDialog()
        }
    }

    private fun showBookScheduleDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_book_schedule, null)
        val etSubjectId = view.findViewById<EditText>(R.id.etSubjectId)
        val tvSelectDate = view.findViewById<TextView>(R.id.tvSelectDate)
        val tvSelectStartTime = view.findViewById<TextView>(R.id.tvSelectStartTime)
        val tvSelectEndTime = view.findViewById<TextView>(R.id.tvSelectEndTime)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)

        var selectedDate = ""
        var selectedStartTime = ""
        var selectedEndTime = ""

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Mở DatePicker
        tvSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormat.format(calendar.time)
                tvSelectDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Mở TimePicker cho StartTime
        tvSelectStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedStartTime = timeFormat.format(calendar.time)
                tvSelectStartTime.text = selectedStartTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Mở TimePicker cho EndTime
        tvSelectEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedEndTime = timeFormat.format(calendar.time)
                tvSelectEndTime.text = selectedEndTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Lưu") { dialog, _ ->
                val subjectIdStr = etSubjectId.text.toString()
                val location = etLocation.text.toString()

                if (subjectIdStr.isEmpty() || selectedDate.isEmpty() || selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val subjectId = subjectIdStr.toLongOrNull() ?: 1L
                
                // Gọi ViewModel thực hiện Check Conflict và Create
                viewModel.bookSchedule(
                    studentId = CURRENT_STUDENT_ID,
                    subjectId = subjectId,
                    date = selectedDate,
                    startTime = selectedStartTime,
                    endTime = selectedEndTime,
                    location = location
                )
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
