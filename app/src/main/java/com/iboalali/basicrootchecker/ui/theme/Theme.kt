package com.iboalali.basicrootchecker.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Duration of the light/dark color cross-fade when the user switches the theme. */
private const val ThemeAnimationMillis = 400

@Composable
fun BasicRootCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme.animated(),
        content = content,
    )
}

/**
 * Returns a copy of this [ColorScheme] whose every role is driven by [animateColorAsState], so a
 * theme change cross-fades the whole app instead of swapping colors in a single frame. On the first
 * composition each color snaps to its target (no animation), so launching into a saved theme is
 * instant — only a later change (e.g. toggling the theme in Settings) animates.
 */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = ThemeAnimationMillis)

    @Composable
    fun anim(target: Color): Color = animateColorAsState(target, spec, label = "themeColor").value

    return copy(
        primary = anim(primary),
        onPrimary = anim(onPrimary),
        primaryContainer = anim(primaryContainer),
        onPrimaryContainer = anim(onPrimaryContainer),
        inversePrimary = anim(inversePrimary),
        secondary = anim(secondary),
        onSecondary = anim(onSecondary),
        secondaryContainer = anim(secondaryContainer),
        onSecondaryContainer = anim(onSecondaryContainer),
        tertiary = anim(tertiary),
        onTertiary = anim(onTertiary),
        tertiaryContainer = anim(tertiaryContainer),
        onTertiaryContainer = anim(onTertiaryContainer),
        background = anim(background),
        onBackground = anim(onBackground),
        surface = anim(surface),
        onSurface = anim(onSurface),
        surfaceVariant = anim(surfaceVariant),
        onSurfaceVariant = anim(onSurfaceVariant),
        surfaceTint = anim(surfaceTint),
        inverseSurface = anim(inverseSurface),
        inverseOnSurface = anim(inverseOnSurface),
        error = anim(error),
        onError = anim(onError),
        errorContainer = anim(errorContainer),
        onErrorContainer = anim(onErrorContainer),
        outline = anim(outline),
        outlineVariant = anim(outlineVariant),
        scrim = anim(scrim),
        surfaceBright = anim(surfaceBright),
        surfaceDim = anim(surfaceDim),
        surfaceContainerLowest = anim(surfaceContainerLowest),
        surfaceContainerLow = anim(surfaceContainerLow),
        surfaceContainer = anim(surfaceContainer),
        surfaceContainerHigh = anim(surfaceContainerHigh),
        surfaceContainerHighest = anim(surfaceContainerHighest),
    )
}
