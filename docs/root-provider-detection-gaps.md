# Root Provider Detection: Coverage and Gaps

What the detector can see, where it is blind by design, and how to verify it on an emulator.

## The hard part: root installed but not yet granted

Detection is solid once the user grants root. `requestRoot` forces the libsu superuser dialog, after which `queryMagiskVersion` (`magisk -v`), `probeMagiskFiles` (the `MAGISK_PATHS` directories, tested with root) or a working `su` resolve the provider.

Before a grant (or when a whitelist/SuList mode never shows the prompt), `classify` takes its `granted == false || granted == null` branch. There, only the unprivileged probes in `collectSignals` are available: installed package ids, a `/proc/self/mounts` scan, Magisk path stats, and `su`-binary path stats. The privileged probes (`probeMagiskFiles`, `queryMagiskVersion`) only run when `granted == true`.

Two properties of modern root solutions defeat the unprivileged probes:

1. **Kernel-based roots leave no userspace footprint.** KernelSU, APatch and the KernelSU forks have no `su` binary at standard paths and nothing labeled "magisk" in an unprivileged app's `/proc/self/mounts`. Their *only* ungranted signal is the package id, so an unrecognized id means an undetected device.
2. **Stealth and repackaging.** Kitsune Mask's random package-name generator, and the "hide the manager" feature in KernelSU/SukiSU, produce package names no `<queries>` list can enumerate.

When every passive signal is false and `granted == false`, `classify` returns `NotRooted` even though root is installed.

## What the detector recognizes

`PACKAGE_MANAGERS` in `RootChecker.kt` maps 17 package ids to a `RootManager`, and each manager to its `RootProvider` family. Its order is the tie-break when more than one is installed: Magisk family, then KernelSU family, then APatch, then legacy.

| Family | Manager | Package id(s) |
| --- | --- | --- |
| Magisk | Magisk | `com.topjohnwu.magisk` (+ `.debug` / `.canary` / `.alpha`) |
| Magisk | Kitsune Mask / Magisk Delta | `io.github.huskydg.magisk` |
| KernelSU | KernelSU | `me.weishu.kernelsu` |
| KernelSU | KernelSU Next | `com.rifsxd.ksunext` |
| KernelSU | SukiSU Ultra | `com.sukisu.ultra` |
| KernelSU | ReSukiSU | `com.resukisu.resukisu` |
| APatch | APatch | `me.bmax.apatch` |
| Other | SuperSU | `eu.chainfire.supersu` |
| Other | Superuser | `com.koushikdutta.superuser`, `com.noshufou.android.su`, `com.noshufou.android.su.elite` |
| Other | KingRoot / KingUser | `com.kingroot.kinguser`, `com.kingouser.com` |
| Other | phh superuser | `me.phh.superuser` |

The legacy managers usually drop a real `su` at a standard path, so `probeSuBinary` catches them as `OTHER` anyway. Their package entry upgrades that to a named manager, and covers the case where the binary is not stat-able.

Non-package signals:

- `SU_PATHS`: a `su` binary at a standard path maps to `OTHER`.
- `MAGISK_PATHS` (`/data/adb/magisk`, `/sbin/.magisk`, `/debug_ramdisk/.magisk`), probed unprivileged by `probeMagiskPaths` and with root by `probeMagiskFiles`. A hit is a Magisk fingerprint even when the manager is hidden or renamed. Whether `File.exists()` can see these is device and SELinux dependent, so it is best effort.
- `/data/adb/modules` is deliberately not a Magisk signal. KernelSU (and its forks) and APatch keep their modules there too, so with root granted it exists on all three, and treating it as Magisk would let the family-mismatch guard drop a correctly detected KernelSU or APatch manager. The three remaining paths are Magisk's own: KernelSU works under `/data/adb/ksu`, APatch under `/data/adb/ap`, and the magic-mount metamodules use `/debug_ramdisk` itself or a `workdir` / `.magic_mount` directory inside it, never `.magisk` (checked upstream 2026-10-08).
- `/proc/self/mounts`: a Magisk mount maps to `MAGISK`.

**Family-mismatch guard.** A Magisk mount or path forces `MAGISK` even when the installed manager app is, say, KernelSU. `classify` then surfaces the manager only if its family matches the resolved provider, so the KernelSU app is never shown as the Magisk that was detected.

**Every id must also be in the manifest's `<queries>`.** On API 30+ `PackageManager` cannot see an undeclared package, and the miss is silent. **Do not** switch to `QUERY_ALL_PACKAGES`: it is Play-policy sensitive and not justified here.

