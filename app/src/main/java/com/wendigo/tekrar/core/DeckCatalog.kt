package com.wendigo.tekrar.core

import java.util.Locale

data class DeckWord(
    val term: String,
    val meaning: String,
    val example: String = "",
    val exampleTr: String = "",
    val pos: String = "",
    val cefr: String = "",
)

data class DeckDefinition(
    val name: String,
    val description: String = "",
    val cefrRange: String = "",
    val words: List<DeckWord> = emptyList(),
)

class DeckFormatException(message: String) : IllegalArgumentException(message)

data class DeckParseResult(
    val deck: DeckDefinition,
    val skipped: Int = 0,
    val duplicates: Int = 0,
)

/**
 * Asset destelerini ayrıştıran saf yardımcı. Android/org.json bağımlılığı yoktur;
 * birim testlerinde doğrudan çalışır. Geçersiz satırlar (boş terim/anlam) atlanır,
 * aynı normalleştirilmiş terime sahip mükerrerler yalnızca ilk kez alınır.
 */
object DeckCatalog {

    val CEFR_LEVELS = listOf("A1", "A2", "B1", "B2", "C1")

    fun parseCefr(raw: String?): String {
        val value = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return if (value in CEFR_LEVELS) value else ""
    }

    fun parse(raw: String): DeckParseResult {
        val root = try {
            MiniJson.parse(raw.removePrefix("\uFEFF"))
        } catch (error: MiniJsonException) {
            throw DeckFormatException("Deste dosyası okunamadı: ${error.message}.")
        } catch (error: Exception) {
            throw DeckFormatException("Deste dosyası okunamadı: geçersiz JSON.")
        }
        val obj = root as? Map<*, *>
            ?: throw DeckFormatException("Deste dosyası bir nesne içermiyor.")
        @Suppress("UNCHECKED_CAST")
        val fields = obj as Map<String, Any?>
        val name = (fields["deck"] as? String)?.trim().orEmpty()
        if (name.isEmpty()) {
            throw DeckFormatException("Deste adı (deck) boş olamaz.")
        }
        val description = (fields["description"] as? String)?.trim().orEmpty()
        val cefrRange = (fields["cefr"] as? String)?.trim().orEmpty()
        val rawWords = fields["words"] as? List<*>
            ?: throw DeckFormatException("Deste dosyasında words listesi yok.")

        val seen = HashSet<String>()
        val words = ArrayList<DeckWord>(rawWords.size)
        var skipped = 0
        var duplicates = 0
        for (element in rawWords) {
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
            if (!seen.add(TermKey.normalize(term))) {
                duplicates++
                continue
            }
            words += DeckWord(
                term = term,
                meaning = meaning,
                example = (item["example"] as? String)?.trim().orEmpty(),
                exampleTr = (item["exampleTr"] as? String)?.trim().orEmpty(),
                pos = (item["pos"] as? String)?.trim().orEmpty(),
                cefr = parseCefr(item["cefr"] as? String),
            )
        }
        return DeckParseResult(
            deck = DeckDefinition(
                name = name,
                description = description,
                cefrRange = cefrRange,
                words = words,
            ),
            skipped = skipped,
            duplicates = duplicates,
        )
    }
}
