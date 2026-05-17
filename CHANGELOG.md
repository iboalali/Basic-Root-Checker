# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Root provider detection: when the device is rooted, the app now identifies whether root is provided by [Magisk](https://github.com/topjohnwu/Magisk), [KernelSU](https://github.com/tiann/KernelSU), [APatch](https://github.com/bmax121/APatch), or an unknown superuser, and displays the provider on the result card.
- Magisk version detection — shown alongside the provider when the app has been granted root.
- Generic `su` binary detection across the common system paths, so devices rooted without one of the recognized managers are still detected.
- "Request Root access" action — when a root provider is detected but the app has not yet been granted access, the result card surfaces a button that triggers the superuser allow dialog directly from the app.
- Error telemetry: previously silent failures inside the root probes and unhandled crashes are now reported via [TelemetryDeck](https://telemetrydeck.com/)'s preset-errors signal, gated by the existing in-app telemetry preference.

### Changed
- Compiles against Android 17 (API 37). Minimum supported version is unchanged (Android 6.0, API 23).
- Root status now distinguishes four outcomes: *Rooted*, *Not granted*, *Not rooted*, and *Unknown*. *Not granted* is new and means a root provider was detected on the device but the app has not been granted access to it yet.
- Root probes (installed-package query, `/proc/self/mounts`, `su` binary check) now run regardless of whether libsu reports the app as already granted, closing a detection gap where rooted devices were reported as not rooted on the first check.
- Dependencies updated.
- Debug-build telemetry signals are now marked as test data so they no longer mix into production statistics.

### Fixed
- Magisk version detection now recognizes the modern `/debug_ramdisk/.magisk` path and falls back from `magisk -v` to decoding `magisk -V` when only the numeric version code is available.
- Privacy policy menu item points to the correct URL.
