#!/usr/bin/env bash
#
# Render the Play Console in-app-product icons (512×512) from the app's OWN vector drawables.
#
# Each IAP icon is a white Material glyph, centered on the launcher purple. Taking the glyph straight
# from `res/drawable/` means the store icon is the same artwork the app draws, so it cannot drift from
# the UI: every tip uses `favorite_24px`, the heart on the main screen's support card.
#
# Two files per product, matching what the Play Console wants:
#   icon.png          full-bleed, RGB (no alpha): the icon proper
#   icon-rounded.png  same art with rounded corners and transparent outside, a fallback for when the
#                     purchase sheet renders the icon un-masked
#
# How it renders: an SVG is built from the drawable's pathData (VectorDrawable path syntax is a subset
# of SVG's) and shot by headless Chrome, so no ImageMagick/librsvg is needed. The glyph is scaled and
# centered by measuring its real bounding box in the browser (getBBox), so a different drawable needs
# no hand-tuned offsets.
#
# Usage:  scripts/generate-iap-icons.sh
set -euo pipefail

# Repo root = parent of this script's dir.
cd "$(dirname "$0")/.."

DRAWABLES="app/src/main/res/drawable"
OUT_ROOT="Play Store/In-App Products"

SIZE=512
# Longest side of the glyph's bounding box, in output pixels. 224 is 44% of 512, comfortably inside
# Play's icon safe area.
GLYPH=224
# The launcher-icon background, `ic_launcher_v2_background` -> `md_purple_500`.
BG="#673AB7"
# Corner radius of the rounded variant, the same as in the other apps' IAP icons.
RADIUS=92

# product folder <TAB> drawable name (without .xml)
PRODUCTS=$(
  cat <<'EOF'
small	favorite_24px
medium	favorite_24px
large	favorite_24px
EOF
)

CHROME=""
for c in google-chrome google-chrome-stable chromium chromium-browser; do
  if command -v "$c" >/dev/null 2>&1; then CHROME="$c"; break; fi
done
if [ -z "$CHROME" ]; then
  echo "error: no google-chrome/chromium on PATH, needed to render the SVG." >&2
  exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

echo "==> Rendering IAP icons on $BG"

while IFS=$'\t' read -r product drawable; do
  [ -z "${product:-}" ] && continue

  src="$DRAWABLES/$drawable.xml"
  if [ ! -f "$src" ]; then
    echo "error: drawable not found: $src" >&2
    exit 1
  fi
  if [ ! -d "$OUT_ROOT/$product" ]; then
    echo "error: no product folder: $OUT_ROOT/$product" >&2
    exit 1
  fi

  # Two variants: full-bleed (square, flattened to RGB) and rounded (transparent outside).
  for variant in flat rounded; do
    if [ "$variant" = "flat" ]; then
      rx=0
      out="$OUT_ROOT/$product/icon.png"
    else
      rx="$RADIUS"
      out="$OUT_ROOT/$product/icon-rounded.png"
    fi

    html="$work/$product-$variant.html"
    SRC="$src" HTML="$html" SIZE="$SIZE" GLYPH="$GLYPH" BG="$BG" RX="$rx" python3 - <<'PY'
import os, pathlib, re

src = pathlib.Path(os.environ["SRC"]).read_text(encoding="utf-8")

# Pull every path out of the VectorDrawable. VectorDrawable pathData uses SVG path syntax, so the
# strings transfer verbatim; only the viewport has to become the SVG viewBox.
# Each path keeps its own android:fillAlpha, so a translucent part of the artwork (the launcher mark's
# inner fill) stays translucent instead of rendering solid white.
paths = []
for element in re.findall(r"<path\b[^>]*>", src):
    data = re.search(r'android:pathData\s*=\s*"([^"]+)"', element)
    if data:
        alpha = re.search(r'android:fillAlpha\s*=\s*"([^"]+)"', element)
        paths.append((data.group(1), alpha.group(1) if alpha else "1"))
if not paths:
    raise SystemExit(f"{os.environ['SRC']}: no android:pathData found")

# NB: the drawable's viewportWidth/Height is deliberately NOT used. The glyph is placed by measuring
# its own bounding box and fitting that to GLYPH px, which normalises away both the viewport size and
# whatever padding the glyph carries inside it.
size = int(os.environ["SIZE"])
glyph = float(os.environ["GLYPH"])
shapes = "".join(f'<path d="{d}" fill-opacity="{a}"/>' for d, a in paths)

pathlib.Path(os.environ["HTML"]).write_text(
    f"""<!doctype html>
<html><head><meta charset="utf-8"><style>
  html,body{{margin:0;padding:0;width:{size}px;height:{size}px;overflow:hidden;background:transparent}}
</style></head><body>
<svg width="{size}" height="{size}" viewBox="0 0 {size} {size}"
     xmlns="http://www.w3.org/2000/svg" shape-rendering="geometricPrecision">
  <rect width="{size}" height="{size}" rx="{os.environ['RX']}" fill="{os.environ['BG']}"/>
  <g id="glyph" fill="#ffffff"><g id="art">{shapes}</g></g>
</svg>
<script>
  /* Scale and centre by the glyph's REAL bounding box rather than the drawable's viewport: Material
     glyphs sit inside a padded 960-unit box, and the padding differs per glyph (a bookmark is much
     taller than it is wide). Measuring getBBox() makes every icon optically the same size without any
     per-glyph nudging.

     #art carries NO transform of its own on purpose. getBBox() reports coordinates in the element's
     own user space, i.e. BEFORE its own transform is applied, so scaling #art here and then scaling
     again by this measurement would apply the factor twice (it rendered the glyph at half size). All
     the scaling therefore lives on the #glyph wrapper, and b is in raw path units. */
  var art = document.getElementById('art'), wrap = document.getElementById('glyph');
  var b = art.getBBox();
  var s = {glyph} / Math.max(b.width, b.height);
  var tx = ({size} - b.width * s) / 2 - b.x * s;
  var ty = ({size} - b.height * s) / 2 - b.y * s;
  wrap.setAttribute('transform', 'translate(' + tx + ',' + ty + ') scale(' + s + ')');
</script>
</body></html>
""",
    encoding="utf-8",
)
PY

    "$CHROME" \
      --headless \
      --disable-gpu \
      --no-sandbox \
      --hide-scrollbars \
      --force-device-scale-factor=1 \
      --default-background-color=00000000 \
      --window-size="$SIZE,$SIZE" \
      --screenshot="$work/$product-$variant.png" \
      "$html" >/dev/null 2>&1

    if [ ! -f "$work/$product-$variant.png" ]; then
      echo "error: Chrome produced no screenshot for $product/$variant." >&2
      exit 1
    fi

    IN="$work/$product-$variant.png" OUT="$out" SIZE="$SIZE" VARIANT="$variant" BG="$BG" python3 - <<'PY'
import os
from PIL import Image

size = int(os.environ["SIZE"])
im = Image.open(os.environ["IN"])
if im.size != (size, size):
    raise SystemExit(f"error: rendered {im.size}, expected {(size, size)}")

if os.environ["VARIANT"] == "flat":
    # The icon proper: no alpha channel.
    im = im.convert("RGBA")
    bg = os.environ["BG"].lstrip("#")
    flat = Image.new("RGB", im.size, tuple(int(bg[i : i + 2], 16) for i in (0, 2, 4)))
    flat.paste(im, mask=im.split()[-1])
    im = flat
else:
    im = im.convert("RGBA")

im.save(os.environ["OUT"], "PNG", optimize=True)
PY

    echo "    $out"
  done
done <<<"$PRODUCTS"

echo "==> Done."
