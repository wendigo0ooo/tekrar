package com.wendigo.tekrar.core.grammar

import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.MiniJsonException

/**
 * Gramer JSON dosyalarını [MiniJson] ile ayrıştırır.
 *
 * `CourseParser` ile aynı yaklaşım: yalnız yapısal zorunlu alanlar ve
 * kontrattaki sayı sınırları denetlenir; içerik kalitesi
 * `tools/content/validate.py` işidir.
 */
class GrammarParseException(
    val path: String,
    message: String,
) : IllegalArgumentException(if (path.isBlank()) message else "$path: $message")

object GrammarParser {

    val ICONS: Set<String> = setOf(
        "person",
        "chat",
        "tag",
        "home",
        "clock",
        "sun",
        "briefcase",
        "question",
        "book",
        "flag",
    )

    fun parseIndex(json: String, source: String = "grammar/index.json"): GrammarIndex {
        val root = rootNode(json, source)
        requireSchema(root)
        val topics = root.objects("topics", min = 1).map { topic ->
            val file = topic.text("file")
            if (file.startsWith("/") || file.split('/').any { it == ".." }) {
                topic.fail("'file' kök dizine göre göreli olmalı")
            }
            GrammarTopicRef(
                id = topic.text("id"),
                titleTr = topic.text("title_tr"),
                subtitleTr = topic.text("subtitle_tr"),
                cefr = topic.text("cefr"),
                file = file,
                icon = topic.optionalText("icon")?.let { icon ->
                    if (icon in ICONS) icon else "book"
                },
                color = topic.optionalText("color")?.also { color ->
                    if (!COLOR_PATTERN.matches(color)) {
                        topic.fail("'color' #RRGGBB biçiminde olmalı")
                    }
                },
                estMin = topic.int("est_min").also {
                    if (it <= 0) topic.fail("'est_min' pozitif olmalı")
                },
                linkedLessons = if (topic.has("linked_lessons")) {
                    topic.stringList("linked_lessons")
                } else {
                    emptyList()
                },
            )
        }
        val ids = HashSet<String>()
        topics.forEach { topic ->
            if (!ids.add(topic.id)) {
                throw GrammarParseException(source, "yinelenen konu kimliği '${topic.id}'")
            }
        }
        return GrammarIndex(
            schema = root.int("schema"),
            language = root.text("language"),
            native = root.text("native"),
            topics = topics,
        )
    }

    fun parseTopic(json: String, source: String = "grammar/topic.json"): GrammarTopic {
        val root = rootNode(json, source)
        requireSchema(root)
        val steps = root.objects("steps", min = 3, max = 6).map { parseStep(it) }
        val rules = root.objects("rules", max = 8).map { parseRule(it) }
        val writeNode = root.objectNode("write")
            ?: root.fail("eksik alan 'write'")
        val tasks = writeNode.objects("tasks", min = 3, max = 3).map { parseTask(it) }
        val taskIds = HashSet<String>()
        tasks.forEach { task ->
            if (!taskIds.add(task.id)) {
                writeNode.fail("yinelenen görev kimliği '${task.id}'")
            }
        }
        return GrammarTopic(
            schema = root.int("schema"),
            id = root.text("id"),
            cefr = root.text("cefr"),
            titleTr = root.text("title_tr"),
            subtitleTr = root.text("subtitle_tr"),
            objectiveTr = root.text("objective_tr"),
            estMin = root.int("est_min").also {
                if (it <= 0) root.fail("'est_min' pozitif olmalı")
            },
            sourceNotes = root.stringList("source_notes"),
            steps = steps,
            rules = rules,
            write = GrammarWrite(
                introTr = writeNode.text("intro_tr"),
                tasks = tasks,
            ),
        )
    }

    private fun parseStep(node: Node): GrammarStep {
        val type = node.text("type")
        val title = node.text("title_tr")
        return when (type) {
            "hook" -> GrammarHookStep(
                titleTr = title,
                bodyTr = node.text("body_tr"),
                examples = parseExamples(node, min = 1, max = 2),
            )

            "rule" -> GrammarRuleStep(
                titleTr = title,
                bodyTr = node.text("body_tr"),
                pattern = node.text("pattern"),
                examples = parseExamples(node, min = 1, max = 3),
            )

            "examples" -> GrammarExamplesStep(
                titleTr = title,
                bodyTr = node.optionalText("body_tr"),
                examples = parseExamples(node, min = 1, max = 3),
            )

            "mistake" -> GrammarMistakeStep(
                titleTr = title,
                wrongEn = node.text("wrong_en"),
                rightEn = node.text("right_en"),
                bodyTr = node.text("body_tr"),
            )

            "tip" -> {
                val example = when {
                    node.has("example") -> parseExample(
                        node.objectNode("example")
                            ?: node.fail("'example' nesne olmalı"),
                    )

                    node.has("examples") -> parseExamples(node, min = 1, max = 1).first()
                    else -> null
                }
                GrammarTipStep(
                    titleTr = title,
                    bodyTr = node.text("body_tr"),
                    example = example,
                )
            }

            else -> node.fail("bilinmeyen kart tipi '$type'")
        }
    }

    private fun parseExamples(node: Node, min: Int, max: Int): List<GrammarExample> =
        node.objects("examples", min = min, max = max).map { parseExample(it) }

    private fun parseExample(node: Node): GrammarExample = GrammarExample(
        en = node.text("en"),
        tr = node.text("tr"),
    )

    private fun parseRule(node: Node): GrammarRule {
        val pattern = node.text("pattern")
        node.compileRegex(pattern, "pattern")
        return GrammarRule(
            pattern = pattern,
            replace = node.text("replace"),
            noteTr = node.text("note_tr"),
        )
    }

    private fun parseTask(node: Node): GrammarWriteTask {
        val expect = node.objects("expect", min = 1, max = 3).map { expectation ->
            val pattern = expectation.text("pattern")
            expectation.compileRegex(pattern, "pattern")
            val anchor = expectation.text("anchor")
            expectation.compileRegex(anchor, "anchor")
            GrammarExpectation(
                pattern = pattern,
                anchor = anchor,
                noteTr = expectation.text("note_tr"),
            )
        }
        val minWords = node.int("min_words")
        if (minWords !in 2..12) node.fail("'min_words' 2-12 arasında olmalı")
        return GrammarWriteTask(
            id = node.text("id"),
            promptTr = node.text("prompt_tr"),
            hintTr = node.optionalText("hint_tr"),
            sample = node.text("sample"),
            minWords = minWords,
            expect = expect,
        )
    }

    private fun requireSchema(root: Node) {
        if (root.int("schema") != 1) root.fail("'schema' 1 olmalı")
    }

    private fun rootNode(json: String, source: String): Node {
        val parsed = try {
            MiniJson.parse(json)
        } catch (error: MiniJsonException) {
            throw GrammarParseException(source, "geçersiz JSON: ${error.message}")
        }
        if (parsed !is Map<*, *>) throw GrammarParseException(source, "kök bir JSON nesnesi olmalı")
        return Node(source, parsed.asValueMap())
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.asValueMap(): Map<String, Any?> = this as Map<String, Any?>

    private val COLOR_PATTERN = Regex("#[0-9A-Fa-f]{6}")

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

        fun int(key: String): Int {
            if (!values.containsKey(key)) fail("eksik alan '$key'")
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

        fun compileRegex(pattern: String, field: String) {
            try {
                Regex(pattern)
            } catch (error: IllegalArgumentException) {
                fail("'$field' geçerli bir regex değil: ${error.message}")
            }
        }

        fun fail(message: String): Nothing = throw GrammarParseException(path, message)
    }
}
