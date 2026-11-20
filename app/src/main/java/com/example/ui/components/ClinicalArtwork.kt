package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.FeelLevel
import com.example.ui.DayStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.OnAmberContainer

@Composable
fun ClinicalHeroEmblem(
    modifier: Modifier = Modifier,
    contentDescriptionText: String
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .size(68.dp)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val w = size.width
        val h = size.height

        // Soft backdrop circle
        drawCircle(
            color = primaryContainer,
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val lensCenter = Offset(w * 0.45f, h * 0.45f)
        val lensRadius = w * 0.25f

        // Optical glass tint inside lens
        drawCircle(
            color = surface.copy(alpha = 0.72f),
            radius = lensRadius,
            center = lensCenter
        )
        drawCircle(
            color = primary.copy(alpha = 0.10f),
            radius = lensRadius,
            center = lensCenter
        )

        // Lens handle extending cleanly down-right
        drawLine(
            color = primary.copy(alpha = 0.78f),
            start = Offset(w * 0.635f, h * 0.635f),
            end = Offset(w * 0.79f, h * 0.79f),
            strokeWidth = w * 0.088f,
            cap = StrokeCap.Round
        )

        // Lipid trend curve inside the lens
        val trendPath = Path().apply {
            moveTo(w * 0.305f, h * 0.405f)
            cubicTo(
                w * 0.37f, h * 0.405f,
                w * 0.40f, h * 0.525f,
                w * 0.47f, h * 0.525f
            )
            cubicTo(
                w * 0.51f, h * 0.525f,
                w * 0.535f, h * 0.475f,
                w * 0.575f, h * 0.475f
            )
        }
        drawPath(
            path = trendPath,
            color = primary,
            style = Stroke(width = w * 0.052f, cap = StrokeCap.Round)
        )

        // Start point
        drawCircle(
            color = primary,
            radius = w * 0.038f,
            center = Offset(w * 0.305f, h * 0.405f)
        )

        // Lens outer ring
        drawCircle(
            color = primary,
            radius = lensRadius,
            center = lensCenter,
            style = Stroke(width = w * 0.072f)
        )

        // Target endpoint precision dot (surface halo + tertiary accent)
        drawCircle(
            color = surface,
            radius = w * 0.068f,
            center = Offset(w * 0.575f, h * 0.475f)
        )
        drawCircle(
            color = tertiary,
            radius = w * 0.048f,
            center = Offset(w * 0.575f, h * 0.475f)
        )
    }
}

@Composable
fun EmptyLogsIllustration(
    modifier: Modifier = Modifier,
    contentDescriptionText: String
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .size(140.dp, 108.dp)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val w = size.width
        val h = size.height

        // Soft ambient sage oval
        drawOval(
            color = primaryContainer.copy(alpha = 0.7f),
            topLeft = Offset(w * 0.08f, h * 0.14f),
            size = Size(w * 0.84f, h * 0.76f)
        )

        // Journal card
        drawRoundRect(
            color = surface,
            topLeft = Offset(w * 0.24f, h * 0.12f),
            size = Size(w * 0.52f, h * 0.76f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(w * 0.24f, h * 0.12f),
            size = Size(w * 0.52f, h * 0.76f),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 2.5f)
        )

        // Checklist lines
        listOf(0.32f, 0.48f, 0.64f).forEachIndexed { idx, yFrac ->
            drawCircle(
                color = if (idx == 0) primary else outline,
                radius = 5f,
                center = Offset(w * 0.34f, h * yFrac)
            )
            drawLine(
                color = if (idx == 0) primary.copy(alpha = 0.75f) else outline,
                start = Offset(w * 0.42f, h * yFrac),
                end = Offset(w * if (idx == 1) 0.66f else 0.62f, h * yFrac),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // Small terracotta pill accent
        drawCircle(
            color = tertiary,
            radius = 9f,
            center = Offset(w * 0.74f, h * 0.20f)
        )
    }
}

@Composable
fun SevenDayAdherenceStrip(
    days: List<DayStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            val log = day.log
            val isOffDay = log == null && !day.scheduledDue
            val bgColor = when {
                isOffDay -> MaterialTheme.colorScheme.surface
                log == null -> MaterialTheme.colorScheme.surfaceVariant
                log.taken -> MaterialTheme.colorScheme.primary
                else -> AmberContainer
            }
            val contentColor = when {
                isOffDay -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                log == null -> MaterialTheme.colorScheme.onSurfaceVariant
                log.taken -> MaterialTheme.colorScheme.onPrimary
                else -> OnAmberContainer
            }
            val borderColor = when {
                isOffDay -> MaterialTheme.colorScheme.outlineVariant
                log == null -> MaterialTheme.colorScheme.outline
                log.taken -> MaterialTheme.colorScheme.primary
                else -> AmberWarning
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isOffDay -> Text(
                            text = "–",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                        log == null -> Text(
                            text = "·",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                        log.taken -> Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Taken",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Skipped",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = day.dayLetter,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun FeelLevel.icon(): ImageVector = when (this) {
    FeelLevel.GREAT -> Icons.Outlined.SentimentVerySatisfied
    FeelLevel.GOOD -> Icons.Outlined.SentimentSatisfied
    FeelLevel.OKAY -> Icons.Outlined.SentimentNeutral
    FeelLevel.SLUGGISH -> Icons.Outlined.SentimentDissatisfied
    FeelLevel.ROUGH -> Icons.Outlined.SentimentVeryDissatisfied
}

@Composable
fun StatusPill(
    label: String,
    isPositive: Boolean,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isWarning -> AmberContainer
        isPositive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val fg = when {
        isWarning -> OnAmberContainer
        isPositive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
