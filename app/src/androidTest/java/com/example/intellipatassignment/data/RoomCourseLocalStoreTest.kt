package com.example.intellipatassignment.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellipatassignment.data.local.AppDatabase
import com.example.intellipatassignment.data.local.CourseEntity
import com.example.intellipatassignment.data.local.LessonEntity
import com.example.intellipatassignment.data.local.LocalSnapshot
import com.example.intellipatassignment.data.local.RoomCourseLocalStore
import com.example.intellipatassignment.data.remote.LessonDto
import com.example.intellipatassignment.data.repository.LessonMerger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCourseLocalStoreTest {
    private lateinit var db: AppDatabase
    private lateinit var store: RoomCourseLocalStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomCourseLocalStore(db)
    }

    @After
    fun tearDown() = db.close()

    private fun course(id: Int) = CourseEntity(id, "Course $id", "Instructor")

    private fun lesson(id: Int, courseId: Int, done: Boolean = false, pending: Boolean = false) =
        LessonEntity(id, courseId, "Lesson $id", position = id, isCompleted = done, isSyncPending = pending)

    private fun seed(courses: List<CourseEntity>, lessons: List<LessonEntity>) = runBlocking {
        store.replaceWith(syncedAt = 1) { LocalSnapshot(courses, lessons) }
    }

    private fun refresh(courseId: Int, remote: List<LessonDto>, syncedAt: Long = 2) = runBlocking {
        store.replaceWith(syncedAt) { local ->
            LocalSnapshot(listOf(course(courseId)), LessonMerger.merge(courseId, remote, local))
        }
    }

    private fun lessons() = runBlocking { db.courseDao().getAllLessons().associateBy { it.id } }

    @Test
    fun offlineCompletionSurvivesARefreshThatStillReportsItPending() = runBlocking {
        seed(listOf(course(1)), listOf(lesson(101, 1)))
        store.markCompleted(101)

        refresh(1, listOf(LessonDto(101, "Lesson 101", completed = false)))

        val merged = lessons().getValue(101)
        assertTrue(merged.isCompleted)
        assertTrue(merged.isSyncPending)
    }

    @Test
    fun pendingFlagClearsOnceTheServerConfirms() = runBlocking {
        seed(listOf(course(1)), listOf(lesson(101, 1, done = true, pending = true)))

        refresh(1, listOf(LessonDto(101, "Lesson 101", completed = true)))

        val merged = lessons().getValue(101)
        assertTrue(merged.isCompleted)
        assertEquals(false, merged.isSyncPending)
    }

    @Test
    fun coursesAndLessonsTheServerDroppedAreDeleted() = runBlocking {
        seed(
            listOf(course(1), course(2)),
            listOf(lesson(101, 1), lesson(102, 1), lesson(201, 2)),
        )

        refresh(1, listOf(LessonDto(101, "Lesson 101", completed = false)))

        assertEquals(listOf(1), db.courseDao().getCourseIds())
        assertEquals(setOf(101), lessons().keys)
    }

    @Test
    fun deletingMoreRowsThanSqliteAllowsAsBoundVariablesStillWorks() = runBlocking {
        val many = (1..STALE_ROWS).map { course(it) }
        seed(many, emptyList())

        store.replaceWith(syncedAt = 2) { LocalSnapshot(listOf(course(1)), emptyList()) }

        assertEquals(listOf(1), db.courseDao().getCourseIds())
    }

    @Test
    fun rejectedCompletionIsRevertedAndRememberedUntilAcknowledged() = runBlocking {
        val completed = lesson(101, 1, done = true, pending = true)
        seed(listOf(course(1)), listOf(completed))

        store.rejectCompletion(completed)

        val reverted = lessons().getValue(101)
        assertEquals(false, reverted.isCompleted)
        assertEquals(false, reverted.isSyncPending)
        assertEquals(listOf("Lesson 101"), store.observeRejectedTitles().first())

        store.acknowledgeRejections()
        assertTrue(store.observeRejectedTitles().first().isEmpty())
    }

    @Test
    fun clearAllWipesCoursesSyncStateAndNotices() = runBlocking {
        val completed = lesson(101, 1, done = true, pending = true)
        seed(listOf(course(1)), listOf(completed))
        store.rejectCompletion(completed)

        store.clearAll()

        assertTrue(db.courseDao().getCourseIds().isEmpty())
        assertNull(store.getLastSyncedAt())
        assertTrue(store.observeRejectedTitles().first().isEmpty())
    }

    private companion object {
        const val STALE_ROWS = 33_000
    }
}
