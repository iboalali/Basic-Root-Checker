# Fix Plan: Kitsune Mask (Magisk Delta) Not Detected

## Background

A user reported that their device with **Kitsune Mask** installed and root active was
reported by the app as **not rooted**.

Kitsune Mask is the current name for **Magisk Delta**, an unofficial Magisk fork
(application id `io.github.huskydg.magisk`). Its headline feature is *detection
avoidance*: a random package-name generator ("hide the manager" / repackaging) plus
stealth mechanisms that scrub or rename Magisk's standard fingerprints. In other words,
it is purpose-built to defeat exactly the kind of passive probes this app uses.

## Root Cause

The failure occurs in the **"root installed but not yet granted to this app"** path —
the `granted == false || granted == null` branch of `classify`
(`RootChecker.kt:49-62`). Every unprivileged probe in `collectSignals`
(`RootChecker.kt:119`) misses Kitsune:

1. **Package probe misses it twice.** `MAGISK_PACKAGES` (`RootChecker.kt:74-79`) only
   lists `com.topjohnwu.magisk[.debug/.canary/.alpha]`. Kitsune's id is
   `io.github.huskydg.magisk`, which is absent. Even if added, the `<queries>` block in
   `AndroidManifest.xml:8-16` doesn't declare it, so on API 30+ (this app targets SDK 37)
   `getPackageInfo` cannot see it. Worse, Kitsune's random-package-name feature means the
   installed manager may carry an unpredictable name that `<queries>` cannot enumerate at
   all. → `magiskPackageHit = false`
2. **Mount probe misses it.** `probeMagiskMounts` (`RootChecker.kt:145`) reads the app's
   own `/proc/self/mounts` for the string "magisk". Modern systemless Magisk is not
   visible in an unprivileged app's mount namespace, and Kitsune's stealth scrubs/renames
   these entries. → `magiskMountHit = false`
3. **su-binary probe misses it.** `probeSuBinary` (`RootChecker.kt:152`) stats hard-coded
   paths such as `/system/bin/su`. MagiskSU is daemon-based — there is no `su` file at
   those paths for an unprivileged app to stat. → `suBinaryHit = false`
4. **Strong signals are gated behind a grant.** `probeMagiskFiles` (`/data/adb/magisk`)
   and `queryMagiskVersion` (`magisk -v`) only run when `granted == true`
   (`RootChecker.kt:125-126`), so they never contribute in the ungranted case.

With all passive signals false and `granted == false`, `classify` returns `NotRooted`
(`RootChecker.kt:59`).

### Note: the *granted* case already works

If the user grants root, `requestRoot` (`RootChecker.kt:110`) forces the libsu superuser
dialog. Magisk Delta supports libsu's request mechanism, so once granted,
`queryMagiskVersion` and/or `probeMagiskFiles` succeed and the device is correctly
reported as `Rooted(MAGISK, …)`. The bug is confined to detection *before* a grant (or
when Kitsune's SuList/whitelist mode never surfaces the prompt for this app).

## Proposed Fix

### 1. Recognize the Kitsune / Magisk Delta package id

- Add `io.github.huskydg.magisk` to `MAGISK_PACKAGES` (`RootChecker.kt:74`).
- Add a matching `<package android:name="io.github.huskydg.magisk" />` entry to the
  `<queries>` block (`AndroidManifest.xml:8`). Without this, package visibility on API
  30+ blocks the lookup.

### 2. Broaden the systemless-Magisk path heuristic (no root required)

Magisk and its forks leave world-readable / stat-able directories that an unprivileged
app can probe with `File.exists()`, independent of a grant:

- `/data/adb/magisk`
- `/data/adb/modules`
- `/sbin/.magisk`
- `/debug_ramdisk/.magisk`

Add a new ungranted probe (e.g. `probeMagiskPaths()`) that stats these and feeds a new
signal into `RootSignals`. This catches Kitsune even when the package is hidden/renamed,
as long as the path is stat-able. Treat a hit as a Magisk fingerprint in the
`granted == false/null` branch of `classify`. (Note: success of `File.exists()` on these
paths is device/SELinux dependent; it is a best-effort signal, not a guarantee.)

### 3. Add a generic "su on PATH" check

The current `probeSuBinary` only stats fixed paths. Consider, additionally, checking the
`PATH`-resolved `su` (still without granting). This is a weak heuristic and should map to
`RootProvider.OTHER` at most.

### 4. Document the hard limit

Kitsune's random-package-name generator plus SuList/whitelist stealth is explicitly
designed to evade unprivileged detection. There is **no reliable passive way** to detect
it when the manager is hidden/repackaged and no mount/path signal is stat-able. The
honest fallback is: prompt the user to grant root (the existing `requestRoot` flow), after
which detection is solid. The UI copy / About screen could note that hidden root managers
may require granting access to be detected.

## Testing

- Add `classify` unit tests in `RootCheckerTest.kt` for the new signal(s):
  - Kitsune package hit while ungranted → `RootedNotGranted(MAGISK)`.
  - New magisk-paths signal hit while ungranted → `RootedNotGranted(MAGISK)`.
  - Granted + version still resolves to `Rooted(MAGISK, version)`.
- Hardware verification on a real device running Kitsune Mask (the path/mount probes are
  not unit-testable):
  - Before granting → expect `RootedNotGranted(MAGISK)` instead of `NotRooted`.
  - After granting → expect `Rooted(MAGISK, <version>)`.
  - Repeat with Kitsune's "hide the Magisk app" (random package name) enabled to confirm
    the path heuristic still fires and to document residual gaps.

## Changelog

Per `CLAUDE.md`, add a bullet under `## [Unreleased]` → **Fixed** in `CHANGELOG.md` in the
same commit as the implementation, e.g.:

> - Detect Kitsune Mask (Magisk Delta) devices that were previously reported as not rooted.

## References

- Kitsune Mask — Advanced Android Rooting: https://kitsune-mask.vercel.app/
- Magisk Delta (`io.github.huskydg.magisk`) on APKCombo: https://apkcombo.com/magisk-delta/io.github.huskydg.magisk/
- Magisk Delta / Kitsune Mask downloads (SourceForge): https://sourceforge.net/projects/magisk/files/Magisk%20Delta%20(Kitsune%20Mask)/
- [Discussion] Kitsune Mask — unofficial mask of Magisk (XDA): https://xdaforums.com/t/discussion-kitsune-mask-another-unofficial-mask-of-magisk.4460555/
