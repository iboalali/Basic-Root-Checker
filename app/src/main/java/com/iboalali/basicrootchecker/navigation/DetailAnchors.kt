package com.iboalali.basicrootchecker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.toSize

/**
 * Shared, screen-space anchor rectangles that bridge the main screen and the large-screen detail
 * overlay so the overlay can run a *container transform*: open growing out of the tapped overflow
 * menu item, and close collapsing into the overflow icon button.
 *
 * Rects are stored in **screen coordinates** ([LayoutCoordinates.localToScreen]) because the
 * dropdown menu item lives in its own `Popup` window while the overlay renders in-composition
 * inside `AppRoot` — screen space is the only frame both can agree on. The overlay converts these
 * back into its own local space using its measured screen origin (see `DetailOverlayScene`).
 *
 * @see DetailOverlaySceneStrategy
 */
@Stable
internal class DetailAnchorState {
    /**
     * Where a freshly-tapped overflow menu item sat on screen, captured at click time (before the
     * `Popup` tears down). The overlay reads this once to grow from it, then clears it. Null when
     * no open is in flight (the overlay falls back to [overflowScreenRect], then to a centred
     * scale-up).
     */
    var openOriginScreenRect: Rect? by mutableStateOf(null)

    /**
     * The overflow icon button's current on-screen rect; the close transform collapses into this.
     */
    var overflowScreenRect: Rect? by mutableStateOf(null)

    /**
     * Whether the *real* overflow icon glyph should draw. The overlay holds this `false` for its
     * whole lifetime (the morphing card carries its own three-dots glyph) and restores it on
     * dispose — i.e. exactly when the collapse finishes — so the card appears to *become* the
     * overflow icon.
     */
    var overflowIconVisible: Boolean by mutableStateOf(true)
}

/** Provided once from `AppRoot`; identity is stable, so a static local avoids needless plumbing. */
internal val LocalDetailAnchors = staticCompositionLocalOf {
    // A sensible default so previews / tests that don't provide one still work (no overlay there).
    DetailAnchorState()
}

@Composable
internal fun rememberDetailAnchorState(): DetailAnchorState = remember { DetailAnchorState() }

/** This node's rect in absolute screen coordinates (valid even from inside a `Popup` window). */
internal fun LayoutCoordinates.screenRect(): Rect = Rect(localToScreen(Offset.Zero), size.toSize())

/** Translate a screen-space rect into a coordinate space whose origin sits at [overlayOrigin]. */
internal fun Rect.toLocal(overlayOrigin: Offset): Rect =
    translate(-overlayOrigin.x, -overlayOrigin.y)
