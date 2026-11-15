package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException

data class GrammarAiIssue(
    val wrong: String,
    val correct: String,
    val noteTr: String,
)

data class GrammarAiResult(
    val hasError: Boolean,
    val corrected: String?,
    val issues: List<GrammarAiIssue>,
    val verdictTr: String?,
)

/**
 * AI gramer kontrolünün yanıt modeli, prompt köprüsü ve JSON ayrıştırıcısı.
 * AI yalnızca ikincil kontroldür; yerel [com.wendigo.tekrar.core.grammar.GrammarChecker]
 * sonucunu değiştirmez.
 */
object GrammarAi {

    const val MAX_ISSUES = 4
    const val TEMPERATURE = 0.2
    const val MAX_TOKENS = 300

    fun prompt(
        text: String,
        topicTitleTr: String,
        level: String = "A1",
    ): String = PromptBuilder.grammarCheckPrompt(text, topicTitleTr, level)

    fun parseJson(raw: String): GrammarAiResult? {
        val root = parseObject(raw) ?: return null
        val hasError = root["has_error"] as? Boolean ?: return null
        val corrected = (root["corrected"] as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val issues = (root["issues"] as? List<*>)
            .orEmpty()
            .mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val wrong = (map["wrong"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                val correct = (map["correct"] as? String)?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                val note = (map["note_tr"] as? String)?.trim().orEmpty()
                GrammarAiIssue(wrong, correct, note)
            }
            .take(MAX_ISSUES)
        val verdict = (root["verdict_tr"] as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return GrammarAiResult(
            hasError = hasError,
            corrected = corrected,
            issues = issues,
            verdictTr = verdict,
        )
    }

    /**
     * AI'ın `wrong` metnini orijinal cümlede bulur (kırmızı işaretleme için).
     * Bulunamazsa null; UI eşleşmeyenleri işaretlemez.
     */
    fun locate(original: String, wrong: String): IntRange? {
        val needle = wrong.trim()
        if (needle.isEmpty()) return null
        val exact = original.indexOf(needle)
        if (exact >= 0) return exact until exact + needle.length
        val match = runCatching {
            Regex(Regex.escape(needle), RegexOption.IGNORE_CASE).find(original)
        }.getOrNull() ?: return null
        return match.range
    }

    private fun parseObject(raw: String): Map<*, *>? {
        val text = stripCodeFence(raw)
        parseMap(text)?.let { return it }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return parseMap(text.substring(start, end + 1))
    }

    private fun parseMap(text: String): Map<*, *>? = try {
        MiniJson.parse(text) as? Map<*, *>
    } catch (error: MiniJsonException) {
        null
    }

    private fun stripCodeFence(raw: String): String {
        var text = raw.trim()
        if (!text.startsWith("```")) return text
        val firstLineEnd = text.indexOf('\n')
        if (firstLineEnd < 0) {
            return text.removePrefix("```").removeSuffix("```").trim()
        }
        text = text.substring(firstLineEnd + 1)
        val closing = text.lastIndexOf("```")
        if (closing >= 0) text = text.substring(0, closing)
        return text.trim()
    }
}
