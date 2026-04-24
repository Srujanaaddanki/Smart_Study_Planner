package com.example.studyplannerai.data.model

data class Subject(
    val id: String = "",
    val name: String = "",
    val priority: Int = 3, // 1-5
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val colorHex: String = "#6200EE",
    val createdAt: Long = System.currentTimeMillis()
)
