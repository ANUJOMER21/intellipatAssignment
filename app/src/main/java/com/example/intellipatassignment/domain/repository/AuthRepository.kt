package com.example.intellipatassignment.domain.repository

interface AuthRepository {
    val isLoggedIn: Boolean
    suspend fun login(email: String, password: String): Result<Unit>
    fun logout()
}
