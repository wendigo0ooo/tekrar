package com.wendigo.tekrar.core

import com.wendigo.tekrar.data.ReviewLog
import com.wendigo.tekrar.data.Word

/**
 * JSON yedek biçimi. Android Context/Room/org.json bağımlılığı yoktur; bu sayede
 * birim testlerinde doğrudan çalıştırılabilir.
 */
data class BackupSettings(
    val newPerDay: Int = DEFAULT_NEW_PER_DAY,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val weeklyGoalDays: Int = DEFAULT_WEEKLY_GOAL_DAYS,
    val desiredRetention: Int = DEFAULT_RETENTION,
    val themeMode: Int = 0,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
) {
    companion object {
        const val DEFAULT_NEW_PER_DAY = 20
        const val DEFAULT_DAILY_GOAL = 20
        const val DEFAULT_WEEKLY_GOAL_DAYS = 5
        const val DEFAULT_RETENTION = 90
    }
}

data class BackupPackage(
    val exportedAt: Long,
    val words: List<Word>,
    val logs: List<ReviewLog> = emptyList(),
    val settings: BackupSettings? = null,
    val skippedWords: Int = 0,
    val skippedLogs: Int = 0,
)

class BackupFormatException(message: String) : IllegalArgumentException(message)

object BackupFormat {

    const val CURRENT_VERSION = 3
    const val MIN_VERSION = 2

    private const val APP_MARKER = "tekrar"

    fun build(pkg: BackupPackage): String {
        val words = J.Arr(pkg.words.map { wordJson(it) })
        val logs = J.Arr(pkg.logs.map { logJson(it) })
        val root = LinkedHashMap<String, J>()
        root["app"] = J.of(APP_MARKER)
        root["format"] = J.of(CURRENT_VERSION)
        root["exportedAt"] = J.of(pkg.exportedAt)
        root["words"] = words
        root["reviewLogs"] = logs
        pkg.settings?.let { root["settings"] = settingsJson(it) }
        return Writer.encode(J.Obj(root))
    }

