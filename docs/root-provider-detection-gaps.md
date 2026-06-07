# Fix Plan: Root Provider Detection Gaps

## Background

A user reported that their device with **Kitsune Mask** installed and root active was
reported by the app as **not rooted**. Investigating that report surfaced a broader issue:
the app only fingerprints a subset of the root solutions in use in 2026, so several
mainstream (and legacy) providers go undetected — especially *before* the user grants
root to this app.

## What the app recognizes today

From `MAGISK_PACKAGES` / `KERNELSU_PACKAGES` / `APATCH_PACKAGES` (`RootChecker.kt:74-88`)
and the matching `<queries>` block (`AndroidManifest.xml:8-16`):

| Provider | Package id(s) |
| --- | --- |
| Magisk | `com.topjohnwu.magisk` (+ `.debug` / `.canary` / `.alpha`) |
| KernelSU | `me.weishu.kernelsu` |
| KernelSU-Next | `com.rifsxd.ksunext` |
| APatch | `me.bmax.apatch` |

Plus a generic `su` binary check across standard paths (`SU_PATHS`,
`RootChecker.kt:90-102`) that maps to `RootProvider.OTHER`.

## Root Cause

Detection fails in the **"root installed but not yet granted to this app"** path — the
`granted == false || granted == null` branch of `classify` (`RootChecker.kt:49-62`). In
that branch, the only signals available are the unprivileged probes in `collectSignals`
(`RootChecker.kt:119`): package presence, a `/proc/self/mounts` scan, and `su`-binary
path stats. The privileged signals (`probeMagiskFiles`, `queryMagiskVersion`) are gated
behind `granted == true` (`RootChecker.kt:125-126`).

Two properties of modern root solutions defeat the unprivileged probes:

1. **Kernel-based roots leave no userspace footprint.** KernelSU, APatch, and the KernelSU
   forks have no `su` binary at standard paths and nothing labeled "magisk" in an
   unprivileged app's `/proc/self/mounts`. Their *only* ungranted signal is the package
   name — so an unrecognized package id means an undetected device.
2. **Stealth / repackaging.** Kitsune Mask's random package-name generator, and the
   "hide the manager" feature in KernelSU/SukiSU, produce unpredictable package names that
   no `<queries>` list can enumerate.

When every passive signal is false and `granted == false`, `classify` returns `NotRooted`
(`RootChecker.kt:59`) even though root is installed.

### Note: the *granted* case already works

Once the user grants root, `requestRoot` (`RootChecker.kt:110`) forces the libsu superuser
dialog, after which `queryMagiskVersion` (`magisk -v`), `probeMagiskFiles`
(`/data/adb/*`), or a working `su` resolve the provider. The gaps below are about
detection *before* a grant (or when a whitelist/SuList mode never surfaces the prompt).

## Unrecognized Providers

### High relevance (mainstream in 2026)

| Provider | Package id | Notes |
| --- | --- | --- |
| Kitsune Mask / Magisk Delta | `io.github.huskydg.magisk` | Magisk fork; classify as `MAGISK`. Random package name when hidden. |
| SukiSU Ultra | `com.sukisu.ultra` | KernelSU fork (KPM + SUSFS); classify as `KERNELSU`. |
| ReSukiSU | `com.resukisu.resukisu` | KernelSU fork; classify as `KERNELSU`. |

These three are the most important misses. Being Magisk/KernelSU-family, they share the
same ungranted footprint problem: with an unknown package id and no userspace `su`, an
ungranted device reports `NotRooted`.

### Legacy (declining, but min SDK is 23 so still reachable)

| Provider | Package id(s) |
| --- | --- |
| SuperSU | `eu.chainfire.supersu` |
| Old Superuser | `com.koushikdutta.superuser`, `com.noshufou.android.su`, `com.noshufou.android.su.elite` |
| KingRoot / KingUser | `com.kingroot.kinguser`, `com.kingouser.com` |
| phh superuser | `me.phh.superuser` |

These older managers typically drop a real `su` binary at standard paths, so
`probeSuBinary` (`RootChecker.kt:152`) may already catch them as `RootProvider.OTHER`. A
package list would only upgrade them from `OTHER` to a named provider — lower priority.

## Proposed Fix

