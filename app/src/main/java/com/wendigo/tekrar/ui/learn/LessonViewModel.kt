package com.wendigo.tekrar.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.audio.Speaker
import com.wendigo.tekrar.core.learn.CoursePath
import com.wendigo.tekrar.core.learn.CourseUnit
import com.wendigo.tekrar.core.learn.Lesson
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.core.learn.UserAnswer
import com.wendigo.tekrar.repo.CourseProgressRepository
import com.wendigo.tekrar.repo.CourseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ders ekranı durumu. Oturum mantığı [LessonSession] içindedir; bu sınıf
 * yükleme, kaydetme, TTS ve sonuç üretimini yönetir. AI kullanmaz.
 */
class LessonViewModel(
    private val courseRepository: CourseRepository,
    private val courseProgress: CourseProgressRepository,
    private val speaker: Speaker,
) : ViewModel() {

    private data class ResolvedLesson(
        val unit: CourseUnit,
        val lesson: Lesson,
    )

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var session: LessonSession? = null
    private var lesson: Lesson? = null
    private var lastUnitId: String = ""
    private var lastLessonId: String = ""
    private var lastLesson: Lesson? = null
    private var loadJob: Job? = null
    private var completed = false
    private var saving = false

    /**
     * Rota argümanlarıyla ders açar. [unitId] boş bırakılırsa tüm kurs
     * index'inde aranır; normalde Öğren ekranından gelen unitId verilir.
     */
    fun start(unitId: String, lessonId: String) {
        if (lessonId.isBlank()) {
            _state.value = LessonUiState(loading = false, loadFailed = true)
            return
        }
        // Aynı ders ekranı yeniden kurulduğunda (döndürme/tema) oturumu sıfırlama.
        if (lesson?.id == lessonId && session != null) return
        lastUnitId = unitId
        lastLessonId = lessonId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = LessonUiState(loading = true)
            try {
                val resolved = withContext(Dispatchers.IO) { resolveLesson(unitId, lessonId) }
                lastUnitId = resolved.unit.id
                lastLessonId = resolved.lesson.id
                begin(resolved.lesson, resolved.unit.title)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                _state.value = LessonUiState(loading = false, loadFailed = true)
            }
        }
    }

    /** Önceden yüklenmiş bir dersle başlatmak için (test/DI kolaylığı). */
    fun start(lesson: Lesson, unitTitle: String = "") {
        loadJob?.cancel()
        lastLesson = lesson
        lastUnitId = lesson.unit
        lastLessonId = lesson.id
        begin(lesson, unitTitle)
    }

    fun retryStart() {
        if (lastLessonId.isNotBlank()) {
            start(lastUnitId, lastLessonId)
        } else {
            lastLesson?.let { start(it) }
        }
    }

    fun continueFromTeach() {
        val current = session ?: return
        if (current.phase != LessonPhase.TEACH) return
        advance()
    }

    fun next() {
        val current = session ?: return
        if (current.phase != LessonPhase.FEEDBACK) return
        advance()
    }

    /** Sonuç ekranındaki kaydetme hatasını yeniden dener. */
    fun finish() {
        val current = session ?: return
        if (current.phase == LessonPhase.FINISHED) saveCompletion()
    }

    fun submitChoice(index: Int) = submit(UserAnswer.Choice(index))

    fun submitMatch(values: Map<String, String>) = submit(UserAnswer.Pairs(values))

    fun submitTokens(tokens: List<String>) = submit(UserAnswer.Tokens(tokens))

    fun submitText(text: String) = submit(UserAnswer.Text(text))

    fun submitSpeech(transcript: String) = submit(UserAnswer.Speech(transcript))

    fun speak(text: String) {
        if (text.isNotBlank()) speaker.speak(text)
    }

    private fun begin(loadedLesson: Lesson, unitTitle: String) {
        val newSession = LessonSession(loadedLesson)
        session = newSession
        lesson = loadedLesson
        lastLesson = loadedLesson
        completed = false
        saving = false
        _state.value = LessonUiState(
            loading = false,
            loadFailed = false,
            lessonTitle = loadedLesson.title,
            unitTitle = unitTitle.ifBlank { loadedLesson.unit },
            phase = newSession.phase,
            stepIndex = newSession.stepIndex,
            stepTotal = newSession.totalSteps,
            exerciseNumber = newSession.currentExerciseNumber,
            exerciseTotal = newSession.exerciseCount,
            progress = newSession.progress,
            teachCard = newSession.currentTeach,
            exercise = newSession.currentExercise,
        )
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    courseProgress.markOpened(loadedLesson.id, loadedLesson.unit)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                // Açılma kaydı başarısız olsa da ders çalışmaya devam eder.
            }
        }
        if (newSession.phase == LessonPhase.FINISHED) {
            saveCompletion()
        }
    }

    private fun submit(answer: UserAnswer) {
        val current = session ?: return
        if (current.phase != LessonPhase.EXERCISE) return
        try {
            current.submit(answer)
            publishSession()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            _state.update { it.copy(submitFailed = true) }
        }
    }

    private fun advance() {
        val current = session ?: return
        try {
            current.next()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            _state.update { it.copy(submitFailed = true) }
            return
        }
        publishSession()
        if (current.phase == LessonPhase.FINISHED) {
            saveCompletion()
        }
    }

    private fun publishSession() {
        val current = session ?: return
        _state.update { previous ->
            previous.copy(
                loading = false,
                loadFailed = false,
                phase = current.phase,
                stepIndex = current.stepIndex,
                stepTotal = current.totalSteps,
                exerciseNumber = current.currentExerciseNumber,
                exerciseTotal = current.exerciseCount,
                progress = current.progress,
                teachCard = current.currentTeach,
                exercise = current.currentExercise,
                feedback = current.lastGrade?.let { grade ->
                    LessonFeedbackUi(
                        correct = grade.correct,
                        expectedAnswer = grade.expectedAnswer,
                        feedbackTr = grade.feedbackTr,
                        scorePercent = grade.scorePercent,
                    )
                },
                submitFailed = false,
            )
        }
    }

    private fun saveCompletion() {
        val currentLesson = lesson ?: return
        val currentSession = session ?: return
        if (currentSession.phase != LessonPhase.FINISHED) return
        if (completed || saving) return
        saving = true
        val accuracy = currentSession.accuracyPercent
        val correct = currentSession.correctCount
        val total = currentSession.answeredCount
        _state.update {
            it.copy(
                savingResult = true,
                result = null,
                submitFailed = false,
            )
        }
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    courseProgress.completeLesson(
                        lessonId = currentLesson.id,
                        unitId = currentLesson.unit,
                        scorePercent = accuracy,
                    )
                }
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                false
            }

            if (!saved) {
                saving = false
                _state.update {
                    it.copy(
                        savingResult = false,
                        result = LessonResultUiState(
                            accuracyPercent = accuracy,
                            correctCount = correct,
                            totalCount = total,
                            learnedWordCount = currentLesson.vocab.size,
                            addedToReview = false,
                            nextStep = LessonNextStep.None,
                            saveFailed = true,
                        ),
                    )
                }
                return@launch
            }

            completed = true
            val seedResult = try {
                withContext(Dispatchers.IO) {
                    courseRepository.seedLessonVocab(currentLesson)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                null
            }
            val seedFailed = seedResult == null
            val addedToReview = seedResult != null &&
                (seedResult.inserted + seedResult.skipped) > 0
            val nextStepValue = try {
                withContext(Dispatchers.IO) { nextStep(currentLesson) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                LessonNextStep.None
            }

            saving = false
            _state.update {
                it.copy(
                    savingResult = false,
                    result = LessonResultUiState(
                        accuracyPercent = accuracy,
                        correctCount = correct,
                        totalCount = total,
                        learnedWordCount = currentLesson.vocab.size,
                        addedToReview = addedToReview,
                        nextStep = nextStepValue,
                        seedFailed = seedFailed,
                    ),
                )
            }
        }
    }

    private suspend fun resolveLesson(unitId: String, lessonId: String): ResolvedLesson {
        val index = courseRepository.loadIndex()
        for (level in index.levels) {
            for (ref in level.units) {
                if (unitId.isNotBlank() && ref.id != unitId) continue
                val unit = courseRepository.loadUnit(ref)
                val lessonRef = unit.lessons.firstOrNull { it.id == lessonId } ?: continue
                val loaded = courseRepository.loadLesson(ref, lessonRef)
                return ResolvedLesson(unit = unit, lesson = loaded)
            }
        }
        throw IllegalArgumentException("Ders bulunamadı")
    }

    private suspend fun nextStep(currentLesson: Lesson): LessonNextStep {
        val index = courseRepository.loadIndex()
        val statuses = courseProgress.statusMap()
        for (level in index.levels) {
            for (ref in level.units) {
                val unit = courseRepository.loadUnit(ref)
                if (unit.id != currentLesson.unit) continue
                return nextStepForUnit(unit, currentLesson, statuses)
            }
        }
        return LessonNextStep.None
    }

    private fun nextStepForUnit(
        unit: CourseUnit,
        currentLesson: Lesson,
        statuses: Map<String, LessonStatus>,
    ): LessonNextStep {
        val path = CoursePath.buildPath(unit, statuses)
        val position = path.indexOfFirst { it.lesson.id == currentLesson.id }
        val next = if (position >= 0) {
            path.drop(position + 1).firstOrNull {
                it.status == LessonStatus.AVAILABLE || it.status == LessonStatus.IN_PROGRESS
            }
        } else {
            null
        }
        return when {
            next != null -> LessonNextStep.NextLesson(next.lesson.title)
            unit.roleplay != null -> LessonNextStep.RoleplayReady
            else -> LessonNextStep.UnitComplete
        }
    }

    override fun onCleared() {
        speaker.shutdown()
        super.onCleared()
    }
}
