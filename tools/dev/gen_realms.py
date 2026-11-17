"""Generates one template per Firmament realm: arrival shrine, return gate, the trial and the scenery around it.

Run: python -I tools/dev/gen_realms.py
Output: src/main/resources/data/astralvigil/structure/realm/<realm>.nbt

Coordinates in this file are relative to the realm's arrival pad: (0, 0, 0) is the floor block in the middle of the
pad (the Starlight Font sits there), north (-z) leads to the trial. Every template is written so that this anchor lands
at (ANCHOR, FLOOR_Y, ANCHOR) of the template; RealmBuilder places it accordingly (keep both in sync, see Realm.java):

    realm           pad y   trial centre (relative)
    firmament       150     -               (the wild sky of starwood groves; noise islands around the shrine)
    sunspire         80     (0, 0, -31)     Solenne's Sun Spire
    moonwell        100     (0, 0, -43)     Vael's Moon Arena
    starforge       100     (0, 0, -39)     Orrin's Prism Observatory
    archive         100     (0, 0, -54)     Ilyra's Endless Library
    umbral_throne   100     (0, 0, -51)     the Umbral Throne
"""
import gzip
import math
import os
import random
import struct

DATA_VERSION = 3955
ANCHOR = 128
FLOOR_Y = 48
HEIGHT = 256
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources', 'data', 'astralvigil',
                   'structure', 'realm')

AV = 'astralvigil:'
MC = 'minecraft:'


# ------------------------------------------------------------------ NBT

def _s(v):
    b = v.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def _tag(t, n, p):
    return bytes([t]) + _s(n) + p


def _i32(v):
    return struct.pack('>i', v)


def _lst(t, items):
    return bytes([t]) + struct.pack('>i', len(items)) + b''.join(items)


def _comp(e):
    return b''.join(e) + b'\x00'


class World:
    """Sparse block map in pad-relative coordinates."""

    def __init__(self, seed):
        self.blocks = {}
        self.rnd = random.Random(seed)

    def put(self, x, y, z, name, **props):
        if not name.startswith(MC) and ':' not in name:
            name = AV + name
        self.blocks[(int(x), int(y), int(z))] = (name, tuple(sorted((k, str(v).lower()) for k, v in props.items())))

    def soft(self, x, y, z, name, **props):
        """Places only into air (keeps structure the scenery would otherwise overwrite)."""
        if (int(x), int(y), int(z)) not in self.blocks:
            self.put(x, y, z, name, **props)

    def get(self, x, y, z):
        return self.blocks.get((int(x), int(y), int(z)), (None, ()))[0]

    def clear(self, x, y, z):
        self.blocks.pop((int(x), int(y), int(z)), None)

    def column(self, x, z, y0, y1, name, **props):
        for y in range(y0, y1):
            self.put(x, y, z, name, **props)

    def write(self, path):
        palette, index, entries = [], {}, []
        for (x, y, z), (name, props) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            tx, ty, tz = x + ANCHOR, y + FLOOR_Y, z + ANCHOR
            if not (0 <= tx <= 2 * ANCHOR and 0 <= ty < HEIGHT and 0 <= tz <= 2 * ANCHOR):
                continue
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                pe = [_tag(8, 'Name', _s(name))]
                if props:
                    pe.append(_tag(10, 'Properties', _comp([_tag(8, k, _s(v)) for k, v in props])))
                palette.append(_comp(pe))
            entries.append(_comp([_tag(9, 'pos', _lst(3, [_i32(tx), _i32(ty), _i32(tz)])), _tag(3, 'state', _i32(index[key]))]))
        root = _tag(10, '', _comp([
            _tag(3, 'DataVersion', _i32(DATA_VERSION)),
            _tag(9, 'size', _lst(3, [_i32(2 * ANCHOR + 1), _i32(HEIGHT), _i32(2 * ANCHOR + 1)])),
            _tag(9, 'palette', _lst(10, palette)),
            _tag(9, 'blocks', _lst(10, entries)),
            _tag(9, 'entities', _lst(10, [])),
        ]))
        with open(path, 'wb') as f:
            f.write(gzip.compress(root, mtime=0))
        print('wrote', os.path.basename(path), len(entries), 'blocks,', len(palette), 'states')


# ------------------------------------------------------------------ shapes

def island(w, cx, cy, cz, r, top, body, under='minecraft:calcite', depth=None, wobble=0.18, seed=0):
    """A floating island: flat top at cy with a ragged rim, a tapered and dripping underside."""
    rnd = random.Random(seed or (cx * 31 + cz * 17 + cy))
    depth = depth if depth is not None else max(3, int(r * 0.9))
    phase = [rnd.random() * 6.28 for _ in range(3)]
    for x in range(int(cx - r - 3), int(cx + r + 4)):
        for z in range(int(cz - r - 3), int(cz + r + 4)):
            a = math.atan2(z - cz, x - cx)
            rr = r * (1 + wobble * (math.sin(a * 3 + phase[0]) * 0.6 + math.sin(a * 5 + phase[1]) * 0.4))
            d = math.hypot(x - cx, z - cz)
            if d > rr:
                continue
            w.put(x, cy, z, top() if callable(top) else top)
            k = 1 - d / (rr + 0.5)
            under_depth = int(k ** 0.65 * depth) + (rnd.randint(0, 2) if k > 0.3 else 0)
            if rnd.random() < 0.04 and k > 0.2:
                under_depth += rnd.randint(2, 6)  # drips
            for y in range(cy - under_depth, cy):
                w.put(x, y, z, body if y > cy - 3 else under)


def sphere(w, cx, cy, cz, r, block, hollow=False):
    for x in range(int(cx - r), int(cx + r) + 1):
        for y in range(int(cy - r), int(cy + r) + 1):
            for z in range(int(cz - r), int(cz + r) + 1):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                if d <= r and (not hollow or d > r - 1.2):
                    w.put(x, y, z, block() if callable(block) else block)


