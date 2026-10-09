package com.example.intellipatassignment.data

import com.example.intellipatassignment.data.local.LessonEntity
import com.example.intellipatassignment.data.remote.LessonDto
import com.example.intellipatassignment.data.repository.LessonMerger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMergerTest {
    private fun local(id: Int, completed: Boolean, pending: Boolean) =
        LessonEntity(id, courseId = 1, title = "L$id", position = 0, isCompleted = completed, isSyncPending = pending)

    @Test
    fun `offline completion survives a refresh that still reports it pending`() {
        val remote = listOf(LessonDto(1, "L1", completed = false), LessonDto(2, "L2", completed = false))
        val localById = mapOf(1 to local(1, completed = true, pending = true))

        val merged = LessonMerger.merge(1, remote, localById)

        assertTrue(merged[0].isCompleted)
        assertTrue(merged[0].isSyncPending)
        assertFalse(merged[1].isCompleted)
    }

    @Test
    fun `pending flag clears once server confirms completion`() {
        val remote = listOf(LessonDto(1, "L1", completed = true))
        val localById = mapOf(1 to local(1, completed = true, pending = true))

        val merged = LessonMerger.merge(1, remote, localById).single()

        assertTrue(merged.isCompleted)
        assertFalse(merged.isSyncPending)
    }

    @Test
    fun `server order defines position and new lessons are added`() {
        val remote = listOf(LessonDto(5, "B", false), LessonDto(3, "A", true))

        val merged = LessonMerger.merge(1, remote, emptyMap())

        assertEquals(listOf(5, 3), merged.map { it.id })
        assertEquals(listOf(0, 1), merged.map { it.position })
        assertTrue(merged[1].isCompleted)
    }
}
