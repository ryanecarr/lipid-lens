package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.KnownStatin
import com.example.data.model.KnownStatinCatalog
import com.example.data.model.StatinRegimen
import com.example.data.remote.TherapyAddMode
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SwitchStatinSheet(
    activeRegimens: List<StatinRegimen>,
    preselectedStatin: KnownStatin? = null,
    initialMode: TherapyAddMode? = null,
    onDismiss: () -> Unit,
    onSave: (
        statinName: String,
        brandName: String,
        doseMg: Int,
        frequency: DoseFrequency,
        scheduledTimeMinutes: Int,
        therapyMode: TherapyAddMode,
        switchReason: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primaryRegimen = remember(activeRegimens) {
        activeRegimens.firstOrNull { it.normalizedRole() == StatinRegimen.THERAPY_ROLE_PRIMARY }
            ?: activeRegimens.firstOrNull()
    }
    val hasPrimary = primaryRegimen != null

    var therapyMode by remember(activeRegimens, preselectedStatin, initialMode) {
        val defaultMode = when {
            initialMode != null -> initialMode
            !hasPrimary -> TherapyAddMode.SWITCH_PRIMARY
            preselectedStatin?.defaultRole == StatinRegimen.THERAPY_ROLE_ADJUNCT -> TherapyAddMode.ADD_ADJUNCT
            else -> TherapyAddMode.SWITCH_PRIMARY
        }
        mutableStateOf(defaultMode)
    }

    val targetReferenceRegimen = remember(activeRegimens, preselectedStatin, therapyMode) {
        if (therapyMode == TherapyAddMode.ADD_ADJUNCT) {
            preselectedStatin?.let { target ->
                activeRegimens.firstOrNull {
                    it.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT &&
                        it.statinName.equals(target.genericName, ignoreCase = true)
                }
            }
        } else {
            primaryRegimen
        }
    }

    val initialKnown = remember(targetReferenceRegimen, preselectedStatin) {
        preselectedStatin
            ?: targetReferenceRegimen?.let { KnownStatinCatalog.findByName(it.statinName) }
            ?: KnownStatinCatalog.statins.first()
    }

    var isCustom by remember(targetReferenceRegimen, preselectedStatin) {
        mutableStateOf(
            preselectedStatin == null &&
                targetReferenceRegimen != null &&
                KnownStatinCatalog.findByName(targetReferenceRegimen.statinName) == null
        )
    }
    var selectedKnown by remember(initialKnown) { mutableStateOf(initialKnown) }
    var customName by remember(targetReferenceRegimen) {
        mutableStateOf(
            if (isCustom) targetReferenceRegimen?.statinName ?: "" else ""
        )
    }
    var customBrand by remember(targetReferenceRegimen) {
        mutableStateOf(
            if (isCustom) targetReferenceRegimen?.brandName ?: "" else ""
        )
    }
    var doseInput by remember(preselectedStatin, targetReferenceRegimen) {
        val defaultDose = preselectedStatin?.defaultDoseMg
            ?: targetReferenceRegimen?.doseMg
            ?: initialKnown.defaultDoseMg
        mutableStateOf(defaultDose.toString())
    }
    var frequency by remember(preselectedStatin, targetReferenceRegimen) {
        val defaultFreq = preselectedStatin?.preferredFrequency
            ?: targetReferenceRegimen?.let { DoseFrequency.fromKey(it.frequencyKey) }
            ?: DoseFrequency.ONCE_DAILY_EVENING
        mutableStateOf(defaultFreq)
    }
    var scheduledTimeMinutes by remember(targetReferenceRegimen) {
        mutableIntStateOf(targetReferenceRegimen?.scheduledTimeMinutes ?: frequency.defaultTimeMinutes)
    }
    var switchReason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    val parsedDose = doseInput.toIntOrNull() ?: 0
    val effectiveName = if (isCustom) customName.trim() else selectedKnown.genericName
    val effectiveBrand = if (isCustom) customBrand.trim() else selectedKnown.brandName
    val isValid = effectiveName.isNotBlank() && parsedDose in 1..4000

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when {
                        !hasPrimary -> "Set Up Lipid Therapy"
                        therapyMode == TherapyAddMode.ADD_ADJUNCT -> "Add Combination Therapy"
                        else -> "Switch Primary Therapy"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = when {
                        !hasPrimary ->
                            "Choose your prescribed statin, non-statin, or PCSK9 therapy—or enter a custom prescription."
                        therapyMode == TherapyAddMode.ADD_ADJUNCT ->
                            "Keeps ${primaryRegimen!!.statinName} ${primaryRegimen.doseMg} mg active alongside this add-on."
                        else ->
                            "Your past logs stay linked to ${primaryRegimen!!.statinName} ${primaryRegimen.doseMg} mg."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasPrimary) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = therapyMode == TherapyAddMode.SWITCH_PRIMARY,
                        onClick = { therapyMode = TherapyAddMode.SWITCH_PRIMARY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("therapy_mode_switch_primary")
                    ) {
                        Text("Switch Primary", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = therapyMode == TherapyAddMode.ADD_ADJUNCT,
                        onClick = { therapyMode = TherapyAddMode.ADD_ADJUNCT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("therapy_mode_add_adjunct")
                    ) {
                        Text("Add Adjunct", maxLines = 1)
                    }
                }
            }

            // Known Lipid Therapies Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Medication", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KnownStatinCatalog.statins.forEach { statin ->
                        val selected = !isCustom && selectedKnown.id == statin.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                isCustom = false
                                selectedKnown = statin
                                doseInput = statin.defaultDoseMg.toString()
                                frequency = statin.preferredFrequency
                                scheduledTimeMinutes = statin.preferredFrequency.defaultTimeMinutes
                                if (hasPrimary && initialMode == null) {
                                    therapyMode = if (statin.defaultRole == StatinRegimen.THERAPY_ROLE_ADJUNCT) {
                                        TherapyAddMode.ADD_ADJUNCT
                                    } else {
                                        TherapyAddMode.SWITCH_PRIMARY
                                    }
                                }
                            },
                            label = {
                                Text("${statin.genericName} (${statin.brandName})")
                            },
                            modifier = Modifier.testTag("statin_chip_${statin.id}")
                        )
                    }
                    FilterChip(
                        selected = isCustom,
                        onClick = { isCustom = true },
                        label = { Text("Custom Therapy") },
                        modifier = Modifier.testTag("statin_chip_custom")
                    )
                }
            }

            if (isCustom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { if (it.length <= 40) customName = it },
                        label = { Text("Medication Name") },
                        placeholder = { Text("e.g. Atorvastatin") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("custom_statin_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = customBrand,
                        onValueChange = { if (it.length <= 30) customBrand = it },
                        label = { Text("Brand (opt.)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${selectedKnown.therapyClass.label} • ${selectedKnown.timingGuidance}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = selectedKnown.clinicalNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Dose Strength
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dose Strength (mg)", style = MaterialTheme.typography.titleSmall)
                val doseChoices = if (isCustom) listOf(5, 10, 20, 40, 80) else selectedKnown.commonDosesMg
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    doseChoices.forEach { mg ->
                        val selected = parsedDose == mg
                        FilterChip(
                            selected = selected,
                            onClick = { doseInput = mg.toString() },
                            label = { Text("$mg mg") },
                            modifier = Modifier.testTag("dose_chip_${mg}")
                        )
                    }
                }
                OutlinedTextField(
                    value = doseInput,
                    onValueChange = { raw ->
                        if (raw.length <= 4 && raw.all { it.isDigit() }) {
                            doseInput = raw
                        }
                    },
                    label = { Text("Exact Dose (mg)") },
                    suffix = { Text("mg") },
                    supportingText = { Text("1–4000 mg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dose_mg_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Frequency & Scheduled Time
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Frequency & Preferred Time", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DoseFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = {
                                frequency = freq
                                if (freq != DoseFrequency.CUSTOM) {
                                    scheduledTimeMinutes = freq.defaultTimeMinutes
                                }
                            },
                            label = { Text(freq.label) },
                            modifier = Modifier.testTag("freq_chip_${freq.storageKey}")
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showTimePicker = true }
                        .testTag("regimen_time_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Scheduled dose time", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = DateTimeUtils.formatMinutesOfDay(scheduledTimeMinutes),
                            style = ClinicalMonoStyle,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (activeRegimens.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (therapyMode == TherapyAddMode.ADD_ADJUNCT) {
                            "Reason for add-on therapy (optional)"
                        } else {
                            "Reason for switch or adjustment (optional)"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KnownStatinCatalog.switchReasons.forEach { reason ->
                            FilterChip(
                                selected = switchReason == reason,
                                onClick = {
                                    switchReason = if (switchReason == reason) "" else reason
                                },
                                label = { Text(reason) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = switchReason,
                        onValueChange = { if (it.length <= 120) switchReason = it },
                        label = { Text("Clinical note for doctor report") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 200) notes = it },
                label = { Text("Regimen instructions (optional)") },
                placeholder = { Text("e.g., Take with dinner") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

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
                            effectiveName,
                            effectiveBrand,
                            parsedDose,
                            frequency,
                            scheduledTimeMinutes,
                            therapyMode,
                            switchReason,
                            notes
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_regimen_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        when {
                            !hasPrimary -> stringResource(R.string.action_save_regimen)
                            therapyMode == TherapyAddMode.ADD_ADJUNCT -> "Add Adjunct Therapy"
                            else -> "Switch Primary Therapy"
                        }
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = scheduledTimeMinutes / 60,
            initialMinute = scheduledTimeMinutes % 60
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Scheduled Dose Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    scheduledTimeMinutes = timeState.hour * 60 + timeState.minute
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
