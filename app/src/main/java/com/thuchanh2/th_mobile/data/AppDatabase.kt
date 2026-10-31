package com.thuchanh2.th_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thuchanh2.th_mobile.data.dao.AccountDao
import com.thuchanh2.th_mobile.data.dao.StudentDao
import com.thuchanh2.th_mobile.data.dao.SubjectDao
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.Subject

@Database(entities = [Account::class, Student::class, Subject::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "study_manager.db")
                .build().also { instance = it }
        }
    }
}
