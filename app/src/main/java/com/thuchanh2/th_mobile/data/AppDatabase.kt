package com.thuchanh2.th_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thuchanh2.th_mobile.data.dao.AccountDao
import com.thuchanh2.th_mobile.data.dao.StudentDao
import com.thuchanh2.th_mobile.data.dao.SubjectDao
import com.thuchanh2.th_mobile.data.dao.StudyScheduleDao
import com.thuchanh2.th_mobile.data.dao.NotificationDao
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.Subject
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.data.entity.NotificationItem

@Database(
    entities = [Account::class, Student::class, Subject::class, StudySchedule::class, NotificationItem::class],
    version = 3, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studyScheduleDao(): StudyScheduleDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS StudySchedule (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    studentId INTEGER NOT NULL, subjectId INTEGER NOT NULL,
                    studyDate TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL,
                    location TEXT NOT NULL, status TEXT NOT NULL,
                    FOREIGN KEY(studentId) REFERENCES Student(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(subjectId) REFERENCES Subject(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_StudySchedule_studentId ON StudySchedule(studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_StudySchedule_subjectId ON StudySchedule(subjectId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    studentId INTEGER NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL,
                    type TEXT NOT NULL, timestamp INTEGER NOT NULL, isRead INTEGER NOT NULL,
                    scheduleId INTEGER,
                    FOREIGN KEY(studentId) REFERENCES Student(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(scheduleId) REFERENCES StudySchedule(id) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_studentId ON notifications(studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_scheduleId ON notifications(scheduleId)")
            }
        }

        /** Version 2 was shipped with two schemas during the merge. Rebuild both safely. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val hasNotifications = tableExists(db, "notifications")
                if (hasNotifications) {
                    val hasScheduleId = db.query("PRAGMA table_info(notifications)").use { cursor ->
                        val nameColumn = cursor.getColumnIndexOrThrow("name")
                        var found = false
                        while (cursor.moveToNext()) if (cursor.getString(nameColumn) == "scheduleId") found = true
                        found
                    }
                    db.execSQL("""CREATE TABLE _notification_backup_v3 (
                        id INTEGER NOT NULL, studentId INTEGER NOT NULL, title TEXT NOT NULL,
                        message TEXT NOT NULL, type TEXT NOT NULL, timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL, scheduleId INTEGER)""")
                    val scheduleColumn = if (hasScheduleId) "scheduleId" else "NULL"
                    db.execSQL("""INSERT INTO _notification_backup_v3
                        SELECT id, studentId, title, message, type, timestamp, isRead, $scheduleColumn FROM notifications""")
                    // Remove child references before rebuilding its parent, preserving every row above.
                    db.execSQL("DROP TABLE notifications")
                }

                db.execSQL("""CREATE TABLE _study_schedule_v3 (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    studentId INTEGER NOT NULL, subjectId INTEGER NOT NULL,
                    studyDate TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL,
                    location TEXT NOT NULL, status TEXT NOT NULL,
                    FOREIGN KEY(studentId) REFERENCES Student(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(subjectId) REFERENCES Subject(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                if (tableExists(db, "StudySchedule")) {
                    db.execSQL("""INSERT INTO _study_schedule_v3(id, studentId, subjectId, studyDate, startTime, endTime, location, status)
                        SELECT id, studentId, subjectId, studyDate, startTime, endTime, location,
                            CASE WHEN upper(status) IN ('PENDING', 'CONFIRMED', 'CANCELLED') THEN upper(status) ELSE status END
                        FROM StudySchedule""")
                    db.execSQL("DROP TABLE StudySchedule")
                }
                db.execSQL("ALTER TABLE _study_schedule_v3 RENAME TO StudySchedule")
                db.execSQL("CREATE INDEX index_StudySchedule_studentId ON StudySchedule(studentId)")
                db.execSQL("CREATE INDEX index_StudySchedule_subjectId ON StudySchedule(subjectId)")
                db.execSQL("""CREATE TABLE notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    studentId INTEGER NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL,
                    type TEXT NOT NULL, timestamp INTEGER NOT NULL, isRead INTEGER NOT NULL, scheduleId INTEGER,
                    FOREIGN KEY(studentId) REFERENCES Student(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(scheduleId) REFERENCES StudySchedule(id) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE INDEX index_notifications_studentId ON notifications(studentId)")
                db.execSQL("CREATE INDEX index_notifications_scheduleId ON notifications(scheduleId)")
                if (hasNotifications) {
                    db.execSQL("""INSERT INTO notifications(id, studentId, title, message, type, timestamp, isRead, scheduleId)
                        SELECT id, studentId, title, message, type, timestamp, isRead, scheduleId FROM _notification_backup_v3""")
                    db.execSQL("DROP TABLE _notification_backup_v3")
                }
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
            db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? COLLATE NOCASE", arrayOf(name))
                .use { it.moveToFirst() }

        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "study_manager.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
