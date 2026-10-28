package com.wendigo.tekrar

import com.wendigo.tekrar.core.ReminderMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderMessageTest {

    @Test
    fun prefersDueReviews() {
        assertEquals(ReminderMessage.Due(4), ReminderMessage.of(4, 10))
    }

    @Test
    fun fallsBackToNewWords() {
        assertEquals(ReminderMessage.NewWords(3), ReminderMessage.of(0, 3))
    }

    @Test
    fun skipsWhenNothingIsWaiting() {
        assertEquals(ReminderMessage.None, ReminderMessage.of(0, 0))
    }
}
