"""Generates the Firmament templates: the landing hub, the four Watcher sanctums and the Umbral Throne.

Run: python -I tools/dev/gen_firmament.py
Output: src/main/resources/data/astralvigil/structure/firmament/<name>.nbt

Every template is a floating island: tapered underside, the floor at y = FLOOR, centre at (C, FLOOR, C).
SanctumBuilder places them so that FLOOR lands on Firmament.LANDING.y. Deterministic (seeded). Air is not stored:
the Firmament sky at the build height is empty.

Layout (keep in sync with SanctumBuilder): sanctums 240 blocks east (Solenne), west (Vael), south (Orrin) and north
(Ilyra) of the hub; the Umbral Throne 520 blocks west, its bridge runs through Vael's arena (keep z = 0 clear there).
"""
import gzip
import math
import os
import random
import struct

DATA_VERSION = 3955
FLOOR = 10
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data', 'astralvigil',
                   'structure', 'firmament')


# ------------------------------------------------------------------ NBT
def s(v):
    b = v.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def tag(t, n, p):
    return bytes([t]) + s(n) + p


def i32(v):
    return struct.pack('>i', v)


def lst(t, items):
    return bytes([t]) + struct.pack('>i', len(items)) + b''.join(items)


def comp(e):
    return b''.join(e) + b'\x00'


def write(path, size, blocks):
    palette, index, entries = [], {}, []
    for (x, y, z), (name, props) in sorted(blocks.items()):
        if name == 'minecraft:air':
            continue
        key = (name, props)
        if key not in index:
            index[key] = len(palette)
            pe = [tag(8, 'Name', s(name))]
            if props:
                pe.append(tag(10, 'Properties', comp([tag(8, k, s(v)) for k, v in props])))
            palette.append(comp(pe))
        entries.append(comp([tag(9, 'pos', lst(3, [i32(x), i32(y), i32(z)])), tag(3, 'state', i32(index[key]))]))
    root = tag(10, '', comp([
        tag(3, 'DataVersion', i32(DATA_VERSION)),
        tag(9, 'size', lst(3, [i32(v) for v in size])),
        tag(9, 'palette', lst(10, palette)),
        tag(9, 'blocks', lst(10, entries)),
        tag(9, 'entities', lst(10, [])),
    ]))
    with open(path, 'wb') as f:
        f.write(gzip.compress(root, mtime=0))
    print('wrote', os.path.normpath(path), len(entries), 'blocks, size', size)


STONE = 'astralvigil:firmament_stone'


class Island:
    def __init__(self, radius, height, seed=1, extent=None, floor_block=STONE):
        self.r = radius
        ext = extent or radius
        self.size = (2 * ext + 3, height, 2 * ext + 3)
        self.c = ext + 1
        self.blocks = {}
        self.rnd = random.Random(seed)
        self.disc(self.c, self.c, radius, FLOOR, floor_block)
        self.rim()

    def put(self, x, y, z, name, **props):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, tuple(sorted(props.items())))

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), ('minecraft:air', ()))[0]

    def disc(self, cx, cz, r, top, block=STONE, depth=None):
        """A floating disc with a tapered underside reaching `depth` (default FLOOR) blocks down."""
        depth = FLOOR if depth is None else depth
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r:
                    self.put(x, top, z, block)
                    under = int((1 - d / (r + 0.5)) ** 0.7 * depth) + self.rnd.randint(0, 1)
                    for y in range(max(0, top - under), top):
                        self.put(x, y, z, STONE if y > top - 3 else 'minecraft:calcite')

    def rim(self):
        c, r = self.c, self.r
        n = max(32, int(2 * math.pi * r))
        for i in range(n):
            a = i * 2 * math.pi / n
            self.put(round(c + math.cos(a) * r), FLOOR, round(c + math.sin(a) * r),
                     'minecraft:gold_block' if i % 8 == 0 else 'minecraft:smooth_quartz')

    def column(self, x, z, y0, y1, name, **props):
        for y in range(y0, y1):
            self.put(x, y, z, name, **props)


