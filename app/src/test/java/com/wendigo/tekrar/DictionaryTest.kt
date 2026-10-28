package com.wendigo.tekrar

import com.wendigo.tekrar.core.Dictionary
import com.wendigo.tekrar.core.DictionaryEntry
import com.wendigo.tekrar.core.DictionaryFormatException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictionaryTest {

    @Test
    fun parsesArraySkipsInvalidAndDuplicateRows() {
        val raw = """
            [
              {
                "term": "abandon",
                "meaning": "terk etmek",
                "pos": "verb",
                "example": "They abandoned the plan.",
                "exampleTr": "Planı terk ettiler.",
                "cefr": "b2"
              },
              {"term": "  ABANDON ", "meaning": "tekrar"},
              {"term": "", "meaning": "boş terim"},
              {"term": "look after", "meaning": ""},
              "bozuk satır"
            ]
        """.trimIndent()

        val result = Dictionary.parse(raw)

        assertEquals(1, result.entries.size)
        assertEquals(3, result.skipped)
        assertEquals(1, result.duplicates)
        val entry = result.entries.first()
        assertEquals("abandon", entry.term)
        assertEquals("terk etmek", entry.meaning)
        assertEquals("verb", entry.pos)
        assertEquals("They abandoned the plan.", entry.example)
        assertEquals("Planı terk ettiler.", entry.exampleTr)
        assertEquals("B2", entry.cefr)
    }

    @Test
    fun acceptsEntriesObjectSchema() {
        val raw = """
            {"entries":[
              {"term":"give up","meaning":"vazgeçmek","pos":"phrasal verb","cefr":"B1"},
              {"term":"trial","meaning":"deneme","cefr":"Z9"}
            ]}
        """.trimIndent()

        val result = Dictionary.parse(raw)

        assertEquals(2, result.entries.size)
        assertEquals("", result.entries[1].cefr)
    }

    @Test
    fun rejectsInvalidJsonAndMissingEntries() {
        assertThrows(DictionaryFormatException::class.java) {
            Dictionary.parse("not json")
        }
        assertThrows(DictionaryFormatException::class.java) {
            Dictionary.parse("""{"words":[]}""")
        }
    }

    @Test
    fun mergeLetsUserOverrideBundledTerm() {
        val bundled = listOf(
            DictionaryEntry("abandon", "terk etmek", pos = "verb", cefr = "B2"),
            DictionaryEntry("give up", "vazgeçmek", pos = "phrasal verb", cefr = "B1"),
        )
        val user = listOf(
            DictionaryEntry("  ABANDON ", "bırakmak", pos = "verb", cefr = "B2"),
            DictionaryEntry("novel", "roman", pos = "noun", cefr = "B1"),
        )

        val merged = Dictionary.merge(bundled, user)
        val index = Dictionary.index(merged)

        assertEquals(3, merged.size)
        assertEquals("bırakmak", index.lookup("abandon")?.meaning)
        assertEquals("roman", index.lookup("Novel")?.meaning)
        assertEquals("vazgeçmek", index.lookup("GIVE   UP")?.meaning)
    }

    @Test
    fun lookupIsCaseAndSpaceInsensitiveAndSupportsMultiWord() {
        val index = Dictionary.index(
            listOf(
                DictionaryEntry("look after", "bakmak", pos = "phrasal verb"),
                DictionaryEntry("water", "su"),
            )
        )

        assertEquals("bakmak", index.lookup("  LOOK   AFTER ")?.meaning)
        assertEquals("su", index.lookup("Water")?.meaning)
        assertNull(index.lookup("missing"))
        assertNull(index.lookup(""))
        assertTrue(index.contains("look after"))
    }

    @Test
    fun entriesForSkipsMissingAndDuplicateRequests() {
        val index = Dictionary.index(
            listOf(
                DictionaryEntry("water", "su"),
                DictionaryEntry("give up", "vazgeçmek"),
            )
        )

        val entries = index.entriesFor(listOf("Water", "missing", "water", "give up"))

        assertEquals(listOf("water", "give up"), entries.map { it.term })
    }

    @Test
    fun bundledDictionaryIsValidAndLargeEnough() {
        val file = File("src/main/assets/dictionary/en_tr.json")
        assertTrue("Sözlük dosyası bulunamadı", file.isFile)
        val result = Dictionary.parse(file.readText(Charsets.UTF_8))

        assertTrue("Sözlük en az 800 kelime olmalı", result.entries.size >= 800)
        assertEquals(0, result.skipped)
        assertEquals(0, result.duplicates)

        val validCefr = setOf("", "A1", "A2", "B1", "B2", "C1")
        val normalized = result.entries.map { it.term.trim().lowercase() }
        assertEquals(normalized.size, normalized.toSet().size)
        assertTrue(result.entries.map { it.cefr }.toSet().containsAll(
            listOf("A1", "A2", "B1", "B2", "C1")
        ))
        for (entry in result.entries) {
            assertTrue("Boş terim", entry.term.isNotBlank())
            assertTrue("Boş anlam: ${entry.term}", entry.meaning.isNotBlank())
            assertTrue("Geçersiz CEFR: ${entry.term}", entry.cefr in validCefr)
            assertTrue("Geçersiz POS: ${entry.term}", entry.pos in POS_VALUES)
            assertNotNull(entry.meaning)
        }
    }

    private companion object {
        val POS_VALUES = setOf(
            "",
            "noun",
            "verb",
            "adjective",
            "adverb",
            "phrasal verb",
            "preposition",
            "conjunction",
            "pronoun",
        )
    }
}
