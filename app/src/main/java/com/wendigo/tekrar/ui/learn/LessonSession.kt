package com.wendigo.tekrar.ui.learn

import com.wendigo.tekrar.core.learn.Exercise
import com.wendigo.tekrar.core.learn.ExerciseGrader
import com.wendigo.tekrar.core.learn.GradeResult
import com.wendigo.tekrar.core.learn.Lesson
import com.wendigo.tekrar.core.learn.TeachCard
import com.wendigo.tekrar.core.learn.UserAnswer
import kotlin.math.roundToInt

/**
 * Ders oturumunun saf Kotlin durumu. Android/Compose bağımlılığı yoktur.
 *
 * Zaman çizelgesi: önce [teachCards] (0-2 kart), sonra [exercises]. Öğretim
 * kartında yalnız "devam" vardır; alıştırmada cevap gönderilmeden ilerlenemez.
 * Her cevap anında [ExerciseGrader] ile puanlanır ve [answers] listesine yazılır.
 */
enum class LessonPhase {
    TEACH,
    EXERCISE,
    FEEDBACK,
    FINISHED,
}

data class LessonAnswer(
    val exerciseIndex: Int,
    val correct: Boolean,
    val scorePercent: Int,
    val expectedAnswer: String,
)

class LessonSession(
    private val teachCards: List<TeachCard>,
    private val exercises: List<Exercise>,
) {

    constructor(lesson: Lesson) : this(lesson.teach, lesson.exercises)

    /** Zaman çizelgesindeki konum: 0..[totalSteps]. */
    var stepIndex: Int = 0
        private set

    var phase: LessonPhase = when {
        teachCards.isNotEmpty() -> LessonPhase.TEACH
        exercises.isNotEmpty() -> LessonPhase.EXERCISE
        else -> LessonPhase.FINISHED
    }
        private set

    private val recordedAnswers = mutableListOf<LessonAnswer>()

    val answers: List<LessonAnswer> get() = recordedAnswers.toList()

    var lastGrade: GradeResult? = null
        private set

    val teachCount: Int get() = teachCards.size

    val exerciseCount: Int get() = exercises.size

    val totalSteps: Int get() = teachCards.size + exercises.size

    val stepNumber: Int get() = (stepIndex + 1).coerceAtMost(totalSteps.coerceAtLeast(1))

    val currentTeach: TeachCard?
        get() = if (phase == LessonPhase.TEACH) teachCards.getOrNull(stepIndex) else null

    val currentExercise: Exercise?
        get() = when (phase) {
            LessonPhase.EXERCISE, LessonPhase.FEEDBACK -> exercises.getOrNull(stepIndex - teachCards.size)
            else -> null
        }

    /** Aktif alıştırmanın 1 tabanlı sırası; öğretim kartında 0. */
    val currentExerciseNumber: Int
        get() = when {
            currentExercise != null -> (stepIndex - teachCards.size + 1).coerceAtLeast(1)
            phase == LessonPhase.FINISHED -> exerciseCount
            else -> 0
        }

    val answeredCount: Int get() = recordedAnswers.size

    val correctCount: Int get() = recordedAnswers.count { it.correct }

    val accuracyPercent: Int
        get() = if (recordedAnswers.isEmpty()) {
            0
        } else {
            (100.0 * correctCount / recordedAnswers.size).roundToInt().coerceIn(0, 100)
        }

    /**
     * Oturum puanı: alıştırma puanlarının ortalaması. `match` gibi kısmi
     * puanlanan tiplerde [accuracyPercent] tam doğru sayısını, bu değer ise
     * ortalama başarıyı gösterir.
     */
    val score: Int
        get() = if (recordedAnswers.isEmpty()) {
            0
        } else {
            (recordedAnswers.sumOf { it.scorePercent }.toDouble() / recordedAnswers.size)
                .roundToInt()
                .coerceIn(0, 100)
        }

    /**
     * Üst ilerleme çubuğu: cevaplanan alıştırma / toplam alıştırma.
     * Öğretim kartlarında 0, ders bittiğinde 1'dir.
     */
    val progress: Float
        get() = when {
            phase == LessonPhase.FINISHED -> 1f
            phase == LessonPhase.TEACH -> 0f
            exerciseCount <= 0 -> 0f
            else -> {
                val position = (stepIndex - teachCards.size).coerceAtLeast(0)
                val completed = if (phase == LessonPhase.FEEDBACK) position + 1 else position
                (completed.toFloat() / exerciseCount).coerceIn(0f, 1f)
            }
        }

    val isFinished: Boolean get() = phase == LessonPhase.FINISHED

    /**
     * Cevabı puanlar. Yalnız [LessonPhase.EXERCISE] aşamasında çalışır; aksi
     * durumda `null` döner ve durum değişmez (çift gönderim koruması).
     */
    fun submit(answer: UserAnswer): GradeResult? {
        if (phase != LessonPhase.EXERCISE) return null
        val exercise = currentExercise ?: return null
        val result = ExerciseGrader.grade(exercise, answer)
        lastGrade = result
        phase = LessonPhase.FEEDBACK
        recordedAnswers += LessonAnswer(
            exerciseIndex = (stepIndex - teachCards.size).coerceAtLeast(0),
            correct = result.correct,
            scorePercent = result.scorePercent,
            expectedAnswer = result.expectedAnswer,
        )
        return result
    }

    /**
     * Bir sonraki adıma geçer. Öğretimde "devam", geri bildirimde "sonraki
     * alıştırma" anlamındadır. Alıştırma aşamasında cevap beklenir; çağrı
     * yok sayılır.
     */
    fun next(): LessonPhase {
        when (phase) {
            LessonPhase.TEACH -> {
                lastGrade = null
                stepIndex += 1
                phase = when {
                    stepIndex < teachCards.size -> LessonPhase.TEACH
                    exercises.isNotEmpty() -> LessonPhase.EXERCISE
                    else -> LessonPhase.FINISHED
                }
                if (phase == LessonPhase.FINISHED) stepIndex = totalSteps
            }

            LessonPhase.FEEDBACK -> {
                lastGrade = null
                stepIndex += 1
                phase = if (stepIndex < totalSteps) {
                    LessonPhase.EXERCISE
                } else {
                    LessonPhase.FINISHED
                }
                if (phase == LessonPhase.FINISHED) stepIndex = totalSteps
            }

            LessonPhase.EXERCISE, LessonPhase.FINISHED -> Unit
        }
        return phase
    }
}