def gate(isl, cx, cz, width=3, height=4, axis='x'):
    """An Astral Gate frame with a lit Celestial Portal, standing on the floor at (cx, cz)."""
    half = width // 2
    for i in range(-half - 1, half + 2):
        for y in range(FLOOR, FLOOR + height + 2):
            edge = abs(i) == half + 1 or y in (FLOOR, FLOOR + height + 1)
            x, z = (cx + i, cz) if axis == 'x' else (cx, cz + i)
            if edge:
                isl.put(x, y, z, 'astralvigil:astral_gate')
            else:
                isl.put(x, y, z, 'astralvigil:celestial_portal', axis=axis)


# ------------------------------------------------------------------ hub

def landing():
    isl = Island(8, FLOOR + 10, seed=11)
    c = isl.c
    isl.put(c, FLOOR, c, 'astralvigil:starlight_font', facing='up')
    for dx, dz in ((4, 0), (-4, 0), (0, 4)):
        isl.put(c + dx, FLOOR + 1, c + dz, 'minecraft:lantern', hanging='false', waterlogged='false')
    gate(isl, c, c - 5)  # return gate on the north side; arrivals land at (c, c + 3)
    return isl


# ------------------------------------------------------------------ Solenne: the Sun Spire

SPIRE_STEPS = 64


def solenne():
    """A 100-block spire; 64 single pads spiral around it. Gold pads are checkpoints (Solenne's light catches you)."""
    top = FLOOR + 3 + int(SPIRE_STEPS * 1.5)  # last pad stands 2 below the crown
    isl = Island(14, top + 4, seed=21)
    c = isl.c
    for x in range(c - 2, c + 3):
        for z in range(c - 2, c + 3):
            if abs(x - c) + abs(z - c) <= 3:
                for y in range(FLOOR + 1, top):
                    band = (y - FLOOR) % 12 == 0
                    isl.put(x, y, z, 'minecraft:gold_block' if band else 'minecraft:quartz_pillar', **({} if band else {'axis': 'y'}))
    for x in range(c - 2, c + 3):  # crown platform with the goal
        for z in range(c - 2, c + 3):
            isl.put(x, top, z, 'minecraft:gold_block')
    isl.put(c, top + 1, c, 'astralvigil:trial_goal', watcher='solenne')
    y = FLOOR + 1
    for i in range(SPIRE_STEPS):
        a = i * 0.47
        rr = 6.5 + 0.5 * math.sin(i * 0.9)
        x, z = round(c + math.cos(a) * rr), round(c + math.sin(a) * rr)
        y += 2 if i % 2 == 0 else 1
        if i % 16 == 15:  # checkpoint ring
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    isl.put(x + dx, y, z + dz, 'minecraft:smooth_quartz')
            isl.put(x, y, z, 'minecraft:gold_block')
        else:
            isl.put(x, y, z, 'minecraft:magma_block' if i % 7 == 3 else 'minecraft:smooth_quartz')
    return isl


# ------------------------------------------------------------------ Vael: the Moon Arena

def vael():
    """Arena of pillars with six moon targets; four more on floating islets far outside. Echo waves harass archers."""
    isl = Island(26, FLOOR + 30, seed=31, extent=40)
    c = isl.c
    rnd = random.Random(32)
    for i in range(10):  # cover pillars, never on the throne bridge line (z = c)
        a = i * 2 * math.pi / 10 + 0.31
        x, z = round(c + math.cos(a) * 15), round(c + math.sin(a) * 15)
        if abs(z - c) <= 2:
            continue
        isl.column(x, z, FLOOR + 1, FLOOR + 4 + rnd.randint(0, 3), 'minecraft:quartz_pillar', axis='y')
    for i in range(6):
        a = i * math.pi / 3 + 0.5
        x, z = round(c + math.cos(a) * 21), round(c + math.sin(a) * 21)
        h = 4 + (i * 5) % 13
        isl.column(x, z, FLOOR + 1, FLOOR + h, 'minecraft:quartz_pillar', axis='y')
        isl.put(x, FLOOR + h, z, 'astralvigil:moon_target', lit='false')
    for i in range(4):  # far islets
        a = i * math.pi / 2 + math.pi / 4
        x, z = round(c + math.cos(a) * 35), round(c + math.sin(a) * 35)
        y = FLOOR + 10 + i * 4
        isl.disc(x, z, 2.5, y, STONE, depth=3)
        isl.put(x, y + 1, z, 'astralvigil:moon_target', lit='false')
    return isl


