# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

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
- **MainViewModel** (`ui/main/`) — ViewModel with `StateFlow<MainUiState>` for root check state and device info. Uses coroutines to run root check via `RootChecker`.
- **AboutScreen** (`ui/about/`) — About screen with collapsing toolbar, app version, contact links.
- **LicenceScreen** (`ui/licence/`) — License screen with collapsing toolbar, license texts.
- **RootChecker** (`data/`) — Suspend function that runs `Shell.isAppGrantedRoot()` from [libsu](https://github.com/topjohnwu/libsu) on `Dispatchers.IO`. Returns `Boolean?`: `true` = rooted, `false` = not rooted, `null` = unknown.
- **DeviceInfo** (`util/`) — Helpers for app version retrieval and Android version name lookup (maps API level to name via `version_names` string array resource).

### Build Configuration

- **Gradle:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`)
- **SDK:** compile/target 36, min 23
- **Kotlin:** 2.3.20, JVM target 17
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking enabled)
- **Compose** enabled with Compose BOM for dependency management
- **Navigation 3** (`androidx.navigation3`) with Kotlin Serialization for route keys

### Localization

Three locales: English (`en`), German (`de`), Arabic (`ar`). Locale config in `res/xml/app_locales_config.xml`.

### Theming

Compose Material3 with dynamic colors (API 31+), fallback to custom light/dark color schemes defined in `ui/theme/Color.kt`. Splash screen theme chain in XML (`values-v21/v23/v27/v31` theme qualifiers).
