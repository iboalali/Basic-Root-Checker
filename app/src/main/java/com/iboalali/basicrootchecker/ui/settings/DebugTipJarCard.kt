package com.iboalali.basicrootchecker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.billing.TipTier
import kotlinx.collections.immutable.ImmutableSet

/**
 * Debug-only readout of which durable record products are currently owned. The only call
 * site is gated behind `BuildConfig.DEBUG`, so this is stripped from release builds.
 * Labels are hardcoded English on purpose — developer tool.
 */
@Composable
fun DebugTipJarCard(
    supporterTiers: ImmutableSet<TipTier>,
    shape: Shape = settingsGroupShape(isFirst = false, isLast = true),
) {
    OutlinedCard(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(),
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Debug: record products",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Owned non-consumable tips (durable supporter record).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TipTier.entries.forEach { tier ->
                val owned = tier in supporterTiers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tier.recordProductId,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (owned) "✓ owned" else "✗ not owned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (owned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
