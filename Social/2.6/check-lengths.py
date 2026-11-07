#!/usr/bin/env python3
"""Check every social post in this folder against its network's limit.

Run from the repo root:  python3 "Play Store/Release Notes/<version>/social/check-lengths.py"

Thread files are split on the `--- n/N` marker lines and each post is checked on its own —
a thread is only as valid as its longest post.

Two counting rules differ by network and they are the whole reason this script exists:
  * Mastodon counts EVERY link as 23 characters, whatever its real length.
  * Bluesky counts the full URL text, and counts graphemes rather than code points
    (an emoji is 1). Python's len() counts code points, so this over-counts only if a
    post uses ZWJ emoji sequences (family/flag/skin-tone). Keep to simple emoji and the
    two agree; the script warns if it spots one.
"""

import pathlib
import re
import sys

HERE = pathlib.Path(__file__).parent
URL = re.compile(r"https?://\S+|(?<!\w)[\w.-]+\.(?:com|org|net|io|dev)/\S*")
MARKER = re.compile(r"^--- \d+/\d+\s*$", re.MULTILINE)
ZWJ = "‍"

# file, limit, link weight (None = count the URL in full), is a thread
FILES = [
    ("bluesky.txt", 300, None, False),
    
    ("mastodon.txt", 500, 23, False),
    
    ("linkedin.txt", 3000, None, False),
]


def measure(text: str, weight: int | None) -> int:
    if weight is not None:
        text = URL.sub("x" * weight, text)
    return len(text)


def main() -> int:
    failed = False
    for name, limit, weight, threaded in FILES:
        path = HERE / name
        if not path.exists():
            print(f"MISS {name}: not found")
            failed = True
            continue
        raw = path.read_text(encoding="utf-8")
        posts = [p.strip() for p in MARKER.split(raw) if p.strip()] if threaded else [raw.strip()]
        print(f"\n{name}  (limit {limit})")
        for i, post in enumerate(posts, 1):
            n = measure(post, weight)
            ok = n <= limit
            failed |= not ok
            label = f"  {i}/{len(posts)}" if threaded else "  "
            print(f"  {'OK  ' if ok else 'OVER'}{label} {n:4}/{limit}" + ("" if ok else f"  ({n - limit} over)"))
            if ZWJ in post:
                print("       note: ZWJ emoji present — real grapheme count may be lower")
    print("\n" + ("FAIL — something is over its limit" if failed else "All posts fit."))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
