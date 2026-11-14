package com.wendigo.tekrar.ui.ai

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.audio.Speaker

private val ChatActionText = 16.sp

/**
 * AI koç sohbet ekranı. Durum ViewModel'dan gelir; composable tamamen stateless'tir.
 * TTS yalnız UI tarafında (sistem TextToSpeech) çalışır.
 */
@Composable
fun AiChatScreen(
    state: AiChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onEndSession: () -> Unit,
    onNewSession: () -> Unit,
    onClearError: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    speechRate: Float = 1f,
    micSlot: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    var spokenMessageId by rememberSaveable { mutableStateOf(0L) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }

    val lastMessage = state.messages.lastOrNull()
    LaunchedEffect(
        state.autoSpeak,
        state.messages.size,
        lastMessage?.id,
        lastMessage?.streaming,
    ) {
        if (!state.autoSpeak) return@LaunchedEffect
        val last = lastMessage ?: return@LaunchedEffect
        if (last.role != AiChatRole.ASSISTANT) return@LaunchedEffect
        if (last.streaming || last.text.isBlank() || last.id <= 0L) return@LaunchedEffect
        if (last.id == spokenMessageId) return@LaunchedEffect
        spokenMessageId = last.id
        speaker.setRate(speechRate)
        speaker.speak(last.text)
    }

    val modeLabel = when (state.mode) {
        AiChatMode.CHAT -> stringResource(R.string.ai_mode_chat)
        AiChatMode.ROLEPLAY -> stringResource(R.string.ai_mode_roleplay)
        AiChatMode.CORRECTION -> stringResource(R.string.ai_mode_correction)
    }
    val showScenario = state.mode == AiChatMode.ROLEPLAY &&
        (state.settingTr != null || state.goalTr != null || state.targetPhrases.isNotEmpty())
    val showKeyCard = !state.hasKey && !state.isStarting
    val showIntro = state.messages.isEmpty() &&
        !state.isStarting &&
        !state.isSummarizing &&
        !state.limitReached &&
        state.hasKey
    val showPreparing = state.isSummarizing && state.summary == null
    val leadingCount = (if (showScenario) 1 else 0) +
        (if (showKeyCard) 1 else 0) +
        (if (state.isStarting) 1 else 0) +
        (if (showIntro) 1 else 0)
    val lastItemIndex = leadingCount +
        state.messages.size +
        (if (showPreparing) 1 else 0) +
        (if (state.summary != null) 1 else 0) -
        1

    val listState = rememberLazyListState()
    LaunchedEffect(lastItemIndex) {
        if (lastItemIndex >= 0) listState.animateScrollToItem(lastItemIndex)
    }
    val streamingLength = lastMessage?.let { if (it.streaming) it.text.length else 0 } ?: 0
    LaunchedEffect(streamingLength) {
        if (streamingLength > 0 && lastItemIndex >= 0 && !listState.canScrollForward) {
            listState.scrollToItem(lastItemIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        ChatHeader(
            state = state,
            modeLabel = modeLabel,
            onClose = onClose,
            onEndSession = onEndSession,
        )
        if (state.isStreaming || state.isSummarizing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showScenario) {
                item(key = "scenario") {
                    AiChatScenarioCard(
                        settingTr = state.settingTr,
                        goalTr = state.goalTr,
                        targetPhrases = state.targetPhrases,
                    )
                }
            }
            if (showKeyCard) {
                item(key = "key") {
                    AiChatKeyRequiredCard()
                }
            }
            if (state.isStarting) {
                item(key = "starting") {
                    AiChatStartingCard()
                }
            }
            if (showIntro) {
                item(key = "intro") {
                    AiChatEmptyCard(
                        mode = state.mode,
                        onStarter = onDraftChange,
                    )
                }
            }
            items(state.messages, key = { it.id }) { message ->
                AiChatMessageBubble(
                    message = message,
                    onSpeak = { text ->
                        speaker.setRate(speechRate)
                        speaker.speak(text)
                    },
                )
            }
            if (showPreparing) {
                item(key = "summary-loading") {
                    AiChatSummaryPreparingCard()
                }
            }
            val summary = state.summary
            if (summary != null) {
                item(key = "summary") {
                    AiSessionSummaryCard(
                        summary = summary,
                        onNewSession = onNewSession,
                        onClose = onClose,
                    )
                }
            }
        }

        val error = state.error
        if (error != null) {
            AiChatErrorBanner(
                error = error,
                onRetry = {
                    if (error.kind == AiChatErrorKind.SUMMARY) onEndSession() else onRetry()
                },
                onOpenSettings = onOpenSettings,
                onDismiss = onClearError,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        when {
            state.summary != null -> Unit
            state.ended -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_chat_close),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = ChatActionText),
                        )
                    }
                    Button(
                        onClick = onNewSession,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_chat_summary_new),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = ChatActionText),
                        )
                    }
                }
            }
            state.isSummarizing -> Unit
            !state.hasKey -> {
                PrimaryChatAction(
                    text = stringResource(R.string.ai_chat_open_settings),
                    onClick = onOpenSettings,
                )
            }
            state.isStarting -> Unit
            state.sessionId == null -> Unit
            state.limitReached -> Unit
            else -> {
                AiChatInputBar(
                    draft = state.draft,
                    canSend = state.canSend,
                    enabled = true,
                    onDraftChange = onDraftChange,
                    onSend = { onSend(state.draft) },
                    // Mikrofon bu turda yok; alan ileride buraya bağlanacak.
                    micSlot = micSlot,
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    state: AiChatUiState,
    modeLabel: String,
    onClose: () -> Unit,
    onEndSession: () -> Unit,
) {
    val persona = stringResource(R.string.ai_chat_persona)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_close),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = ChatActionText),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onEndSession,
                enabled = state.canEnd,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_end),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = ChatActionText),
                    color = if (state.canEnd) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = state.cefrLevel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.ai_chat_subtitle,
                        modeLabel,
                        persona,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun PrimaryChatAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = onClick,
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
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = ChatActionText,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
