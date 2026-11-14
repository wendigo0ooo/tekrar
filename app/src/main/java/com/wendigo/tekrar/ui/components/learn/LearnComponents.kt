package com.wendigo.tekrar.ui.components.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.learn.LessonStatus

enum class LearnStatusTone {
    NEUTRAL,
    ACTIVE,
    DONE,
    LOCKED,
}

@Composable
fun LearnCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun LearnProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val safeFraction = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (safeFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeFraction)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun LearnProgressRing(
    progress: Float,
    centerText: String,
    caption: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 104.dp,
    stroke: Dp = 9.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (safeProgress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * safeProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LearnStatusBadge(
    text: String,
    tone: LearnStatusTone = LearnStatusTone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val background = when (tone) {
        LearnStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        LearnStatusTone.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        LearnStatusTone.DONE -> MaterialTheme.colorScheme.primary
        LearnStatusTone.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (tone) {
        LearnStatusTone.NEUTRAL, LearnStatusTone.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
        LearnStatusTone.ACTIVE -> MaterialTheme.colorScheme.primary
        LearnStatusTone.DONE -> MaterialTheme.colorScheme.onPrimary
    }
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
        color = contentColor,
        maxLines = 1,
    )
}

@Composable
fun LearnSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun LearnPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
fun LearnSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = if (enabled) 0.8f else 0.4f,
            ),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
fun UnitColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Ünite kartındaki ders/rol yapma düğümü. Connector çizgileri üst/alt komşuya
 * bağlanır; kilitli satır soluk ve tıklanamaz.
 */
@Composable
fun LessonNodeRow(
    title: String,
    subtitle: String,
    status: LessonStatus,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isRoleplay: Boolean = false,
    showStatusBadge: Boolean = true,
) {
    val locked = status == LessonStatus.LOCKED
    val completed = status == LessonStatus.COMPLETED || status == LessonStatus.MASTERED
    val inProgress = status == LessonStatus.IN_PROGRESS
    val accent = MaterialTheme.colorScheme.primary
    val connector = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val circleBackground = when {
        completed -> accent
        inProgress -> MaterialTheme.colorScheme.primaryContainer
        locked -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val circleBorder = when {
        completed -> null
        locked -> MaterialTheme.colorScheme.outline
        else -> accent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .alpha(if (locked) 0.58f else 1f)
            .clickable(
                enabled = !locked && onClick != null,
                role = Role.Button,
                onClick = { onClick?.invoke() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = 18.dp.toPx()
                val gap = 2.dp.toPx()
                val lineWidth = 2.dp.toPx()
                if (!isFirst) {
                    drawLine(
                        color = connector,
                        start = Offset(cx, 0f),
                        end = Offset(cx, cy - radius - gap),
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round,
                    )
                }
                if (!isLast) {
                    drawLine(
                        color = connector,
                        start = Offset(cx, cy + radius + gap),
                        end = Offset(cx, size.height),
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(color = circleBackground, radius = radius, center = Offset(cx, cy))
                if (circleBorder != null) {
                    drawCircle(
                        color = circleBorder,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            NodeGlyph(
                status = status,
                isRoleplay = isRoleplay,
                completed = completed,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                fontWeight = if (locked) FontWeight.Normal else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showStatusBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            LearnStatusBadge(
                text = statusText(status),
                tone = statusTone(status),
            )
        }
    }
}

@Composable
private fun statusText(status: LessonStatus): String = when (status) {
    LessonStatus.LOCKED -> stringResource(R.string.learn_status_locked)
    LessonStatus.AVAILABLE -> stringResource(R.string.learn_status_available)
    LessonStatus.IN_PROGRESS -> stringResource(R.string.learn_status_in_progress)
    LessonStatus.COMPLETED -> stringResource(R.string.learn_status_completed)
    LessonStatus.MASTERED -> stringResource(R.string.learn_status_mastered)
}

private fun statusTone(status: LessonStatus): LearnStatusTone = when (status) {
    LessonStatus.LOCKED -> LearnStatusTone.LOCKED
    LessonStatus.AVAILABLE, LessonStatus.IN_PROGRESS -> LearnStatusTone.ACTIVE
    LessonStatus.COMPLETED, LessonStatus.MASTERED -> LearnStatusTone.DONE
}

@Composable
private fun NodeGlyph(
    status: LessonStatus,
    isRoleplay: Boolean,
    completed: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    when {
        isRoleplay && !completed && status != LessonStatus.LOCKED -> {
            Text(
                text = "R",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = primary,
            )
        }

        completed -> CheckGlyph(color = onPrimary, modifier = Modifier.size(20.dp))
        status == LessonStatus.IN_PROGRESS -> ArcGlyph(
            color = primary,
            modifier = Modifier.size(20.dp),
        )

        status == LessonStatus.LOCKED -> LockGlyph(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )

        else -> PlayGlyph(color = primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CheckGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.52f),
            end = Offset(w * 0.42f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.76f),
            end = Offset(w * 0.84f, h * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun CrossGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.24f, h * 0.24f),
            end = Offset(w * 0.76f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.76f, h * 0.24f),
            end = Offset(w * 0.24f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun PlayGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.30f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.80f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun MicGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.12f),
            size = Size(w * 0.28f, h * 0.46f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.30f),
            size = Size(w * 0.52f, h * 0.42f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.72f),
            end = Offset(w * 0.50f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.90f),
            end = Offset(w * 0.66f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ArcGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.2.dp.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 285f,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2f, size.height - stroke * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun LockGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.9.dp.toPx()
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.28f, h * 0.10f),
            size = Size(w * 0.44f, h * 0.50f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.42f),
            size = Size(w * 0.56f, h * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = w * 0.06f,
            center = Offset(w * 0.50f, h * 0.62f),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.66f),
            end = Offset(w * 0.50f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun FeedbackGlyph(correct: Boolean, modifier: Modifier = Modifier) {
    val accent = if (correct) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (correct) {
            CheckGlyph(color = accent, modifier = Modifier.size(18.dp))
        } else {
            CrossGlyph(color = accent, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * İçerikteki `#RRGGBB` veya `#AARRGGBB` rengini tema güvenli biçimde çözer.
 * Geçersiz/boş değerde tema birincil rengi kullanılır.
 */
@Composable
fun learnUnitColor(hex: String?): Color {
    val fallback = MaterialTheme.colorScheme.primary
    return remember(hex, fallback) {
        val cleaned = hex?.trim()?.removePrefix("#").orEmpty()
        if (cleaned.length != 6 && cleaned.length != 8) {
            return@remember fallback
        }
        val parsed = cleaned.toLongOrNull(16) ?: return@remember fallback
        if (cleaned.length == 6) Color(0xFF000000L or parsed) else Color(parsed)
    }
}
