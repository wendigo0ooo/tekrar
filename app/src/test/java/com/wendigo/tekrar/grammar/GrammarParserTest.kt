package com.wendigo.tekrar.grammar

import com.wendigo.tekrar.core.grammar.GrammarExamplesStep
import com.wendigo.tekrar.core.grammar.GrammarHookStep
import com.wendigo.tekrar.core.grammar.GrammarMistakeStep
import com.wendigo.tekrar.core.grammar.GrammarParseException
import com.wendigo.tekrar.core.grammar.GrammarParser
import com.wendigo.tekrar.core.grammar.GrammarRuleStep
import com.wendigo.tekrar.core.grammar.GrammarTipStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarParserTest {

    @Test
    fun parsesIndexWithUnknownIconFallingBackToBook() {
        val index = GrammarParser.parseIndex(
            """
            {
              "schema": 1,
              "language": "en",
              "native": "tr",
              "topics": [
                {
                  "id": "g-a1-to-be",
                  "title_tr": "am / is / are",
                  "subtitle_tr": "Kendini tanıt",
                  "cefr": "A1",
                  "file": "a1/g01-to-be.json",
                  "icon": "rocket",
                  "color": "#4F8EF7",
                  "est_min": 4,
                  "linked_lessons": ["a1-u01-l01"]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, index.schema)
        assertEquals("en", index.language)
        assertEquals(1, index.topics.size)
        val topic = index.topics.single()
        assertEquals("g-a1-to-be", topic.id)
        assertEquals("a1/g01-to-be.json", topic.file)
        assertEquals("book", topic.icon)
        assertEquals("#4F8EF7", topic.color)
        assertEquals(listOf("a1-u01-l01"), topic.linkedLessons)
    }

    @Test
    fun rejectsDuplicateTopicIds() {
        val json = """
            {
              "schema": 1,
              "language": "en",
              "native": "tr",
              "topics": [
                {"id": "g1", "title_tr": "A", "subtitle_tr": "A", "cefr": "A1",
                 "file": "a1/a.json", "est_min": 3},
                {"id": "g1", "title_tr": "B", "subtitle_tr": "B", "cefr": "A1",
                 "file": "a1/b.json", "est_min": 3}
              ]
            }
        """.trimIndent()

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseIndex(json)
        }
    }

    @Test
    fun rejectsInvalidColor() {
        val json = """
            {
              "schema": 1,
              "language": "en",
              "native": "tr",
              "topics": [
                {"id": "g1", "title_tr": "A", "subtitle_tr": "A", "cefr": "A1",
                 "file": "a1/a.json", "color": "#12345", "est_min": 3}
              ]
            }
        """.trimIndent()

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseIndex(json)
        }
    }

    @Test
    fun parsesAllCardTypesAndWriteTasks() {
        val topic = GrammarParser.parseTopic(topicJson())

        assertEquals("g-a1-to-be", topic.id)
        assertEquals(5, topic.steps.size)
        assertTrue(topic.steps[0] is GrammarHookStep)
        assertTrue(topic.steps[1] is GrammarRuleStep)
        assertTrue(topic.steps[2] is GrammarExamplesStep)
        assertTrue(topic.steps[3] is GrammarMistakeStep)
        assertTrue(topic.steps[4] is GrammarTipStep)

        val hook = topic.steps[0] as GrammarHookStep
        assertEquals("I am Ali.", hook.examples.single().en)

        val rule = topic.steps[1] as GrammarRuleStep
        assertEquals("I + am", rule.pattern)
        assertEquals(1, rule.examples.size)

        val examples = topic.steps[2] as GrammarExamplesStep
        assertNull(examples.bodyTr)

        val mistake = topic.steps[3] as GrammarMistakeStep
        assertEquals("I is a student.", mistake.wrongEn)
        assertEquals("I am a student.", mistake.rightEn)

        val tip = topic.steps[4] as GrammarTipStep
        assertNotNull(tip.example)
        assertEquals("I'm Ali.", tip.example?.en)

        assertEquals(1, topic.rules.size)
        assertEquals("\\bI\\s+(is|are)\\b", topic.rules.single().pattern)
        assertEquals(3, topic.write.tasks.size)
        assertEquals(listOf("w1", "w2", "w3"), topic.write.tasks.map { it.id })
        assertEquals(3, topic.write.tasks.first().minWords)
        assertEquals(1, topic.write.tasks.first().expect.size)
    }

    @Test
    fun rejectsTooFewSteps() {
        val json = topicJson(steps = """
            [
              {"type": "tip", "title_tr": "Bir", "body_tr": "Gövde."},
              {"type": "tip", "title_tr": "İki", "body_tr": "Gövde."}
            ]
        """.trimIndent())

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(json)
        }
    }

    @Test
    fun rejectsWrongTaskCount() {
        val json = topicJson(tasks = """
            [
              {"id": "w1", "prompt_tr": "Bir", "sample": "I am here.",
               "min_words": 3, "expect": [
                 {"pattern": "\\bI am\\b", "anchor": "\\bI\\b", "note_tr": "Not."}
               ]},
              {"id": "w2", "prompt_tr": "İki", "sample": "I am here.",
               "min_words": 3, "expect": [
                 {"pattern": "\\bI am\\b", "anchor": "\\bI\\b", "note_tr": "Not."}
               ]}
            ]
        """.trimIndent())

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(json)
        }
    }

    @Test
    fun rejectsInvalidRegex() {
        val json = topicJson(rules = """
            [{"pattern": "[", "replace": "x", "note_tr": "Bozuk."}]
        """.trimIndent())

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(json)
        }
    }

    @Test
    fun rejectsOutOfRangeMinWords() {
        val json = topicJson(minWords = 1)

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(json)
        }
    }

    @Test
    fun rejectsUnknownCardType() {
        val json = topicJson(steps = """
            [
              {"type": "tip", "title_tr": "Bir", "body_tr": "Gövde."},
              {"type": "tip", "title_tr": "İki", "body_tr": "Gövde."},
              {"type": "video", "title_tr": "Üç", "body_tr": "Gövde."}
            ]
        """.trimIndent())

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(json)
        }
    }

    @Test
    fun rejectsMissingWriteAndInvalidSchema() {
        val noWrite = """
            {
              "schema": 1, "id": "g1", "cefr": "A1", "title_tr": "A",
              "subtitle_tr": "B", "objective_tr": "C", "est_min": 3,
              "source_notes": [], "steps": [
                {"type": "tip", "title_tr": "Bir", "body_tr": "Gövde."},
                {"type": "tip", "title_tr": "İki", "body_tr": "Gövde."},
                {"type": "tip", "title_tr": "Üç", "body_tr": "Gövde."}
              ], "rules": []
            }
        """.trimIndent()

        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(noWrite)
        }
        assertThrows(GrammarParseException::class.java) {
            GrammarParser.parseTopic(topicJson(schema = 2))
        }
    }

    private fun topicJson(
        schema: Int = 1,
        steps: String = DEFAULT_STEPS,
        rules: String = DEFAULT_RULES,
        tasks: String = DEFAULT_TASKS,
        minWords: Int = 3,
    ): String = """
        {
          "schema": $schema,
          "id": "g-a1-to-be",
          "cefr": "A1",
          "title_tr": "am / is / are",
          "subtitle_tr": "Kendini ve çevreni tanıt",
          "objective_tr": "Bu konudan sonra 3 cümle kurabileceksin.",
          "est_min": 4,
          "source_notes": ["a1-u01-l01#0", "a1-u01-l02#0"],
          "steps": $steps,
          "rules": $rules,
          "write": {
            "intro_tr": "Şimdi sıra sende.",
            "tasks": ${tasks.replace("\"min_words\": 3", "\"min_words\": $minWords")}
          }
        }
    """.trimIndent()

    private companion object {

        val DEFAULT_STEPS = """
            [
              {"type": "hook", "title_tr": "Neden önemli?", "body_tr": "Gövde.",
               "examples": [{"en": "I am Ali.", "tr": "Ben Ali."}]},
              {"type": "rule", "title_tr": "Kural", "body_tr": "Gövde.",
               "pattern": "I + am",
               "examples": [{"en": "I am a student.", "tr": "Öğrenciyim."}]},
              {"type": "examples", "title_tr": "Örnekler",
               "examples": [{"en": "We are ready.", "tr": "Hazırız."}]},
              {"type": "mistake", "title_tr": "Sık hata",
               "wrong_en": "I is a student.", "right_en": "I am a student.",
               "body_tr": "I ile am kullanılır."},
              {"type": "tip", "title_tr": "Akılda tut", "body_tr": "Kısa biçimler.",
               "example": {"en": "I'm Ali.", "tr": "Ben Ali."}}
            ]
        """.trimIndent()

        val DEFAULT_RULES = """
            [{"pattern": "\\bI\\s+(is|are)\\b", "replace": "I am",
              "note_tr": "'I' ile 'am' kullanılır."}]
        """.trimIndent()

        val DEFAULT_TASKS = """
            [
              {"id": "w1", "prompt_tr": "Kendini tanıt.", "hint_tr": "I am ...",
               "sample": "I am Deniz.", "min_words": 3,
               "expect": [{"pattern": "\\bI\\s*(am|'m)\\b", "anchor": "\\bI\\b",
                           "note_tr": "Kendini tanıtırken 'I am' kullan."}]},
              {"id": "w2", "prompt_tr": "Bir arkadaşını tanıt.",
               "sample": "She is a student.", "min_words": 3,
               "expect": [{"pattern": "\\b(is|are)\\b", "anchor": "\\bShe\\b",
                           "note_tr": "'is' kullan."}]},
              {"id": "w3", "prompt_tr": "Nerede olduğunu yaz.",
               "sample": "We are at home.", "min_words": 3,
               "expect": [{"pattern": "\\bare\\b", "anchor": "\\bWe\\b",
                           "note_tr": "'are' kullan."}]}
            ]
        """.trimIndent()
    }
}
