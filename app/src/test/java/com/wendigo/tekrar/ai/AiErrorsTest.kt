package com.wendigo.tekrar.ai

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiErrorsTest {

    @Test
    fun mapsUnauthorizedWithoutRetry() {
        val info = AiErrors.fromHttpCode(401)

        assertTrue(info.messageTr.contains("Anahtar geçersiz"))
        assertFalse(info.retryable)
    }

    @Test
    fun mapsRateLimitAndServerErrorsAsRetryable() {
        assertTrue(AiErrors.fromHttpCode(429).retryable)
        assertTrue(AiErrors.fromHttpCode(503).retryable)
        assertTrue(AiErrors.fromHttpCode(429).messageTr.contains("Kota"))
    }

    @Test
    fun mapsBadRequestWithServerDetail() {
        val info = AiErrors.fromHttpCode(400, "unsupported parameter")

        assertTrue(info.messageTr.contains("unsupported parameter"))
        assertFalse(info.retryable)
    }

    @Test
    fun mapsNetworkAndTimeout() {
        assertTrue(AiErrors.fromThrowable(SocketTimeoutException()).messageTr.contains("Yanıt gecikti"))
        assertTrue(AiErrors.fromThrowable(UnknownHostException()).messageTr.contains("İnternet yok"))
    }

    @Test
    fun extractsServerMessageFromJsonBody() {
        assertEquals(
            "model not found",
            AiErrors.extractServerMessage("{\"error\":{\"message\":\"model not found\"}}"),
        )
        assertNull(AiErrors.extractServerMessage("<html>500</html>"))
        assertNull(AiErrors.extractServerMessage(null))
    }
}
