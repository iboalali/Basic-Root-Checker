# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install debug APK on connected device
./gradlew installDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Lint check
./gradlew lint

# Run unit tests
./gradlew :app:testGplayDebugUnitTest

# Generate the baseline profile (both flavors) on a connected device
./gradlew :app:generateBaselineProfile

# Prove the profile moved the numbers (A/B startup + scroll macrobenchmark)
./gradlew :baselineprofile:connectedGplayBenchmarkReleaseAndroidTest

# Update the screenshot-test reference images (regression baseline; no device — JVM/Layoutlib)
./gradlew :app:updateGplayDebugScreenshotTest

# Check the UI against the committed reference images (fails on any visual diff)
./gradlew :app:validateGplayDebugScreenshotTest

# Export clean, upload-ready Play Store screenshots (all screens × all locales × store devices)
./scripts/generate-store-screenshots.sh [output-subfolder]
```

Unit tests live in `app/src/test/` and cover the app's pure decision logic — e.g. `RootChecker.classify`/`parseMagiskVersionCode` and the analytics `SignalGate` startup buffering. The hardware-dependent probes are not unit-tested; verify them on a real rooted device. AppFunctions are verified on a connected device with `adb shell cmd app_function list-app-functions --package <id>` and `... execute-app-function --function '<class>#<fn>' --parameters '{}'`.

## Architecture

Android app that checks whether a device has root access, written in Kotlin with Jetpack Compose.

**Package:** `com.iboalali.basicrootchecker`

### Key Components

- **MainActivity** — Single activity host. Sets up splash screen with custom exit animation, dynamic colors, edge-to-edge, attaches the billing and in-app-update controllers to its lifecycle, and hosts `AppRoot` via `setContent`.
- **AppRoot** (`ui/`) — Thin root overlay: a `Box` hosting `AppNavigation` plus one app-wide `SnackbarHost` for signals not tied to a single screen (e.g. a late-cleared pending tip). Per-screen Scaffolds still own their own snackbars.
- **AppNavigation** (`navigation/`) — Navigation 3 setup with `NavDisplay`, `@Serializable` route keys (`MainRoute`, `SettingsRoute`, `AboutRoute`, `LicenceRoute`), and explicit back stack management. **Adaptive large screens:** at the **expanded** width breakpoint (≥840dp: tablets, unfolded foldables in landscape, desktop windows, XR panels) the secondary screens (Settings/About/Licence) open as a **dialog over the dimmed main screen** instead of a full-screen push. This uses Navigation 3's built-in `DialogSceneStrategy` (an `OverlayScene` that hosts the top entry in a `Dialog`, with scrim-tap + Back dismissal handled for free). It's width-gated by *conditional metadata*: the three secondary entries get `DialogSceneStrategy.dialog()` metadata **only when** `currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)` (the `entryProvider` re-runs on width changes, so it follows fold/unfold/resize live); below the breakpoint they carry no dialog metadata and fall through to the single-pane push flow with the app's own forward/back + predictive-back transitions. The secondary screens' leading icon is chosen by `LocalDetailNavIcon` (`navigation/DetailNavIcon.kt`): provided as `CLOSE` (an X) at expanded width — inherited into the `Dialog` content — and `BACK` (up-arrow) otherwise; both call the same `onNavigateBack`. A `navigateToDetail` helper keeps the back stack at `[Main, oneDetail]` (the three are interchangeable siblings reached only from the main screen). *Note:* the built-in dialog renders each secondary screen as-is inside a platform-sized dialog, not a tightly-bounded card — a custom `OverlayScene` wrapper would be needed for exact card framing.
- **MainScreen** (`ui/main/`) — Main screen composable. Displays device info (model, marketing name, Android version) and root status. FAB triggers root check. Long-press on device info copies to clipboard.
- **MainViewModel** (`ui/main/`) — AndroidViewModel with `StateFlow<MainUiState>` for root check state, root provider + version, device info, and in-app update flow state. Exposes `checkRoot()` (passive evaluation) and `requestRoot()` (forces shell construction to trigger the superuser allow dialog), both running through `RootChecker` on coroutines.
- **SettingsScreen / SettingsViewModel** (`ui/settings/`) — Settings: telemetry and haptics toggles, the in-app language picker (Android 13+), the tip jar, and a privacy-policy link. Collects the billing layer's one-shot `TipEvent`s (Thanks/Pending/Error) as snackbars.
- **AboutScreen** (`ui/about/`) — About screen with collapsing toolbar, app version, contact links, and other apps card (`OtherAppsCard`).
- **LicenceScreen** (`ui/licence/`) — License screen with collapsing toolbar, license texts.
- **RootChecker** (`data/`) — Two suspend entry points on `Dispatchers.IO`: `check(context)` evaluates passively; `requestRoot(context)` executes `Shell.cmd("id")` first to force libsu's main shell to construct (which triggers the Magisk/KernelSU/APatch allow dialog) before re-evaluating. Both return a `RootResult` sealed interface (`NotRooted` / `Unknown` / `Rooted(provider, version)` / `RootedNotGranted(provider)`). Providers are classified via the `RootProvider` enum (`MAGISK` / `KERNELSU` / `APATCH` / `OTHER` / `UNKNOWN`). Unprivileged probes (installed packages declared in `<queries>`, `/proc/self/mounts` scan, `su` binary existence across standard paths) run regardless of grant state, so a device with root installed but the app not yet allowed is reported as `RootedNotGranted` rather than `NotRooted`. When granted, the Magisk version is read via `magisk -v` / `magisk -V` and the `/data/adb/magisk` etc. paths confirm the provider. Both entry points take an `applyUiDelay` flag (default `true`; AppFunctions pass `false` to skip the ~1 s UI settle delay) and record each result via `UserPreferences.recordRootCheck`, so UI (FAB) and AppFunction callers share one "last checked" value.
- **AppFunctions** (`appfunctions/`) — Exposes the app's root-check workflows to the Android system and on-device agents (e.g. Gemini) via `androidx.appfunctions` (Jetpack; resolves on Android 16+, no-ops below). `RootAppFunctions` is a plain no-arg class with three `@AppFunction`s — `checkRootStatus` (fresh passive check), `requestRootAccess` (triggers the superuser dialog), and `getLastRootCheck` (returns the last cached check + `checkedAt` without re-probing). Each gets its `Context` from `AppFunctionContext.context` and maps the sealed `RootResult` to the flat `@AppFunctionSerializable` `RootStatus`. Ships in both flavors (no Google dependency); the schema is generated by the KSP `appfunctions-compiler` (`aggregateAppFunctions=true`) and surfaced via `res/xml/app_metadata.xml` + the `app_metadata` manifest `<property>`, while the `AppFunctionService` is merged in by `appfunctions-service`.
- **Billing / tip jar** (`billing/`) — `BillingController` interface with two flavor implementations: `GPlayBillingController` (`gplay/`, Google Play Billing) and `NoOpBillingController` (`foss/`, reports `isAvailable = false` so the tip jar is hidden). Each `TipTier` (SMALL/MEDIUM/LARGE) has a durable *record* product (acknowledged, kept forever) and a *repeat* product (consumed, repurchasable); `supporterTiers` is recomputed from owned records on every connect, so it survives reinstalls. One-shot `events` (`TipEvent`) drive Settings snackbars; `tipCleared` fires app-wide (via `AppRoot`) when a previously-`PENDING` tip clears — pending purchase tokens are persisted in `UserPreferences` so a clear is recognized even after process death or on the next launch (vs. the routine re-grant of an already-owned tip on every connect).
- **UserPreferences** (`data/`) — DataStore-Preferences store (telemetry, haptics, theme mode, last-seen version code, pending tip tokens, and the last root check — result + timestamp, exposed as `lastRootCheck`/`recordRootCheck` and shared by the UI and AppFunctions).
- **Analytics** (`analytics/`) — Thin `Analytics` object over the TelemetryDeck SDK; every `trackX` call routes through a `SignalGate`. TelemetryDeck is initialized **asynchronously** in `BasicRootCheckerApplication.onCreate` so cold start isn't blocked: the opt-out preference is read off the main thread, and `TelemetryDeck.start()` is posted *back* to the main thread (it registers a lifecycle observer, so it must run there). While the preference is still being read, `SignalGate` is `PENDING` and buffers signals in a bounded queue; `Analytics.setEnabled(true)` — called **after** `start()` so flushed signals reach an initialized SDK — replays them, while `setEnabled(false)` discards them. The Settings telemetry toggle reuses the same `setEnabled` path.
- **DeviceInfo** (`util/`) — Helpers for app version retrieval and Android version name lookup (maps API level to name via `version_names` string array resource).
- **Haptics** (`util/` + `ui/`) — Two layers, both gated on the user's "Haptic feedback" setting (`UserPreferences.hapticsEnabled`). A single `RootHaptics` (`util/`) instance lives on `BasicRootCheckerApplication` (`rootHaptics`) and is shared by both the root-check flow and the UI tap feedback. It plays the rich root-check vibrations from `MainViewModel`: a rising-frequency "checking" ramp plus distinct success/error/neutral result patterns, with graceful fallback across the vibration APIs (PWLE envelopes on API 36+ → amplitude waveforms on API 26+ → legacy patterns on 23–25); it reads the preference before playing. It also exposes subtle one-shots for UI feedback — `playTap`/`playLongPress`, which pick the best actuator API by *queryable* capability: a `PRIMITIVE_CLICK` composition where the actuator genuinely supports it (`areAllPrimitivesSupported`, API 30+ — crispest, e.g. Pixel), else a **raw** `createOneShot` amplitude pulse (API 26+) / legacy duration vibrate. It deliberately skips `createPredefined` (`EFFECT_CLICK` etc.): like the framework constants, predefined effects are HAL-mapped and several OEMs (notably Samsung) silently drop them, whereas a raw amplitude pulse always reaches the motor. `HapticClick` (`ui/`) wires those to Compose via the `rememberHapticClick` / `rememberHapticToggle` / `rememberHapticLongClick` helpers, gated on the `LocalHapticsEnabled` CompositionLocal (both it and `LocalAppHaptics` — the shared `RootHaptics` — are provided once by `MainActivity` around `AppRoot`). **Why the Vibrator and not `View.performHapticFeedback`:** there is no reliable way to detect whether a device renders a framework tap constant — many skinned OEMs (Samsung, OnePlus/Oppo, Xiaomi, Vivo) silently drop `CONTEXT_CLICK`/`VIRTUAL_KEY` *while the call still returns success*. Driving the Vibrator directly sidesteps that lottery (it works wherever there's an actuator, gated by the in-app toggle + master vibration, not the OS "touch feedback" sub-setting).
- **Preview Utilities** (`util/`) — Custom preview annotations: `@PreviewLocales` (en, de, ar, es, ru) and `@PreviewPlayStoreListing` (Phone, 7" Tablet, 10" Tablet).

### Build Configuration

- **Gradle:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`), AGP 9.2.1
- **SDK:** compile/target 37 (Android 17), min 23
- **Kotlin:** 2.4.0, JVM target 17 — AGP 9's **built-in Kotlin** compiles the modules; the app applies the Compose/serialization plugins but no separate `org.jetbrains.kotlin.android`, and the `:baselineprofile` module applies none.
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking enabled)
- **Product flavors:** `gplay` (Google Play services — the in-app-billing tip jar and in-app updates) and `foss` (no Google services — no-op billing/update implementations). Flavor-specific code lives under `app/src/gplay/` and `app/src/foss/`; unit tests run on the `gplay` variant (`:app:testGplayDebugUnitTest`).
- **Compose** enabled with Compose BOM for dependency management
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys
- **Material 3 Adaptive** (`androidx.compose.material3.adaptive:adaptive`, versioned separately from the Compose BOM) — `currentWindowAdaptiveInfoV2`/`WindowSizeClass` (via the transitive `androidx.window:window-core`) for the window-width check that switches the secondary screens to a dialog on large screens. See the **AppNavigation** component.
- **AppFunctions** (`androidx.appfunctions`) with the KSP `appfunctions-compiler` (`com.google.devtools.ksp` plugin, version paired to Kotlin); generated in both flavors
- **Baseline Profiles:** a separate `:baselineprofile` module (`com.android.test`) generates the AOT profile shipped in both flavors; the `androidx.baselineprofile` plugin also adds synthetic `nonMinifiedRelease`/`benchmarkRelease` build types to `:app`. See the **Performance / Baseline Profiles** section.

