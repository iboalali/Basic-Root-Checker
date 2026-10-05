# Architecture

Android app that checks whether a device has root access. Kotlin, Jetpack Compose, single activity.

**Package:** `com.iboalali.basicrootchecker`

Navigation and the large-screen detail overlay have their own doc: [`adaptive-navigation.md`](adaptive-navigation.md).

## Entry points and shell

- **MainActivity**: the single activity host. Sets up the splash screen with a custom exit animation, dynamic colors and edge-to-edge. Attaches the billing, in-app-update and in-app-review controllers to its lifecycle, starts the one background `AppCatalogRepository.refresh()` per launch, and hosts `AppRoot` via `setContent`. Provides `LocalHapticsEnabled` and `LocalAppHaptics` around `AppRoot`.
- **AppRoot** (`ui/`): a thin root `Box` hosting `AppNavigation` plus one app-wide `SnackbarHost` for signals not tied to a single screen. Per-screen Scaffolds own their own snackbars.

  It owns **both** tip flows: `tipCleared` (a pending tip that clears late, long after the purchase and away from Settings) and the one-shot `TipEvent`s (Thanks / Pending / Error).

  **The `TipEvent` collector must live here, not on a screen.** `BillingController.events` is a `Channel`, so it is **single-consumer**: two collectors *split* the events between them rather than each getting all of them. A tip can start from Settings or from the main screen's support card, and at expanded width the secondary screens are composed as an overlay *over a live `MainScreen`*, so both collectors would be active at once. One consumer at the root works whichever surface opened the tip jar, and this host draws above the overlay card.

## Screens

- **MainScreen** (`ui/main/`): device info (model, marketing name, Android version) and root status. The FAB runs the root check. Long-press on device info copies it to the clipboard. Calls `ReportDrawnWhen { … }` so `timeToFullDisplay` marks the first meaningful frame. Device info loads synchronously in the ViewModel's `init`, so it is about equal to `timeToInitialDisplay`.
- **MainViewModel** (`ui/main/`): `AndroidViewModel` with `StateFlow<MainUiState>` for root-check state, root provider and version, device info, in-app update state, and the support-card prompt. Exposes `checkRoot()` (evaluation without a first-time prompt) and `requestRoot()` (forces shell construction to trigger the superuser allow dialog), both running through `RootChecker` on coroutines. Plays the root-check haptic ramp and result patterns.
- **SettingsScreen / SettingsViewModel** (`ui/settings/`): telemetry and haptics toggles, the in-app language picker (Android 13+), the tip jar, and a privacy-policy link. Opens the shared `TipJarDialog` (`ui/tip/TipJar.kt`) and dismisses it on selection. The outcome snackbars are collected in AppRoot, not here.
- **AboutScreen / AboutViewModel** (`ui/about/`): collapsing toolbar, app version, contact links, and the "Other apps" card. A **"Rate this app"** link is added to the contact links only when `ReviewController.isAvailable` (Google Play builds). It opens the Play listing directly, separate from the automatic in-app review card.

  `AboutViewModel` is **read-only**. It observes `AppCatalogRepository.otherApps` and maps each entry to the shared `@Immutable OtherApp`. The repository excludes this app from its own list (it matches the release `applicationId`, stripping the `.debug` suffix first). `MainActivity` owns the catalog fetch.

  The screen splits into `AboutScreen` (wires the VM) and an `internal AboutScreenContent` (stateless, so the screenshot test can render it; the `screenshotTest` source set can only see `public`/`internal`).
- **LicenseScreen** (`ui/license/`): collapsing `LargeTopAppBar` around the license content from `com.iboalali.ui:licences`. The scrollable node keeps the `license_list` testTag, which `StartupBenchmarks` and the Baseline Profile journey depend on.

### Other apps card (`ui/about/OtherAppsCard.kt`)

The **card** is this app's; the **rows in it** are `com.iboalali.appcatalog:ui`'s `OtherAppRow`. This app owns the outlined card, its title and its dividers, plus the three seams the library deliberately leaves to the app: the haptic tick and `Analytics.trackOtherAppClicked` (both through one `onAction` callback, which fires before the intent), and this app's bundled icons (through `fallbackIcon`). The only visual override is `contentPadding`, because the card already pads horizontally.

