package com.iboalali.basicrootchecker.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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
import com.iboalali.basicrootchecker.ui.LocalAppHaptics
import com.iboalali.basicrootchecker.ui.LocalHapticsEnabled
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// How far a swipe must be dragged down (or flung past) before release collapses the card instead of
// springing it back. A fixed distance reads better than a fraction here.
private val DismissDragThreshold = 120.dp

// Minimum downward fling speed that commits a close regardless of distance (the foundation
// default).
private val DismissFlingVelocity = 125.dp

// While a gesture that was scrolling the list over-pulls past the top edge, the card only peeks
// down
// by this much (then never commits) — dismissing requires a *fresh* swipe-down from the top.
private val ScrollEdgeHintTravel = 48.dp

// The asymptote of the elastic swipe-down pull: the card resists, decelerating toward this travel,
// and never slides off-screen. Comfortably above [DismissDragThreshold] so the threshold is
// reachable while the pull still feels springy.
private val MaxElasticPull = 360.dp

// Predictive back is a restrained peek: the card previews at most this fraction of the
// collapse-to-overflow as you pull; the commit animation finishes the rest, so a back swipe doesn't
// fling the card across the screen.
private const val BackMaxCollapseFraction = 0.12f

private val OpenSpec: AnimationSpec<Float> =
    tween(durationMillis = 350, easing = FastOutSlowInEasing)
private val CloseSpec: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
private val DragSpringSpec: AnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/**
 * Drives the large-screen detail dialog's *container transform*. There are two motion drivers:
 * - [progress] (1 open … 0 collapsed) — the single close/open driver. It interpolates the card's
 *   rect (scale + translation), corner radius, scrim alpha, and the content/morph-icon cross-fade
 *   between the resting card and the current anchor rect. Open animates 0→1 from the tapped menu
 *   item; every close animates →0 into the overflow icon.
 * - [dragOffset] (px) — the transient elastic swipe-down pull, meaningful only while open
 *   ([progress] == 1). It is added to the card's downward translation. A release past threshold
 *   folds it into a single [commitClose] (both decay with the same spec, so the motion stays
 *   continuous), and a release below threshold springs it back.
 *
 * All hot values are read inside deferred draw/layout lambdas, so animating them never recomposes
 * the card subtree.
 */
@Stable
internal class DetailMorphState {
    /** 1 = fully open (resting, centred), 0 = collapsed into the current anchor rect. */
    val progress = Animatable(1f)

    /** Elastic swipe-down pull in px, downward-positive. */
    var dragOffset by mutableFloatStateOf(0f)
        private set

    private var rawPull = 0f

    /**
     * Resting card rect (screen space), measured only while untransformed (progress≈1, no drag).
     */
    var openScreenRect by mutableStateOf<Rect?>(null)

    /** Where the card grows *from* on open (the tapped menu item), in overlay-local space. */
    var openOriginLocalRect by mutableStateOf<Rect?>(null)

    /** Where the card collapses *into* on close (the overflow icon), in overlay-local space. */
    var overflowLocalRect by mutableStateOf<Rect?>(null)

    /** True for any close (anchor = overflow + draw the three-dots morph glyph); false on open. */
    var renderMorphIcon by mutableStateOf(false)

    /** Gates the whole overlay visible; held false for the one untransformed measurement frame. */
    var revealed by mutableStateOf(false)

    var overlayOrigin by mutableStateOf(Offset.Zero)
    var overlayOriginKnown by mutableStateOf(false)
    var overlaySize by mutableStateOf(IntSize.Zero)

    private var popRequested = false

    /**
     * Apply a finger delta to the elastic pull with decelerating resistance (optionally capped).
     */
    fun applyDrag(deltaPx: Float, maxPullPx: Float, capPx: Float?) {
        rawPull = (rawPull + deltaPx).coerceAtLeast(0f)
        var resisted = maxPullPx * (1f - exp(-rawPull / maxPullPx))
        if (capPx != null) resisted = resisted.coerceAtMost(capPx)
        dragOffset = resisted
    }

    suspend fun springDragBack() {
        rawPull = 0f
        val anim = Animatable(dragOffset)
        anim.animateTo(0f, DragSpringSpec) { dragOffset = value }
    }

