package com.example.intellipatassignment.domain

import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {
    @Test fun `empty course is zero percent, not a divide by zero`() =
        assertEquals(0, ProgressCalculator.percent(0, 0))

    @Test fun `rounds to nearest percent`() {
        assertEquals(33, ProgressCalculator.percent(1, 3))
        assertEquals(67, ProgressCalculator.percent(2, 3))
        assertEquals(65, ProgressCalculator.percent(13, 20))
    }

    @Test fun `almost done is never shown as done, barely started never as untouched`() {
        assertEquals(99, ProgressCalculator.percent(199, 200))
        assertEquals(1, ProgressCalculator.percent(1, 1000))
        assertEquals(100, ProgressCalculator.percent(200, 200))
        assertEquals(0, ProgressCalculator.percent(0, 200))
    }

    @Test fun `out of range input is clamped`() {
        assertEquals(100, ProgressCalculator.percent(9, 4))
        assertEquals(0, ProgressCalculator.percent(-1, 4))
    }

    @Test fun `course progress follows completed lessons`() {
        fun lesson(id: Int, done: Boolean) = Lesson(id, "L$id", done, isSyncPending = false)
        val course = Course(1, "C", "I", listOf(lesson(1, true), lesson(2, true), lesson(3, false), lesson(4, false)))
        assertEquals(50, course.progress)
        assertEquals(100, course.copy(lessons = course.lessons.map { it.copy(isCompleted = true) }).progress)
    }
}
