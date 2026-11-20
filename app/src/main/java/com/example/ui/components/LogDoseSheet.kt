package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DoseLog
import com.example.data.model.StatinRegimen
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.FeelLevel
import com.example.data.model.SideEffectOption
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogDoseSheet(
    activeRegimens: List<StatinRegimen>,
    existingLog: DoseLog? = null,
    initialRegimen: StatinRegimen? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingLogId: String,
        regimen: StatinRegimen,
        taken: Boolean,
        logDateKey: String,
        takenAtMinutes: Int,
        feelLevel: FeelLevel,
        sideEffects: Set<SideEffectOption>,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedRegimen by remember(activeRegimens, existingLog, initialRegimen) {
        val matched = existingLog?.let { log ->
            activeRegimens.firstOrNull { it.id == log.regimenId }
        } ?: initialRegimen ?: activeRegimens.first()
        mutableStateOf(matched)
    }

    var taken by remember(existingLog) { mutableStateOf(existingLog?.taken ?: true) }
    var logDateKey by remember(existingLog) {
        mutableStateOf(existingLog?.logDateKey ?: DateTimeUtils.todayDateKey())
    }
    var takenAtMinutes by remember(existingLog) {
        mutableIntStateOf(existingLog?.takenAtMinutes ?: DateTimeUtils.currentMinutesOfDay())
    }
    var feelLevel by remember(existingLog) {
        mutableStateOf(
            if (existingLog != null) FeelLevel.fromKey(existingLog.feelLevelKey)
            else FeelLevel.GOOD
        )
    }
    var selectedEffects by remember(existingLog) {
        val parsed = existingLog?.let { SideEffectOption.parseCsv(it.sideEffectsCsv).toSet() }
            ?: setOf(SideEffectOption.NONE)
        mutableStateOf(if (parsed.isEmpty()) setOf(SideEffectOption.NONE) else parsed)
    }
    var notes by remember(existingLog) { mutableStateOf(existingLog?.notes ?: "") }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (existingLog != null) "Edit Therapy Check-In" else "Therapy Check-In",
                    style = MaterialTheme.typography.headlineSmall
                )
                val medName = existingLog?.statinName ?: selectedRegimen.statinName
                val medDose = existingLog?.doseMg ?: selectedRegimen.doseMg
                val freqLabel = DoseFrequency.fromKey(existingLog?.frequencyKey ?: selectedRegimen.frequencyKey).shortLabel
                Text(
                    text = "$medName • $medDose mg • $freqLabel",
                    style = ClinicalMonoStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (existingLog == null && activeRegimens.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select Therapy",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeRegimens.forEach { reg ->
                            val isAdjunct = reg.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT
                            val roleTag = if (isAdjunct) " • Adjunct" else ""
                            FilterChip(
                                selected = selectedRegimen.id == reg.id,
                                onClick = { selectedRegimen = reg },
                                label = { Text("${reg.statinName} ${reg.doseMg} mg$roleTag") },
                                modifier = Modifier.testTag("log_select_regimen_${reg.id}")
                            )
                        }
                    }
                }
            }

            // Taken vs Skipped
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = taken,
                    onClick = { taken = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("dose_taken_segment")
                ) {
                    Text("Dose Taken")
                }
                SegmentedButton(
                    selected = !taken,
                    onClick = { taken = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("dose_skipped_segment")
                ) {
                    Text("Skipped")
                }
            }

            // Date & Time Taken Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pick_log_date_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (logDateKey == DateTimeUtils.todayDateKey()) "Today"
                        else DateTimeUtils.formatDateKeyShort(logDateKey),
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pick_log_time_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = DateTimeUtils.formatMinutesOfDay(takenAtMinutes),
                        style = ClinicalMonoStyle,
                        maxLines = 1
                    )
                }
            }

            // Quick-tap Mood / Feel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How does it make you feel?",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeelLevel.entries.forEach { level ->
                        val selected = feelLevel == level
                        val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        val border = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                        val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { feelLevel = level }
                                .testTag("feel_option_${level.storageKey}"),
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, border)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = level.icon(),
                                    contentDescription = level.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = level.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Tagged Side Effects
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Potential side effects",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SideEffectOption.entries.forEach { option ->
                        val selected = option in selectedEffects
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedEffects = when {
                                    option == SideEffectOption.NONE -> setOf(SideEffectOption.NONE)
                                    selected -> {
                                        val next = selectedEffects - option
                                        if (next.isEmpty()) setOf(SideEffectOption.NONE) else next
                                    }
                                    else -> (selectedEffects - SideEffectOption.NONE) + option
                                }
                            },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (option == SideEffectOption.NONE) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                selectedLabelColor = if (option == SideEffectOption.NONE) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            ),
                            modifier = Modifier.testTag("side_effect_chip_${option.storageKey}")
                        )
                    }
                }
            }

            // Optional brief note
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 400) notes = it },
                label = { Text("Optional notes for your doctor") },
                placeholder = { Text("e.g., Mild thigh soreness after evening walk") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_notes_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        onSave(
                            existingLog?.id ?: "",
                            selectedRegimen,
                            taken,
                            logDateKey,
                            takenAtMinutes,
                            feelLevel,
                            selectedEffects,
                            notes
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_log_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_save_log))
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = takenAtMinutes / 60,
            initialMinute = takenAtMinutes % 60
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Time Taken") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    takenAtMinutes = timeState.hour * 60 + timeState.minute
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        DateKeyPickerDialog(
            selectedDateKey = logDateKey,
            onDateSelected = { selectedKey -> logDateKey = selectedKey },
            onDismiss = { showDatePicker = false },
            confirmTestTag = "dose_date_picker_confirm_button",
            dismissTestTag = "dose_date_picker_cancel_button"
        )
    }
}
