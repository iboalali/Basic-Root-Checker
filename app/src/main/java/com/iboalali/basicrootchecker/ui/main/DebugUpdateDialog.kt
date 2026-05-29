package com.iboalali.basicrootchecker.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class DebugUpdateChoice { AVAILABLE, DOWNLOADING, DOWNLOADED, FAILED, RESET }

/**
 * Debug-only picker that jumps the in-app-update card to any state so the update flow can be
 * exercised without a real Play update. The only call site is gated behind `BuildConfig.DEBUG`, so
 * this is stripped from release builds. Labels are hardcoded English on purpose — developer tool.
 */
@Composable
fun DebugUpdateDialog(
    onSelect: (DebugUpdateChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Demo in-app update") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Drives the update card. The Update / Install buttons stay live, " +
                        "so you can also walk the real flow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                updateChoiceOptions.forEach { option ->
                    DebugUpdateRow(label = option.label) { onSelect(option.choice) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DebugUpdateRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

private data class DebugUpdateOption(val label: String, val choice: DebugUpdateChoice)

private val updateChoiceOptions = listOf(
    DebugUpdateOption("Update available", DebugUpdateChoice.AVAILABLE),
    DebugUpdateOption("Downloading (animated)", DebugUpdateChoice.DOWNLOADING),
    DebugUpdateOption("Update downloaded", DebugUpdateChoice.DOWNLOADED),
    DebugUpdateOption("Update failed", DebugUpdateChoice.FAILED),
    DebugUpdateOption("Reset / hide", DebugUpdateChoice.RESET),
)
