# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Assistants and other apps can now check this device's root status without opening the app, via Android AppFunctions (Android 16+): run a fresh root check, see the last check and when it was taken, or request root access. Available in both Google Play and FOSS builds.
- New **Theme** option in Settings (above **Language**): force light or dark mode, or follow the system (default). The choice persists across launches, and switching cross-fades the colors smoothly instead of snapping.
- New **Reset analytics identity** option in Settings (under the anonymous-usage-data switch): generate a fresh anonymous ID so future usage data can't be linked to anything shared before. Shown only while sharing usage data is enabled.

### Changed
- Redesigned the Settings screen so the options read as one connected group: the first and last items keep their rounded outer corners while the corners where items meet are squared off, and the items sit closer together.
- Moved the tip jar (support development) to the top of Settings.

### Fixed
- Fixed a rare crash when navigating back: pressing back twice in quick succession on Settings, About, or Licences (or a fast back-swipe) could close the app with an error instead of returning to the main screen.
- Detect more root managers that were previously reported as not rooted — Kitsune Mask, SukiSU Ultra, ReSukiSU, KernelSU Next, and legacy managers (SuperSU, KingRoot, Superuser, phh) — and show each by its own name instead of a generic "Magisk"/"KernelSU"/"Other" label.
- On the "no root" result, the app now notes that hidden or renamed root managers may need access granted first, and offers a button to request it.
- Accessibility: screen readers now announce the main-screen overflow (more options) button and the back buttons on Settings, About, and Licences, which were previously unlabeled.

## [2.3] - 2026-06-08

### Added
- Tip jar in Settings: leave an optional tip through Google Play, and tip again whenever you like (Google Play builds only; not shown in FOSS builds).
- In-app language picker in Settings (Android 13+): tap **Language** to switch the app between English, German, Arabic, Spanish, and Russian, or follow the system default. The choice is applied immediately, persists across launches, and stays in sync with Android's per-app language setting.

### Changed
- The app now starts faster, especially on slower devices: analytics initialization no longer blocks the launch sequence.
- The back-swipe animation now follows the side you swipe from: swiping from the left edge slides the screen off to the right, swiping from the right edge slides it off to the left.

### Fixed
- The current screen is now preserved when the app is recreated — for example after switching the in-app language or rotating the device — instead of jumping back to the main screen.
- Fixed a startup crash on Android 7.x and other devices below Android 8.0 where the app closed immediately on launch. Enabled core library desugaring so the modern date/time APIs used by the analytics SDK resolve on older Android versions.

## [2.2] - 2026-05-29

### Added
- Haptic feedback for the root check, toggleable with a new "Haptic feedback" switch in Settings (on by default). Every device gives a short outcome buzz — a positive "dot-daaat" when rooted, a "dot-dot" when not rooted or unknown, and a single soft pulse when a root provider is detected but access has not yet been granted. Devices with advanced haptic actuators additionally feel a rising-frequency vibration while the check runs.
- Debug builds only: developer demo modes for exercising the UI without the real conditions. The root-check button opens a picker that forces any result (rooted with each provider, not granted, not rooted, unknown) through the real check flow, and an overflow-menu "Demo: in-app update" entry drives the update card through its states (available, animated downloading, downloaded, failed). Not included in release builds.
- Russian (`ru`) localization, selectable from the Android 13+ per-app language picker alongside English, German, and Arabic.
- Spanish (`es`) localization using neutral Spanish, also selectable from the per-app language picker.
- Email, Mastodon, and Bluesky contact links on the About screen, grouped with the website link inside a single tonal card.
- App settings (the telemetry preference) are now included in Android's automatic backup, so they are restored after reinstalling the app or moving to a new device.

### Changed
- Privacy policy link now points to the dedicated `iboalali.com/app/basic_root_checker/privacy` page instead of the in-page section on the product page.
- Main screen overflow menu items now show leading icons next to Licences, Settings, and About.
- About screen contact section has been redesigned as a grouped icon-led list instead of plain auto-linked text.

### Fixed
- Translated the "Content Copied" toast and the marketing-name / model-name / Android-version accessibility labels in German and Arabic, which had been left as English fallbacks.

## [2.1] - 2026-05-17

