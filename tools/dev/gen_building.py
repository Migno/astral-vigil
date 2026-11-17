"""Generates the textures of the celestial building blocks (registry/AVBuilding.java).

Run: python -I tools/dev/gen_building.py
Art rules: 16x16, vanilla style, few colours, dark outline where a block has a frame, light from the top-left.
Deterministic (seeded per texture).
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "textures", "block")
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "textures", "item")
C = png.hexc
CLEAR = (0, 0, 0, 0)

NAVY, NAVY2 = C("1E1A2E"), C("2C2942")
CYAN, CYAN_D, WHITE = C("8FEFFF"), C("2FA8D0"), C("FFFFFF")
GOLD, GOLD_D, GOLD_DD = C("F2C94C"), C("C9971E"), C("7A5A12")
VIOLET, VIOLET_L = C("5B2A86"), C("8C5AC8")


def img(fill=CLEAR):
    return [[fill for _ in range(16)] for _ in range(16)]


def noise(seed):
    rnd = random.Random(seed)
    return [[rnd.random() for _ in range(16)] for _ in range(16)]


def pick(v, cols, cuts):
    """Maps a 0..1 value onto cols using ascending cut points."""
    for c, cut in zip(cols, cuts):
        if v < cut:
            return c
    return cols[-1]


def stars(t, seed, count, cols=(CYAN, WHITE), mask=None):
    rnd = random.Random(seed)
    for _ in range(count):
        x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
        if mask and not mask(x, y):
            continue
        t[y][x] = rnd.choice(cols)


# ------------------------------------------------------------------ starwood

BARK = (C("1E1A3E"), C("2C2A5E"), C("3B3A78"), C("4A4790"))
WOOD = (C("A9A2C8"), C("C4BEDC"), C("D8D8E6"), C("ECEAF6"))
PLANK = (C("23214A"), C("2C2A5E"), C("3B3A78"), C("4A4790"), C("6A68B0"))


def starwood_log():
    n = noise(11)
    t = img()
    for y in range(16):
        for x in range(16):
            stripe = (x * 5 + (y // 3) * 2) % 7
            v = n[y][x] * 0.55 + (0.45 if stripe in (0, 1) else 0.0) * (1 if x % 4 else 0.3)
            t[y][x] = pick(v, BARK, (0.25, 0.5, 0.78))
            if x % 4 == 0 and n[y][x] < 0.6:
                t[y][x] = BARK[0]
    stars(t, 12, 5, (CYAN, CYAN_D, WHITE))
    return t


def log_top(outer, rings, seed):
    t = img()
    n = noise(seed)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            r = math.hypot(x - 7.5, y - 7.5)
            if d > 5.5:
                t[y][x] = pick(n[y][x], outer, (0.35, 0.7))
            elif r < 1.3:
                t[y][x] = WHITE
            elif r < 2.2:
                t[y][x] = CYAN
            else:
                t[y][x] = rings[int(r + n[y][x] * 0.6) % len(rings)]
    return t


def stripped_log():
    n = noise(21)
    t = img()
    for y in range(16):
        for x in range(16):
            grain = math.sin(x * 1.7 + math.sin(y * 0.6) * 1.2)
            t[y][x] = pick(grain * 0.4 + n[y][x] * 0.6, WOOD, (0.15, 0.4, 0.7))
            if x in (0, 15):
                t[y][x] = WOOD[0]
    stars(t, 22, 3, (CYAN, CYAN_D))
    return t


def planks(seed=31):
    n = noise(seed)
    t = img()
    for y in range(16):
        board = y // 4
        offset = (board * 5) % 16
        for x in range(16):
            v = n[y][x] * 0.6 + 0.2 + (0.15 if (x + offset) % 16 < 8 else 0.0)
            c = pick(v, PLANK[1:], (0.4, 0.62, 0.86))
            if y % 4 == 3:
                c = PLANK[0]
            elif (x + offset) % 16 == 0:
                c = PLANK[0]
            elif y % 4 == 0 and n[y][x] > 0.7:
                c = PLANK[4]
            t[y][x] = c
    stars(t, seed + 1, 3, (CYAN_D,), mask=lambda x, y: y % 4 != 3)
    return t


def gilded_starwood():
    t = planks(41)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            t[y][x] = GOLD_D
        for (x, y) in ((i, 1), (1, i)):
            if 0 < i < 15:
                t[y][x] = GOLD
    # diagonal gold inlay to a cyan star in the centre
    for i in range(2, 14):
        if abs(i - 7.5) > 1.5:
            t[i][i] = GOLD
            t[i][15 - i] = GOLD_D
    for (x, y), c in {(7, 6): CYAN, (8, 6): CYAN, (6, 7): CYAN, (9, 7): CYAN_D, (6, 8): CYAN, (9, 8): CYAN_D,
                      (7, 9): CYAN_D, (8, 9): CYAN_D, (7, 7): WHITE, (8, 7): WHITE, (7, 8): WHITE, (8, 8): CYAN}.items():
        t[y][x] = c
    return t


def door(top):
    t = planks(51 if top else 52)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                t[y][x] = GOLD_DD
            elif x in (1, 14) or (top and y == 1) or (not top and y == 14):
                t[y][x] = GOLD if (x + y) % 3 else GOLD_D
    if top:
        # star window
        for y in range(3, 12):
            for x in range(3, 13):
                d = abs(x - 7.5) + abs(y - 7)
                if d < 5.2:
                    t[y][x] = CLEAR if d > 2.2 else (WHITE if d < 1 else CYAN)
                elif d < 6:
                    t[y][x] = GOLD_D
    else:
        for y in (3, 4, 9, 10):
            for x in range(3, 13):
                t[y][x] = PLANK[0] if y in (3, 9) else PLANK[4]
        t[6][7] = t[6][8] = CYAN
        t[7][7] = t[7][8] = CYAN_D
    return t


def trapdoor():
    t = planks(61)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                t[y][x] = GOLD_DD
            elif x in (1, 14) or y in (1, 14):
                t[y][x] = GOLD
            elif 3 <= x <= 12 and 3 <= y <= 12:
                d = abs(x - 7.5) + abs(y - 7.5)
                if 2.5 < d < 4.5 and (x + y) % 2 == 0:
                    t[y][x] = CLEAR
                elif d <= 1.5:
                    t[y][x] = CYAN
    return t


def door_item():
    t = img()
    top, bottom = door(True), door(False)
    for y in range(16):
        for x in range(4, 12):
            src = top if y < 8 else bottom
            t[y][x] = src[(y % 8) * 2][(x - 4) * 2]
    return t


LEAF = (C("1E3A5A"), C("2C5A7A"), C("3F7FA8"), C("5BA3C8"))


def leaves():
    n = noise(71)
    t = img()
    for y in range(16):
        for x in range(16):
            v = n[y][x]
            if v < 0.2:
                t[y][x] = CLEAR
            else:
                t[y][x] = pick((v + ((15 - x) + (15 - y)) / 60), LEAF, (0.45, 0.7, 0.95))
    stars(t, 72, 9, (CYAN, WHITE, C("B9F6FF")))
    return t


def sapling():
    t = img()
    for y in range(8, 16):
        t[y][8] = PLANK[3] if y < 13 else PLANK[1]
    t[11][7] = PLANK[3]
    t[10][9] = PLANK[3]
    for (x, y), c in {(8, 2): WHITE, (7, 3): CYAN, (8, 3): WHITE, (9, 3): CYAN, (8, 4): CYAN, (6, 5): LEAF[2], (7, 5): LEAF[3],
                      (9, 5): LEAF[3], (10, 5): LEAF[2], (5, 6): LEAF[1], (6, 6): LEAF[2], (10, 6): LEAF[2], (11, 6): LEAF[1],
                      (7, 6): LEAF[3], (9, 6): LEAF[3], (8, 5): CYAN, (8, 6): LEAF[2], (8, 7): LEAF[1], (5, 9): LEAF[2],
                      (6, 9): LEAF[3], (6, 10): LEAF[1], (10, 8): LEAF[3], (11, 8): LEAF[2], (11, 9): LEAF[1],
                      (4, 8): CYAN, (12, 7): CYAN_D}.items():
        t[y][x] = c
    return t


# ------------------------------------------------------------------ stone sets

MARBLE = (C("B9B3CC"), C("D8D8E6"), C("E8E8F2"), C("F7F7FC"))
SUN = (C("A86A1A"), C("C9971E"), C("E0A030"), C("F2C94C"), C("FFE08A"))
MOON = (C("7F93B8"), C("9AA3B8"), C("C8D4EA"), C("E6EEFA"))
UMBRA = (C("0B0912"), C("1E1A2E"), C("2C1F45"), C("3A2560"))


def stone(cols, seed, veins=None, vein_col=None):
    n = noise(seed)
    t = img()
    rnd = random.Random(seed + 5)
    for y in range(16):
        for x in range(16):
            v = (n[y][x] + n[(y + 1) % 16][x] + n[y][(x + 1) % 16]) / 3
            v += ((15 - x) + (15 - y)) / 120 - 0.12
            t[y][x] = pick(v, cols[1:], [0.38 + i * 0.18 for i in range(len(cols) - 2)])
    if veins:
        for _ in range(veins):
            x, y = rnd.randrange(16), rnd.randrange(16)
            dx = rnd.choice((-1, 1))
            for _ in range(rnd.randrange(4, 9)):
                t[y % 16][x % 16] = vein_col or cols[0]
                x += dx if rnd.random() < 0.6 else 0
                y += 1
    return t


def bricks(base, mortar, highlight=None, w=8, h=4):
    t = [row[:] for row in base]
    for y in range(16):
        for x in range(16):
            row = y // h
            off = (w // 2) * (row % 2)
            if y % h == h - 1 or (x + off) % w == w - 1:
                t[y][x] = mortar
            elif highlight and (y % h == 0 or (x + off) % w == 0):
                t[y][x] = highlight
    return t


def chiseled_marble():
    t = stone(MARBLE, 81)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            t[y][x] = MARBLE[0]
        for (x, y) in ((i, 1), (1, i)):
            if 0 < i < 15:
                t[y][x] = GOLD_D
        for (x, y) in ((i, 14), (14, i)):
            if 0 < i < 15:
                t[y][x] = GOLD_DD
    # eight-rayed sun star
    for y in range(3, 13):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            ray = abs(math.cos(a * 4)) > 0.82
            if r < 1.3:
                t[y][x] = WHITE
            elif r < 2.3:
                t[y][x] = CYAN
            elif r < 3.0:
                t[y][x] = GOLD
            elif ray and r < 5.2:
                t[y][x] = GOLD if dx + dy < 0 else GOLD_D
    return t


def pillar_side():
    t = img()
    for y in range(16):
        for x in range(16):
            flute = x % 4
            c = (MARBLE[3], MARBLE[2], MARBLE[1], MARBLE[0])[flute]
            if x in (0, 15):
                c = MARBLE[0]
            t[y][x] = c
    for y in (0, 15):
        for x in range(16):
            t[y][x] = GOLD_DD if y == 15 else GOLD_D
    for x in range(16):
        t[1][x] = GOLD
        t[14][x] = GOLD_D
    return t


def pillar_top():
    t = stone(MARBLE, 91)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15):
                t[y][x] = MARBLE[0]
            elif 5.2 < r < 6.4:
                t[y][x] = GOLD if x + y < 15 else GOLD_D
            elif r < 1.5:
                t[y][x] = CYAN
    return t


def chiseled_umbral():
    t = bricks(stone(UMBRA, 101), UMBRA[0])
    for y in range(2, 14):
        for x in range(1, 15):
            dx, dy = (x - 7.5) / 6.2, (y - 7.5) / 3.6
            e = dx * dx + dy * dy
            if e <= 1.0:
                r = math.hypot(x - 7.5, y - 7.5)
                t[y][x] = WHITE if r < 1.2 else C("E0C8FF") if r < 2.0 else C("B57CFF") if r < 2.9 else UMBRA[1]
            elif e <= 1.35:
                t[y][x] = VIOLET
    return t


def lamp(frame, frame_hi, core, core_hi, core_lo, seed):
    t = img()
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                t[y][x] = frame
            elif x in (1, 14) or y in (1, 14):
                t[y][x] = frame_hi if (x + y) % 2 else frame
            else:
                r = math.hypot(x - 7.5, y - 7.5)
                lattice = (x - 2) % 4 == 3 or (y - 2) % 4 == 3
                if lattice and r > 2:
                    t[y][x] = frame
                else:
                    t[y][x] = core_hi if r < 2.5 else core if rnd.random() < 0.75 else core_lo
    return t


def constellation_tiles():
    t = img()
    n = noise(111)
    for y in range(16):
        for x in range(16):
            t[y][x] = NAVY2 if n[y][x] > 0.4 else NAVY
            if x % 8 == 7 or y % 8 == 7:
                t[y][x] = C("0F0C1A")
    pts = [(2, 2), (5, 4), (4, 9), (10, 3), (12, 5), (11, 11), (13, 13), (3, 13)]
    for (ax, ay), (bx, by) in ((pts[0], pts[1]), (pts[1], pts[2]), (pts[3], pts[4]), (pts[4], pts[5]), (pts[5], pts[6])):
        steps = max(abs(bx - ax), abs(by - ay))
        for i in range(steps + 1):
            x = round(ax + (bx - ax) * i / steps)
            y = round(ay + (by - ay) * i / steps)
            t[y][x] = GOLD_DD
    for i, (x, y) in enumerate(pts):
        t[y][x] = WHITE if i % 3 == 0 else CYAN
    return t


def halo_glass():
    t = img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                t[y][x] = GOLD_D
            elif (x in (1, 14) or y in (1, 14)) and (x <= 3 or x >= 12 or y <= 3 or y >= 12):
                t[y][x] = GOLD
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t[y][x] = CYAN
    for i in range(3):
        t[3 + i][5 - i] = C("FFFFFF", 150)
    t[4][9] = C("FFFFFF", 110)
    return t


def nebula_glass():
    t = img()
    n = noise(121)
    for y in range(16):
        for x in range(16):
            v = (n[y][x] + n[y][(x + 3) % 16] + math.sin((x + y) * 0.5) * 0.5 + 1) / 3
            t[y][x] = C("5B2A86", 120) if v > 0.62 else C("2C2A5E", 110) if v > 0.42 else C("2FA8D0", 90)
            if x in (0, 15) or y in (0, 15):
                t[y][x] = C("8C5AC8", 170)
    stars(t, 122, 6, (C("FFFFFF", 220), C("8FEFFF", 200)))
    return t


def lantern():
    t = img()
    # cap side (1..5, 0..2), body side (0..6, 2..9), body top/bottom (0..6, 9..15), handle (11..14, 1..3 / 10..12)
    for x in range(1, 5):
        t[0][x] = GOLD
        t[1][x] = GOLD_D
    for y in range(2, 9):
        for x in range(6):
            if x in (0, 5) or y in (2, 8):
                t[y][x] = GOLD_DD if y == 8 or x == 5 else GOLD_D
            else:
                t[y][x] = WHITE if (x, y) in ((2, 5), (3, 5), (2, 4)) else CYAN if 3 <= y <= 6 else CYAN_D
    for y in range(9, 15):
        for x in range(6):
            t[y][x] = GOLD_D if x in (0, 5) or y in (9, 14) else GOLD
    for x in range(1, 5):
        for y in range(10, 14):
            t[y][x] = GOLD if (x + y) % 2 else GOLD_D
    for x in range(11, 14):
        t[1][x] = GOLD
        t[2][x] = GOLD_D
        t[10][x] = GOLD
        t[11][x] = GOLD_D
    return t


def lantern_item():
    t = img()
    for x in range(7, 9):
        t[1][x] = GOLD_D
    t[2][6] = t[2][9] = GOLD_D
    for x in range(5, 11):
        t[3][x] = GOLD
        t[4][x] = GOLD_D
    for y in range(5, 13):
        for x in range(4, 12):
            if x in (4, 11):
                t[y][x] = GOLD_D if x == 4 else GOLD_DD
            else:
                r = math.hypot(x - 7.5, y - 8.5)
                t[y][x] = WHITE if r < 1.3 else CYAN if r < 2.6 else CYAN_D
    for x in range(4, 12):
        t[13][x] = GOLD_D
        t[14][x] = GOLD_DD
    for y in range(5, 13):
        t[y][7] = GOLD_D if y not in (8, 9) else t[y][7]
    return t


def main():
    marble = stone(MARBLE, 131, veins=3, vein_col=MARBLE[0])
    sun = stone(SUN, 141, veins=2, vein_col=C("FFF4C0"))
    moon = stone(MOON, 151, veins=3, vein_col=C("FFFFFF"))
    umbra = stone(UMBRA, 161, veins=2, vein_col=VIOLET)
    blocks = {
        "starwood_log": starwood_log(),
        "starwood_log_top": log_top(BARK[:2], (WOOD[1], WOOD[2], WOOD[3], WOOD[2]), 13),
        "stripped_starwood_log": stripped_log(),
        "stripped_starwood_log_top": log_top((WOOD[0], WOOD[1]), (WOOD[2], WOOD[3], WOOD[1], WOOD[3]), 23),
        "starwood_planks": planks(),
        "gilded_starwood": gilded_starwood(),
        "starwood_door_top": door(True),
        "starwood_door_bottom": door(False),
        "starwood_trapdoor": trapdoor(),
        "starwood_leaves": leaves(),
        "starwood_sapling": sapling(),
        "celestial_marble": marble,
        "celestial_bricks": bricks(stone(MARBLE, 132), MARBLE[0], MARBLE[3]),
        "chiseled_celestial_marble": chiseled_marble(),
        "celestial_pillar": pillar_side(),
        "celestial_pillar_top": pillar_top(),
        "gilded_celestial_bricks": bricks(stone(MARBLE, 133), GOLD_D, GOLD),
        "sunstone": sun,
        "sunstone_bricks": bricks(stone(SUN, 142), SUN[0], SUN[4]),
        "moonstone": moon,
        "moonstone_bricks": bricks(stone(MOON, 152), MOON[0], C("FFFFFF")),
        "umbral_stone": umbra,
        "umbral_bricks": bricks(stone(UMBRA, 162), UMBRA[0], VIOLET),
        "chiseled_umbral_bricks": chiseled_umbral(),
        "astral_lamp": lamp(GOLD_DD, GOLD, CYAN, WHITE, CYAN_D, 171),
        "sunstone_lamp": lamp(SUN[0], SUN[2], C("FFE08A"), WHITE, SUN[3], 172),
        "moonstone_lamp": lamp(MOON[0], MOON[2], C("E6EEFA"), WHITE, C("B9F6FF"), 173),
        "umbral_lamp": lamp(UMBRA[0], VIOLET, C("B57CFF"), C("E0C8FF"), VIOLET_L, 174),
        "constellation_tiles": constellation_tiles(),
        "halo_glass": halo_glass(),
        "nebula_glass": nebula_glass(),
        "star_lantern": lantern(),
    }
    for name, t in blocks.items():
        png.write(os.path.join(BLOCK, name + ".png"), t)
    items = {"starwood_door": door_item(), "star_lantern": lantern_item()}
    for name, t in items.items():
        png.write(os.path.join(ITEM, name + ".png"), t)
    print("building textures written:", len(blocks), "blocks,", len(items), "items")


if __name__ == "__main__":
    main()
