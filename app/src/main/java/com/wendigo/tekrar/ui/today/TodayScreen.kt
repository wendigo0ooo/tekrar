package com.wendigo.tekrar.ui.today

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.GoalRing
import com.wendigo.tekrar.ui.components.MiniBars
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ProgressBar
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.components.StatTile
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    state: DashboardUiState,
    onStudy: (StudyMode, StudyFormat) -> Unit,
    onAddWord: () -> Unit,
    onLibrary: () -> Unit,
) {
    val dateText = LocalDate.now().format(
        DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr"))
    )
    val goalProgress = if (state.dailyGoal > 0) {
        state.reviewedToday.toFloat() / state.dailyGoal
    } else {
        0f
    }
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.today_title),
                subtitle = dateText,
                trailing = {
                    TextButton(onClick = onAddWord) {
                        Text(text = stringResource(R.string.today_add_word_short))
                    }
                },
            )

            Panel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    GoalRing(
                        progress = goalProgress,
                        centerText = "${state.reviewedToday}",
                        caption = stringResource(R.string.today_goal_caption),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.today_goal_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.today_goal_progress,
                                state.reviewedToday,
                                state.dailyGoal,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProgressBar(fraction = goalProgress)
                        Text(
                            text = if (state.streak > 0) {
                                stringResource(R.string.today_streak, state.streak)
                            } else {
                                stringResource(R.string.today_streak_none)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Panel {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = stringResource(R.string.today_due),
                        value = state.dueNow.toString(),
                        modifier = Modifier.weight(1f),
                        accent = if (state.dueNow > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    StatTile(
                        label = stringResource(R.string.today_new),
                        value = state.newAvailable.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.today_total),
                        value = state.totalWords.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = stringResource(R.string.today_learned),
                        value = state.learned.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.today_mature),
                        value = state.mature.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            PrimaryButton(
                text = stringResource(R.string.today_start),
                onClick = { onStudy(StudyMode.MIXED, StudyFormat.FLASHCARD) },
            )
            Text(
                text = stringResource(R.string.today_start_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onStudy(StudyMode.REVIEW, StudyFormat.FLASHCARD) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = state.dueNow > 0,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.today_only_reviews))
                }
                OutlinedButton(
                    onClick = { onStudy(StudyMode.NEW, StudyFormat.FLASHCARD) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = state.newAvailable > 0,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.today_only_new))
                }
            }

            SectionTitle(text = stringResource(R.string.today_practice))
            Text(
                text = stringResource(R.string.today_practice_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PracticeButton(
                    title = stringResource(R.string.format_choice),
                    modifier = Modifier.weight(1f),
                    onClick = { onStudy(StudyMode.MIXED, StudyFormat.CHOICE) },
                )
                PracticeButton(
                    title = stringResource(R.string.format_typing),
                    modifier = Modifier.weight(1f),
                    onClick = { onStudy(StudyMode.MIXED, StudyFormat.TYPING) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PracticeButton(
                    title = stringResource(R.string.format_listen),
                    modifier = Modifier.weight(1f),
                    onClick = { onStudy(StudyMode.MIXED, StudyFormat.LISTEN) },
                )
                PracticeButton(
                    title = stringResource(R.string.format_flashcard),
                    modifier = Modifier.weight(1f),
                    onClick = { onStudy(StudyMode.REVIEW, StudyFormat.FLASHCARD) },
                )
            }

            Panel {
                SectionTitle(text = stringResource(R.string.today_last7))
                MiniBars(days = state.last7)
                val nextDue = state.nextDueText
                Text(
                    text = nextDue?.let {
                        stringResource(R.string.today_next_due, it)
                    } ?: stringResource(R.string.today_no_next_due),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.loading && state.totalWords == 0) {
                Panel {
                    EmptyState(
                        title = stringResource(R.string.today_empty_title),
                        hint = stringResource(R.string.today_empty_hint),
                        actionLabel = stringResource(R.string.today_add_word),
                        onAction = onAddWord,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onAddWord,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.today_add_word))
                }
                OutlinedButton(
                    onClick = onLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.today_library))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PracticeButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text = title)
    }
}
