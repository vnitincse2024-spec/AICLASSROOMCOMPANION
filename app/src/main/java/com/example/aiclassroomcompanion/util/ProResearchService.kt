package com.example.aiclassroomcompanion.util

class ProResearchService {
    private val hfService = HuggingFaceService()

    suspend fun performAdvancedResearch(query: String): String? {
        val prompt = "You are an advanced academic research assistant. Provide a deep, academic analysis for: $query"
        return hfService.generateText(prompt, model = "Qwen/Qwen2.5-72B-Instruct")
    }
}
