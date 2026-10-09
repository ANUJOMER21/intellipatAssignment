package com.example.intellipatassignment.testutil

import com.example.intellipatassignment.core.ConnectivityObserver
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.repository.AuthRepository
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCourseRepository(
    cached: List<Course> = emptyList(),
    var refreshResult: suspend () -> Result<Unit> = { Result.success(Unit) },
) : CourseRepository {
    val cache = MutableStateFlow(cached)
    val lastSynced = MutableStateFlow<Long?>(null)
    val rejected = MutableStateFlow<List<String>>(emptyList())

    val refreshCalls = mutableListOf<Boolean>()
    val completed = mutableListOf<Int>()
    var markFailure: Exception? = null
    var cleared = false

    override fun observeCourses(): Flow<List<Course>> = cache
    override fun observeCourse(courseId: Int): Flow<Course?> = cache.map { list -> list.firstOrNull { it.id == courseId } }
    override fun observeLastSyncedAt(): Flow<Long?> = lastSynced
    override fun observeRejectedLessons(): Flow<List<String>> = rejected

    override suspend fun refresh(force: Boolean): Result<Unit> {
        refreshCalls += force
        return refreshResult()
    }

    override suspend fun markLessonCompleted(lessonId: Int) {
        markFailure?.let { throw it }
        completed += lessonId
    }

    override suspend fun sync() = refreshResult()
    override suspend fun acknowledgeRejectedLessons() { rejected.value = emptyList() }
    override suspend fun clearLocalData() {
        cleared = true
        cache.value = emptyList()
    }
}

class FakeScheduler : SyncScheduler {
    var syncWhenOnlineCalls = 0
    var periodicCalls = 0
    var cancelled = false
    override fun syncWhenOnline() { syncWhenOnlineCalls++ }
    override fun schedulePeriodicSync() { periodicCalls++ }
    override fun cancelAll() { cancelled = true }
}

class FakeAuth(var loginResult: suspend () -> Result<Unit> = { Result.success(Unit) }) : AuthRepository {
    var loginCalls = 0
    var loggedOut = false
    override val isLoggedIn get() = !loggedOut
    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls++
        return loginResult()
    }
    override fun logout() { loggedOut = true }
}

class FakeConnectivity(online: Boolean = true) : ConnectivityObserver {
    val state = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = state
    override fun isCurrentlyOnline() = state.value
}
