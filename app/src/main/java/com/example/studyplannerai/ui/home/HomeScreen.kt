package com.example.studyplannerai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.Task
import com.example.studyplannerai.viewmodel.task.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.studyplannerai.ui.components.RationaleDialog
import com.example.studyplannerai.viewmodel.permission.PermissionViewModel
import com.example.studyplannerai.data.model.PlannedStudyBlock
import com.example.studyplannerai.ui.theme.AppDesign
import com.example.studyplannerai.ui.components.StudyBlockCard

@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel,
    permissionViewModel: PermissionViewModel,
    innerPadding: PaddingValues,
    showWeeklyPlan: Boolean
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionViewModel.onPermissionResult(isGranted, "Notification")
    }

    LaunchedEffect(permissionState.isPermissionRequested) {
        android.util.Log.d("HomeScreen", "Checking permissions. Already requested: ${permissionState.isPermissionRequested}")
        if (!permissionState.isPermissionRequested) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                android.util.Log.d("HomeScreen", "Notification permission status: $hasNotificationPermission")
                if (!hasNotificationPermission) {
                    permissionViewModel.showNotificationRationale()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val canScheduleExact = alarmManager.canScheduleExactAlarms()
                android.util.Log.d("HomeScreen", "Can schedule exact alarms: $canScheduleExact")
                if (!canScheduleExact) {
                    permissionViewModel.showAlarmRationale()
                }
            }
            permissionViewModel.setPermissionRequested()
        }
    }

    if (permissionState.showNotificationRationale) {
        RationaleDialog(
            onDismissRequest = { permissionViewModel.onNotificationRationaleDismissed() },
            onConfirm = {
                permissionViewModel.onNotificationRationaleDismissed()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            title = "Notifications Permission",
            text = "Enable notifications to receive timely reminders for your study tasks and deadlines."
        )
    }

    if (permissionState.showAlarmRationale) {
        RationaleDialog(
            onDismissRequest = { permissionViewModel.onAlarmRationaleDismissed() },
            onConfirm = {
                permissionViewModel.onAlarmRationaleDismissed()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                }
            },
            title = "Exact Alarm Permission",
            text = "This app needs permission to schedule exact alarms so that your study reminders trigger exactly when you need them."
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val completionPercent = uiState.completionPercent
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now()
    
    // Mock Study Blocks for display if empty
    val displayBlocks = if (uiState.studyBlocks.isEmpty()) {
        listOf(
            PlannedStudyBlock(
                taskId = "1",
                taskTitle = "Calculus Assignment",
                subjectName = "Mathematics",
                subjectColor = "#6366F1",
                dateEpochDay = today.toEpochDay(),
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis() + 3600000,
                plannedMinutes = 60,
                suggestedDeadlineEpochDay = today.toEpochDay(),
                isRescheduled = false,
                status = "Pending"
            )
        )
    } else uiState.studyBlocks

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You have ${uiState.pendingTasks} tasks to focus on today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = AppDesign.CardShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppDesign.PrimaryGradient)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Daily Progress",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${(completionPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { completionPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Text(
                            text = if (completionPercent > 0.8) "Almost there! Finish strong." else "Keep going! Consistency is key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Today's Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(displayBlocks.size) { index ->
            StudyBlockCard(block = displayBlocks[index])
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TaskSectionCard(
    title: String,
    emptyText: String,
    tasks: List<Task>
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (tasks.isEmpty()) {
                Text(emptyText)
            } else {
                tasks.forEach { task ->
                    TaskSummaryRow(task)
                }
            }
        }
    }
}

@Composable
private fun TaskSummaryRow(task: Task) {
    val zoneId = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(task.title, fontWeight = FontWeight.Medium)
        Text("Subject: ${task.subject}", style = MaterialTheme.typography.bodySmall)
        task.deadline?.let {
            Text(
                "Due ${Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        task.reminderTime?.let {
            Text(
                "Reminder ${Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime().format(timeFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider()
    }
}