    /**
     * Collapse into the overflow, carrying any elastic pull along on the same spec for continuity.
     */
    suspend fun commitClose(spec: AnimationSpec<Float>) {
        renderMorphIcon = true
        rawPull = 0f
        coroutineScope {
            launch { progress.animateTo(0f, spec) }
            if (dragOffset != 0f) {
                launch {
                    val anim = Animatable(dragOffset)
                    anim.animateTo(0f, spec) { dragOffset = value }
                }
            }
        }
    }

    suspend fun cancelClose(spec: AnimationSpec<Float>) {
        progress.animateTo(1f, spec)
        renderMorphIcon = false
    }

    /** Funnels every dismiss path to a single pop. */
    fun requestPop(onBack: () -> Unit) {
        if (popRequested) return
        popRequested = true
        onBack()
    }

    fun isWithinViewport(rect: Rect): Boolean {
        val w = overlaySize.width.toFloat()
        val h = overlaySize.height.toFloat()
        if (w <= 0f || h <= 0f) return true
        val c = rect.center
        return c.x in 0f..w && c.y in 0f..h
    }

    /** Screen content cross-fade: present while open, gone by the time the squash gets severe. */
    fun contentAlpha(): Float = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)

    /** The three-dots morph glyph: fades in over the last half of a close. */
    fun morphIconAlpha(): Float =
        if (renderMorphIcon) ((0.5f - progress.value) / 0.5f).coerceIn(0f, 1f) else 0f

    /** The card's surface (background + elevation): fades out last, so the handoff lands bare. */
    fun surfaceAlpha(): Float = (progress.value / 0.2f).coerceIn(0f, 1f)
}

@Composable
internal fun rememberDetailMorphState(): DetailMorphState = remember { DetailMorphState() }

/** Small rect centred on the resting card — the open-from fallback when no real anchor is known. */
private fun centerFallback(open: Rect): Rect {
    val w = open.width * 0.1f
    val h = open.height * 0.1f
    val c = open.center
    return Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)
}

/**
 * Connects the hosted screen's vertical scroll to the elastic swipe-down dismiss.
 * Inverted-direction twin of Material's
 * `ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection`.
 *
 * **A gesture that was scrolling the list never dismisses.** We track whether the current gesture
 * moved the list ([scrolled]); if it did, an over-pull past the top only peeks the card down by
 * [hintCapPx] and then springs back — never commits. Dismissing requires a fresh swipe-down that
 * starts with the list already at the top (so [scrolled] stays false).
 *
 * - [onPreScroll] grabs upward drag first, to retract a partly-pulled card before the list scrolls.
 * - [onPostScroll] routes leftover downward over-pull — present only once the list is pinned at the
 *   top and the collapsing toolbar is fully expanded — into the elastic pull.
 * - [onPostFling] hands the end-of-gesture velocity to [onRelease].
 */
private fun detailDismissNestedScroll(
    state: DetailMorphState,
    maxPullPx: Float,
    hintCapPx: Float,
    onRelease: (velocity: Float, scrolledThisGesture: Boolean) -> Unit,
): NestedScrollConnection =
    object : NestedScrollConnection {
        private var scrolled = false

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            return if (
                delta < 0f && source == NestedScrollSource.UserInput && state.dragOffset > 0f
            ) {
                val before = state.dragOffset
                state.applyDrag(delta, maxPullPx, if (scrolled) hintCapPx else null)
                Offset(0f, state.dragOffset - before)
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
            if (delta <= 0f) return Offset.Zero
            val before = state.dragOffset
            state.applyDrag(delta, maxPullPx, if (scrolled) hintCapPx else null)
            return Offset(0f, state.dragOffset - before)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            onRelease(available.y, scrolled)
            scrolled = false
            return available
        }
    }

/**
 * The overlay's content: the scrim + card from [DetailCard], wired for the container transform. The
 * card grows from the tapped menu item on open and collapses into the overflow icon on close (scrim
 * tap, the close button, a swipe-down past threshold, or predictive back). The real overflow glyph
 * is held hidden for the overlay's lifetime so the morphing card can become it.
 */
