package com.wendigo.tekrar.core

object SnoozeTime {

    const val DELAY_MS = 60L * 60L * 1000L

    fun next(now: Long, delayMs: Long = DELAY_MS): Long = now + delayMs.coerceAtLeast(0L)
}
