package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.ai.GrammarAi
import com.wendigo.tekrar.ai.PromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarAiTest {

    @Test
    fun parsesValidJson() {
        val result = GrammarAi.parseJson(
            """
            {"has_error": true, "corrected": "I am a student.",
             "issues": [{"wrong": "is", "correct": "am", "note_tr": "I ile am."}],
             "verdict_tr": "Neredeyse doğru."}
            """.trimIndent(),
        )

        assertNotNull(result)
        assertTrue(result!!.hasError)
        assertEquals("I am a student.", result.corrected)
        assertEquals(1, result.issues.size)
        assertEquals("is", result.issues.single().wrong)
        assertEquals("am", result.issues.single().correct)
        assertEquals("I ile am.", result.issues.single().noteTr)
        assertEquals("Neredeyse doğru.", result.verdictTr)
    }

    @Test
    fun parsesJsonInsideCodeFenceAndExtraText() {
        val fenced = """
            ```json
            {"has_error": false, "corrected": "I am happy.", "issues": [], "verdict_tr": ""}
            ```
        """.trimIndent()
        val extra = "İşte sonuç: {\"has_error\": false, \"corrected\": \"I am happy.\"}"

        assertFalse(GrammarAi.parseJson(fenced)!!.hasError)
        assertEquals("I am happy.", GrammarAi.parseJson(extra)?.corrected)
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(GrammarAi.parseJson("bu JSON değil"))
        assertNull(GrammarAi.parseJson("{\"corrected\": \"I am happy.\"}"))
    }

    @Test
    fun issuesAreLimitedToFourAndInvalidEntriesSkipped() {
        val issues = (1..6).joinToString(",") {
            "{\"wrong\": \"w$it\", \"correct\": \"c$it\", \"note_tr\": \"n$it\"}"
        }
        val json = """
            {"has_error": true, "corrected": "I am happy.",
             "issues": [$issues, {"wrong": "", "correct": "x", "note_tr": ""}]}
        """.trimIndent()

        val result = GrammarAi.parseJson(json)!!

        assertEquals(4, result.issues.size)
        assertEquals(listOf("w1", "w2", "w3", "w4"), result.issues.map { it.wrong })
    }

    @Test
    fun locateFindsCaseInsensitiveRange() {
        val original = "I is a student."

        val range = GrammarAi.locate(original, "IS")

        assertNotNull(range)
        assertEquals("is", original.substring(range!!.first, range.last + 1))
        assertNull(GrammarAi.locate(original, "are"))
    }

    @Test
    fun promptContainsTopicLevelAndSentence() {
        val prompt = GrammarAi.prompt("I is a student.", "am / is / are", "A1")

        assertTrue(prompt.contains("am / is / are"))
        assertTrue(prompt.contains("A1"))
        assertTrue(prompt.contains("I is a student."))
        assertTrue(prompt.contains("has_error"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun promptBuilderDelegatesToGrammarAiParser() {
        val json = "{\"has_error\": false, \"corrected\": \"I am happy.\"}"

        assertEquals(
            GrammarAi.parseJson(json),
            PromptBuilder.parseGrammarCheckJson(json),
        )
        assertNull(PromptBuilder.parseGrammarCheckJson("bozuk"))
    }
}
