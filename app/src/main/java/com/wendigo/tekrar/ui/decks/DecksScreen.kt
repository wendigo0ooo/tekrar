package com.wendigo.tekrar.ui.decks

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
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.contracts.DeckCardUi
import com.wendigo.tekrar.ui.contracts.DecksUiState

@Composable
fun DecksScreen(
    state: DecksUiState,
    message: String?,
    onClearMessage: () -> Unit,
    onInstall: (DeckCardUi) -> Unit,
    onStudy: (String) -> Unit,
    onStudyLibrary: () -> Unit,
    onPlacement: () -> Unit,
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
                title = stringResource(R.string.decks_title),
                subtitle = stringResource(R.string.decks_subtitle),
                onBack = onBack,
            )

            if (message != null) {
                Panel {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = onClearMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(text = stringResource(R.string.decks_message_dismiss))
                    }
                }
            }

            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Panel {
                DeckRow(
                    card = state.library,
                    busy = false,
                    actionLabel = stringResource(R.string.decks_study),
                    onAction = onStudyLibrary,
                )
            }

            OutlinedButton(
                onClick = onPlacement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(text = stringResource(R.string.decks_placement))
            }

            SectionTitle(text = stringResource(R.string.decks_installed))
            if (state.installed.isEmpty()) {
                Text(
                    text = stringResource(R.string.decks_empty_installed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.installed.forEach { card ->
                    Panel {
                        DeckRow(
                            card = card,
                            busy = state.busy,
                            actionLabel = stringResource(R.string.decks_study),
                            onAction = { onStudy(card.name) },
                        )
                    }
                }
            }

            SectionTitle(text = stringResource(R.string.decks_available))
            if (state.available.isEmpty()) {
                Text(
                    text = stringResource(R.string.decks_empty_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.available.forEach { card ->
                    Panel {
                        DeckRow(
                            card = card,
                            busy = state.busy,
                            actionLabel = stringResource(R.string.decks_install),
                            onAction = { onInstall(card) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeckRow(
    card: DeckCardUi,
    busy: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (card.cefr.isNotBlank()) {
                Text(
                    text = card.cefr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (card.description.isNotBlank()) {
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.decks_words, card.wordCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.decks_due, card.dueCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = actionLabel)
        }
    }
}
