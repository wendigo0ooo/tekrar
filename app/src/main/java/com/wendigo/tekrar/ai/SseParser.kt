package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException

class SseParser {

    private val lineBuffer = StringBuilder()
    private var finishReason: String? = null
    private var usageValue: Any? = null
    private var opencodeValue: Any? = null
    private var done = false

    fun feed(chunk: String): List<AiStreamEvent> {
        if (chunk.isEmpty() || done) return emptyList()
        lineBuffer.append(chunk)
        val events = ArrayList<AiStreamEvent>()
        while (true) {
            val line = nextLine() ?: break
            handleLine(line, events)
            if (done) break
        }
        return events
    }

    fun reset() {
        lineBuffer.setLength(0)
        finishReason = null
        usageValue = null
        opencodeValue = null
        done = false
    }

    private fun nextLine(): String? {
        var index = 0
        while (index < lineBuffer.length) {
            when (lineBuffer[index]) {
                '\n' -> {
                    val line = lineBuffer.substring(0, index)
                    lineBuffer.delete(0, index + 1)
                    return line
                }
                '\r' -> {
                    if (index + 1 >= lineBuffer.length) return null
                    val skip = if (lineBuffer[index + 1] == '\n') 2 else 1
                    val line = lineBuffer.substring(0, index)
                    lineBuffer.delete(0, index + skip)
                    return line
                }
                else -> index++
            }
        }
        return null
    }

    private fun handleLine(rawLine: String, events: MutableList<AiStreamEvent>) {
        if (rawLine.isEmpty() || rawLine.startsWith(":")) return
        val colon = rawLine.indexOf(':')
        val field = if (colon >= 0) rawLine.substring(0, colon) else rawLine
        if (field != "data") return
        var payload = if (colon >= 0) rawLine.substring(colon + 1) else ""
        if (payload.startsWith(" ")) payload = payload.substring(1)
        if (payload.isEmpty()) return
        if (payload.trim() == DONE_MARKER) {
            finish(events)
            return
        }
        val parsed = try {
            MiniJson.parse(payload)
        } catch (error: MiniJsonException) {
            return
        }
        val root = parsed as? Map<*, *> ?: return
        val choices = root["choices"] as? List<*>
        val choice = choices?.firstOrNull() as? Map<*, *>
        val delta = choice?.get("delta") as? Map<*, *>
        val content = delta?.get("content") as? String
        if (!content.isNullOrEmpty()) {
            events += AiStreamEvent.Delta(content)
        }
        val reason = choice?.get("finish_reason") as? String
        if (!reason.isNullOrBlank()) finishReason = reason
        if (root.containsKey("usage") && root["usage"] != null) usageValue = root["usage"]
        if (root.containsKey("opencode") && root["opencode"] != null) opencodeValue = root["opencode"]
    }

    private fun finish(events: MutableList<AiStreamEvent>) {
        if (done) return
        done = true
        events += AiStreamEvent.Done(finishReason, usagePayload())
    }

    private fun usagePayload(): String? {
        if (usageValue == null && opencodeValue == null) return null
        val payload = LinkedHashMap<String, Any?>()
        usageValue?.let { payload["usage"] = it }
        opencodeValue?.let { payload["opencode"] = it }
        return AiJson.encode(payload)
    }

    private companion object {
        const val DONE_MARKER = "[DONE]"
    }
}
