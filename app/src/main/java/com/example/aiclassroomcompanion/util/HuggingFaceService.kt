package com.example.aiclassroomcompanion.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HFRequest(val inputs: String, val parameters: Map<String, String> = emptyMap())

@Serializable
data class HFResponse(val summary_text: String? = null, val generated_text: String? = null)

class HuggingFaceService {
    private val apiKey = "YOUR_HUGGING_FACE_TOKEN_HERE"
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val qwenModel = "Qwen/Qwen2.5-7B-Instruct"

    suspend fun summarize(text: String): String? {
        return try {
            val prompt = "Summarize the following lecture into a concise paragraph with 5 key bullet points:\n\n$text"
            val response: List<HFResponse> = client.post("https://api-inference.huggingface.co/models/$qwenModel") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(HFRequest(prompt))
            }.body()
            response.firstOrNull()?.generated_text?.removePrefix(prompt)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateText(prompt: String, model: String = "Qwen/Qwen2.5-7B-Instruct"): String? {
        return try {
            val response: List<HFResponse> = client.post("https://api-inference.huggingface.co/models/$model") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(HFRequest(prompt))
            }.body()
            response.firstOrNull()?.generated_text?.removePrefix(prompt)?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
