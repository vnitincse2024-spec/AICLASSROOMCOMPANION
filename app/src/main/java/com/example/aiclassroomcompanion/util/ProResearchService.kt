package com.example.aiclassroomcompanion.util

class ProResearchService {
    private val hfService = HuggingFaceService()

    suspend fun performAdvancedResearch(query: String): String? {
        return hfService.chat(
            systemPrompt = "You are an advanced academic research assistant. Provide a thorough, well-structured academic analysis.",
            userPrompt = "Provide a deep academic analysis for: $query",
            maxTokens = 1200
        )
    }
}
