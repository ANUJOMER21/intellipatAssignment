package com.example.intellipatassignment.data.repository

import com.example.intellipatassignment.data.local.CourseEntity
import com.example.intellipatassignment.data.local.CourseLocalStore
import com.example.intellipatassignment.data.local.LessonEntity
import com.example.intellipatassignment.data.local.LocalSnapshot
import com.example.intellipatassignment.data.local.toDomain
import com.example.intellipatassignment.data.remote.CourseApi
import com.example.intellipatassignment.data.remote.CourseDto
import com.example.intellipatassignment.domain.SyncPolicy
import com.example.intellipatassignment.domain.error.ApiException
import com.example.intellipatassignment.domain.error.MalformedResponseException
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.repository.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class CourseRepositoryImpl(
    private val api: CourseApi,
    private val store: CourseLocalStore,
    private val scheduler: SyncScheduler,
    private val now: () -> Long = System::currentTimeMillis,
) : CourseRepository {
    override fun observeCourses(): Flow<List<Course>> =
        store.observeCourses().map { list -> list.map { it.toDomain() } }

    override fun observeCourse(courseId: Int): Flow<Course?> =
        store.observeCourse(courseId).map { it?.toDomain() }

    override fun observeLastSyncedAt(): Flow<Long?> = store.observeLastSyncedAt()

    override fun observeRejectedLessons(): Flow<List<String>> = store.observeRejectedTitles()

    override suspend fun refresh(force: Boolean): Result<Unit> {
        scheduler.schedulePeriodicSync()
        if (!force && !SyncPolicy.isStale(store.getLastSyncedAt(), now())) {
            return Result.success(Unit)
        }

        return try {
            refreshFromServer()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            scheduler.syncWhenOnline()
            Result.failure(e)
        }
    }

    override suspend fun markLessonCompleted(lessonId: Int) {
        store.markCompleted(lessonId)
        scheduler.syncWhenOnline()
    }

    override suspend fun sync(): Result<Unit> {
        val pushFailure = pushPending()
        val refreshFailure = try {
            refreshFromServer()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }
        val failure = pushFailure ?: refreshFailure
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }

    override suspend fun acknowledgeRejectedLessons() = store.acknowledgeRejections()

    override suspend fun clearLocalData() = store.clearAll()

    private suspend fun refreshFromServer() {
        val remote = remote { api.getCourses() }
        store.replaceWith(syncedAt = now()) { local -> remote.toSnapshot(local) }
    }

    private suspend fun pushPending(): Throwable? {
        var retryable: Throwable? = null
        for (lesson in store.getPendingLessons()) {
            try {
                remote { api.completeLesson(lesson.courseId, lesson.id) }
                store.clearPending(lesson.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.isPermanentRejection()) {
                    store.rejectCompletion(lesson)
                } else {
                    retryable = retryable ?: e
                }
            } catch (e: IOException) {
                retryable = retryable ?: e
            }
        }
        return retryable
    }
}

private suspend fun <T> remote(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw ApiException(e.code())
} catch (e: SerializationException) {
    throw MalformedResponseException(e)
}

private fun List<CourseDto>.toSnapshot(local: Map<Int, LessonEntity>) = LocalSnapshot(
    courses = map { CourseEntity(it.id, it.title, it.instructor) },
    lessons = flatMap { LessonMerger.merge(it.id, it.lessonItems, local) },
)

private fun ApiException.isPermanentRejection() = code in 400..499 && code !in RETRYABLE_CLIENT_CODES

private val RETRYABLE_CLIENT_CODES = setOf(401, 408, 429)
