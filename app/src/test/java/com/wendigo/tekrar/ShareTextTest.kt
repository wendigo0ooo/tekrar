package com.wendigo.tekrar

import com.wendigo.tekrar.core.ShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {

    @Test
    fun keepsPlainWord() {
        assertEquals("ephemeral", ShareText.extract("ephemeral"))
    }

    @Test
    fun takesFirstNonBlankLine() {
        assertEquals(
            "The quick brown fox",
            ShareText.extract("\n  \n  The quick brown fox  \njumps over"),
        )
    }

    @Test
    fun stripsUrls() {
        assertEquals(
            "Look at now",
            ShareText.extract("Look at https://example.com/foo?x=1 now"),
        )
        assertNull(ShareText.extract("https://example.com/foo"))
        assertNull(ShareText.extract("www.example.com"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertNull(ShareText.extract(null))
        assertNull(ShareText.extract(""))
        assertNull(ShareText.extract("   \n\t  "))
    }

    @Test
    fun shortensVeryLongTextToPhraseWithinLimit() {
        val text = "one two three four five six seven eight nine ten eleven twelve thirteen"
        val result = ShareText.extract(text)!!
        assertTrue(result.length <= ShareText.MAX_LENGTH)
        assertTrue(text.startsWith(result))
        assertTrue(result.startsWith("one two three"))
    }

    @Test
    fun truncatesSingleVeryLongToken() {
        val result = ShareText.extract("a".repeat(100))!!
        assertEquals(ShareText.MAX_LENGTH, result.length)
    }
}