Each row: icon (remote via Coil, with the local fallback), name, localized description, highlight bullets (inline Markdown, `**bold**` / `*italic*` only), actions.

- An installable app shows **Open** when a launch intent resolves, else **Install** (Play listing).
- Any entry with a website also gets **Website**.
- A web-only entry (no `packageName`) shows a single button: **Open** when the site is installed as a **PWA/WebAPK**, else **Website**.

The PWA case is detected by matching the shared `org.chromium.webapk.shell_apk.` shell *activity class*, not a package name. WebAPK packages differ per browser, and an *unverified* WebAPK isn't picked up by `ACTION_VIEW` link routing. Both lookups need the `<intent>` entries in a `<queries>` block. The library's manifest ships them and merges in; this app's own copies are a harmless duplicate.

`localIconFor`'s `else` branch returns this app's `ic_baseline_android_24`, not null. Null hands the row the library's generic icon, which is a *different drawable* (the Material Symbols one Billboard uses), and the screenshot test would flag the change.

The card hides itself when the list is empty.

## Root detection

**RootChecker** (`data/`) has two suspend entry points on `Dispatchers.IO`:

- `check(context)` evaluates without prompting a device that never granted. libsu reports a grant only once a root shell exists, so a granted app would otherwise read as undetermined in every fresh process. `check` therefore builds the shell itself when `isAppGrantedRoot()` is undetermined (an `su` is on `PATH`) and the last recorded check was `ROOTED`. A device that has never been `ROOTED` here stays fully passive; one whose grant was revoked may see one prompt, and then records as not granted.
- `requestRoot(context)` runs `Shell.cmd("id")` first to force libsu's main shell to construct (which triggers the Magisk/KernelSU/APatch allow dialog), then evaluates.

Both return a `RootResult` sealed interface (`NotRooted` / `Unknown` / `Rooted(provider, manager, version)` / `RootedNotGranted(provider, manager)`). Providers are the `RootProvider` families (`MAGISK` / `KERNELSU` / `APATCH` / `OTHER` / `UNKNOWN`); `RootManager` names the specific installed manager app.

Unprivileged probes run regardless of grant state: installed packages declared in `<queries>`, a `/proc/self/mounts` scan, Magisk path stats, and `su` binary existence across standard paths. So a device with root installed but the app not yet allowed is reported as `RootedNotGranted`, not `NotRooted`. When granted, the Magisk version is read via `magisk -v` / `magisk -V`, and the `/data/adb/magisk` paths confirm the provider.

Both entry points take an `applyUiDelay` flag (default `true`; AppFunctions pass `false` to skip the ~1 s UI settle delay) and record each result via `UserPreferences.recordRootCheck`, so the FAB and AppFunction callers share one "last checked" value.

Coverage, blind spots and emulator recipes: [`root-provider-detection-gaps.md`](root-provider-detection-gaps.md).

## AppFunctions (`appfunctions/`)

Exposes the root-check workflows to the Android system and on-device agents (for example Gemini) via `androidx.appfunctions`. Ships in both flavors (no Google dependency), release included.

| Function | Behavior |
|---|---|
| `checkRootStatus` | fresh check, same as the FAB |
| `requestRootAccess` | triggers the superuser dialog |
| `getLastRootCheck` | returns the last cached check + `checkedAt` without re-probing |

Each takes no parameters, receives the service's `applicationContext` from the adapter, and maps the sealed `RootResult` to the flat `@AppFunctionSerializable` `RootStatus`.

**None of the three declares an `AppFunctionContext` parameter, and none may.** On the `@AppFunctionServiceEntryPoint` path it breaks every call at runtime while every local check stays green. See the trap in [`../CLAUDE.md`](../CLAUDE.md).

