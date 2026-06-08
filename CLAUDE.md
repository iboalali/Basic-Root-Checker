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
```

Unit tests live in `app/src/test/` and currently cover `RootChecker`'s pure decision logic (`classify`) and `parseMagiskVersionCode`. The hardware-dependent probes are not unit-tested; verify them on a real rooted device.

## Architecture

Android app that checks whether a device has root access, written in Kotlin with Jetpack Compose.

**Package:** `com.iboalali.basicrootchecker`

### Key Components

- **MainActivity** — Single activity host. Sets up splash screen with custom exit animation, dynamic colors, edge-to-edge, attaches the billing and in-app-update controllers to its lifecycle, and hosts `AppRoot` via `setContent`.
- **AppRoot** (`ui/`) — Thin root overlay: a `Box` hosting `AppNavigation` plus one app-wide `SnackbarHost` for signals not tied to a single screen (e.g. a late-cleared pending tip). Per-screen Scaffolds still own their own snackbars.
- **AppNavigation** (`navigation/`) — Navigation 3 setup with `NavDisplay`, `@Serializable` route keys (`MainRoute`, `SettingsRoute`, `AboutRoute`, `LicenceRoute`), and explicit back stack management.
- **MainScreen** (`ui/main/`) — Main screen composable. Displays device info (model, marketing name, Android version) and root status. FAB triggers root check. Long-press on device info copies to clipboard.
- **MainViewModel** (`ui/main/`) — AndroidViewModel with `StateFlow<MainUiState>` for root check state, root provider + version, device info, and in-app update flow state. Exposes `checkRoot()` (passive evaluation) and `requestRoot()` (forces shell construction to trigger the superuser allow dialog), both running through `RootChecker` on coroutines.
- **SettingsScreen / SettingsViewModel** (`ui/settings/`) — Settings: telemetry and haptics toggles, the in-app language picker (Android 13+), the tip jar, and a privacy-policy link. Collects the billing layer's one-shot `TipEvent`s (Thanks/Pending/Error) as snackbars.
- **AboutScreen** (`ui/about/`) — About screen with collapsing toolbar, app version, contact links, and other apps card (`OtherAppsCard`).
- **LicenceScreen** (`ui/licence/`) — License screen with collapsing toolbar, license texts.
- **RootChecker** (`data/`) — Two suspend entry points on `Dispatchers.IO`: `check(context)` evaluates passively; `requestRoot(context)` executes `Shell.cmd("id")` first to force libsu's main shell to construct (which triggers the Magisk/KernelSU/APatch allow dialog) before re-evaluating. Both return a `RootResult` sealed interface (`NotRooted` / `Unknown` / `Rooted(provider, version)` / `RootedNotGranted(provider)`). Providers are classified via the `RootProvider` enum (`MAGISK` / `KERNELSU` / `APATCH` / `OTHER` / `UNKNOWN`). Unprivileged probes (installed packages declared in `<queries>`, `/proc/self/mounts` scan, `su` binary existence across standard paths) run regardless of grant state, so a device with root installed but the app not yet allowed is reported as `RootedNotGranted` rather than `NotRooted`. When granted, the Magisk version is read via `magisk -v` / `magisk -V` and the `/data/adb/magisk` etc. paths confirm the provider.
- **Billing / tip jar** (`billing/`) — `BillingController` interface with two flavor implementations: `GPlayBillingController` (`gplay/`, Google Play Billing) and `NoOpBillingController` (`foss/`, reports `isAvailable = false` so the tip jar is hidden). Each `TipTier` (SMALL/MEDIUM/LARGE) has a durable *record* product (acknowledged, kept forever) and a *repeat* product (consumed, repurchasable); `supporterTiers` is recomputed from owned records on every connect, so it survives reinstalls. One-shot `events` (`TipEvent`) drive Settings snackbars; `tipCleared` fires app-wide (via `AppRoot`) when a previously-`PENDING` tip clears — pending purchase tokens are persisted in `UserPreferences` so a clear is recognized even after process death or on the next launch (vs. the routine re-grant of an already-owned tip on every connect).
- **UserPreferences** (`data/`) — DataStore-Preferences store (telemetry, haptics, last-seen version code, pending tip tokens).
- **DeviceInfo** (`util/`) — Helpers for app version retrieval and Android version name lookup (maps API level to name via `version_names` string array resource).
- **Preview Utilities** (`util/`) — Custom preview annotations: `@PreviewLocales` (en, de, ar, es, ru) and `@PreviewPlayStoreListing` (Phone, 7" Tablet, 10" Tablet).

### Build Configuration

- **Gradle:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`), AGP 9.1.0
- **SDK:** compile/target 37 (Android 17), min 23
- **Kotlin:** 2.4.0, JVM target 17
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking enabled)
- **Product flavors:** `gplay` (Google Play services — the in-app-billing tip jar and in-app updates) and `foss` (no Google services — no-op billing/update implementations). Flavor-specific code lives under `app/src/gplay/` and `app/src/foss/`; unit tests run on the `gplay` variant (`:app:testGplayDebugUnitTest`).
- **Compose** enabled with Compose BOM for dependency management
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys

### Localization

Five locales: English (`en`), German (`de`), Arabic (`ar`), Spanish (`es`), Russian (`ru`). Locale config in `res/xml/app_locales_config.xml`.

### Theming

Compose Material3 with dynamic colors (API 31+), fallback to custom light/dark color schemes defined in `ui/theme/Color.kt`. Splash screen theme chain in XML (`values-v21/v23/v27/v31` theme qualifiers).

## Changelog

`CHANGELOG.md` at the repo root follows the [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) format with sections **Added / Changed / Deprecated / Removed / Fixed / Security**. In-flight work lives under `## [Unreleased]`; at release time that heading is renamed to `## [<version>] - <YYYY-MM-DD>` and a fresh empty `[Unreleased]` is added above it.

**Update the changelog as part of every feature, bug fix, or user-visible behavior change.** Add a bullet under the appropriate section of `[Unreleased]` in the same commit (or PR) that introduces the change. Write entries in user-facing language — no commit SHAs, no internal jargon. Skip the changelog only for pure refactors, internal docs edits, dependency-only bumps with no user impact, or build-config tweaks that don't change shipped behavior.

## Play Store release notes

Per-version store "What's new" text lives under `Play Store/Release Notes/<version>/`, one file per locale named by language (`default`, `german`, `arabic`, `spanish`, `russian` — matching the `Play Store/Listing/` convention, not BCP-47 codes). Each version folder also has:
- `play-console.txt` — all locales in Google Play's `<lang-tag>…</lang-tag>` block format (tags: `en-US`, `de-DE`, `ar`, `es-ES`, `ru-RU`) for pasting every language at once.
- `all-locales.md` — the same content as a human-readable reference.

Conventions: lead each note with `Version <x.y>:` (localized version word), then one line per highlight prefixed with `➕` (added), `🛠️` (changed), or `🔨` (fixed). Keep every locale within Play's **500-character** limit. Mirror the changelog's wording but condensed to user-facing highlights.

**Keep release notes in sync with the changelog.** When `[Unreleased]` is cut to `## [<version>] - <YYYY-MM-DD>`, create `Play Store/Release Notes/<version>/` with all five locale files plus `play-console.txt` and `all-locales.md`, distilling that version's changelog entries into the format above.
