package com.example.aiclassroomcompanion.speech

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.vosk.android.RecognitionListener
import java.io.IOException

class VoskSpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    fun initModel() {
        StorageService.unpack(context, "model-en-us", "model",
            { model ->
                this.model = model
                Log.d("Vosk", "Model loaded")
            },
            { exception ->
                Log.e("Vosk", "Failed to unpack model", exception)
                onError(exception)
            })
    }

    fun startListening() {
        val model = this.model ?: return
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            onError(e)
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
    }

    fun destroy() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    override fun onResult(hypothesis: String) {
        // Vosk returns JSON strings
        onResult(hypothesis)
    }

    override fun onPartialResult(hypothesis: String) {
        onPartialResult(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        onResult(hypothesis)
    }

    override fun onError(exception: Exception) {
        onError(exception)
    }

    override fun onTimeout() {
        speechService?.stop()
    }
}
