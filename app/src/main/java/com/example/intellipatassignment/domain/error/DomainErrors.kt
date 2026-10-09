package com.example.intellipatassignment.domain.error

import java.io.IOException

class NoConnectivityException : IOException("No internet connection")

class ApiException(val code: Int) : Exception("HTTP $code")

class MalformedResponseException(cause: Throwable) : Exception("Unexpected response", cause)

class InvalidCredentialsException : Exception("Invalid email or password")
