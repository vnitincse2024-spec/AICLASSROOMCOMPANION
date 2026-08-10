package com.example.aiclassroomcompanion.util

import com.google.firebase.Timestamp

data class Lecture(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val date: Timestamp = Timestamp.now(),
    val duration: String = "",
    val audioUrl: String = "",
    val transcription: String = "",
    val notes: String = "",
    val summary: String = ""
)
