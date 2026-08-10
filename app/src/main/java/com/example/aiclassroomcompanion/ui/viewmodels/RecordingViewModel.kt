package com.example.aiclassroomcompanion.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclassroomcompanion.speech.VoskSpeechManager
import com.example.aiclassroomcompanion.util.AudioRecorder
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

    private val audioRecorder = AudioRecorder(application)
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val voskManager = VoskSpeechManager(
        context = application,
        onResult = { json ->
            val text = JSONObject(json).optString("text")
            if (text.isNotBlank()) {
                _transcription.value += " $text"
                _partialText.value = ""
            }
        },
        onPartialResult = { json ->
            val partial = JSONObject(json).optString("partial")
            _partialText.value = partial
        },
        onError = { e ->
            Log.e("Vosk", "Error", e)
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
        val file = File(getApplication<Application>().cacheDir, "lecture_${System.currentTimeMillis()}.mp4")
        currentFile = file
        audioRecorder.start(file)
        _isRecording.value = true
        _transcription.value = ""
        _partialText.value = ""
        startTimer()
        voskManager.startListening()
    }

    fun stopRecording() {
        audioRecorder.stop()
        voskManager.stopListening()
        _isRecording.value = false
        stopTimer()
    }

    fun stopAndSaveRecording(title: String) {
        stopRecording()
        
        val file = currentFile ?: return
        val durationString = String.format(Locale.getDefault(), "%02d:%02d", _seconds.value / 60, _seconds.value % 60)
        
        uploadLecture(file, title, durationString)
    }

    private fun uploadLecture(file: File, title: String, duration: String) {
        val userId = auth.currentUser?.uid ?: return
        val fileName = "lectures/$userId/${file.name}"
        val storageRef = storage.reference.child(fileName)
        
        _uploadState.value = UploadState.Uploading
        
        viewModelScope.launch {
            try {
                storageRef.putFile(Uri.fromFile(file)).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                val lecture = Lecture(
                    userId = userId,
                    title = title,
                    duration = duration,
                    audioUrl = downloadUrl,
                    transcription = _transcription.value
                )
                
                firestore.collection("lectures").add(lecture).await()
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
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
        audioRecorder.stop()
        voskManager.destroy()
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}
