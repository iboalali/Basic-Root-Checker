#!/usr/bin/env bash
#
# Render upload-ready Play Store screenshots with the PRODUCTION app name.
#
# This is a launcher. The script itself is `render-store-screenshots.py` in the `iboalali-apps` kit,
# so a fix reaches every app at once instead of being copied and diverging — which is exactly what
# had happened: three repos held three versions of this file with three different restore strategies,
# and each had a safety check the others lacked. Everything app-specific now comes from the
# `screenshots.render` section of "Play Store/store.json".
#
#   ./scripts/generate-store-screenshots.sh [VERSION] [--dry-run]
#
#     VERSION     subfolder of the screenshots dir to write, e.g. "v2.2". Omitted, the export goes
#                 to the dir root and nothing is cleared.
#     --dry-run   print the plan (task, files to patch, name mapping) and change nothing.
#
# Kept as its own entry point rather than a `./scripts/store` verb because that is the name the docs,
# the CLAUDE.md build commands and muscle memory all use. Set AI_KIT_SCRIPTS to point at a checkout
# when iterating on the kit itself.
set -euo pipefail

find_scripts_dir() {
  if [ -n "${AI_KIT_SCRIPTS:-}" ]; then
    printf '%s\n' "$AI_KIT_SCRIPTS"
    return
  fi
  # The plugin cache path carries the plugin version, so glob it and take the highest.
  local candidate
  candidate="$(ls -d "$HOME"/.claude/plugins/cache/ai-kit/iboalali-apps/*/scripts 2>/dev/null | sort -V | tail -1)"
  if [ -n "$candidate" ]; then
    printf '%s\n' "$candidate"
    return
  fi
  # Fall back to a local checkout of the kit.
  if [ -d "$HOME/Projects/ai-kit/iboalali-apps/scripts" ]; then
    printf '%s\n' "$HOME/Projects/ai-kit/iboalali-apps/scripts"
    return
  fi
  echo "error: cannot find the iboalali-apps scripts." >&2
  echo "  Install the kit (/plugin install iboalali-apps) or set AI_KIT_SCRIPTS." >&2
  exit 1
}

dir="$(find_scripts_dir)"
script="$dir/render-store-screenshots.py"
[ -f "$script" ] || { echo "error: $script not found" >&2; exit 1; }

# Run from the repo root: the script searches upward for "Play Store/store.json", and the Gradle
# render it drives has to run from here too.
cd "$(dirname "$0")/.."
exec python3 "$script" "$@"
