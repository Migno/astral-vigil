"""Generates the Firmament templates: landing platform and the four Watcher sanctums.

Run: python -I tools/dev/gen_firmament.py
Output: src/main/resources/data/astralvigil/structure/firmament/<landing|solenne|vael|orrin|ilyra>.nbt

Every template is a floating island: a tapered cloud-stone underside, the floor at y = FLOOR, centre at (C, FLOOR, C).
SanctumBuilder places them so that FLOOR lands on Firmament.LANDING.y. Deterministic (seeded).
"""
import gzip
import math
import os
import random
import struct

DATA_VERSION = 3955
FLOOR = 6
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
    print('wrote', os.path.normpath(path), len(blocks), 'blocks')


class Island:
    def __init__(self, radius, height=24, seed=1):
        self.r = radius
        self.size = (2 * radius + 3, height, 2 * radius + 3)
        self.c = radius + 1
        self.blocks = {}
        self.rnd = random.Random(seed)
        self.base()

    def put(self, x, y, z, name, **props):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, tuple(sorted(props.items())))

    def base(self):
        c, r = self.c, self.r
        for x in range(self.size[0]):
            for z in range(self.size[2]):
                d = math.hypot(x - c, z - c)
                if d <= r:
                    self.put(x, FLOOR, z, 'astralvigil:firmament_stone')
                    # tapered underside
                    depth = int((1 - d / (r + 0.5)) * FLOOR) + self.rnd.randint(0, 1)
                    for y in range(max(0, FLOOR - depth), FLOOR):
                        self.put(x, y, z, 'astralvigil:firmament_stone' if y > FLOOR - 3 else 'minecraft:calcite')
                    for y in range(FLOOR + 1, self.size[1]):
                        self.put(x, y, z, 'minecraft:air')
        # gold rim
        for i in range(64):
            a = i * 2 * math.pi / 64
            self.put(round(c + math.cos(a) * r), FLOOR, round(c + math.sin(a) * r), 'minecraft:gold_block' if i % 8 == 0 else 'minecraft:smooth_quartz')


def landing():
    isl = Island(5, 12, seed=11)
    c = isl.c
    isl.put(c, FLOOR, c, 'astralvigil:starlight_font', facing='up')
    for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        isl.put(c + dx, FLOOR + 1, c + dz, 'minecraft:lantern', hanging='false', waterlogged='false')
    return isl


def solenne():
    """Parkour: golden pads spiralling up around the island to the trial goal."""
    isl = Island(8, 32, seed=21)
    c = isl.c
    steps = 14
    for i in range(steps):
        a = i * 0.9
        rr = 6.0 - i * 0.15
        x = round(c + math.cos(a) * rr)
        z = round(c + math.sin(a) * rr)
        y = FLOOR + 2 + i * 1.5
        y = int(y)
        isl.put(x, y, z, 'minecraft:gold_block' if i % 3 == 0 else 'minecraft:smooth_quartz')
    isl.put(c, FLOOR + 2 + int(steps * 1.5), c, 'minecraft:gold_block')
    isl.put(c, FLOOR + 3 + int(steps * 1.5), c, 'astralvigil:trial_goal', watcher='solenne')
    return isl


def vael():
    """Archery: six moon targets on pillars around the island."""
    isl = Island(11, 18, seed=31)
    c = isl.c
    for i in range(6):
        a = i * math.pi / 3 + 0.3
        x, z = round(c + math.cos(a) * 9), round(c + math.sin(a) * 9)
        h = 2 + (i * 3) % 6
        for y in range(FLOOR + 1, FLOOR + h):
            isl.put(x, y, z, 'minecraft:quartz_pillar', axis='y')
        isl.put(x, FLOOR + h, z, 'astralvigil:moon_target', lit='false')
    return isl


def orrin():
    """Beam puzzle: font -> prism -> (rotate) mirror -> (rotate) mirror -> receptor (needs Stella light)."""
    isl = Island(9, 14, seed=41)
    c, y = isl.c, FLOOR + 1
    isl.put(c - 6, y, c, 'astralvigil:starlight_font', facing='east')
    isl.put(c - 2, y, c, 'astralvigil:prism', facing='east')
    isl.put(c + 3, y, c, 'astralvigil:mirror', facing='up')
    isl.put(c + 3, y, c + 4, 'astralvigil:mirror', facing='up')
    isl.put(c - 1, y, c + 4, 'astralvigil:trial_receptor')
    for dx in range(-7, 8):
        isl.put(c + dx, FLOOR, c - 5, 'minecraft:gold_block' if dx % 3 == 0 else 'minecraft:smooth_quartz')
    return isl


def ilyra():
    """Library maze of bookshelves with the trial goal in the middle."""
    isl = Island(11, 14, seed=51)
    c = isl.c
    n = 10  # maze cells per side -> 21x21 grid
    rnd = random.Random(52)
    walls = [[True] * (2 * n + 1) for _ in range(2 * n + 1)]
    stack, seen = [(n // 2, n // 2)], {(n // 2, n // 2)}
    walls[2 * (n // 2) + 1][2 * (n // 2) + 1] = False
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
    walls[n][2 * n] = False  # entrance on the +z side
    walls[n][2 * n - 1] = False
    ox = oz = c - n
    for gx in range(2 * n + 1):
        for gz in range(2 * n + 1):
            if walls[gx][gz] and math.hypot(ox + gx - c, oz + gz - c) <= isl.r - 0.5:
                for y in (FLOOR + 1, FLOOR + 2):
                    isl.put(ox + gx, y, oz + gz, 'minecraft:bookshelf')
    # closed outer ring so the maze cannot be bypassed; the only gap faces the bridge (+z)
    for x in range(isl.size[0]):
        for z in range(isl.size[2]):
            d = math.hypot(x - c, z - c)
            if isl.r - 1.5 <= d <= isl.r - 0.5 and not (abs(x - c) <= 1 and z > c):
                for y in (FLOOR + 1, FLOOR + 2):
                    isl.put(x, y, z, 'minecraft:bookshelf')
    isl.put(c, FLOOR + 1, c, 'astralvigil:trial_goal', watcher='ilyra')
    return isl


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (('landing', landing), ('solenne', solenne), ('vael', vael), ('orrin', orrin), ('ilyra', ilyra)):
        isl = fn()
        write(os.path.join(OUT, name + '.nbt'), isl.size, isl.blocks)
