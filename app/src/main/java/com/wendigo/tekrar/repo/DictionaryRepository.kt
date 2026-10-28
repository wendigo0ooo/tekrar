package com.wendigo.tekrar.repo

import android.content.Context
import com.wendigo.tekrar.core.DeckCatalog
import com.wendigo.tekrar.core.Dictionary
import com.wendigo.tekrar.core.DictionaryEntry
import com.wendigo.tekrar.core.DictionaryIndex
import com.wendigo.tekrar.core.MiningSource
import com.wendigo.tekrar.core.TermKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class LexiconHit(
    val entry: DictionaryEntry,
    val source: MiningSource,
)

data class DictionaryImportResult(
    val entries: Int,
    val skipped: Int,
    val duplicates: Int,
)

class LexiconSnapshot internal constructor(
    private val dictionary: DictionaryIndex,
    private val decks: DictionaryIndex,
) {

    fun lookupHit(term: String): LexiconHit? =
        dictionary.lookup(term)?.let { LexiconHit(it, MiningSource.DICTIONARY) }
            ?: decks.lookup(term)?.let { LexiconHit(it, MiningSource.DECK) }

    fun entriesFor(words: Iterable<String>): List<DictionaryEntry> {
        val seen = HashSet<String>()
        val result = ArrayList<DictionaryEntry>()
        for (word in words) {
            val key = TermKey.normalize(word)
            if (key.isEmpty() || !seen.add(key)) continue
            val entry = dictionary.lookup(key) ?: decks.lookup(key)
            if (entry != null) result += entry
        }
        return result
    }
}

/**
 * Dosya tabanlı sözlük deposu. Paket sözlüğü, kullanıcı sözlüğü ve paket
 * destelerindeki kelimeler tek bir bellek içi önbellekte birleştirilir. Room'a
 * hiçbir sözlük kaydı yazılmaz.
 */
class DictionaryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var cache: Lexicon? = null

    private val userFile: File
        get() = File(appContext.filesDir, USER_DICTIONARY_FILE)

    suspend fun lookup(term: String): DictionaryEntry? = lookupHit(term)?.entry

    suspend fun snapshot(): LexiconSnapshot = withContext(Dispatchers.IO) {
        val lexicon = lexicon()
        LexiconSnapshot(lexicon.dictionary, lexicon.decks)
    }

    suspend fun lookupHit(term: String): LexiconHit? = withContext(Dispatchers.IO) {
        val lexicon = lexicon()
        lexicon.dictionary.lookup(term)?.let {
            LexiconHit(it, MiningSource.DICTIONARY)
        } ?: lexicon.decks.lookup(term)?.let {
            LexiconHit(it, MiningSource.DECK)
        }
    }

    suspend fun entriesFor(words: Iterable<String>): List<DictionaryEntry> =
        withContext(Dispatchers.IO) {
            val lexicon = lexicon()
            val seen = HashSet<String>()
            val result = ArrayList<DictionaryEntry>()
            for (word in words) {
                val key = TermKey.normalize(word)
                if (key.isEmpty() || !seen.add(key)) continue
                val entry = lexicon.dictionary.lookup(key) ?: lexicon.decks.lookup(key)
                if (entry != null) result += entry
            }
            result
        }

    suspend fun userEntryCount(): Int = withContext(Dispatchers.IO) {
        readUserEntries().size
    }

    suspend fun importUserJson(raw: String): DictionaryImportResult =
        withContext(Dispatchers.IO) {
            val parsed = Dictionary.parse(raw)
            userFile.writeText(raw, Charsets.UTF_8)
            cache = null
            DictionaryImportResult(parsed.entries.size, parsed.skipped, parsed.duplicates)
        }

    fun invalidate() {
        cache = null
    }

    private suspend fun lexicon(): Lexicon = cache ?: mutex.withLock {
        cache ?: buildLexicon().also { cache = it }
    }

    private fun buildLexicon(): Lexicon {
        val bundled = runCatching { readBundled() }.getOrDefault(emptyList())
        val user = readUserEntries()
        val dictionary = Dictionary.index(Dictionary.merge(bundled, user))
        val deckWords = ArrayList<DictionaryEntry>()
        for (asset in DECK_ASSETS) {
            runCatching {
                val raw = appContext.assets
                    .open("decks/$asset")
                    .bufferedReader()
                    .use { it.readText() }
                DeckCatalog.parse(raw).deck.words.map {
                    DictionaryEntry(
                        term = it.term,
                        meaning = it.meaning,
                        pos = it.pos,
                        example = it.example,
                        exampleTr = it.exampleTr,
                        cefr = it.cefr,
                    )
                }
            }.getOrNull()?.let { deckWords += it }
        }
        return Lexicon(dictionary, Dictionary.index(deckWords))
    }

    private fun readBundled(): List<DictionaryEntry> {
        val raw = appContext.assets
            .open(BUNDLED_DICTIONARY_ASSET)
            .bufferedReader()
            .use { it.readText() }
        return Dictionary.parse(raw).entries
    }

    private fun readUserEntries(): List<DictionaryEntry> {
        val file = userFile
        if (!file.isFile) return emptyList()
        return runCatching {
            Dictionary.parse(file.readText(Charsets.UTF_8)).entries
        }.getOrDefault(emptyList())
    }

    private data class Lexicon(
        val dictionary: DictionaryIndex,
        val decks: DictionaryIndex,
    )

    private companion object {
        const val BUNDLED_DICTIONARY_ASSET = "dictionary/en_tr.json"
        const val USER_DICTIONARY_FILE = "dictionary_user.json"
        val DECK_ASSETS = listOf("yds.json", "phrasal.json")
    }
}
