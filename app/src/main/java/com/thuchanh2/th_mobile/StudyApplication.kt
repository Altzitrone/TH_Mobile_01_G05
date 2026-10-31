package com.thuchanh2.th_mobile

import android.app.Application
import com.thuchanh2.th_mobile.auth.SessionManager
import com.thuchanh2.th_mobile.data.AppDatabase

class StudyApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val session by lazy { SessionManager(this, database.accountDao()) }
}
