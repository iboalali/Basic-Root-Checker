# Adaptive navigation and the detail overlay

Navigation 3 setup, and the container-transform overlay that hosts the secondary screens on large
screens. This is the most intricate part of the app; read the "Structural facts" list in
[`../CLAUDE.md`](../CLAUDE.md) before changing any of it.

## AppNavigation (`navigation/`)

Navigation 3 with `NavDisplay`, `@Serializable` route keys (`MainRoute`, `SettingsRoute`,
`AboutRoute`, `LicenseRoute`), and explicit back stack management.

A `navigateToDetail` helper keeps the back stack at `[Main, oneDetail]` — the three secondary screens
are interchangeable siblings reached only from the main screen — which is what makes
`overlaidEntries` deterministic.

## Two presentations, gated on window width

At the **expanded** width breakpoint (≥840dp: tablets, unfolded foldables in landscape, desktop
windows, XR panels) the secondary screens (Settings / About / License) open as a **dialog card over
the dimmed main screen** instead of a full-screen push.

This uses a **custom `OverlayScene` + `SceneStrategy`** (`navigation/DetailOverlayScene.kt`), **not**
the built-in `DialogSceneStrategy`. The custom overlay renders **in-composition** (inside `AppRoot`,
not a separate platform `Dialog` window), which is what lets it:

- be swipe-down-dismissed,
- fade its own scrim with the drag,
- draw a tightly-bounded card,
- own its exit animation via `OverlayScene.onRemove()`.

It's width-gated by **conditional metadata**: the three secondary entries get `detailOverlay()`
metadata **only when**

```kotlin
currentWindowAdaptiveInfoV2().windowSizeClass
    .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
```

The `entryProvider` re-runs on width changes, so this follows fold / unfold / resize live. Below the
breakpoint the entries carry no metadata and fall through to the single-pane push flow with the app's
own forward/back and predictive-back transitions.

## The overlay (`DetailOverlayScene.kt`)

The scene's `overlaidEntries` is `[MainRoute]`, kept live and dimmed below the card.
`DetailOverlayContent` hosts the screen in the shared `DetailCard`
(`navigation/DetailCard.kt` — scrim plus a centered rounded 640dp-max `Surface`).

`DetailCard` is a deliberately **stateless visual**: it owns no motion, taking a `scrimAlpha` lambda
and a `cardModifier`. That way the live overlay and the screenshot test (which renders the resting
open state with the defaults) share one composable, and the committed baseline stays representative.

### Motion: a container transform with exactly two drivers

`DetailMorphState` holds both.

**`progress`** (an `Animatable`; 1 = open/resting … 0 = collapsed into the current anchor rect) — the
single open/close driver. It interpolates:

- the card's rect (scale + translation via `graphicsLayer`),
- its corner radius,
- the scrim alpha,
- the content ↔ morph-icon cross-fade (`contentAlpha()` / `morphIconAlpha()` / `surfaceAlpha()`, each
  fading over a different slice of the range so the handoff lands bare).

**`dragOffset`** (px, downward-positive) — the transient elastic swipe-down pull, meaningful only
while open. `applyDrag` decelerates it toward the `MaxElasticPull` asymptote, so the card resists and
never slides off-screen. A release past threshold folds it into one `commitClose` (both decay on the
same spec, keeping the motion continuous); below threshold it springs back.

### Anchors (`navigation/DetailAnchors.kt`)

`DetailAnchorState` is provided once from `AppRoot` and holds **screen-space** rects — the only frame
the overlay (in-composition) and the overflow `DropdownMenu` (its own `Popup` window) can agree on.
The overlay converts them to its local space via its measured origin.

`MainScreen` reports the overflow `IconButton`'s rect continuously (`onGloballyPositioned`) and stores
the tapped menu item's rect at click time, **before the `Popup` tears down**. So **open grows from the
tapped menu item** (falling back to the overflow rect, then a centred scale-up) and **every close
collapses into the overflow icon**.

`overflowIconVisible` is held `false` for the overlay's whole lifetime and restored on dispose — which,
because `NavDisplay` awaits `onRemove()`, is exactly when the collapse finishes. So the card appears
to *become* the real icon, with the overlay drawing its own three-dots glyph at that slot to hand off
to.

### Swipe-to-dismiss

Driven two ways:

1. A `nestedScroll` connection — an inverted twin of Material's bottom-sheet
   `ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection`, so the pull only engages once the
   screen's inner `verticalScroll` and collapsing toolbar are exhausted at the top.
2. `draggable`, for direct drags on the card chrome.

The connection tracks whether the current gesture ever moved the list (`scrolled`). If it did, an
over-pull past the top only peeks the card down by `ScrollEdgeHintTravel` and always springs back —
**dismissing requires a fresh swipe-down that starts at the top**, so the gesture that was scrolling
can't close the screen.

Crossing `DismissDragThreshold` plays one haptic tick and shows the "Release to close" pill.

### Predictive back

Handled by the overlay's own `PredictiveBackHandler`, **not** `NavDisplay`: since this isn't a platform
`Dialog`, without it the gesture would fall through and finish the Activity.

It previews at most `BackMaxCollapseFraction` (0.12) of the collapse as you pull — a restrained peek,
so a back-swipe doesn't fling the card across the screen — then `commitClose` finishes the rest on
release, or `cancelClose` returns it.

The settle animations run in the composable's `rememberCoroutineScope`, **not** the gesture's own
coroutine, so a canceled gesture can still finish its spring-back.

### All four dismiss paths funnel through `requestPop`

Scrim tap, close button, predictive/system back, and drag-or-fling past threshold. `requestPop` is
**idempotent**, so a double-dismiss can't double-pop.

For the *discrete* paths (scrim tap, close button) the pop happens first and `OverlayScene.onRemove()`
plays the collapse. The *gesture* paths already animated `progress` to 0, so `onRemove()` is then a
no-op.

### Performance and measurement subtleties

**All hot values are read inside deferred `graphicsLayer {}` / `offset {}` / `drawBehind {}`
lambdas**, so animating them never recomposes the card subtree.

The resting card rect is captured **only while untransformed** (`progress > 0.999f && dragOffset == 0f`),
so the `graphicsLayer`'s own scale/translation can't feed back into the source rect. `revealed` holds
the whole overlay at alpha 0 for that single measurement frame before the open animation starts.

## The leading icon

Chosen by `LocalDetailNavIcon` (`navigation/DetailNavIcon.kt`): provided as `CLOSE` (an X) at expanded
width — and inherited into the overlay content, since it composes within `NavDisplay`'s tree — and
`BACK` (up-arrow) otherwise. Both call the same `onNavigateBack`.

## Baseline-profile interaction

Because the overlay renders **in-composition inside `AppRoot`** (unlike a platform `Dialog` window),
the secondary screens' `*_list` testTags stay inside the `testTagsAsResourceId` scope — so `By.res(...)`
finds them on a large-screen journey leg with no re-enabling needed. The overflow-`DropdownMenu` trap
does **not** apply here.

The shipped profile does not currently cover this path — see the large-screen gap noted in
[`../CLAUDE.md`](../CLAUDE.md).
