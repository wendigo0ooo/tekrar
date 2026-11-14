package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiJsonTest {

    @Test
    fun quotesAndEscapesSpecialCharacters() {
        val raw = "He said \"hi\"\n\tpath\\to \u0001"
        val quoted = AiJson.quote(raw)

        assertEquals("\"He said \\\"hi\\\"\\n\\tpath\\\\to \\u0001\"", quoted)
        assertEquals(raw, MiniJson.parse(quoted))
    }

    @Test
    fun encodesAllControlCharactersAsUnicode() {
        assertEquals("\"\\u0000\\u001f\"", AiJson.quote("\u0000\u001F"))
    }

    @Test
    fun buildChatRequestJsonIsParseableAndHidesApiKey() {
        val config = AiConfig(
            apiKey = "oc_sk_secret_123",
            model = "gpt-6-luna",
            temperature = 0.2,
            maxTokens = 250,
        )
        val messages = listOf(
            AiChatMessage(AiRole.SYSTEM, "sys \"quoted\""),
            AiChatMessage(AiRole.USER, "line1\nline2"),
        )

        val json = AiJson.buildChatRequestJson(config, messages, stream = true)

        assertFalse(json.contains("oc_sk_secret_123"))
        val root = MiniJson.parse(json) as Map<*, *>
        assertEquals("gpt-6-luna", root["model"])
        assertEquals(true, root["stream"])
        assertEquals(0.2, root["temperature"] as Double, 0.0)
        assertEquals(250.0, root["max_tokens"] as Double, 0.0)
        val parsedMessages = root["messages"] as List<*>
        assertEquals(2, parsedMessages.size)
        val system = parsedMessages[0] as Map<*, *>
        assertEquals("system", system["role"])
        assertEquals("sys \"quoted\"", system["content"])
        val user = parsedMessages[1] as Map<*, *>
        assertEquals("user", user["role"])
        assertEquals("line1\nline2", user["content"])
    }

    @Test
    fun buildChatRequestJsonSupportsNonStreaming() {
        val config = AiConfig(apiKey = "oc_sk_test", temperature = 0.0, maxTokens = 8)
        val json = AiJson.buildChatRequestJson(
            config,
            listOf(AiChatMessage(AiRole.USER, "ping")),
            stream = false,
        )
        val root = MiniJson.parse(json) as Map<*, *>

        assertEquals(false, root["stream"])
        assertEquals(0.0, root["temperature"] as Double, 0.0)
        assertEquals(8.0, root["max_tokens"] as Double, 0.0)
    }

    @Test
    fun encodeSupportsNestedValues() {
        val json = AiJson.encode(
            mapOf(
                "usage" to mapOf("tokens" to 5, "ok" to true),
                "items" to listOf("a", null, 2),
            )
        )
        val root = MiniJson.parse(json) as Map<*, *>

        val usage = root["usage"] as Map<*, *>
        assertEquals(5.0, usage["tokens"] as Double, 0.0)
        assertEquals(true, usage["ok"])
        val items = root["items"] as List<*>
        assertEquals(listOf("a", null, 2.0), items)
    }

    @Test
    fun encodeRejectsNonFiniteNumbersAsNull() {
        val json = AiJson.encode(mapOf("a" to Double.NaN, "b" to Double.POSITIVE_INFINITY))
        val root = MiniJson.parse(json) as Map<*, *>

        assertNull(root["a"])
        assertNull(root["b"])
    }

    @Test
    fun quoteIsValidForMiniJsonRoundTrip() {
        val values = listOf(
            "",
            "plain",
            "çğıöşü ÇĞİÖŞÜ",
            "quote \" backslash \\ slash /",
            "tab\ttab",
            "unicode \u2028 separator",
        )
        values.forEach { value ->
            assertEquals(value, MiniJson.parse(AiJson.quote(value)))
        }
        assertTrue(AiJson.quote("plain").startsWith("\""))
    }
}
