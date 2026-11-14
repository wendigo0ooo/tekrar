package com.wendigo.tekrar.ui.learn

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.learn.AnswerNormalizer
import com.wendigo.tekrar.core.learn.Choice
import com.wendigo.tekrar.core.learn.Cloze
import com.wendigo.tekrar.core.learn.DialogueChoice
import com.wendigo.tekrar.core.learn.Example
import com.wendigo.tekrar.core.learn.Exercise
import com.wendigo.tekrar.core.learn.ListenChoice
import com.wendigo.tekrar.core.learn.Match
import com.wendigo.tekrar.core.learn.Speak
import com.wendigo.tekrar.core.learn.TeachCard
import com.wendigo.tekrar.core.learn.Translate
import com.wendigo.tekrar.core.learn.WordOrder
import com.wendigo.tekrar.core.learn.Write
import com.wendigo.tekrar.ui.components.learn.CheckGlyph
import com.wendigo.tekrar.ui.components.learn.CrossGlyph
import com.wendigo.tekrar.ui.components.learn.FeedbackGlyph
import com.wendigo.tekrar.ui.components.learn.LearnCard
import com.wendigo.tekrar.ui.components.learn.LearnPrimaryButton
import com.wendigo.tekrar.ui.components.learn.LearnProgressBar
import com.wendigo.tekrar.ui.components.learn.LearnSecondaryButton
import com.wendigo.tekrar.ui.components.learn.MicGlyph
import com.wendigo.tekrar.ui.components.learn.PlayGlyph
import kotlin.random.Random

data class LessonFeedbackUi(
    val correct: Boolean,
    val expectedAnswer: String,
    val feedbackTr: String,
    val scorePercent: Int,
)

data class LessonUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val lessonTitle: String = "",
    val unitTitle: String = "",
    val phase: LessonPhase = LessonPhase.TEACH,
    val stepIndex: Int = 0,
    val stepTotal: Int = 0,
    val exerciseNumber: Int = 0,
    val exerciseTotal: Int = 0,
    val progress: Float = 0f,
    val teachCard: TeachCard? = null,
    val exercise: Exercise? = null,
    val feedback: LessonFeedbackUi? = null,
    val submitFailed: Boolean = false,
    val savingResult: Boolean = false,
    val result: LessonResultUiState? = null,
)