    fun parse(raw: String): BackupPackage {
        val root = try {
            JsonReader(raw.removePrefix("\uFEFF")).readDocument()
        } catch (error: BackupFormatException) {
            throw error
        } catch (error: Exception) {
            throw BackupFormatException("Yedek dosyası okunamadı: geçersiz veya bozuk JSON.")
        }
        val obj = root as? J.Obj
            ?: throw BackupFormatException("Geçersiz yedek dosyası: kök nesne bulunamadı.")
        if (obj.string("app") != APP_MARKER) {
            throw BackupFormatException(
                "Geçersiz yedek dosyası: uygulama imzası eksik veya tanınmıyor."
            )
        }
        val version = obj.longOrNull("format")
            ?: throw BackupFormatException("Geçersiz yedek dosyası: sürüm bilgisi yok.")
        if (version < MIN_VERSION) {
            throw BackupFormatException("Bu yedek çok eski bir sürümle oluşturulmuş.")
        }
        if (version > CURRENT_VERSION) {
            throw BackupFormatException(
                "Bu yedek daha yeni bir uygulama sürümüyle oluşturulmuş; lütfen uygulamayı güncelle."
            )
        }
        val wordsArray = obj.values["words"] as? J.Arr
            ?: throw BackupFormatException("Yedek dosyasında kelime listesi yok.")

        var skippedWords = 0
        val words = ArrayList<Word>(wordsArray.values.size)
        val seenTerms = HashSet<String>()
        val acceptedIds = HashSet<Long>()

        for (element in wordsArray.values) {
            val item = element as? J.Obj
            if (item == null) {
                skippedWords++
                continue
            }
            val term = item.string("term")?.trim().orEmpty()
            val meaning = item.string("meaning")?.trim().orEmpty()
            if (term.isEmpty() || meaning.isEmpty()) {
                skippedWords++
                continue
            }
            if (!seenTerms.add(TermKey.normalize(term))) {
                skippedWords++
                continue
            }
            val rawId = item.longOrNull("id") ?: 0L
            val id = when {
                rawId <= 0L -> 0L
                acceptedIds.add(rawId) -> rawId
                else -> {
                    skippedWords++
                    continue
                }
            }
            val now = item.longOrNull("createdAt") ?: System.currentTimeMillis()
            words += Word(
                id = id,
                term = term,
                meaning = meaning,
                example = item.string("example").orEmpty(),
                exampleTr = item.string("exampleTr").orEmpty(),
                notes = item.string("notes").orEmpty(),
                tags = item.string("tags").orEmpty(),
                pos = item.string("pos").orEmpty(),
                favorite = item.bool("favorite", false),
                createdAt = now,
                state = if (item.int("state", Word.STATE_NEW) == Word.STATE_REVIEW) {
                    Word.STATE_REVIEW
                } else {
                    Word.STATE_NEW
                },
                stability = item.double("stability", 0.0).coerceAtLeast(0.0),
                difficulty = item.double("difficulty", 0.0).coerceIn(0.0, 10.0),
                dueAt = item.longOrNull("dueAt")?.takeIf { it > 0L } ?: now,
                lastReviewAt = item.longOrNull("lastReviewAt")?.takeIf { it > 0L },
                introducedAt = item.longOrNull("introducedAt")?.takeIf { it > 0L },
                reps = item.int("reps", 0).coerceAtLeast(0),
                lapses = item.int("lapses", 0).coerceAtLeast(0),
                suspended = item.bool("suspended", false),
            )
        }

        val logsArray = (obj.values["reviewLogs"] ?: obj.values["logs"]) as? J.Arr
        var skippedLogs = 0
        val logs = ArrayList<ReviewLog>(logsArray?.values?.size ?: 0)
        val usedLogIds = HashSet<Long>()
        if (logsArray != null) {
            for (element in logsArray.values) {
                val item = element as? J.Obj
                if (item == null) {
                    skippedLogs++
                    continue
                }
                val wordId = item.longOrNull("wordId") ?: 0L
                val reviewedAt = item.longOrNull("reviewedAt") ?: 0L
                val rating = item.int("rating", 0)
                if (wordId <= 0L || !acceptedIds.contains(wordId) || reviewedAt <= 0L ||
                    rating < Srs.AGAIN || rating > Srs.EASY
                ) {
                    skippedLogs++
                    continue
                }
                val rawId = item.longOrNull("id") ?: 0L
                val id = when {
                    rawId <= 0L -> 0L
                    usedLogIds.add(rawId) -> rawId
                    else -> {
                        skippedLogs++
                        continue
                    }
                }
                logs += ReviewLog(
                    id = id,
                    wordId = wordId,
                    rating = rating,
                    reviewedAt = reviewedAt,
                    previousDueAt = item.longOrNull("previousDueAt") ?: 0L,
                    previousStability = item.double("previousStability", 0.0),
                    previousDifficulty = item.double("previousDifficulty", 0.0),
                    nextDueAt = item.longOrNull("nextDueAt") ?: reviewedAt,
                    nextStability = item.double("nextStability", 0.0),
                    nextDifficulty = item.double("nextDifficulty", 0.0),
                    elapsedDays = item.double("elapsedDays", 0.0),
                )
            }
        }

        val settings = (obj.values["settings"] as? J.Obj)?.let { item ->
            BackupSettings(
                newPerDay = item.int("newPerDay", BackupSettings.DEFAULT_NEW_PER_DAY),
                dailyGoal = item.int("dailyGoal", BackupSettings.DEFAULT_DAILY_GOAL),
                weeklyGoalDays = item.int(
                    "weeklyGoalDays",
                    BackupSettings.DEFAULT_WEEKLY_GOAL_DAYS,
                ),
                desiredRetention = item.int(
                    "desiredRetention",
                    BackupSettings.DEFAULT_RETENTION,
                ),
                themeMode = item.int("themeMode", 0),
                autoSpeak = item.bool("autoSpeak", true),
                speechRate = item.double("speechRate", 1.0).toFloat(),
                reminderEnabled = item.bool("reminderEnabled", false),
                reminderHour = item.int("reminderHour", 20),
                reminderMinute = item.int("reminderMinute", 0),
            )
        }

        return BackupPackage(
            exportedAt = obj.longOrNull("exportedAt") ?: 0L,
            words = words,
            logs = logs,
            settings = settings,
            skippedWords = skippedWords,
            skippedLogs = skippedLogs,
        )
    }

