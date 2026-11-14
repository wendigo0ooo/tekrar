package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    @Test
    fun parsesDeltaSplitAcrossChunks() {
        val parser = SseParser()
        val events = mutableListOf<AiStreamEvent>()
        events += parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"Mer")
        events += parser.feed("haba\"}}]}\n\n")
        assertEquals(listOf(AiStreamEvent.Delta("Merhaba")), events)
    }

    @Test
    fun handlesCrlfLineEndings() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}\r\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"!\"}}]}\r\n\r\n"
        )
        assertEquals(listOf(AiStreamEvent.Delta("Hi"), AiStreamEvent.Delta("!")), events)
    }

    @Test
    fun doneCarriesFinishReasonAndUsage() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {\"choices\":[{\"delta\":{\"content\":\"Selam\"},\"finish_reason\":null}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]," +
                "\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4}}\n\n" +
                "data: [DONE]\n\n"
        )
        assertEquals(1, events.filterIsInstance<AiStreamEvent.Delta>().size)
        val done = events.filterIsInstance<AiStreamEvent.Done>().single()
        assertEquals("stop", done.finishReason)
        val usageRoot = MiniJson.parse(done.usageJson!!) as Map<*, *>
        val usage = usageRoot["usage"] as Map<*, *>
        assertEquals(12.0, usage["prompt_tokens"] as Double, 0.0)
        assertEquals(4.0, usage["completion_tokens"] as Double, 0.0)
    }

    @Test
    fun doneCarriesOpencodeMetadata() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]," +
                "\"opencode\":{\"cost_usd\":0.0012,\"session\":\"ses_x\"}}\n\n" +
                "data: [DONE]\n\n"
        )
        val done = events.filterIsInstance<AiStreamEvent.Done>().single()
        val root = MiniJson.parse(done.usageJson!!) as Map<*, *>
        assertTrue(root.containsKey("opencode"))
        val opencode = root["opencode"] as Map<*, *>
        assertEquals("ses_x", opencode["session"])
    }

    @Test
    fun mergesUsageAndOpencodeFromSeparateChunks() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {\"usage\":{\"total_tokens\":30}}\n\n" +
                "data: {\"opencode\":{\"cost_usd\":0.01}}\n\n" +
                "data: [DONE]\n\n"
        )
        val done = events.filterIsInstance<AiStreamEvent.Done>().single()
        val root = MiniJson.parse(done.usageJson!!) as Map<*, *>
        assertTrue(root.containsKey("usage"))
        assertTrue(root.containsKey("opencode"))
    }

    @Test
    fun skipsMalformedJsonWithoutErrorEvent() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {bozuk}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}\n\n"
        )
        assertEquals(listOf(AiStreamEvent.Delta("ok")), events)
        assertTrue(events.none { it is AiStreamEvent.Error })
    }

    @Test
    fun ignoresCommentsEventsAndEmptyData() {
        val parser = SseParser()
        val events = parser.feed(": ping\n\nevent: message\n\ndata:\n\ndata: \n\n")
        assertEquals(emptyList<AiStreamEvent>(), events)
    }

    @Test
    fun completeOnlyEmitsOnceAndResetAllowsNewStream() {
        val parser = SseParser()
        val first = parser.feed("data: [DONE]\n\n")
        assertEquals(1, first.size)
        assertEquals(emptyList<AiStreamEvent>(), parser.feed("data: [DONE]\n\n"))

        parser.reset()
        val second = parser.feed("data: [DONE]\n\n")
        assertEquals(1, second.size)
        val done = second.single() as AiStreamEvent.Done
        assertNull(done.finishReason)
        assertNull(done.usageJson)
    }

    @Test
    fun ignoresEmptyDeltaAndNonStringContent() {
        val parser = SseParser()
        val events = parser.feed(
            "data: {\"choices\":[{\"delta\":{\"content\":\"\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":null}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"gizli\"}}]}\n\n"
        )
        assertEquals(emptyList<AiStreamEvent>(), events)
    }

    @Test
    fun handlesChunkBoundaryInsideCrlf() {
        val parser = SseParser()
        val events = mutableListOf<AiStreamEvent>()
        events += parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"a\"}}]}\r")
        assertEquals(emptyList<AiStreamEvent>(), events)
        events += parser.feed("\ndata: {\"choices\":[{\"delta\":{\"content\":\"b\"}}]}\r\n\r\n")
        assertEquals(listOf(AiStreamEvent.Delta("a"), AiStreamEvent.Delta("b")), events)
    }
}
