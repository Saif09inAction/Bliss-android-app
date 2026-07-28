package com.laiza.worker.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            val view = androidx.compose.ui.platform.LocalView.current
            Button(onClick = {
                com.laiza.worker.core.haptics.HapticManager.medium(view)
                onConfirm()
            }) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            val view = androidx.compose.ui.platform.LocalView.current
            TextButton(onClick = {
                com.laiza.worker.core.haptics.HapticManager.light(view)
                onDismiss()
            }) {
                Text(
                    text = dismissButtonText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
