# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

It is deliberately an **index**: the rules that are easy to miss, plus pointers to the doc that owns
each topic. When you learn something worth writing down, put it in the owning doc and link it here —
don't grow this file. If it applies to more than one of my apps, it belongs in the shared kit
(`iboalali-apps` / `android-base` plugins), not here.

## Project overview

Basic Root Checker tells the user whether their device has root access, and which provider grants it
(Magisk / KernelSU / APatch). One screen does the work: device info plus a status area, with a FAB that
runs the check. Settings, About, and Licences are secondary screens reachable from an overflow menu.

The same checks are exposed to the system and to on-device agents through AppFunctions, so an assistant
can answer "am I rooted?" without opening the app.

Two flavors: `gplay` (tip jar, in-app updates, in-app review) and `foss` (no Google services, all three
no-ops).

## Repository hosting

**GitHub** — `github.com/iboalali/Basic-Root-Checker`, default branch `main` (renamed from `master`
2026-07-31). Use `gh`; the `glab` note in the global instructions does not apply here.

## Definition of done

Covered by the shared `definition-of-done` skill — changelog, release notes, screenshot test, Baseline
Profile journey, accessibility, haptics, store assets. Don't restate it here.

App-specific additions to that list:

- Anything touching the root-detection probes must be verified on a **real rooted device**; the
  hardware-dependent paths are not unit-tested.
- Anything touching an `@AppFunction` must be verified with `adb` on API 36+ — a green build proves
  nothing here (see Traps).

## Documentation map

| Topic | Doc |
|---|---|
| Components, data layer, monetization, build config, theming, tests | [`docs/architecture.md`](docs/architecture.md) |
| Navigation 3, the large-screen detail overlay, its motion and gestures | [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md) |
| Signal taxonomy and query cookbook | [`docs/telemetry-optimization.md`](docs/telemetry-optimization.md) |
| Known root-provider detection gaps | [`docs/root-provider-detection-gaps.md`](docs/root-provider-detection-gaps.md) |
| Per-device haptic capability data | [`docs/haptic-capability-queries.json`](docs/haptic-capability-queries.json) |
| Translation notes | [`docs/russian-translation-notes.md`](docs/russian-translation-notes.md), [`docs/spanish-translation-notes.md`](docs/spanish-translation-notes.md) |

Shared conventions live in the kit rather than here: `definition-of-done`, `play-store-assets`,
`telemetry-instrumentation`, `telemetry-and-tql`, `agp9-screenshot-tests`, `baseline-profiles`, `appfunctions-wiring`,
`compose-a11y-checklist`, `haptics-conventions`.

