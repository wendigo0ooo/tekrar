package com.wendigo.tekrar.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.contracts.DashboardUiState
import com.wendigo.tekrar.ui.contracts.StudyMode

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onStudy: (StudyMode) -> Unit,
    onAddWord: () -> Unit,
    onSettings: () -> Unit,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onSettings) {
                    Text(text = stringResource(R.string.dashboard_settings))
                }
            }

            Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (state.loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_due_now),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.dueNow.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        StatLine(
                            label = stringResource(R.string.dashboard_new_words),
                            value = state.newAvailable,
                        )
                        StatLine(
                            label = stringResource(R.string.dashboard_total_words),
                            value = state.totalWords,
                        )
                        StatLine(
                            label = stringResource(R.string.dashboard_reviewed_today),
                            value = state.reviewedToday,
                        )

                        val nextDue = state.nextDueText
                        if (nextDue != null) {
                            Text(
                                text = stringResource(R.string.dashboard_next_due, nextDue),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!state.loading && state.totalWords == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.dashboard_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = { onStudy(StudyMode.MIXED) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = stringResource(R.string.dashboard_start))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onStudy(StudyMode.REVIEW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.dueNow > 0,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_only_reviews),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = { onStudy(StudyMode.NEW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.newAvailable > 0,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_only_new),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            OutlinedButton(
                onClick = onAddWord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(text = stringResource(R.string.dashboard_add_word))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
