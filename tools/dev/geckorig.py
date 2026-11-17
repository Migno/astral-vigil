"""Shared helpers for the GeckoLib asset generators (gen_watcher.py, gen_seraph_wings.py): a box-UV rig builder,
per-face texture painting with tone ramps, keyframe animations with GeckoLib easing, and JSON/geo writers.

Conventions (Bedrock geo space): +Y up, the model faces -Z, the figure's right side is -X. Rotations are degrees.
Box UV strips: top (u+d, v), bottom (u+d+w, v), right side (u, v+d), front (u+d, v+d), left side (u+d+w, v+d),
back (u+2d+w, v+d) - "right"/"left" are the figure's own sides (identical to the vanilla player skin layout).

Animation easing follows GeckoLib: the easing stored on a keyframe shapes the segment that ENDS at that keyframe.
Only easings that GeckoLib 4 and 5 both know are used (see EASINGS).
"""
import json
import math
import os

import png


# ------------------------------------------------------------------ easing (GeckoLib / easings.net formulas)

def _back(t, c1=1.70158):
    return t * t * ((c1 + 1) * t - c1)


def _out(f):
    return lambda t: 1 - f(1 - t)


def _in_out(f):
    return lambda t: f(2 * t) / 2 if t < 0.5 else 1 - f(2 * (1 - t)) / 2


_IN = {
    "Sine": lambda t: 1 - math.cos(t * math.pi / 2),
    "Quad": lambda t: t * t,
    "Cubic": lambda t: t * t * t,
    "Quart": lambda t: t ** 4,
    "Back": _back,
}
EASINGS = {"linear": lambda t: t}
for _n, _f in _IN.items():
    EASINGS["easeIn" + _n] = _f
    EASINGS["easeOut" + _n] = _out(_f)
    EASINGS["easeInOut" + _n] = _in_out(_f)


def ease(name, t):
    return EASINGS[name](max(0.0, min(1.0, t)))


# ------------------------------------------------------------------ rig

class Rig:
    """Bones with box-UV cubes. Cubes with the same size and material share a UV box (mirror pairs)."""

    def __init__(self, tw, th):
        self.tw, self.th = tw, th
        self.bones = []
        self.by_name = {}
        self.uv = {}
        self.cursor = [0, 0, 0]
        self.layout = []  # (u, v, w, h, d, material)
        self.cube_list = []  # (bone, w, h, d, material)

    def bone(self, name, parent, pivot, rotation=None):
        b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
        if parent:
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
        assert v + uh <= self.th, f"texture full at {material} {w}x{h}x{d}"
        self.cursor[0] += uw
        self.cursor[2] = max(self.cursor[2], uh)
        self.uv[key] = (u, v)
        self.layout.append((u, v, w, h, d, material))
        return u, v

    def cube(self, bone, origin, size, material, pivot=None, rotation=None):
        for s in size:
            assert s >= 0.5 and abs(s * 2 - round(s * 2)) < 1e-6, f"size {size} ({material}) breaks texel density"
        w, h, d = size
        uw, uh, ud = (int(math.ceil(s)) for s in size)
        u, v = self.alloc(uw, uh, ud, material)
        c = {"origin": [round(o, 3) for o in origin], "size": list(size), "uv": [u, v]}
        if rotation and any(rotation):
            c["pivot"] = [round(p, 3) for p in pivot]
            c["rotation"] = [round(r, 3) for r in rotation]
        bone["cubes"].append(c)
        self.cube_list.append((bone["name"], uw, uh, ud, material))
        return c

    def count(self):
        return sum(len(b["cubes"]) for b in self.bones)

    def geo(self, identifier, bounds_w, bounds_h, offset_y):
        bones = []
        for b in self.bones:
            b = dict(b)
            if not b["cubes"]:
                del b["cubes"]
            bones.append(b)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": identifier, "texture_width": self.tw, "texture_height": self.th,
                            "visible_bounds_width": bounds_w, "visible_bounds_height": bounds_h,
                            "visible_bounds_offset": [0, offset_y, 0]},
            "bones": bones}]}


