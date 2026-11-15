package com.wendigo.tekrar.ui.grammar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.learn.LessonStatus
import com.wendigo.tekrar.ui.components.learn.LearnCard
import com.wendigo.tekrar.ui.components.learn.LearnPrimaryButton
import com.wendigo.tekrar.ui.components.learn.LearnProgressBar
import com.wendigo.tekrar.ui.components.learn.LearnSecondaryButton
import com.wendigo.tekrar.ui.components.learn.LearnSectionHeader
import com.wendigo.tekrar.ui.components.learn.LearnStatusBadge
import com.wendigo.tekrar.ui.components.learn.LearnStatusTone
import com.wendigo.tekrar.ui.components.learn.learnUnitColor

@Composable
fun GrammarHomeScreen(
    state: GrammarHomeUiState,
    onOpenTopic: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onContinue: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when {
            state.loading -> GrammarLoading(
                text = stringResource(R.string.grammar_loading),
            )

            state.error -> GrammarLoadError(
                title = stringResource(R.string.grammar_error_title),
                hint = stringResource(R.string.grammar_error_hint),
                retry = stringResource(R.string.grammar_retry),
                onRetry = onRetry,
            )

            state.sections.isEmpty() -> GrammarEmptyState(onBack = onBack)

            else -> GrammarHomeContent(
                state = state,
                onOpenTopic = onOpenTopic,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun GrammarHomeContent(
    state: GrammarHomeUiState,
    onOpenTopic: (String) -> Unit,
    onContinue: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GrammarHeader()
        GrammarProgressCard(
            state = state,
            onContinue = onContinue,
        )
        state.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LearnSectionHeader(
                    text = sectionTitle(section.section),
                    trailing = {
                        Text(
                            text = stringResource(
                                R.string.grammar_section_count,
                                section.topics.size,
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                section.topics.forEach { topic ->
                    GrammarTopicCard(
                        topic = topic,
                        onOpen = onOpenTopic,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun GrammarHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = stringResource(R.string.grammar_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.grammar_home_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GrammarProgressCard(
    state: GrammarHomeUiState,
    onContinue: (String) -> Unit,
) {
    val fraction = if (state.totalCount > 0) {
        state.completedCount.toFloat() / state.totalCount
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 520),
        label = "grammar-progress",
    )
    LearnCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.grammar_progress_title),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            if (state.totalEstMin > 0) {
                Text(
                    text = stringResource(
                        R.string.grammar_progress_meta,
                        state.totalEstMin,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.grammar_progress_value,
                state.completedCount,
                state.totalCount,
            ),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        LearnProgressBar(
            fraction = animatedFraction,
            height = 7.dp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
        )
        Text(
            text = if (state.allCompleted) {
                stringResource(R.string.grammar_progress_done)
            } else {
                stringResource(R.string.grammar_next_topic, state.nextTopicTitle)
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LearnPrimaryButton(
            text = stringResource(
                if (state.allCompleted) {
                    R.string.grammar_continue_all
                } else {
                    R.string.grammar_continue
                },
            ),
            onClick = { state.nextTopicId?.let(onContinue) },
            enabled = state.nextTopicId != null,
        )
    }
}

@Composable
private fun GrammarTopicCard(
    topic: GrammarTopicCardUi,
    onOpen: (String) -> Unit,
) {
    val accent = learnUnitColor(topic.colorHex)
    LearnCard(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) { onOpen(topic.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = topic.subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.grammar_topic_est, topic.estMin),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GrammarStatusBadge(status = topic.status)
            }
        }
    }
}

@Composable
private fun GrammarStatusBadge(status: LessonStatus) {
    val labelRes = when (status) {
        LessonStatus.COMPLETED, LessonStatus.MASTERED -> R.string.grammar_status_done
        LessonStatus.IN_PROGRESS -> R.string.grammar_status_in_progress
        else -> R.string.grammar_status_open
    }
    val tone = when (status) {
        LessonStatus.COMPLETED, LessonStatus.MASTERED -> LearnStatusTone.DONE
        LessonStatus.IN_PROGRESS -> LearnStatusTone.ACTIVE
        else -> LearnStatusTone.NEUTRAL
    }
    LearnStatusBadge(
        text = stringResource(labelRes),
        tone = tone,
    )
}

@Composable
private fun sectionTitle(section: GrammarSection): String = stringResource(
    when (section) {
        GrammarSection.BASICS -> R.string.grammar_section_basics
        GrammarSection.MEETING -> R.string.grammar_section_meeting
        GrammarSection.ROUTINE -> R.string.grammar_section_routine
    },
)

@Composable
private fun GrammarLoading(text: String) {
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
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GrammarLoadError(
    title: String,
    hint: String,
    retry: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        LearnCard {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LearnPrimaryButton(text = retry, onClick = onRetry)
        }
    }
}

@Composable
private fun GrammarEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        LearnCard {
            Text(
                text = stringResource(R.string.grammar_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.grammar_empty_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LearnSecondaryButton(
                text = stringResource(R.string.grammar_empty_back),
                onClick = onBack,
            )
        }
    }
}
