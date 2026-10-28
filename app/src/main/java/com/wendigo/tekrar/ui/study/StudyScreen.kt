package com.wendigo.tekrar.ui.study

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.audio.Speaker
import com.wendigo.tekrar.core.Srs
import com.wendigo.tekrar.ui.components.EmptyState
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.PrimaryButton
import com.wendigo.tekrar.ui.components.ProgressBar
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.contracts.StudyCardUi
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyUiState

@Composable
fun StudyScreen(
    state: StudyUiState,
    autoSpeak: Boolean,
    speechRate: Float,
    onReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onChoose: (String) -> Unit,
    onSubmitTyping: (String) -> Unit,
    onContinue: () -> Unit,
    onUndo: () -> Unit,
    onReload: () -> Unit,
    onExit: () -> Unit,
    onLibrary: () -> Unit,
) {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    val snackbar = remember { SnackbarHostState() }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    LaunchedEffect(state.feedbackNonce) {
        if (state.feedbackNonce == 0L || state.finished || state.empty) return@LaunchedEffect
        val text = state.autoSuspendedTerm?.let {
            context.getString(R.string.study_auto_suspended, it)
        } ?: context.getString(R.string.study_saved)
        val result = snackbar.showSnackbar(
            message = text,
            actionLabel = context.getString(R.string.study_undo),
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
    LaunchedEffect(state.card?.id, state.position, state.format, autoSpeak, speechRate) {
        val card = state.card ?: return@LaunchedEffect
        if (state.finished || state.empty) return@LaunchedEffect
        speaker.setRate(speechRate)
        val speakTerm = when (state.format) {
            StudyFormat.LISTEN -> true
            StudyFormat.FLASHCARD -> autoSpeak
            StudyFormat.CHOICE, StudyFormat.TYPING, StudyFormat.CLOZE -> false
        }
        if (speakTerm) speaker.speak(card.term)
    }
    LaunchedEffect(state.card?.id, state.answered, state.format, speechRate) {
        val card = state.card ?: return@LaunchedEffect
        if (state.format != StudyFormat.CLOZE || !state.answered) return@LaunchedEffect
        speaker.setRate(speechRate)
        val sentence = card.example.ifBlank { card.term }
        if (sentence.isNotBlank()) speaker.speak(sentence)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onExit) {
                    Text(
                        text = stringResource(R.string.study_close),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (!state.empty && !state.finished) {
                        Text(
                            text = stringResource(R.string.study_progress, state.position, state.total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressBar(
                            fraction = if (state.total > 0) {
                                state.position.toFloat() / state.total
                            } else {
                                0f
                            },
                            height = 6.dp,
                        )
                    }
                }
                if (state.canUndo) {
                    TextButton(onClick = onUndo) {
                        Text(
                            text = stringResource(R.string.study_undo),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }

            when {
                state.loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.finished -> FinishedContent(
                    state = state,
                    onReload = onReload,
                    onExit = onExit,
                    modifier = Modifier.weight(1f),
                )

                state.empty -> EmptyContent(
                    onLibrary = onLibrary,
                    onExit = onExit,
                    modifier = Modifier.weight(1f),
                )

                state.card != null -> {
                    val card = state.card
                    if (card != null) {
                        StudyContent(
                            state = state,
                            card = card,
                            onReveal = onReveal,
                            onRate = onRate,
                            onChoose = onChoose,
                            onSubmitTyping = onSubmitTyping,
                            onContinue = onContinue,
                            onSpeak = { speaker.speak(card.term) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StudyContent(
    state: StudyUiState,
    card: StudyCardUi,
    onReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onChoose: (String) -> Unit,
    onSubmitTyping: (String) -> Unit,
    onContinue: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(
            text = when (state.format) {
                StudyFormat.FLASHCARD -> stringResource(R.string.study_mode_flashcard)
                StudyFormat.CHOICE -> stringResource(R.string.study_mode_choice)
                StudyFormat.TYPING -> stringResource(R.string.study_mode_typing)
                StudyFormat.LISTEN -> stringResource(R.string.study_mode_listen)
                StudyFormat.CLOZE -> stringResource(R.string.study_mode_cloze)
            },
        )

        if (card.isLeech) {
            Text(
                text = stringResource(R.string.study_leech_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        when (state.format) {
            StudyFormat.FLASHCARD -> FlashcardCard(
                card = card,
                revealed = state.revealed,
            )

            StudyFormat.CHOICE -> PromptCard(
                card = card,
                showTerm = true,
                showMeaning = false,
                hint = stringResource(R.string.study_prompt_choice),
            )

            StudyFormat.TYPING -> PromptCard(
                card = card,
                showTerm = false,
                showMeaning = true,
                hint = stringResource(R.string.study_prompt_typing),
            )

            StudyFormat.LISTEN -> ListenCard(
                card = card,
                answered = state.answered,
                onSpeak = onSpeak,
            )

            StudyFormat.CLOZE -> {
                val prompt = state.clozePrompt
                if (prompt != null) {
                    ClozeCard(
                        card = card,
                        prompt = prompt,
                        answered = state.answered,
                    )
                } else {
                    PromptCard(
                        card = card,
                        showTerm = false,
                        showMeaning = true,
                        hint = stringResource(R.string.study_prompt_cloze_fallback),
                    )
                }
            }
        }

        when (state.format) {
            StudyFormat.FLASHCARD -> {
                if (!state.revealed) {
                    PrimaryButton(
                        text = stringResource(R.string.study_reveal),
                        onClick = onReveal,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.study_rating_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RatingRow(
                        intervals = state.intervalLabels,
                        onRate = onRate,
                    )
                }
            }

            StudyFormat.CHOICE, StudyFormat.LISTEN -> {
                OptionList(
                    options = state.options,
                    correct = card.meaning,
                    selected = state.selectedOption,
                    answered = state.answered,
                    onChoose = onChoose,
                )
                if (state.answered) {
                    AnswerFeedback(
                        correct = state.answerCorrect == true,
                        answer = card.meaning,
                        example = card.example,
                        exampleTr = card.exampleTr,
                        onContinue = onContinue,
                    )
                }
            }

            StudyFormat.TYPING -> {
                if (!state.answered) {
                    TypingAnswerInput(
                        card = card,
                        position = state.position,
                        placeholder = stringResource(R.string.study_typing_hint),
                        onSubmitTyping = onSubmitTyping,
                    )
                } else {
                    AnswerFeedback(
                        correct = state.answerCorrect == true,
                        answer = card.term,
                        example = card.example,
                        exampleTr = card.exampleTr,
                        onContinue = onContinue,
                    )
                }
            }

            StudyFormat.CLOZE -> {
                if (!state.answered) {
                    TypingAnswerInput(
                        card = card,
                        position = state.position,
                        placeholder = stringResource(R.string.study_cloze_hint),
                        onSubmitTyping = onSubmitTyping,
                    )
                } else {
                    AnswerFeedback(
                        correct = state.answerCorrect == true,
                        answer = state.clozeAnswer ?: card.term,
                        example = card.example,
                        exampleTr = card.exampleTr,
                        onContinue = onContinue,
                    )
                }
            }
        }

        if (!state.answered && (state.format == StudyFormat.FLASHCARD && state.revealed)) {
            Spacer(modifier = Modifier.height(2.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.study_correct_count, state.correctCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.study_again_count, state.againCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun FlashcardCard(card: StudyCardUi, revealed: Boolean) {
    Panel(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        if (card.pos.isNotBlank()) {
            Text(
                text = card.pos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = card.term,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!revealed) {
            Text(
                text = stringResource(R.string.study_prompt_flashcard),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = card.meaning,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (card.example.isNotBlank()) {
                Text(
                    text = card.example,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.exampleTr.isNotBlank()) {
                Text(
                    text = card.exampleTr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.notes.isNotBlank()) {
                Text(
                    text = card.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PromptCard(
    card: StudyCardUi,
    showTerm: Boolean,
    showMeaning: Boolean,
    hint: String,
) {
    Panel(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        if (card.pos.isNotBlank()) {
            Text(
                text = card.pos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showTerm) {
            Text(
                text = card.term,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showMeaning) {
            Text(
                text = card.meaning,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClozeCard(
    card: StudyCardUi,
    prompt: String,
    answered: Boolean,
) {
    Panel(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        if (card.pos.isNotBlank()) {
            Text(
                text = card.pos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = prompt,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (answered && card.meaning.isNotBlank()) {
            Text(
                text = card.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypingAnswerInput(
    card: StudyCardUi,
    position: Int,
    placeholder: String,
    onSubmitTyping: (String) -> Unit,
) {
    var typed by remember(card.id, position) { mutableStateOf("") }
    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = placeholder) },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmitTyping(typed) },
        ),
    )
    PrimaryButton(
        text = stringResource(R.string.study_check),
        onClick = { onSubmitTyping(typed) },
        enabled = typed.isNotBlank(),
    )
}

@Composable
private fun ListenCard(
    card: StudyCardUi,
    answered: Boolean,
    onSpeak: () -> Unit,
) {
    Panel(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        Text(
            text = "♪",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onSpeak,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = stringResource(R.string.study_play))
        }
        Text(
            text = if (answered) card.term else stringResource(R.string.study_prompt_listen),
            style = if (answered) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (answered) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun OptionList(
    options: List<String>,
    correct: String,
    selected: String?,
    answered: Boolean,
    onChoose: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        options.forEach { option ->
            val isCorrect = option == correct
            val isSelected = option == selected
            val color = when {
                !answered -> MaterialTheme.colorScheme.onSurface
                isCorrect -> MaterialTheme.colorScheme.primary
                isSelected -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            OutlinedButton(
                onClick = { onChoose(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !answered,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RatingRow(intervals: List<String>, onRate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingButton(
            label = stringResource(R.string.rating_again),
            interval = intervals.getOrNull(0).orEmpty(),
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = { onRate(Srs.AGAIN) },
        )
        RatingButton(
            label = stringResource(R.string.rating_hard),
            interval = intervals.getOrNull(1).orEmpty(),
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = { onRate(Srs.HARD) },
        )
        RatingButton(
            label = stringResource(R.string.rating_good),
            interval = intervals.getOrNull(2).orEmpty(),
            filled = true,
            modifier = Modifier.weight(1f),
            onClick = { onRate(Srs.GOOD) },
        )
        RatingButton(
            label = stringResource(R.string.rating_easy),
            interval = intervals.getOrNull(3).orEmpty(),
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = { onRate(Srs.EASY) },
        )
    }
}

@Composable
private fun RatingButton(
    label: String,
    interval: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            if (interval.isNotBlank()) {
                Text(
                    text = interval,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(60.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(60.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AnswerFeedback(
    correct: Boolean,
    answer: String,
    example: String,
    exampleTr: String,
    onContinue: () -> Unit,
) {
    Panel {
        Text(
            text = if (correct) {
                stringResource(R.string.study_correct)
            } else {
                stringResource(R.string.study_wrong)
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (correct) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Text(
            text = stringResource(R.string.study_correct_answer, answer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (example.isNotBlank()) {
            Text(
                text = example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (exampleTr.isNotBlank()) {
            Text(
                text = exampleTr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(
            text = stringResource(R.string.study_continue),
            onClick = onContinue,
            height = 48.dp,
        )
    }
}

@Composable
private fun FinishedContent(
    state: StudyUiState,
    onReload: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.study_finished_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.study_finished_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Panel {
            Row(modifier = Modifier.fillMaxWidth()) {
                com.wendigo.tekrar.ui.components.StatTile(
                    label = stringResource(R.string.study_reviewed),
                    value = state.reviewed.toString(),
                    modifier = Modifier.weight(1f),
                )
                com.wendigo.tekrar.ui.components.StatTile(
                    label = stringResource(R.string.study_summary_accuracy),
                    value = stringResource(
                        R.string.study_summary_percent,
                        state.accuracyPercent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                com.wendigo.tekrar.ui.components.StatTile(
                    label = stringResource(R.string.study_summary_strengthened),
                    value = state.strengthenedCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                com.wendigo.tekrar.ui.components.StatTile(
                    label = stringResource(R.string.study_summary_struggling),
                    value = state.strugglingCount.toString(),
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.error,
                )
                val tomorrowDue = state.tomorrowDue
                if (tomorrowDue != null) {
                    com.wendigo.tekrar.ui.components.StatTile(
                        label = stringResource(R.string.study_summary_tomorrow_label),
                        value = stringResource(
                            R.string.study_summary_tomorrow_value,
                            tomorrowDue,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = stringResource(R.string.study_elapsed, formatDuration(state.elapsedMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.hardWords.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
                Text(
                    text = stringResource(R.string.study_summary_hardest),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                state.hardWords.forEach { hard ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = hard.term,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.study_summary_hard_failures,
                                hard.failures,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.study_restart),
            onClick = onReload,
        )
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = stringResource(R.string.study_finish))
        }
    }
}

@Composable
private fun EmptyContent(
    onLibrary: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            title = stringResource(R.string.study_empty_title),
            hint = stringResource(R.string.study_empty_hint),
        )
        OutlinedButton(
            onClick = onLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = stringResource(R.string.study_library))
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onExit) {
            Text(text = stringResource(R.string.study_finish))
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) {
        String.format(java.util.Locale.US, "%d dk %02d sn", minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%d sn", seconds)
    }
}
