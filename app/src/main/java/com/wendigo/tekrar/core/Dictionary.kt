package com.wendigo.tekrar.core

import java.util.Locale

data class DictionaryEntry(
    val term: String,
    val meaning: String,
    val pos: String = "",
    val example: String = "",
    val exampleTr: String = "",
    val cefr: String = "",
)

class DictionaryFormatException(message: String) : IllegalArgumentException(message)

data class DictionaryParseResult(
    val entries: List<DictionaryEntry>,
    val skipped: Int = 0,
    val duplicates: Int = 0,
)

/**
 * Dosya tabanlı çevrimdışı sözlük ayrıştırıcısı. Android bağımlılığı yoktur.
 * Üst düzey bir dizi ya da `{"entries": [...]}` nesnesi kabul edilir. Geçersiz
 * satırlar atlanır; aynı [TermKey] anahtarına sahip mükerrerlerde ilk kayıt tutulur.
 */
object Dictionary {

    private val CEFR_LEVELS = setOf("A1", "A2", "B1", "B2", "C1")

    fun parse(raw: String): DictionaryParseResult {
        val root = try {
            MiniJson.parse(raw.removePrefix("\uFEFF"))
        } catch (error: MiniJsonException) {
            throw DictionaryFormatException("Sözlük dosyası okunamadı: ${error.message}.")
        } catch (error: Exception) {
            throw DictionaryFormatException("Sözlük dosyası okunamadı: geçersiz JSON.")
        }
        val rawEntries = when (root) {
            is List<*> -> root
            is Map<*, *> -> root["entries"] as? List<*>
                ?: throw DictionaryFormatException("Sözlük dosyasında entries listesi yok.")
            else -> throw DictionaryFormatException("Sözlük dosyası bir dizi içermiyor.")
        }

        val seen = HashSet<String>()
        val entries = ArrayList<DictionaryEntry>(rawEntries.size)
        var skipped = 0
        var duplicates = 0
        for (element in rawEntries) {
            @Suppress("UNCHECKED_CAST")
            val item = element as? Map<String, Any?>
            if (item == null) {
                skipped++
                continue
            }
            val term = (item["term"] as? String)?.trim().orEmpty()
            val meaning = (item["meaning"] as? String)?.trim().orEmpty()
            if (term.isEmpty() || meaning.isEmpty()) {
                skipped++
                continue
            }
            val key = TermKey.normalize(term)
            if (key.isEmpty() || !seen.add(key)) {
                duplicates++
                continue
            }
            entries += DictionaryEntry(
                term = term,
                meaning = meaning,
                pos = text(item, "pos"),
                example = text(item, "example"),
                exampleTr = text(item, "exampleTr"),
                cefr = parseCefr(item["cefr"] as? String),
            )
        }
        return DictionaryParseResult(entries, skipped, duplicates)
    }

    /**
     * Paket sözlüğü ile kullanıcı sözlüğünü birleştirir. Aynı normalleştirilmiş
     * terimde kullanıcı kaydı paket kaydını geçersiz kılar. Sıra korunur.
     */
    fun merge(
        bundled: List<DictionaryEntry>,
        user: List<DictionaryEntry>,
    ): List<DictionaryEntry> {
        val merged = LinkedHashMap<String, DictionaryEntry>()
        for (entry in bundled) {
            val key = TermKey.normalize(entry.term)
            if (key.isNotEmpty() && key !in merged) merged[key] = entry
        }
        for (entry in user) {
            val key = TermKey.normalize(entry.term)
            if (key.isNotEmpty()) merged[key] = entry
        }
        return merged.values.toList()
    }

    fun index(entries: List<DictionaryEntry>): DictionaryIndex {
        val byKey = LinkedHashMap<String, DictionaryEntry>()
        for (entry in entries) {
            val key = TermKey.normalize(entry.term)
            if (key.isNotEmpty()) byKey[key] = entry
        }
        return DictionaryIndex(byKey)
    }

    fun lookup(entries: List<DictionaryEntry>, term: String): DictionaryEntry? =
        index(entries).lookup(term)

    fun entriesFor(
        entries: List<DictionaryEntry>,
        words: Iterable<String>,
    ): List<DictionaryEntry> = index(entries).entriesFor(words)

    private fun text(item: Map<String, Any?>, key: String): String =
        (item[key] as? String)?.trim().orEmpty()

    private fun parseCefr(raw: String?): String {
        val value = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return if (value in CEFR_LEVELS) value else ""
    }
}

class DictionaryIndex internal constructor(
    private val byKey: Map<String, DictionaryEntry>,
) {

    val size: Int get() = byKey.size

    fun lookup(term: String): DictionaryEntry? = byKey[TermKey.normalize(term)]

    /**
     * Verilen terimler için sırayla eşleşen kayıtları döndürür. Aynı normalize
     * edilmiş terim birden fazla kez istenirse yalnızca ilk kez eklenir.
     */
    fun entriesFor(words: Iterable<String>): List<DictionaryEntry> {
        val seen = HashSet<String>()
        val result = ArrayList<DictionaryEntry>()
        for (word in words) {
            val key = TermKey.normalize(word)
            if (key.isEmpty() || !seen.add(key)) continue
            byKey[key]?.let { result += it }
        }
        return result
    }

    fun contains(term: String): Boolean = lookup(term) != null
}
