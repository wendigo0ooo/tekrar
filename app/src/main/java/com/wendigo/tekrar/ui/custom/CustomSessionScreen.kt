package com.wendigo.tekrar.ui.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.ActionChip
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.LabeledSwitch
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.components.Segmented
import com.wendigo.tekrar.ui.contracts.CustomSessionRequest
import com.wendigo.tekrar.ui.contracts.CustomSessionUiState
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyMode

@Composable
fun CustomSessionScreen(
    state: CustomSessionUiState,
    onMode: (StudyMode) -> Unit,
    onDeck: (String?) -> Unit,
    onTag: (String?) -> Unit,
    onLimit: (Int?) -> Unit,
    onFormat: (StudyFormat) -> Unit,
    onCram: (Boolean) -> Unit,
    onStart: (CustomSessionRequest) -> Unit,
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
                title = stringResource(R.string.custom_title),
                subtitle = stringResource(R.string.custom_subtitle),
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

                else -> {
                    if (state.empty) {
                        Panel {
                            EmptyState(
                                title = stringResource(R.string.custom_empty_title),
                                hint = stringResource(R.string.custom_empty_hint),
                            )
                        }
                    } else {
                        Panel {
                            SectionTitle(text = stringResource(R.string.custom_mode))
                            ModeSelector(mode = state.mode, onMode = onMode)
                        }

                        Panel {
                            SectionTitle(text = stringResource(R.string.custom_deck))
                            ChipRow {
                                state.decks.forEach { deck ->
                                    ActionChip(
                                        text = deck.label,
                                        selected = state.deck == deck.value,
                                        onClick = { onDeck(deck.value) },
                                    )
                                }
                            }
                        }

                        Panel {
                            SectionTitle(text = stringResource(R.string.custom_tag))
                            if (state.tags.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.custom_tag_all),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                ChipRow {
                                    ActionChip(
                                        text = stringResource(R.string.custom_tag_all),
                                        selected = state.tag == null,
                                        onClick = { onTag(null) },
                                    )
                                    state.tags.forEach { tag ->
                                        ActionChip(
                                            text = tag,
                                            selected = state.tag == tag,
                                            onClick = { onTag(tag) },
                                        )
                                    }
                                }
                            }
                        }

                        Panel {
                            SectionTitle(text = stringResource(R.string.custom_limit))
                            LimitSelector(limit = state.limit, onLimit = onLimit)
                        }

                        Panel {
                            SectionTitle(text = stringResource(R.string.custom_format))
                            ChipRow {
                                FORMATS.forEach { format ->
                                    ActionChip(
                                        text = formatLabel(format),
                                        selected = state.format == format,
                                        onClick = { onFormat(format) },
                                    )
                                }
                            }
                        }

                        Panel {
                            LabeledSwitch(
                                title = stringResource(R.string.custom_cram_title),
                                description = stringResource(R.string.custom_cram_desc),
                                checked = state.cram,
                                onCheckedChange = onCram,
                            )
                        }

                        Text(
                            text = if (state.matchingWords > 0) {
                                stringResource(
                                    R.string.custom_match_count,
                                    state.matchingWords,
                                )
                            } else {
                                stringResource(R.string.custom_no_match)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    PrimaryButton(
                        text = stringResource(R.string.custom_start),
                        enabled = state.matchingWords > 0,
                        onClick = {
                            onStart(
                                CustomSessionRequest(
                                    mode = state.mode,
                                    deck = state.deck,
                                    tag = state.tag,
                                    limit = state.limit,
                                    format = state.format,
                                    cram = state.cram,
                                )
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ModeSelector(mode: StudyMode, onMode: (StudyMode) -> Unit) {
    val modes = listOf(StudyMode.REVIEW, StudyMode.NEW, StudyMode.MIXED)
    Segmented(
        options = listOf(
            stringResource(R.string.custom_mode_due),
            stringResource(R.string.custom_mode_new),
            stringResource(R.string.custom_mode_mixed),
        ),
        selectedIndex = modes.indexOf(mode).takeIf { it >= 0 } ?: 2,
        onSelect = { onMode(modes[it]) },
    )
}

@Composable
private fun LimitSelector(limit: Int?, onLimit: (Int?) -> Unit) {
    val limits = listOf<Int?>(10, 20, 50, null)
    Segmented(
        options = listOf(
            "10",
            "20",
            "50",
            stringResource(R.string.custom_limit_all),
        ),
        selectedIndex = limits.indexOf(limit).takeIf { it >= 0 } ?: 1,
        onSelect = { onLimit(limits[it]) },
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun formatLabel(format: StudyFormat): String = stringResource(
    when (format) {
        StudyFormat.FLASHCARD -> R.string.format_flashcard
        StudyFormat.CHOICE -> R.string.format_choice
        StudyFormat.TYPING -> R.string.format_typing
        StudyFormat.LISTEN -> R.string.format_listen
        StudyFormat.CLOZE -> R.string.format_cloze
    }
)

private val FORMATS = listOf(
    StudyFormat.FLASHCARD,
    StudyFormat.CHOICE,
    StudyFormat.TYPING,
    StudyFormat.LISTEN,
    StudyFormat.CLOZE,
)