def ring(w, cx, cy, cz, radius, normal, block, thickness=1.0, gaps=None):
    """A ring of the given radius around (cx, cy, cz) in the plane perpendicular to `normal` (unit-ish vector).
    `block(i, t)` may return None to leave a gap; t in [0, 1) is the position along the ring."""
    nx, ny, nz = normal
    n = math.sqrt(nx * nx + ny * ny + nz * nz)
    nx, ny, nz = nx / n, ny / n, nz / n
    # two vectors spanning the plane
    ax, ay, az = (0, 1, 0) if abs(ny) < 0.9 else (1, 0, 0)
    ux, uy, uz = ay * nz - az * ny, az * nx - ax * nz, ax * ny - ay * nx
    un = math.sqrt(ux * ux + uy * uy + uz * uz)
    ux, uy, uz = ux / un, uy / un, uz / un
    vx, vy, vz = ny * uz - nz * uy, nz * ux - nx * uz, nx * uy - ny * ux
    steps = int(2 * math.pi * radius * 3)
    for i in range(steps):
        t = i / steps
        a = t * 2 * math.pi
        for dr in [d * 0.5 for d in range(int(-thickness * 2), int(thickness * 2) + 1)]:
            for dn in [d * 0.5 for d in range(int(-thickness), int(thickness) + 1)]:
                r = radius + dr
                x = cx + (ux * math.cos(a) + vx * math.sin(a)) * r + nx * dn
                y = cy + (uy * math.cos(a) + vy * math.sin(a)) * r + ny * dn
                z = cz + (uz * math.cos(a) + vz * math.sin(a)) * r + nz * dn
                b = block(i, t)
                if b:
                    w.put(round(x), round(y), round(z), b)


def starwood_tree(w, x, y, z, rnd, height=None, stripped=False, lanterns=True):
    """A starwood tree: a leaning trunk, two or three branches, round glowing crowns with hanging star lanterns."""
    log = 'stripped_starwood_log' if stripped else 'starwood_log'
    h = height or rnd.randint(6, 9)
    lean = (rnd.choice((-1, 0, 1)), rnd.choice((-1, 0, 1)))
    tx, tz = x, z
    for i in range(h):
        if i > h // 2 and rnd.random() < 0.35:
            tx += lean[0]
            tz += lean[1]
        w.put(tx, y + i, tz, log, axis='y')
    crowns = [(tx, y + h, tz, rnd.uniform(2.6, 3.4))]
    for _ in range(rnd.randint(2, 3)):
        dx, dz = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        bx, by, bz = tx, y + h - rnd.randint(2, 3), tz
        length = rnd.randint(2, 4)
        for j in range(1, length + 1):
            bx, bz = bx + dx, bz + dz
            if j == length // 2 + 1:
                by += 1
            w.put(bx, by, bz, log, axis='x' if dx else 'z')
        crowns.append((bx, by + 1, bz, rnd.uniform(1.8, 2.6)))
    for (cx, cy, cz, r) in crowns:
        for lx in range(int(cx - r - 1), int(cx + r + 2)):
            for ly in range(int(cy - r * 0.7 - 1), int(cy + r * 0.8 + 1)):
                for lz in range(int(cz - r - 1), int(cz + r + 2)):
                    d = math.sqrt((lx - cx) ** 2 + ((ly - cy) * 1.35) ** 2 + (lz - cz) ** 2)
                    if d <= r and rnd.random() > 0.06:
                        w.soft(lx, ly, lz, 'starwood_leaves', persistent='true', distance='1', waterlogged='false')
        if lanterns and rnd.random() < 0.8:
            lx, lz = int(cx + rnd.choice((-1, 1)) * int(r)), int(cz + rnd.choice((-1, 0, 1)))
            ly = int(cy - r * 0.7)
            if w.get(lx, ly, lz) == AV + 'starwood_leaves':
                w.put(lx, ly - 1, lz, 'star_lantern', hanging='true', waterlogged='false')


def gate(w, kind, cx, y, cz, axis='x'):
    """A lit gate (frame of `<kind>_gate`, portal of the kind's portal block), 2x3 opening, bottom row at y."""
    frame = kind + '_gate'
    portal = 'celestial_portal' if kind == 'astral' else kind + '_portal'
    for i in range(-2, 2):
        for dy in range(0, 5):
            x, z = (cx + i, cz) if axis == 'x' else (cx, cz + i)
            if i in (-2, 1) or dy in (0, 4):
                w.put(x, y + dy, z, frame)
            else:
                w.put(x, y + dy, z, portal, axis=axis)


def lantern_post(w, x, y, z, post, top='star_lantern'):
    w.column(x, z, y, y + 3, post, **({'axis': 'y'} if 'pillar' in post or 'log' in post else {}))
    w.put(x, y + 3, z, top, **({'hanging': 'false', 'waterlogged': 'false'} if 'lantern' in top else {}))


def pad(w, kind, floor, trim, inlay, post, radius=7):
    """The arrival shrine: a round platform, the Starlight Font in the middle (realm marker), the return gate on the
    south edge, four lantern posts. Arrivals stand at (0, 1, 2) facing north."""
    island(w, 0, 0, 0, radius, floor, floor, depth=radius + 2, wobble=0.0, seed=7)
    for i in range(64):
        a = i * 2 * math.pi / 64
        w.put(round(math.cos(a) * radius), 0, round(math.sin(a) * radius), trim)
    for i in range(32):
        a = i * 2 * math.pi / 32
        w.put(round(math.cos(a) * 3), 0, round(math.sin(a) * 3), inlay)
    w.put(0, 0, 0, 'starlight_font', facing='up')
    gate(w, kind, 0, 1, 5, 'x')
    for dx, dz in ((5, -3), (-5, -3), (4, 3), (-4, 3)):
        lantern_post(w, dx, 1, dz, post)


def bridge(w, z0, z1, deck, edge, lamp, x=0):
    """A 3-wide walkway along -z from z0 to z1 (z0 > z1), with low edges and lamps every 5 blocks."""
    for z in range(z1, z0 + 1):
        for dx in (-1, 0, 1):
            w.soft(x + dx, 0, z, deck)
            w.soft(x + dx, -1, z, edge)
        if (z0 - z) % 5 == 2:
            w.soft(x - 2, 0, z, edge)
            w.soft(x + 2, 0, z, edge)
            w.soft(x - 2, 1, z, lamp, hanging='false', waterlogged='false')
            w.soft(x + 2, 1, z, lamp, hanging='false', waterlogged='false')


def clouds(w, y, extent, rnd, block='minecraft:white_wool', count=40, skip=None):
    """A soft cloud sea below the islands: flat, rounded blobs."""
    for _ in range(count):
        cx, cz = rnd.randint(-extent, extent), rnd.randint(-extent, extent)
        if skip and skip(cx, cz):
            continue
        rx, rz = rnd.randint(4, 11), rnd.randint(3, 8)
        for x in range(cx - rx, cx + rx + 1):
            for z in range(cz - rz, cz + rz + 1):
                e = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
                if e <= 1:
                    w.soft(x, y, z, block)
                    if e < 0.45:
                        w.soft(x, y + 1, z, block)


