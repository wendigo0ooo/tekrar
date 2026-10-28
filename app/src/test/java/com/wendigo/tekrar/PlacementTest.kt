package com.wendigo.tekrar

import com.wendigo.tekrar.core.Placement
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTest {

    @Test
    fun belowFortyPercentRecommendsTwenty() {
        assertEquals(20, Placement.score(0, 15).recommendedNewPerDay)
        assertEquals(20, Placement.score(5, 15).recommendedNewPerDay)
        assertEquals(20, Placement.score(39, 100).recommendedNewPerDay)
    }

    @Test
    fun fortyToSeventyPercentRecommendsFifteen() {
        assertEquals(15, Placement.score(40, 100).recommendedNewPerDay)
        assertEquals(15, Placement.score(6, 15).recommendedNewPerDay)
        assertEquals(15, Placement.score(70, 100).recommendedNewPerDay)
    }

    @Test
    fun aboveSeventyPercentRecommendsTen() {
        assertEquals(10, Placement.score(71, 100).recommendedNewPerDay)
        assertEquals(10, Placement.score(15, 15).recommendedNewPerDay)
    }

    @Test
    fun bandsFollowTheSameBoundaries() {
        assertEquals("A1–A2", Placement.score(0, 15).band)
        assertEquals("A1–A2", Placement.score(39, 100).band)
        assertEquals("B1", Placement.score(40, 100).band)
        assertEquals("B1", Placement.score(70, 100).band)
        assertEquals("B2–C1", Placement.score(71, 100).band)
        assertEquals("B2–C1", Placement.score(15, 15).band)
    }

    @Test
    fun clampsInvalidCountsAndHandlesEmptyTest() {
        assertEquals(20, Placement.score(-3, 10).recommendedNewPerDay)
        assertEquals(10, Placement.score(10, 10).recommendedNewPerDay)
        assertEquals(15, Placement.score(99, 0).recommendedNewPerDay)
        assertEquals("Belirsiz", Placement.score(0, 0).band)
    }
}
