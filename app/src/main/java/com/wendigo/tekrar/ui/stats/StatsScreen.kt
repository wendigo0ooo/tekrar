package com.wendigo.tekrar.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.RetentionBucket
import com.wendigo.tekrar.core.StatsAnalysis
import com.wendigo.tekrar.core.TagPerformance
import com.wendigo.tekrar.ui.components.ForecastRows
import com.wendigo.tekrar.ui.components.GoalRing
import com.wendigo.tekrar.ui.components.Heatmap
import com.wendigo.tekrar.ui.components.MiniBars
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ProgressBar
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.components.StatTile
import com.wendigo.tekrar.ui.contracts.StatsUiState
import java.util.Locale

@Composable
fun StatsScreen(state: StatsUiState, onLeeches: () -> Unit) {
    val goalProgress = if (state.dailyGoal > 0) {
        state.reviewedToday.toFloat() / state.dailyGoal
    } else {
        0f
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.stats_title),
            subtitle = stringResource(R.string.stats_subtitle),
        )

        Panel {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.stats_total),
                    value = state.totalWords.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_learned),
                    value = state.learned.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_mature),
                    value = state.mature.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.stats_young),
                    value = state.young.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_leech),
                    value = state.leech.toString(),
                    modifier = Modifier.weight(1f),
                    accent = if (state.leech > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                StatTile(
                    label = stringResource(R.string.stats_suspended),
                    value = state.suspended.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Panel(modifier = Modifier.clickable(onClick = onLeeches)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.leech_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.leech_count, state.leech),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
                        text = stringResource(R.string.stats_today),
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
                    Text(
                        text = stringResource(
                            R.string.today_weekly_goal,
                            state.weeklyActiveDays,
                            state.weeklyGoalDays,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.today_freeze_stock,
                            state.streakFreezes,
                        ) + if (state.frozenDays > 0) {
                            " · " + stringResource(
                                R.string.today_frozen_days,
                                state.frozenDays,
                            )
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionTitle(text = stringResource(R.string.stats_true_retention))
        Panel {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.stats_retention_general),
                    value = percentLabel(state.trueRetention),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_retention_mature),
                    value = percentLabel(state.matureRetention),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_retention_sample),
                    value = state.retentionSample.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.retentionSample < StatsAnalysis.MIN_RETENTION_SAMPLE) {
                Text(
                    text = stringResource(R.string.stats_retention_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            val totalReviews = state.retentionBuckets.sumOf { it.reviews }
            if (totalReviews == 0) {
                Text(
                    text = stringResource(R.string.stats_retention_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.stats_retention_curve),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RetentionRows(buckets = state.retentionBuckets)
            }
        }

        SectionTitle(text = stringResource(R.string.stats_workload))
        Panel {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.stats_workload_7),
                    value = state.workload7.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_workload_30),
                    value = state.workload30.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_workload_overdue),
                    value = state.overdueNow.toString(),
                    modifier = Modifier.weight(1f),
                    accent = if (state.overdueNow > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Text(
                text = stringResource(R.string.stats_workload_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle(text = stringResource(R.string.stats_tag_title))
        Panel {
            if (state.tagPerformance.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_tag_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TagRows(tags = state.tagPerformance)
            }
        }

        SectionTitle(text = stringResource(R.string.stats_last30))
        Panel {
            MiniBars(days = state.last30, labelEvery = 5)
        }

        SectionTitle(
            text = stringResource(R.string.stats_activity),
            trailing = {
                Text(
                    text = stringResource(R.string.stats_activity_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Panel {
            Heatmap(days = state.heatmap)
        }

        SectionTitle(text = stringResource(R.string.stats_forecast))
        Panel {
            if (state.forecast.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_forecast_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ForecastRows(items = state.forecast)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun RetentionRows(buckets: List<RetentionBucket>) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        buckets.forEach { bucket ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = bucket.label,
                    modifier = Modifier.width(64.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProgressBar(
                    fraction = (bucket.accuracy ?: 0.0).toFloat(),
                    modifier = Modifier.weight(1f),
                    height = 7.dp,
                )
                Text(
                    text = percentLabel(bucket.accuracy),
                    modifier = Modifier.width(42.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = bucket.reviews.toString(),
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TagRows(tags: List<TagPerformance>) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        tags.forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tag.tag,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = percentLabel(tag.accuracy),
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = stringResource(R.string.stats_tag_due, tag.dueNow),
                    modifier = Modifier.width(62.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private fun percentLabel(value: Double?): String =
    value?.let { String.format(Locale.US, "%.0f%%", it * 100) } ?: "—"
