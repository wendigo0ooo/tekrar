package com.wendigo.tekrar.core

class MiniJsonException(message: String) : IllegalArgumentException(message)

/**
 * Küçük, bağımlılıksız JSON ayrıştırıcı. Yalnızca nesne, dizi, metin, sayı,
 * boolean ve null değerlerini destekler. Deste, sözlük ve yedek dosyalarında
 * kullanılan alt küme için yeterlidir.
 */
internal object MiniJson {

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
            throw MiniJsonException("geçersiz JSON ($reason)")
    }
}