**Wiring.** The `@AppFunction`s and their agent-facing KDoc live on the abstract `BaseRootAppFunctionService : AppFunctionService()` (`RootAppFunctionService.kt`), annotated `@AppFunctionServiceEntryPoint(serviceName = "RootAppFunctionService", appFunctionXmlFileName = "root_app_function_service")` plus `@RequiresApi(36)`. Each method is a thin adapter delegating to `RootAppFunctions`, a plain class with no framework annotations, so it is testable without the service lifecycle.

KSP (`appfunctions-compiler`) generates the concrete `RootAppFunctionService` **and** `assets/root_app_function_service.xml`, both declared in `AndroidManifest.xml`. The `<service>` carries the `BIND_APP_FUNCTION_SERVICE` permission, the `android.app.appfunctions.schema` / `.v2` `<property>`s, and the `android.app.appfunctions.AppFunctionService` intent filter. `res/xml/app_metadata.xml` (the app-level `android.app.appfunctions.app_metadata` `<property>`) is the LLM-facing app description.

General mechanics (the single runtime artifact, the R8 keep rule, `adb` verification) are in the `appfunctions-wiring` skill. After a release build, verify the R8 rule with `grep BaseRootAppFunctionService app/build/outputs/mapping/<variant>/mapping.txt`: the class and all three method names must map to themselves.

## Monetization and prompts

### Billing / tip jar (`billing/`)

`BillingController` interface with two flavor implementations: `GPlayBillingController` (`gplay/`, Google Play Billing) and `NoOpBillingController` (`foss/`, reports `isAvailable = false` so the tip jar is hidden).

Each `TipTier` (SMALL / MEDIUM / LARGE) has a durable *record* product (acknowledged, kept forever) and a *repeat* product (consumed, repurchasable). `supporterTiers` is recomputed from owned records on every connect, so it survives reinstalls.

One-shot `events` (`TipEvent`) and `tipCleared` are both collected app-wide by `AppRoot` (see there for why `events` must have exactly one consumer). `tipCleared` fires when a `PENDING` tip clears. Pending purchase tokens are persisted in `UserPreferences`, so a clear is recognized even after process death or on the next launch, and is told apart from the routine re-grant of an already-owned tip on every connect.

### In-app review / rating (`review/` + `ui/main/ReviewGate.kt`)

`ReviewController` interface with two flavor implementations, mirroring the billing/update split: `GPlayReviewController` (`gplay/`, the Play-managed in-app review card) and `NoOpReviewController` (`foss/`, `isAvailable = false`). Attached to `MainActivity`'s lifecycle.

