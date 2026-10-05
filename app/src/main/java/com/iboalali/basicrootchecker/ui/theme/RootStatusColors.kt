package com.iboalali.basicrootchecker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.MaterialColors

/**
 * Badge colors for the root check result. Material 3 has an error role but no success or warning
 * role, so those two are fixed hues. Green is harmonized toward the current primary, so it sits
 * inside a dynamic palette instead of clashing with it. The warning orange is not: it is only a few
 * degrees from the error red, and harmonizing toward a blue primary rotates it through red, which
 * makes "not granted" read as "not rooted".
 */
@Immutable
data class RootStatusColors(
    val success: Color,
    val onSuccess: Color,
    val failure: Color,
    val onFailure: Color,
    val warning: Color,
    val onWarning: Color,
    val neutral: Color,
    val onNeutral: Color,
)

@Composable
fun rememberRootStatusColors(): RootStatusColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val primary = scheme.primary
    return remember(scheme.error, scheme.onError, scheme.outline, scheme.surface, primary, dark) {
        RootStatusColors(
            success = (if (dark) SuccessDark else SuccessLight).harmonizeWith(primary),
            onSuccess = if (dark) OnSuccessDark else Color.White,
            failure = scheme.error,
            onFailure = scheme.onError,
            warning = if (dark) WarningDark else WarningLight,
            onWarning = if (dark) OnWarningDark else Color.White,
            neutral = scheme.outline,
            onNeutral = scheme.surface,
        )
    }
}

private fun Color.harmonizeWith(primary: Color): Color =
    Color(MaterialColors.harmonize(toArgb(), primary.toArgb()))

private val SuccessLight = Color(0xFF2E7D32)
private val SuccessDark = Color(0xFF81C995)
private val OnSuccessDark = Color(0xFF00391A)
private val WarningLight = Color(0xFFBF4A00)
private val WarningDark = Color(0xFFFFB68A)
private val OnWarningDark = Color(0xFF4A1A00)
