package com.example.intellipatassignment.data

import com.example.intellipatassignment.data.local.CourseLocalStore
import com.example.intellipatassignment.data.local.CourseWithLessons
import com.example.intellipatassignment.data.local.LessonEntity
import com.example.intellipatassignment.data.local.LocalSnapshot
import com.example.intellipatassignment.data.remote.CourseApi
import com.example.intellipatassignment.data.remote.CourseDto
import com.example.intellipatassignment.data.repository.CourseRepositoryImpl
import com.example.intellipatassignment.domain.SyncPolicy
import com.example.intellipatassignment.domain.error.ApiException
import com.example.intellipatassignment.domain.error.MalformedResponseException
import com.example.intellipatassignment.domain.error.NoConnectivityException
import com.example.intellipatassignment.testutil.FakeScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRepositoryImplTest {
    private var clock = 1_000_000L
    private val scheduler = FakeScheduler()

    private fun lesson(id: Int) = LessonEntity(id, 1, "L$id", id, isCompleted = true, isSyncPending = true)

    private fun repo(api: FakeApi, store: FakeStore) = CourseRepositoryImpl(api, store, scheduler, now = { clock })

    @Test
    fun `a failing lesson push does not block the others, and sync still reports failure for retry`() = runTest {
        val store = FakeStore(pending = mutableListOf(lesson(1), lesson(2), lesson(3)))
        val api = FakeApi(onComplete = { id -> if (id == 1) throw IOException("flaky") })

        val result = repo(api, store).sync()

        assertEquals(listOf(1, 2, 3), api.pushed)
        assertEquals(listOf(1), store.pending.map { it.id })
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `a permanently rejected lesson is reverted and recorded instead of wedging the queue`() = runTest {
        val store = FakeStore(pending = mutableListOf(lesson(1), lesson(2)))
        val api = FakeApi(onComplete = { id -> if (id == 1) throw httpError(422) })

        val result = repo(api, store).sync()

        assertTrue(result.isSuccess)
        assertTrue(store.pending.isEmpty())
        assertEquals(listOf("L1"), store.rejected.map { it.title })
    }

    @Test
    fun `server errors and auth errors stay queued for retry`() = runTest {
        val store = FakeStore(pending = mutableListOf(lesson(1), lesson(2)))
        val api = FakeApi(onComplete = { id -> throw httpError(if (id == 1) 503 else 401) })

        assertTrue(repo(api, store).sync().isFailure)
        assertEquals(listOf(1, 2), store.pending.map { it.id })
        assertTrue(store.rejected.isEmpty())
    }

    @Test
    fun `non-forced refresh is skipped while the cache is fresh`() = runTest {
        val store = FakeStore(lastSyncedAt = clock - 1_000)
        val api = FakeApi()

        assertTrue(repo(api, store).refresh(force = false).isSuccess)
        assertEquals(0, api.courseCalls)
    }

    @Test
    fun `non-forced refresh hits the network once the cache is stale or empty`() = runTest {
        val api = FakeApi()
        repo(api, FakeStore(lastSyncedAt = clock - SyncPolicy.STALE_AFTER_MS - 1)).refresh(force = false)
        repo(api, FakeStore(lastSyncedAt = null)).refresh(force = false)
        assertEquals(2, api.courseCalls)
    }

    @Test
    fun `forced refresh ignores freshness and records the sync time`() = runTest {
        val store = FakeStore(lastSyncedAt = clock)
        val api = FakeApi()

        repo(api, store).refresh(force = true)

        assertEquals(1, api.courseCalls)
        assertEquals(clock, store.lastSyncedAt)
    }

    @Test
    fun `a failed refresh queues exactly one background retry`() = runTest {
        val api = FakeApi(courses = { throw NoConnectivityException() })

        val result = repo(api, FakeStore()).refresh(force = true)

        assertTrue(result.exceptionOrNull() is NoConnectivityException)
        assertEquals(1, scheduler.syncWhenOnlineCalls)
    }

    @Test
    fun `http and parser failures are translated to domain errors`() = runTest {
        val http = repo(FakeApi(courses = { throw httpError(500) }), FakeStore()).refresh(force = true)
        val parse = repo(FakeApi(courses = { throw SerializationException("bad") }), FakeStore()).refresh(force = true)

        assertEquals(500, (http.exceptionOrNull() as ApiException).code)
        assertTrue(parse.exceptionOrNull() is MalformedResponseException)
    }
}

private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

private class FakeApi(
    private val courses: suspend () -> List<CourseDto> = { emptyList() },
    private val onComplete: suspend (Int) -> Unit = {},
) : CourseApi {
    val pushed = mutableListOf<Int>()
    var courseCalls = 0

    override suspend fun getCourses(): List<CourseDto> {
        courseCalls++
        return courses()
    }

    override suspend fun completeLesson(courseId: Int, lessonId: Int) {
        pushed += lessonId
        onComplete(lessonId)
    }
}

private class FakeStore(
    val pending: MutableList<LessonEntity> = mutableListOf(),
    var lastSyncedAt: Long? = null,
) : CourseLocalStore {
    val writtenCourses = mutableListOf<Int>()
    val rejected = mutableListOf<LessonEntity>()

    override fun observeCourses(): Flow<List<CourseWithLessons>> = emptyFlow()
    override fun observeCourse(courseId: Int): Flow<CourseWithLessons?> = emptyFlow()
    override fun observeLastSyncedAt(): Flow<Long?> = emptyFlow()
    override fun observeRejectedTitles(): Flow<List<String>> = emptyFlow()

    override suspend fun getLastSyncedAt() = lastSyncedAt
    override suspend fun getPendingLessons() = pending.toList()
    override suspend fun markCompleted(lessonId: Int) = Unit
    override suspend fun clearPending(lessonId: Int) { pending.removeAll { it.id == lessonId } }

    override suspend fun rejectCompletion(lesson: LessonEntity) {
        pending.removeAll { it.id == lesson.id }
        rejected += lesson
    }

    override suspend fun acknowledgeRejections() { rejected.clear() }

    override suspend fun replaceWith(syncedAt: Long, build: (Map<Int, LessonEntity>) -> LocalSnapshot) {
        writtenCourses += build(emptyMap()).courses.map { it.id }
        lastSyncedAt = syncedAt
    }

    override suspend fun clearAll() {
        pending.clear()
        writtenCourses.clear()
        lastSyncedAt = null
    }
}
