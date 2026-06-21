package com.iboalali.basicrootchecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.iboalali.basicrootchecker.util.RootHaptics

/**
 * Whether tap/long-press haptics are enabled, mirroring the user's "Haptic feedback" setting.
 * Provided once around the app content (see `MainActivity`) from
 * [com.iboalali.basicrootchecker.data.UserPreferences.hapticsEnabled]; defaults to `true` so
 * previews and any unscoped composable still feel right. `compositionLocalOf` (not static) so
 * only the composables that read it recompose when the setting is toggled.
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * The app-wide [RootHaptics] that plays UI tap/long-press feedback. `null` in previews and tests
 * (the helpers then simply no-op). Provided from `MainActivity`.
 */
val LocalAppHaptics = compositionLocalOf<RootHaptics?> { null }

/**
 * Builds the feedback action for a press, gated on [LocalHapticsEnabled]. We deliberately drive the
 * [android.os.Vibrator] (via [RootHaptics]) rather than `View.performHapticFeedback`: there is no
 * reliable way to detect whether a device actually renders a framework tap constant — many skinned
 * OEMs (Samsung, OnePlus/Oppo, Xiaomi, Vivo) silently drop `CONTEXT_CLICK`/`VIRTUAL_KEY` while the
 * call still reports success. [RootHaptics.playTap]/[RootHaptics.playLongPress] instead pick the
 * best actuator API by *queryable* capability, so a press is felt wherever vibration is possible.
 */
@Composable
private fun rememberPerformHaptic(longPress: Boolean): () -> Unit {
    val appHaptics = LocalAppHaptics.current
    val enabled = LocalHapticsEnabled.current
    return remember(appHaptics, enabled, longPress) {
        {
            if (enabled) {
                if (longPress) appHaptics?.playLongPress() else appHaptics?.playTap()
            }
        }
    }
}

/**
 * Wraps an [onClick] so a tap also plays a subtle haptic, gated on [LocalHapticsEnabled]. Use for
 * buttons, icon buttons, the FAB, clickable rows, `selectable` rows, `Card(onClick = …)`,
 * dropdown/dialog actions — any plain tap handler.
 */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val perform = rememberPerformHaptic(longPress = false)
    return remember(onClick, perform) {
        {
            perform()
            onClick()
        }
    }
}

/**
 * Like [rememberHapticClick] but for `toggleable` rows: wraps an [onValueChange] so flipping the
 * control plays the same subtle tap before the value change runs.
 */
@Composable
fun rememberHapticToggle(onValueChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val perform = rememberPerformHaptic(longPress = false)
    return remember(onValueChange, perform) {
        { value: Boolean ->
            perform()
            onValueChange(value)
        }
    }
}

/**
 * Wraps an [onLongClick] so a long-press plays a firmer haptic before running the action, gated on
 * [LocalHapticsEnabled]. Use for long-press gestures (e.g. copy-on-long-press) that Compose doesn't
 * already vibrate for.
 */
@Composable
fun rememberHapticLongClick(onLongClick: () -> Unit): () -> Unit {
    val perform = rememberPerformHaptic(longPress = true)
    return remember(onLongClick, perform) {
        {
            perform()
            onLongClick()
        }
    }
}
