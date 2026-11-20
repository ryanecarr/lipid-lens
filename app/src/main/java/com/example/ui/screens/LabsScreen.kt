package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.LabRecheckStatus
import com.example.data.model.LabTimeRange
import com.example.data.model.LipidAnalytics
import com.example.data.model.LipidMetric
import com.example.data.model.LipidPanel
import com.example.data.model.RecheckPhase
import com.example.data.remote.AiInsightUiState
import com.example.ui.StatinUiState
import com.example.ui.components.LipidTrendChart
import com.example.ui.components.PersonalCardiologyInsightCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabsScreen(
    state: StatinUiState,
    aiInsightState: AiInsightUiState,
    onSelectMetric: (LipidMetric) -> Unit,
    onSelectRange: (LabTimeRange) -> Unit,
    onAddPanel: () -> Unit,
    onEditPanel: (LipidPanel) -> Unit,
    onDeletePanel: (String) -> Unit,
    onGenerateInsight: () -> Unit,
    onOpenTargets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<LipidPanel?>(null) }

    val filteredPanelsAscending = remember(state.allLipidPanels, state.selectedLabRange) {
        LipidAnalytics.filterPanelsByRange(state.allLipidPanels, state.selectedLabRange)
    }
    val baselinePanel = remember(state.allLipidPanels) {
        LipidAnalytics.resolveBaselinePanel(state.allLipidPanels)
    }
    val latestPanel = state.allLipidPanels.firstOrNull()
    val targets = state.clinicalTargets

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + Add Lab CTA + Personalized Target Pill
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.title_lipid_labs),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (state.allLipidPanels.isEmpty()) {
                                    "Track lipid panels alongside your medications"
                                } else {
                                    "${state.allLipidPanels.size} ${
                                        if (state.allLipidPanels.size == 1) "panel" else "panels"
                                    } synced"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onAddPanel,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("labs_add_panel_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_log_lab_panel))
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenTargets,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("labs_targets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrackChanges,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val extraTargetNote = buildList {
                            targets.nonHdlTargetMgDl?.let { add("Non-HDL <$it") }
                            targets.apoBTargetMgDl?.let { add("ApoB <$it") }
                        }.joinToString(" • ")
                        Text(
                            text = "Goal: LDL <${targets.ldlTargetMgDl} mg/dL" +
                                (if (extraTargetNote.isNotBlank()) " • $extraTargetNote" else "") +
                                " • Edit"
                        )
                    }
                }
            }

            // 4-12 Week Post-Switch Lab Recheck Window Card
            state.labRecheckStatus?.let { recheck ->
                item {
                    LabRecheckBannerCard(
                        recheck = recheck,
                        onLogFollowUp = onAddPanel
                    )
                }
            }

            // Latest Lipid Snapshot Strip
            if (latestPanel != null) {
                item {
                    val nonHdlTarget = targets.nonHdlTargetMgDl ?: (targets.ldlTargetMgDl + 30)
                    val apoBTarget = targets.apoBTargetMgDl ?: 80
                    val baselinePct = if (baselinePanel != null && baselinePanel.id != latestPanel.id) {
                        LipidAnalytics.percentDelta(baselinePanel.ldlMgDl, latestPanel.ldlMgDl)
                    } else null

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LATEST PANEL • ${DateTimeUtils.formatDateKeyShort(latestPanel.drawDateKey).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusPill(
                                    label = if (latestPanel.fasting) "Fasting" else "Non-fasting",
                                    isPositive = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BiomarkerTile("LDL-C", "${latestPanel.ldlMgDl}", Modifier.weight(1f), highlight = true)
                                BiomarkerTile("Total", "${latestPanel.totalCholesterolMgDl}", Modifier.weight(1f))
                                BiomarkerTile("HDL-C", "${latestPanel.hdlMgDl}", Modifier.weight(1f))
                                BiomarkerTile("TG", "${latestPanel.triglyceridesMgDl}", Modifier.weight(1f))
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(
                                    label = if (latestPanel.ldlMgDl < targets.ldlTargetMgDl) {
                                        "LDL at goal (<${targets.ldlTargetMgDl})"
                                    } else {
                                        "LDL goal <${targets.ldlTargetMgDl}"
                                    },
                                    isPositive = latestPanel.ldlMgDl < targets.ldlTargetMgDl,
                                    isWarning = latestPanel.ldlMgDl >= targets.ldlTargetMgDl
                                )
                                if (baselinePct != null) {
                                    val sign = if (baselinePct > 0) "+" else ""
                                    StatusPill(
                                        label = "$sign$baselinePct% LDL vs baseline",
                                        isPositive = baselinePct <= 0,
                                        isWarning = baselinePct > 0
                                    )
                                }
                                val nonHdl = LipidAnalytics.effectiveNonHdl(latestPanel)
                                StatusPill(
                                    label = "Non-HDL $nonHdl mg/dL",
                                    isPositive = nonHdl < nonHdlTarget,
                                    isWarning = nonHdl >= nonHdlTarget
                                )
                                latestPanel.apoBMgDl?.let { apob ->
                                    StatusPill(
                                        label = "ApoB $apob mg/dL",
                                        isPositive = apob < apoBTarget,
                                        isWarning = apob >= apoBTarget
                                    )
                                }
                                latestPanel.lpAMgDl?.let { lpa ->
                                    StatusPill(label = "Lp(a) $lpa mg/dL", isPositive = lpa < 50, isWarning = lpa >= 50)
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Trend Graph Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Longitudinal Trends",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${filteredPanelsAscending.size} in range",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Timeframe Range Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LabTimeRange.entries.forEach { range ->
                                FilterChip(
                                    selected = state.selectedLabRange == range,
                                    onClick = { onSelectRange(range) },
                                    label = { Text(range.label) },
                                    modifier = Modifier.testTag("lab_range_${range.name}")
                                )
                            }
                        }

                        // Biomarker Metric Selector Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LipidMetric.entries.forEach { metric ->
                                FilterChip(
                                    selected = state.selectedLabMetric == metric,
                                    onClick = { onSelectMetric(metric) },
                                    label = { Text(metric.shortLabel) },
                                    modifier = Modifier.testTag("lab_metric_${metric.name}")
                                )
                            }
                        }

                        LipidTrendChart(
                            panelsAscending = filteredPanelsAscending,
                            regimens = state.allRegimens,
                            metric = state.selectedLabMetric,
                            clinicalTargets = targets,
                            baselinePanel = baselinePanel
                        )

                        Text(
                            text = state.selectedLabMetric.guidelineTargetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Carefully Placed Personal Cardiology AI Insight Card
            item {
                PersonalCardiologyInsightCard(
                    insightState = aiInsightState,
                    hasLabs = state.allLipidPanels.isNotEmpty(),
                    onGenerateOrRefresh = onGenerateInsight
                )
            }

            // Blood Work History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lab History",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.allLipidPanels.isNotEmpty()) {
                        Text(
                            text = "Newest first",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.allLipidPanels.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Biotech,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No lipid panels recorded yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Log baseline or follow-up cholesterol blood work to chart LDL-C, Total Cholesterol, HDL-C, Triglycerides, and Non-HDL trends over time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.allLipidPanels, key = { it.id }) { panel ->
                    LipidPanelHistoryCard(
                        panel = panel,
                        onEdit = { onEditPanel(panel) },
                        onDelete = { pendingDelete = panel }
                    )
                }
            }
        }
    }

    pendingDelete?.let { panel ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Lipid Panel?") },
            text = {
                Text(
                    "Remove the lipid panel from ${DateTimeUtils.formatDateKeyShort(panel.drawDateKey)}? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePanel(panel.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_lab_button")
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LabRecheckBannerCard(
    recheck: LabRecheckStatus,
    onLogFollowUp: () -> Unit
) {
    val containerColor = when (recheck.phase) {
        RecheckPhase.PREPARING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        RecheckPhase.IN_WINDOW -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        RecheckPhase.OVERDUE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    }
    val titleText = when (recheck.phase) {
        RecheckPhase.PREPARING -> "4–12 Week Recheck Cadence • Week ${recheck.weeksElapsed}"
        RecheckPhase.IN_WINDOW -> "Follow-Up Lab Window Open (Week ${recheck.weeksElapsed})"
        RecheckPhase.OVERDUE -> "Follow-Up Lipid Recheck Overdue (${recheck.weeksElapsed}w)"
    }
    val bodyText = when (recheck.phase) {
        RecheckPhase.PREPARING ->
            "After starting ${recheck.regimen.statinName} ${recheck.regimen.doseMg} mg, ACC/AHA guidelines suggest rechecking lipids 4–12 weeks later (opens ${DateTimeUtils.formatDateKeyShort(recheck.windowOpenDateKey)})."
        RecheckPhase.IN_WINDOW ->
            "You are in the 4–12 week response window since starting ${recheck.regimen.statinName} ${recheck.regimen.doseMg} mg (through ${DateTimeUtils.formatDateKeyShort(recheck.windowCloseDateKey)})."
        RecheckPhase.OVERDUE ->
            "It has been over 12 weeks since starting ${recheck.regimen.statinName} ${recheck.regimen.doseMg} mg with no follow-up lipid panel logged."
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_recheck_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = if (recheck.phase == RecheckPhase.OVERDUE) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(top = 2.dp).size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recheck.phase != RecheckPhase.PREPARING) {
                    TextButton(
                        onClick = onLogFollowUp,
                        modifier = Modifier.testTag("recheck_log_lab_button")
                    ) {
                        Text("Log Follow-Up Lab")
                    }
                }
            }
        }
    }
}

