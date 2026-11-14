package com.wendigo.tekrar.core.learn

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException

/**
 * Kurs JSON dosyalarını [MiniJson] ile ayrıştırır.
 *
 * Yalnız yapısal zorunlu alanlar denetlenir; içerik yoğunluğu (ders başına
 * alıştırma sayısı, kelime sayısı vb.) `tools/content/validate.py` işidir.
 * Böylece tek alıştırmalı test fixture'ları da ayrıştırılabilir.
 */
class CourseParseException(
    val path: String,
    message: String,
) : IllegalArgumentException(if (path.isBlank()) message else "$path: $message")

object CourseParser {

    fun parseIndex(json: String, source: String = "index.json"): CourseIndex {
        val root = rootNode(json, source)
        requireSchema(root)
        return CourseIndex(
            schema = root.int("schema"),
            language = root.text("language"),
            native = root.text("native"),
            levels = root.objects("levels", min = 1).map { level ->
                CourseLevel(
                    id = level.text("id"),
                    title = level.text("title"),
                    units = level.objects("units", min = 1).map { unit ->
                        UnitRef(
                            id = unit.text("id"),
                            file = unit.text("file"),
                        )
                    },
                )
            },
        )
    }

    fun parseUnit(json: String, source: String = "unit.json"): CourseUnit {
        val root = rootNode(json, source)
        requireSchema(root)
        return CourseUnit(
            schema = root.int("schema"),
            id = root.text("id"),
            cefr = root.text("cefr"),
            title = root.text("title"),
            subtitle = root.text("subtitle"),
            icon = root.optionalText("icon"),
            color = root.optionalText("color"),
            lessons = root.objects("lessons", min = 1).map { lesson ->
                LessonRef(
                    id = lesson.text("id"),
                    title = lesson.text("title"),
                    file = lesson.text("file"),
                    estMin = lesson.int("est_min"),
                )
            },
            roleplay = root.objectNode("roleplay")?.let { rp ->
                RoleplayRef(
                    id = rp.text("id"),
                    title = rp.text("title"),
                    file = rp.text("file"),
                    estMin = rp.int("est_min"),
                )
            },
        )
    }

    fun parseLesson(json: String, source: String = "lesson.json"): Lesson {
        val root = rootNode(json, source)
        requireSchema(root)
        return Lesson(
            schema = root.int("schema"),
            id = root.text("id"),
            unit = root.text("unit"),
            cefr = root.text("cefr"),
            title = root.text("title"),
            objectiveTr = root.text("objective_tr"),
            estMin = root.int("est_min"),
            vocab = root.objects("vocab", min = 1).map { parseVocab(it) },
            grammar = if (root.has("grammar")) root.objects("grammar").map { parseGrammar(it) } else emptyList(),
            dialogue = root.objectNode("dialogue")?.let { parseDialogue(it) },
            teach = root.objects("teach").map { parseTeach(it) },
            exercises = root.objects("exercises", min = 1).map { parseExercise(it) },
        )
    }

    fun parseRoleplay(json: String, source: String = "roleplay.json"): RoleplayScenario {
        val root = rootNode(json, source)
        requireSchema(root)
        return RoleplayScenario(
            schema = root.int("schema"),
            id = root.text("id"),
            unit = root.text("unit"),
            cefr = root.text("cefr"),
            title = root.text("title"),
            settingTr = root.text("setting_tr"),
            roleEn = root.text("role_en"),
            goalTr = root.text("goal_tr"),
            openingEn = root.text("opening_en"),
            targetPhrases = root.stringList("target_phrases", min = 1),
            suggestedTurns = root.int("suggested_turns"),
            successCriteriaTr = root.stringList("success_criteria_tr", min = 1),
        )
    }

    private fun parseVocab(node: Node): VocabItem = VocabItem(
        term = node.text("term"),
        meaningTr = node.text("meaning_tr"),
        pos = node.text("pos"),
        example = node.text("example"),
        exampleTr = node.text("example_tr"),
        ipa = node.optionalText("ipa"),
    )

