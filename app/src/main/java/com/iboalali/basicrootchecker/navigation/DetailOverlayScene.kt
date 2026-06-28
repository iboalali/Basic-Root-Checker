package com.iboalali.basicrootchecker.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// How far the card must be dragged down (or flung past) before release dismisses it instead of
// springing back. A fixed distance reads better than a fraction here, because the dismiss travel
// (the full viewport) is large.
private val DismissDragThreshold = 120.dp

// Minimum fling speed that dismisses regardless of distance, mirroring the foundation default
// (AnchoredDraggableMinFlingVelocity, 125 dp/s).
private val DismissFlingVelocity = 125.dp

// Predictive back is a restrained "peek": the card tracks down to at most this fraction of the
// dismiss distance (with a decelerating ease); the commit animation finishes the rest, so a back
// swipe doesn't fling the card across the screen.
private const val PredictiveBackMaxTravelFraction = 0.12f

// When a gesture that was scrolling the list over-pulls past the top edge, the card only peeks down
// by this much (then stops and springs back) — it never dismisses from the same motion that was
// scrolling. Dismissing requires a fresh swipe-down that starts with the list already at the top.
private val ScrollEdgeHintTravel = 48.dp

/** The two resting positions of the detail card: fully open, or slid off the bottom (dismissed). */
internal enum class DragAnchor {
    Expanded,
    Dismissed,
}

/**
 * Drag/animation state for the detail card. Wraps a foundation [AnchoredDraggableState] whose
 * offset is the card's downward translation in px: 0 at [DragAnchor.Expanded], the viewport height
 * at [DragAnchor.Dismissed] (so the card clears the screen). The same offset drives both the card
 * position and the scrim alpha, so there is a single source of truth for the motion.
 */
@Stable
internal class DetailDismissState(val draggable: AnchoredDraggableState<DragAnchor>) {

    /** 0 fully open … 1 fully dismissed. Read inside draw/layout lambdas only (deferred). */
    val dismissProgress: Float
        get() {
            val offset = draggable.offset
            val dismissed = draggable.anchors.positionOf(DragAnchor.Dismissed)
            return if (offset.isNaN() || dismissed <= 0f) 0f
            else (offset / dismissed).coerceIn(0f, 1f)
        }

    /** Card's vertical translation in px; 0 until the anchors are measured. */
    fun cardOffsetY(): Int {
        val offset = draggable.offset
        return if (offset.isNaN()) 0 else offset.roundToInt()
    }

    /**
     * Set anchors from the measured viewport height; Dismissed = a full viewport-height slide down.
     */
    fun updateViewport(heightPx: Int) {
        if (heightPx <= 0) return
        draggable.updateAnchors(
            DraggableAnchors {
                DragAnchor.Expanded at 0f
                DragAnchor.Dismissed at heightPx.toFloat()
            }
        )
    }

    suspend fun animateToDismissed() = draggable.animateTo(DragAnchor.Dismissed)
}

@Composable
internal fun rememberDetailDismissState(): DetailDismissState {
    // Expanded initial value ⇒ instant appearance (offset snaps to 0 once anchors land). Switching
    // this to Dismissed + a show() animation in the content is all a future open animation needs.
    val draggable = remember { AnchoredDraggableState(initialValue = DragAnchor.Expanded) }
    return remember(draggable) { DetailDismissState(draggable) }
}

/**
 * Decide where a fling lands: dismiss if the fling is fast enough downward, or if the card is
 * already dragged past [DismissDragThreshold]; otherwise spring back open. Upward flings always
 * spring back. Mirrors the foundation fling behavior used for direct drags on the card.
 */
private fun targetForFling(
    draggable: AnchoredDraggableState<DragAnchor>,
    velocity: Float,
    dismissThresholdPx: Float,
    minFlingVelocityPx: Float,
): DragAnchor {
    val offset = draggable.offset
    if (offset.isNaN()) return DragAnchor.Expanded
    val expanded = draggable.anchors.positionOf(DragAnchor.Expanded)
    return when {
        velocity >= minFlingVelocityPx -> DragAnchor.Dismissed
        velocity <= -minFlingVelocityPx -> DragAnchor.Expanded
        offset - expanded >= dismissThresholdPx -> DragAnchor.Dismissed
        else -> DragAnchor.Expanded
    }
}

