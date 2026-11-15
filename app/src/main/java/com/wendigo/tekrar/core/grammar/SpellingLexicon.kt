package com.wendigo.tekrar.core.grammar

import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * Çevrimdışı yazım sözlüğü (kontrat 3.5).
 *
 * Kaynaklar: kurs/dictionary/grammar asset metinleri + Kotlin içindeki temel
 * A1 kelimeleri. Bilinmeyen bir kelime yalnızca yeterince güvenliyse
 * (yakın bir sözlük kelimesi veya yaygın hata haritası) işaretlenir; aksi
 * hâlde yanlış pozitif üretmemek için sessiz kalınır.
 */
class SpellingLexicon private constructor(
    private val counts: Map<String, Int>,
) {

    private val words: Set<String> = counts.keys

    val size: Int get() = words.size

    fun isKnown(raw: String): Boolean {
        val token = clean(raw)
        if (token.isEmpty()) return false
        val lower = token.lowercase(Locale.ROOT)
        if (lower in words) return true
        if (lower in CONTRACTIONS) return true
        if (lower.any { it == '\'' }) {
            if (!lower.endsWith("'s")) return false
            val base = lower.substringBefore('\'')
            return base.isNotEmpty() && (base in words || hasKnownStem(base))
        }
        return hasKnownStem(lower)
    }

    /**
     * Bilinmeyen kelime için öneri; kelime işaretlenmemeliyse null.
     * [sentenceInitial] cümle başındaki büyük harfin özel ad sayılmaması için.
     */
    fun suggestion(raw: String, sentenceInitial: Boolean = false): String? {
        val token = clean(raw)
        if (token.isEmpty()) return null
        val lower = token.lowercase(Locale.ROOT)
        if (lower in words || lower in CONTRACTIONS) return null

        COMMON_TYPOS[lower]?.let { return applyCase(token, it) }

        if (lower.any { it == '\'' }) {
            if (lower.endsWith("'s")) {
                val base = lower.substringBefore('\'')
                if (base.isNotEmpty() && (base in words || hasKnownStem(base))) return null
            }
            return null
        }
        if (lower.length < 3 || !lower.all { it in 'a'..'z' }) return null
        if (!sentenceInitial && token.first().isUpperCase()) return null
        if (hasKnownStem(lower)) return null

        val maxDistance = if (lower.length <= 4) 1 else 2
        val closest = closest(lower, maxDistance) ?: return null
        return applyCase(token, closest)
    }

    private fun closest(word: String, maxDistance: Int): String? {
        var best: String? = null
        var bestDistance = maxDistance + 1
        for (candidate in words) {
            if (abs(candidate.length - word.length) > maxDistance) continue
            val distance = boundedDistance(word, candidate, maxDistance)
            if (distance > maxDistance) continue
            if (distance < bestDistance || (distance == bestDistance && isBetter(candidate, best))) {
                best = candidate
                bestDistance = distance
            }
        }
        return best
    }

    private fun isBetter(candidate: String, current: String?): Boolean {
        if (current == null) return true
        val candidateCount = counts[candidate] ?: 0
        val currentCount = counts[current] ?: 0
        if (candidateCount != currentCount) return candidateCount > currentCount
        if (candidate.length != current.length) return candidate.length < current.length
        return candidate < current
    }

    private fun hasKnownStem(word: String): Boolean =
        stemCandidates(word).any { it.length >= 2 && it in words }

    private fun stemCandidates(word: String): List<String> {
        val candidates = ArrayList<String>(6)
        if (word.length > 4 && word.endsWith("ies")) {
            candidates += word.dropLast(3) + "y"
        }
        if (word.length > 4 && word.endsWith("ied")) {
            candidates += word.dropLast(3) + "y"
        }
        if (word.length > 3 && word.endsWith("es")) {
            candidates += word.dropLast(2)
        }
        if (word.length > 2 && word.endsWith("s") && !word.endsWith("ss")) {
            candidates += word.dropLast(1)
        }
        if (word.length > 3 && word.endsWith("ed")) {
            val stem = word.dropLast(2)
            candidates += stem
            candidates += word.dropLast(1)
            if (stem.length > 2 && stem[stem.lastIndex] == stem[stem.lastIndex - 1]) {
                candidates += stem.dropLast(1)
            }
        }
        if (word.length > 4 && word.endsWith("ing")) {
            val stem = word.dropLast(3)
            candidates += stem
            candidates += stem + "e"
            if (stem.length > 2 && stem[stem.lastIndex] == stem[stem.lastIndex - 1]) {
                candidates += stem.dropLast(1)
            }
        }
        return candidates
    }

    companion object {

        private const val FUNCTION_WORD_COUNT = 100
        private const val CORRECTION_COUNT = 40

        private val TOKEN = Regex("[A-Za-z]+(?:'[A-Za-z]+)*")

        private val TURKISH_LETTERS = setOf(
            'ı', 'ş', 'ğ', 'ü', 'ö', 'ç',
            'İ', 'Ş', 'Ğ', 'Ü', 'Ö', 'Ç',
        )

        /** Geçerli kabul edilen kısaltmalar (küçük harf). */
        val CONTRACTIONS: Set<String> = setOf(
            "i'm", "you're", "he's", "she's", "it's", "we're", "they're",
            "i've", "you've", "we've", "they've",
            "i'll", "you'll", "he'll", "she'll", "it'll", "we'll", "they'll",
            "i'd", "you'd", "he'd", "she'd", "it'd", "we'd", "they'd",
            "isn't", "aren't", "wasn't", "weren't",
            "don't", "doesn't", "didn't",
            "can't", "couldn't", "won't", "wouldn't", "shouldn't",
            "haven't", "hasn't", "hadn't", "mustn't",
            "let's", "that's", "there's", "what's", "who's", "where's",
            "how's", "here's", "when's", "why's", "nobody's", "everybody's",
        )

        /** Yaygın yazım hataları: yanlış → doğru. */
        val COMMON_TYPOS: Map<String, String> = mapOf(
            "im" to "I'm",
            "dont" to "don't",
            "cant" to "can't",
            "wont" to "won't",
            "thats" to "that's",
            "alot" to "a lot",
            "becuase" to "because",
            "beacuse" to "because",
            "tommorow" to "tomorrow",
            "tommorrow" to "tomorrow",
            "studnt" to "student",
            "studet" to "student",
            "teh" to "the",
            "adn" to "and",
            "recieve" to "receive",
            "seperate" to "separate",
            "definately" to "definitely",
            "untill" to "until",
            "wich" to "which",
            "freind" to "friend",
            "beleive" to "believe",
            "wierd" to "weird",
            "acheive" to "achieve",
            "enviroment" to "environment",
            "goverment" to "government",
            "yestarday" to "yesterday",
            "minuts" to "minutes",
            "secounds" to "seconds",
            "beutiful" to "beautiful",
            "buisness" to "business",
            "wanna" to "want to",
            "gonna" to "going to",
            "gotta" to "have to",
            "cuz" to "because",
            "pls" to "please",
            "plz" to "please",
            "thx" to "thanks",
            "bday" to "birthday",
            "eng" to "English",
            "englsh" to "English",
            "turkish" to "Turkish",
        )

        /**
         * Kotlin içinde sabit temel A1 kelimeleri: zamirler, artikel,
         * yardımcı fiiller, edatlar, bağlaçlar, soru kelimeleri, sık zarflar,
         * sayılar, gün/ay, aile ve günlük temel kelimeler.
         */
        val FUNCTION_WORDS: Set<String> = setOf(
            // pronouns / determiners
            "i", "you", "he", "she", "it", "we", "they", "me", "him", "her",
            "us", "them", "my", "your", "his", "its", "our", "their", "mine",
            "yours", "hers", "ours", "theirs", "myself", "yourself", "himself",
            "herself", "itself", "ourselves", "yourselves", "themselves",
            "this", "that", "these", "those", "who", "whom", "whose", "which",
            "what", "someone", "somebody", "something", "anyone", "anybody",
            "anything", "everyone", "everybody", "everything", "nobody",
            "nothing", "none", "all", "some", "any", "both", "each", "other",
            "another", "such",
            // articles
            "a", "an", "the",
            // auxiliary / modal verbs
            "am", "is", "are", "was", "were", "be", "been", "being", "do",
            "does", "did", "have", "has", "had", "can", "could", "will",
            "would", "shall", "should", "may", "might", "must", "need", "ought",
            // prepositions
            "in", "on", "at", "to", "from", "by", "with", "without", "for",
            "of", "about", "under", "over", "above", "below", "between",
            "among", "into", "onto", "through", "during", "before", "after",
            "up", "down", "out", "off", "near", "next", "across", "along",
            "around", "behind", "beside", "beyond", "inside", "outside",
            "toward", "towards", "upon", "within", "since", "until", "till",
            "per", "via", "against", "except", "than", "like", "as",
            // conjunctions
            "and", "but", "or", "so", "because", "if", "when", "while",
            "although", "though", "unless", "whether", "nor", "yet", "either",
            "neither",
            // question words
            "where", "why", "how",
            // adverbs / common words
            "not", "no", "yes", "very", "too", "also", "just", "only", "now",
            "then", "here", "there", "always", "usually", "often", "sometimes",
            "never", "ever", "again", "already", "still", "soon", "later",
            "today", "tomorrow", "yesterday", "tonight", "early", "late",
            "well", "fast", "quickly", "slowly", "really", "much", "many",
            "more", "most", "less", "least", "few", "little", "maybe",
            "perhaps", "please", "sorry", "thank", "thanks", "hello", "hi",
            "hey", "goodbye", "bye", "okay", "ok", "sure", "together", "alone",
            "home", "back", "away", "first", "second", "third", "last", "once",
            "twice", "also", "too",
            // numbers
            "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
            "twenty", "thirty", "forty", "fifty", "sixty", "seventy",
            "eighty", "ninety", "hundred", "thousand", "million",
            // days / months
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
            "sunday", "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december",
            // family / people
            "mother", "father", "mom", "dad", "mum", "brother", "sister",
            "son", "daughter", "family", "friend", "child", "children", "boy",
            "girl", "man", "woman", "men", "women", "people", "person", "name",
            "age",
            // basic A1 adjectives
            "old", "new", "good", "bad", "big", "small", "long", "short",
            "tall", "happy", "sad", "tired", "hungry", "thirsty", "ready",
            "fine", "nice", "great", "beautiful", "easy", "hard", "difficult",
            "hot", "cold", "warm", "cool", "young", "busy", "free", "full",
            "empty", "open", "closed", "right", "wrong", "same", "different",
            "favorite", "favourite", "english", "turkish", "turkey",
            // school / home / city
            "school", "teacher", "student", "class", "lesson", "book",
            "notebook", "pen", "pencil", "desk", "chair", "table", "door",
            "window", "house", "room", "kitchen", "bathroom", "garden", "car",
            "bus", "train", "plane", "bike", "city", "town", "village",
            "street", "road", "shop", "store", "market", "restaurant", "cafe",
            "hotel", "hospital", "bank", "park", "station", "airport", "work",
            "job", "office", "money", "price", "time", "hour", "minute", "day",
            "week", "month", "year", "morning", "afternoon", "evening", "night",
            "birthday", "holiday", "vacation", "weather", "sun", "rain", "snow",
            "wind", "water", "food", "bread", "milk", "tea", "coffee", "apple",
            "orange", "banana", "egg", "cheese", "chicken", "rice", "soup",
            "salad", "fruit", "vegetable", "breakfast", "lunch", "dinner",
            "meat", "fish", "cake", "sugar", "salt", "oil",
            // common verbs
            "say", "get", "make", "go", "know", "take", "see", "come", "think",
            "look", "want", "use", "find", "give", "tell", "call", "try",
            "ask", "feel", "become", "leave", "put", "mean", "keep", "let",
            "begin", "seem", "help", "talk", "turn", "start", "show", "hear",
            "play", "run", "move", "live", "believe", "hold", "bring",
            "happen", "write", "provide", "sit", "stand", "lose", "pay",
            "meet", "include", "continue", "set", "learn", "change", "lead",
            "understand", "watch", "follow", "stop", "create", "speak", "read",
            "allow", "add", "spend", "grow", "win", "offer", "remember",
            "love", "consider", "appear", "buy", "wait", "serve", "die",
            "send", "expect", "build", "stay", "fall", "cut", "reach", "kill",
            "remain", "suggest", "raise", "pass", "sell", "require", "report",
            "decide", "pull", "return", "explain", "hope", "develop", "carry",
            "break", "receive", "agree", "support", "hit", "produce", "eat",
            "cover", "catch", "draw", "choose", "cause", "point", "listen",
            "wear", "practice", "practise", "finish", "prefer", "travel",
            "visit", "study", "teach", "cook", "clean", "wash", "drive",
            "ride", "swim", "sing", "dance", "laugh", "cry", "smile", "sleep",
            "wake", "dream", "walk", "spell", "repeat", "answer", "check",
        )

        /** Metinlerden sözlük kurar; Türkçe karakterli metinleri atlar. */
        fun build(texts: Iterable<String>): SpellingLexicon {
            val counts = HashMap<String, Int>()
            for (text in texts) {
                if (containsTurkishLetter(text)) continue
                for (match in TOKEN.findAll(text)) {
                    val lower = match.value.lowercase(Locale.ROOT)
                    if (lower.isEmpty()) continue
                    if (lower.any { it == '\'' } && lower !in CONTRACTIONS) continue
                    counts[lower] = (counts[lower] ?: 0) + 1
                }
            }
            FUNCTION_WORDS.forEach { word ->
                counts[word] = (counts[word] ?: 0) + FUNCTION_WORD_COUNT
            }
            COMMON_TYPOS.values.forEach { correction ->
                correction.lowercase(Locale.ROOT).split(' ').forEach { word ->
                    if (word.all { it in 'a'..'z' } && word.length >= 2) {
                        counts[word] = (counts[word] ?: 0) + CORRECTION_COUNT
                    }
                }
            }
            return SpellingLexicon(counts)
        }

        fun of(words: Iterable<String>): SpellingLexicon = build(words)

        fun containsTurkishLetter(text: String): Boolean =
            text.any { it in TURKISH_LETTERS }

        private fun clean(raw: String): String =
            raw.trim().trim('\'')

        private fun applyCase(original: String, suggestion: String): String {
            if (original.firstOrNull()?.isUpperCase() != true) return suggestion
            if (suggestion.isEmpty() || suggestion.first().isUpperCase()) return suggestion
            return suggestion.replaceFirstChar { it.uppercaseChar() }
        }

        /** Sınırlı Levenshtein; [max] aşılırsa erken döner. */
        private fun boundedDistance(a: String, b: String, max: Int): Int {
            if (abs(a.length - b.length) > max) return max + 1
            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)
            for (i in 1..a.length) {
                current[0] = i
                var rowMin = current[0]
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    current[j] = min(
                        min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost,
                    )
                    if (current[j] < rowMin) rowMin = current[j]
                }
                if (rowMin > max) return max + 1
                val swap = previous
                previous = current
                current = swap
            }
            return previous[b.length]
        }
    }
}
