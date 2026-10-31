package com.thuchanh2.th_mobile

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.thuchanh2.th_mobile.data.AppDatabase
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VersionTwoMigrationTest {
    @Test fun deviceVersionTwoWithoutNotificationsKeepsSchedulesAndAccounts() = runBlocking { upgrade(false) }
    @Test fun canonicalVersionTwoKeepsNotificationsAndTheirScheduleLinks() = runBlocking { upgrade(true) }

    private suspend fun upgrade(withNotifications: Boolean) {
        val context = RuntimeEnvironment.getApplication()
        val name = "v2-variant-$withNotifications-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        val schemaVersion = if (withNotifications) 2 else 1
        val entities = JSONObject(File("schemas/com.thuchanh2.th_mobile.data.AppDatabase/$schemaVersion.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        val marker = '$' + "{TABLE_NAME}"
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace(marker, table))
            val indexes = entity.getJSONArray("indices")
            for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace(marker, table))
        }
        if (!withNotifications) {
            // Exact schema variant found on the user's emulator, including CASCADE and its extra index.
            old.execSQL("""CREATE TABLE StudySchedule (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, studentId INTEGER NOT NULL,
                subjectId INTEGER NOT NULL, studyDate TEXT NOT NULL, startTime TEXT NOT NULL,
                endTime TEXT NOT NULL, location TEXT NOT NULL, status TEXT NOT NULL,
                FOREIGN KEY(studentId) REFERENCES Student(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(subjectId) REFERENCES Subject(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            old.execSQL("CREATE INDEX index_StudySchedule_studentId ON StudySchedule(studentId)")
            old.execSQL("CREATE INDEX index_StudySchedule_subjectId ON StudySchedule(subjectId)")
            old.execSQL("CREATE INDEX index_StudySchedule_studentId_studyDate ON StudySchedule(studentId, studyDate)")
        }
        old.execSQL("INSERT INTO Account(id, username, password, role) VALUES(1, 'admin', 'keep_admin_hash', 'Admin')")
        old.execSQL("INSERT INTO Account(id, username, password, role) VALUES(2, 'student01', 'keep_student_hash', 'Student')")
        old.execSQL("INSERT INTO Student(id, studentCode, fullname, email, phone, accountId) VALUES(1, 'SV01', 'Keep student', 'sv@example.com', '0901234567', 2)")
        old.execSQL("INSERT INTO Subject(id, subjectCode, subjectName, description, credits) VALUES(1, 'MOB01', 'Keep subject', 'Keep description', 3)")
        old.execSQL("INSERT INTO StudySchedule(id, studentId, subjectId, studyDate, startTime, endTime, location, status) VALUES(27, 1, 1, '2026-11-02', '08:00', '10:00', 'Keep room', 'Confirmed')")
        if (withNotifications) old.execSQL("INSERT INTO notifications(id, studentId, title, message, type, timestamp, isRead, scheduleId) VALUES(12, 1, 'Keep title', 'Keep message', 'CHANGE', 123, 1, 27)")
        old.version = 2
        old.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("keep_admin_hash", migrated.accountDao().findById(1)?.password)
            assertEquals("keep_student_hash", migrated.accountDao().findById(2)?.password)
            assertEquals("Keep student", migrated.studentDao().getById(1, 1)?.student?.fullname)
            val schedule = checkNotNull(migrated.studyScheduleDao().getById(2, 27))
            assertEquals("Keep room", schedule.location)
            assertEquals("CONFIRMED", schedule.status)
            if (withNotifications) {
                val note = migrated.notificationDao().observe(2).first().single()
                assertEquals(12L, note.id)
                assertEquals(27L, note.scheduleId)
                assertTrue(note.isRead)
                assertEquals("Keep message", note.message)
            }
            val newId = migrated.studyScheduleDao().create(2, StudySchedule(studentId = 1, subjectId = 1,
                studyDate = "2026-11-02", startTime = "12:00", endTime = "13:00", location = "New room"))
            assertTrue(newId > 27)
            assertEquals(3, migrated.openHelper.readableDatabase.version)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
