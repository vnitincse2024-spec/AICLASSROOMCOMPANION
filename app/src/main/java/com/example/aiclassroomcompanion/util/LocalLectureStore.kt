package com.example.aiclassroomcompanion.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocalLectureStore {
    private val _localLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val localLectures: StateFlow<List<Lecture>> = _localLectures

    fun addLecture(lecture: Lecture) {
        val current = _localLectures.value.toMutableList()
        current.removeAll { it.id == lecture.id && lecture.id.isNotEmpty() }
        current.add(0, lecture)
        _localLectures.value = current
    }

    fun getLectures(): List<Lecture> = _localLectures.value
}
