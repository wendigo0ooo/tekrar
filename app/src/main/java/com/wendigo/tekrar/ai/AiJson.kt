package com.wendigo.tekrar.ai

object AiJson {

    fun quote(value: String): String = StringBuilder().also { quoteTo(it, value) }.toString()

    fun encode(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

    fun buildChatRequestJson(
        config: AiConfig,
        messages: List<AiChatMessage>,
        stream: Boolean,
    ): String = buildString {
        append('{')
        append("\"model\":")
        quoteTo(this, config.model.trim())
        append(",\"messages\":[")
        messages.forEachIndexed { index, message ->
            if (index > 0) append(',')
            append("{\"role\":")
            quoteTo(this, message.role.wire)
            append(",\"content\":")
            quoteTo(this, message.content)
            append('}')
        }
        append(']')
        append(",\"temperature\":")
        appendNumber(this, config.temperature)
        append(",\"max_tokens\":").append(config.maxTokens)
        append(",\"stream\":").append(stream)
        append('}')
    }

    private fun quoteTo(builder: StringBuilder, value: String) {
        builder.append('"')
        value.forEach { char ->
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\u2028' -> builder.append("\\u2028")
                '\u2029' -> builder.append("\\u2029")
                else -> if (char < ' ') appendUnicodeEscape(builder, char.code) else builder.append(char)
            }
        }
        builder.append('"')
    }

    private fun appendUnicodeEscape(builder: StringBuilder, code: Int) {
        val hex = Integer.toHexString(code)
        builder.append("\\u")
        repeat(4 - hex.length) { builder.append('0') }
        builder.append(hex)
    }

    private fun appendNumber(builder: StringBuilder, value: Double) {
        if (!value.isFinite()) {
            builder.append("null")
        } else if (value % 1.0 == 0.0 && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
            builder.append(value.toInt())
        } else {
            builder.append(value)
        }
    }

    private fun writeValue(builder: StringBuilder, value: Any?) {
        when (value) {
            null -> builder.append("null")
            is String -> quoteTo(builder, value)
            is Boolean -> builder.append(value)
            is Byte, is Short, is Int, is Long -> builder.append(value.toString())
            is Float -> appendNumber(builder, value.toDouble())
            is Double -> appendNumber(builder, value)
            is Map<*, *> -> {
                builder.append('{')
                var first = true
                for ((key, item) in value) {
                    if (key == null) continue
                    if (!first) builder.append(',')
                    first = false
                    quoteTo(builder, key.toString())
                    builder.append(':')
                    writeValue(builder, item)
                }
                builder.append('}')
            }
            is Iterable<*> -> {
                builder.append('[')
                var first = true
                for (item in value) {
                    if (!first) builder.append(',')
                    first = false
                    writeValue(builder, item)
                }
                builder.append(']')
            }
            is Array<*> -> {
                builder.append('[')
                value.forEachIndexed { index, item ->
                    if (index > 0) builder.append(',')
                    writeValue(builder, item)
                }
                builder.append(']')
            }
            else -> quoteTo(builder, value.toString())
        }
    }
}
