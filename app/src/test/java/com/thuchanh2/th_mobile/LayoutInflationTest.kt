package com.thuchanh2.th_mobile

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LayoutInflationTest {
    @Test fun allScreensInflateWithMaterialTheme() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_TH_Mobile_G5)
        val inflater = LayoutInflater.from(context)
        listOf(R.layout.activity_main, R.layout.activity_admin_dashboard, R.layout.activity_records,
            R.layout.activity_student_editor, R.layout.activity_subject_editor, R.layout.activity_student_home,
            R.layout.row_record, R.layout.dialog_change_password, R.layout.activity_study_schedule,
            R.layout.dialog_book_schedule, R.layout.row_booking, R.layout.fragment_calendar,
            R.layout.fragment_notification, R.layout.item_study_schedule, R.layout.item_notification).forEach {
            assertNotNull(inflater.inflate(it, null, false))
        }
    }
}
