package com.wendigo.tekrar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val LightBackground = Color(0xFFFAF8F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFEBE2)
val LightOnBackground = Color(0xFF191C1A)
val LightOnSurface = Color(0xFF191C1A)
val LightOnSurfaceVariant = Color(0xFF5A6058)
val LightPrimary = Color(0xFF1F6F5C)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCFE9DF)
val LightOnPrimaryContainer = Color(0xFF052E24)
val LightOutline = Color(0xFFC7C1B4)
val LightError = Color(0xFFB3261E)

val DarkBackground = Color(0xFF0F1412)
val DarkSurface = Color(0xFF161C19)
val DarkSurfaceVariant = Color(0xFF212A26)
val DarkOnBackground = Color(0xFFE6E9E4)
val DarkOnSurface = Color(0xFFE6E9E4)
val DarkOnSurfaceVariant = Color(0xFFB5BDB6)
val DarkPrimary = Color(0xFF8FD3BE)
val DarkOnPrimary = Color(0xFF00382C)
val DarkPrimaryContainer = Color(0xFF1B4C40)
val DarkOnPrimaryContainer = Color(0xFFC6F0E2)
val DarkOutline = Color(0xFF3A453F)
val DarkError = Color(0xFFF2B8B5)

val LightAgainTone = Color(0xFFA8453A)
val DarkAgainTone = Color(0xFFE8A29A)
val LightHardTone = Color(0xFFA9741F)
val DarkHardTone = Color(0xFFE3C07A)
val LightGoodTone = LightPrimary
val DarkGoodTone = DarkPrimary
val LightEasyTone = Color(0xFF2F5D8A)
val DarkEasyTone = Color(0xFFA6C8E8)

@Immutable
data class RatingTones(
    val again: Color,
    val hard: Color,
    val good: Color,
    val easy: Color,
)

val LightRatingTones = RatingTones(
    again = LightAgainTone,
    hard = LightHardTone,
    good = LightGoodTone,
    easy = LightEasyTone,
)

val DarkRatingTones = RatingTones(
    again = DarkAgainTone,
    hard = DarkHardTone,
    good = DarkGoodTone,
    easy = DarkEasyTone,
)
