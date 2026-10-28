package com.wendigo.tekrar

import com.wendigo.tekrar.core.QuietHours
import com.wendigo.tekrar.core.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test

class QuietHoursTest {

    @Test
    fun keepsDaytimeReminder() {
        assertEquals(ReminderTime(21, 59), QuietHours.clamp(21, 59))
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(8, 0))
        assertEquals(ReminderTime(8, 1), QuietHours.clamp(8, 1))
    }

    @Test
    fun movesNightReminderToEight() {
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(22, 0))
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(23, 30))
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(0, 0))
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(7, 59))
    }

    @Test
    fun coercesOutOfRangeInput() {
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(25, 70))
        assertEquals(ReminderTime(8, 0), QuietHours.clamp(-3, -1))
        assertEquals(ReminderTime(12, 15), QuietHours.clamp(12, 15))
    }
}
