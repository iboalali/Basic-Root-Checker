package com.iboalali.basicrootchecker.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.PreviewLocales

@Composable
fun UpdateCard(
    updateStatus: AppUpdateEvent,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (updateStatus) {
                AppUpdateEvent.None -> Unit

                AppUpdateEvent.Available -> {
                    Text(
                        text = stringResource(R.string.update_available_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.update_available_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onUpdateClick) {
                        Text(stringResource(R.string.update_action_update))
                    }
                }

                is AppUpdateEvent.Downloading -> {
                    Text(
                        text = stringResource(R.string.update_downloading),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val total = updateStatus.totalBytes
                    val downloaded = updateStatus.bytesDownloaded
                    if (total > 0L) {
                        val progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(
                                R.string.update_progress_megabytes,
                                formatMegabytes(downloaded),
                                formatMegabytes(total),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                AppUpdateEvent.Downloaded -> {
                    Text(
                        text = stringResource(R.string.update_downloaded_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onInstallClick) {
                        Text(stringResource(R.string.update_action_install))
                    }
                }

                is AppUpdateEvent.Failed -> {
                    Text(
                        text = stringResource(R.string.update_failed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun formatMegabytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "%.1f".format(mb)
}

@PreviewLocales
@Composable
private fun UpdateCardAvailablePreview() {
    BasicRootCheckerTheme {
        UpdateCard(
            updateStatus = AppUpdateEvent.Available,
            onUpdateClick = {},
            onInstallClick = {},
        )
    }
}

@PreviewLocales
@Composable
private fun UpdateCardDownloadingPreview() {
    BasicRootCheckerTheme {
        UpdateCard(
            updateStatus = AppUpdateEvent.Downloading(
                bytesDownloaded = 3_500_000,
                totalBytes = 12_000_000,
            ),
            onUpdateClick = {},
            onInstallClick = {},
        )
    }
}

@PreviewLocales
@Composable
private fun UpdateCardDownloadedPreview() {
    BasicRootCheckerTheme {
        UpdateCard(
            updateStatus = AppUpdateEvent.Downloaded,
            onUpdateClick = {},
            onInstallClick = {},
        )
    }
}
