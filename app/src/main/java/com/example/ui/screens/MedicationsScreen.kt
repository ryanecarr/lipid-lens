package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.KnownStatin
import com.example.data.model.KnownStatinCatalog
import com.example.data.model.StatinRegimen
import com.example.data.remote.TherapyAddMode
import com.example.ui.StatinUiState
import com.example.ui.components.StatusPill
import com.example.ui.theme.ClinicalMonoStyle

@Composable
fun MedicationsScreen(
    state: StatinUiState,
    onSwitchWithCatalogItem: (KnownStatin?) -> Unit,
    onOpenAddTherapy: (TherapyAddMode?) -> Unit = { onSwitchWithCatalogItem(null) },
    onDeactivateRegimen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingStopRegimen by remember { mutableStateOf<StatinRegimen?>(null) }

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
            // Current Active Therapy Regimen(s) Card
            item {
                val activeList = state.activeRegimens
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when {
                                activeList.isEmpty() -> "No Active Therapy Yet"
                                activeList.size > 1 -> "Current Combination Therapy (${activeList.size})"
                                else -> "Current Active Therapy"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (activeList.isEmpty()) {
                            Text(
                                text = "Choose a statin, non-statin, or PCSK9 therapy below—or enter a custom prescription to start tracking.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onOpenAddTherapy(TherapyAddMode.SWITCH_PRIMARY) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("meds_switch_button")
                            ) {
                                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Set Up Lipid Therapy")
                            }
                        } else {
                            activeList.forEachIndexed { idx, reg ->
                                if (idx > 0) HorizontalDivider()
                                val freq = DoseFrequency.fromKey(reg.frequencyKey)
                                val brandPart = if (reg.brandName.isNotBlank()) " (${reg.brandName})" else ""
                                val isAdjunct = reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StatusPill(
                                            label = if (isAdjunct) "Adjunct" else "Primary",
                                            isPositive = !isAdjunct
                                        )
                                        Text(
                                            text = "${reg.statinName}$brandPart — ${reg.doseMg} mg",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "${freq.label} • ${DateTimeUtils.formatMinutesOfDay(reg.scheduledTimeMinutes)} • Started ${DateTimeUtils.formatDateMillis(reg.startedAtMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = { pendingStopRegimen = reg },
                                        modifier = Modifier.testTag("stop_regimen_${reg.id}")
                                    ) {
                                        Text("Stop")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenAddTherapy(TherapyAddMode.SWITCH_PRIMARY) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("meds_switch_button")
                                ) {
                                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Switch Primary", maxLines = 1)
                                }
                                Button(
                                    onClick = { onOpenAddTherapy(TherapyAddMode.ADD_ADJUNCT) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("meds_add_adjunct_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Adjunct", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Regimen History (if user has switched or started regimens)
            if (state.allRegimens.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Therapy Timeline",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(state.allRegimens, key = { "reg_${it.id}" }) { reg ->
                    val freq = DoseFrequency.fromKey(reg.frequencyKey)
                    val roleLabel = if (reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "Adjunct" else "Primary"
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${reg.statinName} ${reg.doseMg} mg • $roleLabel",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(
                                    label = if (reg.active) "Active" else "Prior",
                                    isPositive = reg.active,
                                    isWarning = !reg.active
                                )
                            }
                            val endLabel = reg.endedAtMillis?.let { DateTimeUtils.formatDateMillis(it) } ?: "Present"
                            Text(
                                text = "${freq.shortLabel} • ${DateTimeUtils.formatDateMillis(reg.startedAtMillis)} to $endLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (reg.switchReason.isNotBlank()) {
                                Text(
                                    text = "Note: ${reg.switchReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Known Lipid Therapies Directory
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Cholesterol Therapies Directory",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap any statin, oral non-statin (ezetimibe, bempedoic acid), PCSK9 therapy, or prescription EPA to switch or add to your regimen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(KnownStatinCatalog.statins, key = { it.id }) { statin ->
                val activeMatch = state.activeRegimens.firstOrNull {
                    it.statinName.equals(statin.genericName, ignoreCase = true)
                }
                val isCurrent = activeMatch != null
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${statin.genericName} (${statin.brandName})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Doses: ${statin.commonDosesMg.joinToString(", ")} mg",
                                    style = ClinicalMonoStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isCurrent) {
                                StatusPill(label = "Current", isPositive = true)
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = statin.therapyClass.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${statin.timingGuidance} — ${statin.clinicalNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onSwitchWithCatalogItem(statin) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("catalog_select_${statin.id}")
                        ) {
                            Text(
                                when {
                                    isCurrent -> "Adjust Dose / Schedule"
                                    state.activePrimaryRegimen != null &&
                                        statin.defaultRole == StatinRegimen.THERAPY_ROLE_ADJUNCT ->
                                        "Add ${statin.genericName} as Adjunct"
                                    state.activePrimaryRegimen != null ->
                                        "Switch to ${statin.genericName}"
                                    else -> "Use ${statin.genericName}"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingStopRegimen?.let { reg ->
        AlertDialog(
            onDismissRequest = { pendingStopRegimen = null },
            title = { Text("Stop Active Therapy?") },
            text = {
                Text(
                    "Move ${reg.statinName} ${reg.doseMg} mg to your prior therapy timeline? All past dose logs and lipid panels stay intact."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeactivateRegimen(reg.id)
                        pendingStopRegimen = null
                    },
                    modifier = Modifier.testTag("confirm_stop_regimen_button")
                ) {
                    Text("Move to History")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStopRegimen = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