# ------------------------------------------------------------------ Orrin: the Prism Observatory

def orrin():
    """Three bays, each: font -> prism -> mirrors (start wrongly turned) -> receptor. All three must glow together."""
    isl = Island(22, FLOOR + 14, seed=41)
    c, y = isl.c, FLOOR + 1
    wall = 'minecraft:quartz_bricks'

    def wall_line(x0, x1, z, h=5):
        for x in range(x0, x1 + 1):
            for yy in range(y, y + h):
                isl.put(x, yy, z, wall)

    for bz in (-19, -6, 6, 19):
        wall_line(c - 19, c + 14, c + bz)
    for z in range(c - 19, c + 20):  # back wall behind the fonts
        for yy in range(y, y + 5):
            isl.put(c - 19, yy, z, wall)
    # glass slits let the player look across the bays
    for bz in (-6, 6):
        for x in range(c - 10, c + 10, 4):
            isl.put(x, y + 2, c + bz, 'minecraft:light_blue_stained_glass')

    def bay(z, mirrors, receptor, obstacle):
        isl.put(c - 17, y, c + z, 'astralvigil:starlight_font', facing='east')
        isl.put(c - 13, y, c + z, 'astralvigil:prism', facing='east')
        for (mx, my, mz, facing) in mirrors:
            isl.put(c + mx, y + my, c + mz, 'astralvigil:mirror', facing=facing)
        rx, ry, rz = receptor
        if ry > 0:
            isl.column(c + rx, c + rz, y, y + ry, 'minecraft:quartz_pillar', axis='y')
        isl.put(c + rx, y + ry, c + rz, 'astralvigil:trial_receptor')
        for (ox, oy, oz) in obstacle:
            isl.put(c + ox, y + oy, c + oz, wall)

    # bay A: north detour (solution: A1 north, A2 east)
    bay(-12, [(-5, 0, -12, 'up'), (-5, 0, -17, 'down')], (8, 0, -17), [(0, 0, -12), (0, 1, -12), (1, 0, -12)])
    # bay B: over the wall (solution: B1 up, B2 east)
    bay(0, [(-6, 0, 0, 'east'), (-6, 5, 0, 'west')], (6, 5, 0), [(0, yy, 0) for yy in range(0, 4)])
    # bay C: around the corner (solution: C1 south, C2 east, C3 north)
    bay(12, [(-7, 0, 12, 'up'), (-7, 0, 17, 'west'), (3, 0, 17, 'down')], (3, 0, 12), [(-2, 0, 12), (-1, 0, 12), (0, 0, 12)])
    return isl


# ------------------------------------------------------------------ Ilyra: the Endless Library

