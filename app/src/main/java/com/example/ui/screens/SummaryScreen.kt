package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.DoseLog
import com.example.data.model.FeelLevel
import com.example.data.model.SideEffectOption
import com.example.data.model.StatinRegimen
import com.example.data.remote.AiInsightUiState
import com.example.ui.StatinUiState
import com.example.ui.components.ClinicalHeroEmblem
import com.example.ui.components.SevenDayAdherenceStrip
import com.example.ui.components.StatusPill
import com.example.ui.components.SummaryInsightCard
import com.example.ui.components.icon
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(
    state: StatinUiState,
    aiInsightState: AiInsightUiState,
    onSetupOrSwitchStatin: () -> Unit,
    onLogTodayDose: () -> Unit,
    onLogForRegimen: (StatinRegimen) -> Unit = { onLogTodayDose() },
    onEditLog: (DoseLog) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenLabs: () -> Unit,
    onGenerateInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.activeRegimen

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (active == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ClinicalHeroEmblem(
                                contentDescriptionText = stringResource(R.string.cd_clinical_emblem)
                            )
                            Text(
                                text = stringResource(R.string.hero_no_statin_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = stringResource(R.string.hero_no_statin_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onSetupOrSwitchStatin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("onboarding_setup_statin_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Outlined.Medication, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_start_regimen))
                            }
                        }
                    }
                }
            } else {
                // Active Medication Hero Card
                item {
                    val freq = DoseFrequency.fromKey(active.frequencyKey)
                    val adjuncts = state.activeAdjunctRegimens.filter { it.id != active.id }
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (adjuncts.isNotEmpty()) "ACTIVE COMBINATION THERAPY"
                                        else stringResource(R.string.label_active_regimen),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = active.statinName,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    val brandPart = if (active.brandName.isNotBlank()) "${active.brandName} • " else ""
                                    Text(
                                        text = "$brandPart${active.doseMg} mg • ${freq.shortLabel}",
                                        style = ClinicalMonoStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Scheduled ${DateTimeUtils.formatMinutesOfDay(active.scheduledTimeMinutes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSetupOrSwitchStatin,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("dashboard_switch_statin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SwapHoriz,
                                        contentDescription = stringResource(R.string.cd_switch_statin),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Manage")
                                }
                            }

                            if (adjuncts.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    adjuncts.forEach { adj ->
                                        val adjFreq = DoseFrequency.fromKey(adj.frequencyKey).shortLabel
                                        StatusPill(
                                            label = "+ ${adj.statinName} ${adj.doseMg} mg • $adjFreq",
                                            isPositive = true
                                        )
                                    }
                                }
                            }

                            // Today's Check-In Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                if (state.activeRegimens.size > 1) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.label_todays_checkin),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        state.activeRegimens.forEachIndexed { idx, reg ->
                                            if (idx > 0) HorizontalDivider()
                                            val regLog = state.todayLogsByRegimenId[reg.id]
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${reg.statinName} ${reg.doseMg} mg",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = when {
                                                            regLog == null -> "Not logged yet today"
                                                            regLog.taken -> "Taken at ${DateTimeUtils.formatMinutesOfDay(regLog.takenAtMinutes)}"
                                                            else -> "Marked skipped today"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (regLog?.taken == true) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                                if (regLog != null) {
                                                    TextButton(
                                                        onClick = { onEditLog(regLog) },
                                                        modifier = Modifier.testTag("edit_regimen_log_${reg.id}")
                                                    ) {
                                                        Text("Edit")
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onLogForRegimen(reg) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.testTag("log_regimen_dose_${reg.id}")
                                                    ) {
                                                        Text("Log")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val todayLog = state.todayLog
                                    if (todayLog == null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.label_todays_checkin),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Not logged yet today",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                                StatusPill(
                                                    label = "Pending",
                                                    isPositive = false,
                                                    isWarning = true
                                                )
                                            }
                                            Button(
                                                onClick = onLogTodayDose,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                                    .testTag("log_todays_dose_button"),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.action_log_todays_dose))
                                            }
                                        }
                                    } else {
                                        val feel = FeelLevel.fromKey(todayLog.feelLevelKey)
                                        val effects = SideEffectOption.parseCsv(todayLog.sideEffectsCsv)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (todayLog.taken) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.tertiary
                                                    )
                                                    Column {
                                                        Text(
                                                            text = if (todayLog.taken) "Taken today at ${DateTimeUtils.formatMinutesOfDay(todayLog.takenAtMinutes)}"
                                                            else "Marked skipped today",
                                                            style = MaterialTheme.typography.titleSmall
                                                        )
                                                        Text(
                                                            text = "Feeling ${feel.label}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                TextButton(
                                                    onClick = { onEditLog(todayLog) },
                                                    modifier = Modifier.testTag("edit_todays_log_button")
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(stringResource(R.string.action_edit))
                                                }
                                            }
                                            if (effects.isNotEmpty()) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    effects.forEach { effect ->
                                                        StatusPill(label = effect.label, isPositive = false)
                                                    }
                                                }
                                            } else {
                                                StatusPill(label = "No side effects reported", isPositive = true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Adherence Metrics Row + 7-Day Strip
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricTile(
                                label = stringResource(R.string.label_current_streak),
                                value = "${state.currentStreakDays}d",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = stringResource(R.string.label_adherence_7d),
                                value = if (state.allLogs.isEmpty()) "—" else "${state.adherence7DayPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = stringResource(R.string.label_adherence_30d),
                                value = if (state.allLogs.isEmpty()) "—" else "${state.adherence30DayPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SevenDayAdherenceStrip(days = state.last7Days)
                    }
                }
            }

            // How You're Feeling & Side-Effect Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
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
                                text = stringResource(R.string.label_feeling_overview),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Last 30 days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (state.recentMoodCounts.isEmpty()) {
                            Text(
                                text = "Log your check-in to track how your energy and comfort respond over time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.recentMoodCounts.forEach { (feel, count) ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = feel.icon(),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${feel.label}: $count",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.label_side_effects_overview),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (state.recentSideEffectCounts.isEmpty()) {
                            Text(
                                text = if (state.allLogs.isEmpty()) "No side effects logged yet."
                                else "No side effects reported in the past 30 days.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxCount = state.recentSideEffectCounts.maxOf { it.second }.coerceAtLeast(1)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.recentSideEffectCounts.take(4).forEach { (effect, count) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = effect.label,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "$count ${if (count == 1) "day" else "days"}",
                                                style = ClinicalMonoStyle,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { count.toFloat() / maxCount.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Holistic Cardiology AI Insight Preview Card
            item {
                SummaryInsightCard(
                    insightState = aiInsightState,
                    latestLdlMgDl = state.allLipidPanels.firstOrNull()?.ldlMgDl,
                    ldlTargetMgDl = state.clinicalTargets.ldlTargetMgDl,
                    onAnalyze = onGenerateInsight,
                    onOpenLabs = onOpenLabs
                )
            }

            // Reminder & Doctor Export Quick Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val reminder = state.reminderSettings
                    val activeFreq = DoseFrequency.fromKey(active?.frequencyKey)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenReminders)
                            .testTag("summary_reminder_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (reminder.enabled) Icons.Outlined.NotificationsActive
                                else Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = if (reminder.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Reminder", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (reminder.enabled) {
                                    "${activeFreq.shortLabel} • ${DateTimeUtils.formatMinutesOfDay(reminder.hour * 60 + reminder.minute)}"
                                } else {
                                    "Tap to turn on"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenReports)
                            .testTag("summary_report_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Doctor Visit?",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Export formatted PDF clinical summary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
