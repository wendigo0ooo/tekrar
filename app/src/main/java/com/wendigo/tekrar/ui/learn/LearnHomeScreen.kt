package com.wendigo.tekrar.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.ui.components.learn.LearnCard
import com.wendigo.tekrar.ui.components.learn.LearnPrimaryButton
import com.wendigo.tekrar.ui.components.learn.LearnProgressBar
import com.wendigo.tekrar.ui.components.learn.LearnProgressRing
import com.wendigo.tekrar.ui.components.learn.LearnSecondaryButton
import com.wendigo.tekrar.ui.components.learn.LearnSectionHeader
import com.wendigo.tekrar.ui.components.learn.LearnStatusBadge
import com.wendigo.tekrar.ui.components.learn.LearnStatusTone
import com.wendigo.tekrar.ui.components.learn.LessonNodeRow
import com.wendigo.tekrar.ui.components.learn.UnitColorDot
import com.wendigo.tekrar.ui.components.learn.learnUnitColor

data class LearnHomeUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val levelTitle: String = "",
    val units: List<LearnUnitUi> = emptyList(),
    val nextLesson: LearnNextLessonUi? = null,
    val streak: Int = 0,
    val reviewedToday: Int = 0,
    val dailyGoal: Int = 0,
    val dueNow: Int = 0,
    val aiReady: Boolean = false,
)

data class LearnUnitUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val colorHex: String?,
    val locked: Boolean,
    val completedLessons: Int,
    val totalLessons: Int,
    val lessons: List<LearnLessonUi>,
    val roleplay: LearnLessonUi? = null,
    val complete: Boolean = false,
) {
    val progress: Float
        get() = if (totalLessons <= 0) 0f else completedLessons.toFloat() / totalLessons
}

data class LearnLessonUi(
    val id: String,
    val unitId: String,
    val title: String,
    val status: LessonStatus,
    val estMin: Int,
    val number: Int,
    val total: Int,
    val isRoleplay: Boolean = false,
)

data class LearnNextLessonUi(
    val unitId: String,
    val unitTitle: String,
    val lessonId: String,
    val lessonTitle: String,
    val estMin: Int,
    val colorHex: String?,
    val isContinue: Boolean,
    val lessonNumber: Int,
    val lessonTotal: Int,
)

@Composable
fun LearnHomeScreen(
    state: LearnHomeUiState,
    onLesson: (unitId: String, lessonId: String) -> Unit,
    onRoleplay: (unitId: String, roleplayId: String) -> Unit,
    onPractice: () -> Unit,
    onRetry: () -> Unit,
    onAiCoach: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when {
            state.loading -> LearnLoading()
            state.error -> LearnLoadError(onRetry = onRetry)
            else -> LearnHomeContent(
                state = state,
                onLesson = onLesson,
                onRoleplay = onRoleplay,
                onPractice = onPractice,
                onAiCoach = onAiCoach,
            )
        }
    }
}

