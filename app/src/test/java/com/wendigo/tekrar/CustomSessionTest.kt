package com.wendigo.tekrar

import com.wendigo.tekrar.core.CustomSession
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.contracts.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSessionTest {

    private fun word(
        id: Long,
        state: Int = Word.STATE_REVIEW,
        dueAt: Long = 0L,
        tags: String = "",
        deck: String = "",
        suspended: Boolean = false,
    ) = Word(
        id = id,
        term = "term$id",
        meaning = "meaning$id",
        tags = tags,
        deck = deck,
        dueAt = dueAt,
        state = state,
        suspended = suspended,
    )

    @Test
    fun parseTagsTrimsAndDropsEmptyParts() {
        assertEquals(
            listOf("yds", "grammar", "phrasal verb"),
            CustomSession.parseTags(" yds, grammar ,, phrasal verb "),
        )
        assertEquals(emptyList<String>(), CustomSession.parseTags(" , , "))
    }

    @Test
    fun normalizeTagIgnoresCaseAndWhitespace() {
        assertEquals("grammar", CustomSession.normalizeTag("  GRAMMAR  "))
        assertEquals("i", CustomSession.normalizeTag("I"))
        assertEquals("", CustomSession.normalizeTag("   "))
    }

    @Test
    fun tagMatchingIsExactAndCaseInsensitive() {
        val word = word(1, tags = "YDS, Grammar")

        assertTrue(CustomSession.hasTag(word, "grammar"))
        assertTrue(CustomSession.hasTag(word, "  YDS "))
        assertFalse(CustomSession.hasTag(word, "gram"))
        assertFalse(CustomSession.hasTag(word, "grammars"))
        assertTrue(CustomSession.hasTag(word, null))
        assertTrue(CustomSession.hasTag(word, ""))
    }

    @Test
    fun tagsAreDistinctCaseInsensitivelyAndSorted() {
        val words = listOf(
            word(1, tags = "YDS, Grammar"),
            word(2, tags = "grammar, Phrasal Verb"),
            word(3, tags = "YDS"),
        )

        assertEquals(
            listOf("Grammar", "Phrasal Verb", "YDS"),
            CustomSession.tags(words),
        )
    }

    @Test
    fun nonCramModeFiltersByDueNewAndLimit() {
        val due = word(1, dueAt = 500L)
        val future = word(2, dueAt = 2_000L)
        val fresh = word(3, state = Word.STATE_NEW, dueAt = 500L)

        val words = listOf(due, future, fresh)
        val now = 1_000L

        assertEquals(
            listOf(1L),
            CustomSession.selectQueue(words, now, CustomSession.Query(mode = StudyMode.REVIEW))
                .map { it.id },
        )
        assertEquals(
            listOf(3L),
            CustomSession.selectQueue(words, now, CustomSession.Query(mode = StudyMode.NEW))
                .map { it.id },
        )
        assertEquals(
            listOf(1L, 3L),
            CustomSession.selectQueue(words, now, CustomSession.Query(mode = StudyMode.MIXED))
                .map { it.id },
        )
        assertEquals(
            listOf(1L),
            CustomSession.selectQueue(
                words,
                now,
                CustomSession.Query(mode = StudyMode.MIXED, maxCards = 1),
            ).map { it.id },
        )
    }

    @Test
    fun deckAndTagFiltersCombineWithExactTagMatch() {
        val match = word(1, dueAt = 500L, deck = "YDS", tags = "grammar, yds")
        val otherDeck = word(2, dueAt = 500L, deck = "Phrasal", tags = "grammar")
        val otherTag = word(3, dueAt = 500L, deck = "YDS", tags = "grammars")
        val suspended = word(
            4,
            dueAt = 500L,
            deck = "YDS",
            tags = "grammar",
            suspended = true,
        )

        val result = CustomSession.selectQueue(
            listOf(match, otherDeck, otherTag, suspended),
            1_000L,
            CustomSession.Query(
                mode = StudyMode.REVIEW,
                deck = "YDS",
                tag = "GRAMMAR",
            ),
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun libraryDeckMatchesBlankDeckWords() {
        val library = word(1, dueAt = 500L, deck = "")
        val deck = word(2, dueAt = 500L, deck = "YDS")

        val result = CustomSession.selectQueue(
            listOf(library, deck),
            1_000L,
            CustomSession.Query(mode = StudyMode.REVIEW, deck = ""),
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun cramIgnoresDueDateAndIncludesNewButSkipsSuspended() {
        val due = word(1, dueAt = 500L)
        val future = word(2, dueAt = 5_000L)
        val fresh = word(3, state = Word.STATE_NEW, dueAt = 500L)
        val suspended = word(4, dueAt = 500L, suspended = true)

        val result = CustomSession.selectQueue(
            listOf(due, future, fresh, suspended),
            1_000L,
            CustomSession.Query(mode = StudyMode.REVIEW, cram = true),
            shuffle = { it },
        )

        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun cramStillAppliesDeckAndTagFilters() {
        val match = word(1, dueAt = 5_000L, deck = "YDS", tags = "grammar")
        val wrongDeck = word(2, dueAt = 5_000L, deck = "Phrasal", tags = "grammar")
        val wrongTag = word(3, dueAt = 5_000L, deck = "YDS", tags = "yds")

        val result = CustomSession.selectQueue(
            listOf(match, wrongDeck, wrongTag),
            1_000L,
            CustomSession.Query(
                deck = "YDS",
                tag = "GRAMMAR",
                cram = true,
            ),
            shuffle = { it },
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun cramUsesShuffleAndAppliesLimit() {
        val words = (1L..4L).map { word(it, dueAt = it * 1_000L) }

        val result = CustomSession.selectQueue(
            words,
            1L,
            CustomSession.Query(cram = true, maxCards = 2),
            shuffle = { it.reversed() },
        )

        assertEquals(listOf(4L, 3L), result.map { it.id })
    }

    @Test
    fun deckArgumentsRoundTrip() {
        assertEquals(CustomSession.ALL_DECKS_ARG, CustomSession.deckArg(null))
        assertEquals(CustomSession.LIBRARY_DECK_ARG, CustomSession.deckArg(""))
        assertEquals("YDS", CustomSession.deckArg("YDS"))

        assertNull(CustomSession.deckFromArg(null))
        assertNull(CustomSession.deckFromArg(CustomSession.ALL_DECKS_ARG))
        assertEquals("", CustomSession.deckFromArg(CustomSession.LIBRARY_DECK_ARG))
        assertEquals("YDS", CustomSession.deckFromArg("YDS"))
    }

    @Test
    fun tagArgumentsRoundTrip() {
        assertEquals(CustomSession.ALL_TAGS_ARG, CustomSession.tagArg(null))
        assertEquals(CustomSession.ALL_TAGS_ARG, CustomSession.tagArg("   "))
        assertEquals("grammar", CustomSession.tagArg(" grammar "))

        assertNull(CustomSession.tagFromArg(null))
        assertNull(CustomSession.tagFromArg(CustomSession.ALL_TAGS_ARG))
        assertNull(CustomSession.tagFromArg("   "))
        assertEquals("grammar", CustomSession.tagFromArg("grammar"))
    }
}
