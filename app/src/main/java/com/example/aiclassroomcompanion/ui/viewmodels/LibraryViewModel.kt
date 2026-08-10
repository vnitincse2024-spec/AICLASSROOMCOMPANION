package com.example.aiclassroomcompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclassroomcompanion.util.Lecture
import com.example.aiclassroomcompanion.util.LocalLectureStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LibraryState {
    object Loading : LibraryState()
    data class Success(val lectures: List<Lecture>) : LibraryState()
    data class Error(val message: String) : LibraryState()
}

class LibraryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _libraryState = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val libraryState: StateFlow<LibraryState> = _libraryState

    private val _recentLecture = MutableStateFlow<Lecture?>(null)
    val recentLecture: StateFlow<Lecture?> = _recentLecture

    init {
        viewModelScope.launch {
            LocalLectureStore.localLectures.collect {
                loadLectures()
            }
        }
    }

    fun loadLectures() {
        val userId = auth.currentUser?.uid ?: "guest_user"
        _libraryState.value = LibraryState.Loading
        
        viewModelScope.launch {
            try {
                val remoteLectures = try {
                    val snapshot = firestore.collection("lectures")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    snapshot.toObjects(Lecture::class.java)
                } catch (e: Exception) {
                    emptyList<Lecture>()
                }
                
                val localList = LocalLectureStore.getLectures()
                val mergedMap = LinkedHashMap<String, Lecture>()
                
                localList.forEach { if (it.id.isNotEmpty()) mergedMap[it.id] = it else mergedMap[it.title] = it }
                remoteLectures.forEach { if (it.id.isNotEmpty()) mergedMap[it.id] = it }

                val allLectures = mergedMap.values.sortedByDescending { it.date }
                
                _libraryState.value = LibraryState.Success(allLectures)
                _recentLecture.value = allLectures.firstOrNull()
            } catch (e: Exception) {
                val fallbackList = LocalLectureStore.getLectures()
                if (fallbackList.isNotEmpty()) {
                    _libraryState.value = LibraryState.Success(fallbackList)
                    _recentLecture.value = fallbackList.firstOrNull()
                } else {
                    _libraryState.value = LibraryState.Error(e.message ?: "Failed to load library")
                }
            }
        }
    }
}
