# Future Feature Research — Basic Root Checker

A curated, scoped catalogue of feature ideas for future iterations of Basic Root Checker, grouped by category and effort. Not a commitment — a menu to pick from.

The app is a small, polished root-check utility: Compose + Material 3 + Navigation 3, gplay/foss flavors, TelemetryDeck analytics, DataStore prefs, in-app updates (gplay), 3 locales (en/de/ar), Material You. Core action is a single FAB → `Shell.isAppGrantedRoot()` from libsu, plus a device-info card and an About/Licence/Settings stack.

## Already implemented (baseline)

For reference — these exist today, so they're not in the menu below:

- Settings screen with telemetry toggle (`ui/settings/`) and DataStore (`data/UserPreferences.kt`)
- TelemetryDeck analytics with opt-in (`analytics/Analytics.kt`)
- Google Play in-app update flow (flexible, 1-day staleness) — `update/AppUpdateController.kt` (gplay) / NoOp (foss)
- Snackbar on app-updated detection via `LAST_SEEN_VERSION_CODE`
- Material You / dynamic colors (`MainActivity.kt`)
- Edge-to-edge + custom splash exit animation
- `enableOnBackInvokedCallback="true"` (predictive back enabled at manifest)
- 3-locale localization with `app_locales_config.xml` + RTL
- gplay vs foss product flavors

---

## Category A — UX polish (small, fully in scope)

Low-risk, high-value tweaks that fit the existing surface area.

### A1. Haptic feedback on key actions
- Add `LocalHapticFeedback` to: FAB tap (LongPress confirm-style), root-result reveal (Confirm or Reject types), long-press copy on device-info card.
- Use `HapticFeedbackConstants` predefined types for OS consistency.
- Files to touch: `ui/main/MainScreen.kt`. ~20 lines total.
- Reference: [Haptics in Compose — Android Developers](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)

### A2. Share device + root status as text
- Add a "Share" item to the top-bar overflow menu (next to Settings/About/Licence).
- Build an `Intent.ACTION_SEND` chooser with text like:
  > Device: Pixel 8 Pro (husky)
  > Android: Android 15 Pie
  > Root: Granted
  > — checked with Basic Root Checker
