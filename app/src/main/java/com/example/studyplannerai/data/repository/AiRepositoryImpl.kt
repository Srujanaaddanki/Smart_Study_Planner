package com.example.studyplannerai.data.repository

import android.util.Log
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.*
import com.example.studyplannerai.data.remote.GroqService
import com.example.studyplannerai.domain.repository.AiRepository
import com.google.gson.Gson
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val groqService: GroqService
) : AiRepository {
    
    // IMPORTANT: Replace with your own Groq API key.
    // Recommended: Move this to local.properties and access via BuildConfig.
    private val apiKey = "Bearer YOUR_GROQ_API_KEY_HERE"

    override suspend fun getTopicsForSubject(subject: String): Resource<List<String>> {
        val prompt = """
            {
              "mode": "topics",
              "subject": "$subject"
            }
            Generate 5–10 realistic topics. granular names. Return ONLY JSON.
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a professional API. Return ONLY valid JSON. No explanations."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            val jsonResponse = Gson().fromJson(responseText, Map::class.java)
            val topics = (jsonResponse["topics"] as List<String>)
            Resource.Success(topics)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun generateSchedule(
        selectedTopics: List<String>,
        availableHours: Int,
        preferredTime: String,
        breakDuration: Int
    ): Resource<List<StudyPlanItem>> {
        val prompt = """
            {
              "mode": "schedule",
              "selected_topics": ${Gson().toJson(selectedTopics)},
              "hours_per_day": $availableHours,
              "preferred_time": "$preferredTime",
              "break_duration": $breakDuration
            }
            Rules: 24h format, include breaks. 
            'task' must be a specific actionable instruction (e.g. "Solve 10 recursion problems" NOT "Study recursion").
            Return ONLY JSON with 'schedule' key.
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a professional API. Return ONLY valid JSON with 'schedule' key."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            val jsonResponse = Gson().fromJson(responseText, Map::class.java)
            val scheduleJson = Gson().toJson(jsonResponse["schedule"])
            val schedule = Gson().fromJson(scheduleJson, Array<StudyPlanItem>::class.java).toList()
            
            val finalizedSchedule = schedule.mapIndexed { index, item ->
                item.copy(id = "${System.currentTimeMillis()}_$index")
            }
            Resource.Success(finalizedSchedule)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun generateScheduleForSubject(subject: String): Resource<List<StudyPlanItem>> {
        val prompt = """
            {
              "mode": "generate",
              "subject": "$subject"
            }
            Return ONLY valid JSON with 'schedule' and 'history' (empty array) keys.
            Rules:
            - Use ISO dates (YYYY-MM-DD) for 'day' starting from today (${java.time.LocalDate.now()})
            - realistic time slots (HH:mm)
            - MUST include "duration_minutes" (30-120)
            - MUST include "status": "pending"
            - MUST include "reminder_offset_minutes": 10
            - actionable tasks
            - Include ALL generated tasks (no truncation)
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a senior backend API. Return ONLY valid JSON with 'schedule' and 'history' keys. No explanations."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            val jsonResponse = Gson().fromJson(responseText, Map::class.java)
            val scheduleJson = Gson().toJson(jsonResponse["schedule"])
            val schedule = Gson().fromJson(scheduleJson, Array<StudyPlanItem>::class.java).toList()
            
            val finalizedSchedule = schedule.mapIndexed { index, item ->
                item.copy(id = "${System.currentTimeMillis()}_$index")
            }
            Resource.Success(finalizedSchedule)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
}
