# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

It is deliberately an **index**: the rules that are easy to miss, plus pointers to the doc that owns each topic. When you learn something worth writing down, put it in the owning doc and link it here. Don't grow this file. If it applies to more than one of my apps, it belongs in the shared kit (`iboalali-apps` / `android-base` plugins), not here.

## Project overview

Basic Root Checker tells the user whether their device has root access, and which provider grants it (Magisk / KernelSU / APatch). One screen does the work: device info plus a status area, with a FAB that runs the check. Settings, About, and Licences are secondary screens reachable from an overflow menu.

The same checks are exposed to the system and to on-device agents through AppFunctions, so an assistant can answer "am I rooted?" without opening the app.

Two flavors: `gplay` (tip jar, in-app updates, in-app review) and `foss` (no Google services, all three no-ops).

## Repository hosting

**GitHub**: `github.com/iboalali/Basic-Root-Checker`, default branch `main`. Use `gh`; the `glab` note in the global instructions does not apply here.

## Definition of done

Covered by the shared `definition-of-done` skill (changelog, release notes, screenshot test, Baseline Profile journey, accessibility, haptics, store assets). Don't restate it here.

App-specific additions to that list:

- Anything touching the root-detection probes needs an emulator run, and only the granted case needs a rooted one. A stock image covers three of the four `RootResult` states and **every provider and manager the app names**: the only signal that yields `KERNELSU` or `APATCH` is an installed package id, so a manifest-only stub APK per id drives the real probe. All 17 ids in `PACKAGE_MANAGERS` were confirmed that way on API 36 (2026-08-27). That is the check worth repeating, because a package missing from the manifest's `<queries>` fails *silently* and looks like a device with no root manager. `Rooted` needs a real provider (Magisk via `rootAVD`). No `su` you write yourself will do, because an app process has an empty capability bounding set and so cannot complete `setuid(0)`. Recipes, the debug-FAB trap that tests the demo picker instead of the detector, and the `libsu` per-process caching trap that makes a mid-session state change look like a detection bug are in [`docs/root-provider-detection-gaps.md`](docs/root-provider-detection-gaps.md).
- Anything touching an `@AppFunction` must be verified with `adb` on API 36+. A green build proves nothing here (see Traps).

## Documentation map

| Topic | Doc |
|---|---|
| Components, data layer, monetization, build config, theming, tests | [`docs/architecture.md`](docs/architecture.md) |
| Navigation 3, the large-screen detail overlay, its motion and gestures | [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md) |
| Signal taxonomy and query cookbook | [`docs/telemetry-optimization.md`](docs/telemetry-optimization.md) |
| The TelemetryDeck dashboard's panels, as paste-ready TQL | [`docs/telemetry-dashboard-queries.json`](docs/telemetry-dashboard-queries.json) |
| Root-provider detection coverage, gaps and emulator recipes | [`docs/root-provider-detection-gaps.md`](docs/root-provider-detection-gaps.md) |
| Open bugs from code review, with fix directions | [`docs/known-bugs.md`](docs/known-bugs.md) |
| Per-device haptic capability data | [`docs/haptic-capability-queries.json`](docs/haptic-capability-queries.json) |
| Every place a new language has to be named | [`docs/adding-a-locale.md`](docs/adding-a-locale.md) |
| Translation notes | [`docs/russian-translation-notes.md`](docs/russian-translation-notes.md), [`docs/spanish-translation-notes.md`](docs/spanish-translation-notes.md), [`docs/dutch-translation-notes.md`](docs/dutch-translation-notes.md), [`docs/chinese-translation-notes.md`](docs/chinese-translation-notes.md), [`docs/malay-translation-notes.md`](docs/malay-translation-notes.md), [`docs/tamil-translation-notes.md`](docs/tamil-translation-notes.md) |

Shared conventions live in the kit rather than here: `definition-of-done`, `play-store-assets`, `telemetry-instrumentation`, `telemetry-and-tql`, `agp9-screenshot-tests`, `baseline-profiles`, `appfunctions-wiring`, `compose-a11y-checklist`, `haptics-conventions`.

