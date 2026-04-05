# Basic Root Checker

A simple Android app that checks whether your device has root access. Displays device info (model, marketing name, Android version) alongside root status.

## Features

- Root detection powered by [libsu](https://github.com/topjohnwu/libsu)
- Device info display (model, marketing name, Android version)
- Material 3 theming with dynamic colors
- Splash screen with custom exit animation
- Localized in English, German, and Arabic

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

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** ViewModel + StateFlow + Coroutines
- **Navigation:** [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (`androidx.navigation3`)
- **Root detection:** [libsu](https://github.com/topjohnwu/libsu) (`Shell.isAppGrantedRoot()`)
- **Device names:** [DeviceMarketingNames](https://github.com/nicoaccessmedia/DeviceMarketingNames)
- **Build system:** Gradle with Kotlin DSL and version catalog