### Localization

Five locales: English (`en`), German (`de`), Arabic (`ar`), Spanish (`es`), Russian (`ru`). Locale config in `res/xml/app_locales_config.xml`.

### Theming

Compose Material3 with dynamic colors (API 31+), fallback to custom light/dark color schemes defined in `ui/theme/Color.kt`. Splash screen theme chain in XML (`values-v21/v23/v27/v31` theme qualifiers).

### Accessibility

**Keep accessibility (TalkBack and large font scales) in mind for all UI code — treat it as part of "done," not a follow-up.** When adding or changing Compose UI, apply these conventions (all already used in the codebase):

- **Label actionable icons.** Any `IconButton`/clickable icon that isn't accompanied by visible text needs a `contentDescription` on its `Icon` (e.g. the overflow and navigation-up buttons). Decorative icons whose meaning is already conveyed by adjacent text — chevrons, the status icon, trailing affordances inside a labelled row — take `contentDescription = null`.
- **Localize accessibility strings.** `contentDescription`s and action labels are user-facing: add them to `res/values/strings.xml` **and all four locale files** (`de`, `ar`, `es`, `ru`), same as any other string. Prefer the wording Android's own AppCompat resources use (e.g. "Navigate up", "More options") so it matches what users hear elsewhere.
- **Announce in-place changes with a live region.** When content updates without moving focus (the root-check result, the in-app update card), mark the changing `Text` with `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` so screen readers read the new value. Don't merge a whole card as a live region if it contains a focusable button.
- **Label controls via their row.** For a row that pairs a title with a `Switch`/`RadioButton`, make the row itself `toggleable(role = Role.Switch)` / `selectable(role = Role.RadioButton)` and pass `onCheckedChange = null` / `onClick = null` to the control. This labels the control with the title and makes the whole row the touch target.
- **Expose non-tap gestures as custom actions.** A long-press (or other gesture) action must also be reachable by screen readers — add a `CustomAccessibilityAction` (with a localized label) via `Modifier.semantics`, and avoid empty `onClick = {}` handlers that advertise a do-nothing action. See `DeviceInfoText` (copy on long-press).
- **Touch targets ≥ 48dp** and **respect font scale** (use `sp`/Material typography, never fixed `dp` text). Keep the `@PreviewFontScale` / `@PreviewLightDark` preview coverage when adding screens.
- **Verify on a real device with TalkBack** for behavioral changes — the IDE previews don't catch announcement/focus issues.

