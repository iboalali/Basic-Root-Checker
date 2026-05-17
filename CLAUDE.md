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
```

There are no tests in this project.

## Architecture

Android app that checks whether a device has root access, written in Kotlin with Jetpack Compose.

**Package:** `com.iboalali.basicrootchecker`

### Key Components

- **MainActivity** — Single activity host. Sets up splash screen with custom exit animation, dynamic colors, edge-to-edge, and hosts Compose UI via `setContent`.
- **AppNavigation** (`navigation/`) — Navigation 3 setup with `NavDisplay`, `@Serializable` route keys (`MainRoute`, `AboutRoute`, `LicenceRoute`), and explicit back stack management.
- **MainScreen** (`ui/main/`) — Main screen composable. Displays device info (model, marketing name, Android version) and root status. FAB triggers root check. Long-press on device info copies to clipboard.
- **MainViewModel** (`ui/main/`) — AndroidViewModel with `StateFlow<MainUiState>` for root check state, root provider + version, device info, and in-app update flow state. Exposes `checkRoot()` (passive evaluation) and `requestRoot()` (forces shell construction to trigger the superuser allow dialog), both running through `RootChecker` on coroutines.
- **AboutScreen** (`ui/about/`) — About screen with collapsing toolbar, app version, contact links, and other apps card (`OtherAppsCard`).
- **LicenceScreen** (`ui/licence/`) — License screen with collapsing toolbar, license texts.
- **RootChecker** (`data/`) — Two suspend entry points on `Dispatchers.IO`: `check(context)` evaluates passively; `requestRoot(context)` executes `Shell.cmd("id")` first to force libsu's main shell to construct (which triggers the Magisk/KernelSU/APatch allow dialog) before re-evaluating. Both return a `RootResult` sealed interface (`NotRooted` / `Unknown` / `Rooted(provider, version)` / `RootedNotGranted(provider)`). Providers are classified via the `RootProvider` enum (`MAGISK` / `KERNELSU` / `APATCH` / `OTHER` / `UNKNOWN`). Unprivileged probes (installed packages declared in `<queries>`, `/proc/self/mounts` scan, `su` binary existence across standard paths) run regardless of grant state, so a device with root installed but the app not yet allowed is reported as `RootedNotGranted` rather than `NotRooted`. When granted, the Magisk version is read via `magisk -v` / `magisk -V` and the `/data/adb/magisk` etc. paths confirm the provider.
- **DeviceInfo** (`util/`) — Helpers for app version retrieval and Android version name lookup (maps API level to name via `version_names` string array resource).
- **Preview Utilities** (`util/`) — Custom preview annotations: `@PreviewLocales` (en, de, ar) and `@PreviewPlayStoreListing` (Phone, 7" Tablet, 10" Tablet).

### Build Configuration

- **Gradle:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`), AGP 9.1.0
- **SDK:** compile/target 37 (Android 17), min 23
- **Kotlin:** 2.3.20, JVM target 17
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking enabled)
- **Compose** enabled with Compose BOM for dependency management
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys

### Localization

Three locales: English (`en`), German (`de`), Arabic (`ar`). Locale config in `res/xml/app_locales_config.xml`.

### Theming

Compose Material3 with dynamic colors (API 31+), fallback to custom light/dark color schemes defined in `ui/theme/Color.kt`. Splash screen theme chain in XML (`values-v21/v23/v27/v31` theme qualifiers).
