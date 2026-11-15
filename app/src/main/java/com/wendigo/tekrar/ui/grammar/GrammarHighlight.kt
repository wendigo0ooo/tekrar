package com.wendigo.tekrar.ui.grammar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.wendigo.tekrar.core.grammar.GrammarIssue

/**
 * Hatalı aralıkları kırmızı + altı çizili gösterir.
 *
 * `GrammarChecker` aralıkları orijinal metne göre verir. Noktalama ekleme
 * issue'larında `start == end` olabilir; bunlar metinde karakter işaretlemez,
 * hata satırında "(eksik)" olarak anlatılır. Çakışan aralıklar birleştirilir.
 */
internal fun highlightedSentence(
    original: String,
    issues: List<GrammarIssue>,
    color: Color,
): AnnotatedString {
    val ranges = mergedIssueRanges(issues, original.length)
    return buildAnnotatedString {
        append(original)
        ranges.forEach { range ->
            addStyle(
                SpanStyle(
                    color = color,
                    background = color.copy(alpha = 0.10f),
                    textDecoration = TextDecoration.Underline,
                ),
                range.first,
                range.last + 1,
            )
        }
    }
}

/** Aynı hatanın tekrarlarını özet ekranında tek satıra indirir. */
internal fun distinctIssues(issues: List<GrammarIssue>): List<GrammarIssue> =
    issues.distinctBy { Triple(it.wrong, it.correct, it.noteTr) }

/** Metin sınırlarına kırpılmış, çakışmayan (bitişik olanlar birleşik) aralıklar. */
internal fun mergedIssueRanges(
    issues: List<GrammarIssue>,
    textLength: Int,
): List<IntRange> {
    if (textLength <= 0 || issues.isEmpty()) return emptyList()
    val ranges = issues.mapNotNull { issue ->
        val start = issue.start.coerceIn(0, textLength)
        val end = issue.end.coerceIn(0, textLength)
        if (end > start) start until end else null
    }.sortedBy { it.first }
    if (ranges.isEmpty()) return emptyList()

    val merged = ArrayList<IntRange>(ranges.size)
    var current: IntRange? = null
    for (range in ranges) {
        val active = current
        current = when {
            active == null -> range
            range.first <= active.last + 1 -> active.first..maxOf(active.last, range.last)
            else -> {
                merged += active
                range
            }
        }
    }
    current?.let { merged += it }
    return merged
}