    private fun parseGrammar(node: Node): GrammarNote = GrammarNote(
        titleTr = node.text("title_tr"),
        explanationTr = node.text("explanation_tr"),
        examples = node.objects("examples", min = 1).map { parseExample(it) },
    )

    private fun parseExample(node: Node): Example = Example(
        en = node.text("en"),
        tr = node.text("tr"),
    )

    private fun parseDialogue(node: Node): Dialogue = Dialogue(
        title = node.text("title"),
        lines = node.objects("lines", min = 1).map { line ->
            DialogueLine(
                speaker = line.text("speaker"),
                en = line.text("en"),
                tr = line.text("tr"),
            )
        },
    )

    private fun parseTeach(node: Node): TeachCard = TeachCard(
        titleTr = node.text("title_tr"),
        bodyTr = node.text("body_tr"),
        examples = node.objects("examples", min = 1).map { parseExample(it) },
        tipTr = node.optionalText("tip_tr"),
    )

    private fun parseExercise(node: Node): Exercise {
        val type = node.text("type")
        val prompt = node.text("prompt_tr")
        val explanation = node.optionalText("explanation_tr")
        return when (type) {
            "choice" -> {
                val options = node.stringList("options", min = 2)
                Choice(prompt, options, node.answerIndex(options.size), explanation)
            }

            "listen_choice" -> {
                val options = node.stringList("options", min = 2)
                ListenChoice(
                    promptTr = prompt,
                    audioText = node.text("audio_text"),
                    options = options,
                    answer = node.answerIndex(options.size),
                    explanationTr = explanation,
                )
            }

            "dialogue_choice" -> {
                val options = node.stringList("options", min = 2)
                DialogueChoice(
                    promptTr = prompt,
                    audioText = node.optionalText("audio_text"),
                    options = options,
                    answer = node.answerIndex(options.size),
                    explanationTr = explanation,
                )
            }

            "match" -> Match(
                promptTr = prompt,
                pairs = node.objects("pairs", min = 1).map { pair ->
                    MatchPair(left = pair.text("left"), right = pair.text("right"))
                },
                explanationTr = explanation,
            )

            "word_order" -> {
                val tokens = node.stringList("tokens", min = 1)
                val answer = node.stringList("answer", min = 1)
                if (tokens.sorted() != answer.sorted()) {
                    node.fail("'tokens' ve 'answer' aynı kelime çokluğuna sahip olmalı")
                }
                WordOrder(
                    promptTr = prompt,
                    tokens = tokens,
                    answer = answer,
                    translationTr = node.optionalText("translation_tr"),
                    explanationTr = explanation,
                )
            }

            "cloze" -> {
                val sentence = node.text("sentence")
                if (sentence.countBlanks() != 1) {
                    node.fail("'sentence' tam olarak bir ___ içermeli")
                }
                val hasOptions = node.has("options")
                val hasAnswers = node.has("answers")
                if (hasOptions == hasAnswers) {
                    node.fail("'options' veya 'answers' alanlarından tam olarak biri bulunmalı")
                }
                val options = if (hasOptions) node.stringList("options", min = 2) else null
                Cloze(
                    promptTr = prompt,
                    sentence = sentence,
                    options = options,
                    answer = if (options != null) node.answerIndex(options.size) else null,
                    answers = if (hasAnswers) node.stringList("answers", min = 1) else null,
                    explanationTr = explanation,
                )
            }

            "translate" -> Translate(
                promptTr = prompt,
                answers = node.stringList("answers", min = 1),
                hintTr = node.optionalText("hint_tr"),
                explanationTr = explanation,
            )

            "speak" -> Speak(
                promptTr = prompt,
                target = node.text("target"),
                hintTr = node.optionalText("hint_tr"),
                explanationTr = explanation,
            )

            "write" -> {
                val minWords = node.int("min_words")
                if (minWords <= 0) node.fail("'min_words' pozitif olmalı")
                Write(
                    promptTr = prompt,
                    minWords = minWords,
                    keywords = if (node.has("keywords")) node.stringList("keywords", min = 1) else emptyList(),
                    sample = node.optionalText("sample"),
                    rubricTr = node.optionalText("rubric_tr"),
                    explanationTr = explanation,
                )
            }

            else -> node.fail("bilinmeyen alıştırma tipi '$type'")
        }
    }

