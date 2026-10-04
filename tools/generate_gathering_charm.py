"""Paint the Gathering Charm icon from the shared Spirit Charm template.

Every charm icon is the same 32x32 drawing: a three-tone ring (highlight, mid, shadow) with a dark
outline, and a parchment tag hanging inside it that carries a small glyph. This script copies
sky_charm.png, recolours its ring, wipes Sky's glyph off the tag and draws a horseshoe magnet in its
place, so the new icon cannot drift from the others.

Ring tones follow the template's rule: highlight = mid + 48, shadow = mid - 64 (clamped).
Run from anywhere; it writes into the repository this file lives in.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _repo_guard import REPO  # noqa: E402

ITEMS = REPO / "src/main/resources/assets/tribalpower/textures/item"
TEMPLATE = ITEMS / "sky_charm.png"
OUT = ITEMS / "gathering_charm.png"

OUTLINE = (17, 26, 34, 255)
PARCHMENT = (236, 226, 204, 255)
# Sky's ring tones, swapped for the new ones below.
SKY_RING = {"hi": (255, 255, 255, 255), "mid": (207, 235, 212, 255), "lo": (143, 171, 148, 255)}
# Lodestone crimson: no other charm uses a red ring (Ember is orange, Hearth is tan).
MID = (206, 52, 66)


def tones(mid):
    hi = tuple(min(255, c + 48) for c in mid) + (255,)
    lo = tuple(max(0, c - 64) for c in mid) + (255,)
    return {"hi": hi, "mid": mid + (255,), "lo": lo}


# The tag's glyph field (Sky's arrow sits here). Columns 13-18, rows 13-18.
GLYPH_X, GLYPH_Y = 13, 13
# Horseshoe magnet, open end up: outline-dark steel tips on a ring-red body, the red edged in the
# ring's shadow tone so it holds its shape on the parchment. "." leaves the parchment.
GLYPH = [
    "......",
    "aa..aa",
    "dd..dc",
    "dd..dc",
    "dd..dc",
    "cddddc",
    ".cccc.",
]


def main() -> None:
    im = Image.open(TEMPLATE).convert("RGBA")
    assert im.size == (32, 32), im.size
    new = tones(MID)
    swap = {SKY_RING[k]: new[k] for k in SKY_RING}
    px = im.load()
    for y in range(32):
        for x in range(32):
            if px[x, y] in swap:
                px[x, y] = swap[px[x, y]]
    # Clear Sky's glyph: outline-dark pixels inside the tag's parchment field become parchment.
    for y in range(13, 22):
        for x in range(13, 19):
            if px[x, y] == OUTLINE:
                px[x, y] = PARCHMENT
    for dy, row in enumerate(GLYPH):
        for dx, ch in enumerate(row):
            if ch == "a":
                px[GLYPH_X + dx, GLYPH_Y + dy] = OUTLINE
            elif ch == "c":
                px[GLYPH_X + dx, GLYPH_Y + dy] = new["lo"]
            elif ch == "d":
                px[GLYPH_X + dx, GLYPH_Y + dy] = new["mid"]
    im.save(OUT)
    print("wrote", OUT.relative_to(REPO))


if __name__ == "__main__":
    main()
