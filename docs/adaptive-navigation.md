# Adaptive navigation and the detail overlay

Navigation 3 setup, and the container-transform overlay that hosts the secondary screens on large screens. This is the most intricate part of the app. Read the "Structural facts" list in [`../CLAUDE.md`](../CLAUDE.md) before changing any of it.

**The overlay lives in `com.iboalali.nav3:overlay`** ([`iboalali/Android-Shared`](https://github.com/iboalali/Android-Shared)): `DetailOverlayScene`, `DetailCard`, `DetailNavIcon` and `DetailAnchors`. Billboard uses the same library. This repo keeps `navigation/AppNavigation.kt`: this app's nav host, its routes, its width gate and its `DetailOverlayStyle`. Everything below describes how the overlay behaves. Change it in the library, not here, and expect the change to reach Billboard too.

## AppNavigation (`navigation/`)

Navigation 3 with `NavDisplay`, `@Serializable` route keys (`MainRoute`, `SettingsRoute`, `AboutRoute`, `LicenseRoute`), and explicit back stack management.

`navigateToDetail` keeps the back stack at `[Main, oneDetail]`. The three secondary screens are interchangeable siblings reached only from the main screen, which makes `overlaidEntries` deterministic.

## Two presentations, gated on window width

At the **expanded** width breakpoint (840dp and up: tablets, unfolded foldables in landscape, desktop windows, XR panels) the secondary screens (Settings / About / License) open as a **dialog card over the dimmed main screen** instead of a full-screen push.

This is a **custom `OverlayScene` + `SceneStrategy`** (`com.iboalali.nav3.overlay`), **not** the built-in `DialogSceneStrategy`. It renders **in-composition** (inside `AppRoot`, not in a separate platform `Dialog` window), which lets it:

- be dismissed by swiping down,
- fade its own scrim with the drag,
- draw a tightly bounded card,
- own its exit animation via `OverlayScene.onRemove()`.

It is width-gated by **conditional metadata**: the three secondary entries get `detailOverlay()` metadata **only when**

```kotlin
currentWindowAdaptiveInfoV2().windowSizeClass
    .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
```

The `entryProvider` re-runs on width changes, so this follows fold, unfold and resize live. Below the breakpoint the entries carry no metadata and fall through to the single-pane push flow with the app's own forward, back and predictive-back transitions.

## The overlay (`DetailOverlayScene.kt`, in the library)

The scene's `overlaidEntries` is every previous entry. For this app's `[Main, oneDetail]` stack that is `[MainRoute]`, kept live and dimmed below the card. `DetailOverlayContent` hosts the screen in the shared `DetailCard` (scrim plus a centered rounded `Surface`, 640dp max).

The scene is a **named class with value-based `equals`/`hashCode`**, not an anonymous `object`. `NavDisplay` compares scenes by equality to decide whether a recomputed scene is the same one. Under reference equality every recomposition looks like a new scene, which breaks its transition bookkeeping and can compose the entry twice at once. Nothing fails visibly when this is wrong, so keep it a named class.

`DetailCard` is a deliberately **stateless visual**. It owns no motion and takes a `scrimAlpha` lambda and a `cardModifier`. So the live overlay and the screenshot test (which renders the resting open state with the defaults) share one composable, and the committed baseline stays representative.

### Motion: a container transform with exactly two drivers

`DetailMorphState` holds both.

**`progress`** is the single open/close driver: an `Animatable` where 1 is open and resting and 0 is collapsed into the current anchor rect. It interpolates:

- the card's rect (scale + translation via `graphicsLayer`),
- its corner radius,
- the scrim alpha,
- the cross-fade between content and morph icon (`contentAlpha()` / `morphIconAlpha()` / `surfaceAlpha()`). Each fades over a different slice of the range so the handoff lands bare.

**`dragOffset`** (px, positive downward) is the transient elastic swipe-down pull, meaningful only while open. `applyDrag` decelerates it toward the `MaxElasticPull` asymptote, so the card resists and never slides off-screen. A release past the threshold folds it into one `commitClose` (both decay on the same spec, so the motion stays continuous). Below the threshold it springs back.

### Anchors (`DetailAnchors.kt`, in the library)

`DetailAnchorState` is provided once from `AppRoot` and holds **screen-space** rects. Screen space is the only frame that the overlay (in-composition) and the overflow `DropdownMenu` (its own `Popup` window) can agree on. The overlay converts the rects to its local space via its measured origin.

`MainScreen` reports the overflow `IconButton`'s rect continuously (`onGloballyPositioned`) and stores the tapped menu item's rect at click time, **before the `Popup` tears down**. So **opening grows from the tapped menu item** (falling back to the overflow rect, then a centered scale-up) and **every close collapses into the overflow icon**.

`overflowIconVisible` is held `false` for the overlay's whole lifetime and restored on dispose. Because `NavDisplay` awaits `onRemove()`, dispose is exactly when the collapse finishes. So the card appears to *become* the real icon, with the overlay drawing its own three-dots glyph at that slot to hand off to.

### Swipe-to-dismiss

Driven two ways:

1. A `nestedScroll` connection, an inverted twin of Material's bottom-sheet `ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection`. The pull only engages once the screen's inner `verticalScroll` and collapsing toolbar are exhausted at the top.
2. `draggable`, for direct drags on the card chrome.

The connection tracks whether the current gesture ever moved the list (`scrolled`). If it did, an over-pull past the top only peeks the card down by `ScrollEdgeHintTravel` and always springs back. **Dismissing requires a fresh swipe-down that starts at the top**, so a gesture that was scrolling can't close the screen.

Crossing `DismissDragThreshold` plays one haptic tick and shows the "Release to close" pill.

### Predictive back

Handled by the overlay's own `PredictiveBackHandler`, **not** `NavDisplay`. Since this isn't a platform `Dialog`, without it the gesture would fall through and finish the Activity.

It previews at most `BackMaxCollapseFraction` (0.12) of the collapse as you pull, a restrained peek so a back-swipe doesn't fling the card across the screen. On release `commitClose` finishes the rest, or `cancelClose` returns it.

The settle animations run in the composable's `rememberCoroutineScope`, **not** the gesture's own coroutine, so a canceled gesture can still finish its spring-back.

### All four dismiss paths funnel through `requestPop`

Scrim tap, close button, predictive/system back, and drag or fling past the threshold. `requestPop` is **idempotent**, so a double dismiss can't double-pop.

For the *discrete* paths (scrim tap, close button) the pop happens first and `OverlayScene.onRemove()` plays the collapse. The *gesture* paths have already animated `progress` to 0, so `onRemove()` is then a no-op.

### Performance and measurement subtleties

**All hot values are read inside deferred `graphicsLayer {}` / `offset {}` / `drawBehind {}` lambdas**, so animating them never recomposes the card subtree.

The resting card rect is captured **only while untransformed** (`progress > 0.999f && dragOffset == 0f`), so the `graphicsLayer`'s own scale and translation can't feed back into the source rect. `revealed` holds the whole overlay at alpha 0 for that single measurement frame before the open animation starts.

### Accessibility

The overlay root carries `semantics { isTraversalGroup = true }`, so a screen reader reads scrim plus card as one unit ahead of the dimmed screen behind it. This is **not** a platform `Dialog` window (the container-transform morph rules that out), so the dimmed screen stays in the accessibility tree. The scrim carries an explicit dismiss action (`onClickLabel`) and remains the touch boundary.

## The leading icon

Chosen by `LocalDetailNavIcon` (`DetailNavIcon.kt`, in the library): `CLOSE` (an X) at expanded width, inherited into the overlay content because it composes within `NavDisplay`'s tree, and `BACK` (up arrow) otherwise. Both call the same `onNavigateBack`.

Draw it with the library's **`DetailNavigationIcon(onBack = onNavigateBack)`**, not a hand-rolled `IconButton`. It picks the glyph and content description from the style and wraps the tap in `rememberHapticClick` itself, so pass `onNavigateBack` unwrapped.

## `DetailOverlayStyle`: where this app's resources are named

`AppNavigation.kt` provides one `DetailOverlayStyle` through `LocalDetailOverlayStyle`, holding seven **resource ids**: the overflow glyph, the back and close icons, and the release-to-close, scrim dismiss, navigate-up and close strings. The library ships its own prefixed defaults so a new consumer works without declaring one. This app overrides all seven to keep its own rendering and wording.

`overflowIcon` must stay the same drawable that `MainScreen`'s overflow icon button draws. The closing card morphs into that slot and hands off to the real glyph as it restores, so a mismatch shows as a flicker at the end of every dismiss.

**There is deliberately no `Modifier.detailDialogShape()`.** A screen presented in the card must not round its own corners. `DetailCard`'s `Surface` clips at `DetailCardDefaults.CornerRadius`, and the close transform's `graphicsLayer` clips again at the same radius while animating. A third clip multiplies the antialiased coverage (α² instead of α) and erodes the arc by a fraction of a pixel, which is enough to break screenshot baselines.

## Baseline Profile interaction

Because the overlay renders **in-composition inside `AppRoot`** (unlike a platform `Dialog` window), the secondary screens' `*_list` testTags stay inside the `testTagsAsResourceId` scope. So `By.res(...)` finds them on a large-screen journey leg without re-enabling anything. The overflow-`DropdownMenu` trap does **not** apply here.

The shipped profile covers this path. How, and how to keep it covered, is in the large-screen Baseline Profile section of [`../CLAUDE.md`](../CLAUDE.md).
