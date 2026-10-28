package com.wendigo.tekrar.ui.mine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.audio.Speaker
import com.wendigo.tekrar.core.MiningSource
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.contracts.MineCandidateUi
import com.wendigo.tekrar.ui.contracts.MineUiState
import kotlinx.coroutines.launch

@Composable
fun MineScreen(
    state: MineUiState,
    onInput: (String) -> Unit,
    onAnalyze: () -> Unit,
    onAdd: (MineCandidateUi) -> Unit,
    onAddAll: () -> Unit,
    onImportDictionary: (android.net.Uri) -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val speaker = remember { Speaker(context) }
    var showBulk by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }

    val dictionaryImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImportDictionary(uri) }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbar.showSnackbar(text)
            onClearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.mine_title),
                    subtitle = stringResource(R.string.mine_subtitle),
                    onBack = onBack,
                    trailing = {
                        TextButton(
                            onClick = {
                                dictionaryImport.launch(
                                    arrayOf("application/json", "text/plain")
                                )
                            },
                        ) {
                            Text(text = stringResource(R.string.mine_dictionary_import))
                        }
                    },
                )
            }

            item {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = stringResource(R.string.mine_input_hint)) },
                    minLines = 6,
                    maxLines = 12,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            item {
                PrimaryButton(
                    text = stringResource(R.string.mine_analyze),
                    onClick = onAnalyze,
                    enabled = !state.analyzing,
                )
            }

            if (state.candidates.isNotEmpty()) {
                item {
                    Panel {
                        Text(
                            text = stringResource(
                                R.string.mine_found_count,
                                state.candidates.size,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.mine_found_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { showBulk = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !state.adding,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(text = stringResource(R.string.mine_add_all))
                        }
                    }
                }
            }

            if (state.analyzed && state.candidates.isEmpty()) {
                item {
                    Panel {
                        EmptyState(
                            title = stringResource(
                                if (state.input.isBlank()) {
                                    R.string.mine_no_text_title
                                } else {
                                    R.string.mine_empty_title
                                }
                            ),
                            hint = stringResource(
                                if (state.input.isBlank()) {
                                    R.string.mine_no_text_hint
                                } else {
                                    R.string.mine_empty_hint
                                }
                            ),
                        )
                    }
                }
            }

            items(state.candidates, key = { it.term }) { candidate ->
                CandidateRow(
                    candidate = candidate,
                    enabled = !state.adding,
                    onAdd = { onAdd(candidate) },
                    onSpeakWord = { speaker.speak(candidate.term) },
                    onSpeakContext = { speaker.speak(candidate.context) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }

    if (showBulk) {
        AlertDialog(
            onDismissRequest = { showBulk = false },
            title = { Text(text = stringResource(R.string.mine_add_all_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.mine_add_all_text,
                        state.candidates.size,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBulk = false
                        onAddAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = stringResource(R.string.mine_add_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulk = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun CandidateRow(
    candidate: MineCandidateUi,
    enabled: Boolean,
    onAdd: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakContext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = candidate.term,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (candidate.cefr.isNotBlank()) {
                Text(
                    text = candidate.cefr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (candidate.pos.isNotBlank()) {
                Text(
                    text = candidate.pos,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = candidate.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (candidate.context.isNotBlank()) {
            Text(
                text = candidate.context,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    if (candidate.source == MiningSource.DECK) {
                        R.string.mine_source_deck
                    } else {
                        R.string.mine_source_dictionary
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSpeakWord) {
                Text(text = stringResource(R.string.mine_speak_word))
            }
            TextButton(onClick = onSpeakContext) {
                Text(text = stringResource(R.string.mine_speak_context))
            }
            TextButton(onClick = onAdd, enabled = enabled) {
                Text(text = stringResource(R.string.mine_add))
            }
        }
    }
}
