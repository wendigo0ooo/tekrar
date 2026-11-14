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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.learn.FeedbackGlyph
import com.wendigo.tekrar.ui.components.learn.LearnCard
import com.wendigo.tekrar.ui.components.learn.LearnPrimaryButton
import com.wendigo.tekrar.ui.components.learn.LearnProgressRing
import com.wendigo.tekrar.ui.components.learn.LearnSecondaryButton
import com.wendigo.tekrar.ui.theme.LocalRatingTones

sealed interface LessonNextStep {
    data class NextLesson(val title: String) : LessonNextStep
    data object UnitComplete : LessonNextStep
    data object RoleplayReady : LessonNextStep
    data object None : LessonNextStep
}

data class LessonResultUiState(
    val accuracyPercent: Int,
    val correctCount: Int,
    val totalCount: Int,
    val learnedWordCount: Int,
    val addedToReview: Boolean,
    val nextStep: LessonNextStep,
    val seedFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

@Composable
fun LessonResultScreen(
    state: LessonResultUiState,
    lessonTitle: String,
    onBackToLearn: () -> Unit,
    onPractice: (() -> Unit)? = null,
    onRetrySave: (() -> Unit)? = null,
) {
    val tones = LocalRatingTones.current
    val accuracyColor = when {
        state.accuracyPercent >= 80 -> MaterialTheme.colorScheme.primary
        state.accuracyPercent >= 50 -> tones.hard
        else -> tones.again
    }
    val safeAccuracy = state.accuracyPercent.coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.learn_result_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (lessonTitle.isNotBlank()) {
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            LearnProgressRing(
                progress = safeAccuracy / 100f,
                centerText = "$safeAccuracy%",
                caption = stringResource(R.string.learn_result_accuracy_caption),
                diameter = 156.dp,
                stroke = 13.dp,
                color = accuracyColor,
            )

            if (state.saveFailed) {
                LearnCard(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.09f)) {
                    Text(
                        text = stringResource(R.string.learn_result_save_error),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (onRetrySave != null) {
                        LearnSecondaryButton(
                            text = stringResource(R.string.learn_result_retry_save),
                            onClick = onRetrySave,
                            height = 50.dp,
                        )
                    }
                }
            }

            LearnCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResultStat(
                        value = "${state.correctCount} / ${state.totalCount}",
                        label = stringResource(R.string.learn_result_correct),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(46.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    )
                    ResultStat(
                        value = state.learnedWordCount.toString(),
                        label = stringResource(R.string.learn_result_words),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (!state.saveFailed) {
                if (state.seedFailed) {
                    LearnCard(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.07f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FeedbackGlyph(correct = false)
                            Text(
                                text = stringResource(R.string.learn_result_seed_error),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    LearnCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FeedbackGlyph(correct = true)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.learn_result_added),
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = if (state.addedToReview) {
                                        stringResource(
                                            R.string.learn_result_added_desc,
                                            state.learnedWordCount,
                                        )
                                    } else {
                                        stringResource(R.string.learn_result_added_none)
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (!state.saveFailed) {
                LearnCard {
                    Text(
                        text = stringResource(R.string.learn_result_next_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = nextStepText(state.nextStep),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            LearnPrimaryButton(
                text = stringResource(R.string.learn_result_back),
                onClick = onBackToLearn,
            )
            if (onPractice != null && !state.saveFailed) {
                LearnSecondaryButton(
                    text = stringResource(R.string.learn_result_practice),
                    onClick = onPractice,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ResultStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun nextStepText(nextStep: LessonNextStep): String = when (nextStep) {
    is LessonNextStep.NextLesson -> stringResource(
        R.string.learn_result_next_lesson,
        nextStep.title,
    )

    LessonNextStep.UnitComplete -> stringResource(R.string.learn_result_next_unit_done)
    LessonNextStep.RoleplayReady -> stringResource(R.string.learn_result_next_roleplay)
    LessonNextStep.None -> stringResource(R.string.learn_result_next_unknown)
}