def debris(w, rnd, count, extent, ymin, ymax, block, accent=None, skip=None, min_dist=30):
    for _ in range(count):
        x, z = rnd.randint(-extent, extent), rnd.randint(-extent, extent)
        if math.hypot(x, z) < min_dist or (skip and skip(x, z)):
            continue
        y = rnd.randint(ymin, ymax)
        r = rnd.uniform(1.2, 3.2)
        sphere(w, x, y, z, r, lambda: accent if accent and rnd.random() < 0.15 else block)


def spire_crystal(w, x, y, z, h, rnd, core, shell):
    """A tall, slightly leaning crystal: a shell of glass around a glowing core."""
    lx, lz = rnd.uniform(-0.25, 0.25), rnd.uniform(-0.25, 0.25)
    for i in range(h):
        r = max(0.5, 1.6 * (1 - i / h))
        cx, cz = x + lx * i, z + lz * i
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                d = math.hypot(dx, dz)
                if d <= r:
                    w.put(round(cx + dx), y + i, round(cz + dz), core if d < r - 0.8 else shell)


# ------------------------------------------------------------------ trials (re-skinned from the old Firmament)

def trial_island(w, cz, r, top, body, rim, seed):
    island(w, 0, 0, cz, r, top, body, depth=int(r * 0.7), wobble=0.05, seed=seed)
    n = max(32, int(2 * math.pi * r))
    for i in range(n):
        a = i * 2 * math.pi / n
        w.put(round(math.cos(a) * r), 0, cz + round(math.sin(a) * r), rim(i))


SPIRE_STEPS = 64


def sun_spire(w, cz):
    """Solenne: a 100-block spire; 64 single pads spiral around it. Gold pads are checkpoints."""
    trial_island(w, cz, 14, 'sunstone_bricks', 'sunstone', lambda i: 'minecraft:gold_block' if i % 8 == 0 else 'gilded_celestial_bricks', 21)
    top = 3 + int(SPIRE_STEPS * 1.5)
    for x in range(-2, 3):
        for z in range(-2, 3):
            if abs(x) + abs(z) <= 3:
                for y in range(1, top):
                    band = y % 12 == 0
                    lamp = y % 12 == 6 and abs(x) + abs(z) == 3
                    w.put(x, y, cz + z, 'minecraft:gold_block' if band else 'sunstone_lamp' if lamp else 'celestial_pillar',
                          **({} if band or lamp else {'axis': 'y'}))
    for x in range(-3, 4):
        for z in range(-3, 4):
            if abs(x) + abs(z) <= 4:
                w.put(x, top, cz + z, 'minecraft:gold_block' if abs(x) + abs(z) < 4 else 'sunstone_lamp')
    w.put(0, top + 1, cz, 'trial_goal', watcher='solenne')
    # a sun crown above the goal
    ring(w, 0, top + 7, cz, 5, (0, 0, 1), lambda i, t: 'sunstone_lamp' if i % 6 == 0 else 'minecraft:gold_block', 0.5)
    y = 1
    for i in range(SPIRE_STEPS):
        a = i * 0.47
        rr = 6.5 + 0.5 * math.sin(i * 0.9)
        x, z = round(math.cos(a) * rr), round(math.sin(a) * rr)
        y += 2 if i % 2 == 0 else 1
        if i % 16 == 15:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    w.put(x + dx, y, cz + z + dz, 'sunstone_bricks')
            w.put(x, y, cz + z, 'minecraft:gold_block')
        else:
            w.put(x, y, cz + z, 'minecraft:magma_block' if i % 7 == 3 else 'sunstone_brick_slab', type='top', waterlogged='false')
    return top


def moon_arena(w, cz):
    """Vael: arena of pillars with six moon targets; four more on floating islets far outside."""
    rnd = random.Random(32)
    trial_island(w, cz, 26, lambda: 'moonstone' if rnd.random() < 0.7 else 'moonstone_bricks', 'moonstone',
                 lambda i: 'moonstone_lamp' if i % 10 == 0 else 'moonstone_bricks', 31)
    for i in range(10):  # cover pillars
        a = i * 2 * math.pi / 10 + 0.31
        x, z = round(math.cos(a) * 15), round(math.sin(a) * 15)
        if abs(x) <= 2 and z > 0:
            continue
        h = 4 + rnd.randint(0, 3)
        w.column(x, cz + z, 1, 1 + h, 'celestial_pillar', axis='y')
        w.put(x, 1 + h, cz + z, 'moonstone_brick_slab', type='bottom', waterlogged='false')
    for i in range(6):
        a = i * math.pi / 3 + 0.5
        x, z = round(math.cos(a) * 21), round(math.sin(a) * 21)
        h = 4 + (i * 5) % 13
        w.column(x, cz + z, 1, h, 'moonstone_brick_wall', up='true', north='none', south='none', east='none', west='none',
                 waterlogged='false')
        w.put(x, h, cz + z, 'moon_target', lit='false')
    for i in range(4):  # far islets
        a = i * math.pi / 2 + math.pi / 4
        x, z = round(math.cos(a) * 35), round(math.sin(a) * 35)
        y = 10 + i * 4
        island(w, x, y, cz + z, 2.5, 'moonstone_bricks', 'moonstone', depth=4, wobble=0.0, seed=40 + i)
        w.put(x, y + 1, cz + z, 'moon_target', lit='false')
    # a still moonwell in the middle: water ringed in moonstone
    for x in range(-4, 5):
        for z in range(-4, 5):
            d = math.hypot(x, z)
            if d <= 3.2:
                w.put(x, 0, cz + z, 'minecraft:water', level='0')
                w.put(x, -1, cz + z, 'moonstone_lamp')
            elif d <= 4.3:
                w.put(x, 0, cz + z, 'moonstone_bricks')


