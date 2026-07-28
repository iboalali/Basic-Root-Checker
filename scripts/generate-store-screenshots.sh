#!/usr/bin/env bash
#
# Generate Play Store listing screenshots (all screens × all locales × store devices) with the
# PRODUCTION app name.
#
# Why this script exists: the Compose Preview Screenshot tool only renders the *debug* variant,
# whose `src/debug/res` renames the app to "Basic Root Checker (Debug)". For store assets we want
# the clean name, so this temporarily neutralizes that override, renders, copies the clean PNGs to
# an export folder, and then RESTORES both the debug strings and the committed regression baseline
# (the reference/ images stay on the normal "(Debug)" render, so `validateGplayDebugScreenshotTest`
# keeps passing on a plain checkout). Nothing is left modified.
#
# Both restores are byte-exact copies taken *before* the render and put back from a single EXIT
# trap, so an interrupt or a failing Gradle run cannot leave the tree half-converted. The baseline is
# deliberately restored from that backup rather than re-rendered: a re-render is slow, can itself
# fail (leaving clean-name PNGs in reference/ and a failing `validate` on an otherwise untouched
# checkout), and would silently "fix up" a baseline that was already stale before this script ran.
# This script does not update the baseline -- run `:app:updateGplayDebugScreenshotTest` for that.
#
# Output is grouped into per-language subfolders (matching the `Play Store/Listing/` convention:
# default/german/arabic/spanish/russian), then a per-device subfolder (Phone/Tablet7/Tablet10),
# e.g. ".../v2.5/arabic/Tablet10/AboutDialogShot.png". Mirrors the Play Console upload flow: pick a
# language, then drop each device folder into its slot.
#
# Usage:  scripts/generate-store-screenshots.sh [output-subfolder]
#         (default output: "Play Store/Generated Screenshots/")
set -euo pipefail

# Repo root = parent of this script's dir.
cd "$(dirname "$0")/.."

OUT_DIR="Play Store/Generated Screenshots/${1:-}"
REF_DIR="app/src/screenshotTestGplayDebug/reference/com/iboalali/basicrootchecker/screenshots/ScreenshotTestsKt"

# Debug source-set strings that carry the "(Debug)" app name (default + localized overrides).
DEBUG_STRINGS=(
  "app/src/debug/res/values/strings.xml"
  "app/src/debug/res/values-ar/strings.xml"
  "app/src/debug/res/values-de/strings.xml"
)

backups=()
ref_backup=""
ref_existed=false
restored=false

# Put the working tree back exactly as we found it: the debug strings, and the regression baseline
# the render overwrote. Idempotent, so running from the EXIT trap is safe however we got there
# (success, Gradle failure, Ctrl-C).
restore_state() {
  if [ "$restored" = true ]; then return; fi
  restored=true
  echo "==> Restoring the debug app name and the regression baseline…"
  for i in "${!DEBUG_STRINGS[@]}"; do
    if [ -n "${backups[$i]:-}" ] && [ -f "${backups[$i]}" ]; then
      # cp, not mv: copying into the existing file keeps its original permissions, whereas mv would
      # move the mktemp inode over it and silently tighten the mode to 600. Git tracks only the exec
      # bit, so that kind of drift is invisible in `git status`.
      cp -f "${backups[$i]}" "${DEBUG_STRINGS[$i]}"
      rm -f "${backups[$i]}"
    fi
  done
  if [ -n "$ref_backup" ] && [ -d "$ref_backup" ]; then
    rm -rf "${REF_DIR:?}"
    if [ "$ref_existed" = true ]; then
      mkdir -p "$REF_DIR"
      # -a keeps mtimes, so restoring doesn't look like an edit to Gradle's up-to-date checks.
      cp -a "$ref_backup/." "$REF_DIR/"
    fi
    rm -rf "$ref_backup"
  fi
}
# Safety net: never leave the tree converted, no matter how we exit.
trap restore_state EXIT

echo "==> Neutralizing the debug \"(Debug)\" app name for the render…"
for i in "${!DEBUG_STRINGS[@]}"; do
  f="${DEBUG_STRINGS[$i]}"
  bak="$(mktemp)"
  cp "$f" "$bak"
  backups[$i]="$bak"
  sed -i 's/Basic Root Checker (Debug)/Basic Root Checker/g' "$f"
done

# Snapshot the baseline before the render overwrites it with clean-name PNGs. Taken even when
# reference/ doesn't exist yet (first ever run), so the restore can remove what the render created
# rather than leaving an unexpected baseline behind.
ref_backup="$(mktemp -d)"
if [ -d "$REF_DIR" ]; then
  ref_existed=true
  cp -a "$REF_DIR/." "$ref_backup/"
fi

echo "==> Rendering store screenshots…"
./gradlew :app:updateGplayDebugScreenshotTest --rerun-tasks

# Locale code -> language subfolder, matching the `Play Store/Listing/` convention (English is
# "default"). An unmapped locale falls back to its raw code so nothing is silently dropped.
lang_dir_for() {
  case "$1" in
    en) echo default ;;
    de) echo german ;;
    ar) echo arabic ;;
    es) echo spanish ;;
    ru) echo russian ;;
    *)  echo "$1" ;;
  esac
}

echo "==> Exporting clean PNGs to: $OUT_DIR (grouped by language, then device)"
# Clear the target first: the loop below only *copies*, so a screen renamed or dropped since the last
# export (e.g. LicenceShot -> LicenseShot) would otherwise linger beside its replacement and get
# uploaded. Only safe when an explicit subfolder was named — with no argument $OUT_DIR is the shared
# parent holding every version's export, which must not be touched.
if [ -n "${1:-}" ] && [ -d "$OUT_DIR" ]; then
  echo "    Clearing the previous export in $OUT_DIR"
  rm -rf "${OUT_DIR:?}"
fi
mkdir -p "$OUT_DIR"
# Group each reference PNG into a <language>/<device>/ subfolder, dropping the trailing _<hash>_0 and
# the now-redundant locale + device tokens (the folders convey them):
#   AboutDialogShot_Tablet10_ar_7f8b2b52_0.png  ->  arabic/Tablet10/AboutDialogShot.png
for f in "$REF_DIR"/*.png; do
  base="$(basename "$f")"
  # Strip the _<hash>_<index>.png tail, leaving {Screen}_{Device}_{locale}.
  stem="$(echo "$base" | sed -E 's/_[0-9a-f]{8}_[0-9]+\.png$//')"
  locale="${stem##*_}"   # last token
  rest="${stem%_*}"      # {Screen}_{Device}
  device="${rest##*_}"   # second-to-last token (Phone / Tablet7 / Tablet10)
  screen="${rest%_*}"    # {Screen}
  dest="$OUT_DIR/$(lang_dir_for "$locale")/$device"
  mkdir -p "$dest"
  cp "$f" "$dest/$screen.png"
done
echo "    Exported $(find "$OUT_DIR" -name '*.png' | wc -l | tr -d ' ') screenshots across $(find "$OUT_DIR" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ') languages."

# The export is done reading reference/, so hand the tree back now. Called explicitly (rather than
# left to the trap) purely so this runs before the closing message.
restore_state

echo "==> Done. Upload-ready screenshots are in: $OUT_DIR"