def face_rects(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Face:
    """What a material painter sees: face name, pixel (x, y), face size (w, h) and the cube size (cw, ch, cd)."""
    __slots__ = ("face", "x", "y", "w", "h", "cw", "ch", "cd")

    def __init__(self, face, x, y, w, h, cw, ch, cd):
        self.face, self.x, self.y, self.w, self.h, self.cw, self.ch, self.cd = face, x, y, w, h, cw, ch, cd

    @property
    def side(self):
        return self.face in ("front", "back", "right", "left")

    @property
    def edge(self):
        return self.x == 0 or self.y == 0 or self.x == self.w - 1 or self.y == self.h - 1


# base tone per face: light from the viewer's top-left (= the figure's top and right side when seen from the front)
FACE_TONE = {"top": 1, "front": 0, "right": 0, "left": -1, "back": -1, "bottom": -2}


def shade(f, base=2, levels=4, rim=True, ao=True, ao_rows=1):
    """Generic ramp index for a face pixel: face orientation, a rim highlight on the top row, ambient occlusion on
    the bottom rows and a darker silhouette column on the shadowed edge. Returns 0..levels-1."""
    t = base + FACE_TONE[f.face]
    if f.side:
        if rim and f.y == 0:
            t += 1
        if ao and f.y >= f.h - ao_rows:
            t -= 1
        if f.face == "front" and f.x == f.w - 1 and f.w > 2:
            t -= 1
        if f.face == "front" and f.x == 0 and f.w > 2 and f.y < f.h // 2:
            t += 1
    return max(0, min(levels - 1, t))


def paint(rig, palettes, materials, out_dir, name_fmt, max_colours=32):
    """Paints every UV box with its material for each palette; writes <name>.png and <name>_glowmask.png.
    A painter returns (colour key or None for transparent, glow key or None). Returns emissive coverage per palette."""
    stats = {}
    for pid, pal in palettes.items():
        colours = {k: png.hexc(c) for k, c in pal.items()}
        base, glow = png.blank(rig.tw, rig.th), png.blank(rig.tw, rig.th)
        box_info = {}
        for u, v, w, h, d, material in rig.layout:
            painter = materials[material]
            opaque = lit = 0
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                area_o = area_g = 0
                for y in range(fh):
                    for x in range(fw):
                        b, g = painter(Face(face, x, y, fw, fh, w, h, d), pid)
                        if b is None:
                            continue
                        base[fy + y][fx + x] = colours[b]
                        area_o += 1
                        if g:
                            glow[fy + y][fx + x] = colours[g]
                            area_g += 1
                if face != "bottom":
                    opaque += area_o
                    lit += area_g
            box_info[(w, h, d, material)] = (opaque, lit)
        total = sum(box_info[(w, h, d, m)][0] for _, w, h, d, m in rig.cube_list)
        emissive = sum(box_info[(w, h, d, m)][1] for _, w, h, d, m in rig.cube_list)
        stats[pid] = emissive / max(1, total)
        for name, img in ((name_fmt.format(pid), base), (name_fmt.format(pid) + "_glowmask", glow)):
            n = len(png.colours(img))
            assert n <= max_colours, f"{name}: {n} colours"
            path = os.path.join(out_dir, name + ".png")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            png.write(path, img)
    return stats


# ------------------------------------------------------------------ animation

def fmt_t(t):
    s = f"{round(t, 4):.4f}".rstrip("0")
    return s + "0" if s.endswith(".") else s


def _vec(v):
    return [round(float(x), 3) for x in v]


class Anim:
    """Keyframed clip. Channel values are offsets from the rest pose (rotation degrees, position units, scale factor)."""

    def __init__(self, length, loop=False):
        self.length = length
        self.loop = loop
        self.tracks = {}  # bone -> channel -> {t: (vec, easing)}

    def key(self, bone, channel, t, vec, easing="easeInOutSine"):
        t = round(max(0.0, min(self.length, t)), 4)
        self.tracks.setdefault(bone, {}).setdefault(channel, {})[t] = (_vec(vec), easing)

    def keys(self, bone, channel, frames):
        for fr in frames:
            self.key(bone, channel, *fr)

    def osc(self, bone, channel, period, amp, phase=0.0, offset=(0, 0, 0)):
        """Loop-safe oscillation offset + amp * cos(2pi (t - phase) / period): keyframes on every extreme eased
        with easeInOutSine (a sine without molang), plus seam keys at 0 and at the clip end.
        The clip length must be a whole multiple of the period."""
        n = self.length / period
        assert abs(n - round(n)) < 1e-6, f"{bone}.{channel}: period {period} does not divide {self.length}"

        def val(t):
            c = math.cos(2 * math.pi * (t - phase) / period)
            return [o + a * c for o, a in zip(offset, amp)]

        half = period / 2
        first = phase % half
        partial = first > 1e-6  # the clip starts mid-motion: first segment decelerates, last one accelerates
        self.key(bone, channel, 0, val(0), "linear")
        t, i = first, 0
        while t < self.length - 1e-6:
            if t > 1e-6:
                self.key(bone, channel, t, val(t), "easeOutSine" if i == 0 and partial else "easeInOutSine")
            t += half
            i += 1
        self.key(bone, channel, self.length, val(self.length), "easeInSine" if partial else "easeInOutSine")

    def pose_track(self, timeline, lag=None, scale=None):
        """timeline: [(t, {bone: {channel: vec}}, easing)]. Every bone/channel named anywhere gets a key at every
        timeline time (missing = rest). lag {bone: seconds} delays a bone (overlapping action / follow-through),
        scale {bone: factor} damps its amplitude."""
        lag, scale = lag or {}, scale or {}
        names = {}
        for _, pose, _ in timeline:
            for b, chans in pose.items():
                for c in chans:
                    names.setdefault(b, set()).add(c)
        for b, chans in names.items():
            dt, k = lag.get(b, 0.0), scale.get(b, 1.0)
            for c in chans:
                rest = [1, 1, 1] if c == "scale" else [0, 0, 0]
                for i, (t, pose, easing) in enumerate(timeline):
                    v = pose.get(b, {}).get(c, rest)
                    if c != "scale":
                        v = [x * k for x in v]
                    tt = t if i == 0 or t >= self.length - 1e-6 else t + dt
                    self.key(b, c, min(tt, self.length), v, easing)

    def sample(self, bone, channel, t):
        frames = self.tracks.get(bone, {}).get(channel)
        if not frames:
            return None
        times = sorted(frames)
        if t <= times[0]:
            return frames[times[0]][0]
        for a, b in zip(times, times[1:]):
            if t <= b:
                va, vb = frames[a][0], frames[b][0]
                k = ease(frames[b][1], (t - a) / (b - a) if b > a else 1)
                return [x + (y - x) * k for x, y in zip(va, vb)]
        return frames[times[-1]][0]

    def to_json(self):
        bones = {}
        for b, chans in self.tracks.items():
            bones[b] = {}
            for c, frames in chans.items():
                out = {}
                for t in sorted(frames):
                    vec, easing = frames[t]
                    kf = {"vector": vec}
                    if easing != "linear":
                        kf["easing"] = easing
                    out[fmt_t(t)] = kf
                bones[b][c] = out
        d = {"animation_length": self.length, "bones": bones}
        if self.loop:
            d = {"loop": True, **d}
        return d


def animation_file(anims):
    return {"format_version": "1.8.0", "animations": {k: a.to_json() for k, a in anims.items()},
            "geckolib_format_version": 2}


def write_json(assets, rel_dirs, name, data):
    for d in rel_dirs:
        path = os.path.join(assets, *d.split("/"), name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=1)
            f.write("\n")
