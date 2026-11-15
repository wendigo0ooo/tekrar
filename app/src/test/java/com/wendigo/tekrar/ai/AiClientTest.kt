package com.wendigo.tekrar.ai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {

    private val config = AiConfig(apiKey = "oc_sk_test_key")

    private fun client(handler: (Request) -> Response): OpenAiCompatibleClient {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain -> handler(chain.request()) }
            .build()
        return OpenAiCompatibleClient(httpClient)
    }

    private fun response(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("text/event-stream".toMediaType()))
            .build()

    private fun Request.bodyText(): String =
        body?.let { Buffer().also(it::writeTo).readUtf8() }.orEmpty()

    @Test
    fun sendsContractHeadersAndStreamsSse() = runBlocking {
        var captured: Request? = null
        val client = client { request ->
            captured = request
            response(
                request,
                code = 200,
                body = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n" +
                    "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]," +
                    "\"usage\":{\"total_tokens\":7}}\n\n" +
                    "data: [DONE]\n\n",
            )
        }

        val events = client.streamChat(
            config,
            listOf(AiChatMessage(AiRole.USER, "hi")),
            "ses_test",
        ).toList()

        assertEquals(2, events.size)
        assertEquals(AiStreamEvent.Delta("Hello"), events[0])
        val done = events[1] as AiStreamEvent.Done
        assertEquals("stop", done.finishReason)
        assertNotNull(done.usageJson)

        val request = captured!!
        assertEquals("Bearer oc_sk_test_key", request.header("Authorization"))
        assertEquals("ses_test", request.header("x-opencode-session"))
        assertEquals("Tekrar/3.1 (Android)", request.header("User-Agent"))
        assertEquals("text/event-stream", request.header("Accept"))
        assertEquals(
            "https://opencode.ai/inference/go/openai/v1/chat/completions",
            request.url.toString(),
        )
        val body = request.bodyText()
        assertTrue(body.contains("\"stream\":true"))
        assertTrue(body.contains("\"role\":\"user\""))
        assertFalse(body.contains("oc_sk_test_key"))
    }

    @Test
    fun prefixesSessionIdWhenMissingPrefix() = runBlocking {
        var captured: Request? = null
        val client = client { request ->
            captured = request
            response(request, code = 200, body = "data: [DONE]\n\n")
        }

        client.streamChat(config, listOf(AiChatMessage(AiRole.USER, "hi")), "abc").toList()

        assertEquals("ses_abc", captured?.header("x-opencode-session"))
    }

    @Test
    fun unauthorizedBecomesTurkishErrorWithoutRetry() = runBlocking {
        var calls = 0
        val client = client { request ->
            calls += 1
            response(request, code = 401, body = "{\"error\":{\"message\":\"bad key\"}}")
        }

        val events = client.streamChat(
            config,
            listOf(AiChatMessage(AiRole.USER, "hi")),
            "ses_x",
        ).toList()

        assertEquals(1, calls)
        val error = events.single() as AiStreamEvent.Error
        assertTrue(error.messageTr.contains("Anahtar geçersiz"))
        assertEquals(401, error.httpCode)
        assertFalse(error.retryable)
    }

    @Test
    fun serverErrorRetriesOnceThenStreams() = runBlocking {
        var calls = 0
        val client = client { request ->
            calls += 1
            if (calls == 1) {
                response(request, code = 503, body = "{}")
            } else {
                response(
                    request,
                    code = 200,
                    body = "data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}\n\n" +
                        "data: [DONE]\n\n",
                )
            }
        }

        val events = client.streamChat(
            config,
            listOf(AiChatMessage(AiRole.USER, "hi")),
            "ses_x",
        ).toList()

        assertEquals(2, calls)
        assertEquals(listOf(AiStreamEvent.Delta("ok"), AiStreamEvent.Done()), events)
    }

    @Test
    fun testConnectionReportsSuccessAndAuthFailure() = runBlocking {
        val okClient = client { request -> response(request, code = 200, body = "{}") }
        assertTrue(okClient.testConnection(config).isSuccess)

        var probeBody = ""
        val badClient = client { request ->
            probeBody = request.bodyText()
            response(request, code = 401, body = "{}")
        }
        val result = badClient.testConnection(config)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AiClientException
        assertTrue(error.message.orEmpty().contains("Anahtar geçersiz"))
        assertEquals(401, error.httpCode)
        assertTrue(probeBody.contains("\"stream\":false"))
        assertTrue(probeBody.contains("\"max_tokens\":8"))
    }
}