    private fun requireSchema(root: Node) {
        if (root.int("schema") != 1) root.fail("'schema' 1 olmalı")
    }

    private fun rootNode(json: String, source: String): Node {
        val parsed = try {
            MiniJson.parse(json)
        } catch (error: MiniJsonException) {
            throw CourseParseException(source, "geçersiz JSON: ${error.message}")
        }
        if (parsed !is Map<*, *>) throw CourseParseException(source, "kök bir JSON nesnesi olmalı")
        return Node(source, parsed.asValueMap())
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.asValueMap(): Map<String, Any?> = this as Map<String, Any?>

    private fun String.countBlanks(): Int {
        var count = 0
        var index = indexOf("___")
        while (index >= 0) {
            count++
            index = indexOf("___", index + 3)
        }
        return count
    }

    private class Node(
        val path: String,
        private val values: Map<String, Any?>,
    ) {

        fun has(key: String): Boolean = values.containsKey(key) && values[key] != null

        fun text(key: String): String = optionalText(key) ?: fail("eksik alan '$key'")

        fun optionalText(key: String): String? {
            if (!values.containsKey(key)) return null
            val value = values[key]
            if (value !is String) fail("'$key' metin olmalı")
            if (value.isBlank()) fail("'$key' boş olamaz")
            return value
        }

        fun int(key: String): Int = optionalInt(key) ?: fail("eksik alan '$key'")

        fun optionalInt(key: String): Int? {
            if (!values.containsKey(key)) return null
            return when (val value = values[key]) {
                is Int -> value
                is Long -> if (value in Int.MIN_VALUE..Int.MAX_VALUE) {
                    value.toInt()
                } else {
                    fail("'$key' tam sayı aralığı dışında")
                }

                is Double -> {
                    if (!value.isFinite() || value % 1.0 != 0.0 ||
                        value < Int.MIN_VALUE || value > Int.MAX_VALUE
                    ) {
                        fail("'$key' tam sayı olmalı")
                    }
                    value.toInt()
                }

                else -> fail("'$key' tam sayı olmalı")
            }
        }

        fun answerIndex(optionCount: Int): Int {
            val answer = int("answer")
            if (answer !in 0 until optionCount) {
                fail("'answer' seçenek aralığında değil")
            }
            return answer
        }

        fun list(key: String, min: Int = 0, max: Int = Int.MAX_VALUE): List<Any?> {
            if (!values.containsKey(key)) fail("eksik alan '$key'")
            val value = values[key]
            if (value !is List<*>) fail("'$key' dizi olmalı")
            if (value.size < min || value.size > max) {
                fail("'$key' $min-$max öğe içermeli")
            }
            return value
        }

        fun stringList(key: String, min: Int = 0, max: Int = Int.MAX_VALUE): List<String> =
            list(key, min, max).mapIndexed { index, value ->
                if (value !is String) fail("'$key[$index]' metin olmalı")
                if (value.isBlank()) fail("'$key[$index]' boş olamaz")
                value
            }

        fun objects(
            key: String,
            min: Int = 0,
            max: Int = Int.MAX_VALUE,
        ): List<Node> = list(key, min, max).mapIndexed { index, value ->
            if (value !is Map<*, *>) fail("'$key[$index]' nesne olmalı")
            Node("$path.$key[$index]", value.asValueMap())
        }

        fun objectNode(key: String): Node? {
            if (!values.containsKey(key) || values[key] == null) return null
            val value = values[key]
            if (value !is Map<*, *>) fail("'$key' nesne olmalı")
            return Node("$path.$key", value.asValueMap())
        }

        fun fail(message: String): Nothing = throw CourseParseException(path, message)
    }
}
