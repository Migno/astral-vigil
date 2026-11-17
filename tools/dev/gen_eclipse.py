"""Generates the Eclipse boss (and its Umbral Pylon) GeckoLib models, animations, textures and emissive glowmasks.

Run: python -I tools/dev/gen_eclipse.py
Outputs (both GeckoLib layouts):
  assets/astralvigil/{geo,geckolib/models}/entity/{eclipse,umbral_pylon}.geo.json
  assets/astralvigil/{animations,geckolib/animations}/entity/{eclipse,umbral_pylon}.animation.json
  assets/astralvigil/textures/entity/eclipse/{eclipse,eclipse_glowmask,umbral_pylon,umbral_pylon_glowmask}.png

Design: the fallen fifth Watcher - a hunched, towering, corrupted seraph. A black-sun corona burns behind its head,
its halo is cracked with two pieces drifting loose, six tattered wings hang from its back, a void core gapes in its
chest, its arms are long with clawed hands, umbral shards orbit it in two tilted rings and its robe frays into
swaying tendrils.

Move timings (ticks) MUST match media.hofferbert.astralvigil.eclipse.EclipseMove - a JUnit test compares the
animation lengths written here with the enum.
Art rules: <= 8 colours per texture, dark outline colour, light from the top-left.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil")

# K abyss, D void navy (outline), R robe, V eclipse violet, N tarnished gold dark, M tarnished gold, G glow, W glow white
PALETTE = {"K": "100C1A", "D": "1E1A2E", "R": "35294F", "V": "5B2A86", "N": "5E4A1C", "M": "9C7A2E",
           "G": "B57CFF", "W": "E0C8FF"}

# name: (windup, active, recovery) in ticks - keep in sync with EclipseMove.java
MOVES = {
    "sweep": (18, 6, 16), "lance": (26, 4, 14), "starfall": (16, 10, 14), "summon": (20, 4, 16),
    "dive": (30, 8, 24), "beam": (30, 100, 30), "nova": (50, 4, 30), "blink": (8, 2, 8), "grasp": (24, 6, 16),
}
SPECIALS = {"entrance": 70, "phase2": 60, "phase3": 60, "shield_raise": 30, "enrage": 40, "stagger": 80,
            "grounded": 60}
DEATH_TICKS = 100


# ------------------------------------------------------------------ materials: painter(face, x, y, w, h) -> (base, glow)

def edge(x, y, w, h):
    return x == 0 or y == 0 or x == w - 1 or y == h - 1


def h31(x, y):
    return (x * 73856093 ^ y * 19349663) & 0xFFFF


def mat_robe(face, x, y, w, h):
    if face in ("top", "bottom"):
        return "D", None
    if y == h - 1:
        return "D", None
    if y == 0:
        return "V", None
    return ("D" if x % 4 == 0 else "V" if (x + y) % 7 == 0 else "R"), None


def mat_carapace(face, x, y, w, h):
    if edge(x, y, w, h):
        return "D", None
    if face == "front" and (x == y or x == w - 1 - y + 2) and 0 < y < h - 2:
        return "G", "G"          # glowing corruption cracks
    return ("V" if (x + y) % 5 == 0 else "R"), None


def mat_armor(face, x, y, w, h):
    if edge(x, y, w, h):
        return "K", None
    return ("D" if (x * 3 + y) % 4 else "R"), None


def mat_trim(face, x, y, w, h):
    if y == h - 1:
        return "N", None
    return ("M" if (x + y) % 3 else "N"), None


def mat_void(face, x, y, w, h):
    return ("K" if (x + y) % 9 else "D"), None


def mat_core(face, x, y, w, h):
    c = "W" if (x in (1, 2) and y in (1, 2)) or (w <= 2) else "G"
    return c, c


def mat_rim(face, x, y, w, h):
    c = "W" if x % 3 == 0 else "G"
    return c, c


def mat_ray(face, x, y, w, h):
    if y >= h - 2:
        return "W", "W"
    return ("G" if y % 2 else "V"), ("G" if y % 2 else None)


def mat_helm(face, x, y, w, h):
    if face == "front":
        if y == h // 2 and 1 < x < w - 2 and x != w // 2:
            return "W", "W"     # slit eyes
        if y == h // 2 + 1 and x in (2, w - 3):
            return "G", "G"     # tear of light
        if x == w // 2 and y > h // 2 + 1:
            return "G", "G"     # crack down the mask
        return ("K" if edge(x, y, w, h) else "D"), None
    if face == "top":
        return "R", None
    return ("K" if y == h - 1 else "R" if y < 2 else "D"), None


def mat_spike(face, x, y, w, h):
    if y == 0 and face != "bottom":
        return "V", None
    return ("K" if x == w - 1 else "D"), None


def mat_crownspike(face, x, y, w, h):
    if y <= 1 or face == "top":
        return "G", "G"
    return ("D" if x else "K"), None


def mat_skin(face, x, y, w, h):
    if edge(x, y, w, h):
        return "D", None
    return ("V" if y % 3 == 0 else "R"), None


def mat_claw(face, x, y, w, h):
    if y >= h - 2:
        return "W", "W"
    return ("K" if x else "D"), None


def mat_halo(face, x, y, w, h):
    if face == "top" and x % 4 == 1:
        return "M", "G"
    return ("N" if y == h - 1 else "M"), ("G" if x == w // 2 and face == "front" else None)


def mat_wingbar(face, x, y, w, h):
    if face == "bottom" or y == h - 1:
        return "K", None
    return ("N" if x % 6 == 0 else "D"), ("G" if x % 6 == 0 and y == 0 else None)


def mat_feather(face, x, y, w, h):
    if face in ("top", "bottom"):
        return "D", None
    if y >= h - 1:
        return ("G" if x % 2 else "D"), ("G" if x % 2 else None)   # frayed, faintly burning tips
    if y >= h - 3 and (x + y) % 2:
        return "K", None
    return ("V" if x == 0 and y % 3 == 0 else "R" if y % 4 else "D"), None


def mat_shard(face, x, y, w, h):
    if edge(x, y, w, h) and (x + y) % 2 == 0:
        return "G", "G"
    return ("K" if (x + y) % 3 else "V"), None


def mat_tendril(face, x, y, w, h):
    if y >= h - 1:
        return "G", "G"
    return ("D" if (x + y) % 3 else "R"), None


MATERIALS = {"robe": mat_robe, "carapace": mat_carapace, "armor": mat_armor, "trim": mat_trim, "void": mat_void,
             "core": mat_core, "rim": mat_rim, "ray": mat_ray, "helm": mat_helm, "spike": mat_spike,
             "crownspike": mat_crownspike, "skin": mat_skin, "claw": mat_claw, "halo": mat_halo,
             "wingbar": mat_wingbar, "feather": mat_feather, "shard": mat_shard, "tendril": mat_tendril}


# ------------------------------------------------------------------ model building

class Model:
    def __init__(self, name, tw, th):
        self.name, self.tw, self.th = name, tw, th
        self.bones = []
        self.uv = {}
        self.cursor = [0, 0, 0]
        self.layout = []  # (u, v, w, h, d, material)

    def bone(self, name, parent, pivot, rotation=None):
        b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        self.bones.append(b)
        return b

    def alloc(self, w, h, d, material):
        key = (w, h, d, material)
        if key in self.uv:
            return self.uv[key]
        uw, uh = 2 * (d + w), d + h
        if self.cursor[0] + uw > self.tw:
            self.cursor = [0, self.cursor[1] + self.cursor[2], 0]
        u, v = self.cursor[0], self.cursor[1]
        assert v + uh <= self.th, f"{self.name}: texture full"
        self.cursor[0] += uw
        self.cursor[2] = max(self.cursor[2], uh)
        self.uv[key] = (u, v)
        self.layout.append((u, v, w, h, d, material))
        return u, v

    def cube(self, bone, origin, size, material, pivot=None, rotation=None, inflate=None):
        w, h, d = size
        u, v = self.alloc(int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d)), material)
        c = {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": [u, v]}
        if rotation:
            c["pivot"] = [round(p, 3) for p in pivot]
            c["rotation"] = [round(r, 3) for r in rotation]
        if inflate:
            c["inflate"] = inflate
        bone["cubes"].append(c)


SIDES = (("right", -1), ("left", 1))
TENDRILS = ((-5, -4, 13), (0, -5.5, 16), (5, -4, 12), (6.5, 1, 14), (3, 5, 15), (-3, 5, 13), (-6.5, 1, 11))


def build_eclipse():
    m = Model("eclipse", 256, 128)
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 36, 0))
    # tattered robe: waist, two flaring tiers; the figure floats, tendrils hang below
    m.cube(body, (-6, 31, -4), (12, 6, 8), "robe")
    m.cube(body, (-7, 24, -5), (14, 7, 10), "robe")
    m.cube(body, (-8, 17, -5.5), (16, 7, 11), "robe")
    m.cube(body, (-6.5, 35.5, -4.5), (13, 1.5, 9), "trim")             # tarnished belt
    m.cube(body, (-1, 17, -5.9), (2, 18, 0.5), "trim")                  # broken stole
    # hunched, broad torso with glowing cracks and a void core
    m.cube(body, (-8, 37, -4.5), (16, 12, 9), "carapace")
    for y in (40, 43, 46):                                              # gold ribs left and right of the core
        m.cube(body, (-7.5, y, -5), (4.5, 1, 1), "trim")
        m.cube(body, (3, y, -5), (4.5, 1, 1), "trim")
    m.cube(body, (-3, 39.5, -5.2), (6, 6, 1), "void")                   # core socket
    m.cube(body, (-2, 40.5, -5.8), (4, 4, 1), "core")                   # the void core itself
    for i, y in enumerate((38, 41.5, 45, 48.5)):                        # spine spikes
        m.cube(body, (-0.75, y, 4.2), (1.5, 3 + i % 2, 1.5), "spike", pivot=(0, y, 5), rotation=(-35, 0, 0))
    for sx in (-1, 1):                                                  # broken pauldrons with spikes
        x = 7 if sx > 0 else -13
        m.cube(body, (x, 46, -5), (6, 4, 10), "armor")
        m.cube(body, (x - 0.5 if sx < 0 else x + 0.5, 49.5, -5.5), (6, 1, 11), "trim")
        for j, (dz, hh) in enumerate(((-3, 7), (0, 9), (3, 6))):
            px = sx * 10
            m.cube(body, (px - 1, 50, dz - 1), (2, hh, 2), "spike", pivot=(px, 50, dz), rotation=(0, 0, sx * -(25 + 10 * j)))
    m.cube(body, (-5, 49, -4), (10, 3, 8), "armor")                     # high collar
    for x in (-4.5, -1.5, 1.5, 4.5):
        m.cube(body, (x - 0.5, 51.5, 2.5), (1, 4 + abs(x) / 2, 1), "spike", pivot=(x, 52, 3), rotation=(-20, 0, x * -4))

    # tendrils: the robe frays into swaying strands, two segments each
    for i, (x, z, ln) in enumerate(TENDRILS):
        t = m.bone(f"tendril_{i}", "body", (x, 17, z))
        m.cube(t, (x - 1, 17 - ln * 0.55, z - 0.5), (2, ln * 0.55, 1), "tendril")
        tip = m.bone(f"tendril_{i}_tip", f"tendril_{i}", (x, 17 - ln * 0.55, z))
        m.cube(tip, (x - 0.5, 17 - ln, z - 0.5), (1, ln * 0.45, 1), "tendril")

    head = m.bone("head", "body", (0, 52, -1))
    m.cube(head, (-4.5, 52, -5.5), (9, 10, 9), "helm")                  # faceless hood-helm
    m.cube(head, (-3.5, 51, -6), (7, 3, 1), "armor")                    # jaw guard
    for sx in (-1, 1):                                                  # swept-back horns
        m.cube(head, (sx * 4.5 - 0.75, 58, -2), (1.5, 7, 1.5), "spike", pivot=(sx * 4.5, 58, -1), rotation=(-30, 0, sx * -30))
        m.cube(head, (sx * 7.5 - 0.5, 63, 1), (1, 5, 1), "spike", pivot=(sx * 7.5, 63, 1.5), rotation=(-50, 0, sx * -10))
    for x, hgt in ((-0.5, 6), (-2.5, 4), (1.5, 4), (-4, 2), (3, 2)):     # jagged eclipse crown
        m.cube(head, (x, 62, -4.5), (1, hgt, 1), "crownspike")

    # the black sun: a lightless disc with a burning rim and jagged rays, hung behind the head
    corona = m.bone("corona", "body", (0, 60, 9))
    for i in range(3):
        m.cube(corona, (-7, 53, 8.5), (14, 14, 1), "void", pivot=(0, 60, 9), rotation=(0, 0, i * 30))
    for i in range(24):
        m.cube(corona, (-1.5, 60 + 10, 8.25), (3, 1.25, 1), "rim", pivot=(0, 60, 9), rotation=(0, 0, i * 15))
    for i in range(12):
        ln = 9 if i % 2 == 0 else 5 + (i % 3)
        m.cube(corona, (-0.5, 60 + 11.5, 8.6), (1, ln, 0.5), "ray", pivot=(0, 60, 9), rotation=(0, 0, i * 30 + 7))

    # cracked halo: three segments missing, two of them drifting loose
    halo = m.bone("halo", "head", (0, 69, -1), rotation=(16, 0, -10))
    for i in range(16):
        if i in (3, 4, 11):
            continue
        m.cube(halo, (-1.75, 69, -9), (3.5, 1.25, 1.25), "halo", pivot=(0, 69, -1), rotation=(0, i * 22.5, 0))
    for name, idx, off in (("halo_shard_a", 3.5, 2.5), ("halo_shard_b", 11, 3.0)):
        a = math.radians(idx * 22.5)
        cx, cz = -math.sin(a) * (8 + off), -1 - math.cos(a) * (8 + off)
        s = m.bone(name, "halo", (cx, 69, cz))
        m.cube(s, (cx - 1.75, 69, cz - 0.6), (3.5, 1.25, 1.25), "halo", pivot=(cx, 69.6, cz), rotation=(25, idx * 22.5, 15))

    # long arms: withered upper arm, gauntleted forearm, clawed hand
    for side, sx in SIDES:
        def at(x_left, width):
            return x_left if sx > 0 else -x_left - width
        arm = m.bone("arm_" + side, "body", (sx * 9.5, 48, 0))
        m.cube(arm, (at(8, 4), 36, -2), (4, 12, 4), "skin")
        m.cube(arm, (at(7.5, 5), 36, -2.5), (5, 1, 5), "trim")
        fore = m.bone("forearm_" + side, "arm_" + side, (sx * 10, 36.5, 0))
        m.cube(fore, (at(7.75, 4.5), 23, -2.25), (4.5, 14, 4.5), "armor")
        m.cube(fore, (at(7.25, 5.5), 30, -2.75), (5.5, 1, 5.5), "trim")
        m.cube(fore, (sx * 12 - 0.75, 32, -0.75), (1.5, 5, 1.5), "spike", pivot=(sx * 12, 33, 0), rotation=(0, 0, sx * -60))
        hand = m.bone("hand_" + side, "forearm_" + side, (sx * 10, 23, 0))
        m.cube(hand, (at(7.5, 5), 19, -2.5), (5, 4, 5), "skin")
        for k, dz in enumerate((-2, -0.6, 0.8)):
            fx = sx * (8 + 1.2 * k) if sx > 0 else sx * (8 + 1.2 * k) - 1
            m.cube(hand, (fx, 9.5, dz - 0.5), (1, 9.5, 1), "claw", pivot=(fx + 0.5, 19, dz), rotation=(-12, 0, sx * (4 - 4 * k)))
        m.cube(hand, (sx * 8 - 0.5 - (1 if sx < 0 else 0) + sx * -1, 12.5, -2.5), (1, 7, 1), "claw",
               pivot=(sx * 8, 19, -2), rotation=(-25, 0, sx * 20))

    # three pairs of tattered wings, swept back
    pairs = (("upper", 49, 38, 38), ("middle", 45, 31, 8), ("lower", 41, 23, -24))
    rag = (14, 9, 0, 12, 7, 16, 5, 11, 0, 13, 8, 15, 10)               # feather lengths, 0 = torn out
    for name, py, length, roll in pairs:
        for side, sx in SIDES:
            wing = m.bone(f"wing_{name}_{side}", "body", (sx * 3, py, 4.5), rotation=(0, sx * -14, sx * -roll))

            def span(start, ln):
                return sx * 3 + (start if sx > 0 else -start - ln)
            m.cube(wing, (span(0, length), py - 1.25, 4.5), (length, 2.5, 1.5), "wingbar")
            m.cube(wing, (span(length - 1, 2), py - 0.5, 4.75), (2, 5, 1), "spike")   # wing talon
            n = length // 3
            for k in range(n):
                ln = rag[(k * 3 + len(name) + (0 if sx > 0 else 5)) % len(rag)] * (0.55 + 0.45 * k / max(1, n - 1))
                if ln < 1:
                    continue
                ln = round(ln * (1.0 if name == "upper" else 0.85 if name == "middle" else 0.7), 1)
                m.cube(wing, (span(k * 3 + 0.25, 2.75), py - 1 - ln, 4.75 + (k % 2) * 0.3), (2.75, ln, 1), "feather")

    # umbral shards in two tilted, counter-rotating rings
    orbit = m.bone("orbit", "root", (0, 40, 0))
    for i in range(8):
        a = math.radians(i * 45 + 20)
        cx, cz = math.sin(a) * 22, math.cos(a) * 22
        m.cube(orbit, (cx - 1.25, 36.5, cz - 1.25), (2.5, 7, 2.5), "shard", pivot=(cx, 40, cz), rotation=(15, 45 + i * 45, 0))
        m.cube(orbit, (cx - 0.5, 43.5, cz - 0.5), (1, 2.5, 1), "shard", pivot=(cx, 40, cz), rotation=(15, 45 + i * 45, 0))
    inner = m.bone("orbit_inner", "root", (0, 47, 0), rotation=(12, 0, 8))
    for i in range(5):
        a = math.radians(i * 72)
        cx, cz = math.sin(a) * 16, math.cos(a) * 16
        m.cube(inner, (cx - 0.75, 45, cz - 0.75), (1.5, 4, 1.5), "shard", pivot=(cx, 47, cz), rotation=(0, 45, 20))
    return m


def build_pylon():
    m = Model("umbral_pylon", 64, 64)
    m.bone("root", None, (0, 0, 0))
    base = m.bone("base", "root", (0, 0, 0))
    m.cube(base, (-4, 0, -4), (8, 3, 8), "armor")
    m.cube(base, (-4.5, 3, -4.5), (9, 1, 9), "trim")
    m.cube(base, (-2.5, 4, -2.5), (5, 12, 5), "void")
    for i in range(4):
        m.cube(base, (-0.5, 4, -4), (1, 9, 1), "spike", pivot=(0, 4, 0), rotation=(0, 45 + i * 90, 0))
    m.cube(base, (-3, 15, -3), (6, 1, 6), "trim")
    crystal = m.bone("crystal", "root", (0, 23, 0))
    m.cube(crystal, (-2, 19, -2), (4, 8, 4), "shard", pivot=(0, 23, 0), rotation=(0, 45, 0))
    m.cube(crystal, (-1, 20.5, -1), (2, 5, 2), "core", pivot=(0, 23, 0), rotation=(0, 45, 0), inflate=0.3)
    ring = m.bone("ring", "root", (0, 23, 0), rotation=(15, 0, 0))
    for i in range(10):
        if i == 6:
            continue
        m.cube(ring, (-1.25, 22.5, -6.5), (2.5, 1, 1), "halo", pivot=(0, 23, 0), rotation=(0, i * 36, 0))
    return m


def geo(m, w, h, off):
    bones = []
    for b in m.bones:
        b = dict(b)
        if not b["cubes"]:
            del b["cubes"]
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + m.name, "texture_width": m.tw, "texture_height": m.th,
                        "visible_bounds_width": w, "visible_bounds_height": h, "visible_bounds_offset": [0, off, 0]},
        "bones": bones}]}


# ------------------------------------------------------------------ animation helpers (pose-to-pose)

NEUTRAL = {"rotation": [0, 0, 0], "position": [0, 0, 0], "scale": [1, 1, 1]}
CHANNELS = {"r": "rotation", "p": "position", "s": "scale"}


def pose(*parts, **bones):
    """Merges dicts from helper functions and keyword bones. Bone value: (x,y,z) rotation or {"r","p","s"}."""
    out = {}
    for part in parts + (bones,):
        for b, v in part.items():
            if isinstance(v, (list, tuple)):
                v = {"r": list(v)}
            out.setdefault(b, {}).update(v)
    return out


def arms(rx=0, ry=0, out=0, fx=0, hx=0, hz=0, right=None, left=None):
    """Symmetric arm pose: rx forward(-)/back(+), out = abduction, fx elbow bend (- = curl forward)."""
    p = {}
    for side, sgn in (() if right or left else (("right", 1), ("left", -1))):
        p["arm_" + side] = {"r": [rx, ry * sgn, out * sgn]}
        p["forearm_" + side] = {"r": [fx, 0, 0]}
        p["hand_" + side] = {"r": [hx, 0, hz * sgn]}
    for side, o in (("right", right), ("left", left)):
        if o:
            for k, v in o.items():
                p[k + "_" + side] = {"r": list(v)}
    return p


def wings(ry=0, rz=0, only=None, rx=0):
    """Symmetric wing pose: ry > 0 sweeps back, rz > 0 raises."""
    p = {}
    for pair in ("upper", "middle", "lower"):
        if only and pair not in only:
            continue
        for side, sgn in (("right", 1), ("left", -1)):
            p[f"wing_{pair}_{side}"] = {"r": [rx, ry * sgn, rz * sgn]}
    return p


def anim(length, keys, loop=False, molang=None):
    """keys: [(time_seconds, pose)]. Every channel used anywhere is keyed at every time (neutral where absent)."""
    used = {}
    for _, p in keys:
        for b, chans in p.items():
            for c in chans:
                used.setdefault(b, set()).add(CHANNELS[c])
    bones = {}
    for b, chans in used.items():
        bones[b] = {}
        for ch in sorted(chans):
            short = {v: k for k, v in CHANNELS.items()}[ch]
            track = {}
            for t, p in keys:
                track[fmt(t)] = [round(float(x), 3) for x in p.get(b, {}).get(short, NEUTRAL[ch])]
            bones[b][ch] = track
    for b, chans in (molang or {}).items():
        bones.setdefault(b, {}).update(chans)
    a = {"animation_length": round(length, 3), "bones": bones}
    if loop:
        a["loop"] = True
    return a


def fmt(t):
    return str(round(t, 3))


def ticks(n):
    return round(n / 20.0, 3)


def tendril_sway(speed=90, amp=8, extra=0):
    m = {}
    for i in range(len(TENDRILS)):
        ph = i * 51
        m[f"tendril_{i}"] = {"rotation": [f"math.sin(query.anim_time * {speed} + {ph}) * {amp + extra}", "0",
                                          f"math.cos(query.anim_time * {speed * 0.7:.0f} + {ph}) * {amp * 0.6:.1f}"]}
        m[f"tendril_{i}_tip"] = {"rotation": [f"math.sin(query.anim_time * {speed} + {ph + 60}) * {amp * 1.4:.1f}", "0", "0"]}
    return m


def idle_molang(speed=1.0, wing_amp=6, bob=1.5, corona=15, halo=30, orbit=-40, jitter=0):
    s = speed
    j = f" + math.sin(query.anim_time * 1440) * {jitter}" if jitter else ""
    m = {
        "root": {"position": ["0", f"2 + math.sin(query.anim_time * {90 * s:.0f}) * {bob}", "0"]},
        "body": {"rotation": [f"6 + math.sin(query.anim_time * {45 * s:.0f}) * 2", "0", "0"]},
        "head": {"rotation": [f"8 + math.sin(query.anim_time * {90 * s:.0f} + 30) * 3{j}", f"math.sin(query.anim_time * {30 * s:.0f}) * 10", "0"]},
        "corona": {"rotation": ["0", "0", f"query.anim_time * {corona}"]},
        "halo": {"rotation": ["0", f"query.anim_time * {halo}", "0"]},
        "halo_shard_a": {"position": ["0", f"math.sin(query.anim_time * 120) * 1.2", f"math.cos(query.anim_time * 80) * 0.8"]},
        "halo_shard_b": {"position": ["0", f"math.sin(query.anim_time * 100 + 90) * 1.0", "0"],
                         "rotation": ["0", "0", f"math.sin(query.anim_time * 60) * 12"]},
        "orbit": {"rotation": ["0", f"query.anim_time * {orbit}", "0"],
                  "position": ["0", f"math.sin(query.anim_time * 90) * 1.0", "0"]},
        "orbit_inner": {"rotation": ["0", f"query.anim_time * {-orbit * 1.6:.0f}", "0"]},
        "arm_right": {"rotation": [f"-6 + math.sin(query.anim_time * {90 * s:.0f}) * 4", "0", "10"]},
        "arm_left": {"rotation": [f"-6 + math.sin(query.anim_time * {90 * s:.0f} + 90) * 4", "0", "-10"]},
        "forearm_right": {"rotation": [f"-18 + math.sin(query.anim_time * {90 * s:.0f}) * 4", "0", "0"]},
        "forearm_left": {"rotation": [f"-18 + math.sin(query.anim_time * {90 * s:.0f} + 90) * 4", "0", "0"]},
        "hand_right": {"rotation": [f"-10 + math.sin(query.anim_time * {180 * s:.0f}) * 6", "0", "0"]},
        "hand_left": {"rotation": [f"-10 + math.sin(query.anim_time * {180 * s:.0f} + 50) * 6", "0", "0"]},
    }
    for pair, amp, ph in (("upper", wing_amp, 0), ("middle", wing_amp * 0.75, 50), ("lower", wing_amp * 0.6, 100)):
        for side, sgn in (("right", 1), ("left", -1)):
            m[f"wing_{pair}_{side}"] = {"rotation": [
                "0", f"{sgn} * (math.sin(query.anim_time * {80 * s:.0f} + {ph}) * {amp})",
                f"{sgn} * math.sin(query.anim_time * {80 * s:.0f} + {ph}) * {amp * 0.7:.1f}{j}"]}
    m.update(tendril_sway(90 * s, 8))
    return m


def stage(name, n_ticks, keys):
    return name, anim(ticks(n_ticks), [(ticks(n_ticks) * f, p) for f, p in keys])


def animations():
    A = {}
    A["idle"] = {"loop": True, "animation_length": 8, "bones": idle_molang()}
    A["idle_total"] = {"loop": True, "animation_length": 8, "bones": idle_molang(1.3, 8, 2, 40, 70, -70)}
    A["idle_enraged"] = {"loop": True, "animation_length": 8, "bones": idle_molang(1.9, 10, 2.5, 90, 140, -120, jitter=1.5)}
    shielded = idle_molang(0.8, 3, 1, 10, 20, -90)
    # cocooned in its wings, arms crossed over the core
    for pair, wy, wz in (("upper", -55, 10), ("middle", -60, 0), ("lower", -50, -8)):
        for side, sgn in (("right", 1), ("left", -1)):
            shielded[f"wing_{pair}_{side}"] = {"rotation": ["0", f"{sgn * wy} + math.sin(query.anim_time * 60) * 3", str(sgn * wz)]}
    shielded["arm_right"] = {"rotation": ["-60", "-30", "-10"]}
    shielded["arm_left"] = {"rotation": ["-60", "30", "10"]}
    shielded["forearm_right"] = {"rotation": ["-70", "0", "0"]}
    shielded["forearm_left"] = {"rotation": ["-70", "0", "0"]}
    shielded["head"] = {"rotation": ["25", "0", "0"]}
    A["idle_shielded"] = {"loop": True, "animation_length": 8, "bones": shielded}

    base = pose({"root": {"p": [0, 2, 0]}, "body": (6, 0, 0), "head": (8, 0, 0)},
                arms(-6, 0, 10, -18, -10))

    def P(*parts, **kw):
        return pose(base, *parts, **kw)

    w, a, r = MOVES["sweep"]
    coil = P(arms(right={"arm": (-40, -30, 80), "forearm": (-30, 0, 0), "hand": (-20, 0, 0)}), wings(-25, 20, ("upper", "middle")),
             body=(0, 35, 0), head=(5, -20, 0))
    slash = P(arms(right={"arm": (-80, 60, -10), "forearm": (-10, 0, 0), "hand": (10, 0, 0)}), wings(-55, 0, ("upper", "middle")),
              body=(12, -45, 0), head=(10, 25, 0))
    A.update([stage("sweep_windup", w, [(0, base), (0.7, coil), (1, P(coil, body=(0, 40, 0)))]),
              stage("sweep_strike", a, [(0, P(coil, body=(0, 40, 0))), (0.5, slash), (1, slash)]),
              stage("sweep_recover", r, [(0, slash), (0.4, slash), (1, base)])])

    w, a, r = MOVES["lance"]
    aim = P(arms(left={"arm": (-95, -10, 0), "forearm": (-5, 0, 0), "hand": (0, 0, 0)}), wings(15, 25),
            body=(-4, -15, 0), head=(0, 15, 0), corona={"s": [1.25, 1.25, 1.25]}, orbit_inner={"s": [0.6, 0.6, 0.6]})
    fire = P(aim, body=(-12, -15, 0), arm_left=(-110, -10, 0), corona={"s": [1.4, 1.4, 1.4]})
    A.update([stage("lance_windup", w, [(0, base), (0.4, aim), (0.75, P(aim, corona={"s": [1.35, 1.35, 1.35]})), (1, aim)]),
              stage("lance_strike", a, [(0, aim), (0.5, fire), (1, fire)]),
              stage("lance_recover", r, [(0, fire), (1, base)])])

    w, a, r = MOVES["starfall"]
    up = P(arms(-170, 0, 25, 0, 0), wings(-10, 35), body=(-10, 0, 0), head=(-35, 0, 0), orbit={"s": [1.3, 1, 1.3]})
    down = P(arms(-40, 0, 70, -10, 30), wings(10, -10), body=(15, 0, 0), head=(20, 0, 0))
    A.update([stage("starfall_windup", w, [(0, base), (1, up)]),
              stage("starfall_strike", a, [(0, up), (0.35, down), (1, down)]),
              stage("starfall_recover", r, [(0, down), (1, base)])])

    w, a, r = MOVES["summon"]
    spread = P(arms(-20, 0, 75, -10, 20), wings(-30, 30), body=(-12, 0, 0), head=(-20, 0, 0),
               orbit={"s": [1.6, 1.2, 1.6]}, orbit_inner={"s": [1.5, 1.5, 1.5]})
    flare = P(spread, wings(-50, 45), body=(-16, 0, 0))
    A.update([stage("summon_windup", w, [(0, base), (1, spread)]),
              stage("summon_strike", a, [(0, spread), (0.5, flare), (1, flare)]),
              stage("summon_recover", r, [(0, flare), (1, base)])])

    w, a, r = MOVES["dive"]
    rise = P(arms(-10, 0, 20, -60, -20), wings(-20, 55), body=(20, 0, 0), head=(35, 0, 0))
    pump = P(rise, wings(10, 0))
    plunge = P(arms(-150, 0, 15, -10, -30), wings(60, 10), body=(55, 0, 0), head=(-10, 0, 0))
    impact = P(arms(-60, 0, 60, -20, -40), wings(40, -30), body=(35, 0, 0), head=(20, 0, 0), root={"p": [0, -6, 0]})
    A.update([stage("dive_windup", w, [(0, base), (0.25, rise), (0.45, pump), (0.65, rise), (0.85, pump), (1, rise)]),
              stage("dive_strike", a, [(0, rise), (0.5, plunge), (1, plunge)]),
              stage("dive_recover", r, [(0, impact), (0.6, impact), (1, base)])])

    w, a, r = MOVES["beam"]
    charge = P(arms(-60, -30, 20, -60, 0), wings(-15, 40), body=(25, 0, 0), head=(30, 0, 0), root={"p": [0, -2, 0]},
               corona={"s": [1.5, 1.5, 1.5]}, orbit={"s": [0.7, 1, 0.7]})
    fire = P(arms(-75, 0, 55, -5, 40), wings(-35, 20), body=(35, 0, 0), head=(25, 0, 0), root={"p": [0, -4, 0]},
             corona={"s": [1.7, 1.7, 1.7]}, orbit={"s": [1.4, 1, 1.4]})
    slump = P(arms(10, 0, 5, 0, 10), wings(25, -35), body=(30, 0, 0), head=(45, 0, 0), root={"p": [0, -5, 0]})
    A.update([stage("beam_windup", w, [(0, base), (0.7, charge), (1, P(charge, corona={"s": [1.6, 1.6, 1.6]}))])])
    A["beam_strike"] = anim(1.0, [(0, fire), (0.5, P(fire, corona={"s": [1.85, 1.85, 1.85]})), (1.0, fire)], loop=True,
                            molang={"corona": {"rotation": ["0", "0", "query.anim_time * 360"]}})
    A.update([stage("beam_recover", r, [(0, fire), (0.3, slump), (0.75, slump), (1, base)])])

    w, a, r = MOVES["nova"]
    curl = P(arms(-70, -40, -15, -80, -20), wings(-70, 0), body=(35, 0, 0), head=(40, 0, 0),
             corona={"s": [1.6, 1.6, 1.6]}, orbit={"s": [0.4, 0.6, 0.4]}, orbit_inner={"s": [0.4, 0.4, 0.4]})
    tremble = P(curl, body=(38, 0, 3), corona={"s": [1.75, 1.75, 1.75]})
    burst = P(arms(-30, 0, 95, 0, 40), wings(-10, 60), body=(-20, 0, 0), head=(-40, 0, 0),
              corona={"s": [2.2, 2.2, 2.2]}, orbit={"s": [1.8, 1.2, 1.8]}, orbit_inner={"s": [1.8, 1.8, 1.8]})
    A.update([stage("nova_windup", w, [(0, base), (0.3, curl), (0.45, tremble), (0.6, curl), (0.75, tremble),
                                        (0.9, curl), (1, tremble)]),
              stage("nova_strike", a, [(0, tremble), (0.5, burst), (1, burst)]),
              stage("nova_recover", r, [(0, burst), (0.3, burst), (1, base)])])

    w, a, r = MOVES["blink"]
    gone = P(wings(-60, 0), arms(-40, -30, -10, -60), root={"p": [0, 6, 0], "s": [0.15, 1.6, 0.15]})
    A.update([stage("blink_windup", w, [(0, base), (1, gone)]),
              stage("blink_strike", a, [(0, gone), (1, gone)]),
              stage("blink_recover", r, [(0, gone), (0.6, P(base, root={"p": [0, 2, 0], "s": [1.1, 0.95, 1.1]})), (1, base)])])

    w, a, r = MOVES["grasp"]
    reach = P(arms(right={"arm": (-100, 15, 0), "forearm": (0, 0, 0), "hand": (-35, 0, 0)}), wings(-20, 30),
              body=(15, -10, 0), head=(10, -5, 0))
    yank = P(arms(right={"arm": (-30, 30, 40), "forearm": (-90, 0, 0), "hand": (40, 0, 0)}), wings(10, 10),
             body=(-10, 20, 0), head=(-5, 15, 0))
    A.update([stage("grasp_windup", w, [(0, base), (0.6, reach), (0.8, P(reach, hand_right=(-50, 0, 0))), (1, reach)]),
              stage("grasp_strike", a, [(0, reach), (0.6, yank), (1, yank)]),
              stage("grasp_recover", r, [(0, yank), (1, base)])])

    # -------------------------------------------------- specials
    hidden = P(root={"p": [0, 40, 0], "s": [0.05, 0.05, 0.05]}, corona={"s": [0.1, 0.1, 0.1]})
    unfurl = P(wings(-40, 60), arms(-40, 0, 100, 0, 30), body=(-15, 0, 0), head=(-40, 0, 0), corona={"s": [1.8, 1.8, 1.8]},
               orbit={"s": [2, 1, 2]})
    huddle = P(wings(-65, -10), arms(-60, -40, -15, -80), body=(40, 0, 0), head=(45, 0, 0), root={"p": [0, 8, 0], "s": [0.7, 0.7, 0.7]})
    A.update([stage("entrance", SPECIALS["entrance"], [(0, hidden), (0.35, huddle), (0.6, P(huddle, root={"p": [0, 4, 0]})),
                                                       (0.8, unfurl), (0.92, unfurl), (1, base)])])

    def transition(intensity):
        shake = P(arms(-20, 0, 40, -30, 20), wings(-30, 45), body=(-25, 0, 0), head=(-50, 0, 0),
                  halo_shard_a={"p": [6 * intensity, 4, -4]}, halo_shard_b={"p": [-6 * intensity, 6, 3]},
                  corona={"s": [1 + 0.4 * intensity] * 3}, orbit={"s": [1.6, 1.3, 1.6]})
        shake2 = P(shake, wings(-25, 55), body=(-22, 0, 4), head=(-45, 8, 0))
        crouch = P(huddle, root={"p": [0, 0, 0]})
        end = P(base, corona={"s": [1 + 0.15 * (intensity - 1)] * 3})
        return [(0, base), (0.15, crouch), (0.3, crouch), (0.45, shake), (0.55, shake2), (0.65, shake), (0.75, shake2),
                (0.85, shake), (1, end)]
    A.update([stage("phase2", SPECIALS["phase2"], transition(1)),
              stage("phase3", SPECIALS["phase3"], transition(2))])
    raise_ = P(arms(-150, 0, 40, -10, 30), wings(-10, 50), head=(-30, 0, 0), body=(-8, 0, 0), orbit_inner={"s": [1.8, 1.8, 1.8]})
    A.update([stage("shield_raise", SPECIALS["shield_raise"], [(0, base), (0.5, raise_), (1, P(raise_, arms(-60, -30, -10, -70)))])])
    roar = P(arms(-30, 0, 85, -20, 40), wings(-35, 60), body=(-20, 0, 0), head=(-55, 0, 0), corona={"s": [2, 2, 2]})
    roar2 = P(roar, wings(-30, 50), head=(-50, 6, 0), body=(-18, 0, 3))
    A.update([stage("enrage", SPECIALS["enrage"], [(0, base), (0.2, roar), (0.3, roar2), (0.4, roar), (0.5, roar2),
                                                   (0.6, roar), (0.7, roar2), (0.8, roar), (1, base)])])
    reel = P(arms(20, 0, 30, -10, 10), wings(30, 30), body=(-30, 0, 0), head=(-40, 20, 0))
    kneel = P(arms(-15, 0, 25, -10, 20), wings(35, -40), body=(45, 0, 0), head=(50, 0, 0), root={"p": [0, -10, 0]},
              corona={"s": [0.8, 0.8, 0.8]}, orbit={"s": [0.6, 0.6, 0.6], "p": [0, -10, 0]})
    twitch = P(kneel, wings(35, -35, ("upper",)), head=(45, -12, 0))
    A.update([stage("stagger", SPECIALS["stagger"], [(0, base), (0.08, reel), (0.2, kneel), (0.45, twitch), (0.55, kneel),
                                                     (0.75, twitch), (0.88, kneel), (1, base)])])
    A["grounded"] = anim(2.0, [(0, P(kneel, root={"p": [0, -6, 0]})), (1.0, P(twitch, root={"p": [0, -6, 0]})),
                               (2.0, P(kneel, root={"p": [0, -6, 0]}))], loop=True)

    # death: convulses, the halo falls and splits, wings crumble, it sinks and disintegrates into nothing
    d0 = P(reel)
    d1 = P(arms(-10, 0, 50, -20, 30), wings(-40, 60), body=(-35, 0, 0), head=(-60, 0, 0), corona={"s": [2.2, 2.2, 2.2]},
           halo={"p": [0, 4, 0]}, halo_shard_a={"p": [10, 8, -6]}, halo_shard_b={"p": [-10, 10, 6]})
    d2 = P(kneel, halo={"p": [0, -30, 0], "r": [70, 0, 30]}, halo_shard_a={"p": [14, -40, -8]}, halo_shard_b={"p": [-12, -42, 9]},
           corona={"s": [0.5, 0.5, 0.5]}, **{k: {"r": v["r"], "s": [0.6, 0.6, 0.6]} for k, v in wings(50, -60).items()})
    d3 = P(d2, root={"p": [0, -16, 0], "s": [0.6, 0.15, 0.6]}, corona={"s": [0, 0, 0]}, orbit={"s": [0, 0, 0]},
           orbit_inner={"s": [0, 0, 0]}, **{k: {"r": v["r"], "s": [0, 0, 0]} for k, v in wings(60, -70).items()})
    A.update([stage("death", DEATH_TICKS, [(0, base), (0.08, d0), (0.25, d1), (0.32, P(d1, body=(-30, 8, 4))), (0.4, d1),
                                             (0.6, d2), (0.85, P(d2, root={"p": [0, -12, 0], "s": [0.9, 0.6, 0.9]})), (1, d3)])])
    return {"format_version": "1.8.0", "animations": A, "geckolib_format_version": 2}


def pylon_animations():
    idle = {"loop": True, "animation_length": 4, "bones": {
        "crystal": {"rotation": ["0", "query.anim_time * 90", "0"], "position": ["0", "math.sin(query.anim_time * 90) * 1.2", "0"]},
        "ring": {"rotation": ["0", "query.anim_time * -60", "0"]}}}
    return {"format_version": "1.8.0", "animations": {"idle": idle}, "geckolib_format_version": 2}


# ------------------------------------------------------------------ textures + output

def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def textures(m):
    colours = {k: png.hexc(c) for k, c in PALETTE.items()}
    base, glow = png.blank(m.tw, m.th), png.blank(m.tw, m.th)
    for u, v, w, h, d, material in m.layout:
        paint = MATERIALS[material]
        for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    b, g = paint(face, x, y, fw, fh)
                    base[fy + y][fx + x] = colours[b]
                    if g:
                        glow[fy + y][fx + x] = colours[g]
    out = os.path.join(ASSETS, "textures", "entity", "eclipse")
    os.makedirs(out, exist_ok=True)
    for name, img in ((m.name, base), (m.name + "_glowmask", glow)):
        n = len(png.colours(img))
        assert n <= 8, f"{name}: {n} colours"
        png.write(os.path.join(out, name + ".png"), img)


def write_json(rel_dirs, name, data):
    for d in rel_dirs:
        path = os.path.join(ASSETS, *d.split("/"), name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=1)
            f.write("\n")


def main():
    geo_dirs, anim_dirs = ("geo/entity", "geckolib/models/entity"), ("animations/entity", "geckolib/animations/entity")
    e = build_eclipse()
    write_json(geo_dirs, "eclipse.geo.json", geo(e, 9, 8, 3))
    write_json(anim_dirs, "eclipse.animation.json", animations())
    textures(e)
    p = build_pylon()
    write_json(geo_dirs, "umbral_pylon.geo.json", geo(p, 2, 2.5, 1))
    write_json(anim_dirs, "umbral_pylon.animation.json", pylon_animations())
    textures(p)
    for m in (e, p):
        print(m.name + ":", sum(len(b["cubes"]) for b in m.bones), "cubes,", len(m.bones), "bones,",
              len(m.layout), "uv boxes, cursor", m.cursor)


if __name__ == "__main__":
    main()
