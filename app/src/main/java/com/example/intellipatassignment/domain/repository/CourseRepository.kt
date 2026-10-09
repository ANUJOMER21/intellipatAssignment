package com.example.intellipatassignment.domain.repository

import com.example.intellipatassignment.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>
    fun observeCourse(courseId: Int): Flow<Course?>

    fun observeLastSyncedAt(): Flow<Long?>

    fun observeRejectedLessons(): Flow<List<String>>

    suspend fun refresh(force: Boolean): Result<Unit>

    suspend fun markLessonCompleted(lessonId: Int)

    suspend fun sync(): Result<Unit>

    suspend fun acknowledgeRejectedLessons()

    suspend fun clearLocalData()
}
