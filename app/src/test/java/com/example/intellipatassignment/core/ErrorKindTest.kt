package com.example.intellipatassignment.core

import com.example.intellipatassignment.domain.error.ApiException
import com.example.intellipatassignment.domain.error.MalformedResponseException
import com.example.intellipatassignment.domain.error.NoConnectivityException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorKindTest {
    @Test fun `http status codes map to distinct kinds`() {
        assertEquals(ErrorKind.Unauthorized, ApiException(401).toErrorKind())
        assertEquals(ErrorKind.Forbidden, ApiException(403).toErrorKind())
        assertEquals(ErrorKind.NotFound, ApiException(404).toErrorKind())
        assertEquals(ErrorKind.Server, ApiException(500).toErrorKind())
        assertEquals(ErrorKind.Server, ApiException(503).toErrorKind())
        assertEquals(ErrorKind.Unknown, ApiException(418).toErrorKind())
    }

    @Test fun `offline, io and parse failures are told apart`() {
        assertEquals(ErrorKind.Offline, NoConnectivityException().toErrorKind())
        assertEquals(ErrorKind.Network, IOException("reset").toErrorKind())
        assertEquals(ErrorKind.BadResponse, MalformedResponseException(IllegalStateException()).toErrorKind())
        assertEquals(ErrorKind.Unknown, IllegalStateException().toErrorKind())
    }
}
