package com.iboalali.basicrootchecker.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewLocales

/**
 * Inline invitation to the tip jar, shown on the main screen after a root check once [SupportGate]
 * opens. Deliberately lean and dismissible: one line of pitch and a single button that opens the
 * shared `TipJarDialog`, so the ask never becomes a storefront on the app's primary screen.
 *
 * Visually identical to [UpdateCard] (same outlined 600dp-max card) so it reads as one of the
 * screen's cards rather than an ad.
 */
@Composable
fun SupportCard(
    onSupportClick: () -> Unit,
    onDismiss: () -> Unit,
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
                .padding(start = 24.dp, top = 16.dp, end = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.favorite_24px),
                    // Decorative: the title beside it already names the card.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.support_card_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.support_card_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = rememberHapticClick(onDismiss)) {
                    Icon(
                        painter = painterResource(R.drawable.close_24px),
                        contentDescription = stringResource(R.string.support_card_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                onClick = rememberHapticClick(onSupportClick),
            ) {
                Text(stringResource(R.string.support_card_action))
            }
        }
    }
}

@PreviewLocales
@Composable
private fun SupportCardPreview() {
    BasicRootCheckerTheme {
        SupportCard(onSupportClick = {}, onDismiss = {})
    }
}

@PreviewFontScale
@Composable
private fun SupportCardFontScalePreview() {
    BasicRootCheckerTheme {
        SupportCard(onSupportClick = {}, onDismiss = {})
    }
}
