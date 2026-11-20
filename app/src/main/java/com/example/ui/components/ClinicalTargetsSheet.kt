package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ClinicalTargets
import com.example.data.model.TargetPreset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClinicalTargetsSheet(
    currentTargets: ClinicalTargets,
    onDismiss: () -> Unit,
    onSave: (
        ldlTargetMgDl: Int,
        nonHdlTargetMgDl: Int?,
        apoBTargetMgDl: Int?,
        riskLabel: String,
        clinicianNote: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPreset by remember(currentTargets) {
        mutableStateOf(TargetPreset.matchPreset(currentTargets))
    }
    var ldlText by remember(currentTargets) {
        mutableStateOf(currentTargets.ldlTargetMgDl.toString())
    }
    var nonHdlText by remember(currentTargets) {
        mutableStateOf(currentTargets.nonHdlTargetMgDl?.toString() ?: "")
    }
    var apoBText by remember(currentTargets) {
        mutableStateOf(currentTargets.apoBTargetMgDl?.toString() ?: "")
    }
    var clinicianNote by remember(currentTargets) {
        mutableStateOf(currentTargets.clinicianNote)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    text = stringResource(R.string.title_clinical_targets),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Set the lipid thresholds recommended by your clinician. These anchor your trend charts, AI summaries, and doctor PDFs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Guideline Target Tier", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                errorMessage = null
                                if (preset != TargetPreset.CUSTOM) {
                                    ldlText = preset.ldlTargetMgDl.toString()
                                    nonHdlText = preset.nonHdlTargetMgDl.toString()
                                    apoBText = preset.apoBTargetMgDl.toString()
                                }
                            },
                            label = { Text(preset.shortLabel) },
                            modifier = Modifier.testTag("target_preset_${preset.name}")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ldlText,
                onValueChange = { input ->
                    if (input.length <= 3 && input.all(Char::isDigit)) {
                        ldlText = input
                        selectedPreset = TargetPreset.CUSTOM
                        errorMessage = null
                    }
                },
                label = { Text("LDL-C Target *") },
                suffix = { Text("mg/dL") },
                supportingText = { Text("Typical thresholds: <100, <70, or <55 mg/dL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("targets_ldl_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nonHdlText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all(Char::isDigit)) {
                            nonHdlText = input
                            selectedPreset = TargetPreset.CUSTOM
                            errorMessage = null
                        }
                    },
                    label = { Text("Non-HDL Target (opt.)") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("targets_nonhdl_input")
                )
                OutlinedTextField(
                    value = apoBText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all(Char::isDigit)) {
                            apoBText = input
                            selectedPreset = TargetPreset.CUSTOM
                            errorMessage = null
                        }
                    },
                    label = { Text("ApoB Target (opt.)") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("targets_apob_input")
                )
            }

            OutlinedTextField(
                value = clinicianNote,
                onValueChange = { if (it.length <= 160) clinicianNote = it },
                label = { Text("Clinician note (optional)") },
                placeholder = { Text("e.g., Cardiologist goal after CAC score") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("targets_note_input")
            )

            errorMessage?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

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
                        val ldl = ldlText.trim().toIntOrNull()
                        val nonHdl = nonHdlText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        val apoB = apoBText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        val err = when {
                            ldl == null || ldl !in 20..300 -> "Enter an LDL-C target between 20 and 300 mg/dL."
                            nonHdl != null && nonHdl !in 30..400 -> "Non-HDL-C target must be 30–400 mg/dL."
                            apoB != null && apoB !in 20..250 -> "ApoB target must be 20–250 mg/dL."
                            else -> null
                        }
                        if (err != null) {
                            errorMessage = err
                        } else {
                            val label = if (selectedPreset != TargetPreset.CUSTOM) {
                                selectedPreset.riskLabel
                            } else {
                                "Custom (<$ldl)"
                            }
                            onSave(ldl!!, nonHdl, apoB, label, clinicianNote)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_targets_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Targets")
                }
            }
        }
    }
}
