package com.thuchanh2.th_mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class RecordListViewModel(application: Application) : AndroidViewModel(application) {
    val keyword = MutableStateFlow("")
}
