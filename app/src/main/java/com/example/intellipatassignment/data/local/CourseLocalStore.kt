package com.example.intellipatassignment.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import android.util.Log

class LocalSnapshot(
    val courses: List<CourseEntity>,
    val lessons: List<LessonEntity>,
)

interface CourseLocalStore {
    fun observeCourses(): Flow<List<CourseWithLessons>>
    fun observeCourse(courseId: Int): Flow<CourseWithLessons?>
    fun observeLastSyncedAt(): Flow<Long?>
    fun observeRejectedTitles(): Flow<List<String>>

    suspend fun getLastSyncedAt(): Long?
    suspend fun getPendingLessons(): List<LessonEntity>
    suspend fun markCompleted(lessonId: Int)
    suspend fun clearPending(lessonId: Int)

    suspend fun rejectCompletion(lesson: LessonEntity)
    suspend fun acknowledgeRejections()

    suspend fun replaceWith(syncedAt: Long, build: (Map<Int, LessonEntity>) -> LocalSnapshot)

    suspend fun clearAll()
}

class RoomCourseLocalStore(private val db: AppDatabase) : CourseLocalStore {
    private val dao = db.courseDao()

    override fun observeCourses() = dao.observeCourses()
    override fun observeCourse(courseId: Int) = dao.observeCourse(courseId)
    override fun observeLastSyncedAt() = dao.observeLastSyncedAt()
    override fun observeRejectedTitles() = dao.observeRejectedTitles()
    override suspend fun getLastSyncedAt() = dao.getLastSyncedAt()
    override suspend fun getPendingLessons() = dao.getPendingLessons()
    override suspend fun markCompleted(lessonId: Int) = dao.markCompleted(lessonId)
    override suspend fun clearPending(lessonId: Int) = dao.clearPending(lessonId)

    override suspend fun rejectCompletion(lesson: LessonEntity) {
        db.withTransaction {
            dao.revertCompletion(lesson.id)
            Log.d(TAG, "lesson ${lesson.id} reverted and recorded as rejected")
            dao.upsertRejection(RejectionEntity(lesson.id, lesson.title))
        }
    }

    override suspend fun acknowledgeRejections() = dao.clearRejections()

    override suspend fun replaceWith(
        syncedAt: Long,
        build: (Map<Int, LessonEntity>) -> LocalSnapshot,
    ) {
        db.withTransaction {
            val snapshot = build(dao.getAllLessons().associateBy { it.id })

            dao.upsertCourses(snapshot.courses)
            dao.upsertLessons(snapshot.lessons)

            val staleCourses = dao.getCourseIds() - snapshot.courses.map { it.id }.toSet()
            staleCourses.chunked(DELETE_CHUNK_SIZE).forEach { dao.deleteCoursesByIds(it) }
            val staleLessons = dao.getLessonIds() - snapshot.lessons.map { it.id }.toSet()
            staleLessons.chunked(DELETE_CHUNK_SIZE).forEach { dao.deleteLessonsByIds(it) }

            dao.upsertSyncState(SyncStateEntity(lastSyncedAt = syncedAt))
            Log.d(
                TAG,
                "cache replaced: ${snapshot.courses.size} courses, ${snapshot.lessons.size} lessons " +
                    "(removed ${staleCourses.size} courses, ${staleLessons.size} lessons)",
            )
        }
    }

    override suspend fun clearAll() {
        db.withTransaction {
            dao.clearCourses()
            dao.clearSyncState()
            dao.clearRejections()
        }
    }

    private companion object {
        const val TAG = "CourseStore"
        const val DELETE_CHUNK_SIZE = 500
    }
}
