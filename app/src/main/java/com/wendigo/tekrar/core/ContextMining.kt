package com.wendigo.tekrar.core

enum class MiningSource { DICTIONARY, DECK }

data class MiningDefinition(
    val term: String = "",
    val meaning: String,
    val pos: String = "",
    val cefr: String = "",
    val source: MiningSource = MiningSource.DICTIONARY,
)

data class MinedWord(
    val term: String,
    val meaning: String,
    val pos: String = "",
    val cefr: String = "",
    val context: String = "",
    val source: MiningSource = MiningSource.DICTIONARY,
)

data class MiningBatch(
    val words: List<MinedWord>,
    val skipped: Int,
)

/**
 * Metinden bilinmeyen İngilizce kelimeleri çıkaran saf yardımcı. Sözlük ve
 * kitaplık erişimi çağıran taraftan enjekte edilir; Android bağımlılığı yoktur.
 */
object ContextMining {

    const val MIN_LENGTH = 3
    const val MAX_SENTENCE_LENGTH = 300
    private const val MAX_PHRASE_WORDS = 4

    val STOPWORDS: Set<String> = setOf(
        "a", "about", "above", "after", "again", "against", "all", "also", "am",
        "an", "and", "any", "are", "as", "at", "be", "because", "been", "before",
        "being", "below", "between", "both", "but", "by", "can", "could", "did",
        "do", "does", "doing", "done", "down", "during", "each", "either", "else",
        "ever", "every", "for", "from", "further", "had", "has", "have", "having",
        "he", "her", "here", "hers", "him", "his", "how", "i", "if", "in", "into",
        "is", "it", "its", "itself", "just", "may", "me", "might", "more", "most",
        "must", "my", "myself", "neither", "no", "nor", "not", "now", "of", "off",
        "on", "once", "one", "only", "onto", "or", "other", "our", "ours", "out",
        "over", "own", "per", "same", "she", "should", "so", "some", "such",
        "than", "that", "the", "their", "theirs", "them", "then", "there", "these",
        "they", "this", "those", "through", "to", "too", "under", "until", "up",
        "upon", "us", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "whose", "why", "will", "with", "within", "without",
        "would", "you", "your", "yours", "yourself", "i'm", "i've", "i'll", "i'd",
        "you're", "you've", "you'll", "you'd", "he's", "he'll", "he'd", "she's",
        "she'll", "she'd", "it's", "it'll", "we're", "we've", "we'll", "we'd",
        "they're", "they've", "they'll", "they'd", "that's", "there's", "what's",
        "don't", "doesn't", "didn't", "isn't", "aren't", "wasn't", "weren't",
        "can't", "couldn't", "won't", "wouldn't", "shouldn't", "haven't",
        "hasn't", "hadn't",
    )

    private val TOKEN_REGEX = Regex("""[\p{L}]+(?:['’\-][\p{L}]+)*""")
    private val WHITESPACE = Regex("""\s+""")
    private val CLOSING_MARKS = "\"')]}\u00BB\u201D"

