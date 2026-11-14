package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.Choice
import com.wendigo.tekrar.core.learn.CourseSeeder
import com.wendigo.tekrar.core.learn.CourseWordSink
import com.wendigo.tekrar.core.learn.Lesson
import com.wendigo.tekrar.core.learn.VocabItem
import com.wendigo.tekrar.data.Word
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseSeederTest {

    private class FakeSink(initial: List<Word> = emptyList()) : CourseWordSink {
        val stored = initial.toMutableList()

        override suspend fun findByTerm(term: String): Word? =
            stored.firstOrNull { it.term.equals(term, ignoreCase = true) }

        override suspend fun insert(word: Word): Long {
            stored += word
            return stored.size.toLong()
        }
    }

    private fun lesson(vararg vocab: VocabItem) = Lesson(
        schema = 1,
        id = "a1-u01-l01",
        unit = "a1-u01",
        cefr = "A1",
        title = "Merhaba",
        objectiveTr = "Selamlaş.",
        estMin = 4,
        vocab = vocab.toList(),
        teach = emptyList(),
        exercises = listOf(Choice("Soru", listOf("a", "b"), 0)),
    )

    private fun hello() = VocabItem(
        term = "hello",
        meaningTr = "merhaba",
        pos = "ünlem",
        example = "Hello!",
        exampleTr = "Merhaba!",
    )

    @Test
    fun seedsCourseMetadata() = runBlocking {
        val sink = FakeSink()
        val result = CourseSeeder.seed(lesson(hello(), VocabItem("name", "isim", "isim", "My name is Ben.", "Benim adım Ben.")), sink)

        assertEquals(2, result.inserted)
        assertEquals(0, result.skipped)
        assertEquals(2, sink.stored.size)

        val word = sink.stored[0]
        assertEquals("hello", word.term)
        assertEquals("merhaba", word.meaning)
        assertEquals("ünlem", word.pos)
        assertEquals("Hello!", word.example)
        assertEquals("Merhaba!", word.exampleTr)
        assertEquals("course:a1-u01", word.deck)
        assertEquals("course,a1-u01", word.tags)
        assertEquals("A1", word.cefr)
        assertEquals("course:a1-u01-l01:hello", word.sourceKey)
        assertEquals("", word.notes)
        assertTrue(word.isNew)
    }

    @Test
    fun isIdempotentOnSecondRun() = runBlocking {
        val sink = FakeSink()
        val first = CourseSeeder.seed(lesson(hello()), sink)
        val second = CourseSeeder.seed(lesson(hello()), sink)

        assertEquals(1, first.inserted)
        assertEquals(0, second.inserted)
        assertEquals(1, second.skipped)
        assertEquals(1, sink.stored.size)
    }

    @Test
    fun skipsExistingUserWordAndKeepsItUntouched() = runBlocking {
        val existing = Word(term = "Hello", meaning = "kullanıcı anlamı", tags = "kendi")
        val sink = FakeSink(listOf(existing))

        val result = CourseSeeder.seed(
            lesson(hello(), VocabItem("bye", "bay bay", "ünlem", "Bye!", "Bay bay!")),
            sink,
        )

        assertEquals(1, result.inserted)
        assertEquals(1, result.skipped)
        assertEquals(2, sink.stored.size)
        val untouched = sink.stored.first { it.term.equals("hello", ignoreCase = true) }
        assertEquals("kullanıcı anlamı", untouched.meaning)
        assertEquals("kendi", untouched.tags)
        assertEquals(null, untouched.sourceKey)
    }

    @Test
    fun skipsCaseInsensitiveDuplicateWithinLesson() = runBlocking {
        val sink = FakeSink()
        val result = CourseSeeder.seed(
            lesson(hello(), hello().copy(term = "HELLO", meaningTr = "selam")),
            sink,
        )
        assertEquals(1, result.inserted)
        assertEquals(1, result.skipped)
        assertEquals(listOf("hello"), sink.stored.map { it.term })
    }

    @Test
    fun sourceKeyIsCasefoldedAndTrimmed() {
        assertEquals("course:a1-u01-l01:hello", CourseSeeder.sourceKey("a1-u01-l01", "Hello"))
        assertEquals("course:a1-u01-l01:hello", CourseSeeder.sourceKey("a1-u01-l01", "  HELLO  "))
        assertEquals(
            "course:a1-u01-l01:nice-to-meet-you",
            CourseSeeder.sourceKey("a1-u01-l01", "Nice to meet you".replace(' ', '-')),
        )
    }

    @Test
    fun differentLessonsProduceDifferentSourceKeys() = runBlocking {
        val first = FakeSink()
        CourseSeeder.seed(lesson(hello()), first)
        val second = FakeSink()
        CourseSeeder.seed(lesson(hello()).copy(id = "a1-u01-l02"), second)

        assertEquals("course:a1-u01-l01:hello", first.stored.single().sourceKey)
        assertEquals("course:a1-u01-l02:hello", second.stored.single().sourceKey)
    }
}