**`ReviewGate` is the pure, unit-tested decision logic** (`ReviewGateTest`). It is eligible once root has been confirmed `MIN_ROOTED_CHECKS` (3) times **and** the prompt hasn't already fired on this or a later version code: about once per release, on top of Play's own quota. `MainViewModel.maybeRequestReview` runs it after any `RootResult.Rooted` (confirming root is the app's "win" moment).

**Gating rules that are easy to get wrong.** The version code is recorded, spending the release's single prompt, *only* once the request actually reached Play. So (a) FOSS returns early on `!isAvailable` without counting toward the gate, and (b) `requestReview()` returns `Boolean` and reports `false` when no activity is attached (a check finishing mid-recreation), leaving the slot unspent.

Play gives **no "was it shown" callback**, so a `true` return means "handed to Play", never "a card appeared". Don't build on it.

### Post-check asks: support card (`ui/main/SupportCard.kt` + `ui/main/SupportGate.kt`)

After any root check the main screen can offer an inline, dismissible **Support development** card that opens the shared `TipJarDialog`. `SupportGate` is the pure, unit-tested decision logic (`SupportGateTest`), a sibling of `ReviewGate`.

**The two asks count different things.** `SupportGate.MIN_CHECKS` (5) counts every check, whatever it found. `ReviewGate.MIN_ROOTED_CHECKS` (3) counts only the root-found ones. A rating is worth asking for once the app has proved useful, which means finding root. A tip is worth asking for from anyone who keeps coming back, rooted device or not.

**The two asks are deliberately serialized, review first**, because Play's review card is a system-modal overlay that fires about once per release while the support card recurs:

- 5 sits **above** 3, so on a device that keeps reporting root (where every check feeds both gates) the review ask always comes first. A device that never reports root can reach the support card first, but it never becomes eligible for the review ask, so there is nothing to yield to.
- A process-lifetime `reviewRequestedThisSession` flag (a `@Volatile` companion field on `MainViewModel`, so an activity recreation can't reset it) keeps them out of the *same session*. A frame-level check wouldn't do: a card drawn behind Play's overlay would greet the user the moment they dismissed it, and read as a double ask.

The gate also requires:

- billing available (`gplay`),
- Play prices **loaded** (otherwise the card leads to a dead spinner),
- `supporterTiers` empty (never ask someone who already tipped),
- no pending update (that card is functional and time-sensitive, so it owns the slot),
- the snooze/dismissal budget unspent. Dismissing *or* opening the tip jar snoozes for `SNOOZE_MILLIS` (30 days), but only a dismissal counts toward `MAX_DISMISSALS` (3), after which the card never returns.

`MainViewModel.maybeShowSupportPrompt` runs after `maybeRequestReview` and owns its own counter, `UserPreferences.checkCount`, incremented once per check from the main screen. Checks made through an AppFunction don't count: the card they would gate lives on a screen the caller isn't looking at.

When the `check_count` key is missing, it is seeded from `rooted_check_count`, so progress already earned toward the card doesn't restart at zero. The fallback runs in the same transaction as the increment, so it is used exactly once and the two counters diverge from there.

**Two subtleties.** The gate only sets state. `MainScreen` decides to *draw* the card (`supportPromptVisible && updateStatus is None`, so an update arriving later still wins) and reports `supportCardShown` from there, so the signal never counts a card the screen didn't draw. And the card is **unreviewable in a debug build**: the `.debug` applicationId means Play Billing never returns tip products, so `productsLoaded` can't become true. The debug-gated **"Demo: support card"** overflow item calls `demoSupportPrompt()`, which bypasses the gate (like `demoUpdate`).

## Data

### App catalog (`com.iboalali.appcatalog:data`)

The About screen's "Other apps" list is loaded by `com.iboalali.appcatalog:data` from [`Android-Shared`](https://github.com/iboalali/Android-Shared) (see CLAUDE.md, "Shared code"). The library owns the feed fetch, the HTTP cache, and the bundled `assets/apps*.json` snapshots, which merge in from it. **Read that repo's `CLAUDE.md` before changing catalog behavior.** The feed schema is a cross-repo contract, and the per-URL validator rule (which stops the English fallback from producing a `304` against a localized file) is enforced there by OkHttp's cache.

This app's side: `BasicRootCheckerApplication` owns the repository as a lazy app-scoped singleton and supplies the library's `CatalogAnalytics`/`CatalogLogger` seams (the library is deliberately Hilt-free, so scoping is each app's job). `MainActivity` calls `refresh()` once at launch, and the About screen only observes.

### UserPreferences (`data/`)

DataStore Preferences store: telemetry, haptics, theme mode, last-seen version code, pending tip tokens, the rooted-check count and last review-prompt version code behind `ReviewGate`, the check count, snooze deadline and dismissal count behind `SupportGate`, and the last root check (result + timestamp, exposed as `lastRootCheck` / `recordRootCheck` and shared by the UI and AppFunctions).

## Cross-cutting

### Analytics (`analytics/`)

Thin `Analytics` object over the TelemetryDeck SDK. **This app owns only the signal vocabulary.** The lifecycle around it (the startup buffer, when the SDK starts, the ordering between them, and test-traffic detection) is `com.iboalali.telemetry:core`. Every `trackX` call goes through `TelemetryController.submit`.

TelemetryDeck starts **asynchronously** in `BasicRootCheckerApplication.onCreate` so cold start isn't blocked. The opt-out preference is read off the main thread, then `Analytics.resolveStartupPreference` is posted *back* to the main thread (`TelemetryDeck.start()` registers a lifecycle observer, so it must run there). Signals fired while the preference is being read are buffered in a bounded queue and released once it resolves: after the SDK starts, or discarded if the user opted out. The shared `TelemetryController` guarantees start-before-flush, with a test; flushing first would deliver the backlog to an uninitialized SDK and lose it silently.

The Settings toggle goes through `Analytics.setEnabled(context, enabled)`. It takes a `Context` because **opting in has to be able to start the SDK**: a session that launched opted out never started it.

`trackDeviceType` is **one-shot per process** (a `@Volatile` flag), so rotations, folds and resizes can't inflate the count.

**Synthetic traffic is flagged as test mode, not dropped.** `com.iboalali.telemetry:core`'s `TestTraffic` detects **Firebase Test Lab** and the **Play Console pre-launch report** robot, and `TelemetryController` feeds it into TelemetryDeck's `testMode` alongside `BuildConfig.DEBUG`. The documented `firebase.test.lab` setting alone reads back null on much of the pre-launch pool, so the detector also checks emulator fingerprints, all three settings namespaces (not just `System`), a spoofed-farm signature (a Pixel hardware codename on a non-Google device), and `ActivityManager.isUserAMonkey()`. Probing **fails open** (any read error counts as a real user), pinned by a test in the shared module. Why test mode and not a query filter or a client-side drop: [`telemetry-optimization.md`](telemetry-optimization.md).

Signal taxonomy and query cookbook: [`telemetry-optimization.md`](telemetry-optimization.md). General TelemetryDeck practice and the TQL reference are in the `telemetry-and-tql` skill.

### Haptics (`util/RootHaptics.kt` + `com.iboalali.haptics`)

The engine (actuator selection, the SDK tiers, the OEM workarounds) is `com.iboalali.haptics:core`. A single `RootHaptics` instance lives on `BasicRootCheckerApplication` (`rootHaptics`) and holds only this app's vocabulary: the rising-frequency "checking" ramp and the success / error / neutral result patterns, played from `MainViewModel`. It also reports the device's actuator capabilities as a signal.

The ramp needs wave-envelope support. Elsewhere it is a no-op and the user feels only the result buzz, on purpose: a substitute buzz would read as a result arriving early.

Its `haptics` engine is also provided as `LocalAppHaptics`, so the Compose tap wrappers from `com.iboalali.haptics:compose` (`rememberHapticClick` / `rememberHapticToggle` / `rememberHapticLongClick`, gated on `LocalHapticsEnabled`) and the result patterns drive one instance. That keeps a tap from cutting off a result buzz. Both honor `UserPreferences.hapticsEnabled`.

Device capability data: [`haptic-capability-queries.json`](haptic-capability-queries.json). The conventions, and why the app drives `Vibrator` rather than `View.performHapticFeedback`, are in the `haptics-conventions` skill.

### DeviceInfo (`util/`)

Helpers for the app version and the Android version name (maps API level to name via the `version_names` string array). `getAppVersionName` guards its package-manager call so Layoutlib-rendered screenshots don't crash.

### Preview utilities (`util/`)

`@PreviewLocales` (one preview per shipped locale) and `@PreviewPlayStoreListing` (Phone, 7" Tablet, 10" Tablet, dp-based, for browsing in the IDE). `com.iboalali.previews:matrix` (Android-Shared) holds the native-resolution counterparts the screenshot tests use: 3 store devices × 5 locales = 15 PNGs per screen. Each entry's `name` has no special characters, because the tool embeds it in the reference filename.

## Build configuration

- **Gradle:** Kotlin DSL with a version catalog (`gradle/libs.versions.toml`), the source of truth for every version.
- **SDK:** compile/target 37 (Android 17), min 24.
- **Kotlin:** JVM target 17. AGP 9's **built-in Kotlin** compiles the modules. The app applies the Compose and serialization plugins but no separate `org.jetbrains.kotlin.android`, and the `:baselineprofile` module applies none.
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking).
- **Product flavors:** `gplay` (Google Play services: tip jar, in-app updates, in-app review card) and `foss` (no Google services: no-op billing/update/review). Flavor-specific code lives under `app/src/gplay/` and `app/src/foss/`; unit tests run on `gplay`.
- **Compose** with the Compose BOM.
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys.
- **Material 3 Adaptive** (`androidx.compose.material3.adaptive:adaptive`, versioned separately from the Compose BOM): `currentWindowAdaptiveInfoV2` / `WindowSizeClass` (via the transitive `androidx.window:window-core`) for the width check described in [`adaptive-navigation.md`](adaptive-navigation.md).
- **AppFunctions** (`androidx.appfunctions`: `appfunctions` + `appfunctions-compiler` (KSP)) with the `com.google.devtools.ksp` plugin, version paired to Kotlin. Generated in both flavors.
- **Baseline Profiles:** a separate `:baselineprofile` module (`com.android.test`). The `androidx.baselineprofile` plugin also adds synthetic `nonMinifiedRelease` / `benchmarkRelease` build types to `:app`. `StartupBenchmarks` pairs every metric A/B, `CompilationMode.None` against `Partial(BaselineProfileMode.Require)`. `Require` is the load-bearing part: it fails the test when the profile is absent instead of quietly measuring an unprofiled build. Medians measured 2026-08-26 on the two physical test devices (gplay, 10 startup iterations, 7 scroll):

  | | cold start (TTID) | scroll `frameDurationCpuMs` P99 | `frameOverrunMs` P99 |
  |---|---|---|---|
  | SM-G766B (phone, 384dp) | 812.9 → 737.9 ms (−9.2%) | 21.6 → 18.9 ms | 9.3 → 9.0 ms |
  | SM-X356B (tablet, portrait) | 668.8 → 586.9 ms (−12.2%) | 23.2 → 14.9 ms | 10.4 → 2.9 ms |

  On the tablet, `frameOverrunMs` P50 goes from +1.9 ms to −4.8 ms: the median frame stops missing its deadline. Treat its percentiles as directional, though. The fling covers 9-10 frames per iteration against the phone's ~70, so about 65 frames back them rather than about 490. The scroll target is the Licenses list, which in portrait is the full-screen path, not the 840dp+ overlay.
- **App catalog feed** (both flavors): `com.iboalali.appcatalog:data` fetches and parses the feed (OkHttp, `kotlinx-serialization-json`). **Coil 3** (`coil-compose` + `coil-network-okhttp`) loads the remote icons.
- **In-app review** (`com.google.android.play:review-ktx`, `gplay` only).
- **Screenshot tests:** the `com.android.compose.screenshot` plugin. Under AGP 9 it needs **both** `android.experimental.enableScreenshotTest=true` in `gradle.properties` **and** `experimentalProperties["android.experimental.enableScreenshotTest"] = true` in the `android {}` block.

## Localization

Nine locales: English (`en`), German (`de`), Arabic (`ar`), Spanish (`es`), Russian (`ru`), Dutch (`nl`), Simplified Chinese (`zh-Hans`), Malay (`ms`), Tamil (`ta`). Locale config in `res/xml/app_locales_config.xml`. Every place a new language has to be named: [`adding-a-locale.md`](adding-a-locale.md). Translation notes are linked from the CLAUDE.md documentation map.

## Theming

Compose Material3 with dynamic colors (API 31+), falling back to custom light/dark color schemes in `ui/theme/Color.kt`. `com.iboalali.ui:theme` animates the cross-fade over every color role. The splash screen theme chain is in XML (`values/` and `values-v27/` themes).

## Tests

Unit tests in `app/src/test/` cover the app's pure decision logic: `RootChecker.classify` / `parseMagiskVersionCode`, the two post-check prompt gates (`ReviewGate`, `SupportGate`), `TipTier` and `AppLanguage`. The analytics startup buffering is tested in `com.iboalali.telemetry:core`. The device-dependent probes are **not** unit-tested; verify them on an emulator or device as described in [`root-provider-detection-gaps.md`](root-provider-detection-gaps.md).

AppFunctions are verified on a connected device (API 36+):

```bash
adb shell cmd app_function list-app-functions --package <id>
adb shell cmd app_function execute-app-function \
  --function 'com.iboalali.basicrootchecker.appfunctions.BaseRootAppFunctionService#<fn>' \
  --parameters '{}'
```

Note the entry-point host `BaseRootAppFunctionService`, not the generated `RootAppFunctionService`.
