package com.thuchanh2.th_mobile.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thuchanh2.th_mobile.StudyScheduleActivity
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.StudyApplication
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

object ScheduleReminders {
    private const val CHANNEL = "study_reminders"
    private fun pending(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ScheduleReminderReceiver::class.java)
            .setData(Uri.parse("studyhub://reminder/$id")).putExtra("scheduleId", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, id))
        NotificationManagerCompat.from(context).cancel("schedule-$id", 1)
    }

    suspend fun reschedule(context: Context, id: Long) {
        cancel(context, id)
        val app = context.applicationContext as StudyApplication
        val schedule = app.database.studyScheduleDao().forReminder(id) ?: return
        if (schedule.status == ScheduleStatus.CANCELLED.name) return
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        val start = runCatching { formatter.parse("${schedule.studyDate} ${schedule.startTime}")?.time }.getOrNull() ?: return
        if (start <= System.currentTimeMillis()) return
        val at = maxOf(System.currentTimeMillis() + 1000, start - 15 * 60 * 1000)
        // Inexact alarms require no exact-alarm permission; Android may defer delivery.
        try {
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, at, pending(context, id)
            )
        } catch (error: SecurityException) { Log.w("StudyReminders", "Unable to schedule reminder", error) }
    }

    suspend fun restore(context: Context) {
        val app = context.applicationContext as StudyApplication
        app.database.studyScheduleDao().activeReminders().forEach { reschedule(context, it.id) }
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, tag: String, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.notification_channel_description)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, StudyScheduleActivity::class.java).putExtra("tab", R.id.navigation_notification)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_study_notification)
            .setContentTitle(title).setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(tag, 1, notification)
    }
}

class ScheduleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    ScheduleReminders.restore(context)
                } else {
                    val app = context.applicationContext as StudyApplication
                    val id = intent.getLongExtra("scheduleId", 0)
                    val row = app.database.studyScheduleDao().forReminder(id)
                    if (row != null && row.status != ScheduleStatus.CANCELLED.name) {
                        val start = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${row.studyDate} ${row.startTime}")?.time ?: 0
                        if (start > System.currentTimeMillis() + 15 * 60 * 1000 || start < System.currentTimeMillis()) {
                            ScheduleReminders.reschedule(context, id)
                            return@launch
                        }
                        val title = context.getString(R.string.upcoming_title)
                        val message = context.getString(R.string.upcoming_message, row.subjectName, row.startTime, row.location)
                        val inserted = app.database.notificationDao().recordUpcoming(id, title, message, System.currentTimeMillis())
                        val ownStudent = app.database.studyScheduleDao().studentIdForAccount(app.session.reminderAccountId())
                        if (inserted > 0 && ownStudent == row.studentId) {
                            ScheduleReminders.notify(context, "schedule-$id", title, message)
                        }
                    }
                }
            } catch (error: Exception) { Log.e("StudyReminders", "Reminder failed", error) }
            finally { result.finish() }
        }
    }
}