@Composable
internal fun DetailOverlayContent(
    state: DetailMorphState,
    onDismissRequested: () -> Unit,
    content: @Composable () -> Unit,
) {
    val anchors = LocalDetailAnchors.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { DismissDragThreshold.toPx() }
    val flingVelocityPx = with(density) { DismissFlingVelocity.toPx() }
    val hintCapPx = with(density) { ScrollEdgeHintTravel.toPx() }
    val maxPullPx = with(density) { MaxElasticPull.toPx() }
    val openCornerPx = with(density) { DetailCardDefaults.CornerRadius.toPx() }
    val scope = rememberCoroutineScope()

    val appHaptics = LocalAppHaptics.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val playTick: () -> Unit =
        remember(appHaptics, hapticsEnabled) { { if (hapticsEnabled) appHaptics?.playTap() } }

    // Keep the screen→overlay-local conversions fresh so onRemove() (which runs outside
    // composition)
    // and the deferred transform reads always see the current rects.
    SideEffect {
        if (state.overlayOriginKnown) {
            state.overflowLocalRect = anchors.overflowScreenRect?.toLocal(state.overlayOrigin)
        }
    }

    // Open: once our origin and the resting card rect are measured (both captured at progress==1,
    // before this animation starts), grow from the tapped item — falling back to the overflow, then
    // to a centred scale-up.
    LaunchedEffect(Unit) {
        snapshotFlow { state.overlayOriginKnown && state.openScreenRect != null }
            .filter { it }
            .first()
        val open = state.openScreenRect?.toLocal(state.overlayOrigin) ?: return@LaunchedEffect
        val originLocal =
            anchors.openOriginScreenRect?.toLocal(state.overlayOrigin)?.takeIf {
                state.isWithinViewport(it)
            } ?: state.overflowLocalRect ?: centerFallback(open)
        state.openOriginLocalRect = originLocal
        anchors.openOriginScreenRect = null // consume so a later open can't reuse a stale rect
        state.renderMorphIcon = false
        state.progress.snapTo(0f)
        state.revealed = true
        state.progress.animateTo(1f, OpenSpec)
    }

    // Hide the real overflow glyph while the overlay is up; restore it the instant the overlay
    // leaves
    // composition — which, because NavDisplay awaits onRemove(), is after the collapse completes.
    DisposableEffect(anchors) {
        anchors.overflowIconVisible = false
        onDispose { anchors.overflowIconVisible = true }
    }

    // One light tap when a swipe first crosses the dismiss threshold (the "Release to close"
    // moment).
    LaunchedEffect(state, thresholdPx) {
        snapshotFlow { state.dragOffset >= thresholdPx }
            .distinctUntilChanged()
            .collect { past -> if (past) playTick() }
    }

    // Predictive back mirrors the swipe: preview a peek of the collapse, commit on release, return
    // on
    // cancel. The custom overlay isn't a platform Dialog, so without this the gesture would fall
    // through and finish the Activity. The settle animations run in `scope` (not the gesture's own
    // coroutine), so a cancelled gesture can still finish its spring-back.
    PredictiveBackHandler(enabled = true) { backEvents ->
        state.renderMorphIcon = true
        try {
            backEvents.collect { event ->
                val eased = FastOutSlowInEasing.transform(event.progress)
                state.progress.snapTo(1f - eased * BackMaxCollapseFraction)
            }
            state.commitClose(CloseSpec)
            state.requestPop(onDismissRequested)
        } catch (e: CancellationException) {
            scope.launch { state.cancelClose(CloseSpec) }
            throw e
        }
    }

    val onRelease: (Float, Boolean) -> Unit =
        remember(state, thresholdPx, flingVelocityPx, onDismissRequested) {
            { velocity, scrolled ->
                scope.launch {
                    if (scrolled) {
                        state.springDragBack()
                    } else if (state.dragOffset >= thresholdPx || velocity >= flingVelocityPx) {
                        state.commitClose(CloseSpec)
                        state.requestPop(onDismissRequested)
                    } else {
                        state.springDragBack()
                    }
                }
            }
        }

    val dismissConnection =
        remember(state, maxPullPx, hintCapPx, onRelease) {
            detailDismissNestedScroll(state, maxPullPx, hintCapPx, onRelease)
        }
    val chromeDrag = rememberDraggableState { delta -> state.applyDrag(delta, maxPullPx, null) }

    val showReleaseHint by
        remember(state, thresholdPx) { derivedStateOf { state.dragOffset >= thresholdPx } }

    Box(modifier = Modifier.fillMaxSize()) {
        DetailCard(
            modifier =
                Modifier.onGloballyPositioned { coords ->
                        state.overlayOrigin = coords.localToScreen(Offset.Zero)
                        state.overlaySize = coords.size
                        state.overlayOriginKnown = true
                    }
                    .graphicsLayer { alpha = if (state.revealed) 1f else 0f },
            scrimAlpha = { DetailCardDefaults.MaxScrimAlpha * state.progress.value },
            onScrimClick = rememberHapticClick { state.requestPop(onDismissRequested) },
            scrimClickLabel = stringResource(R.string.content_description_dismiss_dialog),
            cardModifier =
                Modifier.fillMaxHeight()
                    .onGloballyPositioned { coords ->
                        // Measure the resting rect only while untransformed, so the graphicsLayer's
                        // own scale/translation can't feed back into the source rect.
                        if (state.progress.value > 0.999f && state.dragOffset == 0f) {
                            state.openScreenRect = coords.screenRect()
                        }
                    }
                    .graphicsLayer {
                        val f = 1f - state.progress.value
                        transformOrigin = TransformOrigin(0f, 0f)
                        val open = state.openScreenRect?.toLocal(state.overlayOrigin)
                        val anchor =
                            if (state.renderMorphIcon) state.overflowLocalRect
                            else state.openOriginLocalRect
                        if (open != null && anchor != null && open.width > 0f && open.height > 0f) {
                            scaleX = lerp(1f, anchor.width / open.width, f)
                            scaleY = lerp(1f, anchor.height / open.height, f)
                            translationX = f * (anchor.left - open.left)
                            translationY = f * (anchor.top - open.top) + state.dragOffset
                            clip = true
                            shape =
                                RoundedCornerShape(
                                    lerp(openCornerPx, minOf(anchor.width, anchor.height) / 2f, f)
                                )
                        } else {
                            translationY = state.dragOffset
                        }
                        alpha = state.surfaceAlpha()
                    }
                    .nestedScroll(dismissConnection)
                    .draggable(
                        state = chromeDrag,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> onRelease(velocity, false) },
                    ),
            content = {
                Box(modifier = Modifier.graphicsLayer { alpha = state.contentAlpha() }) {
                    content()
                }
            },
        )

        // The three-dots glyph the card becomes: fades in at the overflow button's slot during a
        // close, so the handoff to the real (restored) icon is seamless.
        if (state.renderMorphIcon) {
            Box(
                modifier =
                    Modifier.offset {
                            val r = state.overflowLocalRect
                            if (r != null) IntOffset(r.left.roundToInt(), r.top.roundToInt())
                            else IntOffset.Zero
                        }
                        .size(48.dp)
                        .graphicsLayer { alpha = state.morphIconAlpha() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Swipe-only "Release to close" cue, floating on the scrim above the dragged card.
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = DetailCardDefaults.ContentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = showReleaseHint,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = DetailCardDefaults.TonalElevation,
                ) {
                    Text(
                        text = stringResource(R.string.detail_release_to_close),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Metadata flag: tag a [NavEntry] with [detailOverlay] to present it via
 * [DetailOverlaySceneStrategy].
 */
internal object DetailOverlayKey : NavMetadataKey<Unit>

/** Entry metadata that opts a secondary screen into the custom dismissable overlay presentation. */
internal fun detailOverlay(): Map<String, Any> = metadata { put(DetailOverlayKey, Unit) }

/**
 * Presents the top entry as an in-composition container-transform card over the dimmed main screen,
 * when (and only when) it carries [detailOverlay] metadata — `AppNavigation` adds that only at
 * expanded width, so below the breakpoint this returns null and the entry falls through to the
 * single-pane push flow. Replaces the built-in `DialogSceneStrategy` (a separate platform `Dialog`
 * window) so the card can be swipe-dismissed, render inside the app's `testTagsAsResourceId` scope,
 * and own its open/close animation via [OverlayScene.onRemove].
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
            private lateinit var morphState: DetailMorphState

            override val content: @Composable () -> Unit = {
                val state = rememberDetailMorphState()
                morphState = state
                DetailOverlayContent(state = state, onDismissRequested = onBack) { entry.Content() }
            }

            override suspend fun onRemove() {
                // Discrete dismissals (scrim tap, close button) pop first and let the collapse play
                // here; gesture dismissals already animated progress to 0, so this is then a no-op.
                if (::morphState.isInitialized && morphState.progress.value != 0f) {
                    morphState.renderMorphIcon = true
                    morphState.progress.animateTo(0f, CloseSpec)
                }
            }
        }
    }
}
