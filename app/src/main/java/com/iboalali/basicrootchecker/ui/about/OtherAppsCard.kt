package com.iboalali.basicrootchecker.ui.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iboalali.appcatalog.ui.OtherApp
import com.iboalali.appcatalog.ui.OtherAppRow
import com.iboalali.appcatalog.ui.OtherAppsDefaults
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewLocales
import com.iboalali.haptics.compose.rememberHapticClick
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The About screen's "Other apps" card. The rows themselves — layout, the Open/Install/Website
 * buttons, the highlights bullets and their shimmer, the PWA detection, the foreground
 * re-resolution — live in `com.iboalali.appcatalog:ui` and are shared with the other apps. What
 * stays here is this app's own chrome (the outlined card and its title) plus the three seams the
 * library deliberately doesn't own: haptics, analytics, and the bundled icons.
 *
 * This app's rows sit inside a card that already pads horizontally, so the only visual override is
 * the row's `contentPadding` — everything else is the shared default, which is what this card
 * rendered before the move.
 *
 * The card hides itself when [apps] is empty (e.g. the catalog hasn't loaded yet on first run with
 * no bundled snapshot).
 */
@Composable
fun OtherAppsCard(apps: ImmutableList<OtherApp>, modifier: Modifier = Modifier) {
    if (apps.isEmpty()) return

    // `rememberHapticClick` wraps a click; an empty one gives just the tick, to fire from
    // `onAction`.
    val playTick = rememberHapticClick {}
    val style = OtherAppsDefaults.style(contentPadding = PaddingValues(vertical = 12.dp))

    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text(
                text = stringResource(R.string.other_apps_title),
                style = MaterialTheme.typography.titleMedium,
            )

            apps.forEachIndexed { index, app ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                OtherAppRow(
                    app = app,
                    style = style,
                    onAction = { id, action ->
                        playTick()
                        Analytics.trackOtherAppClicked(id, action)
                    },
                    fallbackIcon = { entry -> painterResource(localIconFor(entry.packageName)) },
                )
            }
        }
    }
}

/**
 * Crisp local icon for apps we bundle art for (used as Coil's placeholder/fallback so the row looks
 * right while the remote icon loads or when offline); this app's own generic mark otherwise.
 *
 * The `else` branch is deliberately **not** null. Returning null would hand the row the shared
 * component's generic icon, and that is a *different drawable* — the library ships the Material
 * Symbols bugdroid the other two apps use, while this app has always drawn the older
 * `ic_baseline_android_24`. Leaving it null silently changed the art here; the screenshot diff
 * caught it. Keep naming the local one unless the change is a deliberate design decision.
 */
@DrawableRes
private fun localIconFor(packageName: String?): Int =
    when (packageName) {
        "com.iboalali.billboard" -> R.mipmap.billboard_app_icon
        "com.iboalali.hidepersistentnotifications" -> R.mipmap.hide_persistent_notification_app_icon
        else -> R.drawable.ic_baseline_android_24
    }

@PreviewLocales
@Composable
private fun OtherAppsCardPreview() {
    BasicRootCheckerTheme {
        OtherAppsCard(
            apps =
                persistentListOf(
                    OtherApp(
                        name = "Billboard",
                        description =
                            "Show large text on screen, as big as possible without cutting it off.",
                        iconUrl = null,
                        website = "https://iboalali.com/app/billboard/",
                        packageName = "com.iboalali.billboard",
                        highlights = listOf("New **dark theme** and bigger text scaling"),
                    ),
                    OtherApp(
                        name = "Icon Recomposer",
                        description =
                            "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                        iconUrl = null,
                        website = "https://iboalali.com/Icon-Recomposer/",
                        packageName = null,
                        highlights =
                            listOf(
                                "Your work is **saved automatically** and restored when you return"
                            ),
                    ),
                ),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp),
        )
    }
}