- Touches `MainScreen.kt` only; uses existing `MainUiState` fields. Trivial.
- Reference: [ShareCompat.IntentBuilder](https://developer.android.com/reference/kotlin/androidx/core/app/ShareCompat.IntentBuilder)

### A3. "Last checked" timestamp + result persistence
- Persist last result + timestamp in DataStore (extend `UserPreferences.kt` with `LAST_CHECK_RESULT`, `LAST_CHECK_TIMESTAMP_MS`).
- On `MainViewModel` init, hydrate the status card with the cached result and a small subtitle "Last checked 5 min ago" (use `DateUtils.getRelativeTimeSpanString`).
- Avoids the cold "NOT_CHECKED" empty state every launch. Fully reuses existing DataStore wiring.

### A4. Pull-to-refresh on the main screen
- `Modifier.pullToRefresh` (Material 3) wrapping the scroll column to re-trigger `viewModel.checkRoot()`.
- Natural gesture, redundant with the FAB but expected on Android in 2026.
- File: `ui/main/MainScreen.kt`.

### A5. Replace 1-second artificial delay with a min-display window
- `RootChecker.kt` currently `delay(1000)` after the actual check. Change to "ensure CHECKING state shows for at least 600 ms" via a min-duration helper so fast paths feel snappier without flashing.
- File: `data/RootChecker.kt`. ~10 lines.

### A6. Migrate raster status icons to vector
- `ic_success_c.png`, `ic_fail_c.png`, `ic_unknown_c.png` are PNG. Replace with Material Symbols (`check_circle`, `cancel`, `help`) tinted by theme — gives proper dark-mode contrast and dynamic-color tinting and shrinks the APK.
- Files: drawables + `MainScreen.kt`.

### A7. Material 3 Expressive motion polish
- Adopt the new spring-based motion APIs for the status icon transition and update card slide-in. Material 3 Expressive ships with Compose Material3 and is the design direction on Android 16+.
- Reference: [Material 3 Expressive overview](https://www.androidauthority.com/google-material-3-expressive-features-changes-availability-supported-devices-3556392/)

---

## Category B — Phone / tablet / OS surface integrations (medium, fits theme)

These plug the app into Android's broader UX surfaces.

### B1. Quick Settings Tile — "Check Root" ⭐ recommended
- A `TileService` that, on tap, runs `Shell.isAppGrantedRoot()` and updates the tile label/icon to ✓ / ✗ / ?.
- Pure system-API, no extra dependencies. Manifest: declare service with `BIND_QUICK_SETTINGS_TILE` permission.
- Survives app restart since the tile is OS-managed.
- Files: new `tile/RootCheckTileService.kt`, manifest entry, two new strings.
- Lifecycle hooks: `onClick()` runs check on a coroutine scope, `Tile.updateTile()`. ~80 LOC.
- Reference: [Custom Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles)

### B2. App Shortcuts (static + pinned) ⭐ recommended
- One static shortcut "Check root now" that launches MainActivity with an intent extra so `MainViewModel.checkRoot()` runs immediately on resume.
- Optional pinned shortcut so users can drop a 1-tap launcher icon.
- Limit to 1–2 shortcuts (best-practice cap is 4); 10-char short, 25-char long descriptions.
- Files: `res/xml/shortcuts.xml`, manifest meta-data, intent handling in `MainActivity.kt`. ~40 LOC.
- Reference: [App Shortcuts best practices](https://developer.android.com/develop/ui/views/launch/shortcuts/best-practices)

### B3. Home-screen widget via Jetpack Glance
- Single small widget showing last-known root status + tap-to-recheck.
- Add `androidx.glance:glance-appwidget` dep, `GlanceAppWidget` subclass, `AppWidgetReceiver`, XML provider info.
- Reuses the same `RootChecker` suspend function and `UserPreferences` for cached state.
- Bigger than a tile but more visible. ~150 LOC + drawable.
- Reference: [Create an app widget with Glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget)

### B4. Per-app language picker (Settings deep-link)
- Add a "Language" row in Settings that opens `Settings.ACTION_APP_LOCALE_SETTINGS` (Android 13+). `app_locales_config.xml` is already present, this just exposes it in-UI rather than relying on system Settings.
- 1 row, 1 intent. Trivial.

### B5. Adaptive layout for tablets / foldables
- The app caps cards at 600 dp which is fine, but on `WindowWidthSizeClass.EXPANDED` (tablets/foldables unfolded) you could:
  - Use `NavigationSuiteScaffold` to put nav as a rail instead of using overflow menu.
  - Use `ListDetailPaneScaffold` for About/Licence so list+detail show side by side.
- Adds `androidx.compose.material3:material3-adaptive-navigation-suite` + `material3-adaptive`.
- Higher effort but the app already has 3 secondary destinations that fit list-detail naturally.
- Reference: [About adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive)

### B6. Foldable posture awareness (low priority)
- Detect tabletop posture via `WindowInfoTracker` and split status card to top half, device-info to bottom half. Cute but not impactful for a single-screen app.

---

## Category C — Root-checking depth (medium, fits theme directly)

The app currently shows a binary granted/not-granted. The whole topic of "is this device rooted" has way more interesting surface to expose.

### C1. Detect root manager + version ⭐ recommended
- After granting, run `su -v` and `su -V` via libsu `Shell.cmd("su -v").exec()` to get the manager name + version code (e.g. "Magisk-v27.0:MAGISKSU").
- Show a chip below the status: `Magisk 27.0`, `KernelSU 0.9.5`, `APatch 10688`, or `Unknown`.
- Detection of installed manager packages as a fallback (queries needed in manifest with Android 11+ package visibility):
  - `com.topjohnwu.magisk`
  - `me.weishu.kernelsu`
  - `me.bmax.apatch`
- Reference: [libsu README](https://github.com/topjohnwu/libsu)

### C2. su binary path & properties
- Show which `su` binary path responded (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/debug_ramdisk/su`, etc.).
- Useful for power users diagnosing partial-root setups.

### C3. Bootloader / verified boot status
- Read `ro.boot.verifiedbootstate` and `ro.boot.flash.locked` via `SystemProperties` (reflective access still works on most OEMs) or via `getprop` shell.
- Surface as: Bootloader: `Locked` / `Unlocked` / `Unknown`; Verified boot: `Green` / `Yellow` / `Orange` / `Red`.
- Reference: [Verified Boot — AOSP](https://source.android.com/docs/security/features/verifiedboot)

### C4. SELinux mode
- Read `/sys/fs/selinux/enforce` (or `getenforce` via shell) — show `Enforcing` / `Permissive` / `Disabled`.
- One-liner addition to a "Security details" expandable section.

### C5. Play Integrity verdict comparison (educational angle)
- Optional card: run a Play Integrity standard request and show `MEETS_DEVICE_INTEGRITY` / `MEETS_BASIC_INTEGRITY` / `MEETS_STRONG_INTEGRITY` verdicts.
- Pedagogically interesting — shows users how Google sees their device vs raw root state. Probably fits as a separate "Device security" screen.
- gplay flavor only. Requires Play Console setup + a server-side decryption step (or local with classic request, deprecated). Non-trivial.
- Reference: [Play Integrity API](https://developer.android.com/google/play/integrity/overview)

### C6. "Security details" expandable section
- Bundles C2–C4 behind a "More details" disclosure on the device-info card so casual users aren't overwhelmed but power users can drill in.

---

## Category D — Settings expansion (small)

Builds on the existing `SettingsScreen` skeleton.

- **D1.** Auto-check root on launch (Boolean pref, default off).
- **D2.** Theme override: System / Light / Dark (DataStore + a wrapper around `BasicRootCheckerTheme`).
- **D3.** Dynamic color toggle (some users prefer the brand orange).
- **D4.** Reset settings button.
- **D5.** Build info row (versionCode, flavor, gitSha if added) — handy for bug reports.

All are 5–15 LOC each in `ui/settings/SettingsScreen.kt` + `data/UserPreferences.kt`.

---

## Category E — Out of scope (flagged, not recommended)

Listed here so they're explicitly off the menu and don't get re-suggested:

- Background `WorkManager` periodic root checks + notifications — reintroduces battery/permission complexity for a check that takes <1 s on demand.
- Crash reporting (Crashlytics / Sentry) — TelemetryDeck is already wired, app is tiny, no real value.
- Biometric lock to view root status — pure ceremony, the data is on the device anyway.
- History graph of root status over time — root status doesn't meaningfully change daily.
- Cloud sync — no cross-device data to sync.
- In-app browser / WebView — external links open in the browser, fine.
- Wear OS companion tile — major scope blow-up.

---

## Recommended top-5 ordered by ROI

A "ship next" shortlist, in order:

1. **B1 Quick Settings Tile** — biggest "wow per LOC", directly maps to the app's single core action.
2. **C1 Detect root manager + version** — turns a binary answer into actually useful info; small surface.
3. **A3 Last-checked persistence + A2 Share** — eliminates the cold empty state, adds the obvious "show off your root" share path.
4. **B2 App Shortcuts** — 1-tap entry from launcher long-press.
5. **A1 Haptics + A6 Vector status icons** — pure polish, no API surface, visible to every user.

---

## Critical files referenced

- `app/src/main/java/com/iboalali/basicrootchecker/data/RootChecker.kt` — A5, C1
- `app/src/main/java/com/iboalali/basicrootchecker/data/UserPreferences.kt` — A3, D1–D4
- `app/src/main/java/com/iboalali/basicrootchecker/ui/main/MainScreen.kt` — A1, A2, A4, C-cards
- `app/src/main/java/com/iboalali/basicrootchecker/ui/main/MainViewModel.kt` — C1, A3
- `app/src/main/java/com/iboalali/basicrootchecker/ui/settings/SettingsScreen.kt` — D1–D5, B4
- `app/src/main/AndroidManifest.xml` — B1 (TileService), B2 (shortcuts meta), B3 (widget receiver), C1 (`<queries>`)
- `app/src/main/res/values/strings.xml` — every category, localized to de/ar
- `gradle/libs.versions.toml` — Glance (B3), adaptive (B5), Play Integrity (C5)
- New: `app/src/main/java/.../tile/RootCheckTileService.kt` (B1)
- New: `app/src/main/res/xml/shortcuts.xml` (B2)
- New: `app/src/main/java/.../widget/RootStatusWidget.kt` (B3)

## Verification approach (when any of these is implemented)

- `./gradlew assembleDebug` — already the project's build path.
- `./gradlew lint` — catches manifest/dependency issues for new TileServices, shortcuts, widgets.
- Manual: install on a rooted + a non-rooted device (or emulator with Magisk) to verify each surface (tile/widget/shortcut) behaves on both root states, plus on the foss flavor where in-app updates are no-op.
- Translation pass: every new user-facing string needs `values-de` and `values-ar` entries.