### 1. Extend the provider package lists (`RootChecker.kt:74-88`)

- `MAGISK_PACKAGES` += `io.github.huskydg.magisk` (Kitsune / Magisk Delta).
- `KERNELSU_PACKAGES` += `com.sukisu.ultra`, `com.resukisu.resukisu`.
- (Optional, legacy) introduce an `OTHER`/named list for `eu.chainfire.supersu`,
  `com.koushikdutta.superuser`, `com.noshufou.android.su[.elite]`,
  `com.kingroot.kinguser`, `com.kingouser.com`, `me.phh.superuser`.

### 2. Mirror every new id in `<queries>` (`AndroidManifest.xml:8`)

On API 30+ (this app targets SDK 37) `probeAnyPackage` can only see a package declared in
`<queries>`. Every id added in step 1 needs a matching `<package android:name="…" />`
entry. **Do not** switch to `QUERY_ALL_PACKAGES` — it is Play-policy sensitive and not
justified for this use case.

### 3. Broaden the systemless-Magisk path heuristic (no root required)

Add an ungranted probe (e.g. `probeMagiskPaths()`) that stats world-stat-able directories
left by Magisk and its forks, feeding a new `RootSignals` field:

- `/data/adb/magisk`, `/data/adb/modules`, `/sbin/.magisk`, `/debug_ramdisk/.magisk`

Treat a hit as a Magisk fingerprint in the `granted == false/null` branch. This catches
Kitsune even when the package is hidden/renamed, as long as the path is stat-able.
(`File.exists()` success here is device/SELinux dependent — a best-effort signal, not a
guarantee.)

### 4. Document the hard limit

Random package names (Kitsune) and hidden managers (KernelSU/SukiSU) are explicitly
designed to evade unprivileged detection. There is **no reliable passive way** to detect
them once repackaged and when no path/mount signal is stat-able. The honest fallback is
the existing `requestRoot` flow: prompt for a grant, after which detection is solid. The
UI / About screen could note that hidden root managers may require granting access to be
detected.

## Testing

- Add `classify` unit tests in `RootCheckerTest.kt` for the new signals:
  - Kitsune package hit while ungranted → `RootedNotGranted(MAGISK)`.
  - SukiSU / ReSukiSU package hit while ungranted → `RootedNotGranted(KERNELSU)`.
  - New magisk-paths signal hit while ungranted → `RootedNotGranted(MAGISK)`.
  - Granted + version still resolves to `Rooted(MAGISK, version)`.
- Hardware verification on real devices (path/mount probes are not unit-testable):
  - Kitsune Mask, SukiSU Ultra before granting → expect `RootedNotGranted(...)` instead of
    `NotRooted`; after granting → expect `Rooted(...)`.
  - Repeat with Kitsune's "hide the Magisk app" / KernelSU hidden-manager enabled to
    confirm the path heuristic still fires and to document residual gaps.

## Changelog

Per `CLAUDE.md`, add a bullet under `## [Unreleased]` → **Fixed** in `CHANGELOG.md` in the
same commit as the implementation, e.g.:

> - Detect more root managers (Kitsune Mask / Magisk Delta, SukiSU Ultra, ReSukiSU) that
>   were previously reported as not rooted.

## References

- Kitsune Mask — Advanced Android Rooting: https://kitsune-mask.vercel.app/
- Magisk Delta (`io.github.huskydg.magisk`) on APKCombo: https://apkcombo.com/magisk-delta/io.github.huskydg.magisk/
- Magisk vs KernelSU vs APatch — Best Root in 2026: https://awesome-android-root.org/rooting-guides/root-framework-comparison
- SukiSU-Ultra (GitHub): https://github.com/sukisu-ultra/sukisu-ultra
- ReSukiSU (GitHub): https://github.com/ReSukiSU/ReSukiSU
- KernelSU-Next (GitHub): https://github.com/KernelSU-Next/KernelSU-Next
- [Discussion] Kitsune Mask — unofficial mask of Magisk (XDA): https://xdaforums.com/t/discussion-kitsune-mask-another-unofficial-mask-of-magisk.4460555/
- Replace KingRoot with SuperSU (XDA) — legacy package names: https://xdaforums.com/t/how-to-remove-replace-kingroot-kinguser-with-supersu.3308989/
