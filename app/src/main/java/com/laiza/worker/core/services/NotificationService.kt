package com.laiza.worker.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ATTENDANCE = "attendance_channel"
        const val CHANNEL_WORK = "work_channel"
        const val CHANNEL_SALARY = "salary_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for clock-in/out and selfie upload notifications."
            }

            val workChannel = NotificationChannel(
                CHANNEL_WORK,
                "Work Assignments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Used when new work or raw materials are assigned."
            }

            val salaryChannel = NotificationChannel(
                CHANNEL_SALARY,
                "Salary Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about salary credit and kharcha."
            }

            notificationManager.createNotificationChannel(attendanceChannel)
            notificationManager.createNotificationChannel(workChannel)
            notificationManager.createNotificationChannel(salaryChannel)
        }
    }

    fun showNotification(
        id: Int,
        channelId: String,
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System icon fallback
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
