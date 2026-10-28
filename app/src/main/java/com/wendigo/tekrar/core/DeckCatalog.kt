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
        } catch (error: DeckFormatException) {
            throw error
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

    private object MiniJson {

        fun parse(text: String): Any? {
            val reader = Reader(text)
            val value = reader.readValue()
            reader.skipWhitespace()
            if (!reader.atEnd()) reader.fail("fazladan içerik")
            return value
        }

        private class Reader(private val text: String) {

            private var position = 0

            fun atEnd(): Boolean = position >= text.length

            fun skipWhitespace() {
                while (position < text.length) {
                    when (text[position]) {
                        ' ', '\t', '\n', '\r' -> position++
                        else -> return
                    }
                }
            }

            fun readValue(): Any? {
                skipWhitespace()
                if (position >= text.length) fail("beklenmedik dosya sonu")
                return when (val char = text[position]) {
                    '{' -> readObject()
                    '[' -> readArray()
                    '"' -> readString()
                    't' -> {
                        expect("true")
                        true
                    }
                    'f' -> {
                        expect("false")
                        false
                    }
                    'n' -> {
                        expect("null")
                        null
                    }
                    '-', in '0'..'9' -> readNumber()
                    else -> fail("beklenmeyen karakter '$char'")
                }
            }

            private fun readObject(): Map<String, Any?> {
                position++
                val values = LinkedHashMap<String, Any?>()
                skipWhitespace()
                if (peek() == '}') {
                    position++
                    return values
                }
                while (true) {
                    skipWhitespace()
                    if (peek() != '"') fail("nesne anahtarı metin olmalı")
                    val key = readString()
                    skipWhitespace()
                    if (peek() != ':') fail("':' bekleniyordu")
                    position++
                    values[key] = readValue()
                    skipWhitespace()
                    when (peek()) {
                        ',' -> position++
                        '}' -> {
                            position++
                            return values
                        }
                        else -> fail("',' veya '}' bekleniyordu")
                    }
                }
            }

            private fun readArray(): List<Any?> {
                position++
                val values = ArrayList<Any?>()
                skipWhitespace()
                if (peek() == ']') {
                    position++
                    return values
                }
                while (true) {
                    values += readValue()
                    skipWhitespace()
                    when (peek()) {
                        ',' -> position++
                        ']' -> {
                            position++
                            return values
                        }
                        else -> fail("',' veya ']' bekleniyordu")
                    }
                }
            }

            private fun readString(): String {
                position++
                val builder = StringBuilder()
                while (true) {
                    if (position >= text.length) fail("metin sonlanmadı")
                    when (val char = text[position++]) {
                        '"' -> return builder.toString()
                        '\\' -> {
                            if (position >= text.length) fail("kaçış dizisi eksik")
                            when (val escaped = text[position++]) {
                                '"' -> builder.append('"')
                                '\\' -> builder.append('\\')
                                '/' -> builder.append('/')
                                'b' -> builder.append('\b')
                                'f' -> builder.append('\u000C')
                                'n' -> builder.append('\n')
                                'r' -> builder.append('\r')
                                't' -> builder.append('\t')
                                'u' -> {
                                    if (position + 4 > text.length) fail("eksik unicode kaçışı")
                                    val hex = text.substring(position, position + 4)
                                    val code = hex.toIntOrNull(16)
                                        ?: fail("geçersiz unicode kaçışı")
                                    builder.append(code.toChar())
                                    position += 4
                                }
                                else -> fail("bilinmeyen kaçış dizisi '\\$escaped'")
                            }
                        }
                        else -> {
                            if (char < ' ') fail("metinde kontrol karakteri")
                            builder.append(char)
                        }
                    }
                }
            }

            private fun readNumber(): Double {
                val start = position
                if (peek() == '-') position++
                while (position < text.length && text[position] in '0'..'9') position++
                if (position < text.length && text[position] == '.') {
                    position++
                    while (position < text.length && text[position] in '0'..'9') position++
                }
                if (position < text.length && (text[position] == 'e' || text[position] == 'E')) {
                    position++
                    if (position < text.length &&
                        (text[position] == '+' || text[position] == '-')
                    ) {
                        position++
                    }
                    while (position < text.length && text[position] in '0'..'9') position++
                }
                val raw = text.substring(start, position)
                return raw.toDoubleOrNull() ?: fail("geçersiz sayı")
            }

            private fun expect(literal: String) {
                if (!text.startsWith(literal, position)) fail("'$literal' bekleniyordu")
                position += literal.length
            }

            private fun peek(): Char {
                if (position >= text.length) fail("beklenmedik dosya sonu")
                return text[position]
            }

            fun fail(reason: String): Nothing =
                throw DeckFormatException("Deste dosyası okunamadı: geçersiz JSON ($reason).")
        }
    }
}
