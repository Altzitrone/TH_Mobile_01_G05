package com.thuchanh2.th_mobile

import android.content.Context
import androidx.room.Room
import com.thuchanh2.th_mobile.auth.PasswordHasher
import com.thuchanh2.th_mobile.data.AppDatabase
import com.thuchanh2.th_mobile.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScheduleIntegrationTest {
    private lateinit var db: AppDatabase
    private var adminId = 0L
    private var accountId = 0L
    private var otherAccountId = 0L
    private var studentId = 0L
    private var subjectId = 0L

    @Before fun setup() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.accountDao().ensureDefaultAdmin()
        adminId = checkNotNull(db.accountDao().findByUsername("admin")).id
        val hash = PasswordHasher.hash("student123")
        studentId = db.studentDao().create(adminId,
            Student(studentCode = "SV01", fullname = "Nguyen An", email = "an@example.com", phone = "0901234567"), "student01", hash)
        db.studentDao().create(adminId,
            Student(studentCode = "SV02", fullname = "Tran Binh", email = "binh@example.com", phone = "0901234568"), "student02", hash)
        accountId = checkNotNull(db.accountDao().findByUsername("student01")).id
        otherAccountId = checkNotNull(db.accountDao().findByUsername("student02")).id
        subjectId = db.subjectDao().create(adminId, Subject(subjectCode = "MOB01", subjectName = "Android", description = "Mobile", credits = 3))
    }

    @After fun close() { db.close() }

    private fun booking(start: String = "08:00", end: String = "10:00", date: String = "2026-11-02",
                        status: String = "PENDING") = StudySchedule(
        studentId = studentId, subjectId = subjectId, studyDate = date,
        startTime = start, endTime = end, location = "A301", status = status)

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Operation should be rejected") }
        catch (_: IllegalArgumentException) { }
        catch (_: IllegalStateException) { }
    }

    @Test fun bookingCrudSearchCalendarAndAccountMapping() = runBlocking {
        assertNotEquals(accountId, studentId)
        assertEquals(studentId, db.studyScheduleDao().studentIdForAccount(accountId))
        assertEquals(subjectId, db.studyScheduleDao().availableSubjects(accountId).single().id)
        val id = db.studyScheduleDao().create(accountId, booking())
        db.studyScheduleDao().create(accountId, booking(date = "2026-11-03"))
        assertEquals(id, db.studyScheduleDao().search(accountId, "Android").first().first().id)
        assertEquals(2, db.studyScheduleDao().search(accountId, "A301").first().size)
        assertEquals(1, db.studyScheduleDao().getSchedulesByDate(accountId, "2026-11-02").first().size)
        assertEquals(2, db.studyScheduleDao().getSchedulesByDateRange(accountId, "2026-11-01", "2026-11-30").first().size)
        val row = checkNotNull(db.studyScheduleDao().getById(accountId, id))
        db.studyScheduleDao().update(accountId, row.copy(location = "B202", startTime = "09:00", endTime = "11:00"))
        assertEquals("B202", db.studyScheduleDao().getById(accountId, id)?.location)
        db.studyScheduleDao().delete(accountId, id)
        assertNull(db.studyScheduleDao().getById(accountId, id))
        assertEquals(1, db.studyScheduleDao().search(accountId).first().size)
    }

    @Test fun overlapsRejectedButAdjacentDatesAndCancelledBookingsAreAllowed() = runBlocking {
        val first = db.studyScheduleDao().create(accountId, booking())
        rejected { db.studyScheduleDao().create(accountId, booking("09:00", "11:00")) }
        rejected { db.studyScheduleDao().create(accountId, booking("07:00", "09:00")) }
        rejected { db.studyScheduleDao().create(accountId, booking("08:30", "09:00")) }
        rejected { db.studyScheduleDao().create(accountId, booking("07:00", "11:00")) }
        db.studyScheduleDao().create(accountId, booking("10:00", "11:00"))
        db.studyScheduleDao().create(accountId, booking(date = "2026-11-03"))
        db.studyScheduleDao().changeStatus(accountId, first, "CANCELLED")
        val replacement = db.studyScheduleDao().create(accountId, booking())
        rejected { db.studyScheduleDao().changeStatus(accountId, first, "CONFIRMED") }
        assertEquals("CANCELLED", db.studyScheduleDao().getById(accountId, first)?.status)
        db.studyScheduleDao().changeStatus(accountId, replacement, "CONFIRMED")
        assertEquals(1, db.studyScheduleDao().search(accountId, status = "CONFIRMED").first().size)
    }

    @Test fun concurrentBookingsUseAtomicConflictValidation() = runBlocking {
        val outcomes = coroutineScope {
            List(2) { async(Dispatchers.Default) { runCatching { db.studyScheduleDao().create(accountId, booking()) }.isSuccess } }.awaitAll()
        }
        assertEquals(1, outcomes.count { it })
        assertEquals(1, db.studyScheduleDao().search(accountId).first().size)
        assertEquals(1, db.notificationDao().observe(accountId).first().size)
    }

    @Test fun otherStudentsAndAdminCannotModifyOrReadPersonalSchedules() = runBlocking {
        val id = db.studyScheduleDao().create(accountId, booking())
        assertNull(db.studyScheduleDao().getById(otherAccountId, id))
        assertTrue(db.studyScheduleDao().search(otherAccountId).first().isEmpty())
        assertTrue(db.studyScheduleDao().getSchedulesByDate(adminId, "2026-11-02").first().isEmpty())
        rejected { db.studyScheduleDao().delete(otherAccountId, id) }
        rejected { db.studyScheduleDao().changeStatus(otherAccountId, id, "CANCELLED") }
        rejected { db.studyScheduleDao().update(otherAccountId, booking().copy(id = id)) }
        rejected { db.studyScheduleDao().create(otherAccountId, booking()) }
        rejected { db.studyScheduleDao().create(adminId, booking()) }
        assertTrue(db.notificationDao().observe(otherAccountId).first().isEmpty())
        val notification = db.notificationDao().observe(accountId).first().single()
        assertEquals(0, db.notificationDao().markAsRead(otherAccountId, notification.id))
        assertFalse(db.notificationDao().observe(accountId).first().single().isRead)
    }

    @Test fun invalidDatesTimesSubjectsAndLocationsAreRejected() = runBlocking {
        listOf(
            booking("10:00", "08:00"), booking("08:00", "08:00"), booking("8:00", "10:00"),
            booking("08:00", "25:00"), booking(date = "2026-02-30"), booking(date = "2026-2-3"),
            booking().copy(location = " "), booking().copy(subjectId = 9999),
            booking(status = "UNKNOWN")
        ).forEach { invalid -> rejected { db.studyScheduleDao().create(accountId, invalid) } }
        assertTrue(db.studyScheduleDao().search(accountId).first().isEmpty())
        assertTrue(db.notificationDao().observe(accountId).first().isEmpty())
    }

    @Test fun changesAndUpcomingNotificationsAreOwnedAndDeduplicated() = runBlocking {
        val id = db.studyScheduleDao().create(accountId, booking())
        assertEquals(1, db.notificationDao().observe(accountId, "CHANGE").first().size)
        assertTrue(db.notificationDao().recordUpcoming(id, "Reminder", "Upcoming", 1) > 0)
        assertTrue(db.notificationDao().recordUpcoming(id, "Reminder", "Upcoming", 2) <= 0)
        assertEquals(1, db.notificationDao().observe(accountId, "UPCOMING").first().size)
        db.notificationDao().markAllAsRead(accountId)
        assertTrue(db.notificationDao().observe(accountId).first().all { it.isRead })
        db.studyScheduleDao().changeStatus(accountId, id, "CANCELLED")
        assertTrue(db.notificationDao().observe(accountId, "UPCOMING").first().isEmpty())
        assertTrue(db.notificationDao().recordUpcoming(id, "Reminder", "Cancelled", 3) <= 0)
        assertTrue(db.notificationDao().observe(accountId, "CHANGE").first().any { !it.isRead })
    }

    @Test fun studentDeletionCascadesButBookedSubjectsAreProtected() = runBlocking {
        db.studyScheduleDao().create(accountId, booking())
        var failed = false
        try { db.subjectDao().delete(adminId, subjectId) }
        catch (_: android.database.sqlite.SQLiteConstraintException) { failed = true }
        assertTrue(failed)
        assertNotNull(db.subjectDao().getById(adminId, subjectId))
        db.studentDao().delete(adminId, studentId)
        assertTrue(db.studyScheduleDao().search(accountId).first().isEmpty())
        assertNull(db.accountDao().findById(accountId))
        assertEquals(1, db.subjectDao().delete(adminId, subjectId))
    }
}
