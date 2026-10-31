package com.thuchanh2.th_mobile.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.db.AppDatabase
import com.thuchanh2.th_mobile.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationFragment : Fragment() {

    private lateinit var btnMarkAllRead: MaterialButton
    private lateinit var chipGroupNotif: ChipGroup
    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmptyNotif: LinearLayout

    private val notificationAdapter = NotificationAdapter { item ->
        markAsRead(item)
    }

    private var currentStudentId: Long = 1L
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        seedSampleNotificationsIfEmpty()
        loadNotifications()
    }

    private fun initViews(view: View) {
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead)
        chipGroupNotif = view.findViewById(R.id.chipGroupNotif)
        rvNotifications = view.findViewById(R.id.rvNotifications)
        layoutEmptyNotif = view.findViewById(R.id.layoutEmptyNotif)
    }

    private fun setupRecyclerView() {
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = notificationAdapter
    }

    private fun setupListeners() {
        btnMarkAllRead.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext())
                    .notificationDao()
                    .markAllAsRead(currentStudentId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
                }
            }
        }

        chipGroupNotif.setOnCheckedStateChangeListener { _, _ ->
            loadNotifications()
        }
    }

    private fun loadNotifications() {
        val db = AppDatabase.getInstance(requireContext())
        val checkedChipId = chipGroupNotif.checkedChipId

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            when (checkedChipId) {
                R.id.chipUpcomingNotif -> {
                    db.notificationDao().getNotificationsByType(currentStudentId, "UPCOMING")
                        .collectLatest { list -> updateUiList(list) }
                }
                R.id.chipChangesNotif -> {
                    db.notificationDao().getNotificationsByType(currentStudentId, "CHANGE")
                        .collectLatest { list -> updateUiList(list) }
                }
                else -> { // All
                    db.notificationDao().getNotificationsByStudent(currentStudentId)
                        .collectLatest { list -> updateUiList(list) }
                }
            }
        }
    }

    private fun markAsRead(item: NotificationItem) {
        if (!item.isRead) {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext())
                    .notificationDao()
                    .markAsRead(item.id)
            }
        }
    }

    private fun updateUiList(list: List<NotificationItem>) {
        notificationAdapter.submitList(list)
        val isEmpty = list.isEmpty()
        rvNotifications.isVisible = !isEmpty
        layoutEmptyNotif.isVisible = isEmpty
    }

    private fun seedSampleNotificationsIfEmpty() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireContext())
            val now = System.currentTimeMillis()

            try {
                val conn = db.openHelper.writableDatabase
                conn.execSQL("INSERT OR IGNORE INTO notifications (id, studentId, title, message, type, timestamp, isRead) VALUES (1, 1, 'Lịch học sắp diễn ra', 'Môn Lập trình Android (MOB204) sẽ bắt đầu lúc 08:00 tại phòng A302.', 'UPCOMING', ${now - 600000}, 0)")
                conn.execSQL("INSERT OR IGNORE INTO notifications (id, studentId, title, message, type, timestamp, isRead) VALUES (2, 1, 'Thay đổi phòng học', 'Môn Lập trình Web (WEB205) được chuyển sang phòng B105.', 'CHANGE', ${now - 3600000}, 0)")
                conn.execSQL("INSERT OR IGNORE INTO notifications (id, studentId, title, message, type, timestamp, isRead) VALUES (3, 1, 'Đăng ký lịch học thành công', 'Bạn đã đăng ký thành công lịch học Cơ sở dữ liệu.', 'GENERAL', ${now - 86400000}, 1)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
