"""Generates the Eclipse boss, its Umbral Pylon and the Eclipse Echo: GeckoLib models, animations, textures and
emissive glowmasks. Follows docs/ART_GUIDE.md.

Run: python -I tools/dev/gen_eclipse.py            (writes the assets)
     python -I tools/dev/eclipse_preview.py <dir>   (software preview of key poses, front + side)
Outputs (both GeckoLib layouts):
  assets/astralvigil/{geo,geckolib/models}/entity/{eclipse,umbral_pylon,eclipse_echo}.geo.json
  assets/astralvigil/{animations,geckolib/animations}/entity/{eclipse,umbral_pylon,eclipse_echo}.animation.json
  assets/astralvigil/textures/entity/eclipse/{eclipse,umbral_pylon,eclipse_echo}{,_glowmask}.png
  assets/astralvigil/textures/entity/eclipse_echo.png (the Echo's base texture, default GeckoLib path)

Design: the fallen fifth Watcher - a hunched, towering, corrupted seraph built on scaled-up player proportions
(head 9, broad 16-wide chest tapering to a 12-wide waist, arms of shoulder + 9 upper arm + 9 forearm + 6-wide hand with
3-px fingers and 1-px claws). Two silhouette ideas: the black-sun corona burning behind its head and two pairs of
tattered, jointed wings. A cracked halo, a void core, a robe fraying into jointed tendrils and two rings of umbral
shards support them. Every attack summons its own weapon (hidden at scale 0 otherwise): Umbral Scythe (sweep),
Void Lance (lance), Starfall Scepter (starfall), Echo Orb (summon), twin Eclipse Claws (dive), the Eclipse Focus
disc (ray and nova) and the Umbral Chain (grasp).

Animations are pose-to-pose with GeckoLib keyframe easing; secondary chains (wings, tendrils, corona rays, halo,
shards) lag the primary motion and overshoot (easeOutBack). Ambient motion uses query.life_time so it never snaps when
the controller switches clips. Easing types used: see EASINGS_USED (printed on run).

Move timings (ticks) MUST match media.hofferbert.astralvigil.eclipse.EclipseMove, weapon timings MUST match
EclipseWeapon - JUnit tests compare them with the animation file.
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

# Ramp, dark -> light. K abyss (AO), D void navy (outline), R umbral plum, V eclipse violet, N halo gold dark,
# M halo gold, G glow violet, W glow white. Cool navy shadows, warm gold / pale violet highlights.
PALETTE = {"K": "0D0A17", "D": "1E1A2E", "R": "35294F", "V": "5B2A86", "N": "7A5A12", "M": "C9971E",
           "G": "B57CFF", "W": "F2E6FF"}

# name: (windup, active, recovery) in ticks - keep in sync with EclipseMove.java
MOVES = {
    "sweep": (18, 6, 16), "lance": (26, 4, 14), "starfall": (16, 10, 14), "summon": (20, 4, 16),
    "dive": (30, 8, 24), "beam": (30, 100, 30), "nova": (50, 4, 30), "blink": (8, 2, 8), "grasp": (24, 6, 16),
}
SPECIALS = {"entrance": 70, "phase2": 60, "phase3": 60, "shield_raise": 30, "enrage": 40, "stagger": 80,
            "grounded": 60}
DEATH_TICKS = 100

# move: (weapon, appear (stage, tick), vanish (stage, tick), dissolve ticks) - keep in sync with EclipseWeapon.java.
# At "appear" a spark flares at the hand and the weapon grows 0 -> 1.15 -> 1 over FORM ticks; at "vanish" it
# dissolves (-> 0) over the dissolve ticks.
WEAPON_TIMING = {
    "sweep": ("scythe", ("windup", 2), ("recover", 6), 5),
    "lance": ("lance", ("windup", 2), ("strike", 1), 2),
    "starfall": ("scepter", ("windup", 1), ("recover", 6), 5),
    "summon": ("orb", ("windup", 2), ("recover", 4), 5),
    "dive": ("claws", ("windup", 4), ("recover", 12), 6),
    "beam": ("focus", ("windup", 3), ("recover", 10), 6),
    "nova": ("focus", ("windup", 4), ("recover", 2), 4),
    "grasp": ("chain", ("windup", 3), ("recover", 6), 5),
}
FORM = 6
WEAPON_BONES = {"scythe": ["weapon_scythe"], "lance": ["weapon_lance"], "scepter": ["weapon_scepter"],
                "orb": ["weapon_orb"], "claws": ["weapon_claws_right", "weapon_claws_left"],
                "focus": ["weapon_focus"], "chain": ["weapon_chain"]}
WEAPON_SPARKS = {"scythe": ["spark_right"], "lance": ["spark_left"], "scepter": ["spark_right"], "orb": ["spark_left"],
                 "claws": ["spark_right", "spark_left"], "focus": ["spark_core"], "chain": ["spark_right"]}
ALL_HIDDEN = sorted({b for v in WEAPON_BONES.values() for b in v} | {b for v in WEAPON_SPARKS.values() for b in v})


# ------------------------------------------------------------------ texturing: 4-tone ramps, AO, rim, top-left light

def h31(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    return ((n >> 3) ^ (n >> 11)) & 0xFFFF


# light from the top-left of the viewer: the first box-UV strip ("east") is the model's right = viewer's left
FACE_BIAS = {"top": 0.9, "east": 0.35, "front": 0.0, "west": -0.45, "back": -0.55, "bottom": -1.1}


class Mat:
    """ramp: letters dark -> light; base: ramp level of a front face; grad: top->bottom gradient; noise: dither;
    edge: darken silhouette borders; extra(face, x, y, w, h, level) -> (letter|None|'-', glow) override or None."""

    def __init__(self, ramp, base, grad=0.6, noise=0.5, edge=True, rim=True, extra=None, glow=()):
        self.ramp, self.base, self.grad, self.noise, self.edge, self.rim = ramp, base, grad, noise, edge, rim
        self.extra, self.glow = extra, set(glow)

    def __call__(self, face, x, y, w, h, seed):
        lvl = self.base + FACE_BIAS[face]
        if face not in ("top", "bottom") and h > 1:
            lvl += self.grad * (0.5 - y / (h - 1))
            if self.rim and y == 0 and h >= 3:
                lvl += 0.6                     # rim light on the top edge
            if y == h - 1 and h >= 3:
                lvl -= 0.7                     # ambient occlusion under the form
        if self.edge and face != "top" and w >= 4 and (x == 0 or x == w - 1):
            lvl -= 0.6
        lvl += ((h31(x, y, seed) % 97) / 96.0 - 0.5) * self.noise
        out = None
        if self.extra:
            out = self.extra(face, x, y, w, h, lvl)
        if out is None:
            letter = self.ramp[max(0, min(len(self.ramp) - 1, int(round(lvl))))]
            return letter, letter in self.glow
        return out


def x_cloth(face, x, y, w, h, lvl):
    if face in ("front", "back", "east", "west") and h >= 4:
        if y == h - 1 and (x * 5 + h) % 3 != 0:
            return None, False                 # ragged hem (cut out)
        if y >= h - 3 and (x * 7 + y) % 5 == 0:
            return None, False
        if x % 4 == 1 and y < h - 2:
            return "D", False                  # deep folds
    return None


def x_carapace(face, x, y, w, h, lvl):
    if face == "front" and w >= 8 and 1 <= y < h - 1:
        c = w // 2
        if y in (2, 3) and abs(x - c + 0.5) in (5.5, 6.5) or (abs(x - c + 0.5) == 4.5 and y == 4):
            return "G", True                   # corruption veins running out of the core
    return None


def x_helm(face, x, y, w, h, lvl):
    if face == "front":
        c = w // 2
        if y == h // 2 and 1 <= x < w - 1 and x != c:
            return ("W" if x in (c - 2, c + 2) else "G"), True        # slit eyes, white-hot pupils
        if y == h // 2 + 1 and x in (c - 2, c + 2):
            return "G", True                   # tears of light
        if x == c and y >= h // 2 + 1 and y < h - 1:
            return "V", False                  # crack down the mask
        if y == 0 or y == 1:
            return ("R" if x % 2 else "V"), False   # hood brow
        if x == 0 or x == w - 1:
            return "K", False
        return ("D" if (x + y) % 5 else "R"), False
    return None


def x_feather(face, x, y, w, h, lvl):
    if face in ("front", "back"):
        if y >= h - 2 and (x + h) % 2 == 0:
            return None, False                 # tattered tips (cut out)
        if y == h - 3 and x == 1 and h > 6:
            return "G", True                   # embers along the torn edge
        if x == w // 2 and y < h - 2:
            return "D", False                  # quill
    if face in ("east", "west") and y >= h - 2:
        return None, False
    return None


def x_core(face, x, y, w, h, lvl):
    c = "W" if (0 < x < w - 1 and 0 < y < h - 1) or w <= 2 or h <= 2 else "G"
    return c, True


def x_glowedge(face, x, y, w, h, lvl):
    """Weapon edges: hot white leading edge, violet body."""
    if face in ("front", "back", "east", "west"):
        if x == 0 or y == h - 1:
            return "W", True
        return "G", True
    return "G", True


def x_ray(face, x, y, w, h, lvl):
    if face in ("top", "bottom"):
        return "G", True
    if y >= h - 2:
        return "W", True
    if y < h // 3:
        return "V", False
    return ("G" if y % 2 == 0 else "V"), y % 2 == 0


def x_rim(face, x, y, w, h, lvl):
    return ("W" if (x + y) % 3 == 0 else "G"), True


def x_void(face, x, y, w, h, lvl):
    if face == "back" and (x == 0 or y == 0 or x == w - 1 or y == h - 1):
        return "D", False
    return ("D" if h31(x, y) % 11 == 0 else "K"), False


def x_spike(face, x, y, w, h, lvl):
    if y <= 0 and face != "bottom":
        return "V", False
    return None


def x_crown(face, x, y, w, h, lvl):
    if y <= 1 or face == "top":
        return ("W" if y == 0 else "G"), True
    return None


def x_shard(face, x, y, w, h, lvl):
    if face in ("top", "bottom") or y == 0:
        return "G", True                       # burning tip
    if x == 0 and face in ("front", "east"):
        return "V", False                      # glint on the leading edge
    return None


def x_tendril(face, x, y, w, h, lvl):
    if y == h - 1 and h <= 4:
        return "G", True
    return None


def x_halo(face, x, y, w, h, lvl):
    if face == "top" and x == w // 2:
        return "W", False
    return None


def x_claw(face, x, y, w, h, lvl):
    return ("W", True) if y >= h - 1 else ("G", True)


MATERIALS = {
    "robe": Mat("KDRV", 2.0, grad=0.9, noise=0.6, extra=x_cloth),
    "cloth": Mat("KDRV", 2.1, grad=0.8, noise=0.6),                 # wraps, stole, inner robe (no cut-outs)
    "plate": Mat("KDRV", 1.4, grad=0.8, noise=0.3),                 # dark carapace armour
    "carapace": Mat("KDRV", 1.6, grad=0.9, noise=0.35, extra=x_carapace),
    "skin": Mat("DRV", 1.0, grad=0.6, noise=0.5),                   # withered ash-violet skin
    "trim": Mat("DNMW", 1.5, grad=0.8, noise=0.3),                  # tarnished halo gold
    "halo": Mat("NMW", 1.2, grad=0.8, noise=0.4, edge=False, extra=x_halo),
    "void": Mat("KD", 0.0, extra=x_void, edge=False),
    "core": Mat("GW", 1.0, extra=x_core),
    "rim": Mat("GW", 1.0, extra=x_rim),
    "ray": Mat("VGW", 1.0, extra=x_ray),
    "helm": Mat("KDRV", 1.5, extra=x_helm),
    "spike": Mat("KDR", 1.3, grad=1.2, noise=0.3, edge=False, extra=x_spike),
    "crown": Mat("KDR", 1.3, edge=False, extra=x_crown),
    "claw": Mat("GW", 1.0, extra=x_claw),
    "wingbar": Mat("KDRV", 1.5, grad=0.8, noise=0.3),
    "feather": Mat("DRV", 1.4, grad=1.4, noise=0.45, edge=False, extra=x_feather),
    "shard": Mat("KDRV", 1.2, noise=0.6, edge=False, extra=x_shard),
    "tendril": Mat("KDR", 1.6, grad=1.0, noise=0.6, edge=False, extra=x_tendril),
    "metal": Mat("KDRV", 1.8, grad=0.8, noise=0.4),                 # weapon steel (umbral)
    "edge": Mat("GW", 1.0, extra=x_glowedge),                       # weapon cutting edges (emissive)
}


# ------------------------------------------------------------------ model building

class Model:
    def __init__(self, name, tw, th):
        self.name, self.tw, self.th = name, tw, th
        self.bones = []
        self.by_name = {}
        self.uv = {}
        self.cursor = [0, 0, 0]
        self.layout = []  # (u, v, w, h, d, material)

    def bone(self, name, parent, pivot, rotation=None):
        assert name not in self.by_name, name
        b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
        if parent:
            assert parent in self.by_name, parent
            b["parent"] = parent
        if rotation and any(rotation):
            b["rotation"] = [round(r, 3) for r in rotation]
        self.bones.append(b)
        self.by_name[name] = b
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
        for s in size:
            assert s >= 0.5 and (s == 0.5 or abs(s - round(s)) < 1e-6), f"{self.name}: size {size} not on the texel grid"
        w, h, d = size
        u, v = self.alloc(int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d)), material)
        c = {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": [u, v]}
        if rotation and any(rotation):
            c["pivot"] = [round(p, 3) for p in pivot]
            c["rotation"] = [round(r, 3) for r in rotation]
        if inflate:
            assert inflate <= 0.25
            c["inflate"] = inflate
        c["_mat"] = material
        bone["cubes"].append(c)

    def beam(self, bone, a, b, cross, material, plane):
        """A cube of square-ish cross-section `cross` (w, d) spanning the points a -> b.
        plane 'yz': a, b = (y, z) at x = cross[2]; 'xy': a, b = (x, y) at z = cross[2]."""
        if plane == "yz":
            (y0, z0), (y1, z1) = a, b
            ln = round(math.hypot(y1 - y0, z1 - z0))
            cy, cz, x = (y0 + y1) / 2, (z0 + z1) / 2, cross[2]
            rx = math.degrees(math.atan2(y1 - y0, z1 - z0))
            self.cube(bone, (x - cross[0] / 2, cy - cross[1] / 2, cz - ln / 2), (cross[0], cross[1], max(1, ln)), material,
                      pivot=(x, cy, cz), rotation=(rx, 0, 0))
        else:
            (x0, y0), (x1, y1) = a, b
            ln = round(math.hypot(x1 - x0, y1 - y0))
            cx, cy, z = (x0 + x1) / 2, (y0 + y1) / 2, cross[2]
            rz = -math.degrees(math.atan2(y1 - y0, x1 - x0))
            self.cube(bone, (cx - ln / 2, cy - cross[0] / 2, z - cross[1] / 2), (max(1, ln), cross[0], cross[1]), material,
                      pivot=(cx, cy, z), rotation=(0, 0, rz))


SIDES = (("right", -1), ("left", 1))
# tendrils: (x, z, segment lengths)
TENDRILS = ((-6, -3, (5, 4, 4)), (-1.5, -4.5, (6, 5, 4)), (4, -4, (5, 4, 3)), (6.5, 1.5, (5, 5, 4)), (2.5, 4.5, (6, 4, 4)),
            (-4, 4, (5, 5, 3)))
# wing pairs: name, pivot y, base roll (raise), base sweep, segments [(length, [feather lengths, 0 = torn out])]
WINGS = (("upper", 48, 40, 22, [(14, [12, 16, 10]), (14, [18, 0, 20, 14]), (12, [17, 12, 8])], (-12, -26)),
         ("lower", 42, -2, 30, [(10, [9, 12]), (11, [13, 0, 9])], (-22,)))


def at(sx, x_left, width):
    """x origin of a cube that spans x_left..x_left+width on the left side, mirrored for the right side."""
    return x_left if sx > 0 else -x_left - width


def build_eclipse():
    m = Model("eclipse", 256, 256)
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 36, 0))
    # ---- torso: broad carapace chest tapering to a narrow waist (heroic: shoulders 2x the waist)
    m.cube(body, (-6, 36, -3.5), (12, 5, 7), "plate")                   # abdomen
    m.cube(body, (-8, 41, -4), (16, 9, 8), "carapace")                  # chest
    m.cube(body, (-6.5, 35, -4), (13, 2, 8), "trim")                    # tarnished belt
    for y in (42, 44, 46):                                              # gold ribs beside the core
        m.cube(body, (-7.5, y, -4.5), (4, 1, 1), "trim")
        m.cube(body, (3.5, y, -4.5), (4, 1, 1), "trim")
    m.cube(body, (-3, 42, -4.5), (6, 6, 1), "void")                     # core socket
    m.cube(body, (-2, 43, -5), (4, 4, 1), "core")                       # the void core
    for i, y in enumerate((39, 43, 47)):                                # spine spikes
        m.cube(body, (-0.5, y, 3.5), (1, 3 + i % 2, 2), "spike", pivot=(0, y, 4), rotation=(-40, 0, 0))
    m.cube(body, (-5, 49, -3), (10, 3, 7), "plate")                     # high collar
    for x in (-3.5, -1, 1.5, 4):                                        # collar spines
        m.cube(body, (x - 0.5, 51, 2), (1, 4, 1), "spike", pivot=(x, 51, 2.5), rotation=(-25, 0, x * -5))
    m.cube(body, (-1, 22, -6), (2, 14, 1), "trim")                      # broken stole hanging over the robe

    # ---- robe: two tiers, the lower one sways; frays into jointed tendrils
    robe = m.bone("robe", "body", (0, 36, 0))
    m.cube(robe, (-7, 28, -4.5), (14, 8, 9), "cloth")
    lower = m.bone("robe_lower", "robe", (0, 28, 0))
    m.cube(lower, (-8, 19, -5), (16, 9, 10), "robe")
    for i, (x, z, segs) in enumerate(TENDRILS):
        y = 19
        parent = "robe_lower"
        for k, (suffix, ln) in enumerate(zip(("", "_mid", "_tip"), segs)):
            if k == 0:
                nl = math.hypot(x, z)
                t = m.bone(f"tendril_{i}", parent, (x, y, z), rotation=(14 * z / nl, 0, -14 * x / nl))   # splay outward
            else:
                t = m.bone(f"tendril_{i}{suffix}", parent, (x, y, z))
            wdt = 2 if k == 0 else 1
            m.cube(t, (x - wdt / 2, y - ln, z - 0.5), (wdt, ln, 1), "tendril")
            parent, y = f"tendril_{i}{suffix}", y - ln

    # ---- head: faceless hood-helm, swept horns (two segments), jagged crown
    head = m.bone("head", "body", (0, 51, -1))
    m.cube(head, (-4.5, 51, -5.5), (9, 9, 9), "helm")
    m.cube(head, (-3.5, 50, -6), (7, 3, 1), "plate")                    # jaw guard
    m.cube(head, (-5, 52, 1), (10, 7, 3), "cloth")                      # hood drape at the back
    for sx in (-1, 1):
        hx = sx * 4.5
        m.cube(head, (hx - 1, 57, -2), (2, 4, 2), "spike", pivot=(hx, 57, -1), rotation=(-35, 0, sx * -35))
        m.cube(head, (sx * 6.9 - 0.5, 59.4, 0.4), (1, 4, 1), "spike", pivot=(sx * 6.9, 59.4, 0.9), rotation=(-65, 0, sx * -20))
    for x, hgt in ((-0.5, 5), (-2.5, 3), (1.5, 3), (-4, 2), (3, 2)):
        m.cube(head, (x, 60, -5), (1, hgt, 1), "crown")

    # ---- cracked halo: three segments missing, two of them drifting loose
    halo = m.bone("halo", "head", (0, 65, -1), rotation=(16, 0, -10))
    for i in range(16):
        if i in (3, 4, 11):
            continue
        m.cube(halo, (-1.5, 65, -9), (3, 1, 1), "halo", pivot=(0, 65, -1), rotation=(0, i * 22.5, 0))
    for name, idx, off in (("halo_shard_a", 3.5, 2.5), ("halo_shard_b", 11, 3.0)):
        a = math.radians(idx * 22.5)
        cx, cz = -math.sin(a) * (8 + off), -1 - math.cos(a) * (8 + off)
        s = m.bone(name, "halo", (cx, 65.5, cz))
        m.cube(s, (cx - 1.5, 65, cz - 0.5), (3, 1, 1), "halo", pivot=(cx, 65.5, cz), rotation=(25, idx * 22.5, 15))

    # ---- the black sun: lightless 12-gon, burning rim, jagged counter-rotating rays
    corona = m.bone("corona", "body", (0, 58, 8))
    for i in range(3):
        m.cube(corona, (-10, 48, 8), (20, 20, 1), "void", pivot=(0, 58, 8.5), rotation=(0, 0, i * 30))
    for i in range(16):
        m.cube(corona, (-2, 58 + 10, 7.5), (4, 1, 1), "rim", pivot=(0, 58, 8), rotation=(0, 0, i * 22.5 + 11.25))
    rays = m.bone("corona_rays", "corona", (0, 58, 8))
    for i in range(12):
        ln = (14, 7, 10, 6, 13, 7, 15, 6, 11, 7, 10, 8)[i]
        m.cube(rays, (-0.5, 58 + 11.5, 8.25), (1, ln, 0.5), "ray", pivot=(0, 58, 8.5), rotation=(0, 0, i * 30))

    # ---- arms: shoulder (pauldron) -> upper arm -> gauntleted forearm -> hand -> fingers
    for side, sx in SIDES:
        sh = m.bone("shoulder_" + side, "body", (sx * 8, 48, 0))
        m.cube(sh, (at(sx, 7.5, 6), 45, -4.5), (6, 4, 9), "plate")
        m.cube(sh, (at(sx, 7, 7), 49, -5), (7, 1, 10), "trim")
        for j, (dz, hh) in enumerate(((-3, 5), (0, 7), (3, 4))):
            px = sx * 10.5
            m.cube(sh, (px - 0.5, 49.5, dz - 0.5), (1, hh, 1), "spike", pivot=(px, 50, dz), rotation=(0, 0, sx * -(20 + 12 * j)))
        arm = m.bone("arm_" + side, "shoulder_" + side, (sx * 10.5, 47, 0))
        m.cube(arm, (at(sx, 8, 5), 38, -2.5), (5, 9, 5), "skin")
        m.cube(arm, (at(sx, 7.5, 6), 41, -3), (6, 1, 6), "cloth")      # binding wrap
        fore = m.bone("forearm_" + side, "arm_" + side, (sx * 10.5, 38.5, 0))
        m.cube(fore, (at(sx, 8, 5), 30, -2.5), (5, 9, 5), "plate")
        m.cube(fore, (at(sx, 7.5, 6), 30, -3), (6, 2, 6), "trim")       # wrist cuff
        m.cube(fore, (sx * 13 - 0.5, 34, -0.5), (1, 4, 1), "spike", pivot=(sx * 13, 35, 0), rotation=(0, 0, sx * -55))
        hand = m.bone("hand_" + side, "forearm_" + side, (sx * 10.5, 30, 0))
        m.cube(hand, (at(sx, 7.5, 6), 26, -2), (6, 4, 4), "skin")       # palm: 2/3 of the head's width
        m.cube(hand, (at(sx, 8, 1), 24, -2.5), (1, 3, 1), "skin", pivot=(sx * 8.5, 27, -2), rotation=(-20, 0, 0))  # thumb
        fing = m.bone("fingers_" + side, "hand_" + side, (sx * 10.5, 26, 0))
        for dz in (-1.5, 0, 1.5):                                       # three 3-px fingers + 1-px claws
            m.cube(fing, (at(sx, 10, 1), 23, dz - 0.5), (1, 3, 1), "skin")
            m.cube(fing, (at(sx, 10, 1), 22, dz - 0.5), (1, 1, 1), "claw")
        spark = m.bone("spark_" + side, "hand_" + side, (sx * 10.5, 26.5, 0))
        m.cube(spark, (sx * 10.5 - 0.5, 23.5, -0.5), (1, 6, 1), "core")
        m.cube(spark, (sx * 10.5 - 3, 26, -0.5), (6, 1, 1), "core")
        m.cube(spark, (sx * 10.5 - 0.5, 26, -3), (1, 1, 6), "core")

    weapons(m)

    # ---- two pairs of tattered, jointed wings
    for name, py, roll, sweep, segs, bends in WINGS:
        for side, sx in SIDES:
            parent, px = "body", sx * 3
            base = f"wing_{name}_{side}"
            for k, (ln, feathers) in enumerate(segs):
                bname = base + ("", "_mid", "_tip")[k if len(segs) == 3 else (0 if k == 0 else 2)]
                rot = (0, sx * -sweep, sx * -roll) if k == 0 else (0, sx * -6, sx * -bends[k - 1])
                b = m.bone(bname, parent, (px, py, 4.5), rotation=rot)
                x0 = px if sx > 0 else px - ln
                m.cube(b, (x0, py - 1, 4), (ln, 2, 2), "wingbar")
                fw = ln / len(feathers)
                for j, fl in enumerate(feathers):
                    if fl <= 0:
                        continue
                    fx = px + sx * (j * fw + 0.5)
                    wdt = 3
                    ox = fx if sx > 0 else fx - wdt
                    m.cube(b, (ox, py - 1 - fl, 4.5 + (j % 2) * 0.5), (wdt, fl, 1), "feather")
                if k == len(segs) - 1:
                    m.cube(b, (px + sx * ln - (0 if sx > 0 else 1) - sx * 0.5, py - 0.5, 4.5), (1, 5, 1), "spike",
                           pivot=(px + sx * ln, py, 5), rotation=(0, 0, sx * -20))   # wing talon
                parent, px = bname, px + sx * ln

    # ---- umbral shards in two tilted, counter-rotating rings
    orbit = m.bone("orbit", "root", (0, 40, 0))
    for i in range(8):
        a = math.radians(i * 45 + 20)
        cx, cz = math.sin(a) * 22, math.cos(a) * 22
        hgt = 7 if i % 2 == 0 else 5
        m.cube(orbit, (cx - 1, 40 - hgt / 2, cz - 1), (2, hgt, 2), "shard", pivot=(cx, 40, cz), rotation=(15, 45 + i * 45, 0))
    inner = m.bone("orbit_inner", "root", (0, 47, 0), rotation=(12, 0, 8))
    for i in range(5):
        a = math.radians(i * 72)
        cx, cz = math.sin(a) * 16, math.cos(a) * 16
        m.cube(inner, (cx - 0.5, 45, cz - 0.5), (1, 4, 1), "shard", pivot=(cx, 47, cz), rotation=(0, 45, 20))
    return m


def weapons(m):
    """The summoned arsenal. Every weapon is its own bone on a hand (the Focus floats before the chest), gripped in the
    fist at (+-10.5, 26.5, 0); hafts run along -z (forward when the arm hangs)."""
    gx, gy = 10.5, 26.5
    # Umbral Scythe (right): long haft, gold bands, crescent blade hooking down and back with a burning inner edge
    s = m.bone("weapon_scythe", "hand_right", (-gx, gy, 0))
    m.cube(s, (-gx - 1, gy - 1, -22), (2, 2, 32), "metal")
    for z in (-21, -4, 8):
        m.cube(s, (-gx - 1.5, gy - 1.5, z), (3, 3, 1), "trim")
    m.cube(s, (-gx - 1.5, gy - 2, -25), (3, 4, 3), "metal")            # blade socket
    o_y, o_z, rad = gy - 1, -10, 14
    pts = [(o_y - rad * math.sin(math.radians(t)), o_z - rad * math.cos(math.radians(t))) for t in (0, 22, 44, 66, 88, 108)]
    for k in range(len(pts) - 1):
        hgt = 4 if k < 3 else 3 if k < 4 else 2
        m.beam(s, pts[k], pts[k + 1], (1, hgt, -gx), "metal", "yz")
        # inner cutting edge, one texel inside the arc
        a, b = pts[k], pts[k + 1]
        ia = (a[0] + (o_y - a[0]) * 2.5 / rad, a[1] + (o_z - a[1]) * 2.5 / rad)
        ib = (b[0] + (o_y - b[0]) * 2.5 / rad, b[1] + (o_z - b[1]) * 2.5 / rad)
        m.beam(s, ia, ib, (0.5, 1, -gx), "edge", "yz")
    m.cube(s, (-gx - 0.5, gy - 0.5, -30), (1, 1, 5), "edge")          # back spike

    # Void Lance (left): long spear with three prongs and a burning core
    s = m.bone("weapon_lance", "hand_left", (gx, gy, 0))
    m.cube(s, (gx - 1, gy - 1, -24), (2, 2, 36), "metal")
    for z in (-23, 9):
        m.cube(s, (gx - 1.5, gy - 1.5, z), (3, 3, 1), "trim")
    m.cube(s, (gx - 3, gy - 1, -25), (6, 2, 1), "trim")                # crossguard
    m.cube(s, (gx - 1, gy - 1, -28), (2, 2, 3), "core")                # void core
    m.cube(s, (gx - 0.5, gy - 0.5, -37), (1, 1, 9), "edge")            # central blade
    for sx in (-1, 1):
        m.cube(s, (gx - 0.5 + sx * 1.5, gy - 0.5, -32), (1, 1, 6), "edge", pivot=(gx + sx * 1.5, gy, -26), rotation=(0, sx * 18, 0))

    # Starfall Scepter (right): short haft, gold cradle of four prongs around a captured star
    s = m.bone("weapon_scepter", "hand_right", (-gx, gy, 0))
    m.cube(s, (-gx - 0.5, gy - 0.5, -14), (1, 1, 20), "metal")
    m.cube(s, (-gx - 1, gy - 1, -15), (2, 2, 2), "trim")
    m.cube(s, (-gx - 1.5, gy - 1.5, -20), (3, 3, 3), "core")           # the captured star
    for k in range(4):
        a = k * 90 + 45
        m.cube(s, (-gx - 0.5, gy + 1.5, -21), (1, 1, 6), "trim", pivot=(-gx, gy, -15), rotation=(-14, 0, a))
    m.cube(s, (-gx - 0.5, gy - 0.5, -25), (1, 1, 3), "edge")           # crown point
    m.cube(s, (-gx - 0.5, gy - 0.5, 6), (1, 1, 2), "trim")             # pommel

    # Echo Orb (left): a caged sphere of void glass floating beyond the palm; the cage spins
    s = m.bone("weapon_orb", "hand_left", (gx, 21, 0))
    cage = m.bone("orb_cage", "weapon_orb", (gx, 17, 0))
    m.cube(s, (gx - 1.5, 15.5, -1.5), (3, 3, 3), "core")
    m.cube(s, (gx - 2, 15, -2), (4, 4, 4), "shard")
    for k in range(4):
        a = math.radians(k * 90 + 45)
        cx, cz = gx + math.sin(a) * 4, math.cos(a) * 4
        m.cube(cage, (cx - 0.5, 14, cz - 0.5), (1, 6, 1), "trim", pivot=(cx, 17, cz), rotation=(0, k * 90 + 45, 0))
    for y in (13, 20):
        m.cube(cage, (gx - 1.5, y, -1.5), (3, 1, 3), "trim")

    # Eclipse Claws (both hands): knuckle guard and three burning blades past the fingers
    for side, sx in SIDES:
        s = m.bone("weapon_claws_" + side, "hand_" + side, (sx * gx, gy, 0))
        m.cube(s, (at(sx, 7, 7), 25, -2.5), (7, 3, 5), "metal")
        for dz, tilt in ((-1.5, 10), (0, 0), (1.5, -10)):
            m.cube(s, (at(sx, 10, 1), 14, dz - 0.5), (1, 11, 1), "edge", pivot=(sx * gx, 25, dz), rotation=(tilt, 0, sx * 6))

    # Eclipse Focus (body): a lightless disc ringed in gold, floating before the chest - the lens of the Ray and Nova
    s = m.bone("weapon_focus", "body", (0, 44, -16))
    for i in range(3):
        m.cube(s, (-4, 40, -16.5), (8, 8, 1), "void", pivot=(0, 44, -16), rotation=(0, 0, i * 30))
    for i in range(10):
        m.cube(s, (-1.5, 44 + 5.5, -16.5), (3, 1, 1), "halo", pivot=(0, 44, -16), rotation=(0, 0, i * 36))
    for i in range(4):
        m.cube(s, (-0.5, 44 + 7, -16.25), (1, 4, 0.5), "ray", pivot=(0, 44, -16), rotation=(0, 0, i * 90 + 45))
    m.cube(s, (-1, 43, -17), (2, 2, 1), "core")
    core = m.bone("spark_core", "body", (0, 44, -9))
    m.cube(core, (-0.5, 40, -9.5), (1, 8, 1), "core")
    m.cube(core, (-4, 43.5, -9.5), (8, 1, 1), "core")
    m.cube(core, (-2, 42, -10), (4, 4, 1), "rim", pivot=(0, 44, -9.5), rotation=(0, 0, 45))

    # Umbral Chain (right): links of tarnished gold ending in a burning hook; the far half whips behind
    s = m.bone("weapon_chain", "hand_right", (-gx, gy, 0))
    for k in range(5):
        z = -3 - k * 3
        size = (1, 2, 3) if k % 2 == 0 else (2, 1, 3)
        m.cube(s, (-gx - size[0] / 2, gy - size[1] / 2, z - 2), size, "trim")
    end = m.bone("chain_end", "weapon_chain", (-gx, gy, -17))
    for k in range(4):
        z = -18 - k * 3
        size = (1, 2, 3) if k % 2 == 1 else (2, 1, 3)
        m.cube(end, (-gx - size[0] / 2, gy - size[1] / 2, z - 2), size, "trim")
    m.cube(end, (-gx - 1, gy - 1, -32), (2, 2, 2), "core")
    m.beam(end, (gy, -33), (gy - 4, -36), (1, 1, -gx), "edge", "yz")
    m.beam(end, (gy - 4, -36), (gy - 6, -33), (1, 1, -gx), "edge", "yz")


def build_pylon():
    m = Model("umbral_pylon", 64, 64)
    m.bone("root", None, (0, 0, 0))
    base = m.bone("base", "root", (0, 0, 0))
    m.cube(base, (-4, 0, -4), (8, 3, 8), "plate")
    m.cube(base, (-4.5, 3, -4.5), (9, 1, 9), "trim")
    m.cube(base, (-2.5, 4, -2.5), (5, 11, 5), "void")
    for i in range(4):
        m.cube(base, (-0.5, 4, -4), (1, 9, 1), "spike", pivot=(0, 4, 0), rotation=(0, 45 + i * 90, 0))
    m.cube(base, (-3, 15, -3), (6, 1, 6), "trim")
    crystal = m.bone("crystal", "root", (0, 23, 0))
    m.cube(crystal, (-2, 19, -2), (4, 8, 4), "shard", pivot=(0, 23, 0), rotation=(0, 45, 0))
    m.cube(crystal, (-1, 20, -1), (2, 6, 2), "core", pivot=(0, 23, 0), rotation=(0, 45, 0), inflate=0.25)
    for sy in (-1, 1):
        m.cube(crystal, (-1, 23 + sy * 5 - 1, -1), (2, 2, 2), "shard", pivot=(0, 23 + sy * 5, 0), rotation=(45, 45, 0))
    ring = m.bone("ring", "root", (0, 23, 0), rotation=(15, 0, 0))
    for i in range(10):
        if i == 6:
            continue
        m.cube(ring, (-1.5, 22.5, -6.5), (3, 1, 1), "halo", pivot=(0, 23, 0), rotation=(0, i * 36, 0))
    return m


def build_echo():
    """The Eclipse Echo: a small hooded wraith (the minion) - same language, 40-odd cubes."""
    m = Model("eclipse_echo", 64, 64)
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 9, 0))
    m.cube(body, (-3, 10, -2), (6, 6, 4), "carapace")
    m.cube(body, (-1, 12, -2.5), (2, 2, 1), "core")
    m.cube(body, (-3.5, 9, -2.5), (7, 1, 5), "trim")
    skirt = m.bone("skirt", "body", (0, 9, 0))
    m.cube(skirt, (-3, 4, -2), (6, 5, 4), "robe")
    for i, (x, z) in enumerate(((-2, -1), (0.5, 1), (2, -1))):
        t = m.bone(f"tendril_{i}", "skirt", (x, 4, z))
        m.cube(t, (x - 0.5, 1, z - 0.5), (1, 3, 1), "tendril")
        tip = m.bone(f"tendril_{i}_tip", f"tendril_{i}", (x, 1, z))
        m.cube(tip, (x - 0.5, -2, z - 0.5), (1, 3, 1), "tendril")
    head = m.bone("head", "body", (0, 16, -0.5))
    m.cube(head, (-3, 16, -3.5), (6, 6, 6), "helm")
    m.cube(head, (-3.5, 20, -1), (7, 3, 4), "cloth")                   # hood
    for sx in (-1, 1):
        m.cube(head, (sx * 3 - 0.5, 21, -1), (1, 3, 1), "spike", pivot=(sx * 3, 21, -0.5), rotation=(-30, 0, sx * -25))
    halo = m.bone("halo", "head", (0, 25, -0.5), rotation=(14, 0, 8))
    for i in range(8):
        if i == 2:
            continue
        m.cube(halo, (-1, 25, -4), (2, 1, 1), "halo", pivot=(0, 25, -0.5), rotation=(0, i * 45, 0))
    for side, sx in SIDES:
        arm = m.bone("arm_" + side, "body", (sx * 3.5, 15.5, 0))
        m.cube(arm, (at(sx, 3, 2), 12, -1), (2, 4, 2), "skin")
        fore = m.bone("forearm_" + side, "arm_" + side, (sx * 4, 12, 0))
        m.cube(fore, (at(sx, 3, 2), 9, -1), (2, 3, 2), "plate")
        m.cube(fore, (at(sx, 3, 1), 8, -1), (1, 1, 1), "claw")
        m.cube(fore, (at(sx, 4, 1), 8, 0), (1, 1, 1), "claw")
        wing = m.bone("wing_" + side, "body", (sx * 1.5, 15, 2))
        x0 = sx * 1.5 if sx > 0 else sx * 1.5 - 5
        m.cube(wing, (x0, 14, 2), (5, 1, 1), "wingbar")
        m.cube(wing, (x0 + (0 if sx > 0 else 2), 9, 2.5), (3, 5, 1), "feather")
        tip = m.bone("wing_" + side + "_tip", "wing_" + side, (sx * 6.5, 15, 2))
        x1 = sx * 6.5 if sx > 0 else sx * 6.5 - 4
        m.cube(tip, (x1, 14, 2), (4, 1, 1), "wingbar")
        m.cube(tip, (x1 + (0 if sx > 0 else 1), 8, 2.5), (3, 6, 1), "feather")
    return m


def geo(m, w, h, off):
    bones = []
    for b in m.bones:
        b = dict(b)
        b["cubes"] = [{k: v for k, v in c.items() if not k.startswith("_")} for c in b["cubes"]]
        if not b["cubes"]:
            del b["cubes"]
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + m.name, "texture_width": m.tw, "texture_height": m.th,
                        "visible_bounds_width": w, "visible_bounds_height": h, "visible_bounds_offset": [0, off, 0]},
        "bones": bones}]}


# ------------------------------------------------------------------ animation: keyed poses with easing + lag

EASINGS_USED = set()
CH = {"r": "rotation", "p": "position", "s": "scale"}
NEUTRAL = {"r": [0, 0, 0], "p": [0, 0, 0], "s": [1, 1, 1]}


def merge(*parts):
    """Merges pose dicts left to right. Bone value: (x,y,z) rotation or {"r","p","s"}."""
    out = {}
    for part in parts:
        for b, v in part.items():
            if isinstance(v, (list, tuple)):
                v = {"r": list(v)}
            out.setdefault(b, {}).update({k: list(x) for k, x in v.items()})
    return out


def arm(side, sh=(0, 0, 0), up=(0, 0, 0), fore=(0, 0, 0), hand=(0, 0, 0), fing=None):
    """One arm. Values are written for the right arm; the left mirrors y/z so the same numbers mean the same motion.
    up = (forward(-)/back(+), twist, abduct(+ = out)); fore x < 0 bends the elbow forward; fing x curls."""
    g = 1 if side == "right" else -1
    p = {f"shoulder_{side}": [sh[0], sh[1] * g, sh[2] * g], f"arm_{side}": [up[0], up[1] * g, up[2] * g],
         f"forearm_{side}": [fore[0], fore[1] * g, fore[2] * g], f"hand_{side}": [hand[0], hand[1] * g, hand[2] * g]}
    if fing is not None:
        f = fing if isinstance(fing, (list, tuple)) else (0, 0, fing)
        p[f"fingers_{side}"] = [f[0], f[1] * g, f[2] * g]
    return p


def arms(**kw):
    return merge(arm("right", **kw), arm("left", **kw))


def wing(side, pair, root=(0, 0, 0), mid=(0, 0, 0), tip=(0, 0, 0)):
    """root = (pitch, sweep back(+), raise(+)); mid/tip bend further (+ raise / - droop)."""
    g = 1 if side == "right" else -1
    base = f"wing_{pair}_{side}"
    p = {base: [root[0], root[1] * g, root[2] * g], base + "_tip": [tip[0], tip[1] * g, tip[2] * g]}
    if pair == "upper":
        p[base + "_mid"] = [mid[0], mid[1] * g, mid[2] * g]
    return p


def wings(root=(0, 0, 0), mid=(0, 0, 0), tip=(0, 0, 0), lower=None):
    lo = lower if lower is not None else (tuple(v * 0.7 for v in root), tuple(v * 0.7 for v in tip))
    p = {}
    for side in ("right", "left"):
        p = merge(p, wing(side, "upper", root, mid, tip), wing(side, "lower", lo[0], (0, 0, 0), lo[1]))
    return p


def tendrils(rx=0, rz=0, curl=1.0):
    """Trailing robe tendrils: rx > 0 trails backwards (swept by a forward motion), curl spreads it down the chain."""
    p = {}
    for i in range(len(TENDRILS)):
        p[f"tendril_{i}"] = [rx * (1 + 0.4 * curl), 0, rz * 1.3]     # mid and tip follow through the lag + sway
    return p


def W(name, r=(0, 0, 0), p=None):
    """Orientation of a summoned weapon in a pose (its scale is driven by WEAPON_TIMING)."""
    out = {}
    for b in WEAPON_BONES[name]:
        out[b] = {"r": list(r)}
        if p:
            out[b]["p"] = list(p)
    return out


# secondary chains: delay in ticks (they trail the primary motion) and their easing on fast keys (overshoot)
LAG = {"corona": 2, "corona_rays": 3, "halo": 2, "halo_shard_a": 3, "halo_shard_b": 4, "orbit": 3, "orbit_inner": 2,
       "robe": 1, "robe_lower": 2, "head": 1}
for _side in ("right", "left"):
    LAG.update({f"wing_upper_{_side}": 1, f"wing_upper_{_side}_mid": 2, f"wing_upper_{_side}_tip": 3,
                f"wing_lower_{_side}": 2, f"wing_lower_{_side}_tip": 3, f"fingers_{_side}": 1, "chain_end": 2})
for _i in range(len(TENDRILS)):
    LAG.update({f"tendril_{_i}": 2, f"tendril_{_i}_mid": 3, f"tendril_{_i}_tip": 4})


def ambient(intensity=1.0, speed=1.0, wing_amp=4.0, jitter=0.0):
    """Organic, never-still layer on top of every clip (query.life_time: it never snaps between clips). Periods are
    non-multiples so the layers never line up; chains get phase offsets so motion travels down them."""
    s, a = speed, intensity
    lt = "query.life_time"
    j = f" + math.sin({lt} * 1500) * {jitter}" if jitter else ""
    amb = {
        "corona": {"rotation": ["0", "0", f"{lt} * {14 * s:.1f}"]},
        "corona_rays": {"rotation": ["0", "0", f"{lt} * {-31 * s:.1f}"]},
        "halo": {"rotation": ["0", f"{lt} * {27 * s:.1f}", "0"]},
        "halo_shard_a": {"position": ["0", f"math.sin({lt} * 117) * {1.2 * a:.2f}", f"math.cos({lt} * 83) * {0.8 * a:.2f}"]},
        "halo_shard_b": {"position": ["0", f"math.sin({lt} * 101 + 90) * {1.0 * a:.2f}", "0"]},
        "orbit": {"rotation": ["0", f"{lt} * {-37 * s:.1f}", "0"], "position": ["0", f"math.sin({lt} * 71) * {1.0 * a:.2f}", "0"]},
        "orbit_inner": {"rotation": ["0", f"{lt} * {61 * s:.1f}", "0"]},
        "orb_cage": {"rotation": ["0", f"{lt} * 140", "0"]},
        "weapon_focus": {"rotation": ["0", "0", f"{lt} * -90"]},
        "robe_lower": {"rotation": [f"math.sin({lt} * 63) * {1.5 * a:.2f}", "0", f"math.sin({lt} * 47) * {1.2 * a:.2f}"]},
    }
    for i in range(len(TENDRILS)):
        ph = i * 57
        for k, suffix in enumerate(("", "_mid", "_tip")):
            amp = (4, 6, 8)[k] * a
            amb[f"tendril_{i}{suffix}"] = {"rotation": [
                f"math.sin({lt} * {83 * s:.0f} + {ph + k * 40}) * {amp:.1f}", "0",
                f"math.cos({lt} * {59 * s:.0f} + {ph + k * 40}) * {amp * 0.6:.1f}"]}
    for pair, amp0, ph0 in (("upper", wing_amp, 0), ("lower", wing_amp * 0.8, 70)):
        for side, g in (("right", 1), ("left", -1)):
            chain = ["", "_tip"] if pair == "lower" else ["", "_mid", "_tip"]
            for k, suffix in enumerate(chain):
                amp = amp0 * a * (1 + 0.35 * k)
                ph = ph0 + k * 35
                amb[f"wing_{pair}_{side}{suffix}"] = {"rotation": [
                    "0", f"{"-" if g < 0 else ""}math.sin({lt} * {67 * s:.0f} + {ph}) * {amp * 0.6:.1f}" if k == 0 else "0",
                    f"{"-" if g < 0 else ""}math.sin({lt} * {67 * s:.0f} + {ph}) * {amp:.1f}{j}"]}
    return amb


def fmt(t):
    return str(round(t + 0.0, 4))


def join(x, ex):
    """Keyed value + ambient Molang, without '+ -' (keeps the expressions simple for every Molang parser)."""
    if not x:
        return ex
    return f"{x} - {ex[1:]}" if ex.startswith("-") else f"{x} + {ex}"


def num(x):
    r = round(x, 3)
    return int(r) if r == int(r) else r


def ease_fn(name):
    """Python versions of the GeckoLib easings (for the preview and lag resampling)."""
    import math as _m
    c1 = 1.70158
    f = {
        "linear": lambda t: t,
        "easeInSine": lambda t: 1 - _m.cos(t * _m.pi / 2),
        "easeOutSine": lambda t: _m.sin(t * _m.pi / 2),
        "easeInOutSine": lambda t: -(_m.cos(_m.pi * t) - 1) / 2,
        "easeInQuad": lambda t: t * t,
        "easeOutQuad": lambda t: 1 - (1 - t) ** 2,
        "easeInOutQuad": lambda t: 2 * t * t if t < 0.5 else 1 - (-2 * t + 2) ** 2 / 2,
        "easeInCubic": lambda t: t ** 3,
        "easeOutCubic": lambda t: 1 - (1 - t) ** 3,
        "easeInOutCubic": lambda t: 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2,
        "easeInQuart": lambda t: t ** 4,
        "easeOutQuart": lambda t: 1 - (1 - t) ** 4,
        "easeInExpo": lambda t: 0 if t == 0 else 2 ** (10 * t - 10),
        "easeOutExpo": lambda t: 1 if t == 1 else 1 - 2 ** (-10 * t),
        "easeInBack": lambda t: (c1 + 1) * t ** 3 - c1 * t * t,
        "easeOutBack": lambda t: 1 + (c1 + 1) * (t - 1) ** 3 + c1 * (t - 1) ** 2,
        "easeOutElastic": lambda t: 0 if t == 0 else 1 if t == 1 else 2 ** (-10 * t) * _m.sin((t * 10 - 0.75) * (2 * _m.pi / 3)) + 1,
    }
    return f[name]


class Key:
    """A key pose at `tick` (may be fractional). ease: easing of the segment that ends here (GeckoLib semantics);
    sec: easing for the lagging secondary chains on that segment (default: overshoot after fast keys)."""

    def __init__(self, tick, pose, ease="easeInOutSine", sec=None):
        self.tick, self.pose, self.ease = tick, pose, ease
        fast = ease in ("easeOutExpo", "easeOutQuart", "easeOutCubic", "easeOutBack", "easeInExpo", "easeInCubic", "easeInQuart", "easeInBack")
        self.sec = sec or ("easeOutBack" if fast else "easeInOutSine")


def build_clip(n_ticks, keys, loop=False, amb=None, weapon_tracks=None, hidden=ALL_HIDDEN, lag=True):
    length = n_ticks / 20.0
    bones = {}
    used = {}
    for k in keys:
        for b, chans in k.pose.items():
            for c in chans:
                used.setdefault(b, set()).add(c)
    for b, chans in used.items():
        for c in sorted(chans):
            track = []
            for k in keys:
                v = k.pose.get(b, {}).get(c, NEUTRAL[c])
                e = k.ease if b not in LAG else k.sec
                if c == "s" and ("Back" in e or "Elastic" in e):
                    e = "easeOutCubic"                # never overshoot a scale through zero
                track.append([k.tick / 20.0, [float(x) for x in v], e])
            if lag and b in LAG and not loop:
                d = LAG[b] / 20.0
                shifted = []
                for i, (t, v, e) in enumerate(track):
                    if 0 < t < length - 1e-6:
                        lo = shifted[-1][0] + 0.025 if shifted else 0
                        t = min(max(t + d, lo), length - 0.05)
                    shifted.append([t, v, e])
                # drop keys pushed onto each other (keep the later pose)
                track = []
                for t, v, e in shifted:
                    if track and abs(track[-1][0] - t) < 0.02:
                        track[-1] = [t, v, e]
                    else:
                        track.append([t, v, e])
            bones.setdefault(b, {})[CH[c]] = track
    for b, tr in (weapon_tracks or {}).items():
        bones.setdefault(b, {})["scale"] = tr
    for b in hidden:
        bones.setdefault(b, {})
        if "scale" not in bones[b]:
            bones[b]["scale"] = "hidden"
    # emit
    out = {}
    for b, chans in bones.items():
        out[b] = {}
        for ch, track in chans.items():
            extra = (amb or {}).get(b, {}).get(ch)
            if track == "hidden":
                out[b][ch] = [0, 0, 0]
                continue
            def values(v):
                vals = [num(x) for x in v]
                if extra:
                    vals = [join(x, ex) if ex not in ("0", 0) else x for x, ex in zip(vals, extra)]
                return vals
            if all(v == track[0][1] for _, v, _ in track):
                out[b][ch] = values(track[0][1])     # never changes in this clip: a constant (or pure Molang) channel
                continue
            frames = {}
            for i, (t, v, e) in enumerate(track):
                vals = values(v)
                entry = {"vector": vals}
                if i > 0 and e and e != "linear":
                    entry["easing"] = e
                    EASINGS_USED.add(e)
                frames[fmt(t)] = entry
            out[b][ch] = frames
    for b, chans in (amb or {}).items():
        for ch, exprs in chans.items():
            if ch not in out.get(b, {}):
                out.setdefault(b, {})[ch] = list(exprs)
    a = {"animation_length": round(length, 4), "bones": out}
    if loop:
        a["loop"] = True
    return a


def weapon_tracks(move, stage, n_ticks):
    """Scale keys for the move's weapon bones and sparks in one stage clip."""
    if move not in WEAPON_TIMING:
        return {}
    name, (ast, at_), (vst, vt), dis = WEAPON_TIMING[move]
    order = {"windup": 0, "strike": 1, "recover": 2}
    si = order[stage]
    L = n_ticks / 20.0
    one, big, zero = [1, 1, 1], [1.15, 1.15, 1.15], [0, 0, 0]
    w, sp = [], []
    if si < order[ast] or si > order[vst]:
        w = [[0, zero, None], [L, zero, None]]
    else:
        w = [[0, zero if si == order[ast] else one, None]]
        if si == order[ast]:
            t0 = at_ / 20.0
            w += [[t0, zero, None], [t0 + FORM * 0.6 / 20, big, "easeOutCubic"], [t0 + FORM / 20, one, "easeInOutSine"]]
            sp += [[0, zero, None], [t0, zero, None], [t0 + 1.5 / 20, [1.6] * 3, "easeOutExpo"], [t0 + FORM / 20, zero, "easeInCubic"]]
        if si == order[vst]:
            t1 = vt / 20.0
            w += [[t1, one, "linear"], [t1 + dis * 0.35 / 20, [1.12] * 3, "easeOutQuad"], [t1 + dis / 20, zero, "easeInBack"]]
            sp += ([[0, zero, None]] if not sp else []) + [[t1, zero, None], [t1 + 1 / 20, [1.3] * 3, "easeOutExpo"],
                                                           [t1 + dis / 20, zero, "easeInQuad"]]
        if w[-1][0] < L - 1e-6:
            w.append([L, w[-1][1], "linear"])
    for tr in (w, sp):
        for k in tr:
            k[0] = min(k[0], L)
    out = {}
    for b in WEAPON_BONES[name]:
        out[b] = w
    if sp:
        if sp[-1][0] < L - 1e-6:
            sp.append([L, zero, "linear"])
        for b in WEAPON_SPARKS[name]:
            out[b] = sp
    return out


