package com.example.aiclassroomcompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclassroomcompanion.util.Lecture
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
        loadLectures()
    }

    fun loadLectures() {
        val userId = auth.currentUser?.uid ?: return
        _libraryState.value = LibraryState.Loading
        
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("lectures")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                val lectures = snapshot.toObjects(Lecture::class.java)
                    .sortedByDescending { it.date }
                
                _libraryState.value = LibraryState.Success(lectures)
                _recentLecture.value = lectures.firstOrNull()
            } catch (e: Exception) {
                _libraryState.value = LibraryState.Error(e.message ?: "Failed to load library")
            }
        }
    }
}