### Haptics

**Every tappable control gives a subtle tap tick, gated on the "Haptic feedback" setting — treat it as part of "done," like accessibility.** Don't call `LocalHapticFeedback`/`Vibrator` directly from a screen; wrap the control's handler with the helpers in `ui/HapticClick.kt`:

- **Plain taps** (`Button`, `IconButton`, `FloatingActionButton`, `Card(onClick = …)`, `DropdownMenuItem`, `Modifier.clickable`, `selectable` rows, dialog buttons) → `onClick = rememberHapticClick(handler)`. For trailing-lambda `.clickable { … }`, rewrite as `.clickable(onClick = rememberHapticClick { … })`.
- **`toggleable` rows** (a title paired with a `Switch`/`Checkbox`) → `onValueChange = rememberHapticToggle(handler)`. Leave the inner control's `onCheckedChange = null` / `onClick = null` (the row owns it, per the accessibility convention above).
- **Long-press / other non-tap gestures** → `rememberHapticLongClick(handler)` (fires the standard `LongPress` buzz, which `detectTapGestures` doesn't add on its own). See `DeviceInfoText` (copy on long-press), which reuses the wrapped action for its `CustomAccessibilityAction` too.

The helpers no-op when haptics are disabled and re-key correctly when the setting toggles, so nothing else is needed. They deliberately drive the `Vibrator` (via `RootHaptics`) rather than `View.performHapticFeedback` — don't "simplify" that to `performHapticFeedback(ContextClick)`, or taps go silent on Samsung/OnePlus/Xiaomi/Vivo (the call returns success but nothing fires, and there's no API to detect it). The root-check ramp/result vibrations are a separate concern — same shared `RootHaptics`, but played from `MainViewModel`, not the UI.

