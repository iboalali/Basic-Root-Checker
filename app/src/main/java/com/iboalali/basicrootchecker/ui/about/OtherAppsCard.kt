package com.iboalali.basicrootchecker.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewLocales
import androidx.core.net.toUri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class OtherApp(
    val name: String,
    @field:StringRes val descriptionRes: Int,
    @field:DrawableRes val iconRes: Int,
    val packageName: String,
)

@Composable
fun OtherAppsCard(
    apps: ImmutableList<OtherApp>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentPackageName = context.packageName
    val filteredApps = apps.filter { it.packageName != currentPackageName }

    if (filteredApps.isEmpty()) return

    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.other_apps_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(8.dp))

            filteredApps.forEach { app ->
                OtherAppItem(
                    app = app,
                    onClick = { openPlayStoreListing(context, app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun OtherAppItem(
    app: OtherApp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(app.iconRes),
                contentDescription = app.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(16.dp))

            Icon(
                painter = painterResource(R.drawable.open_in_new_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(app.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 52.dp, top = 4.dp),
        )
    }
}

private fun openPlayStoreListing(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    }
}

@PreviewLocales
@Composable
private fun OtherAppsCardPreview() {
    BasicRootCheckerTheme {
        OtherAppsCard(
            apps = persistentListOf(
                OtherApp(
                    name = "Billboard",
                    descriptionRes = R.string.other_apps_billboard_description,
                    iconRes = R.mipmap.billboard_app_icon,
                    packageName = "com.iboalali.billboard",
                ),
                OtherApp(
                    name = "Hide Persistent Notifications",
                    descriptionRes = R.string.other_apps_hide_notifications_description,
                    iconRes = R.mipmap.hide_persistent_notification_app_icon,
                    packageName = "com.iboalali.hidepersistentnotifications",
                ),
            ),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
