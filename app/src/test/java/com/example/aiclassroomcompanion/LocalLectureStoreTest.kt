package com.example.aiclassroomcompanion

import com.example.aiclassroomcompanion.util.Lecture
import com.example.aiclassroomcompanion.util.LocalLectureStore
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class LocalLectureStoreTest {

    @Test
    fun testAddAndGetLecturesInMemory() {
        val lecture1 = Lecture(
            id = "test_1",
            title = "Data Structures 101",
            transcription = "Stacks and Queues introduction",
            type = "Recorded"
        )
        val lecture2 = Lecture(
            id = "test_2",
            title = "Algorithms 201",
            transcription = "Binary Search Trees",
            type = "Recorded"
        )

        LocalLectureStore.addLecture(lecture1)
        LocalLectureStore.addLecture(lecture2)

        val lectures = LocalLectureStore.getLectures()
        assertTrue(lectures.size >= 2)
        assertEquals("test_2", lectures[0].id)
        assertEquals("test_1", lectures[1].id)
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val timestamp = Timestamp(1700000000L, 0)
        val originalLecture = Lecture(
            id = "unique_123",
            userId = "user_abc",
            title = "Physics 1",
            date = timestamp,
            duration = "05:30",
            audioUrl = "file:///path/to/audio.wav",
            transcription = "Newton's laws of motion",
            notes = "Key notes",
            summary = "Summary text",
            type = "Recorded"
        )

        val jsonString = LocalLectureStore.serializeLecturesToJson(listOf(originalLecture))
        val parsedList = LocalLectureStore.parseJsonToLectures(jsonString)
        assertEquals(1, parsedList.size)

        val parsed = parsedList[0]
        assertEquals("unique_123", parsed.id)
        assertEquals("user_abc", parsed.userId)
        assertEquals("Physics 1", parsed.title)
        assertEquals(1700000000L, parsed.date.seconds)
        assertEquals("05:30", parsed.duration)
        assertEquals("Newton's laws of motion", parsed.transcription)
        assertEquals("Recorded", parsed.type)
    }
}
