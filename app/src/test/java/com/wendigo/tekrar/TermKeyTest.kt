package com.wendigo.tekrar

import com.wendigo.tekrar.core.TermKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermKeyTest {

    @Test
    fun trimsCollapsesWhitespaceAndLowercases() {
        assertEquals("hello world", TermKey.normalize("  Hello \t  World  "))
        assertEquals("a b c", TermKey.normalize("a\nb\r\nc"))
        assertEquals("", TermKey.normalize("   "))
    }

    @Test
    fun usesRootLocaleForEnglishTerms() {
        assertEquals("i", TermKey.normalize("I"))
        assertEquals("go", TermKey.normalize("GO"))
    }

    @Test
    fun duplicateDecisionIgnoresCaseAndSpacing() {
        assertTrue(TermKey.isDuplicate("GO", "  go  "))
        assertTrue(TermKey.isDuplicate("give   up", "Give Up"))
        assertFalse(TermKey.isDuplicate("go", "goes"))
    }
}
