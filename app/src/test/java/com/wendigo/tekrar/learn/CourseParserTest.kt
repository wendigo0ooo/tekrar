package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.Choice
import com.wendigo.tekrar.core.learn.Cloze
import com.wendigo.tekrar.core.learn.CourseParseException
import com.wendigo.tekrar.core.learn.CourseParser
import com.wendigo.tekrar.core.learn.DialogueChoice
import com.wendigo.tekrar.core.learn.ListenChoice
import com.wendigo.tekrar.core.learn.Match
import com.wendigo.tekrar.core.learn.Speak
import com.wendigo.tekrar.core.learn.Translate
import com.wendigo.tekrar.core.learn.WordOrder
import com.wendigo.tekrar.core.learn.Write
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseParserTest {

    @Test
    fun parsesLessonWithAllExerciseTypes() {
        val lesson = CourseParser.parseLesson(LESSON_JSON, "l01.json")

        assertEquals(1, lesson.schema)
        assertEquals("a1-u01-l01", lesson.id)
        assertEquals("a1-u01", lesson.unit)
        assertEquals("A1", lesson.cefr)
        assertEquals("Merhaba!", lesson.title)
        assertEquals(4, lesson.estMin)
        assertEquals(2, lesson.vocab.size)
        assertEquals("hello", lesson.vocab[0].term)
        assertEquals("merhaba", lesson.vocab[0].meaningTr)
        assertEquals("neɪm", lesson.vocab[1].ipa)
        assertNull(lesson.vocab[0].ipa)

        assertEquals(1, lesson.grammar.size)
        assertEquals("Ben Anna.", lesson.grammar[0].examples.single().tr)
        assertEquals(1, lesson.teach.size)
        assertEquals("Daha samimi.", lesson.teach[0].tipTr)

        val dialogue = lesson.dialogue
        assertNotNull(dialogue)
        assertEquals(2, dialogue!!.lines.size)
        assertEquals("Anna", dialogue.lines[0].speaker)

        assertEquals(10, lesson.exercises.size)
        val choice = lesson.exercises[0] as Choice
        assertEquals(listOf("merhaba", "hoşça kal", "teşekkürler"), choice.options)
        assertEquals(0, choice.answer)
        assertEquals("'Hello' merhaba demektir.", choice.explanationTr)

        val listen = lesson.exercises[1] as ListenChoice
        assertEquals("Hello, my name is Anna.", listen.audioText)
        assertEquals(1, listen.answer)

        val dialogueChoice = lesson.exercises[2] as DialogueChoice
        assertNull(dialogueChoice.audioText)
        assertEquals(0, dialogueChoice.answer)

        val match = lesson.exercises[3] as Match
        assertEquals(2, match.pairs.size)
        assertEquals("hello", match.pairs[0].left)
        assertEquals("merhaba", match.pairs[0].right)

        val order = lesson.exercises[4] as WordOrder
        assertEquals(listOf("My", "name", "is", "Anna"), order.answer)
        assertEquals("Benim adım Anna.", order.translationTr)

        val cloze = lesson.exercises[5] as Cloze
        assertEquals(0, cloze.answer)
        assertEquals(listOf("name", "bye", "hi"), cloze.options)
        assertNull(cloze.answers)

        val typedCloze = lesson.exercises[6] as Cloze
        assertNull(typedCloze.options)
        assertEquals(listOf("name"), typedCloze.answers)

        val translate = lesson.exercises[7] as Translate
        assertEquals(2, translate.answers.size)
        assertEquals("'my name is' kalıbını kullan.", translate.hintTr)

        val speak = lesson.exercises[8] as Speak
        assertEquals("Hello, my name is Anna.", speak.target)

        val write = lesson.exercises[9] as Write
        assertEquals(5, write.minWords)
        assertEquals(listOf("hello", "name"), write.keywords)
        assertEquals("Hello! My name is ...", write.sample)
    }

    @Test
    fun parsesIndex() {
        val index = CourseParser.parseIndex(
            """
            {
              "schema": 1,
              "language": "en",
              "native": "tr",
              "levels": [
                {
                  "id": "a1",
                  "title": "A1 · Başlangıç",
                  "units": [
                    { "id": "a1-u01", "file": "a1/u01/unit.json" }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals("en", index.language)
        assertEquals("tr", index.native)
        assertEquals(1, index.levels.size)
        assertEquals("a1-u01", index.levels[0].units[0].id)
        assertEquals("a1/u01/unit.json", index.levels[0].units[0].file)
    }

    @Test
    fun parsesUnitAndRoleplayReference() {
        val unit = CourseParser.parseUnit(
            """
            {
              "schema": 1,
              "id": "a1-u01",
              "cefr": "A1",
              "title": "Selamlaşma",
              "subtitle": "Merhaba, nasılsın?",
              "icon": "wave",
              "color": "#4F8EF7",
              "lessons": [
                { "id": "a1-u01-l01", "title": "Merhaba", "file": "l01.json", "est_min": 4 }
              ],
              "roleplay": { "id": "a1-u01-rp", "title": "İlk tanışma", "file": "rp01.json", "est_min": 5 }
            }
            """.trimIndent(),
        )
        assertEquals("A1", unit.cefr)
        assertEquals("wave", unit.icon)
        assertEquals(1, unit.lessons.size)
        assertEquals(4, unit.lessons[0].estMin)
        assertEquals("rp01.json", unit.roleplay!!.file)
    }

    @Test
    fun parsesRoleplayScenario() {
        val scenario = CourseParser.parseRoleplay(
            """
            {
              "schema": 1,
              "id": "a1-u01-rp",
              "unit": "a1-u01",
              "cefr": "A1",
              "title": "İlk tanışma",
              "setting_tr": "Partide yeni biriyle tanışıyorsun.",
              "role_en": "Sam, a friendly new acquaintance.",
              "goal_tr": "Selamlaş ve adını söyle.",
              "opening_en": "Hi! I'm Sam.",
              "target_phrases": ["Hi", "My name is ..."],
              "suggested_turns": 6,
              "success_criteria_tr": ["Selamlaştın", "Adını söyledin"]
            }
            """.trimIndent(),
        )
        assertEquals("a1-u01-rp", scenario.id)
        assertEquals(2, scenario.targetPhrases.size)
        assertEquals(6, scenario.suggestedTurns)
        assertEquals(2, scenario.successCriteriaTr.size)
    }

    @Test
    fun rejectsMissingRequiredExerciseField() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """
                    { "type": "choice", "options": ["a", "b"], "answer": 0 }
                    """.trimIndent(),
                ),
            )
        }
        assertTrue(error.message!!.contains("prompt_tr"))
    }

    @Test
    fun rejectsMissingTeachField() {
        val broken = lessonWith(
            """{ "type": "choice", "prompt_tr": "Soru", "options": ["a", "b"], "answer": 0 }""",
        ).replace("\"teach\": [],", "")
        val error = failure { CourseParser.parseLesson(broken) }
        assertTrue(error.message!!.contains("teach"))
    }

    @Test
    fun rejectsOutOfRangeAnswerIndex() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """{ "type": "choice", "prompt_tr": "Soru", "options": ["a", "b"], "answer": 9 }""",
                ),
            )
        }
        assertTrue(error.message!!.contains("answer"))
    }

    @Test
    fun rejectsClozeWithoutExactlyOneBlank() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """{ "type": "cloze", "prompt_tr": "Doldur", "sentence": "My name is Ben.", "answers": ["name"] }""",
                ),
            )
        }
        assertTrue(error.message!!.contains("___"))
    }

    @Test
    fun rejectsClozeWithBothOptionsAndAnswers() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """
                    { "type": "cloze", "prompt_tr": "Doldur", "sentence": "My ___ is Ben.",
                      "options": ["name", "bye"], "answer": 0, "answers": ["name"] }
                    """.trimIndent(),
                ),
            )
        }
        assertTrue(error.message!!.contains("options") || error.message!!.contains("answers"))
    }

    @Test
    fun rejectsUnknownExerciseType() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """{ "type": "typing_race", "prompt_tr": "Soru", "options": ["a", "b"], "answer": 0 }""",
                ),
            )
        }
        assertTrue(error.message!!.contains("typing_race"))
    }

    @Test
    fun rejectsWordOrderWithDifferentMultiset() {
        val error = failure {
            CourseParser.parseLesson(
                lessonWith(
                    """
                    { "type": "word_order", "prompt_tr": "Sıraya koy",
                      "tokens": ["My", "name", "is", "Anna"], "answer": ["My", "name", "is", "Ben"] }
                    """.trimIndent(),
                ),
            )
        }
        assertTrue(error.message!!.contains("tokens"))
    }

    @Test
    fun rejectsMalformedJson() {
        val error = failure { CourseParser.parseLesson("{ bozuk") }
        assertTrue(error.message!!.contains("geçersiz JSON"))
    }

    @Test
    fun rejectsNonObjectRoot() {
        val error = failure { CourseParser.parseLesson("[]") }
        assertTrue(error.message!!.contains("nesne"))
    }

    private fun failure(block: () -> Unit): CourseParseException {
        try {
            block()
        } catch (error: CourseParseException) {
            return error
        }
        throw AssertionError("CourseParseException bekleniyordu")
    }

    private fun lessonWith(exercisesJson: String): String = """
        {
          "schema": 1,
          "id": "a1-u01-l01",
          "unit": "a1-u01",
          "cefr": "A1",
          "title": "Test",
          "objective_tr": "Test dersi.",
          "est_min": 4,
          "vocab": [
            { "term": "hello", "meaning_tr": "merhaba", "pos": "ünlem", "example": "Hello!", "example_tr": "Merhaba!" }
          ],
          "teach": [],
          "exercises": [$exercisesJson]
        }
    """.trimIndent()

    private companion object {
        val LESSON_JSON = """
        {
          "schema": 1,
          "id": "a1-u01-l01",
          "unit": "a1-u01",
          "cefr": "A1",
          "title": "Merhaba!",
          "objective_tr": "Selamlaşma ve vedalaşma.",
          "est_min": 4,
          "vocab": [
            { "term": "hello", "meaning_tr": "merhaba", "pos": "ünlem", "example": "Hello!", "example_tr": "Merhaba!" },
            { "term": "name", "meaning_tr": "isim", "pos": "isim", "example": "My name is Ben.", "example_tr": "Benim adım Ben.", "ipa": "neɪm" }
          ],
          "grammar": [
            {
              "title_tr": "I am",
              "explanation_tr": "Kendini tanıtırken kullanılır.",
              "examples": [{ "en": "I am Anna.", "tr": "Ben Anna." }]
            }
          ],
          "dialogue": {
            "title": "Tanışma",
            "lines": [
              { "speaker": "Anna", "en": "Hi!", "tr": "Merhaba!" },
              { "speaker": "Ben", "en": "Hello!", "tr": "Merhaba!" }
            ]
          },
          "teach": [
            {
              "title_tr": "Selamlaşma",
              "body_tr": "Hello veya Hi denir.",
              "examples": [{ "en": "Hello!", "tr": "Merhaba!" }],
              "tip_tr": "Daha samimi."
            }
          ],
          "exercises": [
            {
              "type": "choice",
              "prompt_tr": "'hello' ne demek?",
              "options": ["merhaba", "hoşça kal", "teşekkürler"],
              "answer": 0,
              "explanation_tr": "'Hello' merhaba demektir."
            },
            {
              "type": "listen_choice",
              "prompt_tr": "Dinle ve seç.",
              "audio_text": "Hello, my name is Anna.",
              "options": ["Merhaba, benim adım Anna.", "Hoşça kal."],
              "answer": 1
            },
            {
              "type": "dialogue_choice",
              "prompt_tr": "Anna kendini nasıl tanıtıyor?",
              "options": ["Merhaba! Ben Anna.", "Hoşça kal, Anna."],
              "answer": 0
            },
            {
              "type": "match",
              "prompt_tr": "Eşleştir.",
              "pairs": [
                { "left": "hello", "right": "merhaba" },
                { "left": "hi", "right": "selam" }
              ]
            },
            {
              "type": "word_order",
              "prompt_tr": "Sıraya koy.",
              "tokens": ["Anna", "name", "My", "is"],
              "answer": ["My", "name", "is", "Anna"],
              "translation_tr": "Benim adım Anna."
            },
            {
              "type": "cloze",
              "prompt_tr": "Boşluğu doldur.",
              "sentence": "Hello, my ___ is Ben.",
              "options": ["name", "bye", "hi"],
              "answer": 0,
              "explanation_tr": "My name is kalıbı."
            },
            {
              "type": "cloze",
              "prompt_tr": "Boşluğu yaz.",
              "sentence": "My ___ is Ben.",
              "answers": ["name"]
            },
            {
              "type": "translate",
              "prompt_tr": "Çevir.",
              "answers": ["Hello, my name is Anna.", "Hi, my name is Anna."],
              "hint_tr": "'my name is' kalıbını kullan."
            },
            {
              "type": "speak",
              "prompt_tr": "Sesli söyle.",
              "target": "Hello, my name is Anna.",
              "hint_tr": "Yavaş dene."
            },
            {
              "type": "write",
              "prompt_tr": "Kendini tanıt.",
              "min_words": 5,
              "keywords": ["hello", "name"],
              "sample": "Hello! My name is ...",
              "rubric_tr": "Selamlaşma yeterli."
            }
          ]
        }
        """.trimIndent()
    }
}