@Composable
fun LessonScreen(
    state: LessonUiState,
    onClose: () -> Unit,
    onSpeak: (String) -> Unit,
    onContinueTeach: () -> Unit,
    onSubmitChoice: (Int) -> Unit,
    onSubmitMatch: (Map<String, String>) -> Unit,
    onSubmitTokens: (List<String>) -> Unit,
    onSubmitText: (String) -> Unit,
    onSubmitSpeech: (String) -> Unit,
    onNext: () -> Unit,
    onRetryLoad: () -> Unit,
    onResultBack: () -> Unit,
    onResultPractice: (() -> Unit)? = null,
    onRetrySave: (() -> Unit)? = null,
) {
    val result = state.result
    if (result != null) {
        LessonResultScreen(
            state = result,
            lessonTitle = state.lessonTitle,
            onBackToLearn = onResultBack,
            onPractice = onResultPractice,
            onRetrySave = onRetrySave,
        )
    } else {
        when {
            state.loading -> LessonLoadingScreen(onClose = onClose)
            state.loadFailed -> LessonLoadErrorScreen(
                onRetry = onRetryLoad,
                onClose = onClose,
            )

            state.savingResult || state.phase == LessonPhase.FINISHED -> LessonSavingScreen()
            else -> LessonTaskScreen(
                state = state,
                onClose = onClose,
                onSpeak = onSpeak,
                onContinueTeach = onContinueTeach,
                onSubmitChoice = onSubmitChoice,
                onSubmitMatch = onSubmitMatch,
                onSubmitTokens = onSubmitTokens,
                onSubmitText = onSubmitText,
                onSubmitSpeech = onSubmitSpeech,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun LessonTaskScreen(
    state: LessonUiState,
    onClose: () -> Unit,
    onSpeak: (String) -> Unit,
    onContinueTeach: () -> Unit,
    onSubmitChoice: (Int) -> Unit,
    onSubmitMatch: (Map<String, String>) -> Unit,
    onSubmitTokens: (List<String>) -> Unit,
    onSubmitText: (String) -> Unit,
    onSubmitSpeech: (String) -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val exercise = state.exercise
    val phase = state.phase
    val stepKey = state.stepIndex
    val answered = state.feedback != null
    val scrollState = rememberScrollState()

    LaunchedEffect(stepKey) {
        scrollState.scrollTo(0)
    }
    LaunchedEffect(state.feedback != null) {
        if (state.feedback != null) {
            withFrameNanos { }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    var selectedIndex by remember(stepKey) { mutableStateOf<Int?>(null) }
    var typed by remember(stepKey) { mutableStateOf("") }
    var matchPairs by remember(stepKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedLeft by remember(stepKey) { mutableStateOf<String?>(null) }
    var selectedRight by remember(stepKey) { mutableStateOf<String?>(null) }
    var orderedIndices by remember(stepKey) { mutableStateOf<List<Int>>(emptyList()) }
    var speechText by remember(stepKey) { mutableStateOf("") }
    var speechFailure by remember(stepKey) { mutableStateOf<SpeechInputError?>(null) }
    var listening by remember(stepKey) { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val speech = remember(stepKey) {
        SpeechInput(
            context = context,
            onListeningChanged = { listening = it },
            onResult = {
                speechText = it
                speechFailure = null
                listening = false
            },
            onFailure = { error ->
                listening = false
                speechFailure = error
                if (error == SpeechInputError.PERMISSION) permissionDenied = true
            },
        )
    }
    val speechAvailable = remember(stepKey) { speech.isAvailable() }
    val currentSpeech by rememberUpdatedState(speech)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        if (granted) currentSpeech.start()
    }
    DisposableEffect(stepKey) {
        onDispose { speech.destroy() }
    }

    val canSubmit = when (exercise) {
        is Choice, is ListenChoice, is DialogueChoice -> selectedIndex != null
        is Match -> matchPairs.size == exercise.pairs.size
        is WordOrder -> orderedIndices.size == exercise.tokens.size
        is Cloze -> if (exercise.options != null) selectedIndex != null else typed.isNotBlank()
        is Translate -> typed.isNotBlank()
        is Speak -> speechText.isNotBlank()
        is Write -> typed.isNotBlank()
        null -> false
    }

    val submitAction: () -> Unit = {
        when (exercise) {
            is Choice, is ListenChoice, is DialogueChoice -> {
                selectedIndex?.let(onSubmitChoice)
            }

            is Match -> {
                if (matchPairs.size == exercise.pairs.size) onSubmitMatch(matchPairs)
            }

            is WordOrder -> {
                if (orderedIndices.size == exercise.tokens.size) {
                    onSubmitTokens(orderedIndices.map { exercise.tokens[it] })
                }
            }

            is Cloze -> {
                if (exercise.options != null) {
                    selectedIndex?.let(onSubmitChoice)
                } else {
                    onSubmitText(typed)
                }
            }

            is Translate, is Write -> onSubmitText(typed)
            is Speak -> onSubmitSpeech(speechText)
            null -> Unit
        }
    }

    val primaryText = when {
        phase == LessonPhase.TEACH -> R.string.learn_lesson_continue
        phase == LessonPhase.FEEDBACK -> R.string.learn_lesson_continue
        else -> R.string.learn_lesson_check
    }
    val primaryEnabled = when {
        phase == LessonPhase.TEACH || phase == LessonPhase.FEEDBACK -> true
        else -> canSubmit
    }
    val primaryAction: () -> Unit = when {
        phase == LessonPhase.TEACH -> onContinueTeach
        phase == LessonPhase.FEEDBACK -> onNext
        else -> submitAction
    }
    val bottomHint = if (phase == LessonPhase.EXERCISE && !canSubmit) {
        hintFor(exercise)
    } else {
        null
    }
    val progressLabel = when {
        phase == LessonPhase.TEACH -> stringResource(R.string.learn_lesson_teach)
        state.exerciseTotal > 0 -> stringResource(
            R.string.learn_lesson_progress,
            state.exerciseNumber,
            state.exerciseTotal,
        )

        else -> ""
    }

    val handleLeft: (String) -> Unit = { left ->
        if (!answered) {
            val existing = matchPairs[left]
            val right = selectedRight
            when {
                existing != null -> {
                    matchPairs = matchPairs - left
                    selectedLeft = left
                    selectedRight = null
                }

                right != null -> {
                    val withoutLeft = matchPairs - left
                    val withoutRight = withoutLeft.filterValues { it != right }
                    matchPairs = withoutRight + (left to right)
                    selectedLeft = null
                    selectedRight = null
                }

                else -> {
                    selectedLeft = if (selectedLeft == left) null else left
                }
            }
        }
    }

    val handleRight: (String) -> Unit = { right ->
        if (!answered) {
            val left = selectedLeft
            val assignedLeft = matchPairs.entries.firstOrNull { it.value == right }?.key
            when {
                left != null -> {
                    val withoutLeft = matchPairs - left
                    val withoutRight = withoutLeft.filterValues { it != right }
                    matchPairs = withoutRight + (left to right)
                    selectedLeft = null
                    selectedRight = null
                }

                assignedLeft != null -> {
                    matchPairs = matchPairs - assignedLeft
                    selectedRight = right
                }

                else -> {
                    selectedRight = if (selectedRight == right) null else right
                }
            }
        }
    }

    val onMicClick: () -> Unit = {
        if (!listening) {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                speechFailure = null
                speech.start()
            }
        } else {
            speech.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        LessonTopBar(
            lessonTitle = state.lessonTitle,
            progress = state.progress,
            progressLabel = progressLabel,
            onClose = { showExit = true },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (phase == LessonPhase.TEACH) {
                TeachCardView(card = state.teachCard)
            } else if (exercise != null) {
                ExerciseCardView(
                    exercise = exercise,
                    selectedIndex = selectedIndex,
                    typed = typed,
                    matchPairs = matchPairs,
                    selectedLeft = selectedLeft,
                    selectedRight = selectedRight,
                    orderedIndices = orderedIndices,
                    speechText = speechText,
                    speechFailure = speechFailure,
                    speechAvailable = speechAvailable,
                    permissionDenied = permissionDenied,
                    listening = listening,
                    answered = answered,
                    onSpeak = onSpeak,
                    onSelect = { selectedIndex = it },
                    onMatchLeft = handleLeft,
                    onMatchRight = handleRight,
                    onAddToken = { index ->
                        if (index !in orderedIndices) {
                            orderedIndices = orderedIndices + index
                        }
                    },
                    onRemoveToken = { index ->
                        orderedIndices = orderedIndices - index
                    },
                    onUndoToken = {
                        if (orderedIndices.isNotEmpty()) {
                            orderedIndices = orderedIndices.dropLast(1)
                        }
                    },
                    onTypedChange = { typed = it },
                    onSpeechChange = {
                        speechText = it
                        speechFailure = null
                    },
                    onMicClick = onMicClick,
                    onPlaySpeech = { onSpeak(it) },
                )
            }

            if (state.feedback != null) {
                FeedbackCard(
                    feedback = state.feedback,
                    exercise = exercise,
                )
            }

            if (state.submitFailed) {
                Text(
                    text = stringResource(R.string.learn_lesson_submit_error),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LearnPrimaryButton(
                text = stringResource(primaryText),
                onClick = primaryAction,
                enabled = primaryEnabled,
            )
            if (bottomHint != null) {
                Text(
                    text = stringResource(bottomHint),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = {
                Text(
                    text = stringResource(R.string.learn_lesson_exit_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.learn_lesson_exit_text),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExit = false
                        onClose()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.learn_lesson_exit_confirm),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExit = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.learn_lesson_exit_cancel),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    )
                }
            },
        )
    }
}

@Composable
private fun LessonTopBar(
    lessonTitle: String,
    progress: Float,
    progressLabel: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = onClose,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.learn_lesson_close),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = lessonTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progressLabel.isNotBlank()) {
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            LearnProgressBar(
                fraction = progress,
                height = 8.dp,
            )
        }
    }
}

@Composable
private fun TeachCardView(card: TeachCard?) {
    if (card == null) return
    LearnCard {
        Text(
            text = stringResource(R.string.learn_lesson_teach_badge),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = card.titleTr,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = card.bodyTr,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        card.examples.forEach { example ->
            ExampleRow(example = example)
        }
        if (!card.tipTr.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.learn_lesson_tip, card.tipTr),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ExampleRow(example: Example) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = example.en,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = example.tr,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseCardView(
    exercise: Exercise,
    selectedIndex: Int?,
    typed: String,
    matchPairs: Map<String, String>,
    selectedLeft: String?,
    selectedRight: String?,
    orderedIndices: List<Int>,
    speechText: String,
    speechFailure: SpeechInputError?,
    speechAvailable: Boolean,
    permissionDenied: Boolean,
    listening: Boolean,
    answered: Boolean,
    onSpeak: (String) -> Unit,
    onSelect: (Int) -> Unit,
    onMatchLeft: (String) -> Unit,
    onMatchRight: (String) -> Unit,
    onAddToken: (Int) -> Unit,
    onRemoveToken: (Int) -> Unit,
    onUndoToken: () -> Unit,
    onTypedChange: (String) -> Unit,
    onSpeechChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onPlaySpeech: (String) -> Unit,
) {
    LearnCard(contentPadding = PaddingValues(18.dp)) {
        Text(
            text = exerciseTypeLabel(exercise),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = exercise.promptTr,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 27.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        when (exercise) {
            is Choice -> ChoiceControls(
                options = exercise.options,
                answerIndex = exercise.answer,
                selectedIndex = selectedIndex,
                answered = answered,
                onSelect = onSelect,
            )

            is ListenChoice -> {
                AudioPlayButton(onClick = { onSpeak(exercise.audioText) })
                ChoiceControls(
                    options = exercise.options,
                    answerIndex = exercise.answer,
                    selectedIndex = selectedIndex,
                    answered = answered,
                    onSelect = onSelect,
                )
            }

            is DialogueChoice -> {
                exercise.audioText?.takeIf { it.isNotBlank() }?.let { audio ->
                    AudioPlayButton(onClick = { onSpeak(audio) })
                }
                ChoiceControls(
                    options = exercise.options,
                    answerIndex = exercise.answer,
                    selectedIndex = selectedIndex,
                    answered = answered,
                    onSelect = onSelect,
                )
            }

            is Match -> MatchControls(
                exercise = exercise,
                assignments = matchPairs,
                selectedLeft = selectedLeft,
                selectedRight = selectedRight,
                answered = answered,
                onLeft = onMatchLeft,
                onRight = onMatchRight,
            )

            is WordOrder -> WordOrderControls(
                exercise = exercise,
                selectedIndices = orderedIndices,
                answered = answered,
                onAdd = onAddToken,
                onRemove = onRemoveToken,
                onUndo = onUndoToken,
            )

            is Cloze -> ClozeControls(
                exercise = exercise,
                selectedIndex = selectedIndex,
                typed = typed,
                answered = answered,
                onSelect = onSelect,
                onTypedChange = onTypedChange,
            )

            is Translate -> {
                exercise.hintTr?.takeIf { it.isNotBlank() }?.let { hint ->
                    Text(
                        text = stringResource(R.string.learn_lesson_translate_hint, hint),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnswerTextField(
                    value = typed,
                    onValueChange = onTypedChange,
                    placeholder = stringResource(R.string.learn_lesson_translate_placeholder),
                    enabled = !answered,
                )
            }

            is Speak -> SpeakControls(
                exercise = exercise,
                speechText = speechText,
                speechFailure = speechFailure,
                speechAvailable = speechAvailable,
                permissionDenied = permissionDenied,
                listening = listening,
                answered = answered,
                onSpeak = onSpeak,
                onSpeechChange = onSpeechChange,
                onMicClick = onMicClick,
                onPlaySpeech = onPlaySpeech,
            )

            is Write -> WriteControls(
                exercise = exercise,
                typed = typed,
                answered = answered,
                onTypedChange = onTypedChange,
            )
        }
    }
}

@Composable
private fun ChoiceControls(
    options: List<String>,
    answerIndex: Int,
    selectedIndex: Int?,
    answered: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index
            val correct = index == answerIndex
            val accent = when {
                answered && correct -> MaterialTheme.colorScheme.primary
                answered && selected -> MaterialTheme.colorScheme.error
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            val background = when {
                answered && correct -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                answered && selected -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
            val contentColor = when {
                answered && correct -> MaterialTheme.colorScheme.primary
                answered && selected -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(background)
                    .border(
                        width = if (selected || (answered && correct)) 2.dp else 1.dp,
                        color = accent.copy(alpha = if (selected || answered) 0.9f else 0.55f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .selectable(
                        selected = selected,
                        enabled = !answered,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RadioGlyph(
                    selected = selected,
                    color = if (selected || (answered && correct)) {
                        accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = option,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = contentColor,
                )
                if (answered && correct) {
                    CheckGlyph(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                } else if (answered && selected) {
                    CrossGlyph(
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioGlyph(selected: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f - 1.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx()),
            )
            if (selected) {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.27f,
                )
            }
        }
    }
}

@Composable
private fun AudioPlayButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        PlayGlyph(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.learn_lesson_play),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        )
    }
}

@Composable
private fun MatchControls(
    exercise: Match,
    assignments: Map<String, String>,
    selectedLeft: String?,
    selectedRight: String?,
    answered: Boolean,
    onLeft: (String) -> Unit,
    onRight: (String) -> Unit,
) {
    val rights = remember(exercise, exercise.pairs) {
        exercise.pairs.map { it.right }.shuffled(Random(exercise.pairs.size * 31))
    }
    Text(
        text = stringResource(R.string.learn_lesson_match_hint),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exercise.pairs.forEachIndexed { index, pair ->
                val assigned = assignments[pair.left]
                val correct = answered && assigned == pair.right
                val wrong = answered && assigned != null && assigned != pair.right
                MatchItem(
                    text = pair.left,
                    selected = selectedLeft == pair.left,
                    pairedNumber = if (assigned != null) index + 1 else null,
                    correct = correct,
                    wrong = wrong,
                    answered = answered,
                    onClick = { onLeft(pair.left) },
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rights.forEach { right ->
                val assignedLeft = assignments.entries.firstOrNull { it.value == right }?.key
                val assignedIndex = exercise.pairs.indexOfFirst { it.left == assignedLeft }
                val correctLeft = exercise.pairs.firstOrNull { it.right == right }?.left
                val correct = answered && assignedLeft != null && assignedLeft == correctLeft
                val wrong = answered && assignedLeft != null && assignedLeft != correctLeft
                MatchItem(
                    text = right,
                    selected = selectedRight == right,
                    pairedNumber = if (assignedLeft != null) assignedIndex + 1 else null,
                    correct = correct,
                    wrong = wrong,
                    answered = answered,
                    onClick = { onRight(right) },
                )
            }
        }
    }
    Text(
        text = stringResource(
            R.string.learn_lesson_match_progress,
            assignments.size,
            exercise.pairs.size,
        ),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MatchItem(
    text: String,
    selected: Boolean,
    pairedNumber: Int?,
    correct: Boolean,
    wrong: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    val accent = when {
        correct -> MaterialTheme.colorScheme.primary
        wrong -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val background = when {
        correct -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        wrong -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                width = if (selected || correct || wrong) 2.dp else 1.dp,
                color = accent.copy(alpha = if (selected || answered) 0.9f else 0.55f),
                shape = RoundedCornerShape(14.dp),
            )
            .selectable(
                selected = selected,
                enabled = !answered,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = when {
                    correct -> MaterialTheme.colorScheme.primary
                    wrong -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            when {
                answered && correct -> CheckGlyph(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )

                answered && wrong -> CrossGlyph(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp),
                )

                pairedNumber != null -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pairedNumber.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordOrderControls(
    exercise: WordOrder,
    selectedIndices: List<Int>,
    answered: Boolean,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onUndo: () -> Unit,
) {
    Text(
        text = stringResource(R.string.learn_lesson_order_hint),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    exercise.translationTr?.takeIf { it.isNotBlank() }?.let { translation ->
        Text(
            text = stringResource(R.string.learn_lesson_order_translation, translation),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(10.dp),
    ) {
        if (selectedIndices.isEmpty()) {
            Text(
                text = stringResource(R.string.learn_lesson_order_empty),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selectedIndices.forEach { index ->
                    TokenChip(
                        text = exercise.tokens.getOrElse(index) { "" },
                        filled = true,
                        enabled = !answered,
                        onClick = { onRemove(index) },
                    )
                }
            }
        }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        exercise.tokens.forEachIndexed { index, token ->
            if (index !in selectedIndices) {
                TokenChip(
                    text = token,
                    filled = false,
                    enabled = !answered,
                    onClick = { onAdd(index) },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onUndo,
            enabled = selectedIndices.isNotEmpty() && !answered,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.learn_lesson_order_undo),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            )
        }
    }
}

@Composable
private fun TokenChip(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (filled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (filled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (filled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                },
                shape = RoundedCornerShape(12.dp),
            )
            .selectable(
                selected = filled,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ClozeControls(
    exercise: Cloze,
    selectedIndex: Int?,
    typed: String,
    answered: Boolean,
    onSelect: (Int) -> Unit,
    onTypedChange: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val styled = remember(exercise.sentence, accent) {
        buildAnnotatedString {
            val parts = exercise.sentence.split("___", limit = 2)
            append(parts[0])
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                append("_____")
            }
            if (parts.size > 1) append(parts[1])
        }
    }
    Text(
        text = styled,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 29.sp),
        color = MaterialTheme.colorScheme.onSurface,
    )
    val options = exercise.options
    if (options != null) {
        ChoiceControls(
            options = options,
            answerIndex = exercise.answer ?: 0,
            selectedIndex = selectedIndex,
            answered = answered,
            onSelect = onSelect,
        )
    } else {
        AnswerTextField(
            value = typed,
            onValueChange = onTypedChange,
            placeholder = stringResource(R.string.learn_lesson_cloze_text_hint),
            enabled = !answered,
        )
    }
}

@Composable
private fun SpeakControls(
    exercise: Speak,
    speechText: String,
    speechFailure: SpeechInputError?,
    speechAvailable: Boolean,
    permissionDenied: Boolean,
    listening: Boolean,
    answered: Boolean,
    onSpeak: (String) -> Unit,
    onSpeechChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onPlaySpeech: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.learn_lesson_speak_target),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = exercise.target,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 29.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    OutlinedButton(
        onClick = { onPlaySpeech(exercise.target) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        PlayGlyph(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.learn_lesson_play),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        )
    }
    exercise.hintTr?.takeIf { it.isNotBlank() }?.let { hint ->
        Text(
            text = hint,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!answered) {
        if (!speechAvailable) {
            Text(
                text = stringResource(R.string.learn_lesson_mic_unavailable),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedButton(
                onClick = onMicClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                MicGlyph(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (listening) R.string.learn_lesson_listening else R.string.learn_lesson_mic,
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                )
            }
            if (permissionDenied) {
                Text(
                    text = stringResource(R.string.learn_lesson_mic_permission),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                speechFailure?.let { failure ->
                    Text(
                        text = speechErrorText(failure),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    Text(
        text = stringResource(R.string.learn_lesson_speech_hint),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AnswerTextField(
        value = speechText,
        onValueChange = onSpeechChange,
        placeholder = stringResource(R.string.learn_lesson_transcript_placeholder),
        enabled = !answered,
    )
}

@Composable
private fun WriteControls(
    exercise: Write,
    typed: String,
    answered: Boolean,
    onTypedChange: (String) -> Unit,
) {
    val wordCount = AnswerNormalizer.wordCount(typed)
    Text(
        text = stringResource(R.string.learn_lesson_write_min, exercise.minWords),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (exercise.keywords.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.learn_lesson_write_keywords,
                exercise.keywords.joinToString(", "),
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AnswerTextField(
        value = typed,
        onValueChange = onTypedChange,
        placeholder = stringResource(R.string.learn_lesson_write_placeholder),
        enabled = !answered,
        minLines = 5,
        maxLines = 8,
    )
    Text(
        text = stringResource(R.string.learn_lesson_write_count, wordCount),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = if (wordCount >= exercise.minWords) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun AnswerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        keyboardOptions = KeyboardOptions(
            imeAction = if (maxLines == 1) ImeAction.Done else ImeAction.Default,
        ),
    )
}

@Composable
private fun FeedbackCard(
    feedback: LessonFeedbackUi,
    exercise: Exercise?,
) {
    val accent = if (feedback.correct) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    LearnCard(containerColor = accent.copy(alpha = 0.08f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeedbackGlyph(correct = feedback.correct)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(
                        if (feedback.correct) {
                            R.string.learn_lesson_feedback_correct
                        } else {
                            R.string.learn_lesson_feedback_wrong
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = accent,
                )
                if (exercise is Speak) {
                    Text(
                        text = stringResource(
                            R.string.learn_lesson_feedback_similarity,
                            feedback.scorePercent,
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = feedback.feedbackTr,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (exercise is Speak || exercise is Write) {
            val label = when (exercise) {
                is Speak -> R.string.learn_lesson_feedback_target
                is Write -> R.string.learn_lesson_feedback_sample
                else -> R.string.learn_lesson_feedback_answer
            }
            if (feedback.expectedAnswer.isNotBlank()) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = feedback.expectedAnswer,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun exerciseTypeLabel(exercise: Exercise): String = stringResource(
    when (exercise) {
        is Choice -> R.string.learn_lesson_type_choice
        is ListenChoice -> R.string.learn_lesson_type_listen
        is DialogueChoice -> R.string.learn_lesson_type_dialogue
        is Match -> R.string.learn_lesson_type_match
        is WordOrder -> R.string.learn_lesson_type_order
        is Cloze -> R.string.learn_lesson_type_cloze
        is Translate -> R.string.learn_lesson_type_translate
        is Speak -> R.string.learn_lesson_type_speak
        is Write -> R.string.learn_lesson_type_write
    },
)

private fun hintFor(exercise: Exercise?): Int = when (exercise) {
    is Choice, is ListenChoice, is DialogueChoice -> R.string.learn_lesson_hint_choice
    is Match -> R.string.learn_lesson_hint_match
    is WordOrder -> R.string.learn_lesson_hint_order
    is Cloze, is Translate, is Write -> R.string.learn_lesson_hint_text
    is Speak -> R.string.learn_lesson_hint_speak
    null -> R.string.learn_lesson_check
}

@Composable
private fun speechErrorText(error: SpeechInputError): String = stringResource(
    when (error) {
        SpeechInputError.PERMISSION -> R.string.learn_lesson_mic_permission_error
        SpeechInputError.NO_MATCH -> R.string.learn_lesson_mic_error
        SpeechInputError.NETWORK -> R.string.learn_lesson_mic_network
        SpeechInputError.BUSY -> R.string.learn_lesson_mic_busy
        SpeechInputError.UNAVAILABLE -> R.string.learn_lesson_mic_unavailable
        SpeechInputError.UNKNOWN -> R.string.learn_lesson_mic_error
    },
)

@Composable
private fun LessonLoadingScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.learn_lesson_loading),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(
            onClick = onClose,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.learn_lesson_close),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            )
        }
    }
}

@Composable
private fun LessonLoadErrorScreen(
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        LearnCard {
            Text(
                text = stringResource(R.string.learn_lesson_load_error),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.learn_lesson_load_error_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LearnPrimaryButton(
                text = stringResource(R.string.learn_retry),
                onClick = onRetry,
            )
            LearnSecondaryButton(
                text = stringResource(R.string.learn_lesson_close),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun LessonSavingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.learn_lesson_saving),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
