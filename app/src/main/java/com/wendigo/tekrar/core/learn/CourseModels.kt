package com.wendigo.tekrar.core.learn

/**
 * Dil Okulu içerik modeli. JSON şeması için `docs/dil-okulu-kontrat.md` bölüm 2.
 * Modeller saf Kotlin'dir; Android/Room bağımlılığı yoktur.
 */

data class CourseIndex(
    val schema: Int,
    val language: String,
    val native: String,
    val levels: List<CourseLevel>,
)

data class CourseLevel(
    val id: String,
    val title: String,
    val units: List<UnitRef>,
)

data class UnitRef(
    val id: String,
    val file: String,
)

data class CourseUnit(
    val schema: Int,
    val id: String,
    val cefr: String,
    val title: String,
    val subtitle: String,
    val icon: String? = null,
    val color: String? = null,
    val lessons: List<LessonRef>,
    val roleplay: RoleplayRef? = null,
)

data class LessonRef(
    val id: String,
    val title: String,
    val file: String,
    val estMin: Int,
)

data class RoleplayRef(
    val id: String,
    val title: String,
    val file: String,
    val estMin: Int,
)

data class Lesson(
    val schema: Int,
    val id: String,
    val unit: String,
    val cefr: String,
    val title: String,
    val objectiveTr: String,
    val estMin: Int,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarNote> = emptyList(),
    val dialogue: Dialogue? = null,
    val teach: List<TeachCard> = emptyList(),
    val exercises: List<Exercise>,
)

data class VocabItem(
    val term: String,
    val meaningTr: String,
    val pos: String,
    val example: String,
    val exampleTr: String,
    val ipa: String? = null,
)

data class GrammarNote(
    val titleTr: String,
    val explanationTr: String,
    val examples: List<Example>,
)

data class Example(
    val en: String,
    val tr: String,
)

data class Dialogue(
    val title: String,
    val lines: List<DialogueLine>,
)

data class DialogueLine(
    val speaker: String,
    val en: String,
    val tr: String,
)

data class TeachCard(
    val titleTr: String,
    val bodyTr: String,
    val examples: List<Example>,
    val tipTr: String? = null,
)

data class MatchPair(
    val left: String,
    val right: String,
)

/** Desteklenen v1 alıştırma tipleri (kontrat 2.4). */
sealed interface Exercise {
    val promptTr: String
    val explanationTr: String?
}

data class Choice(
    override val promptTr: String,
    val options: List<String>,
    val answer: Int,
    override val explanationTr: String? = null,
) : Exercise

data class ListenChoice(
    override val promptTr: String,
    val audioText: String,
    val options: List<String>,
    val answer: Int,
    override val explanationTr: String? = null,
) : Exercise

data class DialogueChoice(
    override val promptTr: String,
    val audioText: String? = null,
    val options: List<String>,
    val answer: Int,
    override val explanationTr: String? = null,
) : Exercise

data class Match(
    override val promptTr: String,
    val pairs: List<MatchPair>,
    override val explanationTr: String? = null,
) : Exercise

data class WordOrder(
    override val promptTr: String,
    val tokens: List<String>,
    val answer: List<String>,
    val translationTr: String? = null,
    override val explanationTr: String? = null,
) : Exercise

data class Cloze(
    override val promptTr: String,
    val sentence: String,
    val options: List<String>? = null,
    val answer: Int? = null,
    val answers: List<String>? = null,
    override val explanationTr: String? = null,
) : Exercise

data class Translate(
    override val promptTr: String,
    val answers: List<String>,
    val hintTr: String? = null,
    override val explanationTr: String? = null,
) : Exercise

data class Speak(
    override val promptTr: String,
    val target: String,
    val hintTr: String? = null,
    override val explanationTr: String? = null,
) : Exercise

data class Write(
    override val promptTr: String,
    val minWords: Int,
    val keywords: List<String> = emptyList(),
    val sample: String? = null,
    val rubricTr: String? = null,
    override val explanationTr: String? = null,
) : Exercise

data class RoleplayScenario(
    val schema: Int,
    val id: String,
    val unit: String,
    val cefr: String,
    val title: String,
    val settingTr: String,
    val roleEn: String,
    val goalTr: String,
    val openingEn: String,
    val targetPhrases: List<String>,
    val suggestedTurns: Int,
    val successCriteriaTr: List<String>,
)

/** Kullanıcıdan gelen cevap. UI, alıştırma tipine uygun varyantı üretir. */
sealed interface UserAnswer {
    data class Choice(val index: Int) : UserAnswer
    data class Pairs(val values: Map<String, String>) : UserAnswer
    data class Tokens(val values: List<String>) : UserAnswer
    data class Text(val value: String) : UserAnswer
    data class Speech(val transcript: String) : UserAnswer
}

data class GradeResult(
    val correct: Boolean,
    val scorePercent: Int,
    val expectedAnswer: String,
    val feedbackTr: String,
)
