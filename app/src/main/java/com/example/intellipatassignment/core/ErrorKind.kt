package com.example.intellipatassignment.core

import com.example.intellipatassignment.domain.error.ApiException
import com.example.intellipatassignment.domain.error.MalformedResponseException
import com.example.intellipatassignment.domain.error.NoConnectivityException
import java.io.IOException

enum class ErrorKind { Offline, Network, Unauthorized, Forbidden, NotFound, Server, BadResponse, Unknown }

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is NoConnectivityException -> ErrorKind.Offline
    is ApiException -> when (code) {
        401 -> ErrorKind.Unauthorized
        403 -> ErrorKind.Forbidden
        404 -> ErrorKind.NotFound
        in 500..599 -> ErrorKind.Server
        else -> ErrorKind.Unknown
    }
    is IOException -> ErrorKind.Network
    is MalformedResponseException -> ErrorKind.BadResponse
    else -> ErrorKind.Unknown
}
