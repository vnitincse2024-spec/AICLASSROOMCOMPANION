package com.example.aiclassroomcompanion.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.os.Handler
import android.os.Looper
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onTextChanged: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onVolumeChanged: (Float) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private var isListening = false

    fun startListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    setupListener()
                }
                isListening = true
                speechRecognizer?.startListening(intent)
                Log.d("Speech", "startListening called")
            } catch (e: Exception) {
                onError("Start Error: ${e.message}")
            }
        }
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { 
                Log.d("SpeechTest", "Ready for speech") 
            }
            override fun onBeginningOfSpeech() { 
                Log.d("SpeechTest", "Speech started") 
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                onVolumeChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                Log.d("SpeechTest", "Speech ended") 
            }

            override fun onError(error: Int) {
                Log.e("SpeechTest", "Error code: $error")
                
                if (isListening) {
                    mainHandler.postDelayed({ 
                        if (isListening) speechRecognizer?.startListening(intent) 
                    }, 500)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                Log.d("SpeechTest", "Result: $text")
                if (text != null) {
                    onFinalText(text)
                }
                if (isListening) startListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) {
                    onTextChanged(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun stopListening() {
        isListening = false
        mainHandler.post {
            speechRecognizer?.stopListening()
        }
    }

    fun destroy() {
        isListening = false
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
