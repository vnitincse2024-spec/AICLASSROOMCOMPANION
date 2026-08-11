package com.example.aiclassroomcompanion.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclassroomcompanion.speech.SpeechRecognizerManager
import com.example.aiclassroomcompanion.speech.VoskSpeechManager
import com.example.aiclassroomcompanion.util.Lecture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.util.*

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var isUsingFallback = false

    private fun extractJsonValue(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "")
        } catch (e: Throwable) {
            val regex = "\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"".toRegex()
            regex.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
        }
    }

    private fun appendTranscription(text: String) {
        val current = _transcription.value
        _transcription.value = if (current.isBlank()) text else "$current $text"
        _partialText.value = ""
    }

    private val speechRecognizerManager by lazy {
        SpeechRecognizerManager(
            context = application,
            onTextChanged = { partial ->
                _partialText.value = partial
            },
            onFinalText = { text ->
                if (text.isNotBlank()) {
                    appendTranscription(text)
                }
            },
            onError = { err ->
                Log.e("RecordingViewModel", "SpeechRecognizer error: $err")
            },
            onVolumeChanged = { vol ->
                _volume.value = vol
            }
        )
    }

    private val voskManager = VoskSpeechManager(
        context = application,
        onResult = { json ->
            val text = extractJsonValue(json, "text")
            if (text.isNotBlank()) {
                appendTranscription(text)
            }
        },
        onPartialResult = { json ->
            val partial = extractJsonValue(json, "partial")
            _partialText.value = partial
        },
        onError = { e ->
            Log.e("RecordingViewModel", "Vosk Speech error, switching to SpeechRecognizer fallback", e)
            if (_isRecording.value && !isUsingFallback) {
                isUsingFallback = true
                speechRecognizerManager.startListening()
            }
        },
        onVolumeChanged = { vol ->
            _volume.value = vol
        }
    )

    private var timerJob: Job? = null
    private var currentFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _seconds = MutableStateFlow(0)
    val seconds: StateFlow<Int> = _seconds
    
    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription
    
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _volume = MutableStateFlow(0f)
    val volume: StateFlow<Float> = _volume

    init {
        voskManager.initModel()
    }

    fun startRecording() {
        val file = File(getApplication<Application>().cacheDir, "lecture_${System.currentTimeMillis()}.wav")
        currentFile = file
        isUsingFallback = false
        _isRecording.value = true
        _transcription.value = ""
        _partialText.value = ""
        startTimer()
        voskManager.startListening(file)
    }

    fun stopRecording() {
        voskManager.stopListening()
        if (isUsingFallback) {
            speechRecognizerManager.stopListening()
            isUsingFallback = false
        }
        val remainingPartial = _partialText.value.trim()
        if (remainingPartial.isNotEmpty()) {
            appendTranscription(remainingPartial)
        }
        _isRecording.value = false
        stopTimer()
    }

    fun stopAndSaveRecording(title: String) {
        stopRecording()
        
        val file = currentFile ?: File(getApplication<Application>().cacheDir, "lecture_${System.currentTimeMillis()}.wav")
        val durationString = String.format(Locale.getDefault(), "%02d:%02d", _seconds.value / 60, _seconds.value % 60)
        
        val userId = auth.currentUser?.uid ?: "guest_user"
        val docId = UUID.randomUUID().toString()
        
        val localLecture = Lecture(
            id = docId,
            userId = userId,
            title = title,
            date = com.google.firebase.Timestamp.now(),
            duration = durationString,
            audioUrl = Uri.fromFile(file).toString(),
            transcription = _transcription.value,
            type = "Recorded"
        )

        // Immediately save locally so UI updates instantly and data is persisted
        com.example.aiclassroomcompanion.util.LocalLectureStore.addLecture(localLecture, getApplication())
        _uploadState.value = UploadState.Success

        // Asynchronously sync to Firebase in background without blocking UI
        uploadLecture(file, localLecture)
    }

    private fun uploadLecture(file: File, lecture: Lecture) {
        viewModelScope.launch {
            try {
                var downloadUrl = lecture.audioUrl
                if (file.exists() && file.length() > 0) {
                    try {
                        val fileName = "lectures/${lecture.userId}/${file.name}"
                        val storageRef = storage.reference.child(fileName)
                        storageRef.putFile(Uri.fromFile(file)).await()
                        downloadUrl = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        Log.w("RecordingViewModel", "Firebase Storage upload failed, keeping local audio URI", e)
                    }
                }

                val docRef = firestore.collection("lectures").document(lecture.id)
                val updatedLecture = lecture.copy(audioUrl = downloadUrl)

                try {
                    docRef.set(updatedLecture).await()
                    com.example.aiclassroomcompanion.util.LocalLectureStore.addLecture(updatedLecture, getApplication())
                } catch (e: Exception) {
                    Log.w("RecordingViewModel", "Firestore save failed, kept saved locally", e)
                }
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Background sync exception", e)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _seconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _seconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        voskManager.destroy()
        if (isUsingFallback) {
            speechRecognizerManager.destroy()
        }
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}
