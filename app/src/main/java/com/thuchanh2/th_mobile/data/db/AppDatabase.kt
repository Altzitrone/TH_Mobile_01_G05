package com.thuchanh2.th_mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thuchanh2.th_mobile.data.dao.NotificationDao
import com.thuchanh2.th_mobile.data.dao.StudyScheduleDao
import com.thuchanh2.th_mobile.data.model.Account
import com.thuchanh2.th_mobile.data.model.NotificationItem
import com.thuchanh2.th_mobile.data.model.Student
import com.thuchanh2.th_mobile.data.model.StudySchedule
import com.thuchanh2.th_mobile.data.model.Subject

@Database(
    entities = [
        Account::class,
        Student::class,
        Subject::class,
        StudySchedule::class,
        NotificationItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studyScheduleDao(): StudyScheduleDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_schedule_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
