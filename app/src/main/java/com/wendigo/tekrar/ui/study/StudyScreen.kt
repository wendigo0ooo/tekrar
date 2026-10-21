package com.wendigo.tekrar.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.contracts.StudyCardUi
import com.wendigo.tekrar.ui.contracts.StudyUiState
import com.wendigo.tekrar.ui.theme.LocalRatingTones

@Composable
fun StudyScreen(
    state: StudyUiState,
    onReveal: () -> Unit,
    onRate: (Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.study_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.study_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onExit,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(text = stringResource(R.string.study_back))
                        }
                    }
                }
            }

            state.finished -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.study_finished_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.study_finished_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRestart,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(text = stringResource(R.string.study_restart))
                        }
                        TextButton(onClick = onExit) {
                            Text(text = stringResource(R.string.study_back))
                        }
                    }
                }
            }

            else -> {
                val card = state.card
                if (card != null) {
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
                        ) {
                            TextButton(onClick = onExit) {
                                Text(text = stringResource(R.string.study_back))
                            }
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.study_progress,
                                        state.position,
                                        state.total,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        FlipCard(
                            revealed = state.revealed,
                            card = card,
                            onTap = onReveal,
                        )

                        if (!state.revealed) {
                            Button(
                                onClick = onReveal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(text = stringResource(R.string.study_reveal))
                            }
                        } else {
                            RatingGrid(
                                intervalLabels = state.intervalLabels,
                                onRate = onRate,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    revealed: Boolean,
    card: StudyCardUi,
    onTap: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "cardFlip",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 380.dp)
            .height(320.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !revealed, onClick = onTap),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            if (rotation <= 90f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = card.term,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.study_tap_to_reveal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = card.term,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = card.meaning,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (card.example.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.study_example_line, card.example),
                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (card.exampleTr.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.study_example_tr_line, card.exampleTr),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (card.notes.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.study_notes_line, card.notes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingGrid(
    intervalLabels: List<String>,
    onRate: (Int) -> Unit,
) {
    val tones = LocalRatingTones.current
    val labels = listOf(
        stringResource(R.string.study_rating_again),
        stringResource(R.string.study_rating_hard),
        stringResource(R.string.study_rating_good),
        stringResource(R.string.study_rating_easy),
    )
    val colors = listOf(tones.again, tones.hard, tones.good, tones.easy)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RatingButton(
                rating = 1,
                label = labels[0],
                tone = colors[0],
                interval = intervalLabels.getOrNull(0),
                onRate = onRate,
                modifier = Modifier.weight(1f),
            )
            RatingButton(
                rating = 2,
                label = labels[1],
                tone = colors[1],
                interval = intervalLabels.getOrNull(1),
                onRate = onRate,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RatingButton(
                rating = 3,
                label = labels[2],
                tone = colors[2],
                interval = intervalLabels.getOrNull(2),
                onRate = onRate,
                modifier = Modifier.weight(1f),
            )
            RatingButton(
                rating = 4,
                label = labels[3],
                tone = colors[3],
                interval = intervalLabels.getOrNull(3),
                onRate = onRate,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RatingButton(
    rating: Int,
    label: String,
    tone: Color,
    interval: String?,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onRate(rating) },
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tone,
            contentColor = contentOn(tone),
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            if (!interval.isNullOrBlank()) {
                Text(text = interval, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun contentOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF10140F) else Color.White
