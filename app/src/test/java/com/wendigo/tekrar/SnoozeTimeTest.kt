package com.wendigo.tekrar

import com.wendigo.tekrar.core.SnoozeTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeTimeTest {

    @Test
    fun defaultsToOneHour() {
        val now = 1_000_000L
        assertEquals(now + SnoozeTime.DELAY_MS, SnoozeTime.next(now))
    }

    @Test
    fun acceptsCustomDelay() {
        assertEquals(1_005_000L, SnoozeTime.next(1_000_000L, 5_000L))
    }

    @Test
    fun neverSchedulesInThePast() {
        assertEquals(1_000_000L, SnoozeTime.next(1_000_000L, -5_000L))
    }
}
