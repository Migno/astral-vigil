"""Exports everything the browser atlas (docs/realm-viewer) shows: realms with real block textures, entity and block
models with their animations, the armour sets with their 3D ornaments, and every texture of the mod.

Run: python -I tools/dev/atlas_export.py <out-dir> <vanilla-client-jar>
     (1.21.1 jar: versions/1.21.1-neoforge/build/moddev/artifacts/neoforge-21.1.266-client-extra-aka-minecraft-resources.jar)
Writes <out-dir>/data/{realm-<id>.json, models.json, armor.json, textures.json, realm-atlas.png}.
"""
import base64
import gzip
import json
import os
import struct
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import png  # noqa: E402
import realm_viewer_data as rvd  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(HERE))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'astralvigil')
TEX = os.path.join(ASSETS, 'textures')


def b64(path_or_bytes):
    data = path_or_bytes if isinstance(path_or_bytes, bytes) else open(path_or_bytes, 'rb').read()
    return 'data:image/png;base64,' + base64.b64encode(data).decode('ascii')


def size_of(data):
    return struct.unpack('>II', data[16:24])


# ------------------------------------------------------------------ textures

def textures():
    out = {}
    for dirpath, _, files in os.walk(TEX):
        for fn in sorted(files):
            if not fn.endswith('.png'):
                continue
            rel = os.path.relpath(os.path.join(dirpath, fn), TEX).replace('\\', '/')
            cat = rel.split('/')[0] if '/' in rel else 'misc'
            if rel.startswith('entity/'):
                parts = rel.split('/')
                cat = 'entity/' + parts[1] if len(parts) > 2 else 'entity'
            data = open(os.path.join(dirpath, fn), 'rb').read()
            w, h = size_of(data)
            animated = os.path.exists(os.path.join(dirpath, fn + '.mcmeta'))
            out.setdefault(cat, []).append({'name': rel[:-4], 'w': w, 'h': h, 'animated': animated, 'src': b64(data)})
    return out


# ------------------------------------------------------------------ models

ENTITIES = [
    ('watcher', 'Watchers', ['solenne', 'vael', 'orrin', 'ilyra'], 'watcher/{}', 'watcher/{}_glowmask'),
    ('eclipse', 'The Eclipse', ['eclipse'], 'eclipse/{}', 'eclipse/{}_glowmask'),
    ('umbral_pylon', 'Umbral Pylon', ['umbral_pylon'], 'eclipse/{}', 'eclipse/{}_glowmask'),
    ('eclipse_echo', 'Eclipse Echo', ['eclipse_echo'], '{}', None),
    ('seraph_wings', 'Seraph Wings', ['seraph_wings'], '{}', None),
]
BLOCK_MODELS = ['convergence_altar', 'astral_forge', 'pedestal', 'starlight_collector', 'lens', 'mirror', 'prism',
                'starlight_reservoir', 'star_anchor']


def tex_src(rel):
    p = os.path.join(TEX, 'entity', rel + '.png')
    return b64(p) if os.path.exists(p) else None


def models():
    ents = []
    for gid, title, variants, tpat, gpat in ENTITIES:
        geo = json.load(open(os.path.join(ASSETS, 'geo', 'entity', gid + '.geo.json')))['minecraft:geometry'][0]
        anims = json.load(open(os.path.join(ASSETS, 'animations', 'entity', gid + '.animation.json')))['animations']
        ents.append({'id': gid, 'title': title, 'geo': geo, 'animations': anims,
                     'variants': [{'name': v, 'tex': tex_src(tpat.format(v)), 'glow': tex_src(gpat.format(v)) if gpat else None}
                                  for v in variants]})
    blocks = []
    for name in BLOCK_MODELS:
        m = json.load(open(os.path.join(ASSETS, 'models', 'block', name + '.json')))
        texs = {}
        for k, v in m.get('textures', {}).items():
            p = os.path.join(TEX, 'block', v.split('/')[-1] + '.png')
            if os.path.exists(p):
                texs[k] = b64(p)
        blocks.append({'id': name, 'elements': m.get('elements', []), 'textures': texs})
    return {'entities': ents, 'blocks': blocks}


