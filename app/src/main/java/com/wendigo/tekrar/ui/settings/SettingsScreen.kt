package com.wendigo.tekrar.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.contracts.SettingsUiState

private const val MIN_NEW_PER_DAY = 0
private const val MAX_NEW_PER_DAY = 200
private const val STEP_NEW_PER_DAY = 5

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNewPerDayChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val current = state.newPerDay.coerceIn(MIN_NEW_PER_DAY, MAX_NEW_PER_DAY)
    val decreaseDescription = stringResource(R.string.settings_decrease_cd)
    val increaseDescription = stringResource(R.string.settings_increase_cd)

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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.settings_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (state.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.settings_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_new_per_day),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(
                            onClick = {
                                onNewPerDayChange(
                                    (current - STEP_NEW_PER_DAY).coerceIn(
                                        MIN_NEW_PER_DAY,
                                        MAX_NEW_PER_DAY,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp),
                            enabled = current > MIN_NEW_PER_DAY,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_minus),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics {
                                    contentDescription = decreaseDescription
                                },
                            )
                        }

                        Text(
                            text = current.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        OutlinedButton(
                            onClick = {
                                onNewPerDayChange(
                                    (current + STEP_NEW_PER_DAY).coerceIn(
                                        MIN_NEW_PER_DAY,
                                        MAX_NEW_PER_DAY,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp),
                            enabled = current < MAX_NEW_PER_DAY,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_plus),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics {
                                    contentDescription = increaseDescription
                                },
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_new_per_day_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_info_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_info_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_total_words, state.totalWords),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.settings_reviewed_today, state.reviewedToday),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
