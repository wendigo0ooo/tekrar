package com.wendigo.tekrar

import com.wendigo.tekrar.core.Cloze
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClozeTest {

    @Test
    fun blanksFirstOccurrence() {
        val prompt = Cloze.build("I run every day", "run")
        assertEquals("I _____ every day", prompt?.sentence)
        assertEquals("run", prompt?.answer)
    }

    @Test
    fun matchesCaseInsensitivelyAndKeepsSurfaceForm() {
        val prompt = Cloze.build("She READ the book", "read")
        assertEquals("She _____ the book", prompt?.sentence)
        assertEquals("READ", prompt?.answer)
    }

    @Test
    fun matchesAcrossPunctuation() {
        assertEquals(
            "He said \"_____\" loudly.",
            Cloze.build("He said \"yes\" loudly.", "yes")?.sentence,
        )
        assertEquals(
            "The (_____) sat",
            Cloze.build("The (cat) sat", "cat")?.sentence,
        )
    }

    @Test
    fun blanksOnlyTheFirstOccurrence() {
        val prompt = Cloze.build("The cat saw the cat", "cat")
        assertEquals("The _____ saw the cat", prompt?.sentence)
        assertEquals("cat", prompt?.answer)
    }

    @Test
    fun matchesMultiWordTerms() {
        val prompt = Cloze.build("They gave up quickly", "gave up")
        assertEquals("They _____ quickly", prompt?.sentence)
        assertEquals("gave up", prompt?.answer)
    }

    @Test
    fun returnsNullWhenTermIsMissing() {
        assertNull(Cloze.build("The cat sat", "dog"))
    }

    @Test
    fun returnsNullForInflectedForms() {
        assertNull(Cloze.build("She ran home", "run"))
    }

    @Test
    fun returnsNullForBlankInput() {
        assertNull(Cloze.build("", "cat"))
        assertNull(Cloze.build("   ", "cat"))
        assertNull(Cloze.build("The cat sat", "  "))
    }

    @Test
    fun keepsTurkishCharacters() {
        val prompt = Cloze.build("Çocuk okula gitti", "okula")
        assertEquals("Çocuk _____ gitti", prompt?.sentence)
        assertEquals("okula", prompt?.answer)
    }

    @Test
    fun treatsApostrophesAsPartOfWord() {
        val prompt = Cloze.build("I don't know", "don't")
        assertEquals("I _____ know", prompt?.sentence)
        assertEquals("don't", prompt?.answer)
        assertNull(Cloze.build("I don't know", "don"))
    }

    @Test
    fun treatsHyphensAsBoundaries() {
        val prompt = Cloze.build("A well-known fact", "well-known")
        assertEquals("A _____ fact", prompt?.sentence)
        assertEquals("well-known", prompt?.answer)
        assertEquals(
            "A well-_____ fact",
            Cloze.build("A well-known fact", "known")?.sentence,
        )
    }

    @Test
    fun rejectsUnderscoreOnlyPrompt() {
        assertNull(Cloze.build("Run", "run"))
    }
}
