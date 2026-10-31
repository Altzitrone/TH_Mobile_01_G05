package com.thuchanh2.th_mobile.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.StudyApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudyApplication
    private val dao = app.database.notificationDao()
    private val accountId = MutableStateFlow(0L)
    private val filterType = MutableStateFlow<String?>(null)
    val notifications = combine(accountId, filterType) { id, type -> id to type }
        .flatMapLatest { (id, type) -> dao.observe(id, type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { accountId.value = app.session.currentAccount()?.id ?: 0L } }

    fun setFilterByChipId(id: Int) {
        filterType.value = when (id) {
            R.id.chipUpcomingNotif -> "UPCOMING"
            R.id.chipChangesNotif -> "CHANGE"
            else -> null
        }
    }
    suspend fun markAllAsRead() { dao.markAllAsRead(accountId.value) }
    suspend fun markAsRead(id: Long) { dao.markAsRead(accountId.value, id) }
}
