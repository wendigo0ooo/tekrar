package com.wendigo.tekrar.core.learn

import java.text.Normalizer
import java.util.Locale

/**
 * Cevap karşılaştırması için deterministik normalizasyon (kontrat 2.4).
 *
 * - NFC, `Locale.ROOT` küçük harf, baştaki/sondaki boşluk ve çoklu boşluk.
 * - Düz/akıllı apostrof ve tire karakterleri eşitlenir.
 * - [normalizeLoose] ek olarak cümle noktalamasını atar; yalnız `translate`
 *   ve `cloze` metin cevaplarında kullanılır. Kesme işareti ve tire korunur.
 *
 * Yazım toleransı yoktur; Levenshtein yalnız [SpeechMatcher] kelime
 * benzerliğinde kullanılır.
 */
object AnswerNormalizer {

    private val whitespace = Regex("\\s+")

    private val sentencePunctuation = setOf(
        '.', ',', '!', '?', ';', ':', '…', '¡', '¿',
        '"', '“', '”', '„', '«', '»', '(', ')', '[', ']', '{', '}',
    )

    /** Temel normalizasyon: noktalama korunur. */
    fun normalize(text: String): String {
        val unified = buildString(text.length) {
            for (char in text) {
                append(
                    when (char) {
                        '\u2018', '\u2019', '\u02BC', '\u00B4', '`' -> '\''
                        '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2212' -> '-'
                        else -> char
                    },
                )
            }
        }
        return Normalizer.normalize(unified, Normalizer.Form.NFC)
            .trim()
            .replace(whitespace, " ")
            .lowercase(Locale.ROOT)
    }

    /** Metin cevapları için noktalama esnetilmiş normalizasyon. */
    fun normalizeLoose(text: String): String {
        val base = normalize(text)
        val stripped = buildString(base.length) {
            for (char in base) {
                if (char !in sentencePunctuation) append(char)
            }
        }
        return stripped.trim().replace(whitespace, " ")
    }

    fun words(text: String): List<String> =
        normalizeLoose(text).split(' ').filter { it.isNotBlank() }

    fun wordCount(text: String): Int = text
        .split(whitespace)
        .count { token -> token.any { it.isLetterOrDigit() } }

    fun containsPhrase(haystack: List<String>, phrase: String): Boolean {
        val needle = words(phrase)
        if (needle.isEmpty()) return true
        if (needle.size > haystack.size) return false
        for (start in 0..haystack.size - needle.size) {
            if (haystack.subList(start, start + needle.size) == needle) return true
        }
        return false
    }
}
