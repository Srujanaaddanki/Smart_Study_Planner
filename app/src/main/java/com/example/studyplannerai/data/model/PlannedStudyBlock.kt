package com.example.studyplannerai.data.model

data class PlannedStudyBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val taskTitle: String,
    val subjectName: String,
    val subjectColor: String = "#6200EE",
    val dateEpochDay: Long,
    val startTime: Long, // Epoch millis
    val endTime: Long, // Epoch millis
    val plannedMinutes: Int,
    val suggestedDeadlineEpochDay: Long,
    val isRescheduled: Boolean,
    val status: String = "Pending" // Pending, InProgress, Completed
)
