package com.wendigo.tekrar.learn

import com.wendigo.tekrar.core.learn.CoursePath
import com.wendigo.tekrar.core.learn.CourseProgressSnapshot
import com.wendigo.tekrar.core.learn.CourseUnit
import com.wendigo.tekrar.core.learn.LessonRef
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.core.learn.UnitRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoursePathTest {

    private val unit = CourseUnit(
        schema = 1,
        id = "a1-u01",
        cefr = "A1",
        title = "Selamlaşma",
        subtitle = "Merhaba",
        lessons = listOf(
            LessonRef("a1-u01-l01", "Merhaba", "l01.json", 4),
            LessonRef("a1-u01-l02", "Nasılsın?", "l02.json", 4),
            LessonRef("a1-u01-l03", "Tanışma", "l03.json", 5),
            LessonRef("a1-u01-l04", "Vedalaşma", "l04.json", 4),
        ),
    )

    @Test
    fun firstLessonIsAvailableRestLocked() {
        val path = CoursePath.buildPath(unit, emptyMap())
        assertEquals(
            listOf(
                LessonStatus.AVAILABLE,
                LessonStatus.LOCKED,
                LessonStatus.LOCKED,
                LessonStatus.LOCKED,
            ),
            path.map { it.status },
        )
        assertTrue(path[0].isUnlocked)
        assertFalse(path[1].isUnlocked)
        assertEquals("a1-u01-l01", path[0].lessonId)
        assertEquals(3, path[3].index)
    }

    @Test
    fun completingLessonUnlocksNext() {
        val path = CoursePath.buildPath(
            unit,
            mapOf(
                "a1-u01-l01" to LessonStatus.COMPLETED,
                "a1-u01-l02" to LessonStatus.IN_PROGRESS,
            ),
        )
        assertEquals(
            listOf(
                LessonStatus.COMPLETED,
                LessonStatus.IN_PROGRESS,
                LessonStatus.LOCKED,
                LessonStatus.LOCKED,
            ),
            path.map { it.status },
        )
    }

    @Test
    fun masteredAlsoUnlocksNext() {
        val path = CoursePath.buildPath(
            unit,
            mapOf("a1-u01-l01" to LessonStatus.MASTERED),
        )
        assertEquals(LessonStatus.AVAILABLE, path[1].status)
    }

    @Test
    fun inProgressDoesNotUnlockNext() {
        val path = CoursePath.buildPath(
            unit,
            mapOf("a1-u01-l01" to LessonStatus.IN_PROGRESS),
        )
        assertEquals(LessonStatus.IN_PROGRESS, path[0].status)
        assertEquals(LessonStatus.LOCKED, path[1].status)
    }

    @Test
    fun storedStatusesArePreserved() {
        val path = CoursePath.buildPath(
            unit,
            mapOf(
                "a1-u01-l01" to LessonStatus.COMPLETED,
                "a1-u01-l02" to LessonStatus.COMPLETED,
                "a1-u01-l03" to LessonStatus.COMPLETED,
                "a1-u01-l04" to LessonStatus.MASTERED,
            ),
        )
        assertEquals(
            listOf(
                LessonStatus.COMPLETED,
                LessonStatus.COMPLETED,
                LessonStatus.COMPLETED,
                LessonStatus.MASTERED,
            ),
            path.map { it.status },
        )
    }

    @Test
    fun gapsKeepLaterLessonsLockedUnlessStored() {
        val path = CoursePath.buildPath(
            unit,
            mapOf("a1-u01-l03" to LessonStatus.COMPLETED),
        )
        assertEquals(LessonStatus.AVAILABLE, path[0].status)
        assertEquals(LessonStatus.LOCKED, path[1].status)
        assertEquals(LessonStatus.COMPLETED, path[2].status)
        assertEquals(LessonStatus.LOCKED, path[3].status)
    }

    @Test
    fun snapshotOverloadBuildsSamePath() {
        val snapshots = listOf(
            CourseProgressSnapshot("a1-u01-l01", LessonStatus.COMPLETED),
            CourseProgressSnapshot("a1-u01-l02", LessonStatus.AVAILABLE),
        )
        val fromSnapshots = CoursePath.buildPath(unit, snapshots)
        val fromMap = CoursePath.buildPath(
            unit,
            mapOf(
                "a1-u01-l01" to LessonStatus.COMPLETED,
                "a1-u01-l02" to LessonStatus.AVAILABLE,
            ),
        )
        assertEquals(fromMap.map { it.status }, fromSnapshots.map { it.status })
    }

    @Test
    fun unitAvailabilityUsesSequentialCompletion() {
        assertTrue(CoursePath.unitAvailable(0, emptySet()))
        assertFalse(CoursePath.unitAvailable(1, emptySet()))
        assertTrue(CoursePath.unitAvailable(1, setOf("a1-u01")))
        assertFalse(CoursePath.unitAvailable(2, setOf("a1-u01")))
        assertTrue(CoursePath.unitAvailable(2, setOf("a1-u01", "a1-u02")))
    }

    @Test
    fun unitAvailabilityWithExplicitList() {
        val units = listOf(
            UnitRef("a1-u01", "a1/u01/unit.json"),
            UnitRef("a1-u02", "a1/u02/unit.json"),
            UnitRef("a1-u03", "a1/u03/unit.json"),
        )
        assertTrue(CoursePath.unitAvailable(0, emptySet(), units))
        assertFalse(CoursePath.unitAvailable(1, emptySet(), units))
        assertTrue(CoursePath.unitAvailable(1, setOf("a1-u01"), units))
        assertFalse(CoursePath.unitAvailable(2, setOf("a1-u01"), units))
        assertTrue(CoursePath.unitAvailable(2, setOf("a1-u01", "a1-u02"), units))
        assertFalse(CoursePath.unitAvailable(3, setOf("a1-u01", "a1-u02"), units))
    }

    @Test
    fun completionHelperTreatsMasteredAsCompleted() {
        assertTrue(CoursePath.isCompleted(LessonStatus.COMPLETED))
        assertTrue(CoursePath.isCompleted(LessonStatus.MASTERED))
        assertFalse(CoursePath.isCompleted(LessonStatus.AVAILABLE))
        assertFalse(CoursePath.isCompleted(LessonStatus.IN_PROGRESS))
        assertFalse(CoursePath.isCompleted(LessonStatus.LOCKED))
    }
}
