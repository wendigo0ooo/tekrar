package com.wendigo.tekrar.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun promptVersionIsPinned() {
        assertEquals("v1", PromptBuilder.PROMPT_VERSION)
    }

    @Test
    fun tutorSystemCarriesPersonaLevelScenarioAndTargets() {
        val prompt = PromptBuilder.tutorSystem(
            level = "a1",
            scenarioTitle = "İlk tanışma",
            settingTr = "Bir partide yeni biriyle tanışıyorsun.",
            roleEn = "Sam, a friendly new acquaintance at a party.",
            goalTr = "Selamlaş, adını söyle ve nereden geldiğini sor.",
            targetPhrases = listOf("Hi", "My name is ...", "Where are you from?"),
            targetWords = listOf("hello", "name"),
        )

        assertTrue(prompt.contains("Deniz"))
        assertTrue(prompt.contains("A1"))
        assertTrue(prompt.contains("recast"))
        assertTrue(prompt.contains("Türkçe"))
        assertTrue(prompt.contains("İlk tanışma"))
        assertTrue(prompt.contains("My name is ..."))
        assertTrue(prompt.contains("hello"))
        assertTrue(prompt.contains("exactly 1 question"))
        assertTrue(prompt.contains("personal data"))
    }

    @Test
    fun tutorSystemOmitsBlankOptionalFields() {
        val prompt = PromptBuilder.tutorSystem(level = "A2")

        assertTrue(prompt.contains("A2"))
        assertFalse(prompt.contains("Scenario:"))
        assertFalse(prompt.contains("Target phrases"))
        assertFalse(prompt.contains("Target words"))
        assertFalse(prompt.contains("null"))
    }

    @Test
    fun tutorMessagesKeepsSystemPlusLastTenMessages() {
        val history = (1..12).map { AiChatMessage(AiRole.USER, "m$it") }
        val result = PromptBuilder.tutorMessages(history, "SISTEM")

        assertEquals(11, result.size)
        assertEquals(AiRole.SYSTEM, result.first().role)
        assertEquals("SISTEM", result.first().content)
        assertEquals("m3", result[1].content)
        assertEquals("m12", result.last().content)
    }

    @Test
    fun tutorMessagesDropsHistorySystemMessages() {
        val history = listOf(
            AiChatMessage(AiRole.SYSTEM, "eski sistem"),
            AiChatMessage(AiRole.USER, "hi"),
        )
        val result = PromptBuilder.tutorMessages(history, "S")

        assertEquals(2, result.size)
        assertEquals("S", result.first().content)
        assertEquals(1, result.count { it.role == AiRole.SYSTEM })
    }

    @Test
    fun tutorMessagesDropsBlankMessages() {
        val history = listOf(
            AiChatMessage(AiRole.USER, "hi"),
            AiChatMessage(AiRole.ASSISTANT, "   "),
            AiChatMessage(AiRole.USER, "hello"),
        )
        val result = PromptBuilder.tutorMessages(history, "S")

        assertEquals(3, result.size)
        assertEquals("hi", result[1].content)
        assertEquals("hello", result[2].content)
    }

    @Test
    fun correctionPromptContainsSchemaAndUserMessage() {
        val prompt = PromptBuilder.correctionPrompt("I goed to school yesterday.")

        assertTrue(prompt.contains("Yalnızca JSON"))
        assertTrue(prompt.contains("has_error"))
        assertTrue(prompt.contains("corrected"))
        assertTrue(prompt.contains("note_tr"))
        assertTrue(prompt.contains("severity"))
        assertTrue(prompt.contains("I goed to school yesterday."))
    }

    @Test
    fun summaryPromptIncludesDraftAndTranscript() {
        val transcript = listOf(
            AiChatMessage(AiRole.USER, "I goed to school."),
            AiChatMessage(AiRole.ASSISTANT, "You went to school. What did you study?"),
        )
        val prompt = PromptBuilder.summaryPrompt(transcript, "Doğruluk: 3/5, hedef ifade: 2/3")

        assertTrue(prompt.contains("Doğruluk: 3/5"))
        assertTrue(prompt.contains("USER: I goed to school."))
        assertTrue(prompt.contains("ASSISTANT: You went to school."))
        assertTrue(prompt.contains("strengths"))
        assertTrue(prompt.contains("next_focus"))
        assertTrue(prompt.contains("uydurma"))
    }

    @Test
    fun summaryPromptSkipsSystemMessages() {
        val transcript = listOf(
            AiChatMessage(AiRole.SYSTEM, "gizli sistem talimatı"),
            AiChatMessage(AiRole.USER, "hello"),
        )
        val prompt = PromptBuilder.summaryPrompt(transcript, null)

        assertFalse(prompt.contains("gizli sistem talimatı"))
        assertTrue(prompt.contains("USER: hello"))
    }

    @Test
    fun parsesCorrectionInsideCodeFence() {
        val raw = "```json\n{\"has_error\": true, \"corrected\": \"I went to school.\", " +
            "\"note_tr\": \"go fiilinin geçmişi went.\", \"severity\": \"medium\"}\n```"
        val result = PromptBuilder.parseCorrectionJson(raw)

        assertEquals(true, result?.hasError)
        assertEquals("I went to school.", result?.corrected)
        assertEquals("go fiilinin geçmişi went.", result?.noteTr)
        assertEquals("medium", result?.severity)
    }

    @Test
    fun parsesCorrectionWithMissingOptionalFieldsAsNull() {
        val result = PromptBuilder.parseCorrectionJson("{\"has_error\": false}")

        assertEquals(false, result?.hasError)
        assertNull(result?.corrected)
        assertNull(result?.noteTr)
        assertNull(result?.severity)
    }

    @Test
    fun rejectsCorrectionWithoutBooleanFlag() {
        assertNull(PromptBuilder.parseCorrectionJson("{\"corrected\": \"x\"}"))
        assertNull(PromptBuilder.parseCorrectionJson("bozuk metin"))
        assertNull(PromptBuilder.parseCorrectionJson("{\"has_error\": \"true\"}"))
    }

    @Test
    fun keepsPresentEmptyNoteAsEmptyString() {
        val result = PromptBuilder.parseCorrectionJson(
            "{\"has_error\": false, \"corrected\": \"I am fine.\", \"note_tr\": \"\"}"
        )

        assertEquals("", result?.noteTr)
        assertEquals("I am fine.", result?.corrected)
    }

    @Test
    fun parsesCorrectionWithTrailingProse() {
        val result = PromptBuilder.parseCorrectionJson("{\"has_error\": false} Umarım yardımcı olur.")

        assertEquals(false, result?.hasError)
    }

    @Test
    fun ignoresUnknownSeverity() {
        val result = PromptBuilder.parseCorrectionJson(
            "{\"has_error\": true, \"corrected\": \"I am fine.\", \"severity\": \"critical\"}"
        )
        assertNull(result?.severity)
    }

    @Test
    fun parsesSummaryWithProseAroundJson() {
        val raw = "İşte özet:\n```json\n{\"strengths\": [\"Selamlaşma iyiydi\", \"Cesareti yüksekti\"], " +
            "\"fixes\": [\"Geçmiş zaman\", \"Artikel\"], " +
            "\"next_focus\": \"Düzenli geçmiş zamanda 5 cümle yaz.\", \"cefr_note\": \"A1\"}\n```\nUmarım yardımcı olur."
        val summary = PromptBuilder.parseSummaryJson(raw)

        assertEquals(listOf("Selamlaşma iyiydi", "Cesareti yüksekti"), summary?.strengths)
        assertEquals(listOf("Geçmiş zaman", "Artikel"), summary?.fixes)
        assertEquals("Düzenli geçmiş zamanda 5 cümle yaz.", summary?.nextFocus)
        assertEquals("A1", summary?.cefrNote)
    }

    @Test
    fun parsesSummaryWithMissingFieldsAsNull() {
        val summary = PromptBuilder.parseSummaryJson("{}")

        assertNull(summary?.strengths)
        assertNull(summary?.fixes)
        assertNull(summary?.nextFocus)
        assertNull(summary?.cefrNote)
    }

    @Test
    fun rejectsInvalidSummary() {
        assertNull(PromptBuilder.parseSummaryJson("sadece metin"))
        assertNull(PromptBuilder.parseSummaryJson("[1, 2, 3]"))
    }
}