Cross-project state (what's in flight across all my Android repos, and open items that affect this one) lives in the kit's `TODO.md` (`~/Projects/ai-kit/TODO.md`, [Personal-AI-KIT](https://github.com/iboalali/Personal-AI-KIT)). **It currently flags a suspected AppFunctions bug in this app** that needs on-device verification.

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

Screenshot tests run on **`gplayDebug`**. These screens are flavor-independent, and flavor-specific bits like the tip jar are passed in as plain arguments. Prefer the Android Studio gutter icon to regenerate a single preview; the Gradle task is variant-level and rewrites every reference.

## Structural facts to know before changing navigation or the tip flow

1. **The large-screen overlay is a custom `OverlayScene` + `SceneStrategy`, not `DialogSceneStrategy`, and it lives in `com.iboalali.nav3:overlay`, not this repo.** It renders **in-composition** inside `AppRoot`, which is what allows swipe-down dismissal, a drag-linked scrim, a tightly bounded card, and its own exit animation. Don't "simplify" it to a platform `Dialog`: that breaks all four, plus predictive back and the testTag scope. Changing it changes Billboard too. → [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)
2. **It's width-gated by conditional metadata**, not a branch in the screen: the secondary entries get `detailOverlay()` metadata only at 840dp and up, and the `entryProvider` re-runs on width change so it follows fold/unfold live.
3. **`navigateToDetail` keeps the back stack at `[Main, oneDetail]`.** The three secondary screens are interchangeable siblings reached only from the main screen, which makes `overlaidEntries` deterministic.
4. **`AppRoot` owns both tip flows, and must.** `BillingController.events` is a `Channel`, so it's **single-consumer**: two collectors *split* events rather than duplicating them. A tip can start from Settings or from the main screen's support card, and at expanded width both surfaces are composed at once. → [`docs/architecture.md`](docs/architecture.md)
5. **The two post-check asks are serialized, review first, and they count different things.** `SupportGate.MIN_CHECKS` (5) counts every check; `ReviewGate.MIN_ROOTED_CHECKS` (3) counts only the root-found ones. 5 above 3 orders them on a device that keeps reporting root, and a `@Volatile` session flag keeps them out of the same session. Don't reorder or loosen either without reading why.

## Stack

**Kotlin** · Java 17 · minSdk 24 · compile/target SDK 37 · **AGP** (built-in Kotlin) · Compose BOM · Navigation 3 · Material 3 Adaptive · `androidx.appfunctions` + KSP · Coil 3 · TelemetryDeck.

`gradle/libs.versions.toml` is **the source of truth for every version**, and each entry carries its upstream release-notes URL. No version number is repeated here, so there is nothing to drift.

## Shared code: this repo does not build alone

The `com.iboalali.*` modules are **published AARs** from [`iboalali/Android-Shared`](https://github.com/iboalali/Android-Shared) (private), resolved from `maven-repo`: a Maven repository kept in git, checked out beside this repo and declared in `settings.gradle.kts`.

**The version is the `shared` entry in `gradle/libs.versions.toml`, and it is real.** One line covers all eleven modules. Bumping it is how this app adopts a shared change; leaving it alone is how it declines one, and the other two apps sit on whatever their own catalogs say. A version that is not in the `maven-repo` checkout cannot resolve, so `git pull` there first.

**A fresh clone needs the `maven-repo` checkout.** It does *not* need the Android-Shared checkout, unless a shared change is being developed here, which is what the composite switch is for:

```bash
./gradlew :app:assembleGplayDebug -PandroidShared.composite=true
```

That includes the Android-Shared build from `../Android-Shared` (override with `androidShared.path`) and substitutes the local projects by `group:name`, which **throws the pinned version away**: every edit there lands here immediately, and nothing is pinned. That repo then needs its own gitignored `local.properties` with `sdk.dir`, and its AGP must match this app's or configuration fails with "Using multiple versions of the Android Gradle plugin". **Publish a version from Android-Shared before committing anything here that depends on a shared change.**

What lives where, and what this app keeps:

- **`com.iboalali.appcatalog:data` / `:ui`**: the About screen's "Other apps" feed (loading, the OkHttp `CatalogHttpSource` and its HTTP cache, the seed/fetch race guard, bundled `assets/apps*.json`) and the shared row. Look for catalog code and its tests there, not in this repo. This app keeps two things:
  - **Its card chrome.** `ui/about/OtherAppsCard.kt` is the outlined card, its title and its dividers; the rows inside are the library's, with `contentPadding` as the only style override because the card already pads horizontally.
  - **Its own four action strings.** App resources beat library resources of the same name, which is the intended override path. The library ships the same keys in the same five locales as a fallback for an app that hasn't got them.
- **`com.iboalali.telemetry:core`** owns the **TelemetryDeck lifecycle**: the startup signal buffer, automated-test-traffic detection, the identity reset, and the start-before-flush ordering. `analytics/Analytics.kt` keeps only this app's signal vocabulary and delegates the rest. Don't add a local signal gate or test-environment detector.
- **`com.iboalali.previews:matrix`** holds the screenshot matrices (this app's `PreviewPlayStore*` naming). It is declared `implementation`, not `screenshotTestImplementation`, because `util/ConstrainedDevicePreviews.kt` (the preview *functions*) sits in `src/main`. That also means those preview functions ship in the release APK (12 `ConstrainedDevicePreviews` entries in `mapping.txt`); moving them to `screenshotTest` would fix both. `util/PreviewPlayStoreListing.kt` is the separate **dp**-based matrix, unusable for store uploads but fine for browsing previews in the IDE, and used 3×.
- **`com.iboalali.nav3:overlay`** is the large-screen overlay (`DetailOverlayScene`, `DetailCard`, `DetailNavIcon`, `DetailAnchors`), shared with Billboard. `navigation/AppNavigation.kt` stays here as this app's nav host, and provides a `DetailOverlayStyle` naming this app's seven resources, so the library ships none of them. Two properties of the library a local copy would easily lose (detail in [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)):
  - **`semantics { isTraversalGroup = true }` on the overlay root**, so a screen reader reads scrim + card as one unit ahead of the dimmed main screen.
  - **A named scene class with value-based `equals`/`hashCode`.** An anonymous `object : OverlayScene<NavKey>` makes every recomposition look like a new scene to `NavDisplay`, whose transition bookkeeping decides from that whether to compose the entry again. Nothing fails visibly, so the mistake survives.

  There is deliberately no `Modifier.detailDialogShape()`: a screen in the card must not clip its own corners.
- **`com.iboalali.ui:theme`** is the theme cross-fade over every color role, including the twelve fixed-accent roles (constant across light and dark, so only a dynamic-color change moves them). `BasicRootCheckerTheme` keeps its palettes and its dynamic-color decision.
- **`com.iboalali.ui:licences`** is the Licences screen's content: all seven shipped libraries, grouped by license. Four are Apache 2.0, whose §4(a) obliges us to pass the license on. TelemetryDeck publishes a *modified* MIT with the attribution clause removed, so crediting it is courtesy rather than obligation (see Android-Shared's CLAUDE.md). The rows are the links, so no `Linkify` interop is needed. Worth knowing:
  - **`license_list` stays on the scrollable node here**, because `StartupBenchmarks` flings it and the Baseline Profile journey waits on it. The library ships *content*, not a screen, so the collapsing `LargeTopAppBar` and that testTag stay in this repo.
  - **The device-names dependency is `de.boehrsi:devicemarketingnames`, by Boehrsi**, Apache 2.0, **verified upstream 2026-08-17**. The published artifact is silent: no LICENSE in the AAR, no `<licenses>` in the POM, no source header. The repository's `LICENSE.txt` is the Apache 2.0 text, byte-identical to the copy `:ui:licences` ships, and there is no NOTICE file. **A dependency can carry zero license metadata and still be properly licensed**: the repository is the source of truth, not the artifact.
- **`com.iboalali.ui:menu`** is the overflow menu. The shared `AppBarDropdownMenuItem` routes its `onClick` through `rememberHapticClick`, so **call sites must not wrap their own `rememberHapticClick`**, or the item ticks twice. `HapticDropdownMenu` sets `testTagsAsResourceId` itself; the 16dp corner this app wants is a parameter, and `MainScreen` passes it.

## Traps that cost real time

Each of these has been paid for once. One line to recognize it; the detail is in the linked doc or skill.

- **An AppFunction can be completely dead while the build is green.** Compilation, unit tests, and Play's upload validator all pass over a broken surface. Verify with `adb shell cmd app_function execute-app-function`. → `appfunctions-wiring`
- **The `AppFunctionContext` parameter is deliberately absent from all three functions. Don't add it.** On the `@AppFunctionServiceEntryPoint` path it throws `null cannot be cast to non-null type AppFunctionContext` on *every* call, because KSP omits it from the inventory the dispatcher builds its parameter map from. The adapters pass `applicationContext` instead. A past "verified with adb" note does not survive a signature change, so re-verify after every AppFunction edit. `BaseRootAppFunctionService`'s KDoc explains the mechanism; leave that comment in place. → `appfunctions-wiring`
- **R8 vertically merges `BaseRootAppFunctionService` into the generated subclass**, leaving the generated XML pointing at a class absent from the release DEX. Play rejects the upload with *"The Android App Functions XML could not be parsed from the binary."* The keep rule in `proguard-rules.pro` prevents it; verify with `grep BaseRootAppFunctionService app/build/outputs/mapping/<variant>/mapping.txt`. → `appfunctions-wiring`
- **`testTagsAsResourceId` must be re-enabled on the overflow `DropdownMenu`**, because it renders in its own `Popup` window, outside the `AppRoot` scope. `HapticDropdownMenu` does this. Forgetting it fails *silently*: the journey can't find the menu items, so the secondary screens never get profiled. → `baseline-profiles`
- **The support card is unreviewable in a debug build.** The `.debug` applicationId means Play Billing never returns tip products, so `productsLoaded` can't become true. Use the debug-gated **"Demo: support card"** overflow item (`demoSupportPrompt()`). → [`docs/architecture.md`](docs/architecture.md)
- **A `true` from `requestReview()` means "handed to Play", never "a card appeared."** Play gives no "was it shown" callback. The version code (the release's single prompt) is spent only once the request actually reached Play. → [`docs/architecture.md`](docs/architecture.md)
- **Layoutlib's `Context` is a stub and never advances `LaunchedEffect`.** `getPackageInfo` returns null and `queryIntentActivities` is unimplemented. Guard both (see `DeviceInfo.getAppVersionName`, and `findInstalledPwaPackage` in `com.iboalali.appcatalog:ui`) and gate entrance animations on `LocalInspectionMode`. One crash fails the whole screenshot run. → `agp9-screenshot-tests`
- **WebAPK detection matches the shared shell *activity class*** (`org.chromium.webapk.shell_apk.`), not a package name. WebAPK packages differ per browser, and an unverified WebAPK isn't picked up by `ACTION_VIEW` routing. Needs the `<intent>` entries in the manifest's `<queries>`. → [`docs/architecture.md`](docs/architecture.md)
- **The catalog's traps live in Android-Shared with its code.** For example, the per-URL validator rule (replay an `ETag`/`Last-Modified` only against the URL it came from, or the English fallback can produce a `304` against a localized file we don't hold) is enforced there by OkHttp's `Cache`. Read that repo's `CLAUDE.md` before touching `AppCatalogRepository`. → [`iboalali/Android-Shared`](https://github.com/iboalali/Android-Shared)
- **The committed screenshot references intentionally show "(Debug)".** The store export is a separate script that neutralizes the debug name, renders, copies out, then restores both the strings and the regression baseline. → `play-store-assets`
- **`trackDeviceType` is one-shot per process.** It's read through rotations, folds, and resizes, which would otherwise inflate the count. → [`docs/architecture.md`](docs/architecture.md)
- **Haptics deliberately skip `createPredefined`** and `View.performHapticFeedback`. Several OEMs drop those silently while returning success. → `haptics-conventions`
- **The overlay's resting card rect is captured only while untransformed** (`progress > 0.999f && dragOffset == 0f`), or the `graphicsLayer`'s own scale feeds back into the source rect. → [`docs/adaptive-navigation.md`](docs/adaptive-navigation.md)

## Large-screen Baseline Profile coverage

The shipped profile **covers** the expanded-width overlay path. It was generated 2026-08-26 with two devices connected at once, SM-G766B (phone, 384dp) and SM-X356B (Galaxy Tab Active5 Pro, in landscape at 1280dp), and the plugin unioned both into the same `baseline-prof.txt`: `DetailOverlay` 153 rules, `OverlayScene` 140, `PredictiveBack` 37, including the `HPL` morph rules. The journey needs nothing special for it: the overlay renders in-composition, so the `*_list` testTags stay reachable.

**The profile is only as current as the code it was captured against.** It names classes by their real package, so moving code (for example into `Android-Shared`) silently invalidates every rule for the moved classes. Rules that no longer resolve are skipped at install time, so nothing fails; those paths just stop being compiled ahead of time. Regenerate after any extraction. The cheap check after a regeneration is that the shared package counts are non-zero: `com.iboalali.nav3.overlay` 268, `appcatalog` 231, `telemetry` 77, `haptics` 73, `ui.licences` 41, `ui.theme` 29, `ui.menu` 20. `previews.matrix` is legitimately 0, because the preview functions ship in the APK but never run.

**Why the `tabletApi36` GMD exists:** with only `useConnectedDevices = true`, overlay coverage depends on whatever happens to be plugged in. Regenerating on a phone alone silently drops about 1,150 rules while the build stays green. The GMD makes it reproducible. It was verified (2026-07-31) to emit the overlay rules *identically* to the physical tablet (138 / 135 / 37), so it is a real substitute, not an approximation.

Three properties of that device were each verified on the booted emulator, and each is the reason for a specific line:

- **Pixel C, not Pixel Tablet.** Measured 2560×1800 @ 320dpi = **1280 × 900dp**, so it clears 840dp in *both* orientations and boot rotation can't silently sabotage it. Pixel Tablet, Medium Tablet and Nexus 10 are all 800dp in portrait, just under. The physical Tab Active5 Pro has the same trap and only works because it sits in landscape.
- **`systemImageSource = "google"`, not `"aosp"`.** `adb root` succeeds on `google_apis`, so profile capture works; only `*_playstore` images block it. No `aosp` image is installed at any API level, so `"aosp"` would cost a large download for no benefit.
- **API 36 with `testedAbi = "x86_64"`.** Matches the physical test devices, is already on disk, and pinning the ABI silences an AGP warning and stops the choice drifting. → `baseline-profiles`
