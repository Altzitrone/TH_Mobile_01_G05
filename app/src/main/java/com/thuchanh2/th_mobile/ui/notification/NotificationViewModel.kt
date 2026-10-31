package com.thuchanh2.th_mobile.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.db.AppDatabase
import com.thuchanh2.th_mobile.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.notificationDao()

    private val _filterType = MutableStateFlow<String?> (null) // null = ALL
    val filterType: StateFlow<String?> = _filterType.asStateFlow()

    private var currentStudentId: Long = 1L

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationItem>> = _filterType.flatMapLatest { type ->
        if (type == null) {
            dao.getNotificationsByStudent(currentStudentId)
        } else {
            dao.getNotificationsByType(currentStudentId, type)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        seedSampleNotificationsIfEmpty()
    }

    fun setFilterByChipId(chipId: Int) {
        _filterType.value = when (chipId) {
            R.id.chipUpcomingNotif -> "UPCOMING"
            R.id.chipChangesNotif -> "CHANGE"
            else -> null
        }
    }

    fun markAllAsRead(onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markAllAsRead(currentStudentId)
            onCompleted?.invoke()
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markAsRead(notificationId)
        }
    }

    private fun seedSampleNotificationsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
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