    private fun wordJson(word: Word): J = J.Obj(
        linkedMapOf(
            "id" to J.of(word.id),
            "term" to J.of(word.term),
            "meaning" to J.of(word.meaning),
            "example" to J.of(word.example),
            "exampleTr" to J.of(word.exampleTr),
            "notes" to J.of(word.notes),
            "tags" to J.of(word.tags),
            "pos" to J.of(word.pos),
            "favorite" to J.of(word.favorite),
            "createdAt" to J.of(word.createdAt),
            "state" to J.of(word.state),
            "stability" to J.of(word.stability),
            "difficulty" to J.of(word.difficulty),
            "dueAt" to J.of(word.dueAt),
            "lastReviewAt" to J.ofNullable(word.lastReviewAt),
            "introducedAt" to J.ofNullable(word.introducedAt),
            "reps" to J.of(word.reps),
            "lapses" to J.of(word.lapses),
            "suspended" to J.of(word.suspended),
        )
    )

    private fun logJson(log: ReviewLog): J = J.Obj(
        linkedMapOf(
            "id" to J.of(log.id),
            "wordId" to J.of(log.wordId),
            "rating" to J.of(log.rating),
            "reviewedAt" to J.of(log.reviewedAt),
            "previousDueAt" to J.of(log.previousDueAt),
            "previousStability" to J.of(log.previousStability),
            "previousDifficulty" to J.of(log.previousDifficulty),
            "nextDueAt" to J.of(log.nextDueAt),
            "nextStability" to J.of(log.nextStability),
            "nextDifficulty" to J.of(log.nextDifficulty),
            "elapsedDays" to J.of(log.elapsedDays),
        )
    )

    private fun settingsJson(settings: BackupSettings): J = J.Obj(
        linkedMapOf(
            "newPerDay" to J.of(settings.newPerDay),
            "dailyGoal" to J.of(settings.dailyGoal),
            "weeklyGoalDays" to J.of(settings.weeklyGoalDays),
            "desiredRetention" to J.of(settings.desiredRetention),
            "themeMode" to J.of(settings.themeMode),
            "autoSpeak" to J.of(settings.autoSpeak),
            "speechRate" to J.of(settings.speechRate),
            "reminderEnabled" to J.of(settings.reminderEnabled),
            "reminderHour" to J.of(settings.reminderHour),
            "reminderMinute" to J.of(settings.reminderMinute),
        )
    )

    private sealed interface J {
        data class Str(val value: String) : J
        data class Num(val raw: String) : J
        data class Bool(val value: Boolean) : J
        data class Arr(val values: List<J>) : J
        data class Obj(val values: Map<String, J>) : J
        object Null : J

        companion object {
            fun of(value: String): J = Str(value)
            fun of(value: Boolean): J = Bool(value)
            fun of(value: Int): J = Num(value.toString())
            fun of(value: Long): J = Num(value.toString())
            fun of(value: Float): J = Num(if (value.isFinite()) value.toString() else "0")
            fun of(value: Double): J = Num(if (value.isFinite()) value.toString() else "0")
            fun ofNullable(value: Long?): J = value?.let { of(it) } ?: Null
        }
    }

    private fun J.Obj.string(key: String): String? = (values[key] as? J.Str)?.value

    private fun J.Obj.longOrNull(key: String): Long? {
        val number = values[key] as? J.Num ?: return null
        return number.raw.toLongOrNull() ?: number.raw.toDoubleOrNull()?.toLong()
    }

    private fun J.Obj.int(key: String, fallback: Int): Int =
        longOrNull(key)?.let { value ->
            if (value > Int.MAX_VALUE) Int.MAX_VALUE
            else if (value < Int.MIN_VALUE) Int.MIN_VALUE
            else value.toInt()
        } ?: fallback

