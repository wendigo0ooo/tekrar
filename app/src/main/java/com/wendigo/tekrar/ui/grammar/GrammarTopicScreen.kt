package com.wendigo.tekrar.ui.grammar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ai.GrammarAiIssue
import com.wendigo.tekrar.ai.GrammarAiResult
import com.wendigo.tekrar.core.grammar.GrammarCheckResult
import com.wendigo.tekrar.core.grammar.GrammarExample
import com.wendigo.tekrar.core.grammar.GrammarExamplesStep
import com.wendigo.tekrar.core.grammar.GrammarHookStep
import com.wendigo.tekrar.core.grammar.GrammarIssue
import com.wendigo.tekrar.core.grammar.GrammarIssueKind
import com.wendigo.tekrar.core.grammar.GrammarMistakeStep
import com.wendigo.tekrar.core.grammar.GrammarRuleStep
import com.wendigo.tekrar.core.grammar.GrammarSessionSummary
import com.wendigo.tekrar.core.grammar.GrammarStepType
import com.wendigo.tekrar.core.grammar.GrammarTipStep
import com.wendigo.tekrar.core.grammar.GrammarWriteTask
import com.wendigo.tekrar.ui.components.learn.FeedbackGlyph
import com.wendigo.tekrar.ui.components.learn.LearnCard
import com.wendigo.tekrar.ui.components.learn.LearnPrimaryButton
import com.wendigo.tekrar.ui.components.learn.LearnProgressRing
import com.wendigo.tekrar.ui.components.learn.LearnSecondaryButton
import com.wendigo.tekrar.ui.components.learn.PlayGlyph

@Composable
fun GrammarTopicScreen(
    state: GrammarTopicUiState,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onContinueExplain: () -> Unit,
    onInputChange: (String) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onAiCheck: () -> Unit,
    onRetryLoad: () -> Unit,
    onRetryComplete: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        when {
            state.loading -> TopicLoading(onBack = onBack)
            state.error -> TopicLoadError(
                onBack = onBack,
                onRetry = onRetryLoad,
            )

            else -> TopicContent(
                state = state,
                onBack = onBack,
                onSpeak = onSpeak,
                onContinueExplain = onContinueExplain,
                onInputChange = onInputChange,
                onCheck = onCheck,
                onRetry = onRetry,
                onNext = onNext,
                onFinish = onFinish,
                onAiCheck = onAiCheck,
                onRetryComplete = onRetryComplete,
            )
        }
    }
}

