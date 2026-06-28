package com.iboalali.basicrootchecker.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Visual constants for the large-screen detail dialog card; shared so previews match the runtime.
 */
object DetailCardDefaults {
    /** Scrim opacity over the dimmed main screen when the card is fully open. */
    const val MaxScrimAlpha = 0.32f
    val CornerRadius = 32.dp
    val MaxWidth = 640.dp
    val TonalElevation = 6.dp
    /** Inset between the card and the safe-draw area, so the card reads as a floating dialog. */
    val ContentPadding = 24.dp
}

/**
 * The visual frame for a secondary screen presented as a dialog on large screens: a scrim over the
 * dimmed main screen plus a centered, rounded [Surface] hosting [content]. Provides
 * [LocalDetailNavIcon] as [DetailNavIcon.CLOSE] so the hosted screen draws its X.
 *
 * This is intentionally a *stateless* visual: it owns no drag/animation state. The live overlay
 * (`DetailOverlayScene`) drives motion by passing a [scrimAlpha] lambda and a [cardModifier] that
 * offsets/drags the card; the screenshot test renders the resting open state with the defaults.
 * Both share this composable so the committed regression baseline stays representative of the real
 * card.
 *
 * @param modifier applied to the full-screen root (the overlay uses this to measure the viewport).
 * @param scrimAlpha read every frame inside [drawBehind] (a deferred draw-phase read), so animating
 *   it does not recompose the card subtree.
 * @param onScrimClick if non-null, tapping the scrim outside the card invokes it (dismiss). Already
 *   wrapped for haptics by the caller.
 * @param scrimClickLabel accessibility label for the scrim's dismiss action.
 * @param cardModifier extra modifiers for the card [Surface] (height bound, drag offset, gesture
 *   handling).
 */
@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    scrimAlpha: () -> Float = { DetailCardDefaults.MaxScrimAlpha },
    onScrimClick: (() -> Unit)? = null,
    scrimClickLabel: String? = null,
    cardModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim: a sibling *below* the card layer, so taps beside the card fall through to it and
        // dismiss, while taps on the card are consumed by the card drawn on top.
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .drawBehind {
                        drawRect(color = Color.Black, alpha = scrimAlpha().coerceIn(0f, 1f))
                    }
                    .then(
                        if (onScrimClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = scrimClickLabel,
                                onClick = onScrimClick,
                            )
                        } else {
                            Modifier
                        }
                    )
        )

        // Card layer: a transparent, full-screen, centering box (no pointer handler of its own, so
        // empty taps pass through to the scrim) holding the rounded card.
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(DetailCardDefaults.ContentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalDetailNavIcon provides DetailNavIcon.CLOSE) {
                Surface(
                    shape = RoundedCornerShape(DetailCardDefaults.CornerRadius),
                    tonalElevation = DetailCardDefaults.TonalElevation,
                    modifier =
                        Modifier.widthIn(max = DetailCardDefaults.MaxWidth)
                            .fillMaxWidth()
                            .then(cardModifier),
                ) {
                    content()
                }
            }
        }
    }
}