    private fun J.Obj.double(key: String, fallback: Double): Double =
        (values[key] as? J.Num)?.raw?.toDoubleOrNull() ?: fallback

    private fun J.Obj.bool(key: String, fallback: Boolean): Boolean =
        (values[key] as? J.Bool)?.value ?: fallback

    private object Writer {
        fun encode(value: J): String = buildString { write(value, this) }

        private fun write(value: J, out: StringBuilder) {
            when (value) {
                is J.Str -> writeString(value.value, out)
                is J.Num -> out.append(value.raw)
                is J.Bool -> out.append(if (value.value) "true" else "false")
                J.Null -> out.append("null")
                is J.Arr -> {
                    out.append('[')
                    value.values.forEachIndexed { index, item ->
                        if (index > 0) out.append(',')
                        write(item, out)
                    }
                    out.append(']')
                }
                is J.Obj -> {
                    out.append('{')
                    var first = true
                    for ((key, item) in value.values) {
                        if (!first) out.append(',')
                        first = false
                        writeString(key, out)
                        out.append(':')
                        write(item, out)
                    }
                    out.append('}')
                }
            }
        }

        private fun writeString(value: String, out: StringBuilder) {
            out.append('"')
            for (char in value) {
                when (char) {
                    '"' -> out.append("\\\"")
                    '\\' -> out.append("\\\\")
                    '\n' -> out.append("\\n")
                    '\r' -> out.append("\\r")
                    '\t' -> out.append("\\t")
                    '\b' -> out.append("\\b")
                    '\u000C' -> out.append("\\f")
                    else -> if (char < ' ') {
                        out.append("\\u")
                        out.append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        out.append(char)
                    }
                }
            }
            out.append('"')
        }
    }

    private class JsonReader(private val text: String) {

        private var position = 0

        fun readDocument(): J {
            skipWhitespace()
            val value = readValue()
            skipWhitespace()
            if (position != text.length) fail("fazladan içerik")
            return value
        }

        private fun readValue(): J {
            skipWhitespace()
            if (position >= text.length) fail("beklenmedik dosya sonu")
            return when (val char = text[position]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> J.Str(readString())
                't' -> {
                    expectLiteral("true")
                    J.Bool(true)
                }
                'f' -> {
                    expectLiteral("false")
                    J.Bool(false)
                }
                'n' -> {
                    expectLiteral("null")
                    J.Null
                }
                '-', in '0'..'9' -> readNumber()
                else -> fail("beklenmeyen karakter '$char'")
            }
        }

        private fun readObject(): J.Obj {
            position++
            val values = LinkedHashMap<String, J>()
            skipWhitespace()
            if (peek() == '}') {
                position++
                return J.Obj(values)
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
                        return J.Obj(values)
                    }
                    else -> fail("',' veya '}' bekleniyordu")
                }
            }
        }

        private fun readArray(): J.Arr {
            position++
            val values = ArrayList<J>()
            skipWhitespace()
            if (peek() == ']') {
                position++
                return J.Arr(values)
            }
            while (true) {
                values += readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> position++
                    ']' -> {
                        position++
                        return J.Arr(values)
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
                val char = text[position++]
                when (char) {
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

        private fun readNumber(): J.Num {
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
            if (raw.isEmpty() || raw == "-" || raw.toDoubleOrNull() == null) {
                fail("geçersiz sayı")
            }
            return J.Num(raw)
        }

        private fun expectLiteral(literal: String) {
            if (!text.startsWith(literal, position)) fail("'$literal' bekleniyordu")
            position += literal.length
        }

        private fun peek(): Char {
            if (position >= text.length) fail("beklenmedik dosya sonu")
            return text[position]
        }

        private fun skipWhitespace() {
            while (position < text.length) {
                when (text[position]) {
                    ' ', '\t', '\n', '\r' -> position++
                    else -> return
                }
            }
        }

        private fun fail(reason: String): Nothing =
            throw BackupFormatException("Yedek dosyası okunamadı: geçersiz JSON ($reason).")
    }
}
