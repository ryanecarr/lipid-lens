package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.FeelLevel
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.example.data.remote.PdfBriefUiState
import com.example.reports.ClinicalReportData
import com.example.ui.ReportRange
import com.example.ui.StatinUiState
import com.example.ui.components.StatusPill
import com.example.ui.theme.ClinicalMonoStyle

@Composable
fun ReportsScreen(
    state: StatinUiState,
    reportData: ClinicalReportData,
    includeAiInPdf: Boolean = false,
    pdfBriefState: PdfBriefUiState = PdfBriefUiState.Idle,
    currentScopeKey: String = "",
    onToggleIncludeAi: (Boolean) -> Unit = {},
    onRefreshAiBrief: () -> Unit = {},
    onSelectRange: (ReportRange) -> Unit,
    onSelectStatinFilter: (String?) -> Unit,
    onSharePdf: () -> Unit,
    onSavePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range & Statin Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Healthcare Provider Clinical Summary",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Select the time range to generate a clean, multi-page PDF ready for your doctor visit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportRange.entries.forEach { range ->
                            FilterChip(
                                selected = state.reportRange == range,
                                onClick = { onSelectRange(range) },
                                label = { Text(range.label) },
                                modifier = Modifier.testTag("report_range_${range.name}")
                            )
                        }
                    }

                    if (state.distinctLoggedStatins.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.reportStatinFilter == null,
                                onClick = { onSelectStatinFilter(null) },
                                label = { Text("All Therapies") }
                            )
                            state.distinctLoggedStatins.forEach { statinName ->
                                FilterChip(
                                    selected = state.reportStatinFilter == statinName,
                                    onClick = { onSelectStatinFilter(statinName) },
                                    label = { Text(statinName) }
                                )
                            }
                        }
                    }
                }
            }

            // Opt-in AI Physician Visit Brief
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(9.dp).size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.title_pdf_ai_brief),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.subtitle_pdf_ai_brief),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = includeAiInPdf,
                                onCheckedChange = onToggleIncludeAi,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("pdf_ai_brief_switch")
                            )
                        }

                        AnimatedVisibility(visible = includeAiInPdf) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider()
                                when (pdfBriefState) {
                                    PdfBriefUiState.Idle -> {
                                        TextButton(
                                            onClick = onRefreshAiBrief,
                                            modifier = Modifier.testTag("pdf_ai_generate_button")
                                        ) {
                                            Text("Generate Brief for ${reportData.rangeTitle}")
                                        }
                                    }

                                    PdfBriefUiState.Loading -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Text(
                                                text = "Synthesizing physician brief for ${reportData.rangeTitle}…",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    is PdfBriefUiState.Error -> {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = pdfBriefState.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(
                                                    onClick = onRefreshAiBrief,
                                                    modifier = Modifier.testTag("pdf_ai_retry_button")
                                                ) {
                                                    Text("Retry")
                                                }
                                            }
                                        }
                                    }

                                    is PdfBriefUiState.Success -> {
                                        val brief = pdfBriefState.brief
                                        val scopeMatches = currentScopeKey.isBlank() || brief.scopeKey == currentScopeKey
                                        if (!scopeMatches) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Filters changed — refresh to match ${reportData.rangeTitle}.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    TextButton(
                                                        onClick = onRefreshAiBrief,
                                                        modifier = Modifier.testTag("pdf_ai_refresh_stale_button")
                                                    ) {
                                                        Text(stringResource(R.string.action_refresh_brief))
                                                    }
                                                }
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Lipids: ${brief.lipidTrajectory}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        text = "Therapy: ${brief.therapyAndTolerance}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    if (brief.clinicalHighlights.isNotEmpty()) {
                                                        Text(
                                                            text = "Key Takeaways",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        brief.clinicalHighlights.forEach { bullet ->
                                                            Text(
                                                                text = "• $bullet",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                    if (brief.discussionTopics.isNotEmpty()) {
                                                        Text(
                                                            text = "For Visit Discussion",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        brief.discussionTopics.forEach { topic ->
                                                            Text(
                                                                text = "• $topic",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "AI-assisted • ${brief.generatedAtLabel}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        TextButton(
                                                            onClick = onRefreshAiBrief,
                                                            modifier = Modifier.testTag("pdf_ai_refresh_button")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Refresh,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Refresh")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSharePdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_share_pdf))
                    }
                    OutlinedButton(
                        onClick = onSavePdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_pdf_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save_pdf))
                    }
                }
            }

            // Formatted Clinical Report Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PDF Report Preview", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = reportData.rangeTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (reportData.aiVisitBrief != null) {
                                StatusPill(
                                    label = "AI Brief Included",
                                    isPositive = true
                                )
                            }
                        }

                        HorizontalDivider()

                        // Metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PreviewStat("Adherence", "${reportData.adherencePercent}%")
                            PreviewStat("Taken", "${reportData.takenCount}")
                            PreviewStat("Skipped", "${reportData.skippedCount}")
                            PreviewStat("Total Logs", "${reportData.logs.size}")
                        }

                        HorizontalDivider()

                        // Regimens included
                        Text("Regimens Included", style = MaterialTheme.typography.titleSmall)
                        if (reportData.regimens.isEmpty()) {
                            Text(
                                text = "No lipid-lowering regimens recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            reportData.regimens.forEach { reg ->
                                val endLabel = reg.endedAtMillis?.let { DateTimeUtils.formatDateMillis(it) } ?: "Present"
                                val freq = DoseFrequency.fromKey(reg.frequencyKey).shortLabel
                                val role = if (reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "Adjunct" else "Primary"
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${reg.statinName} ${reg.doseMg} mg • $role • $freq",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${DateTimeUtils.formatDateMillis(reg.startedAtMillis)} – $endLabel" +
                                            if (reg.switchReason.isNotBlank()) " • ${reg.switchReason}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Side effects & Mood summary
                        Text("Side-Effect Frequency", style = MaterialTheme.typography.titleSmall)
                        if (reportData.sideEffectBreakdown.isEmpty()) {
                            Text(
                                text = "No side effects reported in this range.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            reportData.sideEffectBreakdown.forEach { (effect, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(effect.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "$count ${if (count == 1) "entry" else "entries"}",
                                        style = ClinicalMonoStyle,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }

                        if (reportData.moodBreakdown.isNotEmpty()) {
                            Text(
                                text = "Mood ratings: " + reportData.moodBreakdown.joinToString(" • ") {
                                    "${it.first.label} (${it.second})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (reportData.lipidPanels.isNotEmpty()) {
                            HorizontalDivider()
                            val latestPanel = reportData.lipidPanels.first()
                            Text("Lipid Panels Included (${reportData.lipidPanels.size})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Target (${reportData.clinicalTargets.riskLabel}): LDL-C <${reportData.clinicalTargets.ldlTargetMgDl} mg/dL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Latest (${DateTimeUtils.formatDateKeyShort(latestPanel.drawDateKey)}): LDL-C ${latestPanel.ldlMgDl} • Total ${latestPanel.totalCholesterolMgDl} • HDL-C ${latestPanel.hdlMgDl} • TG ${latestPanel.triglyceridesMgDl} mg/dL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (reportData.logs.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Recent Entries in PDF", style = MaterialTheme.typography.titleSmall)
                            reportData.logs.take(5).forEach { log ->
                                val effects = SideEffectOption.parseCsv(log.sideEffectsCsv)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${DateTimeUtils.formatDateKeyShort(log.logDateKey)} • ${log.statinName} ${log.doseMg} mg",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Feel: ${FeelLevel.fromKey(log.feelLevelKey).label}" +
                                                if (effects.isNotEmpty()) " • ${effects.joinToString { it.label }}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(
                                        label = if (log.taken) "Taken" else "Skipped",
                                        isPositive = log.taken,
                                        isWarning = !log.taken
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
