package com.wendigo.tekrar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.core.DayCount
import com.wendigo.tekrar.core.DueFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class NavKind { TODAY, LIBRARY, STATS, SETTINGS }

private val navKinds = listOf(
    NavKind.TODAY,
    NavKind.LIBRARY,
    NavKind.STATS,
    NavKind.SETTINGS,
)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(
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
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(
                    text = "Geri",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun GoalRing(
    progress: Float,
    centerText: String,
    caption: String,
    diameter: Dp = 104.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean = true,
    increaseEnabled: Boolean = true,
    valueSuffix: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(
            onClick = onDecrease,
            modifier = Modifier
                .width(58.dp)
                .height(48.dp),
            enabled = decreaseEnabled,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = if (valueSuffix.isEmpty()) value.toString() else "$value $valueSuffix",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = onIncrease,
            modifier = Modifier
                .width(58.dp)
                .height(48.dp),
            enabled = increaseEnabled,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun LabeledSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun BottomBar(
    selectedIndex: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onSelect(index) },
                icon = {
                    NavIcon(
                        kind = navKinds[index],
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                label = { Text(text = label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
fun NavIcon(kind: NavKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.9.dp.toPx()
        when (kind) {
            NavKind.TODAY -> {
                drawCircle(color = tint, radius = w * 0.17f, center = center)
                for (index in 0 until 8) {
                    val angle = index * PI / 4.0
                    val inner = w * 0.29f
                    val outer = w * 0.43f
                    drawLine(
                        color = tint,
                        start = Offset(
                            center.x + cos(angle).toFloat() * inner,
                            center.y + sin(angle).toFloat() * inner,
                        ),
                        end = Offset(
                            center.x + cos(angle).toFloat() * outer,
                            center.y + sin(angle).toFloat() * outer,
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            NavKind.LIBRARY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.19f, h * 0.12f),
                    size = Size(w * 0.62f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.12f),
                    end = Offset(w * 0.35f, h * 0.88f),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.46f, h * 0.35f),
                    end = Offset(w * 0.70f, h * 0.35f),
                    strokeWidth = stroke * 0.8f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.46f, h * 0.52f),
                    end = Offset(w * 0.70f, h * 0.52f),
                    strokeWidth = stroke * 0.8f,
                    cap = StrokeCap.Round,
                )
            }
            NavKind.STATS -> {
                val barWidth = w * 0.15f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.54f),
                    size = Size(barWidth, h * 0.30f),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.32f),
                    size = Size(barWidth, h * 0.52f),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.66f, h * 0.17f),
                    size = Size(barWidth, h * 0.67f),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
            NavKind.SETTINGS -> {
                for (index in 0..2) {
                    val y = h * (0.25f + 0.25f * index)
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.17f, y),
                        end = Offset(w * 0.83f, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    val x = w * (0.34f + 0.16f * index)
                    drawCircle(
                        color = tint,
                        radius = w * 0.075f,
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}

@Composable
fun Heatmap(
    days: List<DayCount>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val today = days.last().date
    val counts = days.associate { it.date to it.count }
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val start = today.minusDays(90)
    val gridStart = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val totalDays = ChronoUnit.DAYS.between(gridStart, today).toInt() + 1
    val weeks = (totalDays + 6) / 7
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val cell = 13.dp
    val gap = 3.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier.height(cell),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (index == 0 || index == 2 || index == 4 || index == 6) {
                            Text(
                                text = DueFormat.WEEKDAYS[index],
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { dayOfWeek ->
                            val date = gridStart.plusDays((week * 7 + dayOfWeek).toLong())
                            if (date.isAfter(today)) {
                                Spacer(modifier = Modifier.size(cell))
                            } else {
                                val count = counts[date] ?: 0
                                val color = if (count <= 0) {
                                    empty.copy(alpha = 0.55f)
                                } else {
                                    primary.copy(alpha = 0.25f + 0.75f * (count.toFloat() / max))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(cell)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = DueFormat.date(
                    start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Bugün",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MiniBars(
    days: List<DayCount>,
    modifier: Modifier = Modifier,
    labelEvery: Int = 1,
) {
    if (days.isEmpty()) return
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height((92f * day.count / max).coerceAtLeast(3f).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (day.count > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (labelEvery <= 1 || index % labelEvery == 0 || index == days.lastIndex) {
                    Text(
                        text = if (days.size <= 10) {
                            DueFormat.shortWeekday(day.date)
                        } else {
                            day.date.dayOfMonth.toString()
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastRows(
    items: List<DayCount>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val max = (items.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items.forEachIndexed { index, item ->
            val label = when (index) {
                0 -> "Bugün"
                1 -> "Yarın"
                else -> DueFormat.shortWeekday(item.date)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.width(46.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (item.count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.count.toFloat() / max)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Text(
                    text = item.count.toString(),
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun ActionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun PrimaryButton(
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
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
