package com.wendigo.tekrar.ui.stats

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
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
fun StatsScreen(state: StatsUiState) {
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
                    Text(
                        text = state.retention?.let {
                            stringResource(
                                R.string.stats_retention_value,
                                String.format(Locale.US, "%.0f", it * 100),
                            )
                        } ?: stringResource(R.string.stats_retention_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
