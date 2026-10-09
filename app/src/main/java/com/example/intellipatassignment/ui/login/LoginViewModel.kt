package com.example.intellipatassignment.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intellipatassignment.core.ErrorKind
import com.example.intellipatassignment.core.toErrorKind
import com.example.intellipatassignment.domain.CredentialsValidator
import com.example.intellipatassignment.domain.EmailError
import com.example.intellipatassignment.domain.PasswordError
import com.example.intellipatassignment.domain.error.InvalidCredentialsException
import com.example.intellipatassignment.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data class Failure(val kind: ErrorKind) : LoginError
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailError? = null,
    val passwordError: PasswordError? = null,
    val isLoading: Boolean = false,
    val error: LoginError? = null,
)

sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

class LoginViewModel(
    private val auth: AuthRepository,
) : ViewModel() {
    private companion object {
        const val TAG = "LoginViewModel"
    }

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun onLoginClick() {
        val current = _state.value
        if (current.isLoading) return

        val emailError = CredentialsValidator.validateEmail(current.email)
        val passwordError = CredentialsValidator.validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            Log.d(TAG, "login blocked by validation (email = $emailError, password = $passwordError)")
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        Log.d(TAG, "login submitted")
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            auth.login(current.email, current.password).fold(
                onSuccess = {
                    Log.d(TAG, "login succeeded")
                    _state.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.LoggedIn)
                },
                onFailure = { t ->
                    Log.d(TAG, "login failed: ${t::class.java.simpleName}")
                    val error = if (t is InvalidCredentialsException) LoginError.InvalidCredentials
                    else LoginError.Failure(t.toErrorKind())
                    _state.update { it.copy(isLoading = false, error = error) }
                },
            )
        }
    }
}
