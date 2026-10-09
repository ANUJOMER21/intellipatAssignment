package com.example.intellipatassignment.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intellipatassignment.core.ConnectivityObserver
import com.example.intellipatassignment.core.ErrorKind
import com.example.intellipatassignment.core.toErrorKind
import com.example.intellipatassignment.domain.SyncPolicy
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.usecase.LogoutUseCase
import com.example.intellipatassignment.ui.common.STOP_TIMEOUT_MS
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log

sealed interface CoursesUiState {
    data object Loading : CoursesUiState
    data object Empty : CoursesUiState
    data class Error(val kind: ErrorKind) : CoursesUiState

    data class Success(
        val courses: List<Course>,
        val isRefreshing: Boolean,
        val refreshError: ErrorKind?,
        val lastSyncedAt: Long?,
        val isStale: Boolean,
    ) : CoursesUiState
}

private sealed interface RefreshState {
    data object Refreshing : RefreshState
    data object Idle : RefreshState
    data class Failed(val kind: ErrorKind, val at: Long) : RefreshState
}

class CoursesViewModel(
    private val repository: CourseRepository,
    private val logoutUseCase: LogoutUseCase,
    connectivity: ConnectivityObserver,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private companion object {
        const val TAG = "CoursesViewModel"
    }

    private val refreshState = MutableStateFlow<RefreshState>(RefreshState.Refreshing)
    private var refreshJob: Job? = null

    val uiState: StateFlow<CoursesUiState> = combine(
        repository.observeCourses(),
        repository.observeLastSyncedAt(),
        refreshState,
    ) { courses, lastSyncedAt, refresh ->

        val effective = if (refresh is RefreshState.Failed && lastSyncedAt != null && lastSyncedAt > refresh.at) {
            RefreshState.Idle
        } else {
            refresh
        }
        if (courses.isEmpty()) {
            when (effective) {
                RefreshState.Refreshing -> CoursesUiState.Loading
                is RefreshState.Failed -> CoursesUiState.Error(effective.kind)
                RefreshState.Idle -> CoursesUiState.Empty
            }
        } else {
            CoursesUiState.Success(
                courses = courses,
                isRefreshing = effective == RefreshState.Refreshing,
                refreshError = (effective as? RefreshState.Failed)?.kind,
                lastSyncedAt = lastSyncedAt,
                isStale = SyncPolicy.isStale(lastSyncedAt, now()),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CoursesUiState.Loading)

    val isOnline: StateFlow<Boolean> = connectivity.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), connectivity.isCurrentlyOnline())

    val rejectedLessons: StateFlow<List<String>> = repository.observeRejectedLessons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        if (refreshJob?.isActive == true) {
            Log.d(TAG, "refresh ignored: one is already running")
            return
        }
        Log.d(TAG, "refresh requested (force = $force)")
        refreshState.value = RefreshState.Refreshing
        refreshJob = viewModelScope.launch {
            repository.refresh(force).fold(
                onSuccess = { refreshState.value = RefreshState.Idle },
                onFailure = { refreshState.value = RefreshState.Failed(it.toErrorKind(), now()) },
            )
        }
    }

    fun dismissRejectedLessons() {
        viewModelScope.launch { repository.acknowledgeRejectedLessons() }
    }

    fun logout(onDone: () -> Unit) {
        Log.d(TAG, "logout requested")
        refreshJob?.cancel()
        viewModelScope.launch {
            logoutUseCase()
            onDone()
        }
    }
}
