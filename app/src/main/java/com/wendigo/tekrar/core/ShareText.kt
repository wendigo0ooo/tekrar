package com.wendigo.tekrar.core

object ShareText {

    const val MAX_LENGTH = 60

    private val URL_REGEX = Regex("""(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    private val WHITESPACE_REGEX = Regex("""\s+""")

    fun extract(raw: CharSequence?): String? {
        val text = raw?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return null
        val cleaned = firstLine
            .replace(URL_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
        if (cleaned.isEmpty()) return null
        if (cleaned.length <= MAX_LENGTH) return cleaned
        return truncate(cleaned)
    }

    private fun truncate(text: String): String {
        var result = ""
        for (word in text.split(' ')) {
            if (word.isEmpty()) continue
            val candidate = if (result.isEmpty()) word else "$result $word"
            if (candidate.length > MAX_LENGTH) {
                return if (result.isEmpty()) word.take(MAX_LENGTH) else result
            }
            result = candidate
        }
        return result
    }
}
