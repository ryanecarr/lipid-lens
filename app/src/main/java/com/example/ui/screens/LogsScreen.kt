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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DoseLog
import com.example.data.model.DateTimeUtils
import com.example.data.model.FeelLevel
import com.example.data.model.SideEffectOption
import com.example.ui.LogFilter
import com.example.ui.StatinUiState
import com.example.ui.components.EmptyLogsIllustration
import com.example.ui.components.StatusPill
import com.example.ui.components.icon
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogsScreen(
    state: StatinUiState,
    onSelectFilter: (LogFilter) -> Unit,
    onSelectStatinFilter: (String?) -> Unit,
    onAddLog: () -> Unit,
    onEditLog: (DoseLog) -> Unit,
    onDeleteLog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteLog by remember { mutableStateOf<DoseLog?>(null) }
    val filteredLogs = state.filteredLogs

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Filter bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.logFilter == LogFilter.ALL,
                        onClick = { onSelectFilter(LogFilter.ALL) },
                        label = { Text("All (${state.allLogs.size})") }
                    )
                    FilterChip(
                        selected = state.logFilter == LogFilter.TAKEN,
                        onClick = { onSelectFilter(LogFilter.TAKEN) },
                        label = { Text("Taken") }
                    )
                    FilterChip(
                        selected = state.logFilter == LogFilter.SKIPPED,
                        onClick = { onSelectFilter(LogFilter.SKIPPED) },
                        label = { Text("Skipped") }
                    )
                    FilterChip(
                        selected = state.logFilter == LogFilter.WITH_SIDE_EFFECTS,
                        onClick = { onSelectFilter(LogFilter.WITH_SIDE_EFFECTS) },
                        label = { Text("Side Effects") }
                    )
                }

                if (state.distinctLoggedStatins.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.logStatinFilter == null,
                            onClick = { onSelectStatinFilter(null) },
                            label = { Text("All Statins") }
                        )
                        state.distinctLoggedStatins.forEach { statinName ->
                            FilterChip(
                                selected = state.logStatinFilter == statinName,
                                onClick = { onSelectStatinFilter(statinName) },
                                label = { Text(statinName) }
                            )
                        }
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmptyLogsIllustration(
                            contentDescriptionText = stringResource(R.string.cd_empty_logs_art)
                        )
                        Text(
                            text = if (state.allLogs.isEmpty()) "No daily logs yet"
                            else "No logs match this filter",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (state.activeRegimen == null) {
                                "Set up your active statin on the Summary tab to begin logging."
                            } else {
                                "Record when you took your statin, how you felt, and any side effects."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.activeRegimen != null && state.allLogs.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onAddLog,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("empty_logs_add_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Log First Dose")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val feel = FeelLevel.fromKey(log.feelLevelKey)
                        val effects = SideEffectOption.parseCsv(log.sideEffectsCsv)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_card_${log.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatDateKeyShort(log.logDateKey),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${log.statinName} ${log.doseMg} mg • ${DateTimeUtils.formatMinutesOfDay(log.takenAtMinutes)}",
                                            style = ClinicalMonoStyle,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusPill(
                                            label = if (log.taken) "Taken" else "Skipped",
                                            isPositive = log.taken,
                                            isWarning = !log.taken
                                        )
                                        IconButton(onClick = { onEditLog(log) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = stringResource(R.string.action_edit),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(onClick = { pendingDeleteLog = log }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.action_delete),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = feel.icon(),
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "Feeling ${feel.label}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    if (effects.isEmpty()) {
                                        StatusPill(label = "No side effects", isPositive = true)
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
                                }

                                if (log.notes.isNotBlank()) {
                                    Text(
                                        text = log.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteLog?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLog = null },
            title = { Text("Delete this log?") },
            text = {
                Text(
                    "Remove your ${DateTimeUtils.formatDateKeyShort(target.logDateKey)} entry for ${target.statinName} ${target.doseMg} mg?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLog(target.id)
                        pendingDeleteLog = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
