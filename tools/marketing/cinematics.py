"""Cinematic shots of the Astral Vigil realms in the look of Minecraft with a popular shader pack
(Complementary / BSL style: soft sun shadows, volumetric light shafts, coloured block light, bloom, blocky clouds,
depth of field, filmic grading).

Run (headless):
  blender -b -P tools/marketing/cinematics.py -- --shots tools/marketing/cinematics.json [--only sunspire,moonwell]
        [--out docs/marketing/cinematics] [--width 2560 --height 1440 --samples 384 --preview]

Builds on keyart.py (GeckoLib importer, player, realm backdrop). Needs the atlas export (tools/dev/atlas_export.py).
"""
import json
import math
import os
import random
import sys

import bpy
from mathutils import Vector

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import keyart as K  # noqa: E402

ROOT = K.ROOT
SHOTS = os.path.abspath(K.arg('--shots', os.path.join(ROOT, 'tools', 'marketing', 'cinematics.json')))
OUT_DIR = os.path.abspath(K.arg('--out', os.path.join(ROOT, 'docs', 'marketing', 'cinematics')))
ONLY = [s for s in (K.arg('--only', '') or '').split(',') if s]


def reset():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)
    for coll in (bpy.data.meshes, bpy.data.materials, bpy.data.images, bpy.data.lights, bpy.data.cameras, bpy.data.worlds):
        for item in list(coll):
            coll.remove(item, do_unlink=True)


