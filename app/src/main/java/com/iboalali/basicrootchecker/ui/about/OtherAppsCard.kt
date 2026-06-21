package com.iboalali.basicrootchecker.ui.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.analytics.OTHER_APP_ACTION_LAUNCH
import com.iboalali.basicrootchecker.analytics.OTHER_APP_ACTION_PLAY_STORE
import com.iboalali.basicrootchecker.analytics.OTHER_APP_ACTION_WEBSITE
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewLocales
import com.iboalali.basicrootchecker.util.openPlayStoreListing
import com.iboalali.basicrootchecker.util.parseInlineMarkdown
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The About screen's "Other apps" card, driven by the (remote/cached) app catalog. Each row shows
 * the app's icon (loaded remotely via Coil, with a bundled fallback), name, localized description,
 * and latest "What's new" highlights, plus the actions:
 *
 * An installable app gets a Play Store–style button — "Open" (launches it) when installed, or
 * "Install" (opens the listing) when not — and any app with a website also gets a "Website" button.
 * A web-only app (no package) shows a single button: "Open" when the site is installed as a PWA
 * (WebAPK), otherwise "Website".
 *
 * The card hides itself when [apps] is empty (e.g. the catalog hasn't loaded yet on first run with
 * no bundled snapshot).
 */
@Composable
fun OtherAppsCard(
    apps: ImmutableList<OtherAppUi>,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty()) return

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

            apps.forEachIndexed { index, app ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                OtherAppRow(app = app)
            }
        }
    }
}

