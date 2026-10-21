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
}