def ilyra():
    """A roofed, dimly lit bookshelf maze (2-block corridors) with the trial goal in the reading room at its heart."""
    n = 17                        # cells per side -> (2n+1) grid lines, 2 blocks per grid line -> 70 blocks
    isl = Island(37, FLOOR + 8, seed=51)
    c = isl.c
    rnd = random.Random(52)
    g = 2 * n + 1
    walls = [[True] * g for _ in range(g)]
    mid = n // 2
    stack, seen = [(mid, mid)], {(mid, mid)}
    walls[2 * mid + 1][2 * mid + 1] = False
    while stack:
        cx, cz = stack[-1]
        nbrs = [(cx + dx, cz + dz, dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                if 0 <= cx + dx < n and 0 <= cz + dz < n and (cx + dx, cz + dz) not in seen]
        if not nbrs:
            stack.pop()
            continue
        nx, nz, dx, dz = rnd.choice(nbrs)
        walls[2 * cx + 1 + dx][2 * cz + 1 + dz] = False
        walls[2 * nx + 1][2 * nz + 1] = False
        seen.add((nx, nz))
        stack.append((nx, nz))
    walls[2 * mid + 1][g - 1] = False          # entrance on the +z side (towards the hub)
    for gx in range(g):                        # reading room in the middle
        for gz in range(g):
            if abs(gx - (2 * mid + 1)) <= 1 and abs(gz - (2 * mid + 1)) <= 1:
                walls[gx][gz] = False
    o = c - g                                   # 2 blocks per grid line
    roof_y = FLOOR + 5
    for x in range(o - 1, o + 2 * g + 1):       # square floor under the whole maze (the island is round)
        for z in range(o - 1, o + 2 * g + 1):
            isl.put(x, FLOOR, z, STONE)
            for yy in range(FLOOR - 3, FLOOR):
                isl.put(x, yy, z, 'minecraft:calcite' if yy < FLOOR - 1 else STONE)
    for gx in range(g):
        for gz in range(g):
            for bx in range(2):
                for bz in range(2):
                    x, z = o + 2 * gx + bx, o + 2 * gz + bz
                    if walls[gx][gz]:
                        for yy in range(FLOOR + 1, roof_y):
                            isl.put(x, yy, z, 'minecraft:bookshelf')
                    elif (gx * 7 + gz * 3) % 23 == 0 and bx == 0 and bz == 0:
                        isl.put(x, roof_y - 1, z, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
                    isl.put(x, roof_y, z, 'minecraft:dark_oak_planks')
    ec = 2 * mid + 1
    isl.put(o + 2 * ec, FLOOR + 1, o + 2 * ec, 'astralvigil:trial_goal', watcher='ilyra')
    for dx, dz in ((-2, -2), (3, -2), (-2, 3), (3, 3)):
        isl.put(o + 2 * ec + dx, FLOOR + 1, o + 2 * ec + dz, 'minecraft:lectern', facing='north', has_book='false', powered='false')
    return isl


# ------------------------------------------------------------------ the Umbral Throne

def throne():
    """The Eclipse's arena: dark stone, eight cover pillars against the Umbral Nova, a seal on the dais."""
    isl = Island(34, FLOOR + 16, seed=61, floor_block='minecraft:polished_blackstone')
    c = isl.c
    for x in range(isl.size[0]):
        for z in range(isl.size[2]):
            d = math.hypot(x - c, z - c)
            if d <= 33 and (int(d) % 6 == 0):
                isl.put(x, FLOOR, z, 'minecraft:crying_obsidian' if int(d) % 12 == 0 else 'minecraft:obsidian')
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        px, pz = round(c + math.cos(a) * 22), round(c + math.sin(a) * 22)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                isl.column(px + dx, pz + dz, FLOOR + 1, FLOOR + 10, 'minecraft:obsidian')
        isl.put(px, FLOOR + 10, pz, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
    for x in range(c - 4, c + 5):
        for z in range(c - 4, c + 5):
            isl.put(x, FLOOR + 1, z, 'minecraft:polished_blackstone_bricks')
    isl.put(c, FLOOR + 2, c, 'astralvigil:eclipse_seal')
    for dx, dz in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        isl.column(c + dx, c + dz, FLOOR + 2, FLOOR + 5, 'minecraft:purple_stained_glass')
    return isl


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (('landing', landing), ('solenne', solenne), ('vael', vael), ('orrin', orrin), ('ilyra', ilyra),
                     ('throne', throne)):
        isl = fn()
        write(os.path.join(OUT, name + '.nbt'), isl.size, isl.blocks)
