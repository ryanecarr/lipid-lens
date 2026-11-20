package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateTimeUtils
import com.example.data.model.LipidPanel
import com.example.ui.theme.ClinicalMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogLipidPanelSheet(
    existingPanel: LipidPanel? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingPanelId: String,
        drawDateKey: String,
        drawnAtMillis: Long,
        totalCholesterolMgDl: Int,
        ldlMgDl: Int,
        hdlMgDl: Int,
        triglyceridesMgDl: Int,
        nonHdlMgDl: Int?,
        apoBMgDl: Int?,
        lpAMgDl: Int?,
        fasting: Boolean,
        baselinePanel: Boolean,
        labName: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var drawDateKey by remember(existingPanel) {
        mutableStateOf(existingPanel?.drawDateKey ?: DateTimeUtils.todayDateKey())
    }
    var fasting by remember(existingPanel) {
        mutableStateOf(existingPanel?.fasting ?: true)
    }
    var baselinePanel by remember(existingPanel) {
        mutableStateOf(existingPanel?.baselinePanel ?: false)
    }
    var ldlText by remember(existingPanel) {
        mutableStateOf(existingPanel?.ldlMgDl?.toString() ?: "")
    }
    var totalText by remember(existingPanel) {
        mutableStateOf(existingPanel?.totalCholesterolMgDl?.toString() ?: "")
    }
    var hdlText by remember(existingPanel) {
        mutableStateOf(existingPanel?.hdlMgDl?.toString() ?: "")
    }
    var tgText by remember(existingPanel) {
        mutableStateOf(existingPanel?.triglyceridesMgDl?.toString() ?: "")
    }

    var showAdvanced by remember(existingPanel) {
        mutableStateOf(
            existingPanel?.nonHdlMgDl != null ||
                existingPanel?.apoBMgDl != null ||
                existingPanel?.lpAMgDl != null
        )
    }
    var nonHdlText by remember(existingPanel) {
        mutableStateOf(existingPanel?.nonHdlMgDl?.toString() ?: "")
    }
    var apoBText by remember(existingPanel) {
        mutableStateOf(existingPanel?.apoBMgDl?.toString() ?: "")
    }
    var lpAText by remember(existingPanel) {
        mutableStateOf(existingPanel?.lpAMgDl?.toString() ?: "")
    }
    var labName by remember(existingPanel) {
        mutableStateOf(existingPanel?.labName ?: "")
    }
    var notes by remember(existingPanel) {
        mutableStateOf(existingPanel?.notes ?: "")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedTotal = totalText.trim().toIntOrNull()
    val parsedHdl = hdlText.trim().toIntOrNull()
    val calculatedNonHdl = if (parsedTotal != null && parsedHdl != null && parsedTotal > parsedHdl) {
        parsedTotal - parsedHdl
    } else null

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
                    text = if (existingPanel != null) "Edit Lipid Panel" else "Log Blood Work (Lipid Panel)",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Enter values in mg/dL from your lab report to track trends and personalize insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Draw Date + Fasting Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("lab_draw_date_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (drawDateKey == DateTimeUtils.todayDateKey()) "Today"
                        else DateTimeUtils.formatDateKeyShort(drawDateKey),
                        maxLines = 1
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1.25f)) {
                    SegmentedButton(
                        selected = fasting,
                        onClick = { fasting = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("lab_fasting_segment")
                    ) {
                        Text("Fasting", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = !fasting,
                        onClick = { fasting = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("lab_non_fasting_segment")
                    ) {
                        Text("Non-fasting", maxLines = 1)
                    }
                }
            }

            // Core Lipid Panel Fields (2x2 grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ldlText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all(Char::isDigit)) {
                            ldlText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("LDL-C *") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lab_ldl_input")
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all(Char::isDigit)) {
                            totalText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Total Chol *") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lab_total_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hdlText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all(Char::isDigit)) {
                            hdlText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("HDL-C *") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lab_hdl_input")
                )
                OutlinedTextField(
                    value = tgText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all(Char::isDigit)) {
                            tgText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Triglycerides *") },
                    suffix = { Text("mg/dL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lab_triglycerides_input")
                )
            }

            if (calculatedNonHdl != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Non-HDL-C (Total − HDL)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$calculatedNonHdl mg/dL",
                            style = ClinicalMonoStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Optional Advanced Biomarkers Toggle
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.testTag("lab_toggle_advanced_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (showAdvanced) "Hide ApoB, Lp(a) & Non-HDL override"
                    else "Add optional ApoB, Lp(a) or Non-HDL-C"
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = nonHdlText,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all(Char::isDigit)) {
                                    nonHdlText = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Non-HDL-C") },
                            placeholder = {
                                if (calculatedNonHdl != null) Text("$calculatedNonHdl")
                            },
                            suffix = { Text("mg/dL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lab_non_hdl_input")
                        )
                        OutlinedTextField(
                            value = apoBText,
                            onValueChange = { input ->
                                if (input.length <= 3 && input.all(Char::isDigit)) {
                                    apoBText = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("ApoB") },
                            suffix = { Text("mg/dL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lab_apob_input")
                        )
                    }
                    OutlinedTextField(
                        value = lpAText,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all(Char::isDigit)) {
                                lpAText = input
                                errorMessage = null
                            }
                        },
                        label = { Text("Lipoprotein(a) / Lp(a)") },
                        suffix = { Text("mg/dL") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lab_lpa_input")
                    )
                }
            }

            OutlinedTextField(
                value = labName,
                onValueChange = { if (it.length <= 80) labName = it },
                label = { Text("Lab or clinic (optional)") },
                placeholder = { Text("e.g., Quest Diagnostics, Cardiology Clinic") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lab_name_input")
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Mark as pre-therapy baseline",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Anchors % LDL-C reduction on charts, AI insights, and PDFs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = baselinePanel,
                        onCheckedChange = { baselinePanel = it },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lab_baseline_switch")
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 400) notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g., 8 weeks after starting Repatha 140 mg") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lab_notes_input")
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
                        val total = totalText.trim().toIntOrNull()
                        val hdl = hdlText.trim().toIntOrNull()
                        val tg = tgText.trim().toIntOrNull()
                        val nonHdl = nonHdlText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        val apoB = apoBText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        val lpA = lpAText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

                        val validationError = when {
                            ldl == null || ldl !in 1..2000 -> "Enter a valid LDL-C (1–2000 mg/dL)."
                            total == null || total !in 1..2000 -> "Enter a valid Total Cholesterol (1–2000 mg/dL)."
                            hdl == null || hdl !in 1..500 -> "Enter a valid HDL-C (1–500 mg/dL)."
                            tg == null || tg !in 1..5000 -> "Enter a valid Triglycerides value (1–5000 mg/dL)."
                            nonHdl != null && nonHdl !in 1..2000 -> "Non-HDL-C must be 1–2000 mg/dL."
                            apoB != null && apoB !in 1..500 -> "ApoB must be 1–500 mg/dL."
                            lpA != null && lpA !in 1..1000 -> "Lp(a) must be 1–1000 mg/dL."
                            else -> null
                        }

                        if (validationError != null) {
                            errorMessage = validationError
                        } else {
                            val drawnMillis = DateTimeUtils.utcMillisFromDateKey(drawDateKey)
                            onSave(
                                existingPanel?.id ?: "",
                                drawDateKey,
                                drawnMillis,
                                total!!,
                                ldl!!,
                                hdl!!,
                                tg!!,
                                nonHdl,
                                apoB,
                                lpA,
                                fasting,
                                baselinePanel,
                                labName,
                                notes
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_lab_panel_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_save_lab_panel))
                }
            }
        }
    }

    if (showDatePicker) {
        DateKeyPickerDialog(
            selectedDateKey = drawDateKey,
            onDateSelected = { selectedKey -> drawDateKey = selectedKey },
            onDismiss = { showDatePicker = false },
            confirmTestTag = "lab_date_picker_confirm_button",
            dismissTestTag = "lab_date_picker_cancel_button"
        )
    }
}
