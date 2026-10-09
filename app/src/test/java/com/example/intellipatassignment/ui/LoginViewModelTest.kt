package com.example.intellipatassignment.ui

import app.cash.turbine.test
import com.example.intellipatassignment.core.ErrorKind
import com.example.intellipatassignment.domain.EmailError
import com.example.intellipatassignment.domain.PasswordError
import com.example.intellipatassignment.domain.error.InvalidCredentialsException
import com.example.intellipatassignment.domain.error.NoConnectivityException
import com.example.intellipatassignment.testutil.FakeAuth
import com.example.intellipatassignment.ui.login.LoginError
import com.example.intellipatassignment.ui.login.LoginEvent
import com.example.intellipatassignment.ui.login.LoginViewModel
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
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun LoginViewModel.fillValid() {
        onEmailChange("user@intellipat.com")
        onPasswordChange("Password1")
    }

    @Test
    fun `invalid input shows field errors and never calls the backend`() = runTest(dispatcher) {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth)

        vm.onLoginClick()
        runCurrent()

        assertEquals(EmailError.Empty, vm.state.value.emailError)
        assertEquals(PasswordError.Empty, vm.state.value.passwordError)
        assertFalse(vm.state.value.isLoading)
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun `typing clears the matching error`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuth())
        vm.onLoginClick()

        vm.onEmailChange("u")

        assertNull(vm.state.value.emailError)
        assertEquals(PasswordError.Empty, vm.state.value.passwordError)
    }

    @Test
    fun `valid credentials show loading, then emit LoggedIn`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuth())
        vm.fillValid()

        vm.events.test {
            vm.onLoginClick()
            assertTrue(vm.state.value.isLoading)

            runCurrent()

            assertEquals(LoginEvent.LoggedIn, awaitItem())
            assertFalse(vm.state.value.isLoading)
        }
    }

    @Test
    fun `wrong credentials surface a specific error and stop loading`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuth(loginResult = { Result.failure(InvalidCredentialsException()) }))
        vm.fillValid()

        vm.onLoginClick()
        runCurrent()

        assertEquals(LoginError.InvalidCredentials, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `other failures are classified, not shown as wrong credentials`() = runTest(dispatcher) {
        val vm = LoginViewModel(FakeAuth(loginResult = { Result.failure(NoConnectivityException()) }))
        vm.fillValid()

        vm.onLoginClick()
        runCurrent()

        assertEquals(LoginError.Failure(ErrorKind.Offline), vm.state.value.error)
    }

    @Test
    fun `a second tap while loading is ignored`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        val auth = FakeAuth(loginResult = { gate.await() })
        val vm = LoginViewModel(auth)
        vm.fillValid()

        vm.onLoginClick()
        runCurrent()
        vm.onLoginClick()
        runCurrent()

        assertEquals(1, auth.loginCalls)
        gate.complete(Result.success(Unit))
    }
}
