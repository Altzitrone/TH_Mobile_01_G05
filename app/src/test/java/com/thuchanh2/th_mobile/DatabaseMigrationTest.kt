package com.thuchanh2.th_mobile

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.thuchanh2.th_mobile.data.AppDatabase
import com.thuchanh2.th_mobile.data.entity.StudySchedule
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
class DatabaseMigrationTest {
    @Test fun migrationPreservesAllAdministrativeDataAndAddsScheduleTables() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-v1-to-v3-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        val schema = JSONObject(File("schemas/com.thuchanh2.th_mobile.data.AppDatabase/1.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        val tableMarker = '$' + "{TABLE_NAME}"
        for (i in 0 until schema.length()) {
            val entity = schema.getJSONObject(i)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace(tableMarker, table))
            val indexes = entity.getJSONArray("indices")
            for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace(tableMarker, table))
        }
        old.execSQL("INSERT INTO Account(id, username, password, role) VALUES(1, 'legacy_admin', 'original_hash', 'Admin')")
        old.execSQL("INSERT INTO Account(id, username, password, role) VALUES(2, 'legacy_student', 'original_hash', 'Student')")
        old.execSQL("INSERT INTO Student(id, studentCode, fullname, email, phone, accountId) VALUES(1, 'OLD01', 'Original Student', 'old@example.com', '0901234567', 2)")
        old.execSQL("INSERT INTO Subject(id, subjectCode, subjectName, description, credits) VALUES(1, 'OLDMOB', 'Original Subject', 'Keep description', 3)")
        old.version = 1
        old.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("original_hash", migrated.accountDao().findById(1)?.password)
            assertEquals("Original Student", migrated.studentDao().getById(1, 1)?.student?.fullname)
            assertEquals("Keep description", migrated.subjectDao().getById(1, 1)?.description)
            val id = migrated.studyScheduleDao().create(2, StudySchedule(
                studentId = 1, subjectId = 1, studyDate = "2026-11-02", startTime = "08:00", endTime = "10:00", location = "A301"))
            assertNotNull(migrated.studyScheduleDao().getById(2, id))
            assertEquals(3, migrated.openHelper.readableDatabase.version)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
