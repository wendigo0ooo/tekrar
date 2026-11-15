package com.wendigo.tekrar.repo

import android.content.Context
import com.wendigo.tekrar.core.MiniJson
import com.wendigo.tekrar.core.grammar.GrammarChecker
import com.wendigo.tekrar.core.grammar.GrammarIndex
import com.wendigo.tekrar.core.grammar.GrammarParseException
import com.wendigo.tekrar.core.grammar.GrammarParser
import com.wendigo.tekrar.core.grammar.GrammarTopic
import com.wendigo.tekrar.core.grammar.GrammarTopicRef
import com.wendigo.tekrar.core.grammar.SpellingLexicon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * `assets/grammar/` içeriğini yükler ve yazım sözlüğünü bir kez kurup
 * önbelleğe alır. Parser/checker saf Kotlin'dir; IO dispatcher'ında çalışır.
 */
class GrammarRepository(
    context: Context,
) {

    private val assets = context.applicationContext.assets
    private val cacheMutex = Mutex()

    private var indexCache: GrammarIndex? = null
    private val topicCache = HashMap<String, GrammarTopic>()

    @Volatile
    private var lexiconCache: SpellingLexicon? = null

    @Volatile
    private var checkerCache: GrammarChecker? = null

    suspend fun loadIndex(): GrammarIndex = withContext(Dispatchers.IO) {
        indexCache ?: cacheMutex.withLock {
            indexCache ?: run {
                val path = "$ROOT/index.json"
                GrammarParser.parseIndex(read(path), path).also { indexCache = it }
            }
        }
    }

    suspend fun loadTopic(ref: GrammarTopicRef): GrammarTopic = withContext(Dispatchers.IO) {
        topicCache[ref.id] ?: cacheMutex.withLock {
            topicCache[ref.id] ?: run {
                val path = "$ROOT/${ref.file}"
                val topic = GrammarParser.parseTopic(read(path), path)
                if (topic.id != ref.id) {
                    throw GrammarParseException(
                        path,
                        "konu kimliği '${topic.id}' index'teki '${ref.id}' ile eşleşmiyor",
                    )
                }
                topic.also { topicCache[ref.id] = it }
            }
        }
    }

    suspend fun loadTopic(topicId: String): GrammarTopic? {
        val ref = loadIndex().topics.firstOrNull { it.id == topicId } ?: return null
        return loadTopic(ref)
    }

    suspend fun loadTopics(): List<GrammarTopic> =
        loadIndex().topics.map { loadTopic(it) }

    /**
     * Yazım sözlüğü: kurs, sözlük ve gramer asset'lerinden bir kez kurulur.
     * Gramer içeriği henüz yoksa yalnız kurs/sözlükten kurulur.
     */
    suspend fun lexicon(): SpellingLexicon = withContext(Dispatchers.IO) {
        lexiconCache ?: cacheMutex.withLock {
            lexiconCache ?: buildLexicon().also { lexiconCache = it }
        }
    }

    suspend fun checker(): GrammarChecker = withContext(Dispatchers.IO) {
        checkerCache ?: run {
            val lexicon = lexicon()
            cacheMutex.withLock {
                checkerCache ?: GrammarChecker(lexicon).also { checkerCache = it }
            }
        }
    }

    fun invalidate() {
        indexCache = null
        topicCache.clear()
        lexiconCache = null
        checkerCache = null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheMutex.withLock { invalidate() }
    }

    private fun buildLexicon(): SpellingLexicon {
        val texts = ArrayList<String>()
        for (path in assetFiles(COURSE_ROOT)) {
            val parsed = parseAsset(path) ?: continue
            collectStrings(parsed, texts, keyBlacklist = COURSE_BLACKLIST)
        }
        collectDictionaryTerms(texts)
        for (path in assetFiles(GRAMMAR_ROOT)) {
            val parsed = parseAsset(path) ?: continue
            collectStrings(parsed, texts, keyBlacklist = GRAMMAR_BLACKLIST)
        }
        return SpellingLexicon.build(texts)
    }

    private fun collectDictionaryTerms(out: MutableList<String>) {
        val path = DICTIONARY_PATH
        val parsed = parseAsset(path) ?: return
        val entries = parsed as? List<*> ?: return
        for (entry in entries) {
            val term = (entry as? Map<*, *>)?.get("term") as? String ?: continue
            out += term
        }
    }

    private fun parseAsset(path: String): Any? = runCatching {
        MiniJson.parse(read(path))
    }.getOrNull()

    private fun collectStrings(
        node: Any?,
        out: MutableList<String>,
        keyBlacklist: Set<String>,
    ) {
        when (node) {
            is String -> out += node
            is Map<*, *> -> node.forEach { (key, value) ->
                val name = key?.toString().orEmpty()
                if (name in keyBlacklist) return@forEach
                if (name.endsWith("_tr")) return@forEach
                collectStrings(value, out, keyBlacklist)
            }

            is List<*> -> node.forEach { collectStrings(it, out, keyBlacklist) }
        }
    }

    private fun assetFiles(root: String): List<String> {
        val files = ArrayList<String>()

        fun walk(path: String) {
            val children = runCatching { assets.list(path) }.getOrNull() ?: return
            for (child in children) {
                val full = "$path/$child"
                val nested = runCatching { assets.list(full) }.getOrNull()
                if (!nested.isNullOrEmpty()) {
                    walk(full)
                } else {
                    files += full
                }
            }
        }

        walk(root)
        return files
    }

    private fun read(path: String): String = try {
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (error: IOException) {
        throw GrammarParseException(path, "asset okunamadı: ${error.message}")
    }

    private companion object {
        const val ROOT = "grammar"
        const val GRAMMAR_ROOT = "grammar"
        const val COURSE_ROOT = "course"
        const val DICTIONARY_PATH = "dictionary/en_tr.json"

        val COURSE_BLACKLIST = setOf("ipa")
        val GRAMMAR_BLACKLIST = setOf(
            "schema",
            "id",
            "file",
            "icon",
            "color",
            "type",
            "cefr",
            "est_min",
            "linked_lessons",
            "source_notes",
            "language",
            "native",
            "pattern",
            "anchor",
            "replace",
            "min_words",
        )
    }
}