Cross-project state — what's in flight across all my Android repos, and open items that affect this one
— lives in the kit's `TODO.md` (`~/Projects/ai-kit/TODO.md`,
[Personal-AI-KIT](https://github.com/iboalali/Personal-AI-KIT)). **It currently flags a suspected
AppFunctions bug in this app** that needs on-device verification.

## Build commands

```bash
./gradlew assembleDebug            # or installDebug / assembleRelease / clean / lint
./gradlew :app:testGplayDebugUnitTest

# Baseline Profile: generate both flavors on a connected device, then prove it moved the numbers
./gradlew :app:generateBaselineProfile
./gradlew :baselineprofile:connectedGplayBenchmarkReleaseAndroidTest

# Screenshot tests (JVM/Layoutlib, no device)
./gradlew :app:updateGplayDebugScreenshotTest      # rewrite reference images
./gradlew :app:validateGplayDebugScreenshotTest    # fail on any visual diff

# Clean, upload-ready Play Store screenshots (all screens × locales × store devices)
./scripts/generate-store-screenshots.sh [VERSION] [--dry-run]
```

Screenshot tests run on **`gplayDebug`** — these screens are flavor-independent, and flavor-specific
bits like the tip jar are passed in as plain arguments. Prefer the Android Studio gutter icon to
regenerate a single preview; the Gradle task is variant-level and rewrites every reference.

## Structural facts to know before changing navigation or the tip flow

1. **The large-screen overlay is a custom `OverlayScene` + `SceneStrategy`, not
   `DialogSceneStrategy`, and it lives in `com.iboalali.nav3:overlay`, not this repo.** It renders
   **in-composition** inside `AppRoot`, which is what allows swipe-down dismissal, a drag-linked scrim,
   a tightly-bounded card, and its own exit animation. Don't "simplify" it back to a platform `Dialog`
   — that breaks all four, plus predictive back and the testTag scope. Changing it changes Billboard
   too. → [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)
2. **It's width-gated by conditional metadata**, not a branch in the screen: the secondary entries get
   `detailOverlay()` metadata only at ≥840dp, and the `entryProvider` re-runs on width change so it
   follows fold/unfold live.
3. **`navigateToDetail` keeps the back stack at `[Main, oneDetail]`.** The three secondary screens are
   interchangeable siblings reached only from the main screen, which is what makes `overlaidEntries`
   deterministic.
4. **`AppRoot` owns both tip flows, and must.** `BillingController.events` is a `Channel`, so it's
   **single-consumer** — two collectors *split* events rather than duplicating them. A tip can start
   from Settings or from the main screen's support card, and at expanded width both surfaces are
   composed at once. → [`docs/architecture.md`](docs/architecture.md)
5. **The two post-check asks are serialized, review first.** `SupportGate.MIN_ROOTED_CHECKS` (5) sits
   above `ReviewGate.MIN_ROOTED_CHECKS` (3), and a `@Volatile` session flag keeps them out of the same
   session. Don't reorder or loosen either without reading why.

## Stack

Kotlin 2.4.10 · Java 17 · minSdk 23 · compile/target SDK 37 · AGP 9.3.1 (built-in Kotlin) ·
Compose BOM · Navigation 3 · Material 3 Adaptive · `androidx.appfunctions` 1.0.0-alpha10 + KSP ·
Coil 3 · TelemetryDeck.

`gradle/libs.versions.toml` is **the source of truth for every version** — check there rather than
trusting the numbers above.

## Shared code — this repo does not build alone

`settings.gradle.kts` runs a **Gradle composite build**: `includeBuild` on
[`iboalali/Android-Shared`](https://github.com/iboalali/Android-Shared) (private), defaulting to the
sibling path `../Android-Shared` and overridable with the `androidShared.path` Gradle property. The
About screen's "Other apps" card comes from `com.iboalali.appcatalog:data` (feed loading, HTTP cache,
bundled `assets/apps*.json`) and `:ui` (the shared row). Gradle substitutes those coordinates by
`group:name`, so **the version in the coordinate is ignored** — don't bump it expecting an effect.

**A fresh clone needs the sibling checkout**, and that repo needs its own gitignored `local.properties`
with `sdk.dir`. Missing either fails at configuration time in a way that reads like a broken build
file rather than an absent prerequisite.

`com.iboalali.telemetry:core` comes from the same build and owns the **TelemetryDeck lifecycle**: the
startup signal buffer, automated-test-traffic detection, the identity reset, and the start-before-flush
ordering. `analytics/Analytics.kt` keeps only this app's signal vocabulary and delegates the rest.
`analytics/SignalGate.kt` and `util/TestEnvironment.kt` are gone — don't reintroduce either.

Two things this app keeps rather than takes:

- **Its card chrome.** `ui/about/OtherAppsCard.kt` is the outlined card, its title and its dividers;
  the rows inside it are the library's, with `contentPadding` as the only style override because the
  card already pads horizontally.
- **Its own four action strings.** App resources beat library resources of the same name, which is the
  intended override path — the library ships the same keys in the same five locales as a fallback for
  an app that hasn't got them.

Much of what used to be here now lives there, and this app contributed most of it: the OkHttp
`CatalogHttpSource`, its conditional-GET tests, and the seed/fetch race guard were all written in this
repo before the move. Look for them in `Android-Shared`, not in `data/catalog/`, which is gone.

**The screenshot matrices are `com.iboalali.previews:matrix` now**, not
`util/PreviewPlayStoreNative.kt` and `util/PreviewConstrainedDevices.kt`. This app won the naming
(`PreviewPlayStore*`); Billboard renamed onto it. Declared `implementation` rather than
`screenshotTestImplementation` because `util/ConstrainedDevicePreviews.kt` — the preview *functions* —
sits in `src/main`. That also means those preview functions ship in the release APK (12
`ConstrainedDevicePreviews` entries in `mapping.txt`); moving them to `screenshotTest` would fix both.
`util/PreviewPlayStoreListing.kt` stays: it is the older **dp**-based matrix, unusable for store
uploads but fine for browsing previews in the IDE, and still used 3×.

**The large-screen overlay is `com.iboalali.nav3:overlay` now, not `navigation/`.** `DetailOverlayScene`,
`DetailCard`, `DetailNavIcon` and `DetailAnchors` are gone from this repo; `AppNavigation.kt` stays,
because it is this app's own nav host. **Billboard was the donor here, not this app**, and adopting
its version brought two fixes this copy never had:

- **`semantics { isTraversalGroup = true }` on the overlay root**, so a screen reader reads scrim +
  card as one unit ahead of the dimmed main screen. This copy had a bare `Box(Modifier.fillMaxSize())`.
- **A named scene class with value-based `equals`/`hashCode`.** This copy returned an anonymous
  `object : OverlayScene<NavKey>`, so every recomposition looked like a new scene to `NavDisplay` —
  which is what its transition bookkeeping uses to decide whether to compose the entry again. Nothing
  failed visibly, which is exactly why it survived.

What this app contributes back is the deletion of `Modifier.detailDialogShape()` — the screen-level
32dp clip left over from the `DialogSceneStrategy` era, which this repo had already removed as
redundant and Billboard had not. `AppNavigation` provides a `DetailOverlayStyle` naming this app's
seven resources, so the library ships none of them.

**The overflow menu is `com.iboalali.ui:menu` now, not `ui/components/AppBarDropdownMenuItem.kt`.**
This adoption *changed behaviour here*, and in this app's favour: the shared `AppBarDropdownMenuItem`
routes its `onClick` through `rememberHapticClick`, and this repo's local copy did not — its menu
items were the only silent tap targets across the three apps. The five call sites in `MainScreen`
consequently **dropped their own `rememberHapticClick` wrappers**, which would otherwise now tick
twice. `MainScreen`'s bare `DropdownMenu` became `HapticDropdownMenu`, which sets
`testTagsAsResourceId` itself; the 16dp corner this app wants is a parameter and is still passed.

## Traps that cost real time

Each of these has been paid for once. One line to recognise it; the detail is in the linked doc or skill.

- **An AppFunction can be completely dead while the build is green.** Compilation, unit tests, and
  Play's upload validator all pass over a broken surface. Verify with
  `adb shell cmd app_function execute-app-function`. → `appfunctions-wiring`
- **The `AppFunctionContext` parameter is deliberately absent from all three functions — don't add it
  back.** On the `@AppFunctionServiceEntryPoint` path it throws
  `null cannot be cast to non-null type AppFunctionContext` on *every* call, because KSP omits it from
  the inventory the dispatcher builds its parameter map from. v2.5 declared it and broke all three
  functions; v2.4 had worked, so the app's own "verified with adb" note was true and stale at once. The
  adapters pass `applicationContext` instead. `BaseRootAppFunctionService`'s KDoc explains the
  mechanism — leave that comment in place. → `appfunctions-wiring`
- **R8 vertically merges `BaseRootAppFunctionService` into the generated subclass**, leaving the
  generated XML pointing at a class absent from the release DEX — Play rejects the upload with *"The
  Android App Functions XML could not be parsed from the binary."* The keep rule in
  `proguard-rules.pro` prevents it; verify with
  `grep BaseRootAppFunctionService app/build/outputs/mapping/<variant>/mapping.txt`. →
  `appfunctions-wiring`
- **`testTagsAsResourceId` must be re-enabled on the overflow `DropdownMenu`** — it renders in its own
  `Popup` window, outside the `AppRoot` scope. Forgetting it fails *silently*: the journey can't find
  the menu items, so the secondary screens never get profiled. → `baseline-profiles`
- **The support card is unreviewable in a debug build.** The `.debug` applicationId means Play Billing
  never returns tip products, so `productsLoaded` can't become true. Use the debug-gated **"Demo:
  support card"** overflow item (`demoSupportPrompt()`). → [`docs/architecture.md`](docs/architecture.md)
- **A `true` from `requestReview()` means "handed to Play", never "a card appeared."** Play gives no
  "was it shown" callback. The version code — the release's single prompt — is spent only once the
  request actually reached Play. → [`docs/architecture.md`](docs/architecture.md)
- **Layoutlib's `Context` is a stub and never advances `LaunchedEffect`.** `getPackageInfo` returns
  null, `queryIntentActivities` is unimplemented; guard both (see `DeviceInfo.getAppVersionName`, and
  `findInstalledPwaPackage` — which now lives in `com.iboalali.appcatalog:ui`, not this repo) and gate
  entrance animations on `LocalInspectionMode`. One crash fails the whole screenshot run. →
  `agp9-screenshot-tests`
- **WebAPK detection matches the shared shell *activity class*** (`org.chromium.webapk.shell_apk.`),
  not a package name — WebAPK packages differ per browser, and an unverified WebAPK isn't picked up by
  `ACTION_VIEW` routing. Needs the `<intent>` entries in the manifest's `<queries>`. →
  [`docs/architecture.md`](docs/architecture.md)
- **The catalog's traps moved out of this repo with its code.** The per-URL validator rule (replay an
  `ETag`/`Last-Modified` only against the URL it came from, or the English fallback can produce a `304`
  against a localized file we don't hold) is now OkHttp's `Cache` enforcing it inside the library, not
  hand-written code here. Read them in `Android-Shared`'s `CLAUDE.md` before touching
  `AppCatalogRepository`. → [`iboalali/Android-Shared`](https://github.com/iboalali/Android-Shared)
- **The committed screenshot references intentionally show "(Debug)".** The store export is a separate
  script that neutralizes the debug name, renders, copies out, then restores both the strings and the
  regression baseline. → `play-store-assets`
- **`trackDeviceType` is one-shot per process.** It's read through rotations, folds, and resizes, which
  would otherwise inflate the count. → [`docs/architecture.md`](docs/architecture.md)
- **Haptics deliberately skip `createPredefined`** and `View.performHapticFeedback` — several OEMs drop
  those silently while returning success. → `haptics-conventions`
- **The overlay's resting card rect is captured only while untransformed**
  (`progress > 0.999f && dragOffset == 0f`), or the `graphicsLayer`'s own scale feeds back into the
  source rect. → [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)

## Large-screen Baseline Profile coverage — covered, and no longer dependent on what's plugged in

The shipped profile **does** cover the expanded-width overlay path. It was regenerated 2026-07-31 with
two devices connected at once — SM-G766B (phone, 384dp) and SM-X356B (Galaxy Tab Active5 Pro, in
landscape at 1280dp) — and the plugin unioned both into the same `baseline-prof.txt`: `DetailOverlay`
rules went 0 → 135, `OverlayScene` 2 → 138, `PredictiveBack` 0 → 37, including the `HPL` morph rules.
No journey change was needed; the overlay renders in-composition, so the `*_list` testTags stay
reachable.

**Why the `tabletApi36` GMD exists:** with only `useConnectedDevices = true`, that coverage was a
property of whatever happened to be plugged in, not of the build — regenerating on a phone alone
silently drops those ~1,150 rules while the build stays green. The GMD makes it reproducible. It was
trial-run 2026-07-31 and emits the overlay rules *identically* to the physical tablet (138 / 135 / 37),
so it is a real substitute, not an approximation.

Three things about that device were each verified on the booted emulator rather than assumed, and are
each the reason for a specific line:

- **Pixel C, not Pixel Tablet.** Measured 2560×1800 @ 320dpi = **1280 × 900dp**, so it clears 840dp in
  *both* orientations and boot rotation can't silently sabotage it. Pixel Tablet, Medium Tablet and
  Nexus 10 are all 800dp in portrait — just under. The physical Tab Active5 Pro has the same trap and
  only works because it sits in landscape.
- **`systemImageSource = "google"`, not `"aosp"`.** `adb root` succeeds on `google_apis`, so profile
  capture works; only `*_playstore` images block it. No `aosp` image is installed at any API level, so
  `"aosp"` would cost a large download for no benefit.
- **API 36 with `testedAbi = "x86_64"`.** Matches the physical test devices, is already on disk, and
  pinning the ABI silences an AGP warning and stops the choice drifting. → `baseline-profiles`
