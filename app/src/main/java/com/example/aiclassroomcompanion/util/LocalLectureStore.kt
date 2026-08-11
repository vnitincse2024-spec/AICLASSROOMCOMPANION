package com.example.aiclassroomcompanion.util

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object LocalLectureStore {
    private const val PREFS_NAME = "local_lecture_store_prefs"
    private const val KEY_LECTURES = "saved_lectures_json"

    private val _localLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val localLectures: StateFlow<List<Lecture>> = _localLectures

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_LECTURES, null)
            if (!jsonString.isNullOrBlank()) {
                val list = parseJsonToLectures(jsonString)
                _localLectures.value = list
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e("LocalLectureStore", "Failed to load local lectures", e)
        }
    }

    fun addLecture(lecture: Lecture, context: Context? = null) {
        val current = _localLectures.value.toMutableList()
        current.removeAll { (it.id.isNotEmpty() && it.id == lecture.id) || (it.id.isEmpty() && it.title == lecture.title) }
        current.add(0, lecture)
        _localLectures.value = current

        context?.let { saveToDisk(it, current) }
    }

    fun saveToDisk(context: Context, lectures: List<Lecture> = _localLectures.value) {
        try {
            val jsonString = serializeLecturesToJson(lectures)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LECTURES, jsonString).apply()
        } catch (e: Throwable) {
            Log.e("LocalLectureStore", "Failed to save lectures to disk", e)
        }
    }

    fun getLectures(): List<Lecture> = _localLectures.value

    fun serializeLecturesToJson(lectures: List<Lecture>): String {
        return try {
            val jsonArray = JSONArray()
            lectures.forEach { lecture ->
                val obj = JSONObject().apply {
                    put("id", lecture.id)
                    put("userId", lecture.userId)
                    put("title", lecture.title)
                    put("dateSeconds", lecture.date.seconds)
                    put("dateNanoseconds", lecture.date.nanoseconds)
                    put("duration", lecture.duration)
                    put("audioUrl", lecture.audioUrl)
                    put("transcription", lecture.transcription)
                    put("notes", lecture.notes)
                    put("summary", lecture.summary)
                    put("type", lecture.type)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Throwable) {
            val items = lectures.joinToString(",") { l ->
                """{"id":"${escapeJson(l.id)}","userId":"${escapeJson(l.userId)}","title":"${escapeJson(l.title)}","dateSeconds":${l.date.seconds},"dateNanoseconds":${l.date.nanoseconds},"duration":"${escapeJson(l.duration)}","audioUrl":"${escapeJson(l.audioUrl)}","transcription":"${escapeJson(l.transcription)}","notes":"${escapeJson(l.notes)}","summary":"${escapeJson(l.summary)}","type":"${escapeJson(l.type)}"}"""
            }
            "[$items]"
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    fun parseJsonToLectures(jsonString: String): List<Lecture> {
        if (jsonString.isBlank()) return emptyList()
        val list = mutableListOf<Lecture>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val seconds = obj.optLong("dateSeconds", System.currentTimeMillis() / 1000)
                val nanoseconds = obj.optInt("dateNanoseconds", 0)
                val timestamp = Timestamp(seconds, nanoseconds)

                val lecture = Lecture(
                    id = obj.optString("id", ""),
                    userId = obj.optString("userId", ""),
                    title = obj.optString("title", ""),
                    date = timestamp,
                    duration = obj.optString("duration", ""),
                    audioUrl = obj.optString("audioUrl", ""),
                    transcription = obj.optString("transcription", ""),
                    notes = obj.optString("notes", ""),
                    summary = obj.optString("summary", ""),
                    type = obj.optString("type", "Recorded")
                )
                list.add(lecture)
            }
        } catch (e: Throwable) {
            val objects = jsonString.split(Regex("(?<=\\}),\\s*(?=\\{)"))
            for (rawObj in objects) {
                val cleanObj = rawObj.trim().removePrefix("[").removeSuffix("]")
                if (cleanObj.isBlank()) continue

                fun getField(key: String): String {
                    val regex = "\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"".toRegex()
                    return regex.find(cleanObj)?.groupValues?.get(1)
                        ?.replace("\\\"", "\"")
                        ?.replace("\\n", "\n")
                        ?.replace("\\r", "\r")
                        ?.replace("\\\\", "\\") ?: ""
                }
                fun getLongField(key: String, default: Long): Long {
                    val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                    return regex.find(cleanObj)?.groupValues?.get(1)?.toLongOrNull() ?: default
                }
                fun getIntField(key: String, default: Int): Int {
                    val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                    return regex.find(cleanObj)?.groupValues?.get(1)?.toIntOrNull() ?: default
                }

                val seconds = getLongField("dateSeconds", System.currentTimeMillis() / 1000)
                val nanoseconds = getIntField("dateNanoseconds", 0)

                val l = Lecture(
                    id = getField("id"),
                    userId = getField("userId"),
                    title = getField("title"),
                    date = Timestamp(seconds, nanoseconds),
                    duration = getField("duration"),
                    audioUrl = getField("audioUrl"),
                    transcription = getField("transcription"),
                    notes = getField("notes"),
                    summary = getField("summary"),
                    type = getField("type").ifEmpty { "Recorded" }
                )
                if (l.title.isNotEmpty() || l.id.isNotEmpty()) {
                    list.add(l)
                }
            }
        }
        return list
    }
}

