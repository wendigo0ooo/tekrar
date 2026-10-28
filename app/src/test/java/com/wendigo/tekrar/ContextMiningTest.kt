package com.wendigo.tekrar

import com.wendigo.tekrar.core.ContextMining
import com.wendigo.tekrar.core.MinedWord
import com.wendigo.tekrar.core.MiningDefinition
import com.wendigo.tekrar.core.MiningSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMiningTest {

    @Test
    fun splitsSentencesOnTerminalPunctuationAndNewlines() {
        val sentences = ContextMining.splitSentences(
            "First one. Second? Third!  \nFourth without stop\n\n Fifth."
        )

        assertEquals(
            listOf(
                "First one.",
                "Second?",
                "Third!",
                "Fourth without stop",
                "Fifth.",
            ),
            sentences,
        )
    }

    @Test
    fun keepsQuotesWithSentenceAndIgnoresDecimals() {
        val sentences = ContextMining.splitSentences(
            "He said \"Stop.\" Then pi is 3.14 roughly. Fine!"
        )

        assertEquals(3, sentences.size)
        assertEquals("He said \"Stop.\"", sentences[0])
        assertTrue(sentences[1].contains("3.14"))
    }

    @Test
    fun tokenizesLettersApostrophesAndHyphens() {
        val tokens = ContextMining.tokenize(
            "Don't stop—well-known 42 rock'n'roll café İstanbul Türkçe."
        )

        assertEquals(
            listOf("Don't", "stop", "well-known", "rock'n'roll"),
            tokens,
        )
    }

    @Test
    fun filtersStopwordsAndShortTokens() {
        assertFalse(ContextMining.isEligible("the"))
        assertFalse(ContextMining.isEligible("a"))
        assertFalse(ContextMining.isEligible("go"))
        assertFalse(ContextMining.isEligible("don't"))
        assertTrue(ContextMining.isEligible("cat"))
        assertTrue(ContextMining.isEligible("well-known"))
    }

    @Test
    fun mineDedupesUsesFirstSentenceAndSkipsKnownWords() {
        val text = "The novel was good. A novel idea appeared. The cat slept."
        val definitions = mapOf(
            "novel" to MiningDefinition("novel", "roman", "noun", "B1"),
            "idea" to MiningDefinition("idea", "fikir", "noun", "A2"),
            "cat" to MiningDefinition("cat", "kedi", "noun", "A1"),
        )

        val mined = ContextMining.mine(text, listOf("CAT")) { key -> definitions[key] }

        assertEquals(listOf("novel", "idea"), mined.map { it.term })
        assertEquals("The novel was good.", mined[0].context)
        assertEquals("roman", mined[0].meaning)
        assertEquals("B1", mined[0].cefr)
        assertEquals(MiningSource.DICTIONARY, mined[0].source)
    }

    @Test
    fun mineUsesDeckSourceAndCanonicalTerm() {
        val text = "They will look after the child."
        val definitions = mapOf(
            "look" to MiningDefinition("look after", "bakmak", source = MiningSource.DECK),
        )

        val mined = ContextMining.mine(text, emptyList()) { key -> definitions[key] }

        assertEquals(1, mined.size)
        assertEquals("look after", mined[0].term)
        assertEquals(MiningSource.DECK, mined[0].source)
        assertEquals("They will look after the child.", mined[0].context)
    }

    @Test
    fun minePrefersMultiWordDeckTerm() {
        val text = "They look after the child."
        val definitions = mapOf(
            "look" to MiningDefinition("look", "bakmak"),
            "look after" to MiningDefinition("look after", "bakmak, ilgilenmek", source = MiningSource.DECK),
        )

        val mined = ContextMining.mine(text, emptyList()) { key -> definitions[key] }

        assertEquals(1, mined.size)
        assertEquals("look after", mined[0].term)
        assertEquals(MiningSource.DECK, mined[0].source)
    }

    @Test
    fun mineReturnsNothingWhenNoLookupHits() {
        val mined = ContextMining.mine(
            "The unknownword appears here.",
            emptyList(),
        ) { null }

        assertTrue(mined.isEmpty())
    }

    @Test
    fun planAddRejectsKnownAndDuplicateCandidates() {
        val candidates = listOf(
            MinedWord("novel", "roman", context = "Novel text."),
            MinedWord("NOVEL", "roman", context = "Another novel."),
            MinedWord("cat", "kedi", context = "A cat."),
            MinedWord("idea", "fikir", context = "An idea."),
        )

        val batch = ContextMining.planAdd(candidates, listOf("cat"))

        assertEquals(listOf("novel", "idea"), batch.words.map { it.term })
        assertEquals(2, batch.skipped)
    }
}
