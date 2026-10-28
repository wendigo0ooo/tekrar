package com.wendigo.tekrar.ui.word

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.DueFormat
import com.wendigo.tekrar.data.Word
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WordDetailScreen(
    word: Word?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggleFavorite: (Word) -> Unit,
    onToggleSuspended: (Word) -> Unit,
    onResetProgress: (Word) -> Unit,
    onDelete: (Word, () -> Unit) -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.word_detail_title),
            onBack = onBack,
            trailing = {
                if (word != null) {
                    TextButton(onClick = { onEdit(word.id) }) {
                        Text(text = stringResource(R.string.word_edit))
                    }
                }
            },
        )

        if (word == null) {
            Text(
                text = stringResource(R.string.word_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = word.term,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onToggleFavorite(word) }) {
                    Text(
                        text = if (word.favorite) "★" else "☆",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (word.pos.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = word.pos,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = statusLabel(word),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(word),
                )
            }
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (word.example.isNotBlank()) {
            Panel {
                Text(
                    text = stringResource(R.string.word_example),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = word.example,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (word.exampleTr.isNotBlank()) {
                    Text(
                        text = word.exampleTr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (word.notes.isNotBlank()) {
            Panel {
                Text(
                    text = stringResource(R.string.word_notes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = word.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (word.tags.isNotBlank()) {
            Panel {
                Text(
                    text = stringResource(R.string.word_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = word.tags,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        SectionTitle(text = stringResource(R.string.word_memory))
        Panel {
            InfoRow(
                label = stringResource(R.string.word_status),
                value = statusLabel(word),
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_next_review),
                value = if (word.isNew) {
                    stringResource(R.string.word_status_new)
                } else {
                    DueFormat.relative(word.dueAt)
                },
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_interval),
                value = if (word.stability > 0.0) {
                    DueFormat.interval(word.stability.toInt().coerceAtLeast(1))
                } else {
                    "—"
                },
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_difficulty),
                value = if (word.difficulty > 0.0) {
                    String.format(Locale.US, "%.1f / 10", word.difficulty)
                } else {
                    "—"
                },
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_reps),
                value = word.reps.toString(),
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_lapses),
                value = word.lapses.toString(),
            )
            DividerLine()
            InfoRow(
                label = stringResource(R.string.word_created),
                value = Instant.ofEpochMilli(word.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"))),
            )
        }

        OutlinedButton(
            onClick = { onToggleSuspended(word) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = if (word.suspended) {
                    stringResource(R.string.word_unsuspend)
                } else {
                    stringResource(R.string.word_suspend)
                }
            )
        }
        OutlinedButton(
            onClick = { showReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = stringResource(R.string.word_reset))
        }
        TextButton(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.word_delete),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showDelete && word != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(text = stringResource(R.string.word_delete_title)) },
            text = { Text(text = stringResource(R.string.word_delete_text, word.term)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDelete = false
                        onDelete(word) { onBack() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.word_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showReset && word != null) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(text = stringResource(R.string.word_reset)) },
            text = { Text(text = stringResource(R.string.word_reset_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        showReset = false
                        onResetProgress(word)
                    },
                ) {
                    Text(text = stringResource(R.string.word_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
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
private fun DividerLine() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

@Composable
private fun statusLabel(word: Word): String = when {
    word.suspended -> stringResource(R.string.word_status_suspended)
    word.isNew -> stringResource(R.string.word_status_new)
    word.stability >= 21.0 -> stringResource(R.string.word_status_mature)
    else -> stringResource(R.string.word_status_young)
}

@Composable
private fun statusColor(word: Word) = when {
    word.suspended -> MaterialTheme.colorScheme.onSurfaceVariant
    word.isNew -> MaterialTheme.colorScheme.secondary
    word.stability >= 21.0 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
