"""Astral Vigil key art: a Blender (Cycles) scene built from the mod's own assets.

Run (headless):
  blender -b -P tools/marketing/keyart.py -- --out docs/marketing/keyart.png --skin <player-skin.png> [--slim]
        [--width 2560 --height 1440 --samples 256 --preview]

Everything is generated from the repo: GeckoLib geo/animation files (posed at chosen animation frames), the realm
template of the Umbral Cathedral (textured with the block atlas from tools/dev/atlas_export.py), the player model with
the given skin holding the Halo Edge, emissive glowmasks, volumetric fog and bloom.
Coordinates: Minecraft/three.js space (y up, models face -z) is converted to Blender (z up) with (x, y, z) -> (x, -z, y).
"""
import base64
import json
import math
import os
import re
import struct
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'astralvigil')
ATLAS_DIR = os.path.join(ROOT, 'docs', 'realm-viewer', 'data')


def arg(name, default=None, flag=False):
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    if flag:
        return name in argv
    return argv[argv.index(name) + 1] if name in argv else default


OUT = os.path.abspath(arg('--out', os.path.join(ROOT, 'docs', 'marketing', 'keyart.png')))
SKIN = arg('--skin')
SLIM = arg('--slim', flag=True)
W, H = int(arg('--width', 2560)), int(arg('--height', 1440))
SAMPLES = int(arg('--samples', 256))
PREVIEW = arg('--preview', flag=True)

# ------------------------------------------------------------------ math helpers


def to_blender(v):
    return Vector((v[0], -v[2], v[1]))


def rot_zyx(x, y, z):
    """three.js Euler order 'ZYX' (matrix = Rz * Ry * Rx)."""
    return Matrix.Rotation(z, 4, 'Z') @ Matrix.Rotation(y, 4, 'Y') @ Matrix.Rotation(x, 4, 'X')


AXIS = Matrix(((1, 0, 0, 0), (0, 0, -1, 0), (0, 1, 0, 0), (0, 0, 0, 1)))  # three -> blender


# ------------------------------------------------------------------ Molang + keyframes (GeckoLib subset)

def _sin(a):
    return math.sin(math.radians(a))


def _cos(a):
    return math.cos(math.radians(a))


MOLANG_NS = {'_sin': _sin, '_cos': _cos, 'abs': abs, 'min': min, 'max': max, 'sqrt': math.sqrt, 'pi': math.pi,
             '_clamp': lambda v, a, b: max(a, min(b, v)), '_lerp': lambda a, b, t: a + (b - a) * t, 'floor': math.floor}


def molang(v, t):
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v)
    s = re.sub(r'\b(query|q)\.(anim_time|life_time)\b', '(t)', s)
    s = re.sub(r'\b(query|q|variable|v)\.[a-z_]+\b', '0', s)
    s = s.replace('math.sin', '_sin').replace('math.cos', '_cos').replace('math.abs', 'abs').replace('math.clamp', '_clamp')
    s = s.replace('math.lerp', '_lerp').replace('math.sqrt', 'sqrt').replace('math.min', 'min').replace('math.max', 'max')
    s = s.replace('math.pi', 'pi').replace('math.floor', 'floor')
    try:
        return float(eval(s, {'__builtins__': {}}, dict(MOLANG_NS, t=t)))  # noqa: S307 – our own asset files
    except Exception:
        return 0.0


def _ease(name, x):
    if not name or name == 'linear':
        return x
    if name == 'step':
        return 0.0 if x < 1 else 1.0
    c1 = 1.70158
    base = {
        'Sine': lambda x: 1 - math.cos(x * math.pi / 2), 'Quad': lambda x: x * x, 'Cubic': lambda x: x ** 3,
        'Quart': lambda x: x ** 4, 'Quint': lambda x: x ** 5, 'Expo': lambda x: 0 if x == 0 else 2 ** (10 * x - 10),
        'Circ': lambda x: 1 - math.sqrt(max(0.0, 1 - x * x)), 'Back': lambda x: (c1 + 1) * x ** 3 - c1 * x * x,
        'Elastic': lambda x: x if x in (0, 1) else -(2 ** (10 * x - 10)) * math.sin((x * 10 - 10.75) * (2 * math.pi / 3)),
    }

    def bounce_out(x):
        n, d = 7.5625, 2.75
        if x < 1 / d:
            return n * x * x
        if x < 2 / d:
            x -= 1.5 / d
            return n * x * x + 0.75
        if x < 2.5 / d:
            x -= 2.25 / d
            return n * x * x + 0.9375
        x -= 2.625 / d
        return n * x * x + 0.984375
    base['Bounce'] = lambda x: 1 - bounce_out(1 - x)
    for k, f in base.items():
        if name == 'easeIn' + k:
            return f(x)
        if name == 'easeOut' + k:
            return 1 - f(1 - x)
        if name == 'easeInOut' + k:
            return f(2 * x) / 2 if x < 0.5 else 1 - f(2 - 2 * x) / 2
    return x


