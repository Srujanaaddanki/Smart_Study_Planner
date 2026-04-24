package com.example.studyplannerai.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import com.example.studyplannerai.MainActivity
import com.example.studyplannerai.R
import kotlinx.coroutines.launch
import com.example.studyplannerai.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty().ifBlank { "Task" }

        if (action == ACTION_MARK_AS_DONE) {
            Log.d("ReminderReceiver", "Mark as Done action received for task: $taskId")
            
            // Use a coroutine to update the repository
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    repository.markComplete(taskId, true)
                    Log.d("ReminderReceiver", "Task $taskId marked as complete")
                    
                    // Cancel the notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId.hashCode())
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error marking task as complete", e)
                }
            }
            return
        }

        Log.d("ReminderReceiver", "Alarm received for task: $taskTitle ($taskId)")
        
        NotificationHelper.createNotificationChannel(context)

        val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        Log.d("ReminderReceiver", "Notification permission granted: $isPermissionGranted")
        
        if (!isPermissionGranted) {
            Log.e("ReminderReceiver", "Permission POST_NOTIFICATIONS is NOT granted. Aborting notification.")
            return
        }
        val taskSubject = intent.getStringExtra(EXTRA_TASK_SUBJECT).orEmpty()
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark as Done action intent
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ACTION_MARK_AS_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen Intent for Alarm-like behavior
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_SUBJECT, taskSubject)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode() + 1, // Unique ID
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reschedule action intent
        val rescheduleIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("EXTRA_ACTION", "RESCHEDULE")
            putExtra("EXTRA_TASK_ID", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reschedulePendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode() + 3,
            rescheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Task Reminder")
            .setContentText(taskTitle)
            .setSubText(taskSubject.ifBlank { null })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Mark as Done", donePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Reschedule", reschedulePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d("ReminderReceiver", "Full-screen intent created. Calling notify() for task: $taskTitle (ID: $taskId)")
        notificationManager.notify(taskId.hashCode(), notification)
        Log.d("ReminderReceiver", "Notification notificationManager.notify() call complete")
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_SUBJECT = "extra_task_subject"
        const val EXTRA_TASK_MESSAGE = "extra_task_message"
        const val ACTION_MARK_AS_DONE = "com.example.studyplannerai.ACTION_MARK_AS_DONE"
    }
}