# ------------------------------------------------------------------ armour

def armor():
    import gen_armor  # noqa: E402
    sets = {}
    for name in ('seraph', 'solar', 'lunar'):
        pieces, atlas = gen_armor.build_ornaments(name)

        def part(p):
            return {'name': p.name, 'pose': list(p.pose),
                    'boxes': [[b.x, b.y, b.z, b.w, b.h, b.d, b.u, b.v] for b in p.boxes],
                    'children': [part(c) for c in p.children]}

        sets[name] = {
            'atlas': [atlas.w, atlas.h],
            'pieces': [{'name': pc.name, 'bone': pc.bone, 'slot': pc.slot, 'level': pc.level, 'anim': pc.anim, 'part': part(pc.part)}
                       for pc in pieces],
            'textures': {
                'layer1': b64(os.path.join(TEX, 'entity', 'equipment', 'humanoid', name + '.png')),
                'layer2': b64(os.path.join(TEX, 'entity', 'equipment', 'humanoid_leggings', name + '.png')),
                'glow1': b64(os.path.join(TEX, 'entity', 'armor', name + '_glow.png')),
                'glow2': b64(os.path.join(TEX, 'entity', 'armor', name + '_leggings_glow.png')),
                'ornaments': b64(os.path.join(TEX, 'entity', 'armor', name + '_ornaments.png')),
                'ornamentsGlow': b64(os.path.join(TEX, 'entity', 'armor', name + '_ornaments_glow.png')),
                'icons': [b64(os.path.join(TEX, 'item', f'{name}_{p}.png')) for p in ('helmet', 'chestplate', 'leggings', 'boots')],
            }}
    return sets


# ------------------------------------------------------------------ realms with textures

VANILLA_FACES = {
    'grass_block': ('grass_block_top', 'grass_block_side', 'dirt'),
    'bookshelf': ('oak_planks', 'bookshelf', 'oak_planks'),
    'chiseled_bookshelf': ('chiseled_bookshelf_top', 'chiseled_bookshelf_occupied', 'chiseled_bookshelf_top'),
    'lectern': ('lectern_top', 'lectern_sides', 'oak_planks'),
    'birch_slab': ('birch_planks',) * 3,
    'soul_campfire': ('soul_campfire_log_lit', 'soul_campfire_log_lit', 'soul_campfire_log_lit'),
    'magma_block': ('magma',) * 3,
    'water': ('water_still',) * 3,
    'smooth_sandstone': ('sandstone_top',) * 3,
    'moss_block': ('moss_block',) * 3,
}
TINT = {'grass_block_top': (0x7c, 0xbd, 0x6b)}


