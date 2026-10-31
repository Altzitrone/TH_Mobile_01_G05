package com.thuchanh2.th_mobile

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.CalendarView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.thuchanh2.th_mobile.auth.PasswordHasher
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.Subject
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StudentNavigationTest {
    private fun awaitUi(check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (check()) return
            Thread.sleep(20)
        }
        fail("UI did not reach expected state")
    }

    @Test fun loginScreenDoesNotTryToLoadStudentFragments() {
        val app = RuntimeEnvironment.getApplication() as StudyApplication
        app.session.logout()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            assertNotNull(controller.get().findViewById<android.view.View>(R.id.loginButton))
            assertNull(controller.get().findViewById<android.view.View>(R.id.bottomNavigation))
            assertTrue(controller.get().supportFragmentManager.fragments.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun authenticatedStudentCanOpenCalendarNotificationsAndRestoreTheHost() {
        val app = RuntimeEnvironment.getApplication() as StudyApplication
        val database = app.database
        val fixture = runBlocking {
            database.accountDao().ensureDefaultAdmin()
            val admin = checkNotNull(database.accountDao().findByUsername("admin"))
            val sid = database.studentDao().create(admin.id,
                Student(studentCode = "NAV001", fullname = "Navigation Student", email = "nav@example.com", phone = "0901234567"),
                "nav_student", PasswordHasher.hash("student123"))
            val account = checkNotNull(database.accountDao().findByUsername("nav_student"))
            val subject = database.subjectDao().create(admin.id, Subject(subjectCode = "NAVMOB", subjectName = "Navigation Subject", description = "", credits = 3))
            database.studyScheduleDao().create(account.id, StudySchedule(studentId = sid, subjectId = subject,
                studyDate = "2099-01-02", startTime = "08:00", endTime = "10:00", location = "A301"))
            app.session.login(account, false)
            Triple(admin.id, sid, subject)
        }
        val intent = Intent(app, StudyScheduleActivity::class.java).putExtra("tab", R.id.navigation_calendar)
        val controller = Robolectric.buildActivity(StudyScheduleActivity::class.java, intent).setup()
        try {
            val activity = controller.get()
            awaitUi { activity.findViewById<CalendarView>(R.id.calendarView) != null }
            val nav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            nav.selectedItemId = R.id.navigation_notification
            awaitUi { activity.findViewById<RecyclerView>(R.id.rvNotifications) != null }
            val saved = Bundle()
            controller.pause().saveInstanceState(saved).stop().destroy()
            val restored = Robolectric.buildActivity(StudyScheduleActivity::class.java, intent)
                .create(saved).start().restoreInstanceState(saved).resume().visible()
            try {
                awaitUi { restored.get().findViewById<RecyclerView>(R.id.rvNotifications) != null }
                restored.get().findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_schedules
                awaitUi {
                    val list = restored.get().findViewById<RecyclerView>(R.id.recyclerViewSchedules)
                    list?.adapter?.itemCount == 1
                }
            } finally { restored.pause().stop().destroy() }
        } finally {
            if (!controller.get().isDestroyed) controller.pause().stop().destroy()
            runBlocking {
                database.studentDao().delete(fixture.first, fixture.second)
                database.subjectDao().delete(fixture.first, fixture.third)
            }
            app.session.logout()
        }
    }
}