def prism_observatory(w, cz):
    """Orrin: three bays, each font -> prism -> mirrors (turned wrong) -> receptor. All three must glow together."""
    trial_island(w, cz, 22, 'constellation_tiles', 'celestial_marble', lambda i: 'astral_lamp' if i % 9 == 0 else 'celestial_bricks', 41)
    y = 1
    wall = 'celestial_bricks'

    def wall_line(x0, x1, z, h=5):
        for x in range(x0, x1 + 1):
            for yy in range(y, y + h):
                w.put(x, yy, cz + z, wall if yy < y + h - 1 else 'gilded_celestial_bricks')

    for bz in (-19, -6, 6, 19):
        wall_line(-19, 14, bz)
    for z in range(-19, 20):
        for yy in range(y, y + 5):
            w.put(-19, yy, cz + z, wall)
    for bz in (-6, 6):
        for x in range(-10, 10, 4):
            w.put(x, y + 2, cz + bz, 'halo_glass')

    def bay(z, mirrors, receptor, obstacle):
        w.put(-17, y, cz + z, 'starlight_font', facing='east')
        w.put(-13, y, cz + z, 'prism', facing='east')
        for (mx, my, mz, facing) in mirrors:
            w.put(mx, y + my, cz + mz, 'mirror', facing=facing)
        rx, ry, rz = receptor
        if ry > 0:
            w.column(rx, cz + rz, y, y + ry, 'celestial_pillar', axis='y')
        w.put(rx, y + ry, cz + rz, 'trial_receptor')
        for (ox, oy, oz) in obstacle:
            w.put(ox, y + oy, cz + oz, wall)

    bay(-12, [(-5, 0, -12, 'up'), (-5, 0, -17, 'down')], (8, 0, -17), [(0, 0, -12), (0, 1, -12), (1, 0, -12)])
    bay(0, [(-6, 0, 0, 'east'), (-6, 5, 0, 'west')], (6, 5, 0), [(0, yy, 0) for yy in range(0, 4)])
    bay(12, [(-7, 0, 12, 'up'), (-7, 0, 17, 'west'), (3, 0, 17, 'down')], (3, 0, 12), [(-2, 0, 12), (-1, 0, 12), (0, 0, 12)])


