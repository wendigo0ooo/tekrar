package com.wendigo.tekrar.core.learn

import com.wendigo.tekrar.data.Word
import java.util.Locale

/**
 * Ders kelimelerini FSRS kelime tablosuna aktarır (kontrat 4).
 *
 * - Aynı `term` zaten varsa (TermKey) yeni kayıt açılmaz; mevcut kart korunur.
 * - Tekrar çalıştırma idempotenttir.
 * - `sourceKey` biçimi: `course:<lessonId>:<term-casefold>`.
 * - Bkz. `Word.sourceKey` unique index; kurs kelimesi kullanıcı kelimesinden
 *   bu anahtarla ayrılır.
 */
interface CourseWordSink {
    suspend fun findByTerm(term: String): Word?
    suspend fun insert(word: Word): Long

    /** Var olan kelimeye kurs etiketi ekler; desteklenmiyorsa sessizce geçer. */
    suspend fun addTag(word: Word, tag: String) {}
}

data class SeedResult(
    val inserted: Int,
    val skipped: Int,
)

object CourseSeeder {

    suspend fun seed(lesson: Lesson, sink: CourseWordSink): SeedResult {
        var inserted = 0
        var skipped = 0
        for (item in lesson.vocab) {
            val term = item.term.trim()
            if (term.isEmpty()) {
                skipped++
                continue
            }
            val existing = sink.findByTerm(term)
            if (existing != null) {
                sink.addTag(existing, "course")
                skipped++
                continue
            }
            val id = sink.insert(
                Word(
                    term = term,
                    meaning = item.meaningTr,
                    example = item.example,
                    exampleTr = item.exampleTr,
                    tags = "course,${lesson.unit}",
                    pos = item.pos,
                    deck = "course:${lesson.unit}",
                    cefr = lesson.cefr,
                    sourceKey = sourceKey(lesson.id, term),
                ),
            )
            if (id >= 0) inserted++ else skipped++
        }
        return SeedResult(inserted = inserted, skipped = skipped)
    }

    fun sourceKey(lessonId: String, term: String): String =
        "course:$lessonId:${term.trim().lowercase(Locale.ROOT)}"
}
