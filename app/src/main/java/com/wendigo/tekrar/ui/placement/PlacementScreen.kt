package com.wendigo.tekrar.ui.placement

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ProgressBar
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.contracts.PlacementUiState

@Composable
fun PlacementScreen(
    state: PlacementUiState,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.placement_title),
                subtitle = stringResource(R.string.placement_subtitle),
                onBack = onBack,
            )

            when {
                state.loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.empty -> EmptyState(
                    title = stringResource(R.string.placement_empty_title),
                    hint = stringResource(R.string.placement_empty_hint),
                )

                state.finished -> PlacementResult(
                    state = state,
                    onApply = onApply,
                    onRetry = onRetry,
                )

                else -> {
                    val question = state.current
                    if (question != null) {
                        Text(
                            text = stringResource(
                                R.string.placement_progress,
                                state.index + 1,
                                state.total,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProgressBar(
                            fraction = if (state.total > 0) {
                                (state.index + 1).toFloat() / state.total
                            } else {
                                0f
                            },
                        )
                        Panel {
                            if (question.cefr.isNotBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.placement_question_cefr,
                                        question.cefr,
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = question.term,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.placement_question),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.answered) {
                                Text(
                                    text = stringResource(
                                        R.string.placement_reveal,
                                        question.meaning,
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Text(
                            text = stringResource(
                                R.string.placement_known_count,
                                state.known,
                                state.total,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!state.answered) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onAnswer(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(text = stringResource(R.string.placement_dont_know))
                                }
                                PrimaryButton(
                                    text = stringResource(R.string.placement_know),
                                    onClick = { onAnswer(true) },
                                    modifier = Modifier.weight(1f),
                                    height = 52.dp,
                                )
                            }
                        } else {
                            PrimaryButton(
                                text = stringResource(R.string.placement_next),
                                onClick = onNext,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PlacementResult(
    state: PlacementUiState,
    onApply: () -> Unit,
    onRetry: () -> Unit,
) {
    Panel {
        Text(
            text = stringResource(R.string.placement_result_title, state.band),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.placement_result_desc,
                state.recommendedNewPerDay,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.placement_known_count,
                state.known,
                state.total,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (state.applied) {
            Text(
                text = stringResource(
                    R.string.placement_applied,
                    state.recommendedNewPerDay,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.placement_apply),
                onClick = onApply,
            )
        }
    }
    OutlinedButton(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text = stringResource(R.string.placement_retry))
    }
}
