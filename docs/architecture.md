# Architecture

Android app that checks whether a device has root access. Kotlin, Jetpack Compose, single activity.

**Package:** `com.iboalali.basicrootchecker`

Navigation and the large-screen detail overlay have their own doc:
[`adaptive-navigation.md`](adaptive-navigation.md).

## Entry points and shell

- **MainActivity** — Single activity host. Sets up the splash screen with a custom exit animation,
  dynamic colors, edge-to-edge; attaches the billing, in-app-update and in-app-review controllers to
  its lifecycle; kicks off the one background `AppCatalogRepository.refresh()` for the launch; and
  hosts `AppRoot` via `setContent`. Also provides `LocalHapticsEnabled` and `LocalAppHaptics` around
  `AppRoot`.
- **AppRoot** (`ui/`) — Thin root overlay: a `Box` hosting `AppNavigation` plus one app-wide
  `SnackbarHost` for signals not tied to a single screen. Per-screen Scaffolds still own their own
  snackbars.

  It owns **both** tip flows: `tipCleared` (a late-cleared pending tip, which lands long after the
  purchase and away from Settings) and the one-shot `TipEvent`s (Thanks / Pending / Error).

  **The latter must live here, not on a screen.** `BillingController.events` is a `Channel`, so it is
  **single-consumer** — two collectors would *split* the events between them rather than duplicate
  them — and a tip can start from either Settings or the main screen's support card. At expanded width
  the secondary screens are composed as an overlay *over a live `MainScreen`*, so both collectors would
  be active at once. One consumer at the root is correct regardless of which surface opened the tip
  jar, and this host draws above the overlay card.

## Screens

- **MainScreen** (`ui/main/`) — Displays device info (model, marketing name, Android version) and root
  status. FAB triggers the root check. Long-press on device info copies to clipboard. Calls
  `ReportDrawnWhen { … }` so `timeToFullDisplay` marks the first meaningful frame (device info loads
  synchronously in the ViewModel's `init`, so it ≈ `timeToInitialDisplay`).
- **MainViewModel** (`ui/main/`) — `AndroidViewModel` with `StateFlow<MainUiState>` for root-check
  state, root provider + version, device info, in-app update flow state, and the support-card prompt.
  Exposes `checkRoot()` (passive evaluation) and `requestRoot()` (forces shell construction to trigger
  the superuser allow dialog), both running through `RootChecker` on coroutines. Plays the root-check
  haptic ramp and result patterns.
- **SettingsScreen / SettingsViewModel** (`ui/settings/`) — Telemetry and haptics toggles, the in-app
  language picker (Android 13+), the tip jar, and a privacy-policy link. Opens the shared
  `TipJarDialog` (`ui/tip/TipJar.kt`) and dismisses it on selection. The outcome snackbars are **not**
  collected here — see AppRoot.
- **AboutScreen / AboutViewModel** (`ui/about/`) — Collapsing toolbar, app version, contact links, and
  the "Other apps" card. A **"Rate this app"** link is appended to the contact links only when
  `ReviewController.isAvailable` (Google Play builds); it opens the Play listing directly, separate
  from the automatic in-app review card.

  `AboutViewModel` is **read-only** — it observes `AppCatalogRepository.otherApps` and projects each
  entry to the shared `@Immutable OtherApp`. Filtering this app out of its own list is no longer done
  here: it moved into the repository, which derives the running package itself (matching on the release
  `applicationId`, so the `.debug` suffix is stripped first). The catalog fetch is owned by
  `MainActivity`.

  The screen splits into `AboutScreen` (wires the VM) and an `internal AboutScreenContent` (stateless,
  so the screenshot test can render it — the `screenshotTest` source set can only see
  `public`/`internal`).
- **LicenseScreen** (`ui/license/`) — Collapsing toolbar, license texts.

### Other apps card (`ui/about/OtherAppsCard.kt`)

The **card** is this app's; the **rows in it** are `com.iboalali.appcatalog:ui`'s `OtherAppRow`. What
stays here is the outlined card, its title and its dividers, plus the three seams the library
deliberately doesn't own — the haptic tick and `Analytics.trackOtherAppClicked` (both through one
`onAction` callback, which fires before the intent) and this app's bundled icons (through
`fallbackIcon`). The only visual override is `contentPadding`, because the card already pads
horizontally. `util/InlineMarkdown.kt` moved to the library with the rows.

