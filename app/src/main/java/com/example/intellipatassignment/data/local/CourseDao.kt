package com.example.intellipatassignment.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY id")
    fun observeCourses(): Flow<List<CourseWithLessons>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId")
    fun observeCourse(courseId: Int): Flow<CourseWithLessons?>

    @Query("SELECT lastSyncedAt FROM sync_state WHERE id = 0")
    fun observeLastSyncedAt(): Flow<Long?>

    @Query("SELECT lastSyncedAt FROM sync_state WHERE id = 0")
    suspend fun getLastSyncedAt(): Long?

    @Query("SELECT title FROM sync_rejections ORDER BY lessonId")
    fun observeRejectedTitles(): Flow<List<String>>

    @Query("SELECT * FROM lessons")
    suspend fun getAllLessons(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE isSyncPending = 1")
    suspend fun getPendingLessons(): List<LessonEntity>

    @Query("SELECT id FROM courses")
    suspend fun getCourseIds(): List<Int>

    @Query("SELECT id FROM lessons")
    suspend fun getLessonIds(): List<Int>

    @Upsert
    suspend fun upsertCourses(courses: List<CourseEntity>)

    @Upsert
    suspend fun upsertLessons(lessons: List<LessonEntity>)

    @Upsert
    suspend fun upsertSyncState(state: SyncStateEntity)

    @Upsert
    suspend fun upsertRejection(rejection: RejectionEntity)

    @Query("DELETE FROM courses WHERE id IN (:ids)")
    suspend fun deleteCoursesByIds(ids: List<Int>)

    @Query("DELETE FROM lessons WHERE id IN (:ids)")
    suspend fun deleteLessonsByIds(ids: List<Int>)

    @Query("UPDATE lessons SET isCompleted = 1, isSyncPending = 1 WHERE id = :lessonId AND isCompleted = 0")
    suspend fun markCompleted(lessonId: Int)

    @Query("UPDATE lessons SET isSyncPending = 0 WHERE id = :lessonId")
    suspend fun clearPending(lessonId: Int)

    @Query("UPDATE lessons SET isCompleted = 0, isSyncPending = 0 WHERE id = :lessonId")
    suspend fun revertCompletion(lessonId: Int)

    @Query("DELETE FROM sync_rejections")
    suspend fun clearRejections()

    @Query("DELETE FROM sync_state")
    suspend fun clearSyncState()

    @Query("DELETE FROM courses")
    suspend fun clearCourses()
}
