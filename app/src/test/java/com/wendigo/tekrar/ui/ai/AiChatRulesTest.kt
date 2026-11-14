package com.wendigo.tekrar.ui.ai

import com.wendigo.tekrar.ai.AiSessionSummary
import com.wendigo.tekrar.ai.CorrectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatRulesTest {

    private fun readyState(): AiChatUiState = AiChatUiState(
        sessionId = "ses_test",
        draft = "Hello",
        hasKey = true,
    )

    @Test
    fun canSendRequiresSessionKeyAndIdleState() {
        assertTrue(AiChatRules.canSend(readyState(), "Hello"))
        assertFalse(AiChatRules.canSend(readyState(), "   "))
        assertFalse(AiChatRules.canSend(readyState().copy(sessionId = null), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(hasKey = false), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(limitReached = true), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(isStarting = true), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(isStreaming = true), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(isSummarizing = true), "Hello"))
        assertFalse(AiChatRules.canSend(readyState().copy(ended = true), "Hello"))
    }

    @Test
    fun canEndNeedsUserTurnAndIdleState() {
        val withUser = readyState().copy(
            messages = listOf(
                AiChatMessageUi(role = AiChatRole.ASSISTANT, text = "Hi!"),
                AiChatMessageUi(role = AiChatRole.USER, text = "Hello"),
            ),
        )
        assertTrue(withUser.canEnd)
        assertFalse(withUser.copy(isStreaming = true).canEnd)
        assertFalse(withUser.copy(ended = true).canEnd)
        assertFalse(withUser.copy(messages = withUser.messages.take(1)).canEnd)
        assertFalse(withUser.copy(sessionId = null).canEnd)
    }

    @Test
    fun summaryUiTrimsDeduplicatesAndCapsItems() {
        val summary = AiSessionSummary(
            strengths = listOf("  iyi  ", "", "iyi", "ikinci", "ucuncu", "dorduncu"),
            fixes = listOf(" duzeltme "),
            nextFocus = "  odak  ",
            cefrNote = null,
        )

        val ui = AiChatRules.summaryUi(summary, "a2")

        assertEquals(listOf("iyi", "ikinci", "ucuncu"), ui?.strengths)
        assertEquals(listOf("duzeltme"), ui?.fixes)
        assertEquals("odak", ui?.nextFocus)
        assertEquals("A2", ui?.cefrNote)
    }

    @Test
    fun summaryUiKeepsAiCefrNoteWhenPresent() {
        val ui = AiChatRules.summaryUi(
            AiSessionSummary(
                strengths = listOf("akıcılık"),
                fixes = emptyList(),
                nextFocus = null,
                cefrNote = " b1 ",
            ),
            fallbackCefr = "A1",
        )

        assertEquals("B1", ui?.cefrNote)
    }

    @Test
    fun summaryUiRejectsEmptyDraft() {
        assertNull(
            AiChatRules.summaryUi(
                AiSessionSummary(
                    strengths = emptyList(),
                    fixes = listOf(" "),
                    nextFocus = "   ",
                    cefrNote = "A1",
                ),
                fallbackCefr = "A1",
            ),
        )
        assertNull(AiChatRules.summaryUi(null, "A1"))
    }

    @Test
    fun correctionUiSkipsCleanAndEmptyResults() {
        assertNull(
            AiChatRules.correctionUi(
                CorrectionResult(hasError = false, corrected = "x", noteTr = "", severity = null),
                "x",
            ),
        )
        assertNull(
            AiChatRules.correctionUi(
                CorrectionResult(hasError = true, corrected = null, noteTr = "not", severity = "low"),
                "x",
            ),
        )
        assertNull(
            AiChatRules.correctionUi(
                CorrectionResult(hasError = true, corrected = "x", noteTr = "", severity = "low"),
                " x ",
            ),
        )
    }

    @Test
    fun correctionUiBuildsCardFromError() {
        val ui = AiChatRules.correctionUi(
            CorrectionResult(
                hasError = true,
                corrected = " I go ",
                noteTr = " geçmiş zaman ",
                severity = "HIGH",
            ),
            " I goed ",
        )

        assertEquals("I goed", ui?.original)
        assertEquals("I go", ui?.corrected)
        assertEquals("geçmiş zaman", ui?.noteTr)
        assertEquals("high", ui?.severity)
    }

    @Test
    fun correctionUiKeepsNoteOnlyDifference() {
        val ui = AiChatRules.correctionUi(
            CorrectionResult(
                hasError = true,
                corrected = "Hello",
                noteTr = "Daha doğal bir selam.",
                severity = "low",
            ),
            "Hello",
        )

        assertEquals("Hello", ui?.corrected)
        assertEquals("Daha doğal bir selam.", ui?.noteTr)
    }

    @Test
    fun cleanDraftCapsLength() {
        val long = "a".repeat(AiChatRules.MAX_DRAFT_LENGTH + 50)
        assertEquals(AiChatRules.MAX_DRAFT_LENGTH, AiChatRules.cleanDraft(long).length)
        assertEquals("kısa", AiChatRules.cleanDraft("kısa"))
    }
}