Each row: icon (remote via Coil, with the local fallback above), name, localized description,
highlights bullets (inline Markdown — `**bold**` / `*italic*` only, no Markdown dependency), actions.

- An installable app shows **Open** when a launch intent resolves, else **Install** (Play listing).
- Any entry with a website also gets **Website**.
- A web-only entry (no `packageName`) shows a single button: **Open** when the site is installed as a
  **PWA/WebAPK**, else **Website**.

The PWA case is detected by matching the shared `org.chromium.webapk.shell_apk.` shell *activity
class* rather than a package name, because WebAPK packages differ per browser and an *unverified*
WebAPK isn't picked up by `ACTION_VIEW` link routing. Both lookups need the `<intent>` entries in a
`<queries>` block — which now also ship in the library's own manifest and merge in, so a consumer
can't forget them; this app's are a harmless duplicate.

`localIconFor`'s `else` branch names this app's `ic_baseline_android_24` rather than returning null.
Returning null would hand the row the library's generic icon, which is a *different drawable* — the
Material Symbols one Billboard uses. Leaving it null silently changed the art here, and the screenshot
test is what caught it.

The card hides itself when the list is empty.

## Root detection

**RootChecker** (`data/`) — Two suspend entry points on `Dispatchers.IO`:

- `check(context)` evaluates passively.
- `requestRoot(context)` executes `Shell.cmd("id")` first to force libsu's main shell to construct
  (which triggers the Magisk/KernelSU/APatch allow dialog) before re-evaluating.

Both return a `RootResult` sealed interface (`NotRooted` / `Unknown` / `Rooted(provider, version)` /
`RootedNotGranted(provider)`). Providers are classified via the `RootProvider` enum (`MAGISK` /
`KERNELSU` / `APATCH` / `OTHER` / `UNKNOWN`).

Unprivileged probes — installed packages declared in `<queries>`, a `/proc/self/mounts` scan, and `su`
binary existence across standard paths — run regardless of grant state, so a device with root
installed but the app not yet allowed is reported as `RootedNotGranted` rather than `NotRooted`. When
granted, the Magisk version is read via `magisk -v` / `magisk -V`, and the `/data/adb/magisk` etc.
paths confirm the provider.

Both entry points take an `applyUiDelay` flag (default `true`; AppFunctions pass `false` to skip the
~1 s UI settle delay) and record each result via `UserPreferences.recordRootCheck`, so UI (FAB) and
AppFunction callers share one "last checked" value.

Known detection gaps: [`root-provider-detection-gaps.md`](root-provider-detection-gaps.md).

## AppFunctions (`appfunctions/`)

Exposes the root-check workflows to the Android system and on-device agents (e.g. Gemini) via
`androidx.appfunctions`. Ships in both flavors (no Google dependency), release included.

Three functions:

| Function | Behavior |
|---|---|
| `checkRootStatus` | fresh passive check |
| `requestRootAccess` | triggers the superuser dialog |
| `getLastRootCheck` | returns the last cached check + `checkedAt` without re-probing |

Each takes no parameters, receives the service's `applicationContext` from the adapter, and maps the
sealed `RootResult` to the flat `@AppFunctionSerializable` `RootStatus`.

**None of the three declares an `AppFunctionContext` parameter, and none may** — see the trap in
[`../CLAUDE.md`](../CLAUDE.md). v2.5 briefly did, which broke all three at runtime while every local
check stayed green.

