package com.example.intellipatassignment.ui

import app.cash.turbine.test
import com.example.intellipatassignment.core.ErrorKind
import com.example.intellipatassignment.domain.error.NoConnectivityException
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.model.Lesson
import com.example.intellipatassignment.domain.usecase.LogoutUseCase
import com.example.intellipatassignment.testutil.FakeAuth
import com.example.intellipatassignment.testutil.FakeConnectivity
import com.example.intellipatassignment.testutil.FakeCourseRepository
import com.example.intellipatassignment.testutil.FakeScheduler
import com.example.intellipatassignment.ui.courses.CoursesUiState
import com.example.intellipatassignment.ui.courses.CoursesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoursesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = 10_000L
    private val course = Course(1, "Python", "John", listOf(Lesson(1, "Intro", true, false)))
    private val offline = { Result.failure<Unit>(NoConnectivityException()) }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: FakeCourseRepository,
        scheduler: FakeScheduler = FakeScheduler(),
    ) = CoursesViewModel(
        repository = repo,
        logoutUseCase = LogoutUseCase(FakeAuth(), repo, scheduler),
        connectivity = FakeConnectivity(),
        now = { clock },
    )

    @Test
    fun `offline with cached courses keeps showing them and flags the failure`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(cached = listOf(course), refreshResult = { offline() })

        viewModel(repo).uiState.test {
            runCurrent()
            val state = expectMostRecentItem() as CoursesUiState.Success

            assertEquals(listOf(course), state.courses)
            assertEquals(ErrorKind.Offline, state.refreshError)
            assertTrue(state.isStale)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `offline with empty cache shows the error screen, then recovers on retry`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(refreshResult = { offline() })
        val vm = viewModel(repo)

        vm.uiState.test {
            assertEquals(CoursesUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(CoursesUiState.Error(ErrorKind.Offline), awaitItem())

            repo.refreshResult = { Result.success(Unit) }
            repo.cache.value = listOf(course)
            repo.lastSynced.value = clock + 1
            vm.refresh()
            runCurrent()

            val recovered = expectMostRecentItem() as CoursesUiState.Success
            assertEquals(listOf(course), recovered.courses)
            assertNull(recovered.refreshError)
            assertFalse(recovered.isRefreshing)
        }
    }

    @Test
    fun `successful refresh with no courses shows the empty state`() = runTest(dispatcher) {
        val vm = viewModel(FakeCourseRepository())

        vm.uiState.test {
            assertEquals(CoursesUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(CoursesUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `a background sync that finishes after a failure clears the failure notice`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(cached = listOf(course), refreshResult = { offline() })

        viewModel(repo).uiState.test {
            runCurrent()
            assertEquals(ErrorKind.Offline, (expectMostRecentItem() as CoursesUiState.Success).refreshError)

            repo.lastSynced.value = clock + 500
            runCurrent()

            val healed = expectMostRecentItem() as CoursesUiState.Success
            assertNull(healed.refreshError)
            assertFalse(healed.isStale)
        }
    }

    @Test
    fun `opening the screen trusts a fresh cache while pull-to-refresh forces a fetch`() = runTest(dispatcher) {
        val repo = FakeCourseRepository(cached = listOf(course))
        val vm = viewModel(repo)

        runCurrent()
        assertEquals(listOf(false), repo.refreshCalls)

        vm.refresh()
        runCurrent()
        assertEquals(listOf(false, true), repo.refreshCalls)
    }

    @Test
    fun `logout cancels an in-flight refresh and wipes local data`() = runTest(dispatcher) {
        val never = CompletableDeferred<Result<Unit>>()
        val repo = FakeCourseRepository(cached = listOf(course), refreshResult = { never.await() })
        val scheduler = FakeScheduler()
        val vm = viewModel(repo, scheduler)
        runCurrent()

        var done = false
        vm.logout { done = true }
        runCurrent()

        assertTrue(done)
        assertTrue(repo.cleared)
        assertTrue(scheduler.cancelled)
        assertTrue(never.isActive)
    }

    @Test
    fun `rejected lessons are surfaced once and cleared when dismissed`() = runTest(dispatcher) {
        val repo = FakeCourseRepository()
        val vm = viewModel(repo)

        vm.rejectedLessons.test {
            assertEquals(emptyList<String>(), awaitItem())

            repo.rejected.value = listOf("Fine-tuning")
            runCurrent()
            assertEquals(listOf("Fine-tuning"), awaitItem())

            vm.dismissRejectedLessons()
            runCurrent()
            assertEquals(emptyList<String>(), awaitItem())
        }
    }
}
