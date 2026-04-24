package com.example.studyplannerai.data.remote

import com.example.studyplannerai.data.model.GroqRequest
import com.example.studyplannerai.data.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse
}