def endless_library(w, cz):
    """Ilyra: a roofed, dim bookshelf maze (2-block corridors), the goal in the reading room at its heart."""
    n = 17
    rnd = random.Random(52)
    g = 2 * n + 1
    walls = [[True] * g for _ in range(g)]
    mid = n // 2
    stack, seen = [(mid, mid)], {(mid, mid)}
    walls[2 * mid + 1][2 * mid + 1] = False
    while stack:
        cx, ccz = stack[-1]
        nbrs = [(cx + dx, ccz + dz, dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                if 0 <= cx + dx < n and 0 <= ccz + dz < n and (cx + dx, ccz + dz) not in seen]
        if not nbrs:
            stack.pop()
            continue
        nx, nz, dx, dz = rnd.choice(nbrs)
        walls[2 * cx + 1 + dx][2 * ccz + 1 + dz] = False
        walls[2 * nx + 1][2 * nz + 1] = False
        seen.add((nx, nz))
        stack.append((nx, nz))
    walls[2 * mid + 1][g - 1] = False          # entrance on the +z side (towards the arrival pad)
    for gx in range(g):
        for gz in range(g):
            if abs(gx - (2 * mid + 1)) <= 1 and abs(gz - (2 * mid + 1)) <= 1:
                walls[gx][gz] = False
    o = -g                                       # 2 blocks per grid line, centred on x = 0 / z = cz
    roof_y = 5
    island(w, 0, 0, cz, 37, 'constellation_tiles', 'celestial_marble', depth=14, wobble=0.04, seed=51)
    for x in range(o - 1, o + 2 * g + 1):
        for z in range(o - 1, o + 2 * g + 1):
            w.put(x, 0, cz + z, 'starwood_planks' if (x + z) % 7 else 'constellation_tiles')
            for yy in range(-3, 0):
                w.put(x, yy, cz + z, 'celestial_marble')
    for gx in range(g):
        for gz in range(g):
            for bx in range(2):
                for bz in range(2):
                    x, z = o + 2 * gx + bx, o + 2 * gz + bz
                    if walls[gx][gz]:
                        for yy in range(1, roof_y):
                            w.put(x, yy, cz + z, 'minecraft:bookshelf' if yy != 1 or (x + z) % 5 else 'minecraft:chiseled_bookshelf',
                                  **({} if yy != 1 or (x + z) % 5 else {'facing': 'south', 'slot_0_occupied': 'true', 'slot_1_occupied': 'false',
                                                                         'slot_2_occupied': 'true', 'slot_3_occupied': 'true',
                                                                         'slot_4_occupied': 'false', 'slot_5_occupied': 'true'}))
                    elif (gx * 7 + gz * 3) % 23 == 0 and bx == 0 and bz == 0:
                        w.put(x, roof_y - 1, cz + z, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
                    w.put(x, roof_y, cz + z, 'gilded_starwood' if gx % 6 == 0 or gz % 6 == 0 else 'starwood_planks')
    ec = 2 * mid + 1
    gx0, gz0 = o + 2 * ec, cz + o + 2 * ec
    w.put(gx0, 1, gz0, 'trial_goal', watcher='ilyra')
    for dx, dz in ((-2, -2), (3, -2), (-2, 3), (3, 3)):
        w.put(gx0 + dx, 1, gz0 + dz, 'minecraft:lectern', facing='north', has_book='false', powered='false')
    # a dome of halo glass and marble ribs over the reading room, crowned with a lamp
    for dx in range(-7, 9):
        for dz in range(-7, 9):
            for dy in range(0, 9):
                d = math.sqrt((dx - 0.5) ** 2 + (dz - 0.5) ** 2 + (dy * 1.1) ** 2)
                if 6.6 < d <= 7.6:
                    rib = abs(dx - 0.5) < 1 or abs(dz - 0.5) < 1 or dy == 0
                    w.put(gx0 + dx, roof_y + dy, gz0 + dz, 'celestial_bricks' if rib else 'halo_glass')
                elif d <= 6.6 and dy == 0:
                    w.put(gx0 + dx, roof_y, gz0 + dz, 'nebula_glass')
    w.put(gx0, roof_y + 8, gz0, 'astral_lamp')
    w.put(gx0, roof_y + 9, gz0, 'star_lantern', hanging='false', waterlogged='false')
    # four corner towers with lamps
    for sx in (o - 1, o + 2 * g):
        for sz in (o - 1, o + 2 * g):
            for dy in range(1, 13):
                for ddx in (-1, 0, 1):
                    for ddz in (-1, 0, 1):
                        w.put(sx + ddx, dy, cz + sz + ddz, 'celestial_pillar' if (ddx, ddz) == (0, 0) else 'celestial_bricks',
                              **({'axis': 'y'} if (ddx, ddz) == (0, 0) else {}))
            w.put(sx, 13, cz + sz, 'astral_lamp')
            w.put(sx, 14, cz + sz, 'star_lantern', hanging='false', waterlogged='false')
    # entrance arch
    ex, ez = o + 2 * (2 * mid + 1), cz + o + 2 * (g - 1) + 2
    for dy in range(1, 6):
        w.put(ex - 1, dy, ez, 'celestial_pillar', axis='y')
        w.put(ex + 2, dy, ez, 'celestial_pillar', axis='y')
    for dx in range(-1, 3):
        w.put(ex + dx, 6, ez, 'chiseled_celestial_marble')


# ------------------------------------------------------------------ realms

def realm_firmament():
    """The Firmament wilds: a starwood shrine floating among noise islands (the dimension's own terrain)."""
    w = World(100)
    rnd = w.rnd
    island(w, 0, 0, 0, 16, lambda: 'minecraft:grass_block' if rnd.random() < 0.8 else 'minecraft:moss_block', 'minecraft:dirt',
           'celestial_marble', depth=12, wobble=0.2, seed=101)
    pad(w, 'astral', 'celestial_marble', 'gilded_celestial_bricks', 'constellation_tiles', 'celestial_pillar')
    for x, z in ((10, -6), (-11, -4), (8, 9), (-9, 10), (2, -13)):
        starwood_tree(w, x, 1, z, rnd)
    # a broken ring of pillars around the shrine (an old observatory)
    for i in range(12):
        a = i * math.pi / 6
        x, z = round(math.cos(a) * 13), round(math.sin(a) * 13)
        if w.get(x, 1, z) is None:
            h = rnd.randint(2, 6)
            w.column(x, z, 1, 1 + h, 'celestial_pillar', axis='y')
            if h > 4:
                w.put(x, 1 + h, z, 'celestial_marble_slab', type='bottom', waterlogged='false')
    # a great halo hanging over the shrine
    ring(w, 0, 30, 0, 18, (0.25, 1, 0.1), lambda i, t: 'astral_lamp' if i % 14 == 0 else 'minecraft:gold_block' if i % 2 else 'gilded_celestial_bricks', 0.5)
    for _ in range(9):  # satellite islets with trees
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(30, 60)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d), rnd.randint(-14, 16)
        r = rnd.uniform(4, 7)
        island(w, x, y, z, r, 'minecraft:grass_block', 'minecraft:dirt', 'celestial_marble', seed=rnd.randint(1, 999))
        starwood_tree(w, x, y + 1, z, rnd)
    return w


def realm_sunspire():
    w = World(200)
    rnd = w.rnd
    cz = -31
    pad(w, 'solar', 'sunstone_bricks', 'minecraft:gold_block', 'sunstone_lamp', 'celestial_pillar')
    bridge(w, -8, cz + 15, 'sunstone_bricks', 'sunstone', 'star_lantern')
    top = sun_spire(w, cz)
    # the risen sun behind the spire: a vast ring with rays
    sx, sy, sz = 0, 70, cz - 60
    ring(w, sx, sy, sz, 38, (0, 0, 1), lambda i, t: 'sunstone_lamp' if i % 5 == 0 else 'minecraft:gold_block', 1.0)
    ring(w, sx, sy, sz, 31, (0, 0, 1), lambda i, t: ('sunstone_lamp' if i % 3 == 0 else 'minecraft:gold_block') if (i // 18) % 2 == 0 else None, 0.5)
    for k in range(16):
        a = k * math.pi / 8
        length = 14 if k % 2 == 0 else 8
        for j in range(length):
            r = 41 + j
            x, y = round(sx + math.cos(a) * r), round(sy + math.sin(a) * r)
            w.put(x, y, sz, 'sunstone_lamp' if j % 4 == 0 else 'gilded_celestial_bricks')
    # golden islands drifting at sunrise, with sunstone obelisks
    for k in range(10):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(34, 70)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz // 2, rnd.randint(-10, 40)
        if abs(x) < 18 and -50 < z < 8:
            continue
        r = rnd.uniform(3, 7)
        island(w, x, y, z, r, 'sunstone', 'sunstone', 'minecraft:smooth_sandstone', seed=k + 210)
        h = rnd.randint(4, 9)
        w.column(x, z, y + 1, y + 1 + h, 'sunstone_bricks')
        w.put(x, y + 1 + h, z, 'sunstone_lamp')
    # arches on the arrival island
    for dx in (-5, 5):
        for dy in range(1, 6):
            w.put(dx, dy, -6, 'celestial_pillar', axis='y')
    for dx in range(-5, 6):
        w.put(dx, 6, -6, 'gilded_celestial_bricks' if dx % 2 else 'sunstone_lamp')
    clouds(w, -24, 110, rnd, count=70)
    debris(w, rnd, 26, 100, -16, 60, 'sunstone', 'minecraft:gold_block', min_dist=40)
    return w


def realm_moonwell():
    w = World(300)
    rnd = w.rnd
    cz = -43
    pad(w, 'lunar', 'moonstone_bricks', 'moonstone_lamp', 'moonstone', 'celestial_pillar')
    bridge(w, -8, cz + 27, 'moonstone_bricks', 'moonstone', 'star_lantern')
    moon_arena(w, cz)
    # a great crescent moon sculpture beyond the arena
    mx, my, mz = 0, 55, cz - 75
    for x in range(-32, 33):
        for y in range(-32, 33):
            d = math.hypot(x, y)
            d2 = math.hypot(x - 13, y + 6)
            if d <= 30 and d2 > 25:
                edge = d > 28.5 or d2 < 26.5
                w.put(mx + x, my + y, mz, 'moonstone_lamp' if edge and (x + y) % 3 == 0 else 'moonstone' if not edge else 'moonstone_bricks')
                if not edge:
                    w.put(mx + x, my + y, mz - 1, 'moonstone')
    # silver groves on drifting isles, with still pools
    for k in range(9):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(48, 80)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(-12, 24)
        r = rnd.uniform(5, 8)
        island(w, x, y, z, r, 'moonstone', 'moonstone', 'minecraft:calcite', seed=k + 310)
        starwood_tree(w, x + 1, y + 1, z + 1, rnd, stripped=True)
        for px in range(-2, 0):
            for pz in range(-2, 0):
                w.put(x + px - 1, y, z + pz - 1, 'minecraft:water', level='0')
    clouds(w, -30, 120, rnd, block='minecraft:white_stained_glass', count=50)
    debris(w, rnd, 30, 110, -20, 50, 'moonstone', 'moonstone_lamp', min_dist=55)
    return w


def realm_starforge():
    w = World(400)
    rnd = w.rnd
    cz = -39
    pad(w, 'prism', 'celestial_bricks', 'astral_lamp', 'constellation_tiles', 'celestial_pillar')
    bridge(w, -8, cz + 23, 'celestial_bricks', 'celestial_marble', 'star_lantern')
    prism_observatory(w, cz)
    # the orrery: a glowing core with tilted golden orbits and planets
    ox, oy, oz = 0, 40, cz
    sphere(w, ox, oy, oz, 4.5, lambda: 'astral_lamp' if rnd.random() < 0.6 else 'minecraft:gold_block')
    for radius, normal, planet in ((12, (0.2, 1, 0), 'sunstone_lamp'), (19, (0.6, 1, 0.3), 'moonstone_lamp'),
                                   (27, (-0.4, 1, 0.5), 'umbral_lamp')):
        ring(w, ox, oy, oz, radius, normal, lambda i, t: 'minecraft:gold_block' if i % 4 else 'halo_glass', 0.0)
        # planet sitting on its orbit
        nx, ny, nz = normal
        a = rnd.uniform(0, 6.28)
        px, pz = ox + math.cos(a) * radius, oz + math.sin(a) * radius
        py = oy - (nx * (px - ox) + nz * (pz - oz)) / ny
        sphere(w, px, py, pz, 2.2, planet)
    # crystal spires on shards of the old forge
    for k in range(12):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(36, 85)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(-20, 30)
        r = rnd.uniform(3, 6)
        island(w, x, y, z, r, 'celestial_marble', 'celestial_marble', 'umbral_stone', seed=k + 410)
        for _ in range(rnd.randint(1, 3)):
            spire_crystal(w, x + rnd.randint(-2, 2), y + 1, z + rnd.randint(-2, 2), rnd.randint(6, 14), rnd,
                          'astral_lamp', rnd.choice(('nebula_glass', 'minecraft:amethyst_block', 'halo_glass')))
    debris(w, rnd, 40, 120, -30, 70, 'minecraft:amethyst_block', 'nebula_glass', min_dist=45)
    return w


def realm_archive():
    w = World(500)
    rnd = w.rnd
    cz = -54
    pad(w, 'archive', 'starwood_planks', 'gilded_starwood', 'constellation_tiles', 'starwood_log')
    bridge(w, -8, cz + 39, 'starwood_planks', 'starwood_log', 'minecraft:soul_lantern')
    endless_library(w, cz)
    # floating bookshelf towers with spiral starwood stairs and lamps on top
    for k in range(10):
        a = k * 2 * math.pi / 10 + rnd.uniform(-0.2, 0.2)
        d = rnd.uniform(52, 78)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(-25, 5)
        h = rnd.randint(14, 28)
        r = 3.2
        for yy in range(h):
            for dx in range(-4, 5):
                for dz in range(-4, 5):
                    dd = math.hypot(dx, dz)
                    if dd <= r:
                        w.put(x + dx, y + yy, z + dz, 'minecraft:bookshelf' if yy % 6 else 'gilded_starwood')
        for yy in range(h):  # spiral stair around the tower
            ang = yy * 0.55
            sx, sz = round(x + math.cos(ang) * 4.6), round(z + math.sin(ang) * 4.6)
            w.soft(sx, y + yy, sz, 'starwood_slab', type='top', waterlogged='false')
        island(w, x, y, z, 5, 'starwood_planks', 'celestial_marble', 'celestial_marble', depth=5, seed=k + 510)
        w.put(x, y + h, z, 'astral_lamp')
        w.put(x, y + h + 1, z, 'star_lantern', hanging='false', waterlogged='false')
    # drifting pages and open books
    for _ in range(70):
        x, z, y = rnd.randint(-100, 100), rnd.randint(-110, 60), rnd.randint(-20, 40)
        if math.hypot(x, z - cz) < 45 or math.hypot(x, z) < 14:
            continue
        w.soft(x, y, z, 'minecraft:birch_slab', type='bottom', waterlogged='false')
        if rnd.random() < 0.3:
            w.soft(x + 1, y + 1, z, 'minecraft:birch_slab', type='bottom', waterlogged='false')
    for k in range(4):
        bx, by, bz = rnd.choice((-1, 1)) * rnd.randint(30, 60), rnd.randint(10, 30), cz + rnd.randint(-60, 30)
        for i in range(-6, 7):
            for j in range(-4, 5):
                tilt = abs(i) // 3
                w.soft(bx + i, by + tilt, bz + j, 'minecraft:white_wool' if i != 0 else 'starwood_log', **({'axis': 'z'} if i == 0 else {}))
                w.soft(bx + i, by + tilt - 1, bz + j, 'starwood_planks' if abs(i) == 6 or abs(j) == 4 else 'minecraft:white_wool')
    debris(w, rnd, 18, 110, -30, 30, 'celestial_marble', 'minecraft:bookshelf', min_dist=50)
    return w


def cathedral(w, cz):
    """The Umbral Cathedral: a vast broken church of the fallen fifth Watcher, open to the sky. The nave is the arena;
    its pillar rows are the cover against the Umbral Nova; the throne stands in the apse. Nave centre = (0, 0, cz)."""
    rnd = random.Random(66)
    R = 38
    island(w, 0, 0, cz, R, lambda: 'umbral_stone' if rnd.random() < 0.8 else 'cracked_umbral_bricks', 'umbral_stone',
           'minecraft:obsidian', depth=22, wobble=0.06, seed=661)
    n0, n1 = cz - 22, cz + 28          # nave from the apse step (north) to the doors (south)
    WALL_X = 18
    # ---- floor: dark flagstones, a central aisle of constellation tiles, rings of crying obsidian under the dome
    for x in range(-WALL_X, WALL_X + 1):
        for z in range(n0 - 14, n1 + 1):
            if math.hypot(x, z - cz) > R:
                continue
            b = 'umbral_bricks' if (x // 3 + z // 3) % 2 == 0 else 'umbral_stone'
            if abs(x) <= 2:
                b = 'constellation_tiles' if abs(x) < 2 else 'chiseled_umbral_bricks'
            if int(math.hypot(x, z - cz)) in (9, 16) and abs(x) > 2:
                b = 'minecraft:crying_obsidian'
            w.put(x, 0, z, b)

    # ---- outer walls with lancet windows and buttresses, jagged and broken
    def wall_height(z, side):
        base = 19 + int(3 * math.sin(z * 0.37 + side))
        if side > 0 and n0 + 18 < z < n0 + 34:   # the east wall has collapsed in the middle
            base = 3 + int(4 * abs(math.sin(z * 0.9)))
        return base

    for side in (-1, 1):
        x = side * WALL_X
        for z in range(n0, n1 + 1):
            h = wall_height(z, side)
            bay = (z - n0) % 8
            for y in range(1, h):
                window = 2 <= bay <= 5 and 4 <= y <= 15 - (0 if 3 <= bay <= 4 else 2)
                if window:
                    if rnd.random() < 0.75:
                        w.put(x, y, z, 'eclipse_glass' if (y + bay) % 5 else 'nebula_glass')
                    continue
                w.put(x, y, z, 'cracked_umbral_bricks' if rnd.random() < 0.18 else 'umbral_bricks')
            if bay == 0:   # buttress
                for dx in (1, 2, 3):
                    top = max(2, h - 2 - dx * 3)
                    for y in range(1, top):
                        w.put(x + side * dx, y, z, 'umbral_bricks')
                    w.put(x + side * dx, top, z, 'umbral_brick_slab', type='bottom', waterlogged='false')
                w.put(x + side * 3, max(2, h - 11) + 1, z, 'umbral_lamp')
            elif bay in (3, 4) and h > 16:   # candles on the window sills
                w.put(x, 4, z, 'minecraft:purple_candle', candles='3', lit='true', waterlogged='false')

    # ---- the apse: a half-ring of tall walls with slit windows behind the throne
    ax, az = 0, n0
    for i in range(90):
        a = math.pi + i * math.pi / 89
        for rr in (14, 15):
            x, z = round(ax + math.cos(a) * rr), round(az + math.sin(a) * rr)
            h = 24 + int(2 * math.sin(i * 0.4))
            for y in range(1, h):
                slit = i % 15 in (6, 7, 8) and 5 <= y <= 18 and rr == 14
                w.put(x, y, z, 'eclipse_glass' if slit else ('cracked_umbral_bricks' if rnd.random() < 0.12 else 'umbral_bricks'))
            if i % 15 == 0 and rr == 15:
                w.put(x, h, z, 'umbral_lamp')
    for x in range(-14, 15):   # apse floor
        for z in range(az - 15, az + 1):
            if math.hypot(x, z - az) <= 14:
                w.put(x, 0, z, 'umbral_bricks' if (x + z) % 4 else 'constellation_tiles')

    # ---- the throne dais (three steps), the throne, the seal
    tz = az - 6
    for step, half in ((1, 8), (2, 6), (3, 4)):
        for x in range(-half, half + 1):
            for z in range(tz - half + 2, tz + half - 1):
                w.put(x, step, z, 'umbral_bricks')
        for x in range(-half, half + 1):
            w.put(x, step, tz + half - 1, 'umbral_brick_stairs', facing='north', half='bottom', shape='straight', waterlogged='false')
    w.put(0, 4, tz - 1, 'chiseled_umbral_bricks')
    for y in range(4, 12):
        w.put(0, y, tz - 2, 'umbral_pillar', axis='y')
        w.put(-1, y, tz - 2, 'umbral_bricks' if y < 10 else 'umbral_lamp')
        w.put(1, y, tz - 2, 'umbral_bricks' if y < 10 else 'umbral_lamp')
    ring(w, 0, 14, tz - 2, 4, (0, 0, 1), lambda i, t: None if 0.42 < t < 0.58 else ('umbral_lamp' if i % 5 == 0 else 'minecraft:obsidian'), 0.0)
    w.put(-1, 4, tz - 1, 'umbral_brick_stairs', facing='east', half='bottom', shape='straight', waterlogged='false')
    w.put(1, 4, tz - 1, 'umbral_brick_stairs', facing='west', half='bottom', shape='straight', waterlogged='false')
    w.put(0, 4, tz + 2, 'eclipse_seal')
    for dx in (-6, 6):   # candelabra beside the dais
        w.column(dx, tz + 4, 1, 5, 'umbral_brick_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
        w.put(dx, 5, tz + 4, 'minecraft:purple_candle', candles='4', lit='true', waterlogged='false')
        w.put(dx, 1, tz + 5, 'minecraft:soul_campfire', facing='south', lit='true', signal_fire='false', waterlogged='false')

    # ---- the nave arcade: clustered pillars (cover), capitals with lamps; some broken and fallen
    pillar_z = [n1 - 5, n1 - 13, n1 - 21, n1 - 29, n1 - 37, n1 - 45]
    broken = {(1, 2), (-1, 4), (1, 5)}
    for side in (-1, 1):
        px = side * 10
        for k, pz in enumerate(pillar_z):
            h = 16 if (side, k) not in broken else rnd.randint(5, 9)
            for y in range(1, h + 1):
                for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                    w.put(px + dx, y, pz + dz, 'umbral_pillar' if (dx, dz) == (0, 0) or y < h - 1 else 'umbral_bricks', **({'axis': 'y'} if (dx, dz) == (0, 0) or y < h - 1 else {}))
            if h == 16:
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        w.put(px + dx, 17, pz + dz, 'chiseled_umbral_bricks' if abs(dx) + abs(dz) == 1 else 'umbral_bricks')
                w.put(px - side, 15, pz, 'umbral_lamp')
                w.put(px, 18, pz, 'umbral_brick_slab', type='bottom', waterlogged='false')
            else:   # the fallen drum lies across the floor
                d = rnd.choice(((1, 0), (0, 1)))
                for j in range(2, 9):
                    fx, fz = px - side * j * d[0], pz + j * d[1]
                    w.put(fx, 1, fz, 'umbral_pillar', axis='x' if d[0] else 'z')
                for _ in range(6):
                    w.soft(px + rnd.randint(-3, 3), 1, pz + rnd.randint(-3, 3), rnd.choice(('cracked_umbral_bricks', 'umbral_brick_slab')),
                           **({'type': 'bottom', 'waterlogged': 'false'} if rnd.random() < 0.0 else {}))
            # candles at the pillar feet
            w.soft(px - side * 2, 1, pz + 1, 'minecraft:purple_candle', candles=str(rnd.randint(1, 4)), lit='true', waterlogged='false')

    # ---- vault ribs: pointed arches across the nave and along the arcade, many of them broken
    def arch(p0, p1, y0, rise, block, keep):
        (x0, z0), (x1, z1) = p0, p1
        steps = int(max(abs(x1 - x0), abs(z1 - z0)) * 3)
        for i in range(steps + 1):
            t = i / steps
            if not keep(t):
                continue
            u = 2 * t - 1
            y = y0 + rise * (1 - abs(u) ** 1.35)
            x, z = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
            w.put(round(x), round(y), round(z), block)
            w.put(round(x), round(y) + 1, round(z), block)

    for k, pz in enumerate(pillar_z):
        if (1, k) in broken or (-1, k) in broken:
            arch((-10, pz), (10, pz), 18, 10, 'umbral_bricks', lambda t: t < 0.3 or t > 0.85)
        else:
            arch((-10, pz), (10, pz), 18, 10, 'umbral_bricks', lambda t, k=k: k % 2 == 0 or not 0.45 < t < 0.6)
            w.put(0, 27, pz, 'umbral_lamp')
            w.put(0, 26, pz, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
        for side in (-1, 1):
            if k + 1 < len(pillar_z) and (side, k) not in broken and (side, k + 1) not in broken:
                arch((side * 10, pz), (side * 10, pillar_z[k + 1]), 17, 4, 'umbral_bricks', lambda t: True)
            # aisle arches from the pillar to the outer wall
            if (side, k) not in broken:
                arch((side * 10, pz), (side * WALL_X, pz), 14, 3, 'umbral_bricks', lambda t: t < 0.95)
    # diagonal ribs over the crossing, half of them fallen
    arch((-10, pillar_z[2]), (10, pillar_z[3]), 18, 12, 'umbral_bricks', lambda t: t < 0.55)
    arch((10, pillar_z[2]), (-10, pillar_z[3]), 18, 12, 'umbral_bricks', lambda t: True)

    # ---- the south facade: great doors, a rose window, two broken towers
    fz = n1
    for x in range(-WALL_X, WALL_X + 1):
        h = 30 - abs(x) // 2
        for y in range(1, h):
            u = x / 4.0
            door = abs(x) <= 3 and y <= 9 + int(3 * (1 - abs(u) ** 1.5))
            rose = math.hypot(x, y - 18) <= 7
            if door:
                continue
            if rose:
                r = math.hypot(x, y - 18)
                a = math.atan2(y - 18, x)
                tracery = r > 6.3 or r < 1.4 or abs(math.sin(a * 6)) < 0.18 or 3.6 < r < 4.2
                if tracery:
                    w.put(x, y, fz, 'umbral_bricks' if r > 1.4 else 'umbral_lamp')
                elif rnd.random() < 0.85:
                    w.put(x, y, fz, 'eclipse_glass' if r > 4.2 else 'nebula_glass')
                continue
            w.put(x, y, fz, 'cracked_umbral_bricks' if rnd.random() < 0.15 else 'umbral_bricks')
    for dx in range(-4, 5):   # door frame lintel
        w.put(dx, 12, fz + 1, 'chiseled_umbral_bricks' if dx % 2 == 0 else 'umbral_bricks')
    for side in (-1, 1):
        tx0 = side * 16
        th = 38 if side < 0 else 21
        for y in range(1, th):
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if max(abs(dx), abs(dz)) == 3 and not (y > 8 and abs(dx) < 2 and y % 9 < 5):
                        if side > 0 and y > 16 and rnd.random() < (y - 16) / 6:
                            continue
                        w.put(tx0 + dx, y, fz + dz, 'cracked_umbral_bricks' if rnd.random() < 0.2 else 'umbral_bricks')
        if side < 0:
            for y in range(th, th + 9):   # spire
                r = 3 * (1 - (y - th) / 9)
                for dx in range(-3, 4):
                    for dz in range(-3, 4):
                        if abs(math.hypot(dx, dz) - r) < 0.7:
                            w.put(tx0 + dx, y, fz + dz, 'umbral_bricks')
            w.put(tx0, th + 9, fz, 'umbral_lamp')
            w.put(tx0, th - 1, fz, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
    # broad stairs down from the doors to the bridge
    for i in range(1, 6):
        for x in range(-5, 6):
            w.soft(x, 0, fz + i, 'umbral_bricks')
    # ---- rubble everywhere
    for _ in range(90):
        x, z = rnd.randint(-WALL_X + 1, WALL_X - 1), rnd.randint(n0 - 10, n1 - 1)
        if abs(x) <= 2 or math.hypot(x, z - cz) > R - 2:
            continue
        if w.get(x, 1, z) is None:
            w.put(x, 1, z, rnd.choice(('cracked_umbral_bricks', 'umbral_stone', 'umbral_brick_slab')),
                  **({'type': 'bottom', 'waterlogged': 'false'} if rnd.random() < 0.5 and False else {}))


def realm_umbral_throne():
    w = World(600)
    rnd = w.rnd
    cz = -51
    pad(w, 'umbral', 'umbral_bricks', 'umbral_lamp', 'chiseled_umbral_bricks', 'umbral_brick_wall')
    # the post shape uses walls; replace the lantern posts with proper wall posts
    bridge(w, -8, cz + 34, 'umbral_bricks', 'umbral_stone', 'minecraft:soul_lantern')
    cathedral(w, cz)
    # a shattered halo above the throne, its fragments drifting
    ring(w, 0, 42, cz, 46, (0.35, 1, -0.2),
         lambda i, t: None if (int(t * 20) % 5 == 0) else ('umbral_lamp' if i % 11 == 0 else 'umbral_bricks'), 1.0)
    for k in range(30):
        t = rnd.uniform(0, 1)
        a = t * 2 * math.pi
        r = 46 + rnd.uniform(-4, 8)
        sphere(w, round(math.cos(a) * r), 42 + rnd.randint(-12, 12), cz + round(math.sin(a) * r), rnd.uniform(0.8, 1.8),
               lambda: 'umbral_bricks' if rnd.random() < 0.8 else 'umbral_lamp')
    # black spires on broken islands, crowned with void crystals
    for k in range(10):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(48, 90)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(-20, 20)
        r = rnd.uniform(4, 7)
        island(w, x, y, z, r, 'umbral_stone', 'umbral_stone', 'minecraft:obsidian', seed=k + 610)
        h = rnd.randint(8, 22)
        for i in range(h):
            rr = 2.2 * (1 - i / h) + 0.4
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if math.hypot(dx, dz) <= rr:
                        w.put(x + dx, y + 1 + i, z + dz, 'minecraft:crying_obsidian' if rnd.random() < 0.12 else 'minecraft:obsidian')
        w.put(x, y + 1 + h, z, 'void_crystal')
    debris(w, rnd, 40, 120, -35, 60, 'umbral_stone', 'void_crystal', min_dist=55)
    return w


REALMS = {
    'firmament': realm_firmament,
    'sunspire': realm_sunspire,
    'moonwell': realm_moonwell,
    'starforge': realm_starforge,
    'archive': realm_archive,
    'umbral_throne': realm_umbral_throne,
}

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, fn in REALMS.items():
        fn().write(os.path.join(OUT, name + '.nbt'))
