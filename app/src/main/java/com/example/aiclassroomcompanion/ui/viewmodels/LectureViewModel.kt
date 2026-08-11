package com.example.aiclassroomcompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclassroomcompanion.util.HuggingFaceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AIState {
    object Idle : AIState()
    object Processing : AIState()
    data class Success(val result: String) : AIState()
    data class FlashcardsSuccess(val flashcards: List<com.example.aiclassroomcompanion.ui.screens.Flashcard>) : AIState()
    data class QuizSuccess(val questions: List<com.example.aiclassroomcompanion.ui.screens.Question>) : AIState()
    data class Error(val message: String) : AIState()
}

class LectureViewModel : ViewModel() {
    private val hfService = HuggingFaceService()

    private val _notesState = MutableStateFlow<AIState>(AIState.Idle)
    val notesState: StateFlow<AIState> = _notesState

    private val _summaryState = MutableStateFlow<AIState>(AIState.Idle)
    val summaryState: StateFlow<AIState> = _summaryState

    private val _flashcardsState = MutableStateFlow<AIState>(AIState.Idle)
    val flashcardsState: StateFlow<AIState> = _flashcardsState

    private val _quizState = MutableStateFlow<AIState>(AIState.Idle)
    val quizState: StateFlow<AIState> = _quizState

    private val _transcriptionState = MutableStateFlow<AIState>(AIState.Idle)
    val transcriptionState: StateFlow<AIState> = _transcriptionState

    fun transcribeAndProcess(audioBytes: ByteArray) {
        viewModelScope.launch {
            _transcriptionState.value = AIState.Processing
            try {
                // Mock transcription for now
                val transcription = "This is a lecture about Data Structures. We are covering Stacks and Queues."
                _transcriptionState.value = AIState.Success(transcription)
                
                generateNotes(transcription)
                generateSummary(transcription)
                generateFlashcards(transcription)
                generateQuiz(transcription)
                
            } catch (e: Exception) {
                _transcriptionState.value = AIState.Error(e.message ?: "Processing failed")
            }
        }
    }

    fun generateNotes(transcription: String) {
        viewModelScope.launch {
            _notesState.value = AIState.Processing
            try {
                val prompt = "Generate structured classroom notes from this transcription. Use Markdown headers and bullet points. Transcription: $transcription"
                val response = hfService.generateText(prompt)
                val notesText = if (!response.isNull_or_blank_or_fallback()) response!! else getFallbackNotes(transcription)
                _notesState.value = AIState.Success(notesText)
            } catch (e: Exception) {
                _notesState.value = AIState.Success(getFallbackNotes(transcription))
            }
        }
    }

    fun generateSummary(transcription: String) {
        viewModelScope.launch {
            _summaryState.value = AIState.Processing
            try {
                val response = hfService.summarize(transcription)
                val summaryText = if (!response.isNull_or_blank_or_fallback()) response!! else getFallbackSummary(transcription)
                _summaryState.value = AIState.Success(summaryText)
            } catch (e: Exception) {
                _summaryState.value = AIState.Success(getFallbackSummary(transcription))
            }
        }
    }

