package com.wendigo.tekrar.ai

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException
import java.util.Locale

data class CorrectionResult(
    val hasError: Boolean,
    val corrected: String?,
    val noteTr: String?,
    val severity: String?,
)

data class AiSessionSummary(
    val strengths: List<String>?,
    val fixes: List<String>?,
    val nextFocus: String?,
    val cefrNote: String?,
)

object PromptBuilder {

    const val PROMPT_VERSION = "v1"
    const val MAX_HISTORY_MESSAGES = 10
    const val MAX_SUMMARY_MESSAGES = 40

    private val SEVERITIES = setOf("low", "medium", "high")

    fun tutorSystem(
        level: String,
        scenarioTitle: String? = null,
        settingTr: String? = null,
        roleEn: String? = null,
        goalTr: String? = null,
        targetPhrases: List<String> = emptyList(),
        targetWords: List<String> = emptyList(),
    ): String {
        val cefr = level.trim().uppercase(Locale.ROOT).ifEmpty { "A1" }
        val builder = StringBuilder()
        builder.appendLine(
            "You are Deniz, a patient and friendly English tutor. " +
                "The learner is a Turkish speaker learning English."
        )
        builder.appendLine("Learner CEFR level: $cefr. Keep your English at this level.")
        appendOptional(builder, "Scenario", scenarioTitle)
        appendOptional(builder, "Setting (Turkish)", settingTr)
        appendOptional(builder, "Your role in the scenario", roleEn)
        appendOptional(builder, "Learner goal (Turkish)", goalTr)
        val phrases = targetPhrases.map { it.trim() }.filter { it.isNotEmpty() }
        if (phrases.isNotEmpty()) {
            builder.appendLine(
                "Target phrases to encourage: " + phrases.joinToString(", ") { "\"$it\"" } + "."
            )
        }
        val words = targetWords.map { it.trim() }.filter { it.isNotEmpty() }
        if (words.isNotEmpty()) {
            builder.appendLine("Target words to reuse naturally: " + words.joinToString(", ") + ".")
        }
        builder.appendLine("Rules:")
        builder.appendLine("- Reply in English with at most 2 short sentences followed by exactly 1 question.")
        builder.appendLine("- Keep the learner talking; never write long explanations or lists.")
        builder.appendLine(
            "- Do not lecture about grammar. When the learner makes a mistake, recast the sentence " +
                "naturally in your reply and continue the conversation."
        )
        builder.appendLine(
            "- If the learner writes in Turkish or seems stuck, give a very short Turkish (Türkçe) " +
                "hint in one sentence, then continue in English."
        )
        builder.appendLine(
            "- Stay in the scenario and in your role. Do not change the topic or invent a new setting."
        )
        builder.appendLine(
            "- Never ask for or repeat personal data (full name, address, phone, school, " +
                "workplace, passwords, financial details)."
        )
        builder.appendLine("- No inappropriate content, no markdown, no emojis.")
        return builder.toString().trimEnd()
    }

    fun tutorMessages(
        history: List<AiChatMessage>,
        system: String,
    ): List<AiChatMessage> {
        val recent = history
            .filter { it.role != AiRole.SYSTEM && it.content.isNotBlank() }
            .takeLast(MAX_HISTORY_MESSAGES)
        return buildList {
            add(AiChatMessage(AiRole.SYSTEM, system))
            addAll(recent)
        }
    }

    fun correctionPrompt(userMessage: String): String = buildString {
        appendLine("Kullanıcının İngilizce mesajını değerlendir. Yalnızca JSON döndür; JSON dışında hiçbir şey yazma.")
        appendLine(
            "{\"has_error\": bool, \"corrected\": \"düzeltilmiş cümle\", " +
                "\"note_tr\": \"kısa Türkçe açıklama\", \"severity\": \"low|medium|high\"}"
        )
        appendLine("Kurallar:")
        appendLine("- Hata yoksa has_error=false, corrected=orijinal cümle, note_tr=\"\" olsun.")
        appendLine("- corrected her zaman İngilizce ve tek cümle olsun.")
        appendLine("- note_tr en fazla 12 kelime, sade Türkçe olsun.")
        appendLine("- severity: küçük yazım/ton hatası low, dilbilgisi hatası medium, anlaşılmayan cümle high.")
        append("Mesaj: ").append(userMessage.trim())
    }

    fun summaryPrompt(
        transcript: List<AiChatMessage>,
        deterministicDraft: String? = null,
    ): String = buildString {
        appendLine("Aşağıdaki İngilizce pratik oturumunu değerlendir. Yalnızca JSON döndür; JSON dışında hiçbir şey yazma.")
        appendLine("{\"strengths\": [\"...\"], \"fixes\": [\"...\"], \"next_focus\": \"...\", \"cefr_note\": \"A1\"}")
        appendLine("Kurallar:")
        appendLine("- strengths 2-3, fixes 2-3, next_focus tek cümle, cefr_note kısa seviye (A1/A2) olsun.")
        appendLine("- Yalnızca aşağıdaki konuşmaya ve deterministik taslağa dayan; olmayan bir şey uydurma.")
        appendLine("- Metinler Türkçe olsun; İngilizce örnekler tırnak içinde kalabilir.")
        if (!deterministicDraft.isNullOrBlank()) {
            appendLine("Deterministik taslak:")
            appendLine(deterministicDraft.trim())
        }
        appendLine("Oturum:")
        val lines = transcript
            .filter { it.role != AiRole.SYSTEM && it.content.isNotBlank() }
            .takeLast(MAX_SUMMARY_MESSAGES)
        if (lines.isEmpty()) {
            appendLine("(boş oturum)")
        } else {
            lines.forEach { appendLine("${it.role.name}: ${it.content.trim()}") }
        }
    }

    fun parseCorrectionJson(raw: String): CorrectionResult? {
        val root = parseObject(raw) ?: return null
        val hasError = root["has_error"] as? Boolean ?: return null
        val corrected = (root["corrected"] as? String)?.trim()
        val noteTr = (root["note_tr"] as? String)?.trim()
        val severity = (root["severity"] as? String)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it in SEVERITIES }
        return CorrectionResult(
            hasError = hasError,
            corrected = corrected,
            noteTr = noteTr,
            severity = severity,
        )
    }

    fun parseSummaryJson(raw: String): AiSessionSummary? {
        val root = parseObject(raw) ?: return null
        return AiSessionSummary(
            strengths = stringList(root["strengths"]),
            fixes = stringList(root["fixes"]),
            nextFocus = (root["next_focus"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            cefrNote = (root["cefr_note"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun appendOptional(builder: StringBuilder, label: String, value: String?) {
        val text = value?.trim().orEmpty()
        if (text.isNotEmpty()) builder.appendLine("$label: $text")
    }

    private fun stringList(value: Any?): List<String>? {
        val list = value as? List<*> ?: return null
        return list.mapNotNull { (it as? String)?.trim()?.takeIf { text -> text.isNotEmpty() } }
    }

    private fun parseObject(raw: String): Map<*, *>? {
        val text = stripCodeFence(raw)
        parseMap(text)?.let { return it }
        val extracted = extractObject(text) ?: return null
        return parseMap(extracted)
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

    private fun extractObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
