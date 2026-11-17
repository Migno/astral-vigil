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
    starforge       100     (0, 0, -50)     Orrin's forge-observatory (crystal vault, Great Forge, nebulae)
    archive         100     (0, 0, -63)     Ilyra's grand library (maze inside, dome, towers, portico)
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

    PAD_HALF = 12   # piece 0: the arrival shrine and its gate, placed at once when the first traveller arrives
    TILE = 16       # the rest is split into 16x16 columns that the server places over the following ticks

    def write_pieces(self, directory):
        """Writes p0.nbt (the arrival pad), then p1..pN (one per non-empty 16x16 column) - all in the same frame.
        The last piece also carries the completion marker (bedrock hidden under the pad, see RealmBuilder)."""
        os.makedirs(directory, exist_ok=True)
        for f in os.listdir(directory):
            if f.endswith('.nbt'):
                os.remove(os.path.join(directory, f))
        pad, tiles = {}, {}
        for (x, y, z), v in self.blocks.items():
            if abs(x) <= self.PAD_HALF and abs(z) <= self.PAD_HALF:
                pad[(x, y, z)] = v
            else:
                tiles.setdefault(((x + ANCHOR) // self.TILE, (z + ANCHOR) // self.TILE), {})[(x, y, z)] = v
        pieces = [pad] + [tiles[k] for k in sorted(tiles)]
        pieces[-1][(0, -3, 0)] = ('minecraft:bedrock', ())
        for i, blocks in enumerate(pieces):
            self.write(os.path.join(directory, 'p%d.nbt' % i), blocks, quiet=True)
        print('wrote', os.path.basename(directory), len(self.blocks), 'blocks in', len(pieces), 'pieces')

    def write(self, path, blocks=None, quiet=False):
        palette, index, entries = [], {}, []
        for (x, y, z), (name, props) in sorted((blocks if blocks is not None else self.blocks).items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
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
        if not quiet:
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


# ------------------------------------------------------------------ shared building helpers

def box(w, x0, y0, z0, x1, y1, z1, block, soft=False, **props):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                (w.soft if soft else w.put)(x, y, z, block, **props)


def stair(w, x, y, z, block, facing, half='bottom'):
    w.put(x, y, z, block, facing=facing, half=half, shape='straight', waterlogged='false')


def line(w, a, b, block, step=0.5, soft=True, **props):
    (ax, ay, az), (bx, by, bz) = a, b
    n = max(1, int(math.dist(a, b) / step))
    for i in range(n + 1):
        t = i / n
        p = (round(ax + (bx - ax) * t), round(ay + (by - ay) * t), round(az + (bz - az) * t))
        (w.soft if soft else w.put)(*p, block, **props)


def sag_bridge(w, a, b, deck, rail, sag=4):
    """A hanging rope bridge between two points: a slab deck sagging in the middle, fence posts along both sides."""
    (ax, ay, az), (bx, by, bz) = a, b
    n = int(max(abs(bx - ax), abs(bz - az)))
    for i in range(n + 1):
        t = i / n
        x = round(ax + (bx - ax) * t)
        z = round(az + (bz - az) * t)
        y = round(ay + (by - ay) * t - sag * 4 * t * (1 - t))
        w.soft(x, y, z, deck, type='bottom', waterlogged='false')
        if i % 3 == 0:
            side = (1, 0) if abs(bz - az) > abs(bx - ax) else (0, 1)
            for s in (-1, 1):
                w.soft(x + side[0] * s, y + 1, z + side[1] * s, rail, north='false', south='false', east='false', west='false',
                       waterlogged='false')


def noise3(x, y, z, seed):
    h = (x * 374761393 + y * 668265263 + z * 2147483647 + seed * 1274126177) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def nebula(w, cx, cy, cz, rx, ry, rz, cols, seed, density=0.035):
    """A cloud of stained glass that thins out towards its edge, salted with starry end rods."""
    for x in range(int(cx - rx), int(cx + rx) + 1):
        for y in range(int(cy - ry), int(cy + ry) + 1):
            for z in range(int(cz - rz), int(cz + rz) + 1):
                e = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                if e > 1:
                    continue
                swirl = math.sin(x * 0.13 + seed) + math.sin(y * 0.21 + z * 0.09 + seed * 2)
                d = density * (1 - e) * (1.2 + 0.6 * swirl)
                r = noise3(x, y, z, seed)
                if r < d:
                    w.soft(x, y, z, cols[int(noise3(x, z, y, seed + 1) * len(cols)) % len(cols)])
                elif r > 0.9993:
                    w.soft(x, y, z, 'minecraft:end_rod', facing='up')


def rock(w, cx, cy, cz, r, rnd, body, veins=(), vein_chance=0.08):
    """A lumpy asteroid."""
    bumps = [(rnd.uniform(-r, r), rnd.uniform(-r, r), rnd.uniform(-r, r), rnd.uniform(r * 0.4, r * 0.8)) for _ in range(4)]
    for x in range(int(cx - r * 1.6), int(cx + r * 1.6) + 1):
        for y in range(int(cy - r * 1.6), int(cy + r * 1.6) + 1):
            for z in range(int(cz - r * 1.6), int(cz + r * 1.6) + 1):
                inside = math.dist((x, y, z), (cx, cy, cz)) <= r
                for bx, by, bz, br in bumps:
                    inside = inside or math.dist((x, y, z), (cx + bx, cy + by, cz + bz)) <= br
                if inside:
                    w.put(x, y, z, rnd.choice(veins) if veins and rnd.random() < vein_chance else body)


# ------------------------------------------------------------------ Orrin: the Starforge

def prism_puzzle(w, cz, y=1):
    """The trial itself (unchanged mechanics): three bays, font -> prism -> mirrors (turned wrong) -> receptor."""
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


def starforge_hall(w, cz):
    """Orrin's forge-observatory: a crystal barrel vault on gilded ribs over the three mirror bays, a closed west apse
    with a rose window, an open colonnade to the east and the Great Forge on its plaza."""
    rnd = random.Random(401)
    X0, X1, Z = -24, 18, 24
    # the island: a broad disc for the hall, a second terrace for the plaza
    island(w, -3, 0, cz, 32, 'celestial_marble', 'celestial_marble', 'umbral_stone', depth=20, wobble=0.04, seed=402)
    island(w, 33, 0, cz, 17, 'celestial_marble', 'celestial_marble', 'umbral_stone', depth=14, wobble=0.05, seed=403)
    # hall floor: constellation tiles framed in gold
    for x in range(X0, X1 + 1):
        for z in range(-Z, Z + 1):
            edge = x in (X0, X1) or abs(z) == Z
            w.put(x, 0, cz + z, 'gilded_celestial_bricks' if edge else 'constellation_tiles' if (x + z) % 9 else 'astral_lamp')
    # outer walls: pilasters, a gilded band, tall halo-glass slits
    for x in range(X0, X1 + 1):
        for side in (-1, 1):
            z = cz + side * Z
            pil = (x - X0) % 6 == 0
            for y in range(1, 9):
                if pil:
                    w.put(x, y, z, 'celestial_pillar', axis='y')
                elif y == 4:
                    w.put(x, y, z, 'gilded_celestial_bricks')
                elif 2 <= y <= 7 and (x - X0) % 6 in (2, 3, 4):
                    w.put(x, y, z, 'halo_glass')
                else:
                    w.put(x, y, z, 'celestial_bricks')
            w.put(x, 9, z, 'gilded_celestial_bricks')
    # the crystal vault: a half-cylinder along x, ribs every six blocks
    R = Z
    for x in range(X0, X1 + 1):
        rib = (x - X0) % 6 == 0
        for i in range(181):
            a = math.pi * i / 180
            zz, yy = round(math.cos(a) * R), round(math.sin(a) * R * 0.85) + 9
            if rib:
                w.put(x, yy, cz + zz, 'minecraft:gold_block' if i % 30 else 'astral_lamp')
            else:
                w.soft(x, yy, cz + zz, 'halo_glass' if (x + i // 15) % 5 else 'nebula_glass')
    # west apse wall under the vault, with a rose window
    for z in range(-Z, Z + 1):
        top = round(math.sqrt(max(0, R * R - z * z)) * 0.85) + 9
        for y in range(1, top + 1):
            r = math.hypot(z, y - 16)
            a = math.atan2(y - 16, z)
            if r <= 9:
                tracery = r > 8.2 or r < 1.6 or abs(math.sin(a * 4)) < 0.16 or 4.5 < r < 5.2
                w.put(X0, y, cz + z, 'minecraft:gold_block' if tracery and r > 1.6 else 'astral_lamp' if r <= 1.6
                      else ('nebula_glass' if r < 4.5 else 'halo_glass'))
            else:
                w.put(X0, y, cz + z, 'celestial_bricks' if (y + z) % 11 else 'chiseled_celestial_marble')
    # east colonnade and a great gilded arch where the vault opens onto the plaza
    for z in range(-Z, Z + 1, 4):
        w.column(X1, cz + z, 1, 9, 'celestial_pillar', axis='y')
        w.put(X1, 9, cz + z, 'chiseled_celestial_marble')
    ring(w, X1 + 1, 9, cz, R, (1, 0, 0), lambda i, t: None if math.sin(t * 2 * math.pi) < -0.02 else
         ('astral_lamp' if i % 20 == 0 else 'minecraft:gold_block'), 0.5)
    # south door in the outer wall: a cloister walk links the bridge to the plaza
    for dx in (-1, 0, 1):
        for y in range(1, 6):
            w.clear(dx, y, cz + Z)
    for dx in (-2, 2):
        w.column(dx, cz + Z + 1, 1, 7, 'celestial_pillar', axis='y')
    for dx in range(-2, 3):
        w.put(dx, 7, cz + Z + 1, 'chiseled_celestial_marble' if dx == 0 else 'gilded_celestial_bricks')
    # hanging star lanterns along the vault ribs
    for x in range(X0 + 6, X1, 6):
        for z in (-12, 0, 12):
            w.put(x, 26 if z == 0 else 23, cz + z, 'star_lantern', hanging='true', waterlogged='false')
            line(w, (x, 27 if z == 0 else 24, cz + z), (x, 30 if z == 0 else 28, cz + z), 'minecraft:end_rod', soft=True, facing='up')

    # ---- the plaza and the Great Forge
    px = 33
    for x in range(px - 13, px + 14):
        for z in range(-13, 14):
            d = math.hypot(x - px, z)
            if d > 13.5:
                continue
            a = math.atan2(z, x - px)
            ray = abs(math.sin(a * 6)) < 0.18
            w.put(x, 0, cz + z, 'sunstone_lamp' if d < 3 else 'minecraft:gold_block' if ray and d > 4 else
                  'sunstone' if int(d) % 4 == 0 else 'celestial_marble')
    # the anvil
    for x in range(px - 4, px + 5):
        for z in range(-2, 3):
            w.put(x, 1, cz + z, 'umbral_bricks')
    for x in range(px - 2, px + 3):
        for z in range(-1, 2):
            for y in (2, 3):
                w.put(x, y, cz + z, 'umbral_bricks')
    for x in range(px - 6, px + 6):
        for z in range(-2, 3):
            for y in (4, 5):
                w.put(x, y, cz + z, 'minecraft:obsidian' if y == 4 else 'umbral_bricks')
    for i, x in enumerate(range(px + 6, px + 10)):   # the horn
        for z in range(-1 + (1 if i > 1 else 0), 2 - (1 if i > 1 else 0)):
            w.put(x, 5, cz + z, 'umbral_bricks')
        if i < 2:
            w.put(x, 4, cz, 'umbral_bricks')
    for x in range(px - 2, px + 3):   # a glowing starmetal ingot on the face
        w.put(x, 6, cz, 'astral_lamp')
        w.put(x, 6, cz + (1 if x % 2 else -1), 'minecraft:gold_block')
    # four obelisks at the plaza's corners, light tethers to the captured star
    star = (px, 24, cz)
    for dx, dz in ((-10, -10), (10, -10), (-10, 10), (10, 10)):
        ox, oz = px + dx, cz + dz
        box(w, ox - 1, 1, oz - 1, ox + 1, 2, oz + 1, 'celestial_bricks')
        w.column(ox, oz, 3, 15, 'celestial_pillar', axis='y')
        w.put(ox, 15, oz, 'chiseled_celestial_marble')
        w.put(ox, 16, oz, 'astral_lamp')
        line(w, (ox, 17, oz), star, 'minecraft:end_rod', step=0.6, facing='up')
    # the captured star inside a turning armillary
    sphere(w, *star, 4.6, lambda: 'astral_lamp' if rnd.random() < 0.75 else 'sunstone_lamp')
    for radius, normal in ((7, (0, 1, 0.25)), (8.5, (1, 0.2, 0)), (10, (0.3, 0.4, 1))):
        ring(w, *star, radius, normal, lambda i, t: 'astral_lamp' if i % 18 == 0 else 'minecraft:gold_block', 0.0)
    # forge furnaces along the plaza edge
    for k in range(-1, 2):
        fx, fz = px + 12, cz + k * 7
        box(w, fx, 1, fz - 1, fx + 2, 3, fz + 1, 'umbral_bricks')
        w.put(fx, 2, fz, 'minecraft:magma_block')
        w.column(fx + 1, fz, 4, 9, 'umbral_brick_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
        w.put(fx + 1, 9, fz, 'umbral_lamp')


def starforge_scenery(w, cz):
    rnd = random.Random(410)
    # three nebulae far out in the dark
    nebula(w, -88, 40, cz - 40, 32, 22, 28, ('minecraft:purple_stained_glass', 'minecraft:magenta_stained_glass', 'minecraft:blue_stained_glass'), 1)
    nebula(w, 92, 55, cz + 20, 28, 26, 34, ('minecraft:blue_stained_glass', 'minecraft:light_blue_stained_glass', 'minecraft:cyan_stained_glass'), 2)
    nebula(w, 10, 78, cz - 48, 40, 18, 20, ('minecraft:magenta_stained_glass', 'minecraft:pink_stained_glass', 'minecraft:purple_stained_glass'), 3)
    # an asteroid belt, some rocks mined from little starwood scaffolds
    for k in range(46):
        a = k / 46 * 2 * math.pi + rnd.uniform(-0.05, 0.05)
        d = rnd.uniform(60, 76)
        x, z, y = round(math.cos(a) * d) + 6, round(math.sin(a) * d) + cz + 8, round(rnd.uniform(-14, 14) + math.sin(a * 2) * 8)
        r = rnd.uniform(1.6, 4.2)
        rock(w, x, y, z, r, rnd, 'umbral_stone', ('minecraft:amethyst_block', 'astral_lamp', 'meteoric_stone'))
        if k % 7 == 0:
            top = y + int(r) + 1
            box(w, x - 2, top, z - 2, x + 2, top, z + 2, 'starwood_planks')
            for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
                w.column(x + dx, z + dz, top + 1, top + 3, 'starwood_fence', north='false', south='false', east='false', west='false',
                         waterlogged='false')
            w.put(x, top + 1, z, 'star_lantern', hanging='false', waterlogged='false')
    # two ringed moons
    for (mx, my, mz, r, body, ringb, normal) in ((-60, 30, cz + 55, 7, 'moonstone', 'halo_glass', (0.2, 1, 0.4)),
                                                 (70, 10, cz - 52, 5.5, 'sunstone', 'minecraft:gold_block', (-0.3, 1, 0.2))):
        sphere(w, mx, my, mz, r, lambda: body if rnd.random() < 0.85 else body + '_lamp')
        ring(w, mx, my, mz, r * 1.9, normal, lambda i, t: ringb if i % 3 else None, 1.0)
    # drifting forge fragments: broken anvils and ingots
    for k in range(10):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(40, 60)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(-6, 26)
        if x > 10 and abs(z - cz) < 20:
            continue
        box(w, x - 1, y, z - 1, x + 1, y, z + 1, 'umbral_bricks')
        w.put(x, y + 1, z, 'minecraft:gold_block' if k % 2 else 'astral_lamp')


# ------------------------------------------------------------------ Ilyra: the Endless Archive

def library_maze(w, cz, o):
    """The trial itself (unchanged mechanics): a 2-block-corridor bookshelf maze, the goal in the reading room."""
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
    walls[2 * mid + 1][g - 1] = False
    for gx in range(g):
        for gz in range(g):
            if abs(gx - (2 * mid + 1)) <= 1 and abs(gz - (2 * mid + 1)) <= 1:
                walls[gx][gz] = False
    roof_y = 5
    for gx in range(g):
        for gz in range(g):
            for bx in range(2):
                for bz in range(2):
                    x, z = o + 2 * gx + bx, o + 2 * gz + bz
                    w.put(x, 0, cz + z, 'starwood_planks' if (gx + gz) % 2 else 'constellation_tiles')
                    if walls[gx][gz]:
                        for yy in range(1, roof_y):
                            chis = yy == 2 and (x * 3 + z) % 7 == 0
                            w.put(x, yy, cz + z, 'minecraft:chiseled_bookshelf' if chis else 'minecraft:bookshelf',
                                  **({'facing': 'south', 'slot_0_occupied': 'true', 'slot_1_occupied': 'false', 'slot_2_occupied': 'true',
                                      'slot_3_occupied': 'true', 'slot_4_occupied': 'false', 'slot_5_occupied': 'true'} if chis else {}))
                    elif (gx * 7 + gz * 3) % 23 == 0 and bx == 0 and bz == 0:
                        w.put(x, roof_y - 1, cz + z, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
                    w.put(x, roof_y, cz + z, 'gilded_starwood' if gx % 6 == 0 or gz % 6 == 0 else 'starwood_planks')
    ec = 2 * mid + 1
    gx0, gz0 = o + 2 * ec, cz + o + 2 * ec
    w.put(gx0, 1, gz0, 'trial_goal', watcher='ilyra')
    for dx, dz in ((-2, -2), (3, -2), (-2, 3), (3, 3)):
        w.put(gx0 + dx, 1, gz0 + dz, 'minecraft:lectern', facing='north', has_book='false', powered='false')
    for dx in range(-2, 4):   # the reading room opens up into the dome
        for dz in range(-2, 4):
            w.put(gx0 + dx, roof_y, gz0 + dz, 'nebula_glass')
    return (gx0, gz0), (o + 2 * (2 * mid + 1), cz + o + 2 * g)


def library_hall(w, cz, rnd):
    """Ilyra's grand library around the maze: marble walls with a clerestory of arched windows, a mansard roof with a
    terrace, a ribbed dome over the reading room, four round corner towers and a columned portico facing the shrine."""
    H = 37
    # the platform: a rounded square with a tapering underside
    for x in range(-44, 45):
        for z in range(-44, 45):
            if max(abs(x), abs(z)) > 44 or (abs(x) > 40 and abs(z) > 40 and math.hypot(abs(x) - 40, abs(z) - 40) > 4):
                continue
            w.put(x, 0, cz + z, 'celestial_marble' if (x + z) % 5 else 'celestial_bricks')
            k = 1 - max(abs(x), abs(z)) / 45
            for y in range(-int(4 + k * 22), 0):
                w.put(x, y, cz + z, 'celestial_marble' if y > -3 else 'minecraft:calcite')
    (rx, rz), (ex, ez) = library_maze(w, cz, -35)
    # outer walls
    for t in range(-H, H + 1):
        for (x, z) in ((t, -H), (t, H), (-H, t), (H, t)):
            bay = (t + H) % 6
            for y in range(1, 15):
                if bay == 0:
                    b = 'celestial_pillar'
                elif y in (5, 14):
                    b = 'gilded_celestial_bricks'
                elif 7 <= y <= 12 and bay in (2, 3, 4) and not (bay != 3 and y == 12):
                    b = 'halo_glass'
                else:
                    b = 'celestial_marble' if y < 5 else 'celestial_bricks'
                if b == 'celestial_pillar':
                    w.put(x, y, cz + z, b, axis='y')
                else:
                    w.put(x, y, cz + z, b)
    # the entrance through the south wall, in line with the maze's way in
    for dx in range(-1, 3):
        for y in range(1, 5):
            w.clear(ex - 1 + dx, y, cz + H)
    # mansard roof: steep starwood stairs up to a terrace, then the dome
    for level in range(0, 10):
        r = H + 1 - level
        y = 15 + level
        for t in range(-r, r + 1):
            stair(w, t, y, cz - r, 'starwood_stairs', 'south')
            stair(w, t, y, cz + r, 'starwood_stairs', 'north')
            stair(w, -r, y, cz + t, 'starwood_stairs', 'east')
            stair(w, r, y, cz + t, 'starwood_stairs', 'west')
        for (x, z) in ((-r, -r), (r, -r), (-r, r), (r, r)):
            w.put(x, y, cz + z, 'gilded_starwood')
        if level % 3 == 1:   # dormer lamps
            for t in range(-r + 4, r - 3, 8):
                for (x, z) in ((t, -r - 1), (t, r + 1), (-r - 1, t), (r + 1, t)):
                    w.soft(x, y, cz + z, 'astral_lamp')
    T = H + 1 - 10
    for x in range(-T, T + 1):
        for z in range(-T, T + 1):
            w.put(x, 25, cz + z, 'constellation_tiles' if (x + z) % 4 else 'celestial_marble')
    for t in range(-T, T + 1, 2):
        for (x, z) in ((t, -T), (t, T), (-T, t), (T, t)):
            w.put(x, 26, cz + z, 'starwood_fence', north='false', south='false', east='false', west='false', waterlogged='false')
    # the dome: drum with windows, ribbed shell, lantern on top
    dx0, dz0 = rx + 0.5, rz + 0.5
    for i in range(96):
        a = i * 2 * math.pi / 96
        x, z = round(dx0 + math.cos(a) * 13), round(dz0 + math.sin(a) * 13)
        for y in range(26, 32):
            w.put(x, y, z, 'halo_glass' if 27 <= y <= 30 and i % 8 not in (0, 1) else 'celestial_bricks')
    for x in range(int(dx0) - 14, int(dx0) + 15):
        for z in range(int(dz0) - 14, int(dz0) + 15):
            for y in range(32, 46):
                d = math.sqrt((x - dx0) ** 2 + (z - dz0) ** 2 + ((y - 32) * 1.0) ** 2)
                if 12.2 < d <= 13.3:
                    a = math.atan2(z - dz0, x - dx0)
                    rib = abs(math.sin(a * 6)) < 0.14
                    w.put(x, y, z, 'minecraft:gold_block' if rib else ('nebula_glass' if y > 39 else 'starwood_planks' if y < 35 else 'halo_glass'))
    for y in range(45, 50):   # lantern
        for x in range(int(dx0) - 1, int(dx0) + 2):
            for z in range(int(dz0) - 1, int(dz0) + 2):
                corner = abs(x - int(dx0)) == 1 and abs(z - int(dz0)) == 1
                if corner or y in (45, 49):
                    w.put(x, y, z, 'celestial_pillar' if corner and y not in (45, 49) else 'gilded_celestial_bricks',
                          **({'axis': 'y'} if corner and y not in (45, 49) else {}))
        w.put(int(dx0), 47, int(dz0), 'astral_lamp')
    w.put(int(dx0), 50, int(dz0), 'minecraft:gold_block')
    w.put(int(dx0), 51, int(dz0), 'star_lantern', hanging='false', waterlogged='false')
    # four round corner towers with conical roofs
    for sx in (-H, H):
        for sz in (-H, H):
            for y in range(1, 34):
                for x in range(sx - 5, sx + 6):
                    for z in range(sz - 5, sz + 6):
                        d = math.hypot(x - sx, z - sz)
                        if 3.6 < d <= 4.6:
                            win = y % 8 in (3, 4, 5) and abs(math.sin(math.atan2(z - sz, x - sx) * 2)) < 0.3
                            w.put(x, y, cz + z, 'halo_glass' if win else 'gilded_celestial_bricks' if y % 8 == 0 else 'celestial_bricks')
                        elif d <= 3.6 and y in (1, 16, 33):
                            w.put(x, y, cz + z, 'starwood_planks')
            for k in range(0, 11):
                r = 5.2 - k * 0.5
                for i in range(int(2 * math.pi * max(r, 0.5) * 2) + 1):
                    a = i / (2 * math.pi * max(r, 0.5) * 2) * 2 * math.pi
                    w.put(round(sx + math.cos(a) * r), 34 + k, cz + round(sz + math.sin(a) * r), 'starwood_planks' if k % 3 else 'gilded_starwood')
            w.column(sx, cz + sz, 45, 48, 'minecraft:gold_block')
            w.put(sx, 48, cz + sz, 'astral_lamp')
            w.put(sx, 15, cz + sz + (5 if sz > 0 else -5), 'star_lantern', hanging='false', waterlogged='false')
    # the portico: steps, eight columns, an entablature and a pediment
    pz = H + 1
    for i in range(6):
        for x in range(-13 + i // 2, 14 - i // 2):
            w.put(x, 0, cz + pz + 6 - i, 'celestial_marble')
            if i < 5:
                stair(w, x, 0, cz + pz + 7 - i, 'celestial_marble_stairs', 'north')
    for x in range(-12, 13):
        for z in range(pz, pz + 6):
            w.put(x, 0, cz + z, 'constellation_tiles' if (x + z) % 3 else 'celestial_marble')
    for x in (-11, -8, -5, -2, 2, 5, 8, 11):
        w.column(x, cz + pz + 4, 1, 12, 'celestial_pillar', axis='y')
        w.put(x, 1, cz + pz + 4, 'chiseled_celestial_marble')
        w.put(x, 12, cz + pz + 4, 'chiseled_celestial_marble')
    for x in range(-12, 13):
        for z in range(pz, pz + 6):
            w.put(x, 13, cz + z, 'gilded_celestial_bricks' if z == pz + 5 else 'celestial_bricks')
    for k in range(0, 8):   # pediment
        for x in range(-12 + k * 2, 13 - k * 2):
            w.put(x, 14 + k, cz + pz + 5, 'celestial_marble' if k < 7 else 'chiseled_celestial_marble')
    w.put(0, 16, cz + pz + 6, 'astral_lamp')
    for x in (-12, 12):
        w.put(x, 1, cz + pz + 7, 'minecraft:lectern', facing='south', has_book='false', powered='false')
        w.put(x, 1, cz + pz + 8, 'star_lantern', hanging='false', waterlogged='false')


def archive_scenery(w, cz):
    rnd = random.Random(520)
    # floating stacks: tall bookshelf towers with balconies, joined by rope bridges
    towers = []
    for k in range(9):
        a = k * 2 * math.pi / 9 + rnd.uniform(-0.15, 0.15)
        d = rnd.uniform(62, 78)
        x, z, y = round(math.cos(a) * d), max(-116, round(math.sin(a) * d) + cz), rnd.randint(-22, 2)
        h = rnd.randint(22, 38)
        towers.append((x, y, z, h))
        island(w, x, y, z, 5.5, 'starwood_planks', 'celestial_marble', 'celestial_marble', depth=7, wobble=0.0, seed=530 + k)
        for yy in range(1, h):
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    edge = max(abs(dx), abs(dz))
                    if edge == 3:
                        corner = abs(dx) == 3 and abs(dz) == 3
                        w.put(x + dx, y + yy, z + dz, 'gilded_starwood' if corner else 'minecraft:bookshelf' if yy % 7 else 'starwood_planks')
            if yy % 7 == 0:   # balcony ring with railing and hanging lanterns
                for dx in range(-4, 5):
                    for dz in range(-4, 5):
                        if max(abs(dx), abs(dz)) == 4:
                            w.soft(x + dx, y + yy, z + dz, 'starwood_slab', type='top', waterlogged='false')
                            w.soft(x + dx, y + yy + 1, z + dz, 'starwood_fence', north='false', south='false', east='false', west='false',
                                   waterlogged='false')
                w.soft(x + 4, y + yy - 1, z, 'star_lantern', hanging='true', waterlogged='false')
                w.soft(x - 4, y + yy - 1, z, 'star_lantern', hanging='true', waterlogged='false')
        box(w, x - 3, y + h, z - 3, x + 3, y + h, z + 3, 'gilded_starwood')
        w.put(x, y + h + 1, z, 'astral_lamp')
        for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            w.put(x + dx, y + h + 1, z + dz, 'minecraft:purple_candle', candles='3', lit='true', waterlogged='false')
    for i in range(len(towers)):
        if i % 3 == 2:
            continue
        a, b = towers[i], towers[(i + 1) % len(towers)]
        ya = a[1] + 7 * max(1, (a[3] // 7) - 1)
        yb = b[1] + 7 * max(1, (b[3] // 7) - 1)
        sag_bridge(w, (a[0], ya, a[2]), (b[0], yb, b[2]), 'starwood_slab', 'starwood_fence', sag=6)

    # giant books: stacked and open, in leather colours with gilded spines and page edges
    covers = ('minecraft:red_wool', 'minecraft:blue_wool', 'minecraft:green_wool', 'minecraft:purple_wool', 'minecraft:brown_wool')

    def book(x, y, z, length, thick, width, cover, axis_x=True):
        for i in range(length):
            for j in range(thick):
                for k in range(width):
                    edge = i in (0, length - 1) or j in (0, thick - 1) or k == 0
                    spine = k == 0
                    bx, bz = (x + i, z + k) if axis_x else (x + k, z + i)
                    b = ('minecraft:gold_block' if spine and i % 3 == 1 else cover) if edge else 'minecraft:white_wool'
                    if not edge or (j not in (0, thick - 1) and k == width - 1):
                        b = 'minecraft:white_wool'
                    w.put(bx, y + j, bz, b)

    for s in range(3):
        a = rnd.uniform(0, 6.28)
        d = rnd.uniform(52, 60)
        x, z, y = round(math.cos(a) * d), round(math.sin(a) * d) + cz, rnd.randint(4, 18)
        for level in range(rnd.randint(3, 5)):
            l, wdt = rnd.randint(9, 13), rnd.randint(6, 8)
            book(x - l // 2 + rnd.randint(-1, 1), y, z - wdt // 2 + rnd.randint(-1, 1), l, 2, wdt, rnd.choice(covers), rnd.random() < 0.5)
            y += 2

    def open_book(x, y, z, cover):
        for half in (-1, 1):
            for i in range(1, 12):
                lift = round(i * 0.35)
                for k in range(-6, 7):
                    w.put(x + half * i, y + lift, z + k, cover if i == 11 or abs(k) == 6 else 'minecraft:white_wool')
                    if i < 11 and abs(k) < 6 and (k % 2 == 0) and 2 < i < 10:
                        w.put(x + half * i, y + lift, z + k, 'minecraft:light_gray_wool')
            w.put(x, y, z, cover)
        for k in range(-6, 7):
            w.put(x, y - 1, z + k, 'minecraft:gold_block' if k % 3 == 0 else cover)

    open_book(-58, 26, cz - 40, 'minecraft:blue_wool')
    open_book(60, 34, cz + 10, 'minecraft:red_wool')
    # the inkwell and a great quill
    ix, iy, iz = 48, 2, cz - 46
    island(w, ix, iy, iz, 7, 'celestial_marble', 'celestial_marble', depth=6, wobble=0.1, seed=560)
    for y in range(iy + 1, iy + 7):
        for x in range(ix - 3, ix + 4):
            for z in range(iz - 3, iz + 4):
                d = math.hypot(x - ix, z - iz)
                if d <= 3.3 and (d > 2.2 or y == iy + 1):
                    w.put(x, y, z, 'minecraft:black_concrete' if y < iy + 6 else 'minecraft:gold_block')
                elif d <= 2.2 and y == iy + 5:
                    w.put(x, y, z, 'minecraft:black_concrete_powder' if (x + z) % 2 else 'minecraft:black_concrete')
    for i in range(30):   # the quill, leaning out of the well
        t = i / 29
        qx, qy, qz = ix - 1 + t * 10, iy + 5 + t * 26, iz + t * 6
        vane = 0 if i < 4 else (2.5 if i < 24 else 2.5 * (29 - i) / 5)
        w.put(round(qx), round(qy), round(qz), 'minecraft:gold_block' if i < 3 else 'minecraft:white_wool')
        for v in range(1, int(vane) + 1):
            w.put(round(qx - v * 0.7), round(qy + v * 0.3), round(qz + v * 0.7), 'minecraft:white_wool' if i % 4 else 'minecraft:light_gray_wool')
            w.put(round(qx + v * 0.7), round(qy - v * 0.3), round(qz - v * 0.7), 'minecraft:white_wool')
    # a whirl of pages spiralling around the library
    for i in range(320):
        t = i / 320
        a = t * 6 * math.pi
        r = 52 + 10 * math.sin(t * 9)
        x, z, y = round(math.cos(a) * r), round(math.sin(a) * r) + cz, round(-6 + t * 54 + math.sin(t * 40) * 2)
        if i % 2 == 0:
            w.soft(x, y, z, 'minecraft:birch_slab', type='bottom', waterlogged='false')
    debris(w, rnd, 14, 120, -30, 30, 'celestial_marble', 'minecraft:bookshelf', min_dist=95)


def realm_starforge():
    w = World(400)
    cz = -50
    pad(w, 'prism', 'celestial_bricks', 'astral_lamp', 'constellation_tiles', 'celestial_pillar')
    bridge(w, -8, cz + 33, 'celestial_bricks', 'celestial_marble', 'star_lantern')
    starforge_hall(w, cz)
    prism_puzzle(w, cz)
    starforge_scenery(w, cz)
    return w


def realm_archive():
    w = World(500)
    rnd = w.rnd
    cz = -63
    pad(w, 'archive', 'starwood_planks', 'gilded_starwood', 'constellation_tiles', 'starwood_log')
    bridge(w, -8, cz + 46, 'celestial_marble', 'celestial_bricks', 'star_lantern')
    library_hall(w, cz, rnd)
    archive_scenery(w, cz)
    return w


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
        fn().write_pieces(os.path.join(OUT, name))
        old = os.path.join(OUT, name + '.nbt')
        if os.path.exists(old):
            os.remove(old)
