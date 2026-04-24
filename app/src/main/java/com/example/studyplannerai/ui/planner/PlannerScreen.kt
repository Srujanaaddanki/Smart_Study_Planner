package com.example.studyplannerai.ui.planner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.viewmodel.planner.PlannerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    plannerViewModel: PlannerViewModel,
    innerPadding: PaddingValues
) {
    val uiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
    var subject by remember { mutableStateOf("") }
    val selectedTopics = remember { mutableStateListOf<String>() }
    var showPlanningFlow by remember { mutableStateOf(false) }
    
    var selectedTask by remember { mutableStateOf<StudyPlanItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showCustomTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.studyPlan.isEmpty() && !showPlanningFlow) {
                EmptyStateSection(onStart = { showPlanningFlow = true })
            } else if (showPlanningFlow) {
                PlanningFlowSection(
                    uiState = uiState,
                    subject = subject,
                    onSubjectChange = { subject = it },
                    selectedTopics = selectedTopics,
                    onToggleTopic = { topic ->
                        if (selectedTopics.contains(topic)) selectedTopics.remove(topic)
                        else selectedTopics.add(topic)
                    },
                    onGetTopics = { plannerViewModel.getTopics(subject) },
                    onGenerate = { plannerViewModel.generateFinalSchedule(subject, selectedTopics.toList()) },
                    onAccept = {
                        plannerViewModel.acceptPlan()
                        showPlanningFlow = false
                        selectedTopics.clear()
                    },
                    onCancel = { 
                        showPlanningFlow = false
                        selectedTopics.clear()
                    }
                )
            } else {
                ActivePlanSection(
                    plan = uiState.studyPlan,
                    progress = uiState.progress,
                    onTaskClick = {
                        selectedTask = it
                        showOptionsSheet = true
                    },
                    onAddMore = { showCustomTaskDialog = true },
                    onScheduleAgain = { showPlanningFlow = true }
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (showOptionsSheet && selectedTask != null) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                TaskOptionsContent(
                    task = selectedTask!!,
                    onComplete = {
                        plannerViewModel.toggleTaskCompletion(it)
                        showOptionsSheet = false
                    },
                    onReschedule = { showOptionsSheet = false },
                    onRegenerateTask = { showOptionsSheet = false },
                    onDelete = {
                        plannerViewModel.deleteSingleTask(it.id)
                        showOptionsSheet = false
                    }
                )
            }
        }
        
        if (showCustomTaskDialog) {
            AddCustomTaskDialog(
                onDismiss = { showCustomTaskDialog = false },
                onAdd = { day, title, topic, duration ->
                    plannerViewModel.addCustomTask(day, title, topic, duration)
                    showCustomTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyStateSection(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))
        Text("No active study plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Create a plan to start your learning journey", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Create New Plan")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanningFlowSection(
    uiState: com.example.studyplannerai.viewmodel.planner.PlannerUiState,
    subject: String,
    onSubjectChange: (String) -> Unit,
    selectedTopics: List<String>,
    onToggleTopic: (String) -> Unit,
    onGetTopics: () -> Unit,
    onGenerate: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, null) }
            Text("Back", style = MaterialTheme.typography.titleMedium)
        }
        
        if (uiState.temporaryPlan.isNotEmpty()) {
            PlanPreviewSection(
                plan = uiState.temporaryPlan,
                onAccept = onAccept,
                onRegenerate = onGenerate,
                isLoading = uiState.isLoading
            )
        } else if (uiState.topics.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Select topics for $subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.topics.forEach { topic ->
                        FilterChip(
                            selected = selectedTopics.contains(topic),
                            onClick = { onToggleTopic(topic) },
                            label = { Text(topic) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onGenerate,
                    enabled = selectedTopics.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Generate Schedule")
                }
            }
        } else {
            SubjectInputSection(
                subject = subject,
                onSubjectChange = onSubjectChange,
                onGenerate = onGetTopics,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
fun ActivePlanSection(
    plan: List<StudyPlanItem>,
    progress: Float,
    onTaskClick: (StudyPlanItem) -> Unit,
    onAddMore: () -> Unit,
    onScheduleAgain: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProgressCard(progress)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Task")
                }
                OutlinedButton(
                    onClick = onScheduleAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(4.dp))
                    Text("New Plan")
                }
            }
        }

        // Group by subject (planId) then day
        val groupedByPlan = plan.groupBy { it.planId.ifBlank { "General" } }
        groupedByPlan.forEach { (planId, planTasks) ->
            item {
                Text(
                    text = planId,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            val groupedByDay = planTasks.groupBy { it.day }
            groupedByDay.forEach { (day, tasks) ->
                item {
                    DayHeader(day)
                }
                items(tasks) { task ->
                    PremiumTaskCard(task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

// Re-using helper components from previous version
@Composable
fun PremiumTaskCard(task: StudyPlanItem, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (task.status == "completed") 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (task.status == "completed") Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = if (task.status == "completed") Icons.Default.CheckCircle else Icons.Default.MenuBook, contentDescription = null, tint = if (task.status == "completed") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeBadge(task.time_slot)
                    Spacer(modifier = Modifier.width(8.dp))
                    DurationBadge("${task.duration_minutes}m")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = task.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = task.task, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DurationBadge(duration: String) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(text = duration, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
fun TaskOptionsContent(task: StudyPlanItem, onComplete: (StudyPlanItem) -> Unit, onReschedule: (StudyPlanItem) -> Unit, onRegenerateTask: (StudyPlanItem) -> Unit, onDelete: (StudyPlanItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
        Text(task.topic, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(task.task, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        OptionItem(Icons.Default.CheckCircle, "Mark Complete", Color(0xFF4CAF50)) { onComplete(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.EventRepeat, "Reschedule", MaterialTheme.colorScheme.primary) { onReschedule(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.AutoAwesome, "Regenerate Task", MaterialTheme.colorScheme.secondary) { onRegenerateTask(task) }
        Spacer(modifier = Modifier.height(8.dp))
        OptionItem(Icons.Default.Delete, "Delete Task", MaterialTheme.colorScheme.error) { onDelete(task) }
    }
}

@Composable
fun AddCustomTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var day by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Custom Task") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") })
    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (mins)") }) } }, confirmButton = { Button(onClick = { onAdd(day, title, topic, duration.toIntOrNull() ?: 60) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun DayHeader(day: String) {
    Text(text = day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
}

@Composable
fun ProgressCard(progress: Float) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Overall Progress", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.onPrimary, trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TimeBadge(time: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubjectInputSection(subject: String, onSubjectChange: (String) -> Unit, onGenerate: () -> Unit, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("AI Study Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("What subject would you like to add?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = subject, onValueChange = onSubjectChange, label = { Text("Subject (e.g. Kotlin)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGenerate, enabled = subject.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Get Topics", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PlanPreviewSection(plan: List<StudyPlanItem>, onAccept: () -> Unit, onRegenerate: () -> Unit, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Review New Schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val grouped = plan.groupBy { it.day }
            grouped.forEach { (day, tasks) ->
                item { DayHeader(day) }
                items(tasks) { task -> PremiumTaskCard(task, onClick = {}) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = !isLoading) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Regenerate")
            }
            Button(onClick = onAccept, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), enabled = !isLoading) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Accept & Save")
            }
        }
    }
}
