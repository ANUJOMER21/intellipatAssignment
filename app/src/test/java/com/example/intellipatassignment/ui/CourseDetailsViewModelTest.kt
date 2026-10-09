package com.example.intellipatassignment.ui

import app.cash.turbine.test
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.model.Lesson
import com.example.intellipatassignment.testutil.FakeCourseRepository
import com.example.intellipatassignment.ui.details.CourseDetailsEvent
import com.example.intellipatassignment.ui.details.CourseDetailsUiState
import com.example.intellipatassignment.ui.details.CourseDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val course = Course(7, "Python", "John", listOf(Lesson(1, "Intro", false, false)))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the requested course once it is loaded`() = runTest(dispatcher) {
        val vm = CourseDetailsViewModel(7, FakeCourseRepository(cached = listOf(course)))

        vm.uiState.test {
            assertEquals(CourseDetailsUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(CourseDetailsUiState.Success(course), awaitItem())
        }
    }

    @Test
    fun `an unknown course id shows not-found instead of loading forever`() = runTest(dispatcher) {
        val vm = CourseDetailsViewModel(99, FakeCourseRepository(cached = listOf(course)))

        vm.uiState.test {
            assertEquals(CourseDetailsUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(CourseDetailsUiState.NotFound, awaitItem())
        }
    }

    @Test
    fun `marking a lesson completed is forwarded to the repository`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(cached = listOf(course))
        val vm = CourseDetailsViewModel(7, repo)

        vm.onMarkCompleted(1)
        runCurrent()

        assertEquals(listOf(1), repo.completed)
    }

    @Test
    fun `a failure while marking completed becomes a one-shot event, not a crash`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(cached = listOf(course)).apply { markFailure = IOException("disk full") }
        val vm = CourseDetailsViewModel(7, repo)

        vm.events.test {
            vm.onMarkCompleted(1)
            runCurrent()
            assertEquals(CourseDetailsEvent.MarkCompletedFailed, awaitItem())
        }
    }
}