    fun splitSentences(text: String): List<String> {
        val sentences = ArrayList<String>()
        val current = StringBuilder()

        fun flush() {
            var cleaned = current.toString()
                .replace(WHITESPACE, " ")
                .trim()
            while (cleaned.length > MAX_SENTENCE_LENGTH) {
                var cut = cleaned.lastIndexOf(' ', MAX_SENTENCE_LENGTH)
                if (cut <= 0) cut = MAX_SENTENCE_LENGTH
                val head = cleaned.substring(0, cut).trim()
                if (head.isNotEmpty()) sentences += head
                cleaned = cleaned.substring(cut).trim()
            }
            if (cleaned.isNotEmpty()) sentences += cleaned
            current.setLength(0)
        }

        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == '\n' || char == '\r') {
                flush()
                index++
                continue
            }
            current.append(char)
            if (char == '.' || char == '!' || char == '?') {
                var next = index + 1
                while (next < text.length && text[next] in CLOSING_MARKS) {
                    current.append(text[next])
                    next++
                }
                val following = if (next < text.length) text[next] else null
                if (following == null || following.isWhitespace()) {
                    flush()
                    index = next
                    continue
                }
                index = next
                continue
            }
            index++
        }
        flush()
        return sentences
    }

    /**
     * Cümledeki İngilizce kelimeleri (harf, kesme işareti ve tire) çıkarır.
     * Türkçe/ASCII dışı harf içeren token'lar tamamen atılır; böylece "İstanbul"
     * gibi kelimeler yanlışlıkla İngilizce parçalara bölünmez.
     */
    fun tokenize(sentence: String): List<String> {
        val tokens = ArrayList<String>()
        for (match in TOKEN_REGEX.findAll(sentence)) {
            val token = match.value.replace('’', '\'')
            if (token.isEmpty()) continue
            val ascii = token.all { it in 'a'..'z' || it in 'A'..'Z' || it == '\'' || it == '-' }
            if (ascii) tokens += token
        }
        return tokens
    }

    fun isEligible(token: String): Boolean {
        if (token.length < MIN_LENGTH) return false
        if (token.none { it.isLetter() }) return false
        if (!token.all { it in 'a'..'z' || it in 'A'..'Z' || it == '\'' || it == '-' }) {
            return false
        }
        return TermKey.normalize(token) !in STOPWORDS
    }

    /**
     * Metni cümlelere ayırır, bilinmeyen kelimeleri bulur ve her kelime için ilk
     * geçtiği cümleyi bağlam olarak saklar. [knownTerms] kitaplık + kurulu deste
     * terimlerini, [lookup] ise sözlük/deste anlam çözümlemesini temsil eder.
     */
    fun mine(
        text: String,
        knownTerms: Collection<String>,
        lookup: (String) -> MiningDefinition?,
    ): List<MinedWord> {
        val known = knownTerms.asSequence()
            .map(TermKey::normalize)
            .filter { it.isNotEmpty() }
            .toHashSet()
        val seen = HashSet<String>()
        val result = ArrayList<MinedWord>()
        for (sentence in splitSentences(text)) {
            val tokens = tokenize(sentence)
            var index = 0
            while (index < tokens.size) {
                val phrase = findPhrase(tokens, index, known, seen, lookup)
                if (phrase != null) {
                    phrase.first?.let { result += it.copy(context = sentence) }
                    index += phrase.second
                    continue
                }
                val token = tokens[index]
                val key = TermKey.normalize(token)
                if (key.length >= MIN_LENGTH && key !in STOPWORDS &&
                    key !in known && seen.add(key)
                ) {
                    lookup(key)?.let { definition ->
                        result += MinedWord(
                            term = definition.term.ifBlank { token },
                            meaning = definition.meaning,
                            pos = definition.pos,
                            cefr = definition.cefr,
                            context = sentence,
                            source = definition.source,
                        )
                    }
                }
                index++
            }
        }
        return result
    }

    /**
     * Aynı konumdan başlayan en uzun çok kelimeli sözlük/deste terimini arar.
     * Dönen ikili: bulunan kelime ve tüketilen token sayısı.
     */
    private fun findPhrase(
        tokens: List<String>,
        start: Int,
        known: Set<String>,
        seen: MutableSet<String>,
        lookup: (String) -> MiningDefinition?,
    ): Pair<MinedWord?, Int>? {
        val max = minOf(MAX_PHRASE_WORDS, tokens.size - start)
        if (max < 2) return null
        for (size in max downTo 2) {
            val key = tokens.subList(start, start + size)
                .joinToString(" ") { TermKey.normalize(it) }
            if (key.isEmpty()) continue
            if (key in known || key in seen) {
                seen += key
                return null to size
            }
            val definition = lookup(key)
            if (definition != null) {
                seen += key
                return MinedWord(
                    term = definition.term.ifBlank { key },
                    meaning = definition.meaning,
                    pos = definition.pos,
                    cefr = definition.cefr,
                    context = "",
                    source = definition.source,
                ) to size
            }
        }
        return null
    }

    /**
     * Toplu ekleme öncesi saf seçim: bilinen ve mükerrer terimleri ayıklar.
     */
    fun planAdd(
        candidates: List<MinedWord>,
        knownTerms: Collection<String>,
    ): MiningBatch {
        val known = knownTerms.asSequence()
            .map(TermKey::normalize)
            .filter { it.isNotEmpty() }
            .toHashSet()
        val accepted = ArrayList<MinedWord>(candidates.size)
        var skipped = 0
        for (candidate in candidates) {
            val key = TermKey.normalize(candidate.term)
            if (key.isEmpty() || !known.add(key)) {
                skipped++
                continue
            }
            accepted += candidate
        }
        return MiningBatch(accepted, skipped)
    }
}