@Composable
private fun TopicContent(
    state: GrammarTopicUiState,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onContinueExplain: () -> Unit,
    onInputChange: (String) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onAiCheck: () -> Unit,
    onRetryComplete: (() -> Unit)?,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    LaunchedEffect(state.phase, state.stepIndex, state.taskIndex, state.result != null) {
        scrollState.scrollTo(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GrammarTopicTopBar(
            title = state.title,
            phase = state.phase,
            stepIndex = state.stepIndex,
            stepCount = state.stepCount,
            taskIndex = state.taskIndex,
            taskCount = state.taskCount,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state.phase) {
                GrammarPhase.EXPLAIN -> ExplainPhase(state = state, onSpeak = onSpeak)
                GrammarPhase.WRITE -> WritePhase(
                    state = state,
                    onInputChange = onInputChange,
                    onSpeak = onSpeak,
                    onAiCheck = onAiCheck,
                )

                GrammarPhase.SUMMARY -> SummaryPhase(
                    state = state,
                    onRetryComplete = onRetryComplete,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        GrammarFooter(
            state = state,
            onContinueExplain = onContinueExplain,
            onCheck = {
                focusManager.clearFocus()
                onCheck()
            },
            onRetry = onRetry,
            onNext = onNext,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun GrammarTopicTopBar(
    title: String,
    phase: GrammarPhase,
    stepIndex: Int,
    stepCount: Int,
    taskIndex: Int,
    taskCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.grammar_back),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (phase) {
                        GrammarPhase.EXPLAIN -> stringResource(
                            R.string.grammar_explain_progress,
                            stepIndex + 1,
                            stepCount,
                        )

                        GrammarPhase.WRITE -> stringResource(
                            R.string.grammar_write_progress,
                            taskIndex + 1,
                            taskCount,
                        )

                        GrammarPhase.SUMMARY -> stringResource(R.string.grammar_summary_progress)
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val dotCount = when (phase) {
                GrammarPhase.EXPLAIN -> stepCount
                GrammarPhase.WRITE -> taskCount
                GrammarPhase.SUMMARY -> 0
            }
            val activeDot = when (phase) {
                GrammarPhase.EXPLAIN -> stepIndex
                GrammarPhase.WRITE -> taskIndex
                GrammarPhase.SUMMARY -> 0
            }
            if (dotCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(dotCount) { index ->
                        val active = index == activeDot
                        val done = index < activeDot
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        active -> MaterialTheme.colorScheme.primary
                                        done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplainPhase(
    state: GrammarTopicUiState,
    onSpeak: (String) -> Unit,
) {
    val step = state.step ?: return
    LearnCard(contentPadding = PaddingValues(22.dp)) {
        Text(
            text = stepTypeLabel(step.type),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (step.type == GrammarStepType.MISTAKE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Text(
            text = step.titleTr,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                lineHeight = 31.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        when (step) {
            is GrammarHookStep -> {
                BodyText(step.bodyTr)
                ExampleList(examples = step.examples, onSpeak = onSpeak)
            }

            is GrammarRuleStep -> {
                BodyText(step.bodyTr)
                PatternStrip(pattern = step.pattern)
                ExampleList(examples = step.examples, onSpeak = onSpeak)
            }

            is GrammarExamplesStep -> {
                step.bodyTr?.takeIf { it.isNotBlank() }?.let { BodyText(it) }
                ExampleList(examples = step.examples, onSpeak = onSpeak)
            }

            is GrammarMistakeStep -> {
                MistakeBlock(step = step)
                BodyText(step.bodyTr)
            }

            is GrammarTipStep -> {
                TipBlock(body = step.bodyTr)
                step.example?.let { example ->
                    ExampleList(examples = listOf(example), onSpeak = onSpeak)
                }
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PatternStrip(pattern: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.grammar_pattern_label),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
        )
        Text(
            text = pattern,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MistakeBlock(step: GrammarMistakeStep) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "✗",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = step.wrongEn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.LineThrough,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = step.rightEn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TipBlock(body: String) {
    Text(
        text = body,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ExampleList(
    examples: List<GrammarExample>,
    onSpeak: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { example ->
            ExampleRow(example = example, onSpeak = onSpeak)
        }
    }
}

@Composable
private fun ExampleRow(
    example: GrammarExample,
    onSpeak: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
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
        ListenInline(text = example.en, onSpeak = onSpeak)
    }
}

@Composable
private fun ListenInline(
    text: String,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(role = Role.Button) { onSpeak(text) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        PlayGlyph(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.grammar_listen),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun WritePhase(
    state: GrammarTopicUiState,
    onInputChange: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onAiCheck: () -> Unit,
) {
    val task = state.task ?: return
    TaskCard(
        task = task,
        taskIndex = state.taskIndex,
        taskCount = state.taskCount,
        writeIntro = state.writeIntro,
        input = state.input,
        showInput = state.result == null,
        onInputChange = onInputChange,
    )
    state.result?.let { result ->
        ResultCard(result = result, onSpeak = onSpeak)
    }
    val result = state.result
    if (result != null && state.aiAvailable) {
        AiSection(
            state = state,
            original = result.original,
            onAiCheck = onAiCheck,
            onSpeak = onSpeak,
        )
    }
}

@Composable
private fun TaskCard(
    task: GrammarWriteTask,
    taskIndex: Int,
    taskCount: Int,
    writeIntro: String,
    input: String,
    showInput: Boolean,
    onInputChange: (String) -> Unit,
) {
    LearnCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.grammar_write_label),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.grammar_write_progress,
                    taskIndex + 1,
                    taskCount,
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = task.promptTr,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 29.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (taskIndex == 0 && writeIntro.isNotBlank()) {
            Text(
                text = writeIntro,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        task.hintTr?.takeIf { it.isNotBlank() }?.let { hint ->
            HintBanner(
                text = hint,
                label = stringResource(R.string.grammar_hint_label),
            )
        }
        if (showInput) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.grammar_task_placeholder),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                ),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
            val words = countWords(input)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.grammar_task_word_count, words),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = if (words >= task.minWords) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = stringResource(R.string.grammar_task_min_words, task.minWords),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: GrammarCheckResult,
    onSpeak: (String) -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val annotated = remember(result.original, result.issues, errorColor) {
        highlightedSentence(
            original = result.original,
            issues = result.issues,
            color = errorColor,
        )
    }
    LearnCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeedbackGlyph(correct = result.correct)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        if (result.correct) {
                            R.string.grammar_result_clean
                        } else {
                            R.string.grammar_result_review
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.grammar_result_score, result.scorePercent),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.grammar_result_yours),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (result.issues.isNotEmpty() &&
            result.corrected.isNotBlank() &&
            result.corrected != result.original.trim()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.grammar_result_correct),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ListenInline(text = result.corrected, onSpeak = onSpeak)
                }
                Text(
                    text = result.corrected,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (result.issues.isNotEmpty()) {
            IssueRows(issues = result.issues)
        }
        result.hintTr?.takeIf { it.isNotBlank() }?.let { hint ->
            HintBanner(
                text = hint,
                label = stringResource(R.string.grammar_hint_label),
            )
        }
    }
}

@Composable
private fun IssueRows(issues: List<GrammarIssue>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        issues.forEach { issue ->
            IssueRow(issue = issue)
        }
    }
}

@Composable
private fun IssueRow(issue: GrammarIssue) {
    val errorColor = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "✗",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = errorColor,
            )
            Text(
                text = issue.wrong.ifBlank {
                    stringResource(R.string.grammar_result_missing)
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    textDecoration = if (issue.wrong.isNotBlank()) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
                ),
                color = errorColor,
            )
            if (issue.kind != GrammarIssueKind.MISSING) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = issue.correct,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = primary,
                )
            }
        }
        Text(
            text = issue.noteTr,
            modifier = Modifier.padding(start = 20.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiSection(
    state: GrammarTopicUiState,
    original: String,
    onAiCheck: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    if (state.aiLoading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.grammar_ai_loading),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val ai = state.aiResult
    when {
        ai != null -> AiResultCard(
            ai = ai,
            original = original,
            onSpeak = onSpeak,
        )

        state.aiFailed -> {
            HintBanner(text = stringResource(R.string.grammar_ai_failed))
            LearnSecondaryButton(
                text = stringResource(R.string.grammar_ai_check_again),
                onClick = onAiCheck,
            )
        }

        else -> LearnSecondaryButton(
            text = stringResource(R.string.grammar_ai_check),
            onClick = onAiCheck,
        )
    }
}

@Composable
private fun AiResultCard(
    ai: GrammarAiResult,
    original: String,
    onSpeak: (String) -> Unit,
) {
    val corrected = ai.corrected
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != original.trim() }
    val verdict = ai.verdictTr?.trim()?.takeIf { it.isNotBlank() }
    LearnCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            text = stringResource(R.string.grammar_ai_title),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        verdict?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        corrected?.let {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.grammar_ai_suggestion),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ListenInline(text = it, onSpeak = onSpeak)
                }
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (ai.issues.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ai.issues.forEach { issue ->
                    AiIssueRow(issue = issue)
                }
            }
        } else if (verdict == null && corrected == null) {
            Text(
                text = stringResource(R.string.grammar_ai_clean),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.grammar_ai_note),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiIssueRow(issue: GrammarAiIssue) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "✗",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = issue.wrong,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.LineThrough,
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = issue.correct,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        issue.noteTr.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                modifier = Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryPhase(
    state: GrammarTopicUiState,
    onRetryComplete: (() -> Unit)?,
) {
    val summary = state.summary ?: return
    SummaryHeroCard(
        summary = summary,
        saveFailed = state.saveFailed,
        onRetryComplete = onRetryComplete,
    )
    LearnCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            text = stringResource(R.string.grammar_summary_issues_title),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        val uniqueIssues = distinctIssues(summary.issues)
        if (uniqueIssues.isEmpty()) {
            Text(
                text = stringResource(R.string.grammar_summary_clean),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            IssueRows(issues = uniqueIssues)
        }
    }
}

@Composable
private fun SummaryHeroCard(
    summary: GrammarSessionSummary,
    saveFailed: Boolean,
    onRetryComplete: (() -> Unit)?,
) {
    LearnCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            text = stringResource(R.string.grammar_summary_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.grammar_summary_tasks,
                summary.taskCount,
                summary.taskCount,
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LearnProgressRing(
                progress = summary.averageScore / 100f,
                centerText = summary.averageScore.toString(),
                caption = stringResource(R.string.grammar_summary_avg),
                diameter = 96.dp,
                stroke = 8.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryStat(
                    label = stringResource(R.string.grammar_summary_correct),
                    value = stringResource(
                        R.string.grammar_summary_correct_value,
                        summary.correctCount,
                        summary.taskCount,
                    ),
                )
                SummaryStat(
                    label = stringResource(R.string.grammar_summary_errors),
                    value = summary.issues.size.toString(),
                )
            }
        }
        Text(
            text = stringResource(R.string.grammar_summary_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (saveFailed) {
            HintBanner(text = stringResource(R.string.grammar_summary_save_failed))
            if (onRetryComplete != null) {
                LearnSecondaryButton(
                    text = stringResource(R.string.grammar_summary_save_retry),
                    onClick = onRetryComplete,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GrammarFooter(
    state: GrammarTopicUiState,
    onContinueExplain: () -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (state.phase) {
            GrammarPhase.EXPLAIN -> {
                val last = state.stepCount <= 0 || state.stepIndex >= state.stepCount - 1
                LearnPrimaryButton(
                    text = stringResource(
                        if (last) R.string.grammar_go_write else R.string.grammar_continue,
                    ),
                    onClick = onContinueExplain,
                )
            }

            GrammarPhase.WRITE -> {
                if (state.result == null) {
                    LearnPrimaryButton(
                        text = stringResource(R.string.grammar_check),
                        onClick = onCheck,
                        enabled = state.input.isNotBlank(),
                    )
                } else {
                    val last = state.taskIndex >= state.taskCount - 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LearnSecondaryButton(
                            text = stringResource(R.string.grammar_try_again),
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            height = 52.dp,
                        )
                        LearnPrimaryButton(
                            text = stringResource(
                                if (last) R.string.grammar_finish else R.string.grammar_next_sentence,
                            ),
                            onClick = onNext,
                            modifier = Modifier.weight(1.35f),
                            height = 52.dp,
                        )
                    }
                }
            }

            GrammarPhase.SUMMARY -> {
                LearnPrimaryButton(
                    text = stringResource(R.string.grammar_summary_finish),
                    onClick = onFinish,
                )
            }
        }
    }
}

@Composable
private fun TopicLoading(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopicBackBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.grammar_topic_loading),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopicLoadError(onBack: () -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopicBackBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            LearnCard {
                Text(
                    text = stringResource(R.string.grammar_topic_error_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.grammar_topic_error_hint),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LearnPrimaryButton(
                    text = stringResource(R.string.grammar_retry),
                    onClick = onRetry,
                )
            }
        }
    }
}

@Composable
private fun TopicBackBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.grammar_back),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HintBanner(text: String, label: String? = null) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (dark) Color(0xFF3A2F08) else Color(0xFFFFF2C2)
    val content = if (dark) Color(0xFFF1D77E) else Color(0xFF6B5200)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = content,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = content,
        )
    }
}

@Composable
private fun stepTypeLabel(type: GrammarStepType): String = stringResource(
    when (type) {
        GrammarStepType.HOOK -> R.string.grammar_step_hook
        GrammarStepType.RULE -> R.string.grammar_step_rule
        GrammarStepType.EXAMPLES -> R.string.grammar_step_examples
        GrammarStepType.MISTAKE -> R.string.grammar_step_mistake
        GrammarStepType.TIP -> R.string.grammar_step_tip
    },
)

private val WORD_SEPARATOR = Regex("\\s+")

private fun countWords(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.split(WORD_SEPARATOR).count { it.isNotBlank() }
}
