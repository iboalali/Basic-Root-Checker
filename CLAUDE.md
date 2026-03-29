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

Android app that checks whether a device has root access, written in Java with Data Binding.

**Package:** `com.iboalali.basicrootchecker`

### Key Components

- **MainActivity** — Launcher activity. Displays device info (model, marketing name, Android version) and root status. Uses `RootChecker` via `RootCheckerContract` callback interface. Implements splash screen with custom exit animation.
- **RootChecker** (`components/`) — Runs `Shell.isAppGrantedRoot()` from [libsu](https://github.com/topjohnwu/libsu) on a single-threaded executor. Returns `Boolean`: `true` = rooted, `false` = not rooted, `null` = unknown.
- **RootCheckerContract** (`components/`) — Callback interface with `onPreExecute()` and `onResult(Boolean)`.
- **Utils** — Static helpers: app version retrieval and Android version name lookup (maps API level to name via `version_names` string array resource).
- **AboutActivity / LicenceActivity** — Simple info screens with collapsing toolbars.

### Build Configuration

- **Gradle:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`)
- **SDK:** compile/target 36, min 21
- **Java:** 17
- **Build variants:** debug (appId suffix `.debug`, version suffix `-debug`) and release (minification + resource shrinking enabled)
- **Data Binding** enabled — layouts generate binding classes (e.g., `ActivityMainBinding`)

### Localization

Three locales: English (`en`), German (`de`), Arabic (`ar`). Locale config in `res/xml/app_locales_config.xml`.

### Theming

Material3 with dynamic colors. Multiple theme resource qualifiers for API 21/23/27/31+. Custom font: Noto Sans Regular.
