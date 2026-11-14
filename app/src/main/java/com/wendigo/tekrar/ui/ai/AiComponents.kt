package com.wendigo.tekrar.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R

/** Balon ve kart metinleri için ortak gövde ölçüsü (en az 16sp). */
private val AiBody = 16.sp

@Composable
fun AiChatMessageBubble(
    message: AiChatMessageUi,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == AiChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 6.dp,
            bottomStart = 18.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 6.dp,
            bottomEnd = 18.dp,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 304.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = stringResource(
                    if (isUser) R.string.ai_chat_role_user else R.string.ai_chat_role_assistant,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = shape,
                color = bubbleColor,
                contentColor = contentColor,
                border = if (isUser) {
                    null
                } else {
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    )
                },
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor,
                        )
                    }
                    if (message.streaming) {
                        if (message.text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = contentColor,
                            )
                            Text(
                                text = stringResource(R.string.ai_chat_typing),
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
            if (message.failed) {
                Text(
                    text = stringResource(R.string.ai_chat_failed),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            if (!isUser && !message.streaming && message.text.isNotBlank()) {
                TextButton(
                    onClick = { onSpeak(message.text) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ai_chat_speak),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        val correction = message.correction
        if (isUser && correction != null) {
            Spacer(modifier = Modifier.height(6.dp))
            AiCorrectionCard(
                correction = correction,
                modifier = Modifier
                    .widthIn(max = 304.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AiCorrectionCard(
    correction: AiCorrectionUi,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(correction) { mutableStateOf(true) }
    val severityText = when (correction.severity) {
        "low" -> stringResource(R.string.ai_chat_severity_low)
        "medium" -> stringResource(R.string.ai_chat_severity_medium)
        "high" -> stringResource(R.string.ai_chat_severity_high)
        else -> null
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_correction_title),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AiBody,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (severityText != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = severityText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (expanded) {
                                R.string.ai_chat_correction_hide
                            } else {
                                R.string.ai_chat_correction_show
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                Text(
                    text = stringResource(R.string.ai_chat_correction_original),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = correction.original,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ai_chat_correction_corrected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = correction.corrected,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (correction.noteTr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = correction.noteTr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = correction.corrected,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun AiChatScenarioCard(
    settingTr: String?,
    goalTr: String?,
    targetPhrases: List<String>,
    modifier: Modifier = Modifier,
) {
    if (settingTr == null && goalTr == null && targetPhrases.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_chat_scenario_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (settingTr != null) {
                AiLabeledLine(
                    label = stringResource(R.string.ai_chat_scenario_setting),
                    value = settingTr,
                )
            }
            if (goalTr != null) {
                AiLabeledLine(
                    label = stringResource(R.string.ai_chat_scenario_goal),
                    value = goalTr,
                )
            }
            if (targetPhrases.isNotEmpty()) {
                AiLabeledLine(
                    label = stringResource(R.string.ai_chat_scenario_phrases),
                    value = targetPhrases.joinToString(" · "),
                )
            }
        }
    }
}

@Composable
private fun AiLabeledLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun AiChatEmptyCard(
    mode: AiChatMode,
    onStarter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_chat_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (mode == AiChatMode.CORRECTION) {
                    R.string.ai_chat_empty_correction_hint
                } else {
                    R.string.ai_chat_empty_hint
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (mode == AiChatMode.CHAT) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ai_chat_starter_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val starterOne = stringResource(R.string.ai_chat_starter_one)
            val starterTwo = stringResource(R.string.ai_chat_starter_two)
            AiStarterChip(text = starterOne, onClick = { onStarter(starterOne) })
            AiStarterChip(text = starterTwo, onClick = { onStarter(starterTwo) })
        }
    }
}

@Composable
private fun AiStarterChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun AiChatStartingCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ai_chat_starting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AiChatKeyRequiredCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ai_chat_key_required_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.ai_chat_key_required_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AiChatSummaryPreparingCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ai_chat_summary_preparing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AiSessionSummaryCard(
    summary: AiSessionSummaryUi,
    onNewSession: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_summary_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = summary.cefrNote,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
            AiSummarySection(
                title = stringResource(R.string.ai_chat_summary_strengths),
                items = summary.strengths,
            )
            AiSummarySection(
                title = stringResource(R.string.ai_chat_summary_fixes),
                items = summary.fixes,
            )
            if (summary.nextFocus != null) {
                Text(
                    text = stringResource(R.string.ai_chat_summary_focus),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = summary.nextFocus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onNewSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_summary_new),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AiBody,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_summary_close),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiSummarySection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    items.forEach { item ->
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun AiChatErrorBanner(
    error: AiChatErrorUi,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (error.kind == AiChatErrorKind.KEY_MISSING) {
                    TextButton(onClick = onOpenSettings) {
                        Text(
                            text = stringResource(R.string.ai_chat_open_settings),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                        )
                    }
                }
                if (error.retryable) {
                    TextButton(onClick = onRetry) {
                        Text(
                            text = stringResource(R.string.ai_chat_retry),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                        )
                    }
                }
                if (error.kind != AiChatErrorKind.LIMIT &&
                    error.kind != AiChatErrorKind.SUMMARY
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.ai_chat_close),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiBody),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alt giriş çubuğu. Mikrofon bu turda yok; [micSlot] ileride mikrofon düğmesi
 * için ayrılmıştır (null iken hiçbir şey çizilmez).
 */
@Composable
fun AiChatInputBar(
    draft: String,
    canSend: Boolean,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    micSlot: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = {
                    Text(
                        text = stringResource(R.string.ai_chat_input_hint),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(16.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (canSend) onSend() },
                ),
            )
            if (micSlot != null) {
                micSlot()
            }
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_send),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AiBody,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
