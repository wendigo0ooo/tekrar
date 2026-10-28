package com.wendigo.tekrar.core

import java.util.Locale

/**
 * Kelime tekilliği için normalleştirme. İngilizce terimler hedeflendiği için
 * [Locale.ROOT] kullanılır; Türkçe küçültme kuralı (I -> ı) uygulanmaz.
 */
object TermKey {

    private val whitespace = Regex("\\s+")

    fun normalize(term: String): String =
        term.trim().replace(whitespace, " ").lowercase(Locale.ROOT)

    fun isDuplicate(first: String, second: String): Boolean =
        normalize(first) == normalize(second)
}