def render_settings(shot):
    sc = bpy.context.scene
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
    sc.cycles.samples = 64 if K.PREVIEW else K.SAMPLES
    sc.cycles.use_denoising = True
    sc.cycles.max_bounces = 8
    sc.cycles.volume_bounces = 1
    sc.cycles.volume_step_rate = 3.0 if K.PREVIEW else 1.5
    sc.cycles.volume_max_steps = 256
    sc.render.resolution_x, sc.render.resolution_y = (K.W // 3, K.H // 3) if K.PREVIEW else (K.W, K.H)
    sc.view_settings.view_transform = 'AgX'
    sc.view_settings.look = shot.get('look', 'AgX - Punchy')
    sc.view_settings.exposure = shot.get('exposure', 0.0)


# ------------------------------------------------------------------ skies

def world(shot):
    sky = shot['sky']
    w = bpy.data.worlds.new('sky_' + shot['id'])
    bpy.context.scene.world = w
    w.use_nodes = True
    nt = w.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputWorld')
    bg = nt.nodes.new('ShaderNodeBackground')
    nt.links.new(bg.outputs['Background'], out.inputs['Surface'])
    kind = sky['type']
    if kind == 'nishita':
        st = nt.nodes.new('ShaderNodeTexSky')
        st.sky_type = 'NISHITA'
        st.sun_elevation = math.radians(sky['sun_elevation'])
        st.sun_rotation = math.radians(sky['sun_rotation'])
        st.air_density = sky.get('air', 1.0)
        st.dust_density = sky.get('dust', 2.0)
        st.sun_disc = True
        st.sun_size = math.radians(3.0)
        st.sun_intensity = sky.get('sun_intensity', 0.6)
        nt.links.new(st.outputs['Color'], bg.inputs['Color'])
        bg.inputs['Strength'].default_value = sky.get('strength', 0.35)
    else:
        # night / end: vertical gradient + procedural star field (shader-pack style stars)
        tc = nt.nodes.new('ShaderNodeTexCoord')
        sep = nt.nodes.new('ShaderNodeSeparateXYZ')
        nt.links.new(tc.outputs['Generated'], sep.inputs['Vector'])
        ramp = nt.nodes.new('ShaderNodeValToRGB')
        ramp.color_ramp.elements[0].position = 0.35
        ramp.color_ramp.elements[0].color = tuple(sky['horizon']) + (1,)
        ramp.color_ramp.elements[1].position = 0.75
        ramp.color_ramp.elements[1].color = tuple(sky['zenith']) + (1,)
        nt.links.new(sep.outputs['Z'], ramp.inputs['Fac'])
        vor = nt.nodes.new('ShaderNodeTexVoronoi')
        vor.feature = 'DISTANCE_TO_EDGE' if False else 'F1'
        vor.inputs['Scale'].default_value = 420.0
        nt.links.new(tc.outputs['Generated'], vor.inputs['Vector'])
        thr = nt.nodes.new('ShaderNodeMath')
        thr.operation = 'LESS_THAN'
        thr.inputs[1].default_value = sky.get('star_size', 0.035)
        nt.links.new(vor.outputs['Distance'], thr.inputs[0])
        rnd = nt.nodes.new('ShaderNodeMath')
        rnd.operation = 'GREATER_THAN'
        rnd.inputs[1].default_value = 1.0 - sky.get('star_density', 0.35)
        nt.links.new(vor.outputs['Color'], rnd.inputs[0])
        both = nt.nodes.new('ShaderNodeMath')
        both.operation = 'MULTIPLY'
        nt.links.new(thr.outputs['Value'], both.inputs[0])
        nt.links.new(rnd.outputs['Value'], both.inputs[1])
        stars = nt.nodes.new('ShaderNodeMath')
        stars.operation = 'MULTIPLY'
        stars.inputs[1].default_value = sky.get('star_strength', 6.0)
        nt.links.new(both.outputs['Value'], stars.inputs[0])
        add = nt.nodes.new('ShaderNodeMix')
        add.data_type = 'RGBA'
        add.blend_type = 'ADD'
        add.inputs['Factor'].default_value = 1.0
        nt.links.new(ramp.outputs['Color'], add.inputs['A'])
        nt.links.new(stars.outputs['Value'], add.inputs['B'])
        nt.links.new(add.outputs['Result'], bg.inputs['Color'])
        bg.inputs['Strength'].default_value = sky.get('strength', 1.0)
    fog = shot.get('fog')
    if fog:
        fog_domain(shot, fog)


def fog_domain(shot, fog):
    """Atmospheric haze in a bounded box around the scene (a world volume would swallow the whole sky)."""
    c = Vector(shot['camera']['target'])
    size = fog.get('size', (260, 260, 160))
    me = bpy.data.meshes.new('fog')
    sx, sy, sz = (v / 2 for v in size)
    verts = [(x, y, z) for x in (-sx, sx) for y in (-sy, sy) for z in (-sz, sz)]
    faces = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    me.from_pydata(verts, [], faces)
    ob = bpy.data.objects.new('fog', me)
    ob.location = Vector((c.x, c.y, c.z + fog.get('lift', 0)))
    bpy.context.scene.collection.objects.link(ob)
    m = bpy.data.materials.new('fog_mat')
    m.use_nodes = True
    nt = m.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    vol = nt.nodes.new('ShaderNodeVolumePrincipled')
    vol.inputs['Color'].default_value = tuple(fog['color']) + (1,)
    vol.inputs['Density'].default_value = fog['density']
    vol.inputs['Anisotropy'].default_value = fog.get('anisotropy', 0.6)
    nt.links.new(vol.outputs['Volume'], out.inputs['Volume'])
    me.materials.append(m)
    ob.visible_shadow = False


def sun(cfg):
    ld = bpy.data.lights.new('sun', 'SUN')
    ld.energy = cfg['strength']
    ld.color = cfg['color']
    ld.angle = math.radians(cfg.get('angle', 0.6))
    ob = bpy.data.objects.new('sun', ld)
    bpy.context.scene.collection.objects.link(ob)
    el, rot = math.radians(cfg['elevation']), math.radians(cfg['rotation'])
    direction = Vector((math.sin(rot) * math.cos(el), math.cos(rot) * math.cos(el), math.sin(el)))  # towards the sun
    ob.rotation_euler = (-direction).to_track_quat('-Z', 'Y').to_euler()


# ------------------------------------------------------------------ minecraft touches

def blocky_clouds(cfg):
    """Vanilla-style cloud layer: 12x4x12 slabs on a noisy grid, soft white, slightly translucent."""
    rnd = random.Random(cfg.get('seed', 3))
    mb = K.MeshBuilder()
    cx, cy, z, extent = cfg['x'], cfg['y'], cfg['z'], cfg['extent']
    cell = 12
    cells = set()
    for i in range(-extent // cell, extent // cell):
        for j in range(-extent // cell, extent // cell):
            n = math.sin(i * 0.7 + j * 0.31) + math.sin(i * 0.23 - j * 0.57) + rnd.random() * 0.9
            if n > cfg.get('threshold', 0.9):
                cells.add((i, j))
    for (i, j) in cells:
        x0, y0 = cx + i * cell, cy + j * cell
        for name, *c in K.box_faces(x0, z, -(y0 + cell), x0 + cell, z + 4, -y0):
            nb = {'east': (i + 1, j), 'west': (i - 1, j), 'south': (i, j - 1), 'north': (i, j + 1)}.get(name)
            if nb and nb in cells:
                continue
            mb.quad([K.to_blender(Vector(v)) for v in c], (0, 1, 1, 0), 0)
    m = bpy.data.materials.new('clouds')
    m.use_nodes = True
    b = m.node_tree.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = tuple(cfg.get('color', (1, 1, 1))) + (1,)
    b.inputs['Alpha'].default_value = cfg.get('alpha', 0.8)
    b.inputs['Roughness'].default_value = 1.0
    b.inputs['Emission Color'].default_value = tuple(cfg.get('color', (1, 1, 1))) + (1,)
    b.inputs['Emission Strength'].default_value = cfg.get('glow', 0.15)
    m.blend_method = 'BLEND'
    mb.build('clouds', [m])


def floating_blocks(shot, atlas):
    cfg = shot.get('floaters')
    if not cfg:
        return
    import base64
    import struct
    d = json.load(open(os.path.join(K.ATLAS_DIR, 'realm-%s.json' % shot['realm'])))
    pal = d['palette']
    wanted = [i for i, p in enumerate(pal) if p[0] in cfg['blocks']]
    if not wanted:
        return
    meta = atlas
    cols, rows = meta['cols'], meta['rows']
    rnd = random.Random(cfg.get('seed', 7))
    tex = bpy.data.images.get('realm_atlas')
    mats = [K.pixel_material('float_solid', tex, roughness=0.8), K.pixel_material('float_glow', tex, emission_all=4.0)]
    mb = K.MeshBuilder()
    cx, cy, cz = cfg['centre']
    for _ in range(cfg['count']):
        p = pal[rnd.choice(wanted)]
        s = rnd.uniform(0.45, 1.0) if rnd.random() > 0.15 else rnd.uniform(1.2, 2.0)
        pos = Vector((cx + rnd.uniform(-1, 1) * cfg['spread'], cy + rnd.uniform(-1, 1) * cfg['spread'], cz + rnd.uniform(0, cfg['height'])))
        rx, ry, rz = (rnd.uniform(-0.6, 0.6) for _ in range(3))
        from mathutils import Euler
        rot = Euler((rx, ry, rz)).to_matrix()
        for name, *c in K.box_faces(-s / 2, -s / 2, -s / 2, s / 2, s / 2, s / 2):
            tile = max(p[3][0] if name == 'up' else p[3][2] if name == 'down' else p[3][1], 0)
            tx, ty = tile % cols, tile // cols
            uv = (tx / cols, 1 - ty / rows, (tx + 1) / cols, 1 - (ty + 1) / rows)
            mb.quad([pos + rot @ K.to_blender(Vector(v)) for v in c], uv, 1 if p[2] == 1 else 0)
    mb.build('floaters', mats)


# ------------------------------------------------------------------ shot

def shot_scene(shot):
    reset()
    render_settings(shot)
    world(shot)
    if shot.get('sun'):
        sun(shot['sun'])
    meta = json.load(open(os.path.join(K.ATLAS_DIR, 'realm-atlas.json')))
    K.build_realm(os.path.join(K.ATLAS_DIR, 'realm-%s.json' % shot['realm']), os.path.join(K.ATLAS_DIR, 'realm-atlas.png'), meta)
    if shot.get('clouds'):
        blocky_clouds(shot['clouds'])
    floating_blocks(shot, meta)
    geo = os.path.join(K.ASSETS, 'geo', 'entity')
    ani = os.path.join(K.ASSETS, 'animations', 'entity')
    tx = os.path.join(K.ASSETS, 'textures', 'entity')
    for e in shot.get('entities', []):
        kind = e['kind']
        if kind == 'watcher':
            K.build_geo('watcher_' + e['name'], os.path.join(geo, 'watcher.geo.json'), os.path.join(ani, 'watcher.animation.json'),
                        e['anim'], e['time'], K.image(os.path.join(tx, 'watcher', e['name'] + '.png'), e['name'] + '_tex'),
                        K.image(os.path.join(tx, 'watcher', e['name'] + '_glowmask.png'), e['name'] + '_glow'),
                        Vector(e['location']), e['yaw'], e.get('scale', 1.4), e.get('glow', 4.0))
        elif kind in ('eclipse', 'umbral_pylon', 'eclipse_echo'):
            tex_dir = os.path.join(tx, 'eclipse')
            base = os.path.join(tex_dir, kind + '.png') if kind != 'eclipse_echo' else os.path.join(tx, 'eclipse_echo.png')
            K.build_geo(kind, os.path.join(geo, kind + '.geo.json'), os.path.join(ani, kind + '.animation.json'), e['anim'], e['time'],
                        K.image(base, kind + '_tex'), K.image(os.path.join(tex_dir, kind + '_glowmask.png'), kind + '_glow'),
                        Vector(e['location']), e['yaw'], e.get('scale', 1.4), e.get('glow', 8.0))
        elif kind == 'player':
            pose = {k: tuple(math.radians(a) for a in v) for k, v in e['pose'].items()}
            K.build_player(K.SKIN or e['skin'], K.SLIM or e.get('slim', True), Vector(e['location']), e['yaw'], pose,
                           os.path.join(K.ASSETS, 'textures', 'item', e.get('item', 'halo_edge') + '.png') if e.get('item', 'halo_edge') else None)
    for lc in shot.get('lights', []):
        ob = K.light(lc['type'], Vector(lc['location']), lc['energy'], lc['color'], lc.get('size', 1.0), spot=lc.get('spot'))
        if 'target' in lc:
            K.look_at(ob, lc['target'])
    c = shot['camera']
    cam_d = bpy.data.cameras.new('cam_' + shot['id'])
    cam_d.lens = c['lens']
    cam_d.clip_end = 2000
    if c.get('focus'):
        cam_d.dof.use_dof = True
        cam_d.dof.focus_distance = c['focus']
        cam_d.dof.aperture_fstop = c.get('fstop', 2.8)
    cam = bpy.data.objects.new('cam', cam_d)
    cam.location = Vector(c['location'])
    bpy.context.scene.collection.objects.link(cam)
    K.look_at(cam, c['target'])
    if c.get('roll'):
        cam.rotation_euler.rotate_axis('Z', math.radians(c['roll']))
    bpy.context.scene.camera = cam
    compositor(shot)


def compositor(shot):
    sc = bpy.context.scene
    sc.use_nodes = True
    nt = sc.node_tree
    for n in list(nt.nodes):
        nt.nodes.remove(n)
    rl = nt.nodes.new('CompositorNodeRLayers')
    bloom = nt.nodes.new('CompositorNodeGlare')
    bloom.glare_type = 'FOG_GLOW'
    bloom.quality = 'HIGH'
    bloom.threshold = shot.get('bloom_threshold', 0.9)
    bloom.size = 9
    bloom.mix = shot.get('bloom_mix', -0.2)
    cc = nt.nodes.new('CompositorNodeColorCorrection')
    cc.master_saturation = shot.get('saturation', 1.12)
    cc.master_contrast = shot.get('contrast', 1.05)
    lens = nt.nodes.new('CompositorNodeLensdist')
    for sock in lens.inputs:
        if sock.name.lower().startswith('disp'):
            sock.default_value = 0.008
    # vignette: elliptical mask blurred, multiplied over the image
    mask = nt.nodes.new('CompositorNodeEllipseMask')
    mask.width, mask.height = 0.92, 0.88
    blur = nt.nodes.new('CompositorNodeBlur')
    blur.size_x = blur.size_y = 220
    blur.use_relative = False
    mix_v = nt.nodes.new('CompositorNodeMixRGB')
    mix_v.blend_type = 'MULTIPLY'
    mix_v.inputs['Fac'].default_value = shot.get('vignette', 0.35)
    out = nt.nodes.new('CompositorNodeComposite')
    nt.links.new(rl.outputs['Image'], bloom.inputs['Image'])
    nt.links.new(bloom.outputs['Image'], cc.inputs['Image'])
    nt.links.new(cc.outputs['Image'], lens.inputs['Image'])
    nt.links.new(mask.outputs['Mask'], blur.inputs['Image'])
    nt.links.new(lens.outputs['Image'], mix_v.inputs[1])
    nt.links.new(blur.outputs['Image'], mix_v.inputs[2])
    nt.links.new(mix_v.outputs['Image'], out.inputs['Image'])


def main():
    sys.path.insert(0, os.path.join(ROOT, 'tools', 'dev'))
    import png as png_mod
    sys.modules['png_reader'] = png_mod
    shots = json.load(open(SHOTS))['shots']
    os.makedirs(OUT_DIR, exist_ok=True)
    for shot in shots:
        if ONLY and shot['id'] not in ONLY:
            continue
        shot_scene(shot)
        path = os.path.join(OUT_DIR, shot['id'] + ('-preview' if K.PREVIEW else '') + '.png')
        bpy.context.scene.render.filepath = path
        bpy.ops.render.render(write_still=True)
        print('CINEMATIC_OK', path)


main()
