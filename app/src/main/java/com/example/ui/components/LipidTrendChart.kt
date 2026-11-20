package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClinicalTargets
import com.example.data.model.DateTimeUtils
import com.example.data.model.LipidAnalytics
import com.example.data.model.LipidMetric
import com.example.data.model.LipidPanel
import com.example.data.model.StatinRegimen
import com.example.ui.theme.ClinicalMonoStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class LipidChartPoint(
    val panel: LipidPanel,
    val value: Int,
    val axisTimeMillis: Long
)

@Composable
fun LipidTrendChart(
    panelsAscending: List<LipidPanel>,
    regimens: List<StatinRegimen>,
    metric: LipidMetric,
    clinicalTargets: ClinicalTargets = ClinicalTargets(),
    baselinePanel: LipidPanel? = null,
    modifier: Modifier = Modifier
) {
    val points = remember(panelsAscending, metric) {
        panelsAscending.mapNotNull { panel ->
            LipidAnalytics.valueForMetric(panel, metric)?.let { value ->
                LipidChartPoint(
                    panel = panel,
                    value = value,
                    axisTimeMillis = DateTimeUtils.utcMillisFromDateKey(panel.drawDateKey)
                )
            }
        }
    }

    if (points.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (metric == LipidMetric.APOB) "No ApoB entries in this timeframe"
                    else "No ${metric.shortLabel} draws in this timeframe",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (metric == LipidMetric.APOB) {
                        "Add ApoB when logging a lab panel, or expand the timeframe filter above."
                    } else {
                        "Log a lipid panel or select a wider date range to visualize your trend."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]

    val baselineMetricVal = baselinePanel?.let { LipidAnalytics.valueForMetric(it, metric) }
    val firstVal = points.first().value
    val latestVal = points.last().value
    val deltaPct = when {
        baselinePanel != null &&
            baselineMetricVal != null &&
            selectedPoint.panel.id != baselinePanel.id ->
            LipidAnalytics.percentDelta(baselineMetricVal, selectedPoint.value)
        points.size >= 2 -> LipidAnalytics.percentDelta(firstVal, latestVal)
        else -> null
    }
    val isBaselinePointSelected = baselinePanel != null && selectedPoint.panel.id == baselinePanel.id

    val (primaryTarget, secondaryTarget) = remember(clinicalTargets, metric) {
        LipidAnalytics.targetsForMetric(clinicalTargets, metric)
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val minRaw = min(
        points.minOf { it.value },
        min(
            primaryTarget ?: Int.MAX_VALUE,
            secondaryTarget ?: Int.MAX_VALUE
        )
    )
    val maxRaw = max(
        points.maxOf { it.value },
        max(
            primaryTarget ?: Int.MIN_VALUE,
            secondaryTarget ?: Int.MIN_VALUE
        )
    )
    val spanRaw = (maxRaw - minRaw).coerceAtLeast(20)
    val pad = max(10, (spanRaw * 0.16f).toInt())
    val yMin = max(0, floor((minRaw - pad) / 10.0).toInt() * 10)
    val yMax = ceil((maxRaw + pad) / 10.0).toInt() * 10
    val ySpan = (yMax - yMin).coerceAtLeast(20)
    val yMid = yMin + ySpan / 2

    val minTime = points.first().axisTimeMillis
    val maxTime = points.last().axisTimeMillis
    val switchMarkerTimes = remember(regimens, minTime, maxTime) {
        if (points.size < 2 || maxTime <= minTime) emptyList()
        else regimens
            .map { DateTimeUtils.utcMillisFromDateKey(DateTimeUtils.dateKeyForMillis(it.startedAtMillis)) }
            .filter { it in (minTime + 1)..maxTime }
            .distinct()
            .sorted()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Selected point & overall delta readout
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${DateTimeUtils.formatDateKeyShort(selectedPoint.panel.drawDateKey)} • ${
                            if (selectedPoint.panel.fasting) "Fasting" else "Non-fasting"
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${selectedPoint.value}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                        Text(
                            text = "${metric.unit} ${metric.shortLabel}",
                            style = ClinicalMonoStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                if (isBaselinePointSelected) {
                    StatusPill(
                        label = "Baseline anchor",
                        isPositive = true
                    )
                } else if (deltaPct != null) {
                    val isFavorable = if (metric.lowerIsBetter) deltaPct <= 0 else deltaPct >= 0
                    val sign = if (deltaPct > 0) "+" else ""
                    val deltaSuffix = if (baselinePanel != null && baselineMetricVal != null) {
                        "vs baseline"
                    } else {
                        "range change"
                    }
                    StatusPill(
                        label = "$sign$deltaPct% $deltaSuffix",
                        isPositive = isFavorable,
                        isWarning = !isFavorable && abs(deltaPct) > 0
                    )
                } else {
                    StatusPill(
                        label = "Baseline draw",
                        isPositive = true
                    )
                }
            }
        }

        // Chart area with Y-axis labels on left
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(190.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$yMax",
                    style = ClinicalMonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$yMid",
                    style = ClinicalMonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$yMin",
                    style = ClinicalMonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            val chartCd = buildString {
                append("${metric.label} trend chart showing ${points.size} draws. ")
                append("Latest ${latestVal} ${metric.unit}.")
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(190.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .semantics { contentDescription = chartCd }
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val padX = 16.dp.toPx()
                                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                                val timeSpan = (maxTime - minTime).coerceAtLeast(1L)
                                var nearestIdx = 0
                                var bestDist = Float.MAX_VALUE
                                points.forEachIndexed { idx, pt ->
                                    val xFrac = if (points.size == 1) 0.5f
                                    else if (maxTime == minTime) idx.toFloat() / (points.size - 1).coerceAtLeast(1)
                                    else (pt.axisTimeMillis - minTime).toFloat() / timeSpan.toFloat()
                                    val px = padX + xFrac * usableW
                                    val dist = abs(tapOffset.x - px)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        nearestIdx = idx
                                    }
                                }
                                selectedIndex = nearestIdx
                            }
                        }
                ) {
                    val padX = 16.dp.toPx()
                    val padY = 14.dp.toPx()
                    val plotW = (size.width - padX * 2).coerceAtLeast(1f)
                    val plotH = (size.height - padY * 2).coerceAtLeast(1f)

                    fun yForValue(v: Int): Float {
                        val frac = (v - yMin).toFloat() / ySpan.toFloat()
                        return padY + plotH * (1f - frac.coerceIn(0f, 1f))
                    }

                    // Horizontal background gridlines
                    listOf(yMin, yMid, yMax).forEach { tick ->
                        val gy = yForValue(tick)
                        drawLine(
                            color = outlineVariant.copy(alpha = 0.7f),
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Clinical target reference dashed lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    primaryTarget?.let { target ->
                        if (target in yMin..yMax) {
                            val ty = yForValue(target)
                            drawLine(
                                color = secondary.copy(alpha = 0.85f),
                                start = Offset(0f, ty),
                                end = Offset(size.width, ty),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }
                    }
                    secondaryTarget?.let { target2 ->
                        if (target2 in yMin..yMax) {
                            val ty2 = yForValue(target2)
                            drawLine(
                                color = primary.copy(alpha = 0.55f),
                                start = Offset(0f, ty2),
                                end = Offset(size.width, ty2),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }
                    }

                    // Medication switch vertical markers
                    val timeSpan = (maxTime - minTime).coerceAtLeast(1L)
                    switchMarkerTimes.forEach { markerTime ->
                        val frac = (markerTime - minTime).toFloat() / timeSpan.toFloat()
                        val sx = padX + frac * plotW
                        drawLine(
                            color = tertiary.copy(alpha = 0.6f),
                            start = Offset(sx, padY),
                            end = Offset(sx, padY + plotH),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Compute node offsets
                    val offsets = points.mapIndexed { idx, pt ->
                        val xFrac = if (points.size == 1) 0.5f
                        else if (maxTime == minTime) idx.toFloat() / (points.size - 1).coerceAtLeast(1)
                        else (pt.axisTimeMillis - minTime).toFloat() / timeSpan.toFloat()
                        Offset(
                            x = padX + xFrac * plotW,
                            y = yForValue(pt.value)
                        )
                    }

                    if (offsets.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(offsets.first().x, offsets.first().y)
                            for (i in 1 until offsets.size) {
                                val prev = offsets[i - 1]
                                val curr = offsets[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(offsets.last().x, padY + plotH)
                            lineTo(offsets.first().x, padY + plotH)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                startY = padY,
                                endY = padY + plotH
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primary,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Selected point guide line
                    val sel = offsets[safeIndex]
                    drawLine(
                        color = outline.copy(alpha = 0.5f),
                        start = Offset(sel.x, padY),
                        end = Offset(sel.x, padY + plotH),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Data point nodes
                    offsets.forEachIndexed { idx, pt ->
                        val isSelected = idx == safeIndex
                        if (isSelected) {
                            drawCircle(
                                color = primary.copy(alpha = 0.22f),
                                radius = 11.dp.toPx(),
                                center = pt
                            )
                        }
                        drawCircle(
                            color = surface,
                            radius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = primary,
                            radius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                            center = pt,
                            style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.2.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-axis date range labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DateTimeUtils.formatDateKeyShort(points.first().panel.drawDateKey),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size >= 2) {
                Text(
                    text = "Tap any dot to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Text(
                    text = DateTimeUtils.formatDateKeyShort(points.last().panel.drawDateKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Clinical guideline legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryTarget?.let { target ->
                val isPersonalized = metric == LipidMetric.LDL_C ||
                    metric == LipidMetric.NON_HDL_C ||
                    metric == LipidMetric.APOB
                val prefix = if (isPersonalized) "Your target" else "Target"
                LegendChip(
                    color = secondary,
                    text = if (metric.lowerIsBetter) "$prefix <$target ${metric.unit}"
                    else "$prefix ≥$target ${metric.unit}"
                )
            }
            if (secondaryTarget != null) {
                metric.secondaryTargetLabel?.let { label ->
                    LegendChip(color = primary.copy(alpha = 0.7f), text = label)
                }
            }
            if (switchMarkerTimes.isNotEmpty()) {
                LegendChip(color = tertiary, text = "Therapy change")
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
