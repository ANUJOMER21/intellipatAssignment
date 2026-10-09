package com.example.intellipatassignment.domain.model

import com.example.intellipatassignment.domain.ProgressCalculator

data class Lesson(
    val id: Int,
    val title: String,
    val isCompleted: Boolean,

    val isSyncPending: Boolean,
)

data class Course(
    val id: Int,
    val title: String,
    val instructor: String,
    val lessons: List<Lesson>,
) {
    val lessonCount: Int get() = lessons.size
    val completedLessonCount: Int get() = lessons.count { it.isCompleted }

    val progress: Int get() = ProgressCalculator.percent(completedLessonCount, lessonCount)
}