def animations():
    A = {}
    amb = ambient()

    # -------------------------------------------------- the rest pose every attack starts from and returns to
    base = merge({"root": {"p": [0, 2, 0]}, "body": (8, 0, 0), "head": (6, 0, 0)},
                 arms(sh=(0, 0, 0), up=(-6, 0, 8), fore=(-16, 0, 0), hand=(-8, 0, 0), fing=12),
                 wings((0, 0, 0)), tendrils(0))

    def P(*parts, **kw):
        return merge(base, *parts, kw)

    def stage(move, st, keys, wt=True, n=None, **kw):
        idx = {"windup": 0, "strike": 1, "recover": 2}[st]
        ticks = n if n is not None else MOVES[move][idx]
        A[f"{move}_{st}"] = build_clip(ticks, keys, amb=amb, weapon_tracks=weapon_tracks(move, st, ticks) if wt else None, **kw)

    # -------------------------------------------------- idles (loops: layered sines on query.life_time + breathing)
    def idle(speed=1.0, wing_amp=5.0, bob=1.6, jitter=0.0, pose=None, length=12):
        lt = "query.life_time"
        a = ambient(1.0 + 0.2 * (speed - 1), speed, wing_amp, jitter)
        s = speed
        breath = f"math.sin({lt} * {90 * s:.0f})"           # 4 s breath
        a.update({
            "root": {"position": ["0", f"2 + math.sin({lt} * {120 * s:.0f}) * {bob} + math.sin({lt} * {47 * s:.0f}) * 0.5", "0"],
                     "rotation": [f"math.sin({lt} * {37 * s:.0f}) * 1.5", f"math.sin({lt} * {23 * s:.0f}) * 4", f"math.sin({lt} * {53 * s:.0f}) * 1.2"]},
            "body": {"rotation": [f"8 + {breath} * 2.5", "0", f"math.sin({lt} * {41 * s:.0f}) * 1.5"]},
            "head": {"rotation": [f"6 - {breath} * 2 + math.sin({lt} * {131 * s:.0f}) * 1.5{(' + math.sin(' + lt + ' * 1700) * ' + str(jitter)) if jitter else ''}",
                                  f"math.sin({lt} * {29 * s:.0f}) * 12 + math.sin({lt} * {73 * s:.0f}) * 3", f"math.sin({lt} * {19 * s:.0f}) * 4"]},
            "corona": {"rotation": ["0", "0", f"{lt} * {14 * s:.1f}"], "scale": [f"1 + {breath} * 0.03"] * 3},
        })
        for side, g in (("right", 1), ("left", -1)):
            ph = 0 if side == "right" else 70
            a[f"shoulder_{side}"] = {"position": ["0", f"{breath} * 0.4", "0"]}
            a[f"arm_{side}"] = {"rotation": [f"-6 + math.sin({lt} * {90 * s:.0f} + {ph}) * 4", "0", f"{g} * (8 + math.sin({lt} * {61 * s:.0f} + {ph}) * 2)"]}
            a[f"forearm_{side}"] = {"rotation": [f"-16 + math.sin({lt} * {90 * s:.0f} + {ph + 40}) * 5", "0", "0"]}
            a[f"hand_{side}"] = {"rotation": [f"-8 + math.sin({lt} * {90 * s:.0f} + {ph + 80}) * 6", "0", f"{g} * math.sin({lt} * {113 * s:.0f}) * 4"]}
            a[f"fingers_{side}"] = {"rotation": ["0", "0", f"{-g} * (12 + math.sin({lt} * {157 * s:.0f} + {ph}) * 10)"]}
        if pose:
            for b, chans in pose.items():
                a[b] = {**a.get(b, {}), **chans}
        for b in ALL_HIDDEN:
            a[b] = {"scale": [0, 0, 0]}
        return {"loop": True, "animation_length": length, "bones": a}

    A["idle"] = idle()
    A["idle_total"] = idle(1.3, 7, 2.0)
    A["idle_enraged"] = idle(1.8, 9, 2.4, jitter=1.2)
    # shielded: cocooned in its wings, arms crossed over the core, corona dimmed
    lt = "query.life_time"
    cocoon = {"head": {"rotation": [f"24 + math.sin({lt} * 90) * 2", f"math.sin({lt} * 29) * 4", "0"]},
              "body": {"rotation": [f"14 + math.sin({lt} * 90) * 2", "0", "0"]},
              "corona": {"rotation": ["0", "0", f"{lt} * 8"], "scale": ["0.85", "0.85", "0.85"]}}
    for side, g in (("right", 1), ("left", -1)):
        cocoon[f"arm_{side}"] = {"rotation": ["-62", f"{g * -28}", f"{g * -12}"]}
        cocoon[f"forearm_{side}"] = {"rotation": ["-74", "0", "0"]}
        cocoon[f"hand_{side}"] = {"rotation": ["10", "0", "0"]}
        for suffix, (y, z) in (("", (-58, 18)), ("_mid", (-20, 25)), ("_tip", (-10, 20))):
            cocoon[f"wing_upper_{side}{suffix}"] = {"rotation": ["0", f"{g * y} + math.sin({lt} * 60 + {len(suffix) * 20}) * 2.5", f"{g * z}"]}
        for suffix, (y, z) in (("", (-55, 6)), ("_tip", (-25, 10))):
            cocoon[f"wing_lower_{side}{suffix}"] = {"rotation": ["0", f"{g * y} + math.sin({lt} * 60 + 40) * 2", f"{g * z}"]}
    A["idle_shielded"] = idle(0.7, 2, 1.0, pose=cocoon)

    # -------------------------------------------------- Umbral Sweep: draw the scythe across the body, rake 130 degrees
    w, a_, r = MOVES["sweep"]
    # the scythe's haft extends the arm (x 90) with the blade twisted flat (y -90): the arm sweeps a horizontal arc
    # (forward raise, then yaw from far right to far left), the blade leading
    reap = W("scythe", (90, -90, 0))
    draw = P(arm("right", sh=(0, 0, 8), up=(-70, 50, 10), fore=(-30, 0, 0), hand=(-10, 0, 0), fing=60), reap,
             arm("left", up=(-40, -10, -10), fore=(-50, 0, 0), hand=(0, 0, 0), fing=20),
             wings((0, -25, 26), (0, 0, 12), (0, 0, 6)), tendrils(-6, 8),
             body=(4, 30, 3), head=(4, -20, 0), root={"p": [0, 3, 0]})
    coil = P(draw, arm("right", sh=(0, 0, 12), up=(-86, 84, 6), fore=(-6, 0, 0), hand=(0, 0, 0), fing=60),
             body=(2, 44, 5), head=(6, -34, 0), root={"p": [0, 3.5, 1]})
    contact = P(arm("right", sh=(4, 0, -4), up=(-88, -30, 0), fore=(-4, 0, 0), hand=(6, 0, 0), fing=60), reap,
                arm("left", up=(18, 10, 38), fore=(-20, 0, 0), hand=(0, 0, 20), fing=10),
                wings((0, -55, 8), (0, 0, -4), (0, 0, -10)), tendrils(10, -10),
                body=(12, -38, -4), head=(8, 18, 0), root={"p": [0, 0, -3]})
    over = P(contact, arm("right", sh=(6, 0, -6), up=(-80, -72, 0), fore=(-18, 0, 0), hand=(10, 0, 0), fing=60),
             body=(16, -54, -5), head=(12, 24, 0))
    settle = P(over, arm("right", sh=(0, 0, 0), up=(-55, -45, 20), fore=(-40, 0, 0), hand=(0, 0, 0), fing=40),
               body=(12, -36, -3))
    coiled = P(coil, body=(2, 48, 6))
    stage("sweep", "windup", [Key(0, base), Key(8, draw, "easeOutCubic"), Key(14, coil, "easeInOutSine"),
                              Key(18, coiled, "easeInOutSine")])
    stage("sweep", "strike", [Key(0, coiled), Key(2.5, contact, "easeInExpo"), Key(4, over, "easeOutCubic"),
                              Key(6, over, "linear")])
    stage("sweep", "recover", [Key(0, over), Key(5, settle, "easeOutCubic"), Key(16, base, "easeInOutSine")])

    # -------------------------------------------------- Void Lance: summon the spear, cock it overhead, hurl it (it becomes the beam)
    w, a_, r = MOVES["lance"]
    summon_l = P(arm("left", sh=(0, 0, 6), up=(-60, 0, 30), fore=(-40, 0, 0), hand=(0, 0, 0), fing=55), W("lance", (0, 0, 0)),
                 wings((0, 10, 18), (0, 0, 10), (0, 0, 8)), body=(2, -10, 0), head=(-6, 10, 0),
                 corona={"s": [1.08, 1.08, 1.08]})
    cock = P(arm("left", sh=(-10, 0, 14), up=(-160, -20, 10), fore=(-40, 0, 0), hand=(30, 0, 0), fing=55), W("lance", (196, 0, 0)),
             arm("right", up=(-60, 10, -10), fore=(-30, 0, 0), hand=(0, 0, 0), fing=10),
             wings((0, -10, 28), (0, 0, 11), (0, 0, 6)), tendrils(-8, 0),
             body=(-14, -30, -4), head=(-4, 26, 0), root={"p": [0, 3, 2]}, corona={"s": [1.17, 1.17, 1.17]}, orbit_inner={"s": [0.6, 0.6, 0.6]})
    tense = P(cock, body=(-17, -34, -5), arm_left=(-168, -24, 12), weapon_lance={"r": [206, 0, 0]}, corona={"s": [1.21, 1.21, 1.21]})
    hurl = P(arm("left", sh=(10, 0, 0), up=(-80, 10, 4), fore=(-4, 0, 0), hand=(-10, 0, 0), fing=10), W("lance", (118, 0, 0)),
             arm("right", up=(10, -10, 30), fore=(-20, 0, 0), hand=(0, 0, 0), fing=10),
             wings((0, 25, 10), (0, 0, -6), (0, 0, -12)), tendrils(14, 0),
             body=(18, 22, 2), head=(2, -14, 0), root={"p": [0, 1, -3]}, corona={"s": [1.27, 1.27, 1.27]})
    follow = P(hurl, body=(22, 28, 3), arm_left=(-55, 20, 10), head=(8, -18, 0))
    stage("lance", "windup", [Key(0, base), Key(7, summon_l, "easeOutCubic"), Key(16, cock, "easeInOutCubic"),
                              Key(18, cock), Key(26, tense, "easeInOutSine")])
    stage("lance", "strike", [Key(0, tense), Key(1.5, hurl, "easeInExpo"), Key(4, follow, "easeOutQuart")])
    stage("lance", "recover", [Key(0, follow), Key(5, P(follow, body=(16, 18, 2)), "easeOutCubic"), Key(14, base, "easeInOutSine")])

    # -------------------------------------------------- Falling Stars: raise the scepter to the sky, bring it down
    w, a_, r = MOVES["starfall"]
    lift = P(arm("right", sh=(-6, 0, 10), up=(-120, 0, 30), fore=(-30, 0, 0), hand=(0, 0, 0), fing=55), W("scepter", (90, 0, 0)),
             arm("left", up=(-20, 0, 40), fore=(-30, 0, 0), hand=(0, 0, 20), fing=20),
             wings((0, -5, 30), (0, 0, 15), (0, 0, 10)), body=(-6, 0, 0), head=(-20, 0, 0))
    up = P(arm("right", sh=(-10, 0, 14), up=(-178, 0, 8), fore=(-4, 0, 0), hand=(-10, 0, 0), fing=55), W("scepter", (90, 0, 0)),
           arm("left", up=(-30, 0, 70), fore=(-10, 0, 0), hand=(0, 0, 40), fing=0),
           wings((0, -8, 32), (0, 0, 13), (0, 0, 10)), tendrils(-4, 0),
           body=(-14, 0, -3), head=(-38, 0, 0), root={"p": [0, 4, 0]}, orbit={"s": [1.3, 1, 1.3]})
    smite = P(arm("right", sh=(6, 0, 0), up=(-48, 0, 20), fore=(-12, 0, 0), hand=(-20, 0, 0), fing=55), W("scepter", (90, 0, 0)),
              arm("left", up=(10, 0, 55), fore=(-10, 0, 0), hand=(0, 0, 30), fing=10),
              wings((0, 15, -6), (0, 0, -12), (0, 0, -15)), tendrils(8, 0),
              body=(22, 0, 2), head=(18, 0, 0), root={"p": [0, 0, -1]}, orbit={"s": [1.1, 1, 1.1]})
    stage("starfall", "windup", [Key(0, base), Key(6, lift, "easeOutCubic"), Key(16, up, "easeInOutCubic")])
    stage("starfall", "strike", [Key(0, up), Key(2.5, smite, "easeInExpo"), Key(5, P(smite, body=(26, 0, 2)), "easeOutCubic"),
                                 Key(10, P(smite, body=(24, 0, 2)), "linear")])
    stage("starfall", "recover", [Key(0, P(smite, body=(24, 0, 2))), Key(14, base, "easeInOutSine")])

    # -------------------------------------------------- Echo summons: the orb rises from the open palm, shatters into echoes
    w, a_, r = MOVES["summon"]
    offer = P(arm("left", up=(-70, 30, 30), fore=(-50, 0, 0), hand=(40, 0, 0), fing=-10), W("orb", (0, 0, 0)),
              arm("right", up=(-20, 0, 50), fore=(-30, 0, 0), hand=(0, 0, 30), fing=0),
              wings((0, -20, 28), (0, 0, 14), (0, 0, 10)), body=(-6, -15, 0), head=(10, 20, 0))
    raise_o = P(arm("left", sh=(-8, 0, 10), up=(-140, 10, 25), fore=(-20, 0, 0), hand=(30, 0, 0), fing=-15), W("orb", (0, 0, 0)),
                arm("right", up=(-30, 0, 85), fore=(-10, 0, 0), hand=(0, 0, 40), fing=-10),
                wings((0, -18, 30), (0, 0, 14), (0, 0, 11)), tendrils(-4, 0), body=(-14, -8, 0), head=(-26, 10, 0),
                root={"p": [0, 4, 0]}, orbit={"s": [1.5, 1.2, 1.5]}, orbit_inner={"s": [1.4, 1.4, 1.4]})
    burst = P(raise_o, wings((0, -22, 35), (0, 0, 18), (0, 0, 13)), body=(-20, -6, 0), head=(-34, 8, 0),
              arm_left=(-150, 10, 40), corona={"s": [1.17, 1.17, 1.17]}, orbit={"s": [1.8, 1.3, 1.8]})
    stage("summon", "windup", [Key(0, base), Key(7, offer, "easeOutCubic"), Key(20, raise_o, "easeInOutCubic")])
    stage("summon", "strike", [Key(0, raise_o), Key(1.5, burst, "easeOutExpo"), Key(4, burst, "linear")])
    stage("summon", "recover", [Key(0, burst), Key(5, P(burst, body=(-10, -6, 0)), "easeOutCubic"), Key(16, base, "easeInOutSine")])

    # -------------------------------------------------- Shadow Dive: claws spring from the hands, wings pump, plunge, crater
    w, a_, r = MOVES["dive"]
    bare = P(arms(sh=(0, 0, 8), up=(-30, 0, 40), fore=(-60, 0, 0), hand=(-10, 0, 0), fing=40), W("claws"),
             wings((0, -10, 30), (0, 0, 15), (0, 0, 10)), body=(14, 0, 0), head=(-10, 0, 0))
    rise = P(arms(sh=(0, 0, 10), up=(-20, 0, 55), fore=(-70, 0, 0), hand=(-20, 0, 0), fing=40), W("claws"),
             wings((0, -12, 38), (0, 0, 17), (0, 0, 11)), tendrils(-10, 0), body=(22, 0, 0), head=(-16, 0, 0), root={"p": [0, 3, 0]})
    pump = P(rise, wings((0, 20, -12), (0, 0, -18), (0, 0, -20)), body=(26, 0, 0), root={"p": [0, 1, 0]})
    aim_d = P(rise, body=(34, 0, 0), head=(-34, 0, 0))
    plunge = P(arms(sh=(-6, 0, 0), up=(-160, 0, 18), fore=(-8, 0, 0), hand=(-15, 0, 0), fing=40), W("claws"),
               wings((0, 62, 10), (0, 0, -8), (0, 0, -12)), tendrils(-40, 0), body=(62, 0, 0), head=(-30, 0, 0), root={"p": [0, 2, 0]})
    impact = P(arms(sh=(4, 0, 0), up=(-70, 0, 55), fore=(-30, 0, 0), hand=(-50, 0, 0), fing=40), W("claws"),
               wings((0, 35, -28), (0, 0, -20), (0, 0, -16)), tendrils(20, 0), body=(42, 0, 0), head=(16, 0, 0),
               root={"p": [0, -7, 0], "s": [1.06, 0.92, 1.06]})
    rebound = P(impact, root={"p": [0, -5, 0], "s": [1, 1, 1]}, body=(36, 0, 0))
    stage("dive", "windup", [Key(0, base), Key(5, bare, "easeOutCubic"), Key(10, rise, "easeInOutSine"), Key(14, pump, "easeInCubic"),
                             Key(18, rise, "easeOutCubic"), Key(22, pump, "easeInCubic"), Key(26, aim_d, "easeOutCubic"),
                             Key(30, aim_d, "linear")])
    stage("dive", "strike", [Key(0, aim_d), Key(3, plunge, "easeInCubic"), Key(8, plunge, "linear")])
    stage("dive", "recover", [Key(0, impact, "linear"), Key(3, rebound, "easeOutQuad"), Key(10, rebound, "linear"),
                              Key(24, base, "easeInOutSine")])

    # -------------------------------------------------- Eclipse Ray: the Focus disc forms, it kneels and burns the floor
    w, a_, r = MOVES["beam"]
    hold_f = P(arms(sh=(0, 0, 0), up=(-70, -30, 20), fore=(-60, 0, 0), hand=(0, 0, -20), fing=-10),
               wings((0, -5, 29), (0, 0, 11), (0, 0, 7)), body=(10, 0, 0), head=(14, 0, 0),
               weapon_focus={"p": [0, 0, 0], "r": [0, 0, 0]}, corona={"s": [1.17, 1.17, 1.17]})
    charge = P(arms(sh=(4, 0, 0), up=(-58, -36, 26), fore=(-70, 0, 0), hand=(-10, 0, -30), fing=-10),
               wings((0, -10, 34), (0, 0, 16), (0, 0, 11)), tendrils(10, 0), body=(28, 0, 0), head=(28, 0, 0), root={"p": [0, -2, 0]},
               weapon_focus={"p": [0, -10, -2], "r": [60, 0, 0]}, corona={"s": [1.3, 1.3, 1.3]}, orbit={"s": [0.7, 1, 0.7]})
    fire = P(arms(sh=(6, 0, 0), up=(-62, 0, 50), fore=(-12, 0, 0), hand=(-30, 0, 40), fing=-20),
             wings((0, -30, 30), (0, 0, 14), (0, 0, 8)), tendrils(14, 0), body=(34, 0, 0), head=(26, 0, 0), root={"p": [0, -4, 0]},
             weapon_focus={"p": [0, -14, -4], "r": [80, 0, 0]}, corona={"s": [1.41, 1.41, 1.41]}, orbit={"s": [1.4, 1, 1.4]})
    slump = P(arms(up=(12, 0, 6), fore=(-6, 0, 0), hand=(12, 0, 0), fing=0), wings((0, 28, -34), (0, 0, -16), (0, 0, -14)),
              tendrils(6, 0), body=(32, 0, 0), head=(44, 0, 0), root={"p": [0, -5, 0]},
              weapon_focus={"p": [0, -14, -4], "r": [80, 0, 0]})
    stage("beam", "windup", [Key(0, base), Key(8, hold_f, "easeOutCubic"), Key(24, charge, "easeInOutCubic"),
                             Key(30, P(charge, corona={"s": [1.34, 1.34, 1.34]}, root={"p": [0, -2.5, 0]}), "easeInOutSine")])
    A["beam_strike"] = build_clip(20, [Key(0, fire), Key(10, P(fire, body=(36, 0, 1), corona={"s": [1.5, 1.5, 1.5]}, root={"p": [0, -4.5, 0]})),
                                       Key(20, fire)], loop=True, amb=amb,
                                  weapon_tracks={"weapon_focus": [[0, [1, 1, 1], None], [0.5, [1.08, 1.08, 1.08], "easeInOutSine"],
                                                                  [1.0, [1, 1, 1], "easeInOutSine"]]})
    stage("beam", "recover", [Key(0, fire), Key(6, slump, "easeOutCubic"), Key(20, slump, "linear"), Key(30, base, "easeInOutSine")])

    # -------------------------------------------------- Umbral Nova: curls around the Focus, trembles, explodes outward
    w, a_, r = MOVES["nova"]
    gather = P(arms(up=(-60, -30, 10), fore=(-70, 0, 0), hand=(0, 0, -20), fing=-10), wings((0, -10, 30), (0, 0, 12), (0, 0, 8)),
               body=(6, 0, 0), head=(4, 0, 0), weapon_focus={"p": [0, 0, 4], "r": [0, 0, 0]}, corona={"s": [1.17, 1.17, 1.17]})
    curl = P(arms(sh=(6, 0, -8), up=(-72, -42, -14), fore=(-84, 0, 0), hand=(10, 0, -20), fing=50),
             wings((0, -62, 6), (0, 0, -10), (0, 0, -14)), tendrils(-6, 0), body=(36, 0, 0), head=(42, 0, 0), root={"p": [0, -1, 0]},
             weapon_focus={"p": [0, -4, 9], "r": [0, 0, 0], "s": [0.8, 0.8, 0.8]}, corona={"s": [1.3, 1.3, 1.3]},
             orbit={"s": [0.45, 0.6, 0.45]}, orbit_inner={"s": [0.45, 0.45, 0.45]})
    tremble = P(curl, body=(39, 0, 3), head=(44, -5, 0), corona={"s": [1.37, 1.37, 1.37]})
    tremble2 = P(curl, body=(38, 0, -3), head=(43, 5, 0), corona={"s": [1.41, 1.41, 1.41]})
    burst_n = P(arms(sh=(-8, 0, 12), up=(-28, 0, 100), fore=(0, 0, 0), hand=(0, 0, 40), fing=-30),
                wings((0, 4, 12), (0, 0, 12), (0, 0, 22)), tendrils(-20, 0), body=(-22, 0, 0), head=(-42, 0, 0), root={"p": [0, 3, 0]},
                weapon_focus={"p": [0, 6, -2], "r": [0, 0, 0]}, corona={"s": [1.66, 1.66, 1.66]},
                orbit={"s": [1.8, 1.2, 1.8]}, orbit_inner={"s": [1.8, 1.8, 1.8]})
    keys = [Key(0, base), Key(8, gather, "easeOutCubic"), Key(20, curl, "easeInOutCubic")]
    t = 20
    while t + 6 <= 50:
        t += 6
        keys.append(Key(min(t, 50), tremble if (t // 6) % 2 else tremble2, "easeInOutSine"))
    if keys[-1].tick < 50:
        keys.append(Key(50, tremble, "easeInOutSine"))
    stage("nova", "windup", keys)
    stage("nova", "strike", [Key(0, keys[-1].pose), Key(1.5, burst_n, "easeOutExpo"), Key(4, burst_n, "linear")])
    stage("nova", "recover", [Key(0, burst_n), Key(6, P(burst_n, body=(-14, 0, 0), head=(-30, 0, 0)), "easeOutCubic"),
                              Key(30, base, "easeInOutSine")])

    # -------------------------------------------------- Blink: folds into a sliver of shadow, unfolds behind the target
    w, a_, r = MOVES["blink"]
    fold = P(wings((0, -60, 4), (0, 0, -6), (0, 0, -8)), arms(up=(-40, -30, -12), fore=(-60, 0, 0), fing=40), body=(20, 0, 0),
             head=(30, 0, 0), root={"p": [0, 3, 0], "s": [0.85, 1.1, 0.85]})
    gone = P(fold, root={"p": [0, 7, 0], "s": [0.08, 1.7, 0.08]})
    appear = P(base, wings((0, -5, 30), (0, 0, 12), (0, 0, 7)), root={"p": [0, 2, 0], "s": [1.12, 0.92, 1.12]}, body=(-6, 0, 0))
    stage("blink", "windup", [Key(0, base), Key(4, fold, "easeOutCubic"), Key(8, gone, "easeInExpo")], wt=False)
    stage("blink", "strike", [Key(0, gone), Key(2, gone, "linear")], wt=False)
    stage("blink", "recover", [Key(0, gone), Key(3, appear, "easeOutExpo"), Key(8, base, "easeOutBack")], wt=False)

    # -------------------------------------------------- Umbral Grasp: the chain uncoils, lashes out and drags the target in
    w, a_, r = MOVES["grasp"]
    coil_c = P(arm("right", sh=(0, 0, 8), up=(-40, -20, 60), fore=(-60, 0, 0), hand=(-20, 0, 0), fing=60), W("chain", (0, 0, 0)),
               wings((0, -10, 24), (0, 0, 12), (0, 0, 8)), body=(4, 25, 0), head=(6, -20, 0), chain_end=(40, 0, 0))
    wind = P(arm("right", sh=(-6, 0, 14), up=(-150, -30, 40), fore=(-30, 0, 0), hand=(-20, 0, 0), fing=60), W("chain", (-60, 0, 0)),
             wings((0, -12, 28), (0, 0, 11), (0, 0, 7)), tendrils(-6, 0), body=(-10, 30, 0), head=(-6, -20, 0), chain_end=(-50, 0, 0))
    lash = P(arm("right", sh=(8, 0, 0), up=(-95, 10, 0), fore=(0, 0, 0), hand=(-10, 0, 0), fing=60), W("chain", (102, 0, 0)),
             wings((0, 10, 14), (0, 0, 0), (0, 0, -6)), tendrils(10, 0), body=(20, -15, 0), head=(4, 10, 0), chain_end=(0, 0, 0))
    yank = P(arm("right", sh=(-6, 0, 8), up=(-30, 30, 40), fore=(-95, 0, 0), hand=(30, 0, 0), fing=60), W("chain", (90, 0, 0)),
             wings((0, -20, 28), (0, 0, 12), (0, 0, 10)), tendrils(-12, 0), body=(-12, 22, 0), head=(-6, 14, 0), chain_end=(60, 0, 0),
             root={"p": [0, 2, 3]})
    stage("grasp", "windup", [Key(0, base), Key(8, coil_c, "easeOutCubic"), Key(18, wind, "easeInOutCubic"),
                              Key(24, P(wind, chain_end=(-70, 0, 0)), "easeInOutSine")])
    stage("grasp", "strike", [Key(0, P(wind, chain_end=(-70, 0, 0))), Key(2, lash, "easeInExpo"), Key(3, lash, "linear"),
                              Key(6, yank, "easeOutBack")])
    stage("grasp", "recover", [Key(0, yank), Key(5, P(yank, body=(-6, 14, 0)), "easeOutCubic"), Key(16, base, "easeInOutSine")])

    # -------------------------------------------------- specials
    def special(name, keys, **kw):
        A[name] = build_clip(SPECIALS[name] if name in SPECIALS else DEATH_TICKS, keys, amb=amb, **kw)

    hidden = P(root={"p": [0, 30, 0], "s": [0.02, 0.02, 0.02]}, corona={"s": [0.5, 0.5, 0.5]})
    huddle = P(wings((0, -66, -8), (0, 0, -16), (0, 0, -18)), arms(up=(-58, -40, -14), fore=(-84, 0, 0), hand=(10, 0, 0), fing=60),
               tendrils(10, 0), body=(42, 0, 0), head=(48, 0, 0), root={"p": [0, 8, 0], "s": [0.75, 0.75, 0.75]},
               corona={"s": [0.67, 0.67, 0.67]})
    heave = P(huddle, body=(46, 0, 0), root={"p": [0, 5, 0], "s": [0.86, 0.86, 0.86]}, corona={"s": [0.83, 0.83, 0.83]})
    unfurl = P(wings((0, 8, 6), (0, 0, 14), (0, 0, 24)), arms(sh=(-8, 0, 12), up=(-40, 0, 104), fore=(0, 0, 0), hand=(0, 0, 30), fing=-25),
               tendrils(-16, 0), body=(-18, 0, 0), head=(-42, 0, 0), root={"p": [0, 3, 0], "s": [1.04, 1.04, 1.04]},
               corona={"s": [1.5, 1.5, 1.5]}, orbit={"s": [2, 1, 2]})
    special("entrance", [Key(0, hidden), Key(20, huddle, "easeOutQuart"), Key(36, heave, "easeInOutSine"),
                         Key(46, huddle, "easeInOutSine"), Key(52, unfurl, "easeOutBack"), Key(62, unfurl, "linear"),
                         Key(70, base, "easeInOutCubic")])

    def transition(intensity):
        rear = P(arms(sh=(-6, 0, 10), up=(-24, 0, 44), fore=(-30, 0, 0), hand=(0, 0, 20), fing=-20), wings((0, -15, 33), (0, 0, 14), (0, 0, 10)),
                 tendrils(-10, 0), body=(-26, 0, 0), head=(-52, 0, 0), halo_shard_a={"p": [6 * intensity, 4, -4]},
                 halo_shard_b={"p": [-6 * intensity, 6, 3]}, corona={"s": [1 + 0.22 * intensity] * 3}, orbit={"s": [1.6, 1.3, 1.6]})
        rear2 = P(rear, wings((0, -13, 37), (0, 0, 18), (0, 0, 13)), body=(-23, 0, 4), head=(-46, 8, 0))
        crouch = P(huddle, root={"p": [0, 0, 0]}, corona={"s": [0.95, 0.95, 0.95]})
        end = P(base, corona={"s": [1 + 0.08 * (intensity - 1)] * 3})
        return [Key(0, base), Key(9, crouch, "easeOutCubic"), Key(20, P(crouch, body=(46, 0, 0)), "easeInOutSine"),
                Key(24, rear, "easeOutBack"), Key(30, rear2, "easeInOutSine"), Key(36, rear, "easeInOutSine"),
                Key(42, rear2, "easeInOutSine"), Key(48, rear, "easeInOutSine"), Key(60, end, "easeInOutCubic")]
    special("phase2", transition(1))
    special("phase3", transition(2))
    raise_s = P(arms(sh=(-8, 0, 10), up=(-150, 0, 40), fore=(-10, 0, 0), hand=(0, 0, 30), fing=-20), wings((0, -5, 35), (0, 0, 16), (0, 0, 11)),
                head=(-30, 0, 0), body=(-8, 0, 0), orbit_inner={"s": [1.8, 1.8, 1.8]})
    wrap = P(raise_s, arms(up=(-62, -28, -12), fore=(-74, 0, 0), hand=(10, 0, 0), fing=40),
             wings((0, -58, 18), (0, 0, 25), (0, 0, 20)), head=(24, 0, 0), body=(14, 0, 0))
    special("shield_raise", [Key(0, base), Key(12, raise_s, "easeOutCubic"), Key(17, raise_s, "linear"), Key(30, wrap, "easeInOutCubic")])
    roar = P(arms(sh=(-8, 0, 14), up=(-30, 0, 88), fore=(-20, 0, 0), hand=(0, 0, 40), fing=-30), wings((0, 6, 10), (0, 0, 12), (0, 0, 22)),
             tendrils(-14, 0), body=(-22, 0, 0), head=(-56, 0, 0), corona={"s": [1.55, 1.55, 1.55]}, root={"p": [0, 3, 0]})
    roar2 = P(roar, wings((0, 4, 4), (0, 0, 8), (0, 0, 18)), head=(-50, 6, 0), body=(-19, 0, 3))
    draw_in = P(arms(up=(-40, -20, -6), fore=(-70, 0, 0), fing=60), wings((0, -30, 10), (0, 0, -6), (0, 0, -8)),
                body=(26, 0, 0), head=(30, 0, 0))
    special("enrage", [Key(0, base), Key(8, draw_in, "easeOutCubic"), Key(11, roar, "easeOutExpo"), Key(15, roar2, "easeInOutSine"),
                       Key(19, roar, "easeInOutSine"), Key(23, roar2, "easeInOutSine"), Key(27, roar, "easeInOutSine"),
                       Key(40, base, "easeInOutCubic")])
    reel = P(arms(up=(24, 0, 30), fore=(-10, 0, 0), hand=(10, 0, 0), fing=-20), wings((0, 30, 30), (0, 0, 14), (0, 0, 10)),
             tendrils(-12, 0), body=(-30, 0, 0), head=(-42, 22, 0), root={"p": [0, 3, 2]})
    kneel = P(arms(up=(-14, 0, 24), fore=(-10, 0, 0), hand=(20, 0, 0), fing=30), wings((0, 36, -42), (0, 0, -20), (0, 0, -16)),
              tendrils(14, 0), body=(46, 0, 0), head=(52, 0, 0), root={"p": [0, -10, 0]},
              corona={"s": [0.89, 0.89, 0.89]}, orbit={"s": [0.6, 0.6, 0.6], "p": [0, -10, 0]})
    twitch = P(kneel, wing("right", "upper", (0, 36, -34), (0, 0, -10), (0, 0, -6)), head=(46, -12, 0))
    special("stagger", [Key(0, base), Key(4, reel, "easeOutExpo"), Key(16, kneel, "easeInCubic"), Key(19, P(kneel, root={"p": [0, -11, 0], "s": [1.04, 0.95, 1.04]}), "easeOutQuad"),
                        Key(26, kneel, "easeOutBack"), Key(36, twitch, "easeOutQuart"), Key(44, kneel, "easeInOutSine"),
                        Key(58, twitch, "easeOutQuart"), Key(66, kneel, "easeInOutSine"), Key(80, base, "easeInOutCubic")])
    g0 = P(kneel, root={"p": [0, -6, 0]})
    g1 = P(twitch, root={"p": [0, -5.5, 0]}, body=(44, 0, 0))
    A["grounded"] = build_clip(SPECIALS["grounded"], [Key(0, g0), Key(14, P(g0, body=(48, 0, 0)), "easeInOutSine"), Key(30, g1, "easeInOutSine"),
                                                      Key(44, P(g0, body=(47, 0, 0)), "easeInOutSine"), Key(60, g0, "easeInOutSine")],
                               loop=True, amb=amb)

    # death: convulses, the halo falls and splits, wings crumble, it sinks and dissolves into the void
    d1 = P(arms(sh=(-6, 0, 10), up=(-14, 0, 54), fore=(-20, 0, 0), hand=(0, 0, 30), fing=-30), wings((0, 0, 14), (0, 0, 12), (0, 0, 20)),
           body=(-36, 0, 0), head=(-62, 0, 0), corona={"s": [1.66, 1.66, 1.66]}, root={"p": [0, 4, 0]},
           halo={"p": [0, 4, 0]}, halo_shard_a={"p": [10, 8, -6]}, halo_shard_b={"p": [-10, 10, 6]})
    shrink = {}
    for k, v in wings((0, 50, -60), (0, 0, -30), (0, 0, -30)).items():
        shrink[k] = {"r": v["r"], "s": [0.6, 0.6, 0.6]}
    d2 = P(kneel, shrink, halo={"p": [0, -30, 0], "r": [70, 0, 30]}, halo_shard_a={"p": [14, -40, -8]},
           halo_shard_b={"p": [-12, -42, 9]}, corona={"s": [0.72, 0.72, 0.72]})
    gone_w = {k: {"r": v["r"], "s": [0, 0, 0]} for k, v in wings((0, 60, -70), (0, 0, -30), (0, 0, -30)).items()}
    d3 = P(d2, gone_w, root={"p": [0, -16, 0], "s": [0.6, 0.12, 0.6]}, corona={"s": [0.45, 0.45, 0.45]}, orbit={"s": [0, 0, 0]},
           orbit_inner={"s": [0, 0, 0]})
    A["death"] = build_clip(DEATH_TICKS, [Key(0, base), Key(6, reel, "easeOutExpo"), Key(24, d1, "easeOutCubic"),
                                          Key(30, P(d1, body=(-31, 8, 4)), "easeInOutSine"), Key(36, d1, "easeInOutSine"),
                                          Key(60, d2, "easeInCubic"), Key(64, P(d2, root={"p": [0, -11, 0], "s": [1.05, 0.9, 1.05]}), "easeOutQuad"),
                                          Key(84, P(d2, root={"p": [0, -12, 0], "s": [0.9, 0.6, 0.9]}), "easeInOutSine"),
                                          Key(100, d3, "easeInCubic")], amb=amb)
    return {"format_version": "1.8.0", "animations": A, "geckolib_format_version": 2}


def pylon_animations():
    lt = "query.life_time"
    idle = build_clip(80, [
        Key(0, {"crystal": {"p": [0, 0, 0], "s": [1, 1, 1]}}),
        Key(30, {"crystal": {"p": [0, 1.6, 0], "s": [1.04, 1.04, 1.04]}}, "easeInOutSine"),
        Key(36, {"crystal": {"p": [0, 1.8, 0], "s": [1.1, 1.1, 1.1]}}, "easeOutQuad"),     # heartbeat as it feeds the shield
        Key(42, {"crystal": {"p": [0, 1.6, 0], "s": [1.0, 1.0, 1.0]}}, "easeInQuad"),
        Key(80, {"crystal": {"p": [0, 0, 0], "s": [1, 1, 1]}}, "easeInOutSine")], loop=True, hidden=(), lag=False,
        amb={"crystal": {"rotation": ["0", f"{lt} * 70", "0"]},
             "ring": {"rotation": [f"math.sin({lt} * 45) * 6", f"{lt} * -50", "0"]}})
    return {"format_version": "1.8.0", "animations": {"idle": idle}, "geckolib_format_version": 2}


def echo_animations():
    lt = "query.life_time"
    amb = {"halo": {"rotation": ["0", f"{lt} * 60", "0"]}}
    for i in range(3):
        amb[f"tendril_{i}"] = {"rotation": [f"math.sin({lt} * 150 + {i * 70}) * 10", "0", f"math.cos({lt} * 110 + {i * 70}) * 6"]}
        amb[f"tendril_{i}_tip"] = {"rotation": [f"math.sin({lt} * 150 + {i * 70 + 50}) * 16", "0", "0"]}

    def P(**kw):
        return merge({"root": {"p": [0, 0, 0]}, "body": (6, 0, 0), "head": (4, 0, 0),
                      "arm_right": (-10, 0, 10), "arm_left": (-10, 0, -10), "forearm_right": (-20, 0, 0), "forearm_left": (-20, 0, 0),
                      "wing_right": (0, 10, 10), "wing_left": (0, -10, -10), "wing_right_tip": (0, 5, -10), "wing_left_tip": (0, -5, 10)}, kw)

    idle = {"loop": True, "animation_length": 6, "bones": dict(amb)}
    idle["bones"].update({
        "root": {"position": ["0", f"math.sin({lt} * 120) * 1.2 + math.sin({lt} * 47) * 0.4", "0"]},
        "body": {"rotation": [f"6 + math.sin({lt} * 90) * 3", "0", f"math.sin({lt} * 61) * 2"]},
        "head": {"rotation": [f"4 - math.sin({lt} * 90) * 3", f"math.sin({lt} * 37) * 14", "0"]},
        "skirt": {"rotation": [f"math.sin({lt} * 120 + 40) * 4", "0", "0"]},
    })
    for side, g in (("right", 1), ("left", -1)):
        idle["bones"][f"arm_{side}"] = {"rotation": [f"-10 + math.sin({lt} * 90 + {35 * (g + 1)}) * 6", "0", f"{g * 10}"]}
        idle["bones"][f"forearm_{side}"] = {"rotation": [f"-20 + math.sin({lt} * 90 + {35 * (g + 1) + 40}) * 8", "0", "0"]}
        idle["bones"][f"wing_{side}"] = {"rotation": ["0", f"{g} * (10 + math.sin({lt} * 240) * 12)", f"{g} * (10 + math.sin({lt} * 240) * 10)"]}
        idle["bones"][f"wing_{side}_tip"] = {"rotation": ["0", f"{g} * (5 + math.sin({lt} * 240 - 50) * 14)", f"{-g} * (10 + math.sin({lt} * 240 - 50) * 8)"]}
    rest = P()
    coil = P(body=(-12, 25, 0), head=(-6, -15, 0), arm_right=(-150, 20, 30), forearm_right=(-50, 0, 0), arm_left=(-30, 0, -30),
             wing_right=(0, -20, 35), wing_left=(0, 20, -35), root={"p": [0, 1, 1]})
    slash = P(body=(28, -30, 0), head=(14, 20, 0), arm_right=(-40, -40, -20), forearm_right=(-5, 0, 0), arm_left=(10, 0, -40),
              wing_right=(0, 35, -5), wing_left=(0, -35, 5), wing_right_tip=(0, 20, 10), wing_left_tip=(0, -20, -10), root={"p": [0, -1, -2]})
    attack = build_clip(12, [Key(0, rest), Key(2.5, coil, "easeOutCubic"), Key(4, slash, "easeInExpo"), Key(6.5, slash, "linear"),
                             Key(12, rest, "easeInOutSine")], amb=amb, hidden=(), lag=False)
    recoil = P(body=(-30, 0, 0), head=(-40, 0, 0), arm_right=(30, 0, 50), arm_left=(30, 0, -50), wing_right=(0, -30, 50),
               wing_left=(0, 30, -50), root={"p": [0, 2, 1]})
    fade = P(body=(40, 0, 0), head=(50, 0, 0), wing_right=(0, 40, -40), wing_left=(0, -40, 40), root={"p": [0, 4, 0], "s": [0.4, 1.5, 0.4]})
    gone = P(body=(40, 0, 0), head=(50, 0, 0), root={"p": [0, 9, 0], "s": [0, 2.2, 0]})
    death = build_clip(20, [Key(0, rest), Key(3, recoil, "easeOutExpo"), Key(12, fade, "easeInOutSine"), Key(20, gone, "easeInBack")],
                       amb=amb, hidden=(), lag=False)
    return {"format_version": "1.8.0", "animations": {"idle": idle, "attack": attack, "death": death}, "geckolib_format_version": 2}


# ------------------------------------------------------------------ textures + output

def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def textures(m, base_path=None, glow_path=None):
    colours = {k: png.hexc(c) for k, c in PALETTE.items()}
    base, glow = png.blank(m.tw, m.th), png.blank(m.tw, m.th)
    for n, (u, v, w, h, d, material) in enumerate(m.layout):
        paint = MATERIALS[material]
        for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    letter, g = paint(face, x, y, fw, fh, n)
                    if letter is None:
                        continue
                    base[fy + y][fx + x] = colours[letter]
                    if g:
                        glow[fy + y][fx + x] = colours[letter]
    out = os.path.join(ASSETS, "textures", "entity", "eclipse")
    os.makedirs(out, exist_ok=True)
    paths = (base_path or os.path.join(out, m.name + ".png"), glow_path or os.path.join(out, m.name + "_glowmask.png"))
    for path, img in zip(paths, (base, glow)):
        n = len(png.colours(img))
        assert n <= 8, f"{path}: {n} colours"
        png.write(path, img)
    return base, glow


def emissive_share(m, glow, skip=()):
    """Visible emissive fraction over all cube faces (weapons and sparks excluded - they are hidden at rest)."""
    total = lit = 0
    for b in m.bones:
        if any(b["name"].startswith(s) for s in skip) or any(p in skip for p in ancestors(m, b)):
            continue
        for c in b["cubes"]:
            w, h, d = (int(math.ceil(s)) for s in c["size"])
            u, v = c["uv"]
            for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
                for y in range(fh):
                    for x in range(fw):
                        total += 1
                        lit += glow[fy + y][fx + x][3] > 0
    return lit / max(1, total)


def ancestors(m, b):
    out = []
    while b.get("parent"):
        out.append(b["parent"])
        b = m.by_name[b["parent"]]
    return out


def write_json(rel_dirs, name, data, compact=False):
    for d in rel_dirs:
        path = os.path.join(ASSETS, *d.split("/"), name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            if compact:     # one clip per line: the Eclipse's clips are large
                anims = data["animations"]
                f.write('{"format_version":"1.8.0","geckolib_format_version":2,"animations":{\n')
                f.write(",\n".join(json.dumps(k) + ":" + json.dumps(v, separators=(",", ":")) for k, v in anims.items()))
                f.write("\n}}\n")
            else:
                json.dump(data, f, indent=1)
                f.write("\n")


GEO_DIRS = ("geo/entity", "geckolib/models/entity")
ANIM_DIRS = ("animations/entity", "geckolib/animations/entity")


def main():
    e = build_eclipse()
    write_json(GEO_DIRS, "eclipse.geo.json", geo(e, 9, 8, 3))
    write_json(ANIM_DIRS, "eclipse.animation.json", animations(), compact=True)
    _, eg = textures(e)
    p = build_pylon()
    write_json(GEO_DIRS, "umbral_pylon.geo.json", geo(p, 2, 2.5, 1))
    write_json(ANIM_DIRS, "umbral_pylon.animation.json", pylon_animations())
    _, pg = textures(p)
    ec = build_echo()
    write_json(GEO_DIRS, "eclipse_echo.geo.json", geo(ec, 2, 3.5, 1.25))
    write_json(ANIM_DIRS, "eclipse_echo.animation.json", echo_animations())
    _, xg = textures(ec, base_path=os.path.join(ASSETS, "textures", "entity", "eclipse_echo.png"))
    hide = ("weapon_", "spark_")
    for m, g in ((e, eg), (p, pg), (ec, xg)):
        print(f"{m.name}: {sum(len(b['cubes']) for b in m.bones)} cubes, {len(m.bones)} bones, {len(m.layout)} uv boxes, "
              f"cursor {m.cursor}, emissive {emissive_share(m, g, hide) * 100:.1f} %")
    print("weapons:", {w: sum(len(e.by_name[b]['cubes']) for b in e.by_name if b.startswith('weapon_' + w) or
                                 (b in ('orb_cage', 'chain_end') and w in b)) for w in WEAPON_BONES})
    print("easings:", sorted(EASINGS_USED))


if __name__ == "__main__":
    main()
