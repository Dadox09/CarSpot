package com.example.concessionarioapp.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// --- Data Classes for OpenAI API ---

data class AIRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

data class Message(
    val role: String,
    val content: String
)

data class AIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

// --- Retrofit Service Interface ---

interface AIService {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer ${com.example.concessionarioapp.BuildConfig.OPENAI_API_KEY}"
    )
    @POST("v1/chat/completions")
    suspend fun getCompletion(@Body request: AIRequest): Response<AIResponse>
}

// --- Retrofit Client Singleton ---

object RetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"

    val instance: AIService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(AIService::class.java)
    }
}
