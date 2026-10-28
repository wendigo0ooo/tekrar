package com.wendigo.tekrar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedWordsTest {

    private val seedFile = File("src/main/assets/seed_words.json")

    @Test
    fun everySeedWordHasTurkishExampleTranslation() {
        assertTrue("seed_words.json bulunamadı", seedFile.exists())
        val text = seedFile.readText()
        val terms = Regex("\"term\"\\s*:\\s*\"").findAll(text).count()
        val translations = Regex("\"exampleTr\"\\s*:\\s*\"[^\"]+\"").findAll(text).count()
        assertEquals("Her kelime için exampleTr olmalı", terms, translations)
        assertTrue("Seed listesi boş olmamalı", terms > 0)
    }

    @Test
    fun everySeedWordHasStartDeckAndValidCefr() {
        val text = seedFile.readText()
        val terms = Regex("\"term\"\\s*:\\s*\"").findAll(text).count()
        val decks = Regex("\"deck\"\\s*:\\s*\"Başlangıç\"").findAll(text).count()
        val levels = Regex("\"cefr\"\\s*:\\s*\"(A1|A2|B1|B2|C1)\"").findAll(text).count()
        assertEquals("Her seed kelimesi Başlangıç destesinde olmalı", terms, decks)
        assertEquals("Her seed kelimesinin geçerli CEFR değeri olmalı", terms, levels)
    }
}
