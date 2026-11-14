package com.wendigo.tekrar.core.learn

/**
 * Öğren yolu: ünite/ders kilitleri.
 *
 * Dersler sıralı açılır: bir ders ancak kendinden önceki tüm dersler
 * `COMPLETED` veya `MASTERED` ise açılır. İlk ders her zaman açıktır.
 * Ünitenin kendisi de önceki ünite tamamlanınca açılır.
 */
enum class LessonStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED,
    MASTERED,
}

data class CourseProgressSnapshot(
    val lessonId: String,
    val status: LessonStatus,
)

data class LessonNode(
    val lesson: LessonRef,
    val index: Int,
    val status: LessonStatus,
) {
    val lessonId: String get() = lesson.id
    val isUnlocked: Boolean get() = status != LessonStatus.LOCKED
}

object CoursePath {

    fun isCompleted(status: LessonStatus): Boolean =
        status == LessonStatus.COMPLETED || status == LessonStatus.MASTERED

    /**
     * Kayıtlı durumlar korunur; kaydı olmayan ders için sıralı açılma uygulanır.
     * Önceki ders tamamlanmadıkça sonraki ders `LOCKED` kalır.
     */
    fun buildPath(unit: CourseUnit, progress: Map<String, LessonStatus>): List<LessonNode> {
        var previousDone = true
        return unit.lessons.mapIndexed { index, lesson ->
            val stored = progress[lesson.id]
            val status = stored ?: if (previousDone) LessonStatus.AVAILABLE else LessonStatus.LOCKED
            previousDone = previousDone && isCompleted(status)
            LessonNode(lesson = lesson, index = index, status = status)
        }
    }

    fun buildPath(
        unit: CourseUnit,
        snapshots: List<CourseProgressSnapshot>,
    ): List<LessonNode> = buildPath(unit, snapshots.associate { it.lessonId to it.status })

    /**
     * Ünite kilidi. Üniteler sıralı tamamlandığı varsayımıyla, `unitIndex`
     * (0 tabanlı) için en az `unitIndex` ünite tamamlanmışsa açıktır.
     */
    fun unitAvailable(unitIndex: Int, completedUnitIds: Set<String>): Boolean {
        if (unitIndex <= 0) return true
        return completedUnitIds.size >= unitIndex
    }

    /** Ünite listesiyle kesin kilit: yalnız hemen önceki ünite tamamlanmışsa açılır. */
    fun unitAvailable(
        unitIndex: Int,
        completedUnitIds: Set<String>,
        units: List<UnitRef>,
    ): Boolean {
        if (unitIndex <= 0) return true
        val previous = units.getOrNull(unitIndex - 1) ?: return false
        return completedUnitIds.contains(previous.id)
    }
}