**Wiring.** The `@AppFunction`s and their agent-facing KDoc live on the abstract
`BaseRootAppFunctionService : AppFunctionService()` (`RootAppFunctionService.kt`), annotated
`@AppFunctionServiceEntryPoint(serviceName = "RootAppFunctionService", appFunctionXmlFileName = "root_app_function_service")`
plus `@RequiresApi(36)`. Each method is a thin adapter delegating to `RootAppFunctions`, which stays a
plain framework-annotation-free class so it's testable without the service lifecycle.

KSP (`appfunctions-compiler`) generates the concrete `RootAppFunctionService` **and**
`assets/root_app_function_service.xml`, both declared in `AndroidManifest.xml`. The `<service>` carries
the `BIND_APP_FUNCTION_SERVICE` permission, the `android.app.appfunctions.schema` / `.v2`
`<property>`s, and the `android.app.appfunctions.AppFunctionService` intent-filter.
`res/xml/app_metadata.xml` (the app-level `android.app.appfunctions.app_metadata` `<property>`) is the
LLM-facing app description.

General mechanics — the alpha10 artifact merge, the R8 keep rule, `adb` verification — are in the
`appfunctions-wiring` skill. The R8 rule matters here specifically: verify after a release build with
`grep BaseRootAppFunctionService app/build/outputs/mapping/<variant>/mapping.txt`; the class and all
three method names must map to themselves.

## Monetization and prompts

### Billing / tip jar (`billing/`)

`BillingController` interface with two flavor implementations: `GPlayBillingController` (`gplay/`,
Google Play Billing) and `NoOpBillingController` (`foss/`, reports `isAvailable = false` so the tip jar
is hidden).

Each `TipTier` (SMALL / MEDIUM / LARGE) has a durable *record* product (acknowledged, kept forever) and
a *repeat* product (consumed, repurchasable). `supporterTiers` is recomputed from owned records on
every connect, so it survives reinstalls.

One-shot `events` (`TipEvent`) and `tipCleared` are both collected app-wide by `AppRoot` (see there for
why `events` must have exactly one consumer). `tipCleared` fires when a previously-`PENDING` tip
clears — pending purchase tokens are persisted in `UserPreferences` so a clear is recognized even after
process death or on the next launch, as distinct from the routine re-grant of an already-owned tip on
every connect.

### In-app review / rating (`review/` + `ui/main/ReviewGate.kt`)

`ReviewController` interface with two flavor implementations, mirroring the billing/update split:
`GPlayReviewController` (`gplay/`, the Play-managed in-app review card) and `NoOpReviewController`
(`foss/`, `isAvailable = false`). Attached to `MainActivity`'s lifecycle.

**`ReviewGate` is the pure, unit-tested decision logic** (`ReviewGateTest`): eligible once root has been
confirmed `MIN_ROOTED_CHECKS` (3) times **and** the prompt hasn't already fired on this or a later
version code — roughly once per release, on top of Play's own quota.
`MainViewModel.maybeRequestReview` runs it after any `RootResult.Rooted` (confirming root is the app's
"win" moment).

**Gating rules that are easy to get wrong.** The version code is recorded — spending the release's
single prompt — *only* once the request actually reached Play. So (a) FOSS returns early on
`!isAvailable` without even counting toward the gate, and (b) `requestReview()` returns `Boolean` and
reports `false` when no activity is attached (a check finishing mid-recreation), leaving the slot
unspent.

Play gives **no "was it shown" callback**, so a `true` return means "handed to Play", never "a card
appeared" — don't build on it.

### Post-check asks: support card (`ui/main/SupportCard.kt` + `ui/main/SupportGate.kt`)

After a root-found check the main screen can offer an inline, dismissible **Support development** card
that opens the shared `TipJarDialog`. `SupportGate` is the pure, unit-tested decision logic
(`SupportGateTest`), a sibling of `ReviewGate`.

**The two post-check asks are deliberately serialized, review first**, because Play's review card is a
system-modal overlay that fires roughly once per release while this one recurs:

- `SupportGate.MIN_ROOTED_CHECKS` (5) sits **above** `ReviewGate.MIN_ROOTED_CHECKS` (3), so a fresh
  install always reaches the review ask first.
