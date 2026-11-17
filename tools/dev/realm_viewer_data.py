"""Exports the realm templates for the browser 3D viewer (docs/realm-viewer).

Run: python -I tools/dev/realm_viewer_data.py <out-dir> [vanilla-client-jar]
Reads src/main/resources/data/astralvigil/structure/realm/*.nbt, drops blocks hidden on all six sides, colours every
block state from the average of its texture (mod textures from the repo, vanilla ones from the client jar if given)
and writes <out-dir>/<realm>.json: {"palette": [[name, "#rrggbb", kind]], "blocks": base64(int16 x,y,z + uint8 i)}.
kind: 0 solid, 1 glowing, 2 glass/liquid, 3 small (lanterns, candles: drawn smaller).
"""
import base64
import gzip
import io
import json
import os
import struct
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEMPLATES = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'astralvigil', 'structure', 'realm')
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'astralvigil', 'textures', 'block')
ANCHOR, FLOOR_Y = 128, 48


# ------------------------------------------------------------------ NBT
def read_nbt(data):
    f = io.BytesIO(data)

    def rd(fmt):
        return struct.unpack('>' + fmt, f.read(struct.calcsize('>' + fmt)))[0]

    def string():
        return f.read(rd('H')).decode('utf-8')

    def payload(t):
        if t == 1:
            return rd('b')
        if t == 2:
            return rd('h')
        if t == 3:
            return rd('i')
        if t == 4:
            return rd('q')
        if t == 5:
            return rd('f')
        if t == 6:
            return rd('d')
        if t == 7:
            return f.read(rd('i'))
        if t == 8:
            return string()
        if t == 9:
            et, n = rd('b'), rd('i')
            return [payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                tt = rd('b')
                if tt == 0:
                    return out
                key = string()
                out[key] = payload(tt)
        if t == 11:
            return [rd('i') for _ in range(rd('i'))]
        if t == 12:
            return [rd('q') for _ in range(rd('i'))]
        raise ValueError(t)

    t = rd('b')
    string()
    return payload(t)


# ------------------------------------------------------------------ colours
VANILLA_FIXED = {
    'grass_block': '#5f9a3c', 'moss_block': '#5a7a2c', 'water': '#3f76e4', 'white_wool': '#e9ecec',
    'white_stained_glass': '#ffffff', 'soul_lantern': '#7fe4ff', 'purple_candle': '#b57cff', 'soul_campfire': '#5fd8ff',
    'lectern': '#a07a4a', 'birch_slab': '#c8b77a', 'chiseled_bookshelf': '#8a6a3c', 'gold_block': '#f5cf45',
    'magma_block': '#9a4416', 'obsidian': '#15111f', 'crying_obsidian': '#3a1068', 'calcite': '#dfe0dc', 'dirt': '#86603f',
    'bookshelf': '#6b5236', 'amethyst_block': '#8562c0', 'smooth_sandstone': '#dcd09c',
}
GLOW_WORDS = ('lamp', 'lantern', 'portal', 'starlight_font', 'trial_goal', 'moon_target', 'candle', 'campfire', 'magma',
              'crying_obsidian', 'void_crystal', 'trial_receptor', 'eclipse_seal')
GLASS_WORDS = ('glass', 'water', 'portal')
SMALL_WORDS = ('lantern', 'candle', 'campfire', 'lectern', 'mirror', 'prism')


def avg(path_or_bytes, bright=False):
    if isinstance(path_or_bytes, bytes):
        tmp = os.path.join(os.environ.get('TEMP', '.'), '_av_tex.png')
        open(tmp, 'wb').write(path_or_bytes)
        path_or_bytes = tmp
    w, h, rows = png.read(path_or_bytes)
    px = [rows[y][x] for y in range(min(h, 16)) for x in range(w) if rows[y][x][3] > 40]
    if bright and px:
        px.sort(key=lambda p: p[0] + p[1] + p[2])
        px = px[len(px) * 2 // 3:]
    r = g = b = n = 0
    for p in px:
        r, g, b, n = r + p[0], g + p[1], b + p[2], n + 1
    if n == 0:
        return '#888888'
    return '#%02x%02x%02x' % (r // n, g // n, b // n)


def mod_texture(name):
    cands = [name, name + '_side', name + '_top']
    for suf, rep in (('_stairs', ''), ('_slab', ''), ('_wall', '')):
        if name.endswith(suf):
            base = name[: -len(suf)]
            cands = [base, base.replace('brick', 'bricks'), base + 's'] + cands
    if name in ('starwood', 'stripped_starwood'):
        cands.insert(0, name + '_log')
    if name.endswith('_portal') or name == 'celestial_portal':
        cands.insert(0, name)
    for c in cands:
        p = os.path.join(TEX, c + '.png')
        if os.path.exists(p):
            return p
    return None


def colour(name, jar):
    ns, path = name.split(':')
    if ns == 'astralvigil':
        p = mod_texture(path)
        if path == 'starlight_font':
            return '#8fefff'
        if path in ('prism', 'mirror', 'lens'):
            return '#c9971e'
        return avg(p, kind(name) == 1) if p else '#b9b3cc'
    if path in VANILLA_FIXED:
        return VANILLA_FIXED[path]
    if jar:
        for c in (path, path + '_side', path.replace('_slab', '_planks'), path.replace('_slab', '')):
            key = 'assets/minecraft/textures/block/%s.png' % c
            if key in jar.namelist():
                try:
                    return avg(jar.read(key))
                except AssertionError:
                    break
    return '#999999'


def kind(name):
    n = name.split(':')[1]
    if any(w in n for w in GLASS_WORDS) and 'lamp' not in n:
        return 2
    if any(w in n for w in SMALL_WORDS) and n not in ('starlight_font',):
        return 3
    if any(w in n for w in GLOW_WORDS):
        return 1
    return 0


def main():
    out = sys.argv[1]
    jar = zipfile.ZipFile(sys.argv[2]) if len(sys.argv) > 2 else None
    os.makedirs(out, exist_ok=True)
    summary = {}
    for fn in sorted(os.listdir(TEMPLATES)):
        realm = fn[:-4]
        nbt = read_nbt(gzip.decompress(open(os.path.join(TEMPLATES, fn), 'rb').read()))
        names = [p['Name'] for p in nbt['palette']]
        palette, pidx = [], {}
        for nm in names:
            if nm not in pidx:
                pidx[nm] = len(palette)
                palette.append([nm.split(':')[1], colour(nm, jar), kind(nm)])
        solid = {}
        for b in nbt['blocks']:
            x, y, z = b['pos']
            nm = names[b['state']]
            solid[(x - ANCHOR, y - FLOOR_Y, z - ANCHOR)] = pidx[nm]
        opaque = {k for k, v in solid.items() if palette[v][2] in (0, 1)}
        buf = bytearray()
        kept = 0
        for (x, y, z), i in solid.items():
            if (x, y, z) in opaque and all(n in opaque for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z),
                                                                 (x, y, z + 1), (x, y, z - 1))):
                continue
            buf += struct.pack('<hhhB', x, y, z, i)
            kept += 1
        with open(os.path.join(out, realm + '.json'), 'w', encoding='utf-8') as f:
            json.dump({'realm': realm, 'total': len(solid), 'palette': palette, 'blocks': base64.b64encode(bytes(buf)).decode('ascii')}, f,
                      separators=(',', ':'))
        summary[realm] = (len(solid), kept)
        print(realm, len(solid), 'blocks,', kept, 'visible')


if __name__ == '__main__':
    main()
