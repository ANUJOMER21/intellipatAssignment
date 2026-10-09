package com.example.intellipatassignment.data.repository

import com.example.intellipatassignment.data.local.LessonEntity
import com.example.intellipatassignment.data.remote.LessonDto

object LessonMerger {
    fun merge(
        courseId: Int,
        remote: List<LessonDto>,
        localById: Map<Int, LessonEntity>,
    ): List<LessonEntity> = remote.mapIndexed { index, dto ->
        val locallyPending = localById[dto.id]?.isSyncPending == true
        LessonEntity(
            id = dto.id,
            courseId = courseId,
            title = dto.title,
            position = index,
            isCompleted = dto.completed || locallyPending,
            isSyncPending = locallyPending && !dto.completed,
        )
    }
}
