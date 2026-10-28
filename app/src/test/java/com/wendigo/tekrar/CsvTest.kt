package com.wendigo.tekrar

import com.wendigo.tekrar.core.Csv
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun encodeSimpleRow() {
        assertEquals("a;b;c", Csv.encodeRow(listOf("a", "b", "c")))
    }

    @Test
    fun encodeEscapesSeparatorQuoteAndNewline() {
        assertEquals("\"a;b\";c", Csv.encodeRow(listOf("a;b", "c")))
        assertEquals("\"he said \"\"hi\"\"\"", Csv.encodeRow(listOf("he said \"hi\"")))
        assertEquals("\"line1\nline2\"", Csv.encodeRow(listOf("line1\nline2")))
    }

    @Test
    fun parseRoundTrip() {
        val rows = listOf(
            listOf("term", "meaning", "example"),
            listOf("go", "gitmek; hareket etmek", "go home"),
            listOf("say", "söylemek", "he said \"hi\""),
        )
        val text = Csv.encode(rows)
        assertEquals(rows, Csv.parse(text))
    }

    @Test
    fun parseSkipsEmptyRowsAndBom() {
        val text = "\uFEFFa;b\r\n\r\n;c\r\n"
        val rows = Csv.parse(text)
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b"), rows[0])
        assertEquals(listOf("", "c"), rows[1])
    }

    @Test
    fun parseUnterminatedQuoteKeepsContent() {
        val rows = Csv.parse("\"abc")
        assertEquals(listOf("abc"), rows.first())
    }

    @Test
    fun splitLineReturnsFirstRow() {
        assertEquals(listOf("x", "y"), Csv.splitLine("x;y"))
        assertEquals(emptyList<String>(), Csv.splitLine(""))
    }
}
