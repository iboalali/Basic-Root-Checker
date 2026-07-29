package com.iboalali.basicrootchecker.ui.tip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The tip-jar picker, shared by every surface that can start a tip: the Settings entry and the main
 * screen's support card (see `SupportCard`). One dialog means one place that handles the
 * not-yet-loaded price state.
 *
 * Callers are expected to dismiss on selection — Play's own purchase sheet takes over from there,
 * and the outcome is announced app-wide by `AppRoot`, not by whichever screen opened this.
 */
@Composable
internal fun TipJarDialog(
    products: ImmutableList<TipProduct>,
    onSelect: (TipTier) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tip_jar_dialog_title)) },
        text = { TipJarTiers(products = products, onSelect = onSelect) },
        confirmButton = {
            TextButton(onClick = rememberHapticClick(onDismiss)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun TipJarTiers(
    products: ImmutableList<TipProduct>,
    onSelect: (TipTier) -> Unit,
) {
    if (products.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.tip_jar_loading),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            products.forEach { product ->
                Card(
                    onClick = rememberHapticClick { onSelect(product.tier) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(product.tier.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = product.formattedPrice,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mimics the [AlertDialog] surface so the tip-jar layout renders in the IDE preview.
 * A real [AlertDialog] draws inside a [androidx.compose.ui.window.Dialog] window, which the
 * Compose preview renderer shows as blank — so the preview reuses [TipJarTiers] inside a
 * plain dialog-shaped [Surface] instead.
 */
@Composable
private fun TipJarDialogPreviewSurface(products: ImmutableList<TipProduct>) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tip_jar_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            TipJarTiers(products = products, onSelect = {})
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}

@PreviewLightDark
@PreviewDynamicColors
@Composable
private fun TipJarDialogPreview() {
    BasicRootCheckerTheme {
        TipJarDialogPreviewSurface(
            products = persistentListOf(
                TipProduct(TipTier.SMALL, "$1.99"),
                TipProduct(TipTier.MEDIUM, "$4.99"),
                TipProduct(TipTier.LARGE, "$9.99"),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun TipJarDialogLoadingPreview() {
    BasicRootCheckerTheme {
        TipJarDialogPreviewSurface(products = persistentListOf())
    }
}
