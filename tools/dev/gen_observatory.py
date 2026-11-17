"""Generates the Fallen Observatory structure templates (one per constellation lock).

Run: python -I tools/dev/gen_observatory.py
Output: src/main/resources/data/astralvigil/structure/fallen_observatory/<halo|crescent|anvil|quill>.nbt

A ruined round observatory: deepslate floor, broken walls and dome ribs, a telescope in the middle, an open chest,
and a small vault sealed by Constellation Lock blocks. Deterministic (seeded) so regenerating gives identical files.
"""
import gzip
import math
import os
import random
import struct

DATA_VERSION = 3955  # 1.21.1; newer targets upgrade the template via DataFixers
SIZE = (17, 10, 17)
C = 8  # centre x/z
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'data', 'astralvigil',
                   'structure', 'fallen_observatory')


# ------------------------------------------------------------------ NBT writer
def s(v):
    b = v.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def tag(t, name, payload):
    return bytes([t]) + s(name) + payload


def p_int(v):
    return struct.pack('>i', v)


def p_list(t, items):
    return bytes([t]) + struct.pack('>i', len(items)) + b''.join(items)


def comp(entries):
    return b''.join(entries) + b'\x00'


def t_int(n, v):
    return tag(3, n, p_int(v))


def t_str(n, v):
    return tag(8, n, s(v))


def t_list(n, t, items):
    return tag(9, n, p_list(t, items))


def t_comp(n, entries):
    return tag(10, n, comp(entries))


# ------------------------------------------------------------------ layout
def build(sign):
    rnd = random.Random(1730517123)
    blocks = {}

    def put(x, y, z, name, props=None, nbt=None):
        blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    r_floor = 7.5
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            d = math.hypot(x - C, z - C)
            if d <= r_floor:
                if d >= 6.8:
                    put(x, 0, z, 'minecraft:polished_blackstone_bricks')
                else:
                    roll = rnd.random()
                    if roll < 0.12:
                        put(x, 0, z, 'astralvigil:meteoric_stone')
                    elif roll < 0.35:
                        put(x, 0, z, 'minecraft:cracked_deepslate_bricks')
                    else:
                        put(x, 0, z, 'minecraft:deepslate_bricks')
                # clear the interior above the floor
                for y in range(1, SIZE[1]):
                    put(x, y, z, 'minecraft:air')

    # broken walls on the rim
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            d = math.hypot(x - C, z - C)
            if 6.6 <= d <= 7.5:
                height = rnd.choice([0, 1, 2, 3, 3, 4, 4, 5])
                for y in range(1, height + 1):
                    put(x, y, z, 'minecraft:cracked_deepslate_bricks' if rnd.random() < 0.3 else 'minecraft:deepslate_bricks')

    # pillars with gold caps
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        x, z = round(C + math.cos(a) * 6.8), round(C + math.sin(a) * 6.8)
        top = 5 + (i % 3)
        for y in range(1, top + 1):
            put(x, y, z, 'minecraft:polished_blackstone')
        if i % 2 == 0:
            put(x, top + 1, z, 'minecraft:gold_block')

    # dome ribs (some broken off)
    for i in range(8):
        if i in (2, 5):
            continue
        a = i * math.pi / 4 + math.pi / 8
        for step, (rr, y) in enumerate([(6.0, 7), (5.0, 8), (3.8, 9)]):
            if i == 6 and step > 0:
                break
            put(round(C + math.cos(a) * rr), y, round(C + math.sin(a) * rr), 'minecraft:polished_blackstone_bricks')

    # telescope
    put(C, 1, C, 'minecraft:lodestone')
    put(C, 2, C, 'minecraft:chain', {'axis': 'y'})
    put(C, 3, C, 'minecraft:chain', {'axis': 'y'})
    put(C, 4, C, 'minecraft:tinted_glass')
    put(C + 1, 4, C, 'minecraft:gold_block')
    put(C - 1, 4, C, 'minecraft:polished_blackstone')
    put(C, 5, C, 'astralvigil:star_anchor')

    # open chest
    put(C - 3, 1, C + 3, 'minecraft:chest', {'facing': 'east', 'type': 'single', 'waterlogged': 'false'},
        {'id': 'minecraft:chest', 'LootTable': 'astralvigil:chests/fallen_observatory'})

    # sealed vault on the north side (low z)
    for x in range(C - 2, C + 3):
        for z in range(1, 5):
            for y in range(1, 5):
                put(x, y, z, 'minecraft:deepslate_tiles')
    for x in range(C - 1, C + 2):
        for z in range(2, 4):
            for y in range(1, 3):
                put(x, y, z, 'minecraft:air')
    put(C, 1, 4, 'astralvigil:constellation_lock', {'constellation': sign})
    put(C, 2, 4, 'astralvigil:constellation_lock', {'constellation': sign})
    put(C, 1, 2, 'minecraft:chest', {'facing': 'south', 'type': 'single', 'waterlogged': 'false'},
        {'id': 'minecraft:chest', 'LootTable': 'astralvigil:chests/observatory_vault'})
    put(C - 1, 1, 2, 'minecraft:gold_block')
    put(C + 1, 1, 2, 'astralvigil:meteoric_stone')

    # palette + blocks
    palette = []
    index = {}
    entries = []
    for (x, y, z), (name, props, nbt) in sorted(blocks.items()):
        key = (name, props)
        if key not in index:
            index[key] = len(palette)
            pe = [t_str('Name', name)]
            if props:
                pe.append(t_comp('Properties', [t_str(k, v) for k, v in props]))
            palette.append(comp(pe))
        be = [t_list('pos', 3, [p_int(x), p_int(y), p_int(z)]), t_int('state', index[key])]
        if nbt:
            be.append(t_comp('nbt', [t_str(k, v) for k, v in nbt.items()]))
        entries.append(comp(be))

    root = tag(10, '', comp([
        t_int('DataVersion', DATA_VERSION),
        t_list('size', 3, [p_int(v) for v in SIZE]),
        t_list('palette', 10, palette),
        t_list('blocks', 10, entries),
        t_list('entities', 10, []),
    ]))
    return gzip.compress(root, mtime=0)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for sign in ('halo', 'crescent', 'anvil', 'quill'):
        path = os.path.join(OUT, sign + '.nbt')
        with open(path, 'wb') as f:
            f.write(build(sign))
        print('wrote', os.path.normpath(path))
