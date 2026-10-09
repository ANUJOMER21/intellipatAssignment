package com.example.intellipatassignment.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.model.Lesson

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val instructor: String,
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("courseId")],
)
data class LessonEntity(
    @PrimaryKey val id: Int,
    val courseId: Int,
    val title: String,
    val position: Int,
    val isCompleted: Boolean,

    val isSyncPending: Boolean,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 0,
    val lastSyncedAt: Long,
)

@Entity(tableName = "sync_rejections")
data class RejectionEntity(
    @PrimaryKey val lessonId: Int,
    val title: String,
)

data class CourseWithLessons(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val lessons: List<LessonEntity>,
)

fun CourseWithLessons.toDomain() = Course(
    id = course.id,
    title = course.title,
    instructor = course.instructor,
    lessons = lessons.sortedBy { it.position }.map {
        Lesson(it.id, it.title, it.isCompleted, it.isSyncPending)
    },
)