- A process-lifetime `reviewRequestedThisSession` flag (a `@Volatile` companion field on
  `MainViewModel`, so an activity recreation can't reset it) keeps them out of the *same session*. A
  frame-level check wouldn't do: a card drawn behind Play's overlay would greet the user the moment
  they dismissed it, and read as a double-ask.

The gate also requires:

- billing available (`gplay`),
- Play prices **loaded** (otherwise the card leads to a dead spinner),
- `supporterTiers` empty (never ask someone who already tipped),
- no pending update (that card is functional and time-sensitive, so it owns the slot),
- the snooze/dismissal budget unspent — dismissing *or* opening the tip jar snoozes for
  `SNOOZE_MILLIS` (30 days), but only a dismissal counts toward `MAX_DISMISSALS` (3), past which the
  card never returns.

`MainViewModel.maybeShowSupportPrompt` consumes the rooted count that `maybeRequestReview` already
observed, so the counter increments exactly once per check.

**Two subtleties.** The gate only sets state — `MainScreen` decides to *draw* it
(`supportPromptVisible && updateStatus is None`, so an update arriving later still wins) and reports
`supportCardShown` from there, keeping the analytics signal honest about cards the screen never drew.
And the card is **unreviewable in a debug build**: the `.debug` applicationId means Play Billing never
returns tip products, so `productsLoaded` can't become true — hence the debug-gated **"Demo: support
card"** overflow item calling `demoSupportPrompt()`, which bypasses the gate (mirroring `demoUpdate`).

## Data

### App catalog — moved out of this repo

`data/catalog/` is **gone**. The About screen's "Other apps" list is now loaded by
`com.iboalali.appcatalog:data` in the shared
[`Android-Shared`](https://github.com/iboalali/Android-Shared) build, consumed as a Gradle composite
build (see CLAUDE.md → "Shared code"). The five bundled `assets/apps*.json` snapshots went with it and
merge back in from the library, ending a hand-sync across three repos.

Most of that module started here. The OkHttp `CatalogHttpSource` with a real HTTP `Cache`, its seven
MockWebServer tests, and the `Mutex` + `networkAppliedKey` guard that stops a cache seed publishing
over a fresher network result were all written in this repo; the merged implementation is this app's,
plus the two cleanups and the `@JsonNames("whatsNew")` alias the other apps held. **Read that repo's
`CLAUDE.md` before changing catalog behaviour** — the feed schema is a cross-repo contract now, and the
per-URL validator rule that keeps the English fallback from producing a `304` against a localized file
is enforced there by OkHttp's cache rather than by code here.

What remains on this side: `BasicRootCheckerApplication` owns the repository as a lazy app-scoped
singleton and supplies the library's `CatalogAnalytics`/`CatalogLogger` seams (the library is
deliberately Hilt-free, so scoping is each app's job); `MainActivity` calls `refresh()` once at launch;
the About screen only ever observes. Self-filtering — excluding this app from its own list — moved down
into the repository, which derives the running package itself.

### UserPreferences (`data/`)

DataStore-Preferences store: telemetry, haptics, theme mode, last-seen version code, pending tip
tokens, the rooted-check count + last review-prompt version code backing `ReviewGate`, the support-card
snooze deadline + dismissal count backing `SupportGate`, and the last root check (result + timestamp,
exposed as `lastRootCheck` / `recordRootCheck` and shared by the UI and AppFunctions).

## Cross-cutting

### Analytics (`analytics/`)

Thin `Analytics` object over the TelemetryDeck SDK. **This app owns only the signal vocabulary** — the
lifecycle around it (the startup buffer, when the SDK starts, and the ordering between them) moved to
`com.iboalali.telemetry:core`, along with `SignalGate` and the old `util/TestEnvironment`. Every
`trackX` call routes through `TelemetryController.submit`.

TelemetryDeck is initialized **asynchronously** in `BasicRootCheckerApplication.onCreate` so cold start
isn't blocked: the opt-out preference is read off the main thread, then `Analytics.resolveStartupPreference`
is posted *back* to the main thread (`TelemetryDeck.start()` registers a lifecycle observer, so it must
run there). Signals fired while the preference is still being read are buffered in a bounded queue and
released once it resolves — after the SDK starts, or discarded if the user opted out.

**That ordering is no longer this repo's to get right.** Flushing before `start()` would deliver the
backlog to an uninitialized SDK and lose it silently; it is now a property of the shared
`TelemetryController` with a test covering it, rather than a rule spread across this file and the
`Application`.

The Settings toggle goes through `Analytics.setEnabled(context, enabled)`. It takes a `Context`
because **opting in has to be able to start the SDK** — a session that launched opted-out never did.
Before the move this app only reopened its signal buffer here, so opting in from Settings left
telemetry silently dead until the next launch; the shared controller fixes that.

`trackDeviceType` is **one-shot per process** (a `@Volatile` flag), so the rotations / folds / resizes
it's read through can't inflate the count.

Signal taxonomy and query cookbook: [`telemetry-optimization.md`](telemetry-optimization.md). General
TelemetryDeck practice and the TQL language reference are in the `telemetry-and-tql` skill.

### Test-traffic detection — moved out of this repo

`util/TestEnvironment.kt` is **gone**. Detecting synthetic, non-user runs — **Firebase Test Lab** and
the **Play Console pre-launch report** robot — now lives in `com.iboalali.telemetry:core` as
`TestTraffic`, and the shared `TelemetryController` feeds it into TelemetryDeck's `testMode` alongside
`BuildConfig.DEBUG`.

**This app gained coverage in the move.** Its own version read only the documented `firebase.test.lab`
system setting, which reads back null on much of the pre-launch pool — so that traffic was being
counted as real users here. The shared detector adds emulator fingerprints, all three settings
namespaces rather than just `System`, a spoofed-farm check (a Pixel hardware codename on a non-Google
device), and `ActivityManager.isUserAMonkey()`.

**Flagged as test-mode rather than dropped:** a test-mode signal is segregated out of the production
view (including the premade dashboards) yet stays inspectable via the dashboard's Test Mode toggle, so
a detection false-positive merely misfiles a real user's data (recoverable) instead of destroying it.
It has to be decided client-side at signal time because TelemetryDeck has no global/app-level query
filter. Probing **fails open** (any read error → treated as a real user), and that is pinned by a test
in the shared module.

### Haptics (`util/` + `ui/`)

Two layers, both gated on `UserPreferences.hapticsEnabled`. A single `RootHaptics` (`util/`) instance
lives on `BasicRootCheckerApplication` (`rootHaptics`) and is shared by the root-check flow and the UI
tap feedback.

It plays the rich root-check vibrations from `MainViewModel`: a rising-frequency "checking" ramp plus
distinct success / error / neutral result patterns, with graceful fallback across the vibration APIs
(PWLE envelopes on API 36+ → amplitude waveforms on API 26+ → legacy patterns on 23–25). It reads the
preference before playing.

It also exposes `playTap` / `playLongPress` for UI feedback. `HapticClick` (`ui/`) wires those to
Compose via `rememberHapticClick` / `rememberHapticToggle` / `rememberHapticLongClick`, gated on the
`LocalHapticsEnabled` CompositionLocal.

Device capability data: [`haptic-capability-queries.json`](haptic-capability-queries.json). The
conventions and the reasoning behind driving `Vibrator` rather than `View.performHapticFeedback` are in
the `haptics-conventions` skill.

### DeviceInfo (`util/`)

Helpers for app version retrieval and Android version name lookup (maps API level to name via the
`version_names` string array resource). `getAppVersionName` guards its package-manager call so
Layoutlib-rendered screenshots don't crash.

### Preview utilities (`util/`)

`@PreviewLocales` (en, de, ar, es, ru) and `@PreviewPlayStoreListing` (Phone, 7" Tablet, 10" Tablet).
`com.iboalali.previews:matrix` (Android-Shared) holds the native-resolution counterparts used by the
screenshot tests —
3 store devices × 5 locales = 15 PNGs per screen. Each entry's `name` is kept free of special
characters so the tool embeds it in the reference filename.

## Build configuration

- **Gradle:** Kotlin DSL with a version catalog (`gradle/libs.versions.toml`), AGP 9.3.1.
- **SDK:** compile/target 37 (Android 17), min 23.
- **Kotlin:** 2.4.10, JVM target 17. AGP 9's **built-in Kotlin** compiles the modules; the app applies
  the Compose/serialization plugins but no separate `org.jetbrains.kotlin.android`, and the
  `:baselineprofile` module applies none.
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification
  + resource shrinking).
- **Product flavors:** `gplay` (Google Play services — tip jar, in-app updates, in-app review card) and
  `foss` (no Google services — no-op billing/update/review). Flavor-specific code lives under
  `app/src/gplay/` and `app/src/foss/`; unit tests run on `gplay`.
- **Compose** with the Compose BOM.
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys.
- **Material 3 Adaptive** (`androidx.compose.material3.adaptive:adaptive`, versioned separately from
  the Compose BOM) — `currentWindowAdaptiveInfoV2` / `WindowSizeClass` (via the transitive
  `androidx.window:window-core`) for the width check described in
  [`adaptive-navigation.md`](adaptive-navigation.md).
- **AppFunctions** (`androidx.appfunctions` 1.0.0-alpha10: `appfunctions` + `appfunctions-compiler`
  (KSP)) with the `com.google.devtools.ksp` plugin, version paired to Kotlin. Generated in both
  flavors.
- **Baseline Profiles:** a separate `:baselineprofile` module (`com.android.test`); the
  `androidx.baselineprofile` plugin also adds synthetic `nonMinifiedRelease` / `benchmarkRelease` build
  types to `:app`.
- **App catalog feed** (both flavors) — `kotlinx-serialization-json` parses the feed, and **Coil 3**
  (`coil-compose` + `coil-network-okhttp`) loads the remote icons. Note `coil-network-okhttp` pulls
  **OkHttp** in transitively; the catalog's own fetch deliberately uses plain `HttpURLConnection`, so
  OkHttp is currently Coil's alone.
- **In-app review** (`com.google.android.play:review-ktx`, `gplay` only).
- **Screenshot tests:** the `com.android.compose.screenshot` plugin. Needs **both**
  `android.experimental.enableScreenshotTest=true` in `gradle.properties` **and**
  `experimentalProperties["android.experimental.enableScreenshotTest"] = true` in the `android {}`
  block under AGP 9.

## Localization

Five locales: English (`en`), German (`de`), Arabic (`ar`), Spanish (`es`), Russian (`ru`). Locale
config in `res/xml/app_locales_config.xml`. Translation notes:
[`russian-translation-notes.md`](russian-translation-notes.md),
[`spanish-translation-notes.md`](spanish-translation-notes.md).

## Theming

Compose Material3 with dynamic colors (API 31+), falling back to custom light/dark color schemes in
`ui/theme/Color.kt`. Splash screen theme chain in XML (`values-v21/v23/v27/v31` theme qualifiers).

## Tests

Unit tests live in `app/src/test/` and cover the app's pure decision logic — `RootChecker.classify` /
`parseMagiskVersionCode`, and the two post-check prompt gates (`ReviewGate`, `SupportGate`). The
analytics startup buffering used to be tested here too; those tests moved with `SignalGate` into
`com.iboalali.telemetry:core`. The hardware-dependent probes are **not** unit-tested; verify them
on a real rooted device.

AppFunctions are verified on a connected device (API 36+):

```bash
adb shell cmd app_function list-app-functions --package <id>
adb shell cmd app_function execute-app-function \
  --function 'com.iboalali.basicrootchecker.appfunctions.BaseRootAppFunctionService#<fn>' \
  --parameters '{}'
```

Note the entry-point host `BaseRootAppFunctionService`, not the generated `RootAppFunctionService`.
