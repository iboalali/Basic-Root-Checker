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
restore_strings() {
  for i in "${!DEBUG_STRINGS[@]}"; do
    if [ -n "${backups[$i]:-}" ] && [ -f "${backups[$i]}" ]; then
      mv -f "${backups[$i]}" "${DEBUG_STRINGS[$i]}"
    fi
  done
}
# Safety net: restore the debug strings no matter how we exit.
trap restore_strings EXIT

echo "==> Neutralizing the debug \"(Debug)\" app name for the render…"
for i in "${!DEBUG_STRINGS[@]}"; do
  f="${DEBUG_STRINGS[$i]}"
  bak="$(mktemp)"
  cp "$f" "$bak"
  backups[$i]="$bak"
  sed -i 's/Basic Root Checker (Debug)/Basic Root Checker/g' "$f"
done

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

echo "==> Restoring debug app name and the regression baseline…"
restore_strings
trap - EXIT
# Re-render so the committed reference/ baseline matches the normal "(Debug)" debug render again.
./gradlew :app:updateGplayDebugScreenshotTest --rerun-tasks

echo "==> Done. Upload-ready screenshots are in: $OUT_DIR"
