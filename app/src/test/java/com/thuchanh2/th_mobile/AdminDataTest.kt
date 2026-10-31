package com.thuchanh2.th_mobile

import android.content.Context
import androidx.room.Room
import com.thuchanh2.th_mobile.auth.PasswordHasher
import com.thuchanh2.th_mobile.auth.SessionManager
import com.thuchanh2.th_mobile.data.AppDatabase
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.AccountRole
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.Subject
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
class AdminDataTest {
    private lateinit var db: AppDatabase
    private lateinit var admin: Account
    private lateinit var context: Context

    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("login_session", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.accountDao().ensureDefaultAdmin()
        admin = checkNotNull(db.accountDao().login("admin", "admin123"))
    }

    @After fun close() { db.close() }

    private suspend fun addStudent(code: String = "SV001", username: String = "student01"): Long =
        db.studentDao().create(admin.id, Student(studentCode = code, fullname = "Nguyen Van An", email = "an@example.com", phone = "0901234567"),
            username, PasswordHasher.hash("student123"))

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Operation should have been rejected") }
        catch (_: IllegalArgumentException) { /* expected validation/authorization failure */ }
    }

    @Test fun defaultAdminLoginAndPasswordChangeSurviveInitialization() = runBlocking {
        assertNull(db.accountDao().login("admin", "wrong-password"))
        assertNotEquals("admin123", admin.password)
        assertEquals(AccountRole.ADMIN, db.accountDao().login("ADMIN", "admin123")?.role)
        db.accountDao().changePassword(admin.id, "admin123", "updated123")
        db.accountDao().ensureDefaultAdmin()
        assertNull(db.accountDao().login("admin", "admin123"))
        assertEquals(admin.id, db.accountDao().login("admin", "updated123")?.id)
        rejected { db.accountDao().changePassword(admin.id, "incorrect", "different123") }
        assertNotNull(db.accountDao().login("admin", "updated123"))
    }

    @Test fun studentCrudUpdatesCredentialsAndDeletesLinkedAccount() = runBlocking {
        val id = addStudent()
        val original = checkNotNull(db.studentDao().getById(admin.id, id))
        assertEquals(1, db.studentDao().count(admin.id).first())
        assertEquals(id, db.studentDao().search(admin.id, "van an").first().single().student.id)
        assertEquals(id, db.studentDao().search(admin.id, "student01").first().single().student.id)
        db.studentDao().update(admin.id, original.student.copy(fullname = "Tran Binh"), "student02", null)
        assertNull(db.accountDao().login("student01", "student123"))
        assertNotNull(db.accountDao().login("student02", "student123"))
        assertEquals("Tran Binh", db.studentDao().getOwnProfile(original.student.accountId)?.student?.fullname)
        db.studentDao().update(admin.id, original.student.copy(fullname = "Tran Binh"), "student02", PasswordHasher.hash("newpass123"))
        assertNull(db.accountDao().login("student02", "student123"))
        assertNotNull(db.accountDao().login("student02", "newpass123"))
        db.studentDao().delete(admin.id, id)
        assertNull(db.studentDao().getById(admin.id, id))
        assertNull(db.accountDao().findById(original.student.accountId))
        assertEquals(0, db.studentDao().count(admin.id).first())
    }

    @Test fun duplicateStudentCodeOrUsernameLeavesNoOrphanAccount() = runBlocking {
        val id = addStudent()
        rejected { addStudent("sv001", "orphan_user") }
        assertNull(db.accountDao().findByUsername("orphan_user"))
        rejected { addStudent("SV002", "STUDENT01") }
        assertEquals(1, db.studentDao().count(admin.id).first())
        assertNotNull(db.studentDao().getById(admin.id, id))
        // A failed transaction must not prevent later successful writes.
        addStudent("SV002", "student02")
        assertEquals(2, db.studentDao().count(admin.id).first())
    }

    @Test fun subjectCrudSearchAndValidation() = runBlocking {
        val id = db.subjectDao().create(admin.id, Subject(subjectCode = "MOB01", subjectName = "Android", description = "Mobile programming", credits = 3))
        assertEquals(id, db.subjectDao().search(admin.id, "PROGRAM").first().single().id)
        val subject = checkNotNull(db.subjectDao().getById(admin.id, id))
        db.subjectDao().update(admin.id, subject.copy(subjectName = "Mobile Android", credits = 4))
        assertEquals(4, db.subjectDao().getById(admin.id, id)?.credits)
        rejected { db.subjectDao().create(admin.id, subject.copy(id = 0, subjectCode = "mob01")) }
        rejected { db.subjectDao().create(admin.id, subject.copy(id = 0, subjectCode = "BAD", credits = 0)) }
        assertEquals(1, db.subjectDao().count(admin.id).first())
        assertEquals(1, db.subjectDao().delete(admin.id, id))
        assertTrue(db.subjectDao().search(admin.id, "").first().isEmpty())
    }

    @Test fun failedStudentInsertRollsBackNewAccount() = runBlocking {
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_student BEFORE INSERT ON Student
            WHEN NEW.studentCode = 'ROLLBACK' BEGIN SELECT RAISE(ABORT, 'forced failure'); END""")
        var failed = false
        try { addStudent("ROLLBACK", "rollback_user") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { failed = true }
        assertTrue(failed)
        assertNull(db.accountDao().findByUsername("rollback_user"))
        assertEquals(0, db.studentDao().count(admin.id).first())
    }

    @Test fun studentCannotReadOrMutateAdministrativeData() = runBlocking {
        val studentId = addStudent()
        val student = checkNotNull(db.accountDao().findByUsername("student01"))
        val subject = Subject(subjectCode = "MOB01", subjectName = "Android", description = "", credits = 3)
        val subjectId = db.subjectDao().create(admin.id, subject)
        assertNull(db.studentDao().getById(student.id, studentId))
        assertTrue(db.studentDao().search(student.id, "").first().isEmpty())
        assertTrue(db.subjectDao().search(student.id, "").first().isEmpty())
        rejected { db.studentDao().delete(student.id, studentId) }
        rejected { db.studentDao().create(student.id, Student(studentCode = "SV002", fullname = "B", email = "b@example.com", phone = "0901234567"), "student02", "hash") }
        rejected { db.subjectDao().create(student.id, subject.copy(subjectCode = "MOB02")) }
        rejected { db.subjectDao().update(student.id, subject.copy(id = subjectId, credits = 5)) }
        assertEquals(0, db.subjectDao().delete(student.id, subjectId))
        assertNotNull(db.studentDao().getOwnProfile(student.id))
        assertNotNull(db.subjectDao().getById(admin.id, subjectId))
    }

    @Test fun rememberedSessionUsesDatabaseRoleAndLogoutClearsPreferences() = runBlocking {
        addStudent()
        val student = checkNotNull(db.accountDao().findByUsername("student01"))
        SessionManager(context, db.accountDao()).login(student, true)
        val preferences = context.getSharedPreferences("login_session", Context.MODE_PRIVATE)
        assertFalse(preferences.contains("password"))
        preferences.edit().putString("role", AccountRole.ADMIN).commit()
        val restored = SessionManager(context, db.accountDao())
        assertEquals(AccountRole.STUDENT, restored.currentAccount()?.role)
        restored.logout()
        assertNull(SessionManager(context, db.accountDao()).currentAccount())
        assertTrue(preferences.all.isEmpty())
    }

    @Test fun unrememberedOrDeletedAccountIsNotRestored() = runBlocking {
        val studentId = addStudent()
        val student = checkNotNull(db.accountDao().findByUsername("student01"))
        val temporary = SessionManager(context, db.accountDao())
        temporary.login(student, false)
        assertEquals(student.id, temporary.currentAccount()?.id)
        assertNull(SessionManager(context, db.accountDao()).currentAccount())
        temporary.login(student, true)
        db.studentDao().delete(admin.id, studentId)
        assertNull(SessionManager(context, db.accountDao()).currentAccount())
    }
}