## The hard limit

Random package names (Kitsune) and hidden managers (KernelSU/SukiSU) are designed to evade unprivileged detection. Once repackaged, with no path or mount signal stat-able, there is **no reliable passive way** to detect them. The fallback is the `requestRoot` flow: prompt for a grant, after which detection is solid. `MainScreen` shows `root_hidden_manager_hint` to tell users a hidden or renamed manager may need a grant before it is detected.

## What an emulator can and cannot verify

An emulator covers three of the four `RootResult` states, and **every provider and manager the app names**. Only `Rooted` needs more than a stock image, because only `Rooted` needs `granted == true`, and that needs a real root provider. The ungranted paths are real probes here, not mocks, and they are cheap to re-run.

### The manager matrix needs no root at all

`detectInstalledManager` is the *only* signal that ever yields `KERNELSU` or `APATCH`, and it reads nothing but installed package ids. So the whole table is verifiable on a stock image with no root anywhere: install a package carrying the id and run a check.

The packages need no code. A manifest-only APK with `android:hasCode="false"` installs fine and is enough to be seen, so one small loop builds all of them:

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

Read the answer from the persisted record rather than off the screen. It is exact and needs no coordinates:

```bash
adb shell run-as com.iboalali.basicrootchecker.debug \
  cat files/datastore/user_settings.preferences_pb | strings
```

**All 17 ids in `PACKAGE_MANAGERS` were confirmed visible and correctly mapped this way on API 36 (2026-08-27).** This is the check that matters most: a package missing from the manifest's `<queries>`, or misspelled in either list, fails *silently* on API 30+ and looks exactly like a device with no root manager.

The same rig proves the family-mismatch guard, which no unit test can reach through real signals. Give the device a Magisk path fingerprint the app can stat, then install a KernelSU manager beside it:

```bash
adb root
adb shell 'mkdir -p /data/adb/magisk && chmod 755 /data/adb /data/adb/magisk'
```

`probeMagiskPaths` is unprivileged, so `/data/adb` has to be traversable or the probe cannot see the directory at all. With both present the result is `RootedNotGranted(MAGISK, manager = null)` and the card reads a bare "via Magisk": the Magisk path wins the family, and the KernelSU manager is suppressed rather than mislabeled. Put `/data/adb` back to `700` afterwards.

### A debug build's FAB opens the demo picker, not a real check

`BuildConfig.DEBUG` makes the FAB show the demo result picker, so a debug build tests the picker and not the detector. Arming a demo *device* switches it back to the real check:

```bash
adb shell am start -n com.iboalali.basicrootchecker.debug/com.iboalali.basicrootchecker.MainActivity \
  --es demo_device_name "RootSim" --es demo_device_model rootsim
```

`DemoDeviceOverride.recording` is `name != null`, and the FAB consults it. This changes only the device-name card, so every root signal stays real.

### `adb root` is not root the app can use

A `google_apis` (never `*_playstore`) image is `userdebug`, so `adb root` succeeds and the shell becomes uid 0. **The app still sees nothing**, and no amount of relaxing the filesystem changes that. AOSP's `/system/xbin/su` serves only uid 0 and uid 2000 (`AID_SHELL`), and it enforces that *in the binary*: from an app uid it answers `su: not allowed`. Verified with SELinux permissive, with the binary at mode 4755, and with `/system/xbin` traversable: the refusal is unconditional. So `libsu`'s `Shell.isAppGrantedRoot()` cannot return true.

Reaching `Rooted`, and with it `magisk -v` parsing and the privileged `/data/adb/magisk` probe, requires a real root provider in the ramdisk, from a tool such as `rootAVD`.

### A setuid `su` cannot grant an app root either

Supplying your own `su` does not get around AOSP's uid guard. A few lines of C that call `setuid(0)` and exec `/system/bin/sh`, installed root-owned at mode 6755 on an overlay-mounted `/system`, do work from `adb shell` and from `run-as`: both return `uid=0(root)`. **It cannot work from an app**, and the reason is not SELinux:

```
$ adb shell grep CapBnd /proc/$(adb shell pidof <app>)/status
CapBnd:  0000000000000000      # the app process
CapBnd:  000001ffffffffff      # an adb shell, for contrast
```

An app process has an **empty capability bounding set**. The setuid bit still gives the exec'd binary euid 0, but the bounding set caps what the permitted set can ever hold. So `CAP_SETUID` is unavailable and `setuid(0)` fails with `EPERM`. A shim that ignores that failure execs a shell that still runs as the app's own uid, which is what `libsu` then reports: not root. `NoNewPrivs` is `0` and SELinux can be `Permissive` throughout; neither changes the outcome.