@Composable
private fun OtherAppRow(app: OtherAppUi) {
    val context = LocalContext.current
    val packageName = app.packageName
    val website = app.website
    // Non-null only when an installable app is actually installed (apps with a launcher activity are
    // visible via the <queries> MAIN intent in the manifest); reused as the intent to launch it.
    val launchIntent = remember(packageName) {
        packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
    }
    // Crisp local art for apps we ship icons for, used while the remote icon loads / when offline.
    val fallbackIcon = remember(packageName) { localIconFor(packageName) }
    val analyticsId = packageName ?: website ?: app.name

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = null,
                placeholder = painterResource(fallbackIcon),
                error = painterResource(fallbackIcon),
                fallback = painterResource(fallbackIcon),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (app.whatsNew.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            WhatsNewHighlights(highlights = app.whatsNew)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            // Align the buttons under the text (past the icon + its 16dp gap).
            modifier = Modifier.padding(start = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (packageName != null) {
                if (launchIntent != null) {
                    // Installed → "Open" launches the app (mirrors the Play Store's own button).
                    FilledTonalButton(
                        onClick = rememberHapticClick {
                            Analytics.trackOtherAppClicked(analyticsId, OTHER_APP_ACTION_LAUNCH)
                            context.startActivity(launchIntent)
                        },
                    ) {
                        Text(stringResource(R.string.about_other_app_open))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Icon(
                            painter = painterResource(R.drawable.open_in_new_24px),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                } else {
                    // Not installed → "Install" opens the Play Store listing.
                    FilledTonalButton(
                        onClick = rememberHapticClick {
                            Analytics.trackOtherAppClicked(analyticsId, OTHER_APP_ACTION_PLAY_STORE)
                            context.openPlayStoreListing(packageName)
                        },
                    ) {
                        Text(stringResource(R.string.about_other_app_install))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Icon(
                            painter = painterResource(R.drawable.download_24px),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                }
            }
            if (website != null) {
                // For a web-only entry, find an installed PWA (WebAPK) for the site and launch that
                // package directly. We don't rely on ACTION_VIEW link routing because an *unverified*
                // WebAPK (e.g. some Samsung Internet installs) isn't picked up by it — Android would
                // fall back to the browser. With no PWA installed, the button just opens the website.
                val pwaLaunchIntent = remember(packageName, website) {
                    if (packageName != null) null
                    else findInstalledPwaPackage(context, website)
                        ?.let { context.packageManager.getLaunchIntentForPackage(it) }
                }
                FilledTonalButton(
                    onClick = rememberHapticClick {
                        if (pwaLaunchIntent != null) {
                            Analytics.trackOtherAppClicked(analyticsId, OTHER_APP_ACTION_LAUNCH)
                            context.startActivity(pwaLaunchIntent)
                        } else {
                            Analytics.trackOtherAppClicked(analyticsId, OTHER_APP_ACTION_WEBSITE)
                            context.startActivity(Intent(Intent.ACTION_VIEW, website.toUri()))
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (pwaLaunchIntent != null) R.string.about_other_app_open
                            else R.string.about_other_app_website
                        )
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(
                        painter = painterResource(
                            if (pwaLaunchIntent != null) R.drawable.open_in_new_24px
                            else R.drawable.public_24px
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
            }
        }
    }
}

/**
 * The catalog's "What's new" highlights for an app, as a small bulleted list aligned under the row's
 * text (past the icon + its 16dp gap). Bullets may carry inline Markdown (`**bold**`/`*italic*`),
 * rendered via [parseInlineMarkdown].
 */
@Composable
private fun WhatsNewHighlights(highlights: ImmutableList<String>) {
    Column(modifier = Modifier.padding(start = 56.dp)) {
        AnimatedGradientText(
            text = stringResource(R.string.about_other_app_whats_new),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary,
            ),
        )
        Spacer(Modifier.height(4.dp))
        highlights.forEach { highlight ->
            Row(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = parseInlineMarkdown(highlight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One mirror period of the shimmer gradient, in px; the brush translates by this much per loop. */
private const val SHIMMER_SPAN_PX = 220f

/**
 * [text] drawn with a horizontal gradient brush that sweeps sideways on a loop, for a lightweight
 * "shimmer" highlight. The gradient tiles ([TileMode.Mirror]) and is translated each frame, so it
 * flows across text of any width without measuring it; one loop covers a full mirror period
 * (`2 * SHIMMER_SPAN_PX`) so the restart is seamless. The animated read stays in this leaf, so only
 * this `Text` recomposes per frame — not the surrounding row.
 */
@Composable
private fun AnimatedGradientText(
    text: String,
    style: TextStyle,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "whatsNewShimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * SHIMMER_SPAN_PX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "whatsNewShimmerTranslate",
    )
    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(translate, 0f),
        end = Offset(translate + SHIMMER_SPAN_PX, 0f),
        tileMode = TileMode.Mirror,
    )
    Text(text = text, style = style.copy(brush = brush), modifier = modifier)
}

/**
 * Crisp local icon for apps we bundle art for (used as the Coil placeholder/fallback so the row
 * looks right while the remote icon loads or when offline); a generic app icon otherwise.
 */
@DrawableRes
private fun localIconFor(packageName: String?): Int = when (packageName) {
    "com.iboalali.billboard" -> R.mipmap.billboard_app_icon
    "com.iboalali.hidepersistentnotifications" -> R.mipmap.hide_persistent_notification_app_icon
    else -> R.drawable.ic_baseline_android_24
}

/** Activity class shared by Chrome, Samsung Internet, and other Chromium browsers' WebAPK shells. */
private const val WEBAPK_SHELL_ACTIVITY_PREFIX = "org.chromium.webapk.shell_apk."

/**
 * The package of an installed PWA (WebAPK) that handles [url], or null if none. Browsers install
 * PWAs as WebAPKs with browser-specific package names (e.g. `org.chromium.webapk.*` for Chrome,
 * `com.sec.android.app.sbrowser.webapk.*` for Samsung Internet) but a shared shell activity class,
 * so we match on that class rather than the package name. Best-effort: browsers that install PWAs
 * as plain home-screen shortcuts (e.g. Firefox) aren't detectable.
 */
private fun findInstalledPwaPackage(context: Context, url: String): String? {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addCategory(Intent.CATEGORY_BROWSABLE)
    val pm = context.packageManager
    val resolvers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
    return resolvers
        .firstOrNull { it.activityInfo?.name?.startsWith(WEBAPK_SHELL_ACTIVITY_PREFIX) == true }
        ?.activityInfo?.packageName
}

@PreviewLocales
@Composable
private fun OtherAppsCardPreview() {
    BasicRootCheckerTheme {
        OtherAppsCard(
            apps = persistentListOf(
                OtherAppUi(
                    name = "Billboard",
                    description = "Show large text on screen, as big as possible without cutting it off.",
                    iconUrl = null,
                    website = "https://iboalali.com/app/billboard/",
                    packageName = "com.iboalali.billboard",
                    whatsNew = persistentListOf("New **dark theme** and bigger text scaling"),
                ),
                OtherAppUi(
                    name = "Icon Recomposer",
                    description = "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                    iconUrl = null,
                    website = "https://iboalali.com/Icon-Recomposer/",
                    packageName = null,
                    whatsNew = persistentListOf("Your work is **saved automatically** and restored when you return"),
                ),
            ),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