    fun generateFlashcards(transcription: String) {
        viewModelScope.launch {
            _flashcardsState.value = AIState.Processing
            try {
                val prompt = "Create 5 flashcards from this lecture. For each flashcard, provide a 'question' and an 'answer'. Format your response as a simple list where each card is on a new line like 'Q: [question] | A: [answer]'. Transcription: $transcription"
                val response = hfService.generateText(prompt)
                val cards = response?.split("\n")?.filter { it.contains("|") }?.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        com.example.aiclassroomcompanion.ui.screens.Flashcard(
                            question = parts[0].replace("Q:", "").trim(),
                            answer = parts[1].replace("A:", "").trim()
                        )
                    } else null
                } ?: emptyList()
                
                val finalCards = if (cards.isNotEmpty()) cards else getFallbackFlashcards(transcription)
                _flashcardsState.value = AIState.FlashcardsSuccess(finalCards)
            } catch (e: Exception) {
                _flashcardsState.value = AIState.FlashcardsSuccess(getFallbackFlashcards(transcription))
            }
        }
    }

    fun generateQuiz(transcription: String) {
        viewModelScope.launch {
            _quizState.value = AIState.Processing
            try {
                val prompt = "Create a 3-question multiple choice quiz from this lecture. For each question, provide the question text, 4 options, and the index of the correct answer (0-3). Format as 'Q: [text] | O: [opt1, opt2, opt3, opt4] | C: [index]'. Transcription: $transcription"
                val response = hfService.generateText(prompt)
                val questions = response?.split("\n")?.filter { it.contains("|") }?.mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 3) {
                        val options = parts[1].replace("O:", "").trim().removeSurrounding("[", "]").split(",").map { it.trim() }
                        com.example.aiclassroomcompanion.ui.screens.Question(
                            text = parts[0].replace("Q:", "").trim(),
                            options = options,
                            correctAnswer = parts[2].replace("C:", "").trim().toIntOrNull() ?: 0
                        )
                    } else null
                } ?: emptyList()
                
                val finalQuestions = if (questions.isNotEmpty()) questions else getFallbackQuiz(transcription)
                _quizState.value = AIState.QuizSuccess(finalQuestions)
            } catch (e: Exception) {
                _quizState.value = AIState.QuizSuccess(getFallbackQuiz(transcription))
            }
        }
    }

    fun translateContent(text: String, targetLanguage: String) {
        viewModelScope.launch {
            _notesState.value = AIState.Processing
            try {
                val prompt = "Translate the following classroom notes into $targetLanguage while maintaining the Markdown formatting and structure: $text"
                val response = hfService.generateText(prompt)
                val translated = if (!response.isNullOrBlank()) response else "Translated ($targetLanguage):\n\n$text"
                _notesState.value = AIState.Success(translated)
            } catch (e: Exception) {
                _notesState.value = AIState.Success("Translated ($targetLanguage):\n\n$text")
            }
        }
    }

    private fun String?.isNull_or_blank_or_fallback(): Boolean = this.isNullOrBlank()

    private fun getFallbackNotes(transcription: String): String {
        if (transcription == "no_data" || transcription.isBlank()) {
            return """# 📚 Lecture Notes

No transcription available. Please record a lecture and try again."""
        }

        // Split transcription into sentences for discussion points
        val sentences = transcription.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.length > 10 }
            .take(5)

        val discussionPoints = sentences.mapIndexed { i, s ->
            "${i + 1}. $s."
        }.joinToString("\n")

        val preview = transcription.take(120).trimEnd()

        return """
# 📚 Lecture Notes

## 🔍 Overview
$preview...

## 📝 Key Points from Recording
$discussionPoints

## 💡 Takeaways
- Review the full transcription in the **Transcription** tab.
- Focus on the main ideas discussed during the session.
        """.trimIndent()
    }

    private fun getFallbackSummary(transcription: String): String {
        if (transcription == "no_data" || transcription.isBlank()) {
            return "No transcription available. Please record a lecture and try again."
        }

        val preview = transcription.take(500)
        val ellipsis = if (transcription.length > 500) "..." else ""
        val wordCount = transcription.split(Regex("\\s+")).size
        val sentences = transcription.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.length > 8 }

        val bulletPoints = sentences.take(5).joinToString("\n") { "• $it." }

        return """
📋 Lecture Summary  ($wordCount words recorded)

$preview$ellipsis

Key Points:
$bulletPoints
        """.trimIndent()
    }

    private fun getFallbackFlashcards(transcription: String): List<com.example.aiclassroomcompanion.ui.screens.Flashcard> {
        if (transcription == "no_data" || transcription.isBlank()) {
            return listOf(
                com.example.aiclassroomcompanion.ui.screens.Flashcard(
                    question = "No transcription found",
                    answer = "Please record a lecture first, then re-open Flashcards."
                )
            )
        }

        // Build flashcards from actual sentences in the transcription
        val sentences = transcription
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.split(" ").size >= 5 } // at least 5 words
            .take(5)

        return sentences.mapIndexed { index, sentence ->
            val words = sentence.split(" ")
            // Create a fill-in-the-blank style card from the sentence
            val keyWord = words.maxByOrNull { it.length } ?: words.last()
            com.example.aiclassroomcompanion.ui.screens.Flashcard(
                question = "What was said about \"${keyWord.take(30)}\" in the lecture?",
                answer = "$sentence."
            )
        }.ifEmpty {
            listOf(
                com.example.aiclassroomcompanion.ui.screens.Flashcard(
                    question = "What is the main topic of this lecture?",
                    answer = transcription.take(200)
                )
            )
        }
    }

    private fun getFallbackQuiz(transcription: String): List<com.example.aiclassroomcompanion.ui.screens.Question> {
        if (transcription == "no_data" || transcription.isBlank()) {
            return listOf(
                com.example.aiclassroomcompanion.ui.screens.Question(
                    text = "No transcription found — what should you do first?",
                    options = listOf(
                        "Record a lecture",
                        "Open flashcards",
                        "Check settings",
                        "Close the app"
                    ),
                    correctAnswer = 0
                )
            )
        }

        // Build simple comprehension questions from transcription sentences
        val sentences = transcription
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.split(" ").size >= 6 }
            .take(3)

        return sentences.mapIndexed { index, sentence ->
            val words = sentence.split(" ").filter { it.length > 4 }
            val keyWord = words.getOrElse(index) { words.firstOrNull() ?: "topic" }
            val wrongOptions = listOf(
                "It was not mentioned in the lecture",
                "The speaker skipped this part",
                "This was discussed in a different session"
            )
            val correctOption = sentence.take(80)
            val allOptions = (wrongOptions + correctOption).shuffled()
            val correctIndex = allOptions.indexOf(correctOption).coerceAtLeast(0)

            com.example.aiclassroomcompanion.ui.screens.Question(
                text = "Which statement about \"${keyWord.take(20)}\" is correct based on the lecture?",
                options = allOptions,
                correctAnswer = correctIndex
            )
        }.ifEmpty {
            listOf(
                com.example.aiclassroomcompanion.ui.screens.Question(
                    text = "What is the main subject discussed in this lecture?",
                    options = listOf(
                        transcription.take(60),
                        "Something unrelated",
                        "Not discussed",
                        "Cannot determine"
                    ),
                    correctAnswer = 0
                )
            )
        }
    }
}