/**
 * Connects the hosted screen's vertical scroll to the card's dismiss drag. Inverted-direction twin
 * of Material's `ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection`: our card is
 * top-anchored (Expanded = min = 0, Dismissed = max), which mirrors the sheet's geometry, so the
 * consumption logic is identical — with one deliberate difference (the `scrolled` gate below).
 *
 * **A gesture that was scrolling the list never dismisses.** Otherwise a single swipe-up-to-scroll
 * followed by swipe-down-to-scroll would slide straight into a dismiss the moment the list hit its
 * top edge. We track whether the *current* gesture moved the list ([scrolled]); if it did, an
 * over-pull past the top only peeks the card down by [hintCapPx] and then springs back — never
 * dismisses, regardless of velocity. Dismissing requires a *fresh* swipe-down that starts with the
 * list already at the top (so the list consumes nothing and [scrolled] stays false). [scrolled]
 * resets at the end of each gesture (the fling callbacks, which fire on every drag end).
 *
 * - [onPreScroll] grabs upward drag first, to pull a partly-dragged card back up before the list
 *   scrolls. (At Expanded, [AnchoredDraggableState.dispatchRawDelta] clamps to 0, so nothing is
 *   stolen and the list scrolls normally.)
 * - [onPostScroll] consumes leftover downward drag — only present once the list is pinned at the
 *   top and the collapsing toolbar is fully expanded — to drag the card down (capped to a hint
 *   while [scrolled]).
 * - [onPreFling]/[onPostFling] route the fling velocity to [onSettle], passing [scrolled] so a
 *   scroll gesture springs back instead of dismissing.
 */
private fun detailDismissNestedScroll(
    state: AnchoredDraggableState<DragAnchor>,
    hintCapPx: Float,
    onSettle: (velocity: Float, scrolledThisGesture: Boolean) -> Unit,
): NestedScrollConnection =
    object : NestedScrollConnection {
        // True once the list has consumed any scroll during the current gesture.
        private var scrolled = false

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            return if (delta < 0f && source == NestedScrollSource.UserInput) {
                Offset(0f, state.dispatchRawDelta(delta))
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (consumed.y != 0f) scrolled = true
            val delta = available.y
            if (delta <= 0f) return Offset.Zero // only downward over-pull drags the card
            val capped =
                if (scrolled) {
                    // Hint only: let the card peek to hintCapPx, then consume nothing more.
                    val current = state.offset.let { if (it.isNaN()) 0f else it }
                    delta.coerceAtMost((hintCapPx - current).coerceAtLeast(0f))
                } else {
                    delta // fresh swipe-down from the top: full dismiss drag
                }
            return Offset(0f, state.dispatchRawDelta(capped))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val velocity = available.y
            val offset = state.offset
            val minPosition = state.anchors.minPosition()
            return if (velocity < 0f && !offset.isNaN() && offset > minPosition) {
                onSettle(velocity, scrolled)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            onSettle(available.y, scrolled)
            scrolled = false
            return available
        }
    }

/**
 * The overlay's content: the scrim + card from [DetailCard], wired for swipe-down-to-dismiss. The
 * card follows the finger (the offset is read in a deferred layout lambda), the scrim fades with
 * the drag, and any settle onto [DragAnchor.Dismissed] — from a direct drag, a nested-scroll
 * over-pull, a scrim tap, or [DetailDismissState.animateToDismissed] in `onRemove()` — funnels
 * through one [snapshotFlow] observer that calls [onDismissRequested] exactly once.
 */
@Composable
internal fun DetailOverlayContent(
    state: DetailDismissState,
    onDismissRequested: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { DismissDragThreshold.toPx() }
    val minFlingVelocityPx = with(density) { DismissFlingVelocity.toPx() }
    val scrollEdgeHintPx = with(density) { ScrollEdgeHintTravel.toPx() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        snapshotFlow { state.draggable.settledValue }
            .collect { if (it == DragAnchor.Dismissed) onDismissRequested() }
    }

    // Predictive back: track the card down with the back gesture and dismiss on commit. The custom
    // overlay isn't a platform Dialog window, so without this the back gesture isn't intercepted
    // for
    // the card and falls through to finishing the Activity. Registered inside the overlay content
    // so
    // it takes precedence (composed last) only while the card is shown. The settle animations run
    // in
    // `scope`, not the gesture's own coroutine — a cancelled gesture cancels that coroutine, so a
    // suspend spring-back must run independently.
    PredictiveBackHandler(enabled = true) { backEvents ->
        try {
            backEvents.collect { event ->
                val dismissed = state.draggable.anchors.positionOf(DragAnchor.Dismissed)
                val current = state.draggable.offset
                if (!dismissed.isNaN() && !current.isNaN()) {
                    val target =
                        dismissed *
                            PredictiveBackMaxTravelFraction *
                            LinearOutSlowInEasing.transform(event.progress)
                    state.draggable.dispatchRawDelta(target - current)
                }
            }
            scope.launch { state.animateToDismissed() }
        } catch (e: CancellationException) {
            scope.launch { state.draggable.animateTo(DragAnchor.Expanded) }
            throw e
        }
    }

    val dismissConnection =
        remember(state, dismissThresholdPx, minFlingVelocityPx, scrollEdgeHintPx) {
            detailDismissNestedScroll(state.draggable, scrollEdgeHintPx) { velocity, scrolled ->
                scope.launch {
                    // A gesture that was scrolling the list springs back; only a fresh swipe-down
                    // from the top is allowed to settle to Dismissed.
                    val target =
                        if (scrolled) DragAnchor.Expanded
                        else
                            targetForFling(
                                state.draggable,
                                velocity,
                                dismissThresholdPx,
                                minFlingVelocityPx,
                            )
                    state.draggable.animateTo(target)
                }
            }
        }

    val flingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.draggable,
            positionalThreshold = { dismissThresholdPx },
        )

    DetailCard(
        modifier = Modifier.onSizeChanged { state.updateViewport(it.height) },
        scrimAlpha = { DetailCardDefaults.MaxScrimAlpha * (1f - state.dismissProgress) },
        onScrimClick = rememberHapticClick(onDismissRequested),
        scrimClickLabel = stringResource(R.string.content_description_dismiss_dialog),
        cardModifier =
            Modifier.fillMaxHeight()
                .offset { IntOffset(0, state.cardOffsetY()) }
                .nestedScroll(dismissConnection)
                .anchoredDraggable(
                    state = state.draggable,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                ),
        content = content,
    )
}

