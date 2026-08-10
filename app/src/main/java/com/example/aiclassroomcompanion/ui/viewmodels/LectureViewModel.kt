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
                _notesState.value = AIState.Success(response ?: "No notes generated")
            } catch (e: Exception) {
                _notesState.value = AIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun generateSummary(transcription: String) {
        viewModelScope.launch {
            _summaryState.value = AIState.Processing
            try {
                val response = hfService.summarize(transcription)
                _summaryState.value = AIState.Success(response ?: "No summary generated")
            } catch (e: Exception) {
                _summaryState.value = AIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun generateFlashcards(transcription: String) {
        viewModelScope.launch {
            _flashcardsState.value = AIState.Processing
            try {
                val prompt = "Create 5 flashcards from this lecture. For each flashcard, provide a 'question' and an 'answer'. Format your response as a simple list where each card is on a new line like 'Q: [question] | A: [answer]'. Transcription: $transcription"
                val response = hfService.generateText(prompt)
                val cards = response?.split("\n")?.filter { it.contains("|") }?.map { line ->
                    val parts = line.split("|")
                    com.example.aiclassroomcompanion.ui.screens.Flashcard(
                        question = parts[0].replace("Q:", "").trim(),
                        answer = parts[1].replace("A:", "").trim()
                    )
                } ?: emptyList()
                _flashcardsState.value = AIState.FlashcardsSuccess(cards)
            } catch (e: Exception) {
                _flashcardsState.value = AIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun generateQuiz(transcription: String) {
        viewModelScope.launch {
            _quizState.value = AIState.Processing
            try {
                val prompt = "Create a 3-question multiple choice quiz from this lecture. For each question, provide the question text, 4 options, and the index of the correct answer (0-3). Format as 'Q: [text] | O: [opt1, opt2, opt3, opt4] | C: [index]'. Transcription: $transcription"
                val response = hfService.generateText(prompt)
                val questions = response?.split("\n")?.filter { it.contains("|") }?.map { line ->
                    val parts = line.split("|")
                    val options = parts[1].replace("O:", "").trim().removeSurrounding("[", "]").split(",").map { it.trim() }
                    com.example.aiclassroomcompanion.ui.screens.Question(
                        text = parts[0].replace("Q:", "").trim(),
                        options = options,
                        correctAnswer = parts[2].replace("C:", "").trim().toIntOrNull() ?: 0
                    )
                } ?: emptyList()
                _quizState.value = AIState.QuizSuccess(questions)
            } catch (e: Exception) {
                _quizState.value = AIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun translateContent(text: String, targetLanguage: String) {
        viewModelScope.launch {
            _notesState.value = AIState.Processing
            try {
                val prompt = "Translate the following classroom notes into $targetLanguage while maintaining the Markdown formatting and structure: $text"
                val response = hfService.generateText(prompt)
                _notesState.value = AIState.Success(response ?: "Translation failed")
            } catch (e: Exception) {
                _notesState.value = AIState.Error(e.message ?: "An error occurred")
            }
        }
    }
}
