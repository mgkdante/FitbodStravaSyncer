package app.secondclass.healthsyncer.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = {
                    onConfirm()
                    onDismiss()
                }) { Text(UiStrings.YES) }
            },
            dismissButton = {
                Button(onClick = onDismiss) { Text(UiStrings.NO) }
            },
            shape = MaterialTheme.shapes.large // uses theme shape for consistency
        )
    }
}


@Composable
fun InfoHelpDialog(
    showDialog: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(UiStrings.OK) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text(UiStrings.OK)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(UiStrings.CANCEL)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