/**
 * Metadata flag: tag a [NavEntry] with [detailOverlay] to present it via
 * [DetailOverlaySceneStrategy].
 */
internal object DetailOverlayKey : NavMetadataKey<Unit>

/** Entry metadata that opts a secondary screen into the custom dismissable overlay presentation. */
internal fun detailOverlay(): Map<String, Any> = metadata { put(DetailOverlayKey, Unit) }

/**
 * Presents the top entry as an in-composition dismissable card over the dimmed main screen, when
 * (and only when) it carries [detailOverlay] metadata — `AppNavigation` adds that only at expanded
 * width, so below the breakpoint this returns null and the entry falls through to the single-pane
 * push flow. Replaces the built-in `DialogSceneStrategy` (a separate platform `Dialog` window) so
 * the card can be swipe-dismissed, render inside the app's `testTagsAsResourceId` scope, and own
 * its exit animation via [OverlayScene.onRemove].
 */
internal class DetailOverlaySceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>
    ): Scene<NavKey>? {
        val entry = entries.lastOrNull() ?: return null
        entry.metadata[DetailOverlayKey] ?: return null
        val onBack = onBack
        return object : OverlayScene<NavKey> {
            override val key: Any = entry.contentKey
            override val entries: List<NavEntry<NavKey>> = listOf(entry)
            override val previousEntries: List<NavEntry<NavKey>> = entries.dropLast(1)
            // The main screen, rendered live and dimmed beneath the card.
            override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries.takeLast(1)

            // Assigned during content composition; read in onRemove() to run the exit animation.
            // The
            // scene instance is remembered by NavDisplay and kept composed until onRemove()
            // finishes
            // (see NavDisplay's currentOverlayScenes), so the field is valid when onRemove() runs.
            private lateinit var dismissState: DetailDismissState

            override val content: @Composable () -> Unit = {
                val state = rememberDetailDismissState()
                dismissState = state
                DetailOverlayContent(state = state, onDismissRequested = onBack) { entry.Content() }
            }

            override suspend fun onRemove() {
                if (
                    ::dismissState.isInitialized &&
                        dismissState.draggable.currentValue != DragAnchor.Dismissed
                ) {
                    dismissState.animateToDismissed()
                }
            }
        }
    }
}
