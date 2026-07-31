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

**GitHub** — `github.com/iboalali/Basic-Root-Checker`, default branch `master`. Use `gh`; the `glab`
note in the global instructions does not apply here.

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
`telemetry-and-tql`, `agp9-screenshot-tests`, `baseline-profiles`, `appfunctions-wiring`,
`compose-a11y-checklist`, `haptics-conventions`.

Cross-project state — what's in flight across all my Android repos, and open items that affect this one
— lives in the kit's `TODO.md` (`~/StudioProjects/ai-kit/TODO.md`,
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
./scripts/generate-store-screenshots.sh [output-subfolder]
```

Screenshot tests run on **`gplayDebug`** — these screens are flavor-independent, and flavor-specific
bits like the tip jar are passed in as plain arguments. Prefer the Android Studio gutter icon to
regenerate a single preview; the Gradle task is variant-level and rewrites every reference.

## Structural facts to know before changing navigation or the tip flow

1. **The large-screen overlay is a custom `OverlayScene` + `SceneStrategy`, not
   `DialogSceneStrategy`.** It renders **in-composition** inside `AppRoot`, which is what allows
   swipe-down dismissal, a drag-linked scrim, a tightly-bounded card, and its own exit animation. Don't
   "simplify" it back to a platform `Dialog` — that breaks all four, plus predictive back and the
   testTag scope. → [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)
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
  null, `queryIntentActivities` is unimplemented; guard both (see `DeviceInfo.getAppVersionName`,
  `OtherAppsCard.findInstalledPwaPackage`) and gate entrance animations on `LocalInspectionMode`. One
  crash fails the whole screenshot run. → `agp9-screenshot-tests`
- **WebAPK detection matches the shared shell *activity class*** (`org.chromium.webapk.shell_apk.`),
  not a package name — WebAPK packages differ per browser, and an unverified WebAPK isn't picked up by
  `ACTION_VIEW` routing. Needs the `<intent>` entries in the manifest's `<queries>`. →
  [`docs/architecture.md`](docs/architecture.md)
- **Catalog ETag/Last-Modified validators are stored with the URL they came from** and only replayed
  against that same URL — otherwise the English fallback can produce a `304` against a localized file
  we don't hold. → [`docs/architecture.md`](docs/architecture.md)
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

## Known gap: large-screen Baseline Profile coverage

Generation runs on the connected phone and the journey never crosses the 840dp breakpoint, so the
shipped profile covers the single-pane push flow but **omits the expanded-width overlay path**. Cold
start to `MainScreen` is form-factor-independent and already covered; the only missing piece is the
first open of a secondary screen on a large window — a one-time JIT cost instead of AOT. This is
optional polish, not correctness.

To close it, add a tablet/foldable Gradle Managed Device to the `baselineProfile { }` block (keeping
`useConnectedDevices = true`); the plugin unions the rules from every device into the same
`baseline-prof.txt`. No journey change is needed, and because the overlay renders in-composition the
`*_list` testTags stay reachable. Revisit when tablets/foldables become a meaningful share of users. →
`baseline-profiles`