class TileAtlas:
    COLS = 32

    def __init__(self, jar):
        self.jar, self.tiles, self.index = jar, [], {}

    def load(self, ns, name):
        key = ns + ':' + name
        if key in self.index:
            return self.index[key]
        rows = None
        if ns == 'astralvigil':
            p = os.path.join(TEX, 'block', name + '.png')
            if os.path.exists(p):
                rows = png.read(p)[2]
        else:
            k = 'assets/minecraft/textures/block/%s.png' % name
            if k in self.jar.namelist():
                tmp = os.path.join(os.environ.get('TEMP', '.'), '_atlas_tile.png')
                open(tmp, 'wb').write(self.jar.read(k))
                try:
                    rows = png.read(tmp)[2]
                except AssertionError:
                    rows = None
        if rows is None:
            return None
        tile = [[rows[y % len(rows)][x % len(rows[0])] for x in range(16)] for y in range(16)]
        if name in TINT:
            t = TINT[name]
            tile = [[(p[0] * t[0] // 255, p[1] * t[1] // 255, p[2] * t[2] // 255, p[3]) for p in row] for row in tile]
        self.index[key] = len(self.tiles)
        self.tiles.append(tile)
        return self.index[key]

    def faces(self, full):
        ns, name = full.split(':')
        if ns == 'minecraft' and name in VANILLA_FACES:
            names = VANILLA_FACES[name]
            return [self.load('minecraft', n) for n in names]
        cands = [name]
        for suf in ('_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_button', '_pressure_plate', '_trapdoor'):
            if name.endswith(suf):
                base = name[: -len(suf)]
                cands = [base, base.replace('brick', 'bricks'), base + 's', base + '_planks'] + cands
                break
        if name in ('starwood', 'stripped_starwood'):
            cands.insert(0, name + '_log')
        top = side = bottom = None
        for c in cands:
            side = self.load(ns, c) if self.load(ns, c) is not None else self.load(ns, c + '_side')
            if side is not None:
                t = self.load(ns, c + '_top')
                top = t if t is not None else side
                b = self.load(ns, c + '_bottom')
                bottom = b if b is not None else top
                break
        if side is None:
            fallback = {'starlight_font': 'astral_core', 'mirror': 'mirror_plate', 'prism': 'prism_crystal', 'lens': 'lens_glass',
                        'trial_receptor': 'trial_receptor', 'soul_lantern': 'soul_lantern', 'end_rod': 'end_rod'}.get(name)
            if fallback:
                side = top = bottom = self.load(ns, fallback)
        return [top, side, bottom]

    def image(self):
        rows_n = (len(self.tiles) + self.COLS - 1) // self.COLS
        img = [[(0, 0, 0, 0)] * (self.COLS * 16) for _ in range(rows_n * 16)]
        for i, t in enumerate(self.tiles):
            ox, oy = (i % self.COLS) * 16, (i // self.COLS) * 16
            for y in range(16):
                for x in range(16):
                    img[oy + y][ox + x] = t[y][x]
        return img, rows_n


def realms(out, jar):
    atlas = TileAtlas(jar)
    tmp = os.path.join(out, '_realm_colours')
    os.makedirs(tmp, exist_ok=True)
    sys.argv = ['x', tmp, jar.filename]
    rvd.main()   # writes <tmp>/<realm>.json with palette + blocks
    for fn in sorted(os.listdir(tmp)):
        d = json.load(open(os.path.join(tmp, fn)))
        nbt = rvd.read_nbt(gzip.decompress(open(os.path.join(rvd.TEMPLATES, fn.replace('.json', '.nbt')), 'rb').read()))
        names = []
        for p in nbt['palette']:
            if p['Name'] not in names:
                names.append(p['Name'])
        tiles = [atlas.faces(n) for n in names]
        for entry, t in zip(d['palette'], tiles):
            entry.append([-1 if v is None else v for v in t])
        json.dump(d, open(os.path.join(out, 'data', 'realm-' + d['realm'].replace('_', '-') + '.json'), 'w'), separators=(',', ':'))
        os.remove(os.path.join(tmp, fn))
    os.rmdir(tmp)
    img, rows_n = atlas.image()
    png.write(os.path.join(out, 'data', 'realm-atlas.png'), img)
    return {'cols': TileAtlas.COLS, 'rows': rows_n}


def main():
    out, jar = sys.argv[1], zipfile.ZipFile(sys.argv[2])
    os.makedirs(os.path.join(out, 'data'), exist_ok=True)
    meta = realms(out, jar)
    json.dump(meta, open(os.path.join(out, 'data', 'realm-atlas.json'), 'w'))
    json.dump(models(), open(os.path.join(out, 'data', 'models.json'), 'w'), separators=(',', ':'))
    json.dump(armor(), open(os.path.join(out, 'data', 'armor.json'), 'w'), separators=(',', ':'))
    json.dump(textures(), open(os.path.join(out, 'data', 'textures.json'), 'w'), separators=(',', ':'))
    for f in sorted(os.listdir(os.path.join(out, 'data'))):
        print(f, os.path.getsize(os.path.join(out, 'data', f)))


if __name__ == '__main__':
    main()
