# Basic Root Checker

A simple Android app that checks whether your device has root access. Displays device info (model, marketing name, Android version) alongside root status.

## Features

- Root detection powered by [libsu](https://github.com/topjohnwu/libsu)
- Magisk detection with version (when rooted)
- Detects Magisk, KernelSU, and APatch even when the app has not yet been granted root, with an in-app "Request Root access" action
- Device info display (model, marketing name, Android version)
- In-app settings: language picker (Android 13+), telemetry toggle, and haptic feedback
- Optional tip jar to support development (Google Play builds)
- Material 3 theming with dynamic colors
- Splash screen with custom exit animation
- Localized in English, German, Arabic, Spanish, and Russian

## Requirements

- Android 6.0+ (API 23)
- JDK 17

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the release history.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** ViewModel + StateFlow + Coroutines
- **Navigation:** [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (`androidx.navigation3`) with Kotlin Serialization for type-safe routes
- **Root detection:** [libsu](https://github.com/topjohnwu/libsu) (`Shell.isAppGrantedRoot()`) plus unprivileged heuristics (package query, `/proc/self/mounts`, `su` binary search) for Magisk, KernelSU, and APatch
- **Device names:** [DeviceMarketingNames](https://github.com/nicoaccessmedia/DeviceMarketingNames)
- **In-app billing:** Google Play Billing (tip jar) in the `gplay` flavor; a no-op in `foss`
- **Build system:** Gradle with Kotlin DSL and version catalog (AGP 9.1.0)
