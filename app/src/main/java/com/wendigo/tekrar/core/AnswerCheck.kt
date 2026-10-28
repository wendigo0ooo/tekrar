package com.wendigo.tekrar.core

import java.util.Locale

object AnswerCheck {

    fun normalize(value: String): String {
        val lowered = value.lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return lowered.trim('.', ',', '!', '?', ';', ':', '"', '\'', '’', '“', '”', ' ')
    }

    fun variants(expected: String): List<String> {
        val cleaned = expected
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned
            .split('/', ',', ';', '|')
            .map { normalize(it) }
            .map { if (it.startsWith("to ")) it.removePrefix("to ").trim() else it }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun matches(input: String, expected: String): Boolean {
        val answer = normalize(input)
        if (answer.isEmpty()) return false
        val options = variants(expected)
        if (options.isEmpty()) return false
        if (options.any { it == answer }) return true
        return options.any { option ->
            option.length >= 5 &&
                kotlin.math.abs(option.length - answer.length) <= 1 &&
                levenshtein(answer, option) <= 1
        }
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
