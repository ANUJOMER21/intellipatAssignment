package com.example.intellipatassignment.data.repository

import com.example.intellipatassignment.data.session.SessionStorage
import com.example.intellipatassignment.domain.error.InvalidCredentialsException
import com.example.intellipatassignment.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository(
    private val session: SessionStorage,
) : AuthRepository {
    override val isLoggedIn: Boolean get() = session.getToken() != null

    override suspend fun login(email: String, password: String): Result<Unit> {
        delay(LOGIN_LATENCY_MS)
        return if (email.trim().equals(VALID_EMAIL, ignoreCase = true) && password == VALID_PASSWORD) {
            session.saveToken("mock-token-${System.currentTimeMillis()}")
            Result.success(Unit)
        } else {
            Result.failure(InvalidCredentialsException())
        }
    }

    override fun logout() = session.clear()

    companion object {
        private const val LOGIN_LATENCY_MS = 1_200L
        const val VALID_EMAIL = "user@intellipat.com"
        const val VALID_PASSWORD = "Password1"
    }
}