def channel(spec, t):
    if spec is None:
        return None

    def vec(v, tt):
        arr = v if isinstance(v, list) else [v, v, v]
        return [molang(c, tt) for c in arr]

    if isinstance(spec, list) or not isinstance(spec, dict) or 'vector' in spec:
        return vec(spec['vector'] if isinstance(spec, dict) else spec, t)
    keys = []
    for k, val in sorted(spec.items(), key=lambda kv: float(kv[0])):
        if isinstance(val, dict):
            pre = val.get('pre', val.get('vector', val.get('post')))
            post = val.get('post', val.get('vector', val.get('pre')))
            keys.append((float(k), pre, post, val.get('easing'), val.get('lerp_mode') == 'catmullrom'))
        else:
            keys.append((float(k), val, val, None, False))
    if t <= keys[0][0]:
        return vec(keys[0][1], t)
    for i in range(len(keys) - 1):
        t0, _, a_post, _, c0 = keys[i]
        t1, b_pre, _, ease, c1 = keys[i + 1]
        if t > t1:
            continue
        s = (t - t0) / max(t1 - t0, 1e-6)
        a, b = vec(a_post, t), vec(b_pre, t)
        if c0 or c1:
            p0 = vec(keys[i - 1][2] if i > 0 else a_post, t)
            p3 = vec(keys[i + 2][1] if i + 2 < len(keys) else b_pre, t)
            s2, s3 = s * s, s * s * s
            return [0.5 * (2 * p1 + (-q0 + q2) * s + (2 * q0 - 5 * p1 + 4 * q2 - q3) * s2 + (-q0 + 3 * p1 - 3 * q2 + q3) * s3)
                    for q0, p1, q2, q3 in zip(p0, a, b, p3)]
        e = _ease(ease, s)
        return [x + (y - x) * e for x, y in zip(a, b)]
    return vec(keys[-1][2], t)


# ------------------------------------------------------------------ mesh building

class MeshBuilder:
    def __init__(self):
        self.verts, self.faces, self.uvs, self.mats = [], [], [], []

    def quad(self, corners, uv, mat=0):
        """corners: tl, tr, bl, br (Blender space); uv: (u0, v0, u1, v1) in 0..1, v0 = top."""
        base = len(self.verts)
        self.verts.extend(corners)
        tl, tr, bl, br = range(base, base + 4)
        self.faces.append((bl, br, tr, tl))
        u0, v0, u1, v1 = uv
        self.uvs.append(((u0, v1), (u1, v1), (u1, v0), (u0, v0)))
        self.mats.append(mat)

    def build(self, name, materials):
        me = bpy.data.meshes.new(name)
        me.from_pydata([tuple(v) for v in self.verts], [], self.faces)
        layer = me.uv_layers.new(name='UVMap')
        for poly, uvq in zip(me.polygons, self.uvs):
            for li, uv in zip(poly.loop_indices, uvq):
                layer.data[li].uv = uv
        for m in materials:
            me.materials.append(m)
        for poly, mi in zip(me.polygons, self.mats):
            poly.material_index = mi
            poly.use_smooth = False
        ob = bpy.data.objects.new(name, me)
        bpy.context.scene.collection.objects.link(ob)
        return ob