@Composable
private fun BiomarkerTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LipidPanelHistoryCard(
    panel: LipidPanel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nonHdl = LipidAnalytics.effectiveNonHdl(panel)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lab_history_card_${panel.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.formatDateKeyShort(panel.drawDateKey),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (panel.baselinePanel) {
                            StatusPill(label = "Baseline", isPositive = true)
                        }
                    }
                    val meta = buildList {
                        add(if (panel.fasting) "Fasting" else "Non-fasting")
                        if (panel.labName.isNotBlank()) add(panel.labName)
                    }.joinToString(" • ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_lab_button_${panel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit lipid panel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_lab_button_${panel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete lipid panel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("LDL-C ${panel.ldlMgDl}", style = ClinicalMonoStyle, color = MaterialTheme.colorScheme.primary)
                Text("Total ${panel.totalCholesterolMgDl}", style = ClinicalMonoStyle)
                Text("HDL-C ${panel.hdlMgDl}", style = ClinicalMonoStyle)
                Text("TG ${panel.triglyceridesMgDl}", style = ClinicalMonoStyle)
                Text("Non-HDL $nonHdl", style = ClinicalMonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                panel.apoBMgDl?.let {
                    Text("ApoB $it", style = ClinicalMonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                panel.lpAMgDl?.let {
                    Text("Lp(a) $it", style = ClinicalMonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (panel.notes.isNotBlank()) {
                Text(
                    text = panel.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
