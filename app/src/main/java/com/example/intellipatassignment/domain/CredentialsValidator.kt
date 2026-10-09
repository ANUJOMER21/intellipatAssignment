package com.example.intellipatassignment.domain

enum class EmailError { Empty, Invalid }
enum class PasswordError { Empty, TooShort }

object CredentialsValidator {
    const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): EmailError? = when {
        email.isBlank() -> EmailError.Empty
        !EMAIL_REGEX.matches(email.trim()) -> EmailError.Invalid
        else -> null
    }

    fun validatePassword(password: String): PasswordError? = when {
        password.isEmpty() -> PasswordError.Empty
        password.length < MIN_PASSWORD_LENGTH -> PasswordError.TooShort
        else -> null
    }
}