def box_faces(x0, y0, z0, x1, y1, z1):
    """Six faces of an axis-aligned box in three space as (name, tl, tr, bl, br) seen from outside."""
    return [
        ('east', (x1, y1, z1), (x1, y1, z0), (x1, y0, z1), (x1, y0, z0)),
        ('west', (x0, y1, z0), (x0, y1, z1), (x0, y0, z0), (x0, y0, z1)),
        ('up', (x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (x1, y1, z1)),
        ('down', (x0, y0, z1), (x1, y0, z1), (x0, y0, z0), (x1, y0, z0)),
        ('south', (x0, y1, z1), (x1, y1, z1), (x0, y0, z1), (x1, y0, z1)),
        ('north', (x1, y1, z0), (x0, y1, z0), (x1, y0, z0), (x0, y0, z0)),
    ]


def box_uv_rects(u, v, w, h, d, mirror=False):
    r = {'east': (u, v + d, d, h), 'west': (u + d + w, v + d, d, h), 'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
         'south': (u + 2 * d + w, v + d, w, h), 'north': (u + d, v + d, w, h)}
    if mirror:
        r = {k: (x + ww, y, -ww, hh) for k, (x, y, ww, hh) in r.items()}
        r['east'], r['west'] = r['west'], r['east']
    return r


def add_box(mb, mat4, size_min, size_max, rects, tw, th, mat=0, inflate=0.0):
    x0, y0, z0 = [a - inflate for a in size_min]
    x1, y1, z1 = [a + inflate for a in size_max]
    for name, *corners in box_faces(x0, y0, z0, x1, y1, z1):
        rect = rects.get(name)
        if rect is None or (rect[2] == 0 and rect[3] == 0):
            continue
        u, v, w, h = rect
        uv = (u / tw, 1 - v / th, (u + w) / tw, 1 - (v + h) / th)
        if name == 'down':
            uv = (uv[0], uv[3], uv[2], uv[1])
        pts = [AXIS @ (mat4 @ Vector(c)) for c in corners]
        mb.quad(pts, (uv[0], 1 - uv[1], uv[2], 1 - uv[3]) if False else uv_top(uv), mat)


def uv_top(uv):
    """MeshBuilder.quad wants v0 = top edge; our uv already has v measured with 1 = top."""
    u0, v_top, u1, v_bottom = uv
    return (u0, v_top, u1, v_bottom)


# ------------------------------------------------------------------ materials

def image(path_or_b64, name):
    if path_or_b64.startswith('data:'):
        data = base64.b64decode(path_or_b64.split(',', 1)[1])
        path = os.path.join(bpy.app.tempdir or os.environ.get('TEMP', '.'), name + '.png')
        open(path, 'wb').write(data)
        path_or_b64 = path
    img = bpy.data.images.load(path_or_b64, check_existing=False)
    img.name = name
    return img


def pixel_material(name, tex, glow=None, glow_strength=8.0, alpha_blend=False, emission_all=0.0, roughness=0.75):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    bsdf = nt.nodes.new('ShaderNodeBsdfPrincipled')
    bsdf.inputs['Roughness'].default_value = roughness
    ti = nt.nodes.new('ShaderNodeTexImage')
    ti.image = tex
    ti.interpolation = 'Closest'
    nt.links.new(ti.outputs['Color'], bsdf.inputs['Base Color'])
    nt.links.new(ti.outputs['Alpha'], bsdf.inputs['Alpha'])
    if glow is not None:
        gi = nt.nodes.new('ShaderNodeTexImage')
        gi.image = glow
        gi.interpolation = 'Closest'
        nt.links.new(gi.outputs['Color'], bsdf.inputs['Emission Color'])
        bsdf.inputs['Emission Strength'].default_value = glow_strength
    elif emission_all > 0:
        nt.links.new(ti.outputs['Color'], bsdf.inputs['Emission Color'])
        bsdf.inputs['Emission Strength'].default_value = emission_all
    nt.links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    m.blend_method = 'BLEND' if alpha_blend else 'CLIP'
    try:
        m.surface_render_method = 'BLENDED' if alpha_blend else 'DITHERED'
    except AttributeError:
        pass
    return m


# ------------------------------------------------------------------ GeckoLib entities

def build_geo(name, geo_path, anim_path, anim, t, tex, glow, location, yaw_deg, scale, glow_strength=8.0, hide_prefixes=()):
    geo = json.load(open(geo_path))['minecraft:geometry'][0]
    anims = json.load(open(anim_path))['animations'] if anim_path else {}
    clip = anims.get(anim, {}) if anim else {}
    tw, th = geo['description']['texture_width'], geo['description']['texture_height']
    bones = {b['name']: b for b in geo['bones']}
    world = {}

    def pivot(b):
        p = b.get('pivot', [0, 0, 0])
        return Vector((-p[0], p[1], p[2]))

    def bone_world(n):
        if n in world:
            return world[n]
        b = bones[n]
        parent = b.get('parent')
        pw = bone_world(parent) if parent else Matrix.Identity(4)
        pp = pivot(bones[parent]) if parent else Vector((0, 0, 0))
        r = b.get('rotation', [0, 0, 0])
        rx, ry, rz = -math.radians(r[0]), -math.radians(r[1]), math.radians(r[2])
        pos = pivot(b) - pp
        sc = Vector((1, 1, 1))
        ch = clip.get('bones', {}).get(n)
        if ch:
            ar = channel(ch.get('rotation'), t)
            if ar:
                rx, ry, rz = rx - math.radians(ar[0]), ry - math.radians(ar[1]), rz + math.radians(ar[2])
            ap = channel(ch.get('position'), t)
            if ap:
                pos = pos + Vector((-ap[0], ap[1], ap[2]))
            asc = channel(ch.get('scale'), t)
            if asc:
                sc = Vector(asc)
        m = pw @ Matrix.Translation(pos) @ rot_zyx(rx, ry, rz) @ Matrix.Diagonal((sc.x, sc.y, sc.z, 1))
        world[n] = m
        return m

    mb = MeshBuilder()
    for b in geo['bones']:
        if any(b['name'].startswith(p) for p in hide_prefixes):
            continue
        bw = bone_world(b['name'])
        if bw.to_scale().length < 1e-3:
            continue
        for c in b.get('cubes', []):
            size, o, inf = c.get('size', [0, 0, 0]), c.get('origin', [0, 0, 0]), c.get('inflate', 0)
            lo = Vector((-(o[0] + size[0]), o[1], o[2]))
            hi = lo + Vector(size)
            m = bw
            lo = lo - pivot(b)
            hi = hi - pivot(b)
            if c.get('rotation'):
                cp = c.get('pivot', [0, 0, 0])
                piv = Vector((-cp[0], cp[1], cp[2])) - pivot(b)
                cr = c['rotation']
                m = bw @ Matrix.Translation(piv) @ rot_zyx(-math.radians(cr[0]), -math.radians(cr[1]), math.radians(cr[2]))
                lo, hi = lo - piv, hi - piv
            uvs = c.get('uv')
            if isinstance(uvs, list):
                rects = box_uv_rects(uvs[0], uvs[1], size[0], size[1], size[2], c.get('mirror', False))
            elif isinstance(uvs, dict):
                rects = {k: (f['uv'][0], f['uv'][1], f['uv_size'][0], f['uv_size'][1]) for k, f in uvs.items()}
            else:
                rects = {}
            add_box(mb, m, lo, hi, rects, tw, th, 0, inf)
    mat = pixel_material(name + '_mat', tex, glow, glow_strength)
    ob = mb.build(name, [mat])
    ob.scale = (scale / 16, scale / 16, scale / 16)
    ob.rotation_euler = (0, 0, math.radians(yaw_deg))
    ob.location = location
    return ob


# ------------------------------------------------------------------ player

def build_player(skin_path, slim, location, yaw_deg, pose, sword_tex=None):
    tex = image(skin_path, 'player_skin')
    aw = 3 if slim else 4
    # java model space (y down) -> three space: x' = -x, y' = 24 - y (feet at 0)
    parts = {  # pivot, box (x, y, z, w, h, d), uv, overlay uv, overlay inflate
        'head': ((0, 0, 0), (-4, -8, -4, 8, 8, 8), (0, 0), (32, 0), 0.5),
        'body': ((0, 0, 0), (-4, 0, -2, 8, 12, 4), (16, 16), (16, 32), 0.25),
        'right_arm': ((-5, 2 if not slim else 2.5, 0), (-3 if not slim else -2, -2, -2, aw, 12, 4), (40, 16), (40, 32), 0.25),
        'left_arm': ((5, 2 if not slim else 2.5, 0), (-1, -2, -2, aw, 12, 4), (32, 48), (48, 48), 0.25),
        'right_leg': ((-1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (0, 16), (0, 32), 0.25),
        'left_leg': ((1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (16, 48), (0, 48), 0.25),
    }
    mb = MeshBuilder()
    hand = None
    for name, (pv, bx, uv, ouv, inf) in parts.items():
        rx, ry, rz = pose.get(name, (0, 0, 0))
        m = Matrix.Translation(Vector((-pv[0], 24 - pv[1], pv[2]))) @ rot_zyx(rx, ry, rz)
        x, y, z, w, h, d = bx
        lo = Vector((-(x + w), -(y + h), z))
        hi = lo + Vector((w, h, d))
        add_box(mb, m, lo, hi, box_uv_rects(uv[0], uv[1], w, h, d), 64, 64, 0)
        add_box(mb, m, lo, hi, box_uv_rects(ouv[0], ouv[1], w, h, d), 64, 64, 0, inf)
        if name == 'right_arm':
            hand = m @ Matrix.Translation(Vector(((lo.x + hi.x) / 2, lo.y - 1.0, (lo.z + hi.z) / 2)))
    mat = pixel_material('player_mat', tex, roughness=0.85)
    ob = mb.build('player', [mat])
    ob.scale = (1 / 16, 1 / 16, 1 / 16)
    ob.rotation_euler = (0, 0, math.radians(yaw_deg))
    ob.location = location
    if sword_tex:
        sw = build_item(sword_tex, 'halo_edge')
        sw.parent = ob
        # held like vanilla: blade along the arm's forward axis, tilted up
        sw.matrix_parent_inverse = Matrix.Identity(4)
        sw.matrix_basis = AXIS @ hand @ Matrix.Translation(Vector((0, 0, -1))) @ Matrix.Rotation(math.radians(-90), 4, 'X') \
            @ Matrix.Rotation(math.radians(45), 4, 'Z') @ Matrix.Diagonal((16, 16, 16, 1)) @ Matrix.Translation(Vector((-0.5, -0.5, 0)))
    return ob


def build_item(tex_path, name):
    """Vanilla-style extruded item: every opaque pixel becomes a 1/16 voxel (thickness 1/16)."""
    import png_reader
    w, h, rows = png_reader.read(tex_path)
    tex = image(tex_path, name + '_tex')
    mb = MeshBuilder()
    filled = {(x, y) for y in range(h) for x in range(w) if rows[y][x][3] > 20}
    for (x, y) in filled:
        x0, x1 = x / w, (x + 1) / w
        y0, y1 = 1 - (y + 1) / h, 1 - y / h
        uv = (x / w + 0.25 / w, 1 - y / h - 0.25 / h, (x + 1) / w - 0.25 / w, 1 - (y + 1) / h + 0.25 / h)
        t = 1 / 32
        for nm, *c in box_faces(x0, y0, -t, x1, y1, t):
            if nm == 'east' and (x + 1, y) in filled or nm == 'west' and (x - 1, y) in filled \
                    or nm == 'up' and (x, y - 1) in filled or nm == 'down' and (x, y + 1) in filled:
                continue
            mb.quad([Vector(p) for p in c], uv, 0)
    mat = pixel_material(name + '_mat', tex, emission_all=1.5, roughness=0.3)
    return mb.build(name, [mat])


# ------------------------------------------------------------------ realm backdrop

def build_realm(path, atlas_png, atlas_meta, offset=(0, 0, 0)):
    d = json.load(open(path))
    pal = d['palette']
    raw = base64.b64decode(d['blocks'])
    cols, rows_n = atlas_meta['cols'], atlas_meta['rows']
    blocks = {}
    for i in range(len(raw) // 7):
        x, y, z, p = struct.unpack_from('<hhhB', raw, i * 7)
        blocks[(x, y, z)] = p
    opaque = {k for k, p in blocks.items() if pal[p][2] in (0, 1)}
    tex = image(atlas_png, 'realm_atlas')
    mats = [pixel_material('realm_solid', tex, roughness=0.8), pixel_material('realm_glow', tex, emission_all=5.0),
            pixel_material('realm_glass', tex, alpha_blend=True, roughness=0.1)]
    mb = MeshBuilder()
    eps = 0.15 / (16 * cols)
    for (x, y, z), p in blocks.items():
        name, _, kind, tiles = pal[p]
        s = 0.45 if kind == 3 else 1.0
        cx, cy, cz = x + 0.5, y + (0.225 if kind == 3 else 0.5), z + 0.5
        x0, y0, z0, x1, y1, z1 = cx - s / 2, cy - s / 2, cz - s / 2, cx + s / 2, cy + s / 2, cz + s / 2
        mat = {0: 0, 1: 1, 2: 2, 3: 1}[kind]
        for fname, *c in box_faces(x0, y0, z0, x1, y1, z1):
            n = {'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0), 'south': (0, 0, 1), 'north': (0, 0, -1)}[fname]
            if kind != 3 and (x + n[0], y + n[1], z + n[2]) in opaque:
                continue
            tile = tiles[0] if fname == 'up' else tiles[2] if fname == 'down' else tiles[1]
            tile = max(tile, 0)
            tx, ty = tile % cols, tile // cols
            uv = (tx / cols + eps, 1 - ty / rows_n - eps, (tx + 1) / cols - eps, 1 - (ty + 1) / rows_n + eps)
            mb.quad([to_blender(Vector(v)) + Vector(offset) for v in c], uv, mat)
    return mb.build('realm', mats)


# ------------------------------------------------------------------ scene

def setup_world():
    sc = bpy.context.scene
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)
    sc.render.engine = 'CYCLES'
    prefs = bpy.context.preferences.addons['cycles'].preferences
    for backend in ('OPTIX', 'CUDA'):
        try:
            prefs.compute_device_type = backend
            prefs.get_devices()
            if any(d.type == backend for d in prefs.devices):
                for dv in prefs.devices:
                    dv.use = dv.type == backend
                sc.cycles.device = 'GPU'
                break
        except Exception:
            continue
    sc.cycles.samples = 48 if PREVIEW else SAMPLES
    sc.cycles.use_denoising = True
    sc.cycles.max_bounces = 6
    sc.cycles.volume_step_rate = 4.0 if PREVIEW else 2.0
    sc.render.resolution_x, sc.render.resolution_y = (W // 3, H // 3) if PREVIEW else (W, H)
    sc.render.film_transparent = False
    sc.view_settings.view_transform = 'AgX'
    sc.view_settings.look = 'AgX - High Contrast'
    sc.view_settings.exposure = 0.3
    world = bpy.data.worlds.new('sky')
    sc.world = world
    world.use_nodes = True
    nt = world.node_tree
    bg = nt.nodes['Background']
    bg.inputs['Color'].default_value = (0.012, 0.006, 0.03, 1)
    bg.inputs['Strength'].default_value = 1.0
    vol = nt.nodes.new('ShaderNodeVolumePrincipled')
    vol.inputs['Color'].default_value = (0.55, 0.42, 0.85, 1)
    vol.inputs['Density'].default_value = 0.0035
    vol.inputs['Anisotropy'].default_value = 0.35
    nt.links.new(vol.outputs['Volume'], nt.nodes['World Output'].inputs['Volume'])


def light(kind, loc, energy, color, size=1.0, rot=None, spot=None):
    ld = bpy.data.lights.new(kind + '_light', kind)
    ld.energy = energy
    ld.color = color
    if kind in ('POINT', 'SPOT'):
        ld.shadow_soft_size = size
    if kind == 'AREA':
        ld.size = size
    if kind == 'SPOT' and spot:
        ld.spot_size = math.radians(spot)
        ld.spot_blend = 0.6
    ob = bpy.data.objects.new(ld.name, ld)
    ob.location = loc
    if rot:
        ob.rotation_euler = rot
    bpy.context.scene.collection.objects.link(ob)
    return ob


def look_at(ob, target):
    d = Vector(target) - ob.location
    ob.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()


def compositor():
    sc = bpy.context.scene
    sc.use_nodes = True
    nt = sc.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    rl = nt.nodes.new('CompositorNodeRLayers')
    glare = nt.nodes.new('CompositorNodeGlare')
    glare.glare_type = 'FOG_GLOW'
    glare.quality = 'HIGH'
    glare.threshold = 0.85
    glare.size = 9
    glare2 = nt.nodes.new('CompositorNodeGlare')
    glare2.glare_type = 'STREAKS'
    glare2.threshold = 3.0
    glare2.streaks = 6
    glare2.mix = -0.85
    lens = nt.nodes.new('CompositorNodeLensdist')
    for sock in lens.inputs:
        if sock.name.lower().startswith('disp'):
            sock.default_value = 0.012
        elif sock.name.lower().startswith('dist'):
            sock.default_value = -0.01
    out = nt.nodes.new('CompositorNodeComposite')
    nt.links.new(rl.outputs['Image'], glare.inputs['Image'])
    nt.links.new(glare.outputs['Image'], glare2.inputs['Image'])
    nt.links.new(glare2.outputs['Image'], lens.inputs['Image'])
    nt.links.new(lens.outputs['Image'], out.inputs['Image'])


def main():
    sys.path.insert(0, os.path.join(ROOT, 'tools', 'dev'))
    import png as png_mod
    sys.modules['png_reader'] = png_mod
    setup_world()
    meta = json.load(open(os.path.join(ATLAS_DIR, 'realm-atlas.json')))
    build_realm(os.path.join(ATLAS_DIR, 'realm-umbral-throne.json'), os.path.join(ATLAS_DIR, 'realm-atlas.png'), meta)

    geo = os.path.join(ASSETS, 'geo', 'entity')
    ani = os.path.join(ASSETS, 'animations', 'entity')
    tx = os.path.join(ASSETS, 'textures', 'entity')
    cfg = json.load(open(os.path.join(os.path.dirname(__file__), 'keyart_scene.json')))

    e = cfg['eclipse']
    build_geo('eclipse', os.path.join(geo, 'eclipse.geo.json'), os.path.join(ani, 'eclipse.animation.json'), e['anim'], e['time'],
              image(os.path.join(tx, 'eclipse', 'eclipse.png'), 'eclipse_tex'), image(os.path.join(tx, 'eclipse', 'eclipse_glowmask.png'), 'eclipse_glow'),
              Vector(e['location']), e['yaw'], e['scale'], e.get('glow', 10.0))
    for wcfg in cfg['watchers']:
        n = wcfg['name']
        build_geo('watcher_' + n, os.path.join(geo, 'watcher.geo.json'), os.path.join(ani, 'watcher.animation.json'), wcfg['anim'], wcfg['time'],
                  image(os.path.join(tx, 'watcher', n + '.png'), n + '_tex'), image(os.path.join(tx, 'watcher', n + '_glowmask.png'), n + '_glow'),
                  Vector(wcfg['location']), wcfg['yaw'], wcfg['scale'], wcfg.get('glow', 8.0), tuple(wcfg.get('hide', [])))
    p = cfg['player']
    pose = {k: tuple(math.radians(a) for a in v) for k, v in p['pose'].items()}
    build_player(SKIN or p['skin'], SLIM or p.get('slim', False), Vector(p['location']), p['yaw'], pose,
                 os.path.join(ASSETS, 'textures', 'item', 'halo_edge.png'))

    for lc in cfg['lights']:
        ob = light(lc['type'], Vector(lc['location']), lc['energy'], lc['color'], lc.get('size', 1.0), spot=lc.get('spot'))
        if 'target' in lc:
            look_at(ob, lc['target'])
    cam_d = bpy.data.cameras.new('cam')
    c = cfg['camera']
    cam_d.lens = c['lens']
    cam_d.dof.use_dof = c.get('dof', False)
    if cam_d.dof.use_dof:
        cam_d.dof.focus_distance = c['focus']
        cam_d.dof.aperture_fstop = c.get('fstop', 2.8)
    cam = bpy.data.objects.new('cam', cam_d)
    cam.location = Vector(c['location'])
    bpy.context.scene.collection.objects.link(cam)
    look_at(cam, c['target'])
    bpy.context.scene.camera = cam
    compositor()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    bpy.context.scene.render.filepath = OUT
    bpy.ops.render.render(write_still=True)
    print('KEYART_OK', OUT)


if __name__ == '__main__' and os.path.basename(sys.argv[sys.argv.index('-P') + 1] if '-P' in sys.argv else '') == 'keyart.py':
    main()
