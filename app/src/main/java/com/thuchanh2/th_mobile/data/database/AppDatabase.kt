package com.thuchanh2.th_mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thuchanh2.th_mobile.data.dao.StudyScheduleDao
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.data.entity.Subject

@Database(
    entities = [Account::class, Student::class, Subject::class, StudySchedule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studyScheduleDao(): StudyScheduleDao
    // Sau này có thể thêm accountDao(), studentDao(), subjectDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_schedule_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