### Added
- Root provider detection: when the device is rooted, the app now identifies whether root is provided by [Magisk](https://github.com/topjohnwu/Magisk), [KernelSU](https://github.com/tiann/KernelSU), [APatch](https://github.com/bmax121/APatch), or an unknown superuser, and displays the provider on the result card.
- Magisk version detection — shown alongside the provider when the app has been granted root.
- Generic `su` binary detection across the common system paths, so devices rooted without one of the recognized managers are still detected.
- "Request Root access" action — when a root provider is detected but the app has not yet been granted access, the result card surfaces a button that triggers the superuser allow dialog directly from the app.
- Error telemetry: previously silent failures inside the root probes and unhandled crashes are now reported via [TelemetryDeck](https://telemetrydeck.com/)'s preset-errors signal, gated by the existing in-app telemetry preference.
- Added support for Android 17 (API 37). Minimum supported version is unchanged (Android 6.0, API 23).

### Changed
- Root status now distinguishes four outcomes: *Rooted*, *Not granted*, *Not rooted*, and *Unknown*. *Not granted* is new and means a root provider was detected on the device but the app has not been granted access to it yet.
- Root probes (installed-package query, `/proc/self/mounts`, `su` binary check) now run regardless of whether libsu reports the app as already granted, closing a detection gap where rooted devices were reported as not rooted on the first check.
- Dependencies updated.
- Debug-build telemetry signals are now marked as test data so they no longer mix into production statistics.
- About and Licence screens now extend behind the navigation bar for a true edge-to-edge appearance, with the bottom system inset added as breathing room below the last item.

### Fixed
- Magisk version detection now recognizes the modern `/debug_ramdisk/.magisk` path and falls back from `magisk -v` to decoding `magisk -V` when only the numeric version code is available.
- Privacy policy menu item points to the correct URL.

### Removed
- Vendored `libs/AndroidDeviceNames` source — unused since v1.14 when device-name lookup moved to the `boehrsi/devicemarketingnames` Maven dependency.

## [2.0] - 2026-05-01

### Added
- TelemetryDeck integration for privacy-friendly analytics, with an in-app settings toggle to opt out.
- In-app update flow on the Google Play build variant, including a snackbar when an update has been installed.
- Privacy policy menu item on the main screen.
- App-referral link for the website.

### Changed
- Complete rewrite in Kotlin + Jetpack Compose, with Navigation 3, Material 3, dynamic colors, and a redesigned app-bar menu.
- Split the build into a Google Play flavor (`gplay`) and a FOSS flavor (`foss`).
- Minimum supported version raised to Android 6.0 (API 23).

## [1.14] - 2026-03-28

### Added
- Android system language settings hook (Android 13+ per-app language preference exposed in system Settings).

### Changed
- Switched device-name lookup from `jaredrummler/android-device-names` to `boehrsi/devicemarketingnames`.

### Removed
- `INTERNET` and `ACCESS_NETWORK_STATE` permissions — the app no longer needs network access.

## [1.13] - 2025-10-01

### Changed
- Internal cleanup release; no user-visible changes beyond the version bump.

## [1.12] - 2025-07-26

### Added
- Separate debug build variant with its own application ID and version suffix.

## [1.11] - 2025-07-25

### Changed
- Compiled against Android 16 (API 36).

## [1.10] - 2024-11-25

### Changed
- Dependency refresh; no user-visible changes.

## [1.9] - 2024-04-22

### Added
- New "Couldn't get root status" state shown when the root check cannot reach a definitive answer.

### Changed
- Compiled against Android 14 (API 34); minimum supported version raised to Android 4.4 (API 19).
- Switched root probe from the deprecated `Shell.rootAccess()` to `Shell.isAppGrantedRoot()`.

## [1.8] - 2023-11-25

### Added
- Initial Android system language settings hook (Android 13+ per-app language preference exposed in system Settings).
- Predictive back-gesture support.
- Monochrome launcher icon for themed icons on Android 13+.

### Changed
- Compiled against Android 13 (API 33).
- Material 3 dynamic colors applied across all screens.

### Fixed
- Dynamic theme is now applied consistently on every activity.

## [1.7] - 2022-10-09

### Added
- Pre-Android 12 splash screen support (so the custom splash now appears on older devices too).

### Changed
- Material 3 theming reworked; navigation-bar color now follows the active theme.

### Fixed
- Splash-screen theme is applied correctly on cold start.

## [1.6] - 2022-06-03

### Added
- Custom splash screen with exit animation.
- Vector-drawable support on API levels below 19.
- First-pass Material 3 theming.

### Changed
- Compiled against Android 11 (API 30); main launcher activity declared explicitly for Android 12 compatibility.
- About and Licence screens redesigned with toolbars and a single layout that scales across screen sizes.
- Main screen layout is now scrollable.
- Switched to the Noto font.

### Fixed
- Startup crash on certain devices.

## [1.5] - 2020-08-24

### Changed
- Migrated the project to AndroidX.
- Compiled against Android 10 (API 29).

## [1.4] - 2018-10-20

### Changed
- New UI and updated to the latest Android SDK.
- Replaced the SuperSU / chainfire library with `libsu` for the root check.

### Removed
- All SuperSU / chainfire code paths.

## [1.3] - 2018-06-09

### Added
- Adaptive launcher icon (Android 8.0+).

### Changed
- Compiled against Android 8.1 (API 27) with updated build tools.
- Updated device-name database.

## [1.2.2] - 2017-09-10

### Changed
- Compiled against Android 8.0 (API 26).

### Removed
- In-app advertisements.

## [1.2.1] - 2017-09-06

### Changed
- Rebuilt with the latest build tools; no user-facing changes.

## [1.2] - 2017-05-17

### Added
- Licence screen now lists the AndroidDeviceLibrary licence.
- Round launcher icon resources for Android 7.1+.

### Changed
- Compiled against Android 7.1 (API 25); updated the SuperSU library and Play Services.
- Switched to the external `DeviceName` library for marketing names.

## [1.1] - 2016-11-05

### Added
- Initial public release: detects root access via SuperSU, shows device model, marketing name, and Android version, includes an About/Licence screen.
