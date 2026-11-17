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


def gate():
    navy, white, pale, shade = C("1E1A2E"), C("FFFFFF"), C("D8D8E6"), C("B9B3CC")
    gold, gold_d, cyan, cyan_d = C("F2C94C"), C("C9971E"), C("8FEFFF"), C("2FA8D0")
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


def portal(frames=8):
    deep, violet, mid = C("1E1A2E", 200), C("5B2A86", 190), C("2C2942", 190)
    glow, cyan, white = C("8FEFFF", 220), C("2FA8D0", 210), C("FFFFFF", 235)
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


def main():
    for name, img in (("astral_gate", gate()), ("celestial_portal", portal())):
        n = len(png.colours(img))
        assert n <= 8, f"{name}: {n} colours"
        png.write(os.path.join(OUT, name + ".png"), img)
    with open(os.path.join(OUT, "celestial_portal.png.mcmeta"), "w", encoding="utf-8", newline="\n") as f:
        f.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')
    print("gate textures written to", OUT)


if __name__ == "__main__":
    main()
