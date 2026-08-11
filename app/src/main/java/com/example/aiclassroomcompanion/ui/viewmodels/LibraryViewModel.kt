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
import kotlinx.coroutines.withTimeoutOrNull

sealed class LibraryState {
    object Loading : LibraryState()
    data class Success(val lectures: List<Lecture>) : LibraryState()
    data class Error(val message: String) : LibraryState()
}

class LibraryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _libraryState = MutableStateFlow<LibraryState>(
        if (LocalLectureStore.getLectures().isNotEmpty()) {
            LibraryState.Success(LocalLectureStore.getLectures())
        } else {
            LibraryState.Loading
        }
    )
    val libraryState: StateFlow<LibraryState> = _libraryState

    private val _recentLecture = MutableStateFlow<Lecture?>(LocalLectureStore.getLectures().firstOrNull())
    val recentLecture: StateFlow<Lecture?> = _recentLecture

    init {
        viewModelScope.launch {
            LocalLectureStore.localLectures.collect { localList ->
                updateState(localList)
            }
        }
        loadLectures()
    }

    private fun updateState(localList: List<Lecture>) {
        _libraryState.value = LibraryState.Success(localList)
        _recentLecture.value = localList.firstOrNull()
    }

    fun loadLectures() {
        val userId = auth.currentUser?.uid ?: "guest_user"
        val currentLocal = LocalLectureStore.getLectures()
        if (currentLocal.isEmpty() && _libraryState.value !is LibraryState.Success) {
            _libraryState.value = LibraryState.Loading
        }

        viewModelScope.launch {
            try {
                val remoteLectures = withTimeoutOrNull(5000) {
                    try {
                        val snapshot = firestore.collection("lectures")
                            .whereEqualTo("userId", userId)
                            .get()
                            .await()
                        snapshot.toObjects(Lecture::class.java)
                    } catch (e: Exception) {
                        emptyList<Lecture>()
                    }
                } ?: emptyList()

                val localList = LocalLectureStore.getLectures()
                val mergedMap = LinkedHashMap<String, Lecture>()

                localList.forEach { if (it.id.isNotEmpty()) mergedMap[it.id] = it else mergedMap[it.title] = it }
                remoteLectures.forEach { if (it.id.isNotEmpty()) mergedMap[it.id] = it }

                val allLectures = mergedMap.values.sortedByDescending { it.date }

                _libraryState.value = LibraryState.Success(allLectures)
                _recentLecture.value = allLectures.firstOrNull()
            } catch (e: Exception) {
                val fallbackList = LocalLectureStore.getLectures()
                _libraryState.value = LibraryState.Success(fallbackList)
                _recentLecture.value = fallbackList.firstOrNull()
            }
        }
    }
}

