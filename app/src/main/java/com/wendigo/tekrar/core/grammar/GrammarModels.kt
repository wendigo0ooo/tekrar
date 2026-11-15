package com.wendigo.tekrar.core.grammar

/**
 * Gramer modülü içerik ve sonuç modeli. JSON şeması için
 * `docs/gramer-kontrat.md` bölüm 2.
 *
 * Modeller saf Kotlin'dir; Android/Room/AI bağımlılığı yoktur.
 */

data class GrammarIndex(
    val schema: Int,
    val language: String,
    val native: String,
    val topics: List<GrammarTopicRef>,
)

data class GrammarTopicRef(
    val id: String,
    val titleTr: String,
    val subtitleTr: String,
    val cefr: String,
    val file: String,
    val icon: String? = null,
    val color: String? = null,
    val estMin: Int = 0,
    val linkedLessons: List<String> = emptyList(),
)

data class GrammarTopic(
    val schema: Int,
    val id: String,
    val cefr: String,
    val titleTr: String,
    val subtitleTr: String,
    val objectiveTr: String,
    val estMin: Int,
    val sourceNotes: List<String>,
    val steps: List<GrammarStep>,
    val rules: List<GrammarRule>,
    val write: GrammarWrite,
)

data class GrammarExample(
    val en: String,
    val tr: String,
)

/** Konu anlatım kartı tipleri (kontrat 2.2). */
enum class GrammarStepType(val wire: String) {
    HOOK("hook"),
    RULE("rule"),
    EXAMPLES("examples"),
    MISTAKE("mistake"),
    TIP("tip"),
    ;

    companion object {
        fun fromWire(value: String): GrammarStepType? =
            entries.firstOrNull { it.wire == value }
    }
}

sealed interface GrammarStep {
    val type: GrammarStepType
    val titleTr: String
}

data class GrammarHookStep(
    override val titleTr: String,
    val bodyTr: String,
    val examples: List<GrammarExample>,
) : GrammarStep {
    override val type: GrammarStepType get() = GrammarStepType.HOOK
}

data class GrammarRuleStep(
    override val titleTr: String,
    val bodyTr: String,
    val pattern: String,
    val examples: List<GrammarExample>,
) : GrammarStep {
    override val type: GrammarStepType get() = GrammarStepType.RULE
}

data class GrammarExamplesStep(
    override val titleTr: String,
    val bodyTr: String? = null,
    val examples: List<GrammarExample>,
) : GrammarStep {
    override val type: GrammarStepType get() = GrammarStepType.EXAMPLES
}

data class GrammarMistakeStep(
    override val titleTr: String,
    val wrongEn: String,
    val rightEn: String,
    val bodyTr: String,
) : GrammarStep {
    override val type: GrammarStepType get() = GrammarStepType.MISTAKE
}

data class GrammarTipStep(
    override val titleTr: String,
    val bodyTr: String,
    val example: GrammarExample? = null,
) : GrammarStep {
    override val type: GrammarStepType get() = GrammarStepType.TIP
}

data class GrammarWrite(
    val introTr: String,
    val tasks: List<GrammarWriteTask>,
)

data class GrammarWriteTask(
    val id: String,
    val promptTr: String,
    val hintTr: String? = null,
    val sample: String,
    val minWords: Int,
    val expect: List<GrammarExpectation>,
)

data class GrammarExpectation(
    val pattern: String,
    val anchor: String,
    val noteTr: String,
)

data class GrammarRule(
    val pattern: String,
    val replace: String,
    val noteTr: String,
)

enum class GrammarIssueKind {
    GRAMMAR,
    SPELLING,
    CAPITALIZATION,
    PUNCTUATION,
    MISSING,
}

data class GrammarIssue(
    /** Orijinal cümlede karakter aralığı (başlangıç, dahil). */
    val start: Int,
    /** Orijinal cümlede karakter aralığı (bitiş, hariç). */
    val end: Int,
    val wrong: String,
    val correct: String,
    val noteTr: String,
    val kind: GrammarIssueKind,
)

data class GrammarCheckResult(
    val original: String,
    val corrected: String,
    val issues: List<GrammarIssue>,
    val correct: Boolean,
    val scorePercent: Int,
    val hintTr: String? = null,
)

data class GrammarSessionSummary(
    val taskCount: Int,
    val checkedCount: Int,
    val correctCount: Int,
    val averageScore: Int,
    val issues: List<GrammarIssue>,
)
