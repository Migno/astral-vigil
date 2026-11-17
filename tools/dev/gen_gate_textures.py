"""Generates the Astral Gate block texture and the animated Celestial Portal texture (+ .mcmeta).

Run: python -I tools/dev/gen_gate_textures.py
Art rules: 16x16 (portal: 8 animation frames), <= 8 colours, Void Navy outline, light from the top-left.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "textures", "block")
C = png.hexc


# per gate kind: outline, body light, body, body shade, trim, trim dark, sigil, sigil dark
THEMES = {
    "astral": ("1E1A2E", "FFFFFF", "D8D8E6", "B9B3CC", "F2C94C", "C9971E", "8FEFFF", "2FA8D0"),
    "solar": ("7A5A12", "FFF4C0", "F2C94C", "C9971E", "FFFFFF", "E0A030", "FF8A3D", "C2410C"),
    "lunar": ("1E1A2E", "FFFFFF", "C8D4EA", "9AA3B8", "E6EEFA", "7F93B8", "8FEFFF", "2FA8D0"),
    "prism": ("1E1A2E", "B9F6FF", "4F6F8F", "33506B", "C9971E", "7A5A12", "FFFFFF", "8FEFFF"),
    "archive": ("0F0C1A", "6B4A2B", "4A3220", "2F1F14", "F2C94C", "C9971E", "B9F6FF", "4A4790"),
    "umbral": ("0B0912", "3A2560", "2C1F45", "1E1A2E", "8C5AC8", "5B2A86", "E0C8FF", "B57CFF"),
}
# portal swirl: deep, mid, bright, star a, star b, star c
PORTAL_THEMES = {
    "celestial_portal": ("1E1A2E", "2C2942", "5B2A86", "2FA8D0", "8FEFFF", "FFFFFF"),
    "solar_portal": ("7A3A0A", "C2410C", "E0A030", "F2C94C", "FFF4C0", "FFFFFF"),
    "lunar_portal": ("1E1A2E", "33506B", "7F93B8", "C8D4EA", "E6EEFA", "FFFFFF"),
    "prism_portal": ("0F2A3A", "2FA8D0", "5B2A86", "F2C94C", "8FEFFF", "FFFFFF"),
    "archive_portal": ("0F0C1A", "2C2A5E", "4A4790", "C9971E", "B9F6FF", "FFFFFF"),
    "umbral_portal": ("05040A", "1E1A2E", "5B2A86", "8C5AC8", "B57CFF", "E0C8FF"),
}


def gate(theme="astral"):
    navy, white, pale, shade, gold, gold_d, cyan, cyan_d = (C(c) for c in THEMES[theme])
    img = png.blank(16, 16)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = navy
            elif x in (1, 14) or y in (1, 14):
                c = gold if (x + y) % 2 == 0 else gold_d
            elif x == 2 or y == 2:
                c = white
            elif x == 13 or y == 13:
                c = shade
            else:
                c = pale if (x * 7 + y * 3) % 11 else white
            img[y][x] = c
    # four-pointed star in the centre
    for (x, y), c in {(7, 4): cyan, (8, 4): cyan, (7, 11): cyan_d, (8, 11): cyan_d, (4, 7): cyan, (4, 8): cyan_d,
                      (11, 7): cyan, (11, 8): cyan_d}.items():
        img[y][x] = c
    for y in range(5, 11):
        for x in range(5, 11):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 2.5:
                img[y][x] = white if d < 1.2 else cyan
            elif d <= 3.5:
                img[y][x] = cyan_d
    return img


def portal(theme="celestial_portal", frames=8):
    t6 = PORTAL_THEMES[theme]
    deep, mid, violet = C(t6[0], 200), C(t6[1], 190), C(t6[2], 190)
    cyan, glow, white = C(t6[3], 210), C(t6[4], 220), C(t6[5], 235)
    rnd = random.Random(5)
    stars = [(rnd.random() * 16, rnd.random() * 16, rnd.choice((cyan, glow, white))) for _ in range(9)]
    rows = []
    for f in range(frames):
        t = f / frames
        img = png.blank(16, 16)
        for y in range(16):
            for x in range(16):
                # slow swirl around the centre
                a = math.atan2(y - 7.5, x - 7.5)
                r = math.hypot(x - 7.5, y - 7.5)
                v = math.sin(a * 2 + r * 0.7 - t * 2 * math.pi)
                img[y][x] = violet if v > 0.55 else mid if v > -0.2 else deep
        for sx, sy, c in stars:
            x = int((sx + t * 16) % 16)
            y = int((sy - t * 8) % 16)
            img[y][x] = c
            if c is white:
                img[y][(x + 1) % 16] = glow
        rows.extend(img)
    return rows


MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "models", "block")
PORTAL_MODEL = """{
  "parent": "minecraft:block/nether_portal_%s",
  "render_type": "minecraft:translucent",
  "textures": {
    "particle": "astralvigil:block/%s",
    "portal": "astralvigil:block/%s"
  }
}
"""


def main():
    images = [(k + "_gate", gate(k)) for k in THEMES] + [(k, portal(k)) for k in PORTAL_THEMES]
    for name, img in images:
        n = len(png.colours(img))
        assert n <= 8, f"{name}: {n} colours"
        png.write(os.path.join(OUT, name + ".png"), img)
    for name in PORTAL_THEMES:
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w", encoding="utf-8", newline="\n") as f:
            f.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')
        for axis in ("ns", "ew"):
            with open(os.path.join(MODELS, f"{name}_{axis}.json"), "w", encoding="utf-8", newline="\n") as f:
                f.write(PORTAL_MODEL % (axis, name, name))
    print("gate textures + portal models written:", len(images))


if __name__ == "__main__":
    main()
