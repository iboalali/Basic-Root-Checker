#!/usr/bin/env python3
"""Render emoji to 512x512 32-bit PNGs for Play Console one-time-product icons.

Uses Noto Color Emoji (the only color-emoji font on this box). Pillow can only
load that font at its single bitmap strike (size=109), so each glyph is rendered
at native size, alpha-cropped, scaled to a padded inner box, and centered on a
transparent 512x512 canvas.
"""
from PIL import Image, ImageDraw, ImageFont

FONT_PATH = "/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf"
FONT_SIZE = 109          # the only size Pillow accepts for NotoColorEmoji
CANVAS = 512
INNER = 256              # glyph fits here -> ~128px padding on each side
OUT_DIR = "/home/iboalali/StudioProjects/Basic-Root-Checker/play-store-assets"

EMOJI = {
    "tip_coffee_512": "☕",            # coffee
    "tip_cake_512": "\U0001F370",          # shortcake
    "tip_gift_512": "\U0001F381",          # wrapped gift
    "tip_heart_512": "❤️",       # red heart
    "tip_hands_512": "\U0001F64F",         # folded hands
}

font = ImageFont.truetype(FONT_PATH, FONT_SIZE)

for name, ch in EMOJI.items():
    # Render the glyph on a scratch canvas, color emoji enabled.
    scratch = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    ImageDraw.Draw(scratch).text((10, 10), ch, font=font, embedded_color=True)

    bbox = scratch.getbbox()
    if bbox is None:
        print(f"!! {name}: '{ch}' rendered empty (font missing this glyph)")
        continue
    glyph = scratch.crop(bbox)

    # Scale to fit the inner box, preserving aspect ratio.
    scale = min(INNER / glyph.width, INNER / glyph.height)
    new_size = (max(1, round(glyph.width * scale)), max(1, round(glyph.height * scale)))
    glyph = glyph.resize(new_size, Image.LANCZOS)

    out = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    out.paste(glyph, ((CANVAS - glyph.width) // 2, (CANVAS - glyph.height) // 2), glyph)

    path = f"{OUT_DIR}/{name}.png"
    out.save(path, "PNG", optimize=True)
    print(f"ok {path}  {out.size[0]}x{out.size[1]}")
