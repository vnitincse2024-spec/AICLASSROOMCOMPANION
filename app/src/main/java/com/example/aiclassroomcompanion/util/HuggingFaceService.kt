package com.example.aiclassroomcompanion.util

import android.util.Log
import com.example.aiclassroomcompanion.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Chat Completions Request ────────────────────────────────────────────────
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1000,
    val stream: Boolean = false
)

// ── Chat Completions Response ───────────────────────────────────────────────
@Serializable
data class ChatChoice(
    val message: ChatMessage
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

// ───────────────────────────────────────────────────────────────────────────
class HuggingFaceService {

    private val TAG = "HuggingFaceService"

    private val apiKey: String = BuildConfig.HF_API_KEY

    // Qwen2.5-7B-Instruct supports the OpenAI-compatible chat completions endpoint
    private val model = "Qwen/Qwen2.5-7B-Instruct"
    private val baseUrl = "https://api-inference.huggingface.co/models/$model/v1/chat/completions"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Core chat completion call.
     * @param systemPrompt  Instructions that shape the model's behaviour.
     * @param userPrompt    The actual user request / content to process.
     * @param maxTokens     Upper limit on generated tokens (default 1000).
     * @return The model's reply text, or null on any error.
     */
    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 1000
    ): String? {
        if (apiKey.isBlank() || apiKey == "YOUR_HUGGING_FACE_TOKEN_HERE") {
            Log.w(TAG, "HF_API_KEY is not set in local.properties — skipping API call")
            return null
        }
        return try {
            val response: ChatResponse = client.post(baseUrl) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = model,
                        messages = listOf(
                            ChatMessage("system", systemPrompt),
                            ChatMessage("user", userPrompt)
                        ),
                        maxTokens = maxTokens
                    )
                )
            }.body()
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            Log.d(TAG, "API response (first 200): ${content?.take(200)}")
            content
        } catch (e: Exception) {
            Log.e(TAG, "chat() failed: ${e.message}", e)
            null
        }
    }

    // ── Convenience helpers used by LectureViewModel ────────────────────────

    /** Generate free-form text from a single user prompt. */
    suspend fun generateText(userPrompt: String): String? =
        chat(
            systemPrompt = "You are a helpful AI classroom assistant. Follow the instructions precisely and return only the requested output with no extra commentary.",
            userPrompt = userPrompt
        )

    /** Summarise a lecture transcription. */
    suspend fun summarize(transcription: String): String? =
        chat(
            systemPrompt = "You are an expert academic summariser. Be concise and accurate.",
            userPrompt = "Summarise the following lecture transcription in a short paragraph followed by exactly 5 bullet-point key takeaways.\n\nTranscription:\n$transcription"
        )
}
