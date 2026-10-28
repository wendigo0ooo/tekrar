package com.wendigo.tekrar

import com.wendigo.tekrar.core.DeckCatalog
import com.wendigo.tekrar.core.DeckFormatException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeckCatalogTest {

    @Test
    fun parsesDeckMetadataAndWords() {
        val raw = """
            {
              "deck": "Deneme",
              "description": "Test destesi",
              "cefr": "A2-B1",
              "words": [
                {
                  "term": "abandon",
                  "meaning": "terk etmek",
                  "example": "They abandoned the plan.",
                  "exampleTr": "Planı terk ettiler.",
                  "pos": "verb",
                  "cefr": "b2"
                }
              ]
            }
        """.trimIndent()

        val result = DeckCatalog.parse(raw)

        assertEquals("Deneme", result.deck.name)
        assertEquals("Test destesi", result.deck.description)
        assertEquals("A2-B1", result.deck.cefrRange)
        assertEquals(1, result.deck.words.size)
        val word = result.deck.words.first()
        assertEquals("abandon", word.term)
        assertEquals("terk etmek", word.meaning)
        assertEquals("They abandoned the plan.", word.example)
        assertEquals("Planı terk ettiler.", word.exampleTr)
        assertEquals("verb", word.pos)
        assertEquals("B2", word.cefr)
        assertEquals(0, result.skipped)
        assertEquals(0, result.duplicates)
    }

    @Test
    fun skipsInvalidRowsAndDuplicates() {
        val raw = """
            {
              "deck": "Deneme",
              "words": [
                {"term": "give up", "meaning": "vazgeçmek"},
                {"term": "  GIVE   UP ", "meaning": "tekrar"},
                {"term": "", "meaning": "boş terim"},
                {"term": "look after", "meaning": ""},
                {"term": "look after", "meaning": "bakmak"}
              ]
            }
        """.trimIndent()

        val result = DeckCatalog.parse(raw)

        assertEquals(2, result.deck.words.size)
        assertEquals(listOf("give up", "look after"), result.deck.words.map { it.term })
        assertEquals(2, result.skipped)
        assertEquals(1, result.duplicates)
    }

    @Test
    fun unknownCefrBecomesEmpty() {
        val raw = """
            {"deck":"Deneme","words":[
              {"term":"test","meaning":"deneme","cefr":"Z9"},
              {"term":"trial","meaning":"deneme","cefr":"C1"}
            ]}
        """.trimIndent()

        val result = DeckCatalog.parse(raw)

        assertEquals("", result.deck.words[0].cefr)
        assertEquals("C1", result.deck.words[1].cefr)
    }

    @Test
    fun rejectsMissingDeckName() {
        val raw = """{"words":[{"term":"a","meaning":"b"}]}"""
        assertThrows(DeckFormatException::class.java) { DeckCatalog.parse(raw) }
    }

    @Test
    fun bundledDecksAreValidAndMeetMinimumCounts() {
        val dir = File("src/main/assets/decks")
        assertTrue("decks klasörü bulunamadı", dir.isDirectory)
        val yds = DeckCatalog.parse(File(dir, "yds.json").readText())
        val phrasal = DeckCatalog.parse(File(dir, "phrasal.json").readText())

        assertTrue("YDS destesi 100+ kelime olmalı", yds.deck.words.size >= 100)
        assertTrue("Phrasal destesi 60+ kelime olmalı", phrasal.deck.words.size >= 60)
        assertEquals(0, yds.skipped)
        assertEquals(0, yds.duplicates)
        assertEquals(0, phrasal.skipped)
        assertEquals(0, phrasal.duplicates)

        for (deck in listOf(yds.deck, phrasal.deck)) {
            assertTrue(deck.name.isNotBlank())
            val normalized = deck.words.map { it.term.trim().lowercase() }
            assertEquals(normalized.size, normalized.toSet().size)
            for (word in deck.words) {
                assertTrue("Boş terim: ${deck.name}", word.term.isNotBlank())
                assertTrue("Boş anlam: ${word.term}", word.meaning.isNotBlank())
                assertTrue(
                    "Geçersiz CEFR: ${word.term} -> ${word.cefr}",
                    word.cefr in listOf("", "A1", "A2", "B1", "B2", "C1"),
                )
            }
        }
    }
}
