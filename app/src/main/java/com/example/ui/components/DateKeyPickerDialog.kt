package com.example.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.DateTimeUtils

/**
 * Reusable Material 3 date picker dialog that works exclusively in canonical
 * calendar date keys ("yyyy-MM-dd"), encapsulating Material 3 DatePicker's
 * UTC-midnight epoch representation in a single place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateKeyPickerDialog(
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String = "date_picker_confirm_button",
    dismissTestTag: String = "date_picker_cancel_button"
) {
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.utcMillisFromDateKey(selectedDateKey)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    dateState.selectedDateMillis?.let { utcMillis ->
                        onDateSelected(DateTimeUtils.dateKeyFromUtcMillis(utcMillis))
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag(confirmTestTag)
            ) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(dismissTestTag)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = dateState)
    }
}
