package com.example.intellipatassignment.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: Int,
    val title: String,
    val instructor: String,
    val lessonItems: List<LessonDto> = emptyList(),
)

@Serializable
data class LessonDto(
    val id: Int,
    val title: String,
    val completed: Boolean,
)