### Performance / Baseline Profiles

The app ships a **Baseline Profile** (ART AOT-compilation hints) so cold start and first-scroll are pre-compiled on install — measured ~19% faster cold start on a mid-range device. **When you add or rework a screen or a hot user journey, update the generator journey and regenerate — treat it as part of "done," like accessibility and haptics.**

- **`:baselineprofile` module** (`com.android.test`, `androidx.baselineprofile` plugin) mirrors the app's `gplay`/`foss` flavors and targets `:app`. It holds two test classes (tests live in `src/main`, per `com.android.test` convention):
  - `BaselineProfileGenerator` — the `BaselineProfileRule` journey: cold start, then navigate to and scroll Settings / About / Licence. `Journeys.kt` has the shared UI Automator helpers.
  - `StartupBenchmarks` — A/B `MacrobenchmarkRule` tests: each metric has a `*BaselineProfile` test (`CompilationMode.Partial(BaselineProfileMode.Require)` — fails loudly if the profile is missing) paired with a `*NoCompilation` test (`CompilationMode.None`). Compare **medians**. Startup uses `StartupTimingMetric` (cold, ≥10 iters); the Licence scroll uses `FrameTimingMetric` (no `startupMode` — it `killProcess()`s and re-navigates in `setupBlock`, else the measured scroll finds an empty screen).
