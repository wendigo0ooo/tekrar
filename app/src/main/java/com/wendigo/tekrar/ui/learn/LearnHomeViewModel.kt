package com.wendigo.tekrar.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.core.learn.CourseLevel
import com.wendigo.tekrar.core.learn.CoursePath
import com.wendigo.tekrar.core.learn.CourseUnit
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.repo.CourseProgressRepository
import com.wendigo.tekrar.repo.CourseRepository
import com.wendigo.tekrar.repo.WordRepository
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Öğren ana ekranı: kurs index'i + ders ilerlemesi + günlük özet akışlarını
 * birleştirir. Kurs içeriği cihazdaki asset'lerden okunur; AI durumu v1'de
 * her zaman hazır değildir.
 */
class LearnHomeViewModel(
    private val courseRepository: CourseRepository,
    private val courseProgress: CourseProgressRepository,
    private val wordRepository: WordRepository,
) : ViewModel() {

    private data class CourseSnapshot(
        val loading: Boolean = true,
        val error: Boolean = false,
        val level: CourseLevel? = null,
        val units: List<CourseUnit> = emptyList(),
    )

    private val courseState = MutableStateFlow(CourseSnapshot())
    private val progressState = MutableStateFlow<Map<String, LessonStatus>>(emptyMap())

    val state: StateFlow<LearnHomeUiState> = combine(
        courseState,
        progressState,
        wordRepository.observeDashboard(),
    ) { snapshot, progress, dashboard ->
        runCatching { buildState(snapshot, progress, dashboard) }
            .getOrElse {
                LearnHomeUiState(
                    loading = false,
                    error = true,
                    streak = dashboard.streak,
                    reviewedToday = dashboard.reviewedToday,
                    dailyGoal = dashboard.dailyGoal,
                    dueNow = dashboard.dueNow,
                    aiReady = true,
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LearnHomeUiState(),
    )

    init {
        loadCourse()
        viewModelScope.launch {
            courseProgress.observeAll().collect {
                val map = loadStatusMap() ?: return@collect
                progressState.value = map
            }
        }
    }

    /** Ders tamamlanıp ekrana dönüldüğünde ilerlemeyi tazeler. */
    fun refresh() {
        viewModelScope.launch {
            val map = loadStatusMap() ?: return@launch
            progressState.value = map
        }
    }

    fun retry() {
        courseState.value = courseState.value.copy(loading = true, error = false)
        loadCourse()
        refresh()
    }

    private fun loadCourse() {
        viewModelScope.launch {
            courseState.value = courseState.value.copy(loading = true, error = false)
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    val index = courseRepository.loadIndex()
                    val level = index.levels.firstOrNull()
                        ?: throw IllegalStateException("Kurs seviyesi yok")
                    val units = level.units.map { courseRepository.loadUnit(it) }
                    CourseSnapshot(
                        loading = false,
                        error = false,
                        level = level,
                        units = units,
                    )
                }
                courseState.value = snapshot
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                courseState.value = CourseSnapshot(
                    loading = false,
                    error = true,
                    level = courseState.value.level,
                    units = emptyList(),
                )
            }
        }
    }

    private suspend fun loadStatusMap(): Map<String, LessonStatus>? =
        try {
            courseProgress.statusMap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            null
        }

    private fun buildState(
        snapshot: CourseSnapshot,
        progress: Map<String, LessonStatus>,
        dashboard: DashboardUiState,
    ): LearnHomeUiState {
        val base = LearnHomeUiState(
            loading = snapshot.loading,
            error = snapshot.error,
            levelTitle = snapshot.level?.title.orEmpty(),
            streak = dashboard.streak,
            reviewedToday = dashboard.reviewedToday,
            dailyGoal = dashboard.dailyGoal,
            dueNow = dashboard.dueNow,
            aiReady = true,
        )
        val level = snapshot.level ?: return base
        val refs = level.units
        val units = snapshot.units
        if (refs.isEmpty() || units.isEmpty()) return base.copy(loading = false)

        val completedUnitIds = units.asSequence()
            .filter { unit ->
                unit.lessons.isNotEmpty() && unit.lessons.all { lesson ->
                    isDone(progress[lesson.id])
                }
            }
            .map { it.id }
            .toSet()

        val cards = units.mapIndexed { index, unit ->
            val available = refs.getOrNull(index) != null &&
                CoursePath.unitAvailable(index, completedUnitIds, refs)
            val nodes = CoursePath.buildPath(unit, progress)
            val lessons = nodes.mapIndexed { lessonIndex, node ->
                LearnLessonUi(
                    id = node.lesson.id,
                    unitId = unit.id,
                    title = node.lesson.title,
                    status = if (available) node.status else LessonStatus.LOCKED,
                    estMin = node.lesson.estMin,
                    number = lessonIndex + 1,
                    total = nodes.size,
                )
            }
            val completedLessons = lessons.count { isDone(it.status) }
            val unitComplete = lessons.isNotEmpty() && completedLessons == lessons.size
            val roleplay = unit.roleplay?.let { ref ->
                LearnLessonUi(
                    id = ref.id,
                    unitId = unit.id,
                    title = ref.title,
                    status = when {
                        !available -> LessonStatus.LOCKED
                        unitComplete -> LessonStatus.AVAILABLE
                        else -> LessonStatus.LOCKED
                    },
                    estMin = ref.estMin,
                    number = lessons.size + 1,
                    total = lessons.size + 1,
                    isRoleplay = true,
                )
            }
            LearnUnitUi(
                id = unit.id,
                title = unit.title,
                subtitle = unit.subtitle,
                colorHex = unit.color,
                locked = !available,
                completedLessons = completedLessons,
                totalLessons = lessons.size,
                lessons = lessons,
                roleplay = roleplay,
                complete = unitComplete,
            )
        }

        val next = cards.asSequence()
            .filter { !it.locked }
            .flatMap { card -> card.lessons.asSequence().map { lesson -> card to lesson } }
            .firstOrNull { (_, lesson) ->
                lesson.status == LessonStatus.AVAILABLE ||
                    lesson.status == LessonStatus.IN_PROGRESS
            }

        val nextLesson = next?.let { (card, lesson) ->
            LearnNextLessonUi(
                unitId = card.id,
                unitTitle = card.title,
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                estMin = lesson.estMin,
                colorHex = card.colorHex,
                isContinue = lesson.status == LessonStatus.IN_PROGRESS,
                lessonNumber = lesson.number,
                lessonTotal = lesson.total,
            )
        }

        return base.copy(
            loading = false,
            units = cards,
            nextLesson = nextLesson,
        )
    }

    private fun isDone(status: LessonStatus?): Boolean =
        status != null && CoursePath.isCompleted(status)
}