@Composable
private fun LearnHomeContent(
    state: LearnHomeUiState,
    onLesson: (unitId: String, lessonId: String) -> Unit,
    onRoleplay: (unitId: String, roleplayId: String) -> Unit,
    onPractice: () -> Unit,
    onAiCoach: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LearnHeader(
            levelTitle = state.levelTitle,
            streak = state.streak,
        )
        DailyGoalCard(
            reviewedToday = state.reviewedToday,
            dailyGoal = state.dailyGoal,
        )
        NextLessonCard(
            next = state.nextLesson,
            onLesson = onLesson,
        )
        if (state.units.isEmpty()) {
            LearnCard {
                Text(
                    text = stringResource(R.string.learn_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.learn_empty_hint),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LearnSectionHeader(text = stringResource(R.string.learn_units_title))
                state.units.forEach { unit ->
                    UnitCard(
                        unit = unit,
                        onLesson = onLesson,
                        onRoleplay = onRoleplay,
                    )
                }
            }
        }
        PracticeCard(
            dueNow = state.dueNow,
            onPractice = onPractice,
        )
        AiCard(
            aiReady = state.aiReady,
            onAiCoach = onAiCoach,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun LearnHeader(
    levelTitle: String,
    streak: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.learn_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = levelTitle.ifBlank { stringResource(R.string.learn_level_fallback) },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StreakChip(streak = streak)
    }
}

@Composable
private fun StreakChip(streak: Int) {
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (streak > 0) {
                stringResource(R.string.learn_streak_value, streak)
            } else {
                "0"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = if (streak > 0) {
                stringResource(R.string.learn_streak_label)
            } else {
                stringResource(R.string.learn_streak_none_hint)
            },
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun DailyGoalCard(
    reviewedToday: Int,
    dailyGoal: Int,
) {
    val hasGoal = dailyGoal > 0
    val progress = if (hasGoal) {
        (reviewedToday.toFloat() / dailyGoal).coerceIn(0f, 1f)
    } else {
        0f
    }
    LearnCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LearnProgressRing(
                progress = progress,
                centerText = reviewedToday.toString(),
                caption = stringResource(R.string.learn_goal_caption),
                diameter = 72.dp,
                stroke = 7.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.learn_goal_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hasGoal) {
                        stringResource(R.string.learn_goal_progress, reviewedToday, dailyGoal)
                    } else {
                        stringResource(R.string.learn_goal_no_target, reviewedToday)
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LearnProgressBar(
                    fraction = progress,
                    height = 7.dp,
                )
                Text(
                    text = when {
                        !hasGoal -> stringResource(R.string.learn_goal_no_goal_hint)
                        reviewedToday >= dailyGoal -> stringResource(R.string.learn_goal_done)
                        else -> stringResource(
                            R.string.learn_goal_remaining,
                            (dailyGoal - reviewedToday).coerceAtLeast(0),
                        )
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NextLessonCard(
    next: LearnNextLessonUi?,
    onLesson: (unitId: String, lessonId: String) -> Unit,
) {
    LearnCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        if (next == null) {
            Text(
                text = stringResource(R.string.learn_next_all_done),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.learn_next_all_done_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UnitColorDot(
                    color = learnUnitColor(next.colorHex),
                    size = 11.dp,
                )
                Text(
                    text = stringResource(R.string.learn_next_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
            }
            Text(
                text = next.lessonTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.learn_next_lesson_meta,
                    next.unitTitle,
                    next.estMin,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            )
            Text(
                text = stringResource(
                    R.string.learn_next_lesson_number,
                    next.lessonNumber,
                    next.lessonTotal,
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            LearnPrimaryButton(
                text = stringResource(
                    if (next.isContinue) R.string.learn_next_continue else R.string.learn_next_start,
                ),
                onClick = { onLesson(next.unitId, next.lessonId) },
            )
        }
    }
}

@Composable
private fun UnitCard(
    unit: LearnUnitUi,
    onLesson: (unitId: String, lessonId: String) -> Unit,
    onRoleplay: (unitId: String, roleplayId: String) -> Unit,
) {
    val accent = learnUnitColor(unit.colorHex)
    LearnCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UnitColorDot(
                color = if (unit.locked) MaterialTheme.colorScheme.outline else accent,
                size = 11.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = unit.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    color = if (unit.locked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = unit.subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.learn_unit_lessons,
                        unit.completedLessons,
                        unit.totalLessons,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LearnProgressBar(
                    fraction = unit.progress,
                    modifier = Modifier.width(70.dp),
                    height = 6.dp,
                    color = if (unit.locked) MaterialTheme.colorScheme.outline else accent,
                )
            }
        }

        if (unit.locked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LearnStatusBadge(
                    text = stringResource(R.string.learn_status_locked),
                    tone = LearnStatusTone.LOCKED,
                )
                Text(
                    text = stringResource(R.string.learn_unit_locked_hint),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (unit.complete) {
            LearnStatusBadge(
                text = stringResource(R.string.learn_unit_complete),
                tone = LearnStatusTone.DONE,
            )
        }

        Column {
            unit.lessons.forEachIndexed { index, lesson ->
                val click: (() -> Unit)? = if (lesson.status != LessonStatus.LOCKED) {
                    { onLesson(unit.id, lesson.id) }
                } else {
                    null
                }
                LessonNodeRow(
                    title = lesson.title,
                    subtitle = stringResource(R.string.learn_lesson_est, lesson.estMin),
                    status = lesson.status,
                    isFirst = index == 0,
                    isLast = index == unit.lessons.lastIndex && unit.roleplay == null,
                    onClick = click,
                    showStatusBadge = !unit.locked,
                )
            }
            unit.roleplay?.let { roleplay ->
                val click: (() -> Unit)? = if (roleplay.status != LessonStatus.LOCKED) {
                    { onRoleplay(unit.id, roleplay.id) }
                } else {
                    null
                }
                LessonNodeRow(
                    title = stringResource(R.string.learn_roleplay_title, roleplay.title),
                    subtitle = stringResource(R.string.learn_lesson_est, roleplay.estMin),
                    status = roleplay.status,
                    isFirst = unit.lessons.isEmpty(),
                    isLast = true,
                    onClick = click,
                    isRoleplay = true,
                    showStatusBadge = !unit.locked,
                )
            }
        }
    }
}

@Composable
private fun PracticeCard(
    dueNow: Int,
    onPractice: () -> Unit,
) {
    LearnCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.learn_practice_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (dueNow > 0) {
                        stringResource(R.string.learn_practice_due, dueNow)
                    } else {
                        stringResource(R.string.learn_practice_none)
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.learn_practice_hint),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = dueNow.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = if (dueNow > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        LearnSecondaryButton(
            text = stringResource(R.string.learn_practice_start),
            onClick = onPractice,
            enabled = dueNow > 0,
        )
    }
}

@Composable
private fun AiCard(
    aiReady: Boolean,
    onAiCoach: (() -> Unit)?,
) {
    LearnCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.learn_ai_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LearnStatusBadge(
                text = stringResource(
                    if (aiReady) R.string.learn_ai_ready_badge else R.string.learn_ai_soon_badge,
                ),
                tone = if (aiReady) LearnStatusTone.ACTIVE else LearnStatusTone.NEUTRAL,
            )
        }
        Text(
            text = stringResource(
                if (aiReady) R.string.learn_ai_ready_desc else R.string.learn_ai_soon_desc,
            ),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (aiReady) {
            onAiCoach?.let { coach ->
                LearnSecondaryButton(
                    text = stringResource(R.string.learn_ai_open),
                    onClick = coach,
                )
            }
        }
    }
}

@Composable
private fun LearnLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.learn_loading),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LearnLoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        LearnCard {
            Text(
                text = stringResource(R.string.learn_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.learn_error_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LearnPrimaryButton(
                text = stringResource(R.string.learn_retry),
                onClick = onRetry,
            )
        }
    }
}
