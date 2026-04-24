package com.example.studyplannerai.data.model

data class Task(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subject: String = "", // Keeping for backward compatibility/quick display
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 60,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
