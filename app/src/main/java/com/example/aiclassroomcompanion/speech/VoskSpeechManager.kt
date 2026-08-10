package com.example.aiclassroomcompanion.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.io.IOException
import kotlin.math.sqrt

class VoskSpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onError: (Exception) -> Unit,
    private val onVolumeChanged: (Float) -> Unit = {}
) {

    private var model: Model? = null
    private var isUnpacking = false
    private var pendingStartFile: File? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun initModel() {
        if (model != null || isUnpacking) return
        isUnpacking = true
        StorageService.unpack(context, "model-en-us", "model",
            { loadedModel ->
                synchronized(this@VoskSpeechManager) {
                    this.model = loadedModel
                    this.isUnpacking = false
                    Log.d("VoskSpeechManager", "Model successfully loaded")
                    if (pendingStartFile != null) {
                        val file = pendingStartFile
                        pendingStartFile = null
                        startListeningInternal(file)
                    }
                }
            },
            { exception ->
                synchronized(this@VoskSpeechManager) {
                    this.isUnpacking = false
                    this.pendingStartFile = null
                    Log.e("VoskSpeechManager", "Failed to unpack Vosk model", exception)
                    mainHandler.post { onError(exception) }
                }
            }
        )
    }

    @Synchronized
    fun startListening(outputWavFile: File? = null) {
        if (isRecording) return
        if (model == null) {
            pendingStartFile = outputWavFile
            initModel()
        } else {
            startListeningInternal(outputWavFile)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startListeningInternal(outputWavFile: File?) {
        val currentModel = model ?: return
        isRecording = true

        recordingThread = Thread {
            var audioRecord: AudioRecord? = null
            var recognizer: Recognizer? = null
            var outputStream: FileOutputStream? = null
            var totalAudioLen: Long = 0

            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                if (minBufferSize <= 0) {
                    throw IOException("Invalid AudioRecord minBufferSize: $minBufferSize")
                }
                val bufferSize = maxOf(minBufferSize, 4096)

                recognizer = Recognizer(currentModel, sampleRate.toFloat())
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (outputWavFile != null) {
                    outputStream = FileOutputStream(outputWavFile)
                    writeWavHeaderPlaceholder(outputStream)
                }

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    throw IOException("AudioRecord initialization failed")
                }

                audioRecord.startRecording()
                Log.d("VoskSpeechManager", "Recording started")

                val buffer = ByteArray(bufferSize)

                while (isRecording && !Thread.currentThread().isInterrupted) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (outputStream != null) {
                            outputStream.write(buffer, 0, read)
                            totalAudioLen += read
                        }

                        if (recognizer.acceptWaveForm(buffer, read)) {
                            val res = recognizer.result
                            mainHandler.post { onResult(res) }
                        } else {
                            val partial = recognizer.partialResult
                            mainHandler.post { onPartialResult(partial) }
                        }

                        val volume = calculateRmsVolume(buffer, read)
                        mainHandler.post { onVolumeChanged(volume) }
                    }
                }

                val finalRes = recognizer.finalResult
                mainHandler.post { onResult(finalRes) }

            } catch (e: Exception) {
                Log.e("VoskSpeechManager", "Error in recording thread", e)
                mainHandler.post { onError(e) }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    Log.e("VoskSpeechManager", "Error releasing AudioRecord", e)
                }
                try {
                    outputStream?.flush()
                    outputStream?.close()
                } catch (e: Exception) {
                    Log.e("VoskSpeechManager", "Error closing output stream", e)
                }

                if (outputWavFile != null && totalAudioLen > 0) {
                    try {
                        updateWavHeader(outputWavFile, totalAudioLen, 16000, 1, 16)
                    } catch (e: Exception) {
                        Log.e("VoskSpeechManager", "Error updating WAV header", e)
                    }
                }

                try {
                    recognizer?.close()
                } catch (e: Exception) {
                    Log.e("VoskSpeechManager", "Error closing recognizer", e)
                }
            }
        }.apply { start() }
    }

    @Synchronized
    fun stopListening() {
        if (!isRecording) return
        isRecording = false
        pendingStartFile = null
        recordingThread?.interrupt()
        recordingThread = null
    }

    @Synchronized
    fun destroy() {
        stopListening()
        model = null
    }

    private fun calculateRmsVolume(buffer: ByteArray, readBytes: Int): Float {
        var sum = 0.0
        val sampleCount = readBytes / 2
        if (sampleCount == 0) return 0f
        for (i in 0 until readBytes - 1 step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val shortVal = sample.toShort()
            sum += shortVal * shortVal
        }
        val mean = sum / sampleCount
        val rms = sqrt(mean)
        val db = if (rms > 0) 20 * Math.log10(rms / 32768.0) + 90 else 0.0
        return (db / 10.0).toFloat().coerceIn(0f, 10f)
    }

    private fun writeWavHeaderPlaceholder(out: FileOutputStream) {
        val header = ByteArray(44)
        out.write(header, 0, 44)
    }

    private fun updateWavHeader(file: File, totalAudioLen: Long, sampleRate: Int, channels: Int, bitDepth: Int) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitDepth / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1
        header[21] = 0

        header[22] = channels.toByte()
        header[23] = 0

        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = (channels * bitDepth / 8).toByte()
        header[33] = 0

        header[34] = bitDepth.toByte()
        header[35] = 0

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header, 0, 44)
        }
    }
}

