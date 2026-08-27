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

## What an emulator can and cannot verify

An emulator covers three of the four `RootResult` states, and **every provider and manager the
app names**. Only `Rooted` needs more than a stock image, because only `Rooted` needs
`granted == true`, and that needs a real root provider. The ungranted paths are real probes
here, not mocks, and they are cheap to re-run.

### The manager matrix needs no root at all

`detectInstalledManager` is the *only* signal that ever yields `KERNELSU` or `APATCH`, and it
reads nothing but installed package ids. So the whole table is verifiable on a stock image with
no root anywhere: install a package carrying the id and run a check.

The packages need no code. A manifest-only APK with `android:hasCode="false"` installs fine and
is enough to be seen, so one small loop builds all of them:

```bash
BT=$ANDROID_HOME/build-tools/36.0.0
cat > AndroidManifest.xml <<XML
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$PKG">
    <application android:hasCode="false" android:label="$LABEL (stub)" />
</manifest>
XML
$BT/aapt2 link -o unsigned.apk -I $ANDROID_HOME/platforms/android-36/android.jar \
  --manifest AndroidManifest.xml --min-sdk-version 23 --target-sdk-version 36
$BT/zipalign -f 4 unsigned.apk aligned.apk
$BT/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey --out "$PKG.apk" aligned.apk
```

Read the answer out of the persisted record rather than off the screen, which is exact and needs
no coordinates:

```bash
adb shell run-as com.iboalali.basicrootchecker.debug \
  cat files/datastore/user_settings.preferences_pb | strings
```

**All 17 ids in `PACKAGE_MANAGERS` were confirmed visible and correctly mapped this way on API 36
(2026-08-27)**, which is the check that matters most: a package missing from the manifest's
`<queries>`, or misspelled in either list, fails *silently* on API 30+ and looks exactly like a
device with no root manager.

The same rig proves the family-mismatch guard, which no unit test can reach through real signals.
Give the device a Magisk path fingerprint the app can actually stat, then install a KernelSU
manager beside it:

```bash
adb root
adb shell 'mkdir -p /data/adb/magisk && chmod 755 /data/adb /data/adb/magisk'
```

`probeMagiskPaths` is unprivileged, so `/data/adb` has to be traversable or the probe cannot see
the directory at all. With both present the result is `RootedNotGranted(MAGISK, manager = null)`
and the card reads a bare "via Magisk": the Magisk path wins the family, and the KernelSU manager
is suppressed rather than mislabeled as the Magisk that was detected. Put `/data/adb` back to
`700` afterwards.

### A debug build's FAB opens the demo picker, not a real check

`BuildConfig.DEBUG` makes the FAB show the demo result picker, so a debug build tests the picker
and not the detector. Arming a demo *device* flips it back to the real check:

```bash
adb shell am start -n com.iboalali.basicrootchecker.debug/com.iboalali.basicrootchecker.MainActivity \
  --es demo_device_name "RootSim" --es demo_device_model rootsim
```

`DemoDeviceOverride.recording` is `name != null`, and the FAB consults it. This changes only the
device-name card, so every root signal stays real.

### `adb root` is not root the app can use

A `google_apis` (never `*_playstore`) image is `userdebug`, so `adb root` succeeds and the
shell becomes uid 0. **The app still sees nothing**, and no amount of relaxing the filesystem
changes that. AOSP's `/system/xbin/su` serves only uid 0 and uid 2000 (`AID_SHELL`), and it
enforces that *in the binary*: from an app uid it answers `su: not allowed`. Verified with
SELinux permissive, with the binary at mode 4755, and with `/system/xbin` traversable — the
refusal is unconditional. `libsu`'s `Shell.isAppGrantedRoot()` therefore cannot return true.

Reaching `Rooted`, and with it `magisk -v` parsing and the privileged `/data/adb/magisk` probe,
requires a real root provider in the ramdisk, i.e. a tool such as `rootAVD`.

### A setuid `su` cannot grant an app root either

The obvious next idea is to skip AOSP's uid guard by supplying your own `su`: a few lines of C
that call `setuid(0)` and exec `/system/bin/sh`, installed root-owned at mode 6755 on an
overlay-mounted `/system`. It genuinely works from an `adb shell` and from `run-as`, both of which
return `uid=0(root)`. **It cannot work from an app**, and the reason is not SELinux:

```
$ adb shell grep CapBnd /proc/$(adb shell pidof <app>)/status
CapBnd:  0000000000000000      # the app process
CapBnd:  000001ffffffffff      # an adb shell, for contrast
```

An app process has an **empty capability bounding set**. The setuid bit still gives the exec'd
binary euid 0, but the bounding set caps what the permitted set can ever hold, so `CAP_SETUID` is
unavailable and `setuid(0)` fails with `EPERM`. A shim that ignores that failure execs a shell
that is still the app's own uid, which is what `libsu` then reports: not root. `NoNewPrivs` is
`0` and SELinux can be `Permissive` throughout, and neither changes the outcome.

This is also why every real provider is built the way it is. Magisk, KernelSU and APatch do not
ship a setuid binary; their `su` is a thin client that asks a privileged daemon (or a kernel hook)
to spawn the root shell and hands the caller's stdio to it. Simulating `Rooted` without one of
them means reimplementing that daemon, which tests the rig rather than the app.

### A two-state rig for the ungranted paths

`/system/xbin` ships as `drwxr-x--- root shell`, so an app cannot traverse into it and
`suBinaryHit` is false. Granting search permission makes the `su` binary stat-able while
still unusable — which is exactly the `RootedNotGranted` shape. Boot with `-writable-system`
(and `-gpu host`, or the emulator quietly falls back to `lavapipe`/`swangle` software
rendering), then `adb root && adb remount` once per boot:

```bash
adb shell chmod 755 /system/xbin   # -> RootedNotGranted(OTHER)   rooted=true,  accessGranted=false
adb shell chmod 750 /system/xbin   # -> NotRooted                 rooted=false, accessGranted=false
adb shell am force-stop com.iboalali.basicrootchecker    # between every flip — see below
```

This drives the real `suBinaryHit` signal and the `suBinaryHit -> OTHER` branch in
[`classify`](../app/src/main/java/com/iboalali/basicrootchecker/data/RootChecker.kt), so it is
worth more than a unit test with a hand-built `RootSignals`. Reading the result through the
`checkRootStatus` AppFunction returns `status`, `provider` and `manager` in one call, which
also exercises the AppFunctions surface at the same time.

### Force-stop between checks, or the rig lies

`granted` is `Boolean?`, and the `false` and `null` cases classify differently: no provider
plus `granted == false` is `NotRooted`, while no provider plus `granted == null` is `Unknown`.
`libsu` resolves the grant once per process and caches it, so **changing root state under a
live process reports `Unknown`** — which reads exactly like a detection bug and is not one.
Alternating the mode above without `am force-stop` produces that phantom `Unknown`; with a
fresh process each time the two states are deterministic.

The same caching is why a real device only needs one check per session, and why an agent
calling `checkRootStatus` repeatedly gets a stable answer.

### What is left for real hardware

Narrower than "KernelSU and APatch are untested". Split by what each layer needs:

| Layer | Covered by |
| --- | --- |
| `classify` for every provider, granted and ungranted, including Magisk precedence | Unit tests over hand-built `RootSignals` |
| `detectInstalledManager` and the manifest `<queries>`, all 17 ids | Stub APKs on a stock emulator |
| The family-mismatch guard on real probe output | Magisk path fingerprint + KernelSU stub |
| `granted == true`, `magisk -v`, privileged `/data/adb` probe | Magisk, on the rooted emulator or a device |
| `granted == true` **through KernelSU's or APatch's own `su`** | Nothing yet |

Only the last row is open, and it is one signal: whether `libsu` resolves a grant through a
provider that is not Magisk. `libsu` only runs `su` and reads the result, and both providers ship
a `su` it drives the same way, so the residual risk is low. Everything the app does *with* that
answer for those two families is package-name lookup plus the branch logic above, and both are
covered.

Note that a device is not automatically better than the emulator here. Reproducing the ungranted
matrix on hardware means installing 17 real manager apps, several of which will not coexist; the
stubs isolate one id at a time, which no real device can.

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