- **Generated profiles are committed and shipped.** `generateBaselineProfile` writes per-flavor `baseline-prof.txt`/`startup-prof.txt` under `app/src/<flavor>Release/generated/baselineProfiles/`; release builds merge them into `assets/dexopt/baseline.prof[m]` (verify with Analyze APK). Both flavors get a profile (gplay's is larger — it includes the Play/billing paths).
- **`testTagsAsResourceId` is mandatory for the journeys** — UI Automator finds elements via `By.res(<testTag>)`, locale-independently (the app ships 5 locales, so text matchers won't do). It's enabled on the `AppRoot` Box (the main window) **and must be re-enabled on every `Popup`/`DropdownMenu`/dialog**, which render in their own window outside that scope — see the overflow `DropdownMenu` in `MainScreen` (forgetting this silently breaks navigation: the menu items aren't found, so the secondary screens never get profiled). Any nav control or scroll container a journey touches needs a `Modifier.testTag(...)`; any new popup needs its own `Modifier.semantics { testTagsAsResourceId = true }`. Note: tags surface in release-based builds (`nonMinifiedRelease`/`benchmarkRelease`), not in `debug` — don't try to verify them with a debug build.
- **Large-screen coverage is a known gap — revisit when tablets/foldables become a meaningful share of users.** Generation runs on the connected phone (`useConnectedDevices = true`) and `Journeys.kt` never crosses the 840dp breakpoint, so the shipped profile covers the single-pane push flow but **omits the expanded-width path** — the `DialogSceneStrategy`/`OverlayScene` `Dialog` classes that host Settings/About/Licence on tablets and unfolded foldables (see the **AppNavigation** component). There is no separate per-form-factor profile to ship: each flavor has exactly one profile, and a baseline profile is a list of class/method descriptors that ART applies at install time regardless of screen size. To cover large screens you don't make a second file — you make the generator *also* run at expanded width, and the `androidx.baselineprofile` plugin **merges** (unions) the rules from every device it generates on into the same `baseline-prof.txt`. Concretely: add a tablet/foldable **Gradle Managed Device** to the `baselineProfile { … }` block in `baselineprofile/build.gradle.kts` (keep `useConnectedDevices = true` so the phone is still included). At ≥840dp the existing navigate-to-secondary-screen steps take the dialog branch automatically — no journey logic change needed. **Gotcha:** the dialog renders in its own window, *outside* the `AppRoot` `testTagsAsResourceId` scope, so the large-screen leg silently profiles nothing unless `testTagsAsResourceId = true` is re-enabled inside the `DialogSceneStrategy` content (the same trap as the overflow `DropdownMenu`, above). This is optional polish, not correctness — cold start to `MainScreen` is form-factor-independent and already covered; the only missing piece is the first open of a secondary screen on a large window (one-time JIT cost instead of AOT).
- **TTFD:** `MainScreen` calls `ReportDrawnWhen { … }` so `timeToFullDisplay` marks the first meaningful frame (device info loads synchronously in the ViewModel's `init`, so it ≈ `timeToInitialDisplay`).
- **Device requirements:** generation runs the journey on a connected device and needs API 33+ or root (works on the API 36 test device); measurement needs `<profileable android:shell="true">` (already in the manifest) on API 29+.
- **Versions:** `benchmark`/`androidx.baselineprofile` are on `1.5.0-alpha` — required for AGP 9 (stable 1.4.x predates it). This build emits `frameCount` but not `frameOverrunMs` for the app's non-lazy `verticalScroll` screens, so startup is the headline metric and the scroll test mainly guards the journey. The plugin debug-signs its synthetic build types automatically (`release` has no signing config in `build.gradle.kts`).

### Screenshot Tests (Compose Preview)

The app uses the **official AndroidX Compose Preview Screenshot Testing** plugin (`com.android.compose.screenshot`) to render `@Preview`-style composables to PNGs **on the JVM via Layoutlib — no device/emulator**. It serves two goals at once: a **visual-regression baseline** (committed reference images, checked in CI) and **Play Store listing assets** (all screens, all locales, native device resolution). **When you add or rework a screen, add/refresh its `@PreviewTest` and regenerate — treat it as part of "done," like accessibility, haptics, and baseline profiles.**

- **Where it lives:** `@PreviewTest` functions are in the **`screenshotTest` source set** (`app/src/screenshotTest/`), one per screen/state — they render the stateless `*Content` composables (e.g. `MainScreenContent`, `SettingsScreenContent`, `AboutScreenContent`, `LicenceScreen`) with fixed sample state. *The source set can only see `public`/`internal` members of `main`* — so a composable a screenshot needs must be at least `internal` (this is why `AboutScreenContent` is `internal`, not `private`). Enabled by the plugin + `screenshotTestImplementation(libs.screenshot.validation.api)` + `screenshotTestImplementation(libs.androidx.compose.ui.tooling)`, plus **both** `android.experimental.enableScreenshotTest=true` in `gradle.properties` **and** `experimentalProperties["android.experimental.enableScreenshotTest"] = true` in the `android {}` block (AGP 9 requires both, or the plugin fails to apply).
- **Native-resolution device matrix:** `util/PreviewPlayStoreNative.kt` (the companion to `PreviewPlayStoreListing`) is the multipreview applied to each screen — **3 store devices × 5 locales = 15 PNGs per screen**. The tool sizes each PNG **purely from the `@Preview` device spec** (`output px == spec px`; there is no scale knob), so the devices use **pixel-based** specs (`spec:width=1080px,…,dpi=420`) to get high-resolution, native-size output. Each entry's `name` is kept free of special characters (no quotes) so the tool **embeds it in the reference filename** (`MainRootedShot_Phone_ar_<hash>_0.png`), making the PNGs sortable by screen/device/locale. Bare `@Preview(showBackground = true)` would render at small wrap-content size — always use a px device spec.
- **Generate / validate:** `./gradlew :app:updateGplayDebugScreenshotTest` writes references under `app/src/screenshotTestGplayDebug/reference/…`; `:app:validateGplayDebugScreenshotTest` fails on any diff. The CLI tasks operate at the **variant level** (no per-preview filter) — to regenerate just one preview, use the **Android Studio gutter icon** on its `@PreviewTest` function. Run on the `gplayDebug` variant (these screens are flavor-independent; flavor-specific bits like the tip jar are passed in as plain args).
- **Layoutlib preview-safety:** Layoutlib's `Context`/`PackageManager` is a stub — `getPackageInfo` returns null, `queryIntentActivities` is unimplemented, and `LaunchedEffect`-driven enter animations never advance. Code reached by a screenshot must degrade gracefully: guard package-manager calls (see `DeviceInfo.getAppVersionName`, `OtherAppsCard.findInstalledPwaPackage`) and gate entrance animations on `LocalInspectionMode.current` so the resting state renders (see the result-icon scale in `MainScreen`). A crash in any rendered composable fails the whole screenshot.
- **Play Store export:** `scripts/generate-store-screenshots.sh [subfolder]` produces clean, upload-ready PNGs in `Play Store/Generated Screenshots/<subfolder>/`. The screenshot tool only renders the **debug** variant, whose `src/debug/res` renames the app to "… (Debug)"; the script temporarily neutralizes that override (the `screenshotTest` source set **cannot** override the app-under-test's resources), renders, copies the clean PNGs out with their `_<hash>_0` suffix stripped, then **restores both the debug strings and the committed regression baseline** — so a plain `validate` still passes and nothing is left modified. The committed `reference/` images therefore intentionally show "(Debug)"; the clean store assets are a separate export.

## Changelog

`CHANGELOG.md` at the repo root follows the [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) format with sections **Added / Changed / Deprecated / Removed / Fixed / Security**. In-flight work lives under `## [Unreleased]`; at release time that heading is renamed to `## [<version>] - <YYYY-MM-DD>` and a fresh empty `[Unreleased]` is added above it.

**Update the changelog as part of every feature, bug fix, or user-visible behavior change.** Add a bullet under the appropriate section of `[Unreleased]` in the same commit (or PR) that introduces the change. Write entries in user-facing language — no commit SHAs, no internal jargon. Skip the changelog only for pure refactors, internal docs edits, dependency-only bumps with no user impact, or build-config tweaks that don't change shipped behavior.

## Play Store release notes

Per-version store "What's new" text lives under `Play Store/Release Notes/<version>/`, one file per locale named by language (`default`, `german`, `arabic`, `spanish`, `russian` — matching the `Play Store/Listing/` convention, not BCP-47 codes). Each version folder also has:
- `play-console.txt` — all locales in Google Play's `<lang-tag>…</lang-tag>` block format (tags: `en-US`, `de-DE`, `ar`, `es-ES`, `ru-RU`) for pasting every language at once.
- `all-locales.md` — the same content as a human-readable reference.

Conventions: lead each note with `Version <x.y>:` (localized version word), then one line per highlight prefixed with `➕` (added), `🛠️` (changed), or `🔨` (fixed). Keep every locale within Play's **500-character** limit. Mirror the changelog's wording but condensed to user-facing highlights.

**Keep release notes in sync with the changelog.** When `[Unreleased]` is cut to `## [<version>] - <YYYY-MM-DD>`, create `Play Store/Release Notes/<version>/` with all five locale files plus `play-console.txt` and `all-locales.md`, distilling that version's changelog entries into the format above.
