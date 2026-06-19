package com.iboalali.basicrootchecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Whether tap/long-press haptics are enabled, mirroring the user's "Haptic feedback" setting.
 * Provided once around the app content (see `MainActivity`) from
 * [com.iboalali.basicrootchecker.data.UserPreferences.hapticsEnabled]; defaults to `true` so
 * previews and any unscoped composable still feel right. `compositionLocalOf` (not static) so
 * only the composables that read it recompose when the setting is toggled.
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * Wraps an [onClick] so a tap also plays a subtle [HapticFeedbackType.ContextClick] tick,
 * gated on [LocalHapticsEnabled]. Use for buttons, icon buttons, the FAB, clickable rows,
 * `selectable` rows, `Card(onClick = …)`, dropdown/dialog actions — any plain tap handler.
 */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(onClick, enabled, haptics) {
        {
            if (enabled) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
    }
}

/**
 * Like [rememberHapticClick] but for `toggleable` rows: wraps an [onValueChange] so flipping
 * the control plays the same subtle tick before the value change runs.
 */
@Composable
fun rememberHapticToggle(onValueChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val haptics = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(onValueChange, enabled, haptics) {
        { value: Boolean ->
            if (enabled) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onValueChange(value)
        }
    }
}

/**
 * Wraps an [onLongClick] so a long-press plays the standard [HapticFeedbackType.LongPress]
 * buzz before running the action, gated on [LocalHapticsEnabled]. Use for long-press gestures
 * (e.g. copy-on-long-press) that Compose doesn't already vibrate for.
 */
@Composable
fun rememberHapticLongClick(onLongClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(onLongClick, enabled, haptics) {
        {
            if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        }
    }
}
