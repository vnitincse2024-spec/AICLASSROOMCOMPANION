package com.example.aiclassroomcompanion

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

class SpeechAndAudioPipelineTest {

    private fun extractJsonValue(json: String, key: String): String {
        val regex = "\"$key\"\\s*:\\s*\"(.*?)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    @Test
    fun testVoskJsonParsing_validText() {
        val jsonString = """{"text" : "welcome to data structures lecture"}"""
        val text = extractJsonValue(jsonString, "text")
        assertEquals("welcome to data structures lecture", text)
    }

    @Test
    fun testVoskJsonParsing_validPartialText() {
        val jsonString = """{"partial" : "welcome to data"}"""
        val partial = extractJsonValue(jsonString, "partial")
        assertEquals("welcome to data", partial)
    }

    @Test
    fun testVoskJsonParsing_emptyOrMalformed() {
        val emptyJson = """{"text" : ""}"""
        val text = extractJsonValue(emptyJson, "text")
        assertTrue(text.isBlank())

        val missingKeyJson = """{"other" : "value"}"""
        val missingText = extractJsonValue(missingKeyJson, "text")
        assertTrue(missingText.isBlank())
    }

    @Test
    fun testRmsVolumeCalculation_silence() {
        val buffer = ByteArray(1024) { 0 }
        val volume = calculateRmsVolume(buffer, buffer.size)
        assertEquals(0.0f, volume, 0.01f)
    }

    @Test
    fun testRmsVolumeCalculation_audioSignal() {
        val buffer = ByteArray(1024)
        // Generate a 1kHz sine wave sample into 16-bit PCM buffer
        for (i in 0 until 512) {
            val sample = (10000 * Math.sin(2.0 * Math.PI * i / 16.0)).toInt().toShort()
            buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        val volume = calculateRmsVolume(buffer, buffer.size)
        assertTrue("Volume should be greater than 0 for active audio", volume > 0f)
    }

    @Test
    fun testWavHeaderGeneration() {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.deleteOnExit()

        // Write 44 byte header placeholder + 3200 bytes PCM data
        val out = tempFile.outputStream()
        out.write(ByteArray(44))
        val pcmData = ByteArray(3200) { 0x10 }
        out.write(pcmData)
        out.flush()
        out.close()

        updateWavHeader(tempFile, pcmData.size.toLong(), 16000, 1, 16)

        val headerBytes = tempFile.readBytes().sliceArray(0 until 44)
        val riffHeader = String(headerBytes, 0, 4)
        val waveHeader = String(headerBytes, 8, 4)
        val fmtHeader = String(headerBytes, 12, 4)
        val dataHeader = String(headerBytes, 36, 4)

        assertEquals("RIFF", riffHeader)
        assertEquals("WAVE", waveHeader)
        assertEquals("fmt ", fmtHeader)
        assertEquals("data", dataHeader)
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
