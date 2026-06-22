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

echo "==> Exporting clean PNGs to: $OUT_DIR"
mkdir -p "$OUT_DIR"
# Copy each reference PNG, dropping the trailing _<hash>_0 so names are upload-friendly:
#   MainRootedShot_Phone_ar_7f8b2b52_0.png  ->  MainRootedShot_Phone_ar.png
for f in "$REF_DIR"/*.png; do
  base="$(basename "$f")"
  clean="$(echo "$base" | sed -E 's/_[0-9a-f]{8}_[0-9]+\.png$/.png/')"
  cp "$f" "$OUT_DIR/$clean"
done
echo "    Exported $(ls -1 "$OUT_DIR"/*.png | wc -l | tr -d ' ') screenshots."

echo "==> Restoring debug app name and the regression baseline…"
restore_strings
trap - EXIT
# Re-render so the committed reference/ baseline matches the normal "(Debug)" debug render again.
./gradlew :app:updateGplayDebugScreenshotTest --rerun-tasks

echo "==> Done. Upload-ready screenshots are in: $OUT_DIR"