This is also why every real provider is built the way it is. Magisk, KernelSU and APatch do not ship a setuid binary. Their `su` is a thin client that asks a privileged daemon (or a kernel hook) to spawn the root shell and hands the caller's stdio to it. Simulating `Rooted` without one of them means reimplementing that daemon, which tests the rig rather than the app.

### A two-state rig for the ungranted paths

`/system/xbin` ships as `drwxr-x--- root shell`, so an app cannot traverse into it and `suBinaryHit` is false. Granting search permission makes the `su` binary stat-able but still unusable, which is exactly the `RootedNotGranted` shape. Boot with `-writable-system` (and `-gpu host`, or the emulator quietly falls back to `lavapipe`/`swangle` software rendering), then run `adb root && adb remount` once per boot:

```bash
adb shell chmod 755 /system/xbin   # -> RootedNotGranted(OTHER)   rooted=true,  accessGranted=false
adb shell chmod 750 /system/xbin   # -> NotRooted                 rooted=false, accessGranted=false
adb shell am force-stop com.iboalali.basicrootchecker    # between every flip, see below
```

This drives the real `suBinaryHit` signal and the `suBinaryHit -> OTHER` branch in [`classify`](../app/src/main/java/com/iboalali/basicrootchecker/data/RootChecker.kt), so it is worth more than a unit test with a hand-built `RootSignals`. Reading the result through the `checkRootStatus` AppFunction returns `status`, `provider` and `manager` in one call, and exercises the AppFunctions surface at the same time.

### Force-stop between checks, or the rig lies

`granted` is `Boolean?`, and `false` and `null` classify differently: no provider plus `granted == false` is `NotRooted`, while no provider plus `granted == null` is `Unknown`. `libsu` resolves the grant once per process and caches it, so **changing root state under a live process reports `Unknown`**. That reads exactly like a detection bug and is not one. Flipping the mode above without `am force-stop` produces that phantom `Unknown`. With a fresh process each time, the two states are deterministic.

The same caching is why a real device only needs one check per session, and why an agent calling `checkRootStatus` repeatedly gets a stable answer.

### What is left for real hardware

| Layer | Covered by |
| --- | --- |
| `classify` for every provider, granted and ungranted, including Magisk precedence | Unit tests over hand-built `RootSignals` (`RootCheckerTest.kt`) |
| `detectInstalledManager` and the manifest `<queries>`, all 17 ids | Stub APKs on a stock emulator |
| The family-mismatch guard on real probe output | Magisk path fingerprint + KernelSU stub |
| `granted == true`, `magisk -v`, privileged `MAGISK_PATHS` probe | Magisk, on the rooted emulator or a device |
| `granted == true` **through KernelSU's or APatch's own `su`** | Nothing yet |

Only the last row is open, and it is one signal: whether `libsu` resolves a grant through a provider that is not Magisk. `libsu` only runs `su` and reads the result, and both providers ship a `su` it drives the same way, so the residual risk is low. Everything the app does *with* that answer for those two families is package-name lookup plus the branch logic above, and both are covered.

A device is not automatically better than the emulator here. Reproducing the ungranted matrix on hardware means installing 17 real manager apps, several of which will not coexist. The stubs isolate one id at a time, which no real device can.

Path and mount probes on hidden managers (Kitsune's "hide the Magisk app", KernelSU's hidden manager) can only be checked on real installs. They are best effort by design.

## References

- Kitsune Mask, Advanced Android Rooting: https://kitsune-mask.vercel.app/
- Magisk Delta (`io.github.huskydg.magisk`) on APKCombo: https://apkcombo.com/magisk-delta/io.github.huskydg.magisk/
- Magisk vs KernelSU vs APatch, Best Root in 2026: https://awesome-android-root.org/rooting-guides/root-framework-comparison
- SukiSU-Ultra (GitHub): https://github.com/sukisu-ultra/sukisu-ultra
- ReSukiSU (GitHub): https://github.com/ReSukiSU/ReSukiSU
- KernelSU-Next (GitHub): https://github.com/KernelSU-Next/KernelSU-Next
- Kitsune Mask, unofficial mask of Magisk (XDA): https://xdaforums.com/t/discussion-kitsune-mask-another-unofficial-mask-of-magisk.4460555/
- Replace KingRoot with SuperSU (XDA), legacy package names: https://xdaforums.com/t/how-to-remove-replace-kingroot-kinguser-with-supersu.3308989/
