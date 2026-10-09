package com.example.intellipatassignment.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.ui.common.STOP_TIMEOUT_MS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log

sealed interface CourseDetailsUiState {
    data object Loading : CourseDetailsUiState
    data object NotFound : CourseDetailsUiState
    data class Success(val course: Course) : CourseDetailsUiState
}

sealed interface CourseDetailsEvent {
    data object MarkCompletedFailed : CourseDetailsEvent
}

class CourseDetailsViewModel(
    private val courseId: Int,
    private val repository: CourseRepository,
) : ViewModel() {
    val uiState: StateFlow<CourseDetailsUiState> = repository.observeCourse(courseId)
        .map { course ->
            if (course == null) CourseDetailsUiState.NotFound else CourseDetailsUiState.Success(course)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CourseDetailsUiState.Loading)

    private companion object {
        const val TAG = "CourseDetailsVM"
    }

    private val _events = Channel<CourseDetailsEvent>(Channel.BUFFERED)
    val events: Flow<CourseDetailsEvent> = _events.receiveAsFlow()

    fun onMarkCompleted(lessonId: Int) {
        Log.d(TAG, "mark completed requested for lesson $lessonId (course $courseId)")
        viewModelScope.launch {
            try {
                repository.markLessonCompleted(lessonId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "marking lesson $lessonId failed: ${e::class.java.simpleName}")
                _events.send(CourseDetailsEvent.MarkCompletedFailed)
            }
        }
    }
}
