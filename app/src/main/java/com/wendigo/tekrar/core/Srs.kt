package com.wendigo.tekrar.core

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

object Srs {

    const val AGAIN = 1
    const val HARD = 2
    const val GOOD = 3
    const val EASY = 4

    const val DECAY = -0.5
    const val FACTOR = 19.0 / 81.0
    const val MAX_INTERVAL_DAYS = 36_500
    const val MIN_STABILITY = 0.1

    val DEFAULT_PARAMS: DoubleArray = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575,
        0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621,
    )

    data class Result(
        val stability: Double,
        val difficulty: Double,
        val intervalDays: Int,
    )

    fun retrievability(elapsedDays: Double, stability: Double): Double {
        val s = stability.coerceAtLeast(MIN_STABILITY)
        val t = elapsedDays.coerceAtLeast(0.0)
        return (1.0 + FACTOR * t / s).pow(DECAY)
    }

    fun intervalDays(stability: Double, desiredRetention: Double = 0.9): Int {
        val s = stability.coerceAtLeast(MIN_STABILITY)
        val r = desiredRetention.coerceIn(0.7, 0.98)
        val days = (s / FACTOR) * (r.pow(1.0 / DECAY) - 1.0)
        return days.roundToInt().coerceIn(1, MAX_INTERVAL_DAYS)
    }

    fun review(
        stability: Double,
        difficulty: Double,
        elapsedDays: Double,
        isNew: Boolean,
        rating: Int,
        desiredRetention: Double = 0.9,
        params: DoubleArray = DEFAULT_PARAMS,
    ): Result {
        val g = rating.coerceIn(AGAIN, EASY)
        val (nextS, nextD) = when {
            isNew -> initialStability(g, params) to initialDifficulty(g, params)
            elapsedDays < 1.0 -> {
                val capped = stability.coerceAtLeast(MIN_STABILITY)
                capped * exp(params[17] * (g - 3.0 + params[18])) to
                    nextDifficulty(difficulty, g, params)
            }
            else -> {
                val s = stability.coerceAtLeast(MIN_STABILITY)
                val d = difficulty.coerceIn(1.0, 10.0)
                val r = retrievability(elapsedDays, s)
                val updated = if (g == AGAIN) forgetStability(d, s, r, params)
                else recallStability(d, s, r, g, params)
                updated to nextDifficulty(d, g, params)
            }
        }
        val s = nextS.coerceAtLeast(MIN_STABILITY)
        return Result(s, nextD.coerceIn(1.0, 10.0), intervalDays(s, desiredRetention))
    }

    private fun initialStability(rating: Int, w: DoubleArray): Double =
        w[rating - 1].coerceAtLeast(MIN_STABILITY)

    private fun initialDifficulty(rating: Int, w: DoubleArray): Double =
        (w[4] - exp(w[5] * (rating - 1)) + 1.0).coerceIn(1.0, 10.0)

    private fun nextDifficulty(difficulty: Double, rating: Int, w: DoubleArray): Double {
        val d = difficulty.coerceIn(1.0, 10.0)
        val delta = -w[6] * (rating - 3)
        val moved = d + (10.0 - d) * delta / 9.0
        val target = initialDifficulty(EASY, w)
        return (w[7] * target + (1.0 - w[7]) * moved).coerceIn(1.0, 10.0)
    }

    private fun recallStability(
        d: Double,
        s: Double,
        r: Double,
        rating: Int,
        w: DoubleArray,
    ): Double {
        val hardPenalty = if (rating == HARD) w[15] else 1.0
        val easyBonus = if (rating == EASY) w[16] else 1.0
        val growth = exp(w[8]) * (11.0 - d) * s.pow(-w[9]) *
            (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus
        return s * (1.0 + growth)
    }

    private fun forgetStability(d: Double, s: Double, r: Double, w: DoubleArray): Double {
        val longTerm = w[11] * d.pow(-w[12]) *
            ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        val shortTermCap = s / exp(w[17] * w[18])
        return minOf(longTerm, shortTermCap)
    }
}
