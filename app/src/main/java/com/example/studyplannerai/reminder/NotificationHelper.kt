package com.example.studyplannerai.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID = "task_channel"
    private const val CHANNEL_NAME = "Task Reminder"
    private const val CHANNEL_DESCRIPTION = "Reminders for your tasks"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        android.util.Log.d("NotificationHelper", "Ensuring notification channel exists: $CHANNEL_ID")
        
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true) // Added to help alarm behavior
        }

        notificationManager.createNotificationChannel(channel)
        android.util.Log.d("NotificationHelper", "Notification channel configuration completed")
    }

    /**
     * Call this if you suspect the channel importance was downgraded by the system/user
     * and you need to force-reset it for debugging.
     */
    fun recreateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        android.util.Log.w("NotificationHelper", "Force-recreating channel: $CHANNEL_ID")
        notificationManager.deleteNotificationChannel(CHANNEL_ID)
        createNotificationChannel(context)
    }
}
