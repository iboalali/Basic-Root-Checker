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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult

/**
 * Debug-only picker that lets a developer force any [RootResult] through the real check flow so the
 * animations and haptics can be exercised on a device without a matching root state. The only call
 * site is gated behind `BuildConfig.DEBUG`, so this is stripped from release builds. Labels are
 * hardcoded English on purpose — it is a developer tool, not user-facing UI.
 */
@Composable
fun DebugRootResultDialog(
    onSelectResult: (RootResult) -> Unit,
    onRealCheck: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Demo root result") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Runs the full check flow (animation + haptics) with a forced result.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                DemoRow(label = "Run real check", onClick = onRealCheck)
                HorizontalDivider()
                demoResultOptions.forEach { option ->
                    DemoRow(label = option.label) { onSelectResult(option.result) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DemoRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

private data class DemoResultOption(val label: String, val result: RootResult)

private val demoResultOptions = listOf(
    DemoResultOption("Rooted · Magisk v29.0", RootResult.Rooted(RootProvider.MAGISK, "29.0")),
    DemoResultOption("Rooted · KernelSU", RootResult.Rooted(RootProvider.KERNELSU, null)),
    DemoResultOption("Rooted · APatch", RootResult.Rooted(RootProvider.APATCH, null)),
    DemoResultOption("Rooted · Other", RootResult.Rooted(RootProvider.OTHER, null)),
    DemoResultOption("Not granted · Magisk", RootResult.RootedNotGranted(RootProvider.MAGISK)),
    DemoResultOption("Not granted · KernelSU", RootResult.RootedNotGranted(RootProvider.KERNELSU)),
    DemoResultOption("Not granted · APatch", RootResult.RootedNotGranted(RootProvider.APATCH)),
    DemoResultOption("Not granted · Other", RootResult.RootedNotGranted(RootProvider.OTHER)),
    DemoResultOption("Not rooted", RootResult.NotRooted),
    DemoResultOption("Unknown", RootResult.Unknown),
)
