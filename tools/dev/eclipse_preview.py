"""Software preview for gen_eclipse.py: renders the generated GeckoLib models with their textures, glowmasks and
animations (keyframes with easing, Molang ambient layers) from the front, the side and three-quarter, so key poses and
silhouettes can be judged without starting the game. Transform conventions match docs/realm-viewer (and GeckoLib).

Run: python -I tools/dev/eclipse_preview.py <out_dir> [model] [clip[@t1,t2,...] ...]
     model: eclipse (default), eclipse_echo, umbral_pylon. Without clips a default set of key poses is rendered.
"""
import json
import math
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402
from gen_eclipse import ASSETS, ease_fn  # noqa: E402

SCALE = 6
BG = (92, 98, 122)


def load(model):
    geo = json.load(open(os.path.join(ASSETS, "geo", "entity", model + ".geo.json")))["minecraft:geometry"][0]
    anims = json.load(open(os.path.join(ASSETS, "animations", "entity", model + ".animation.json")))["animations"]
    tex_dir = os.path.join(ASSETS, "textures", "entity")
    base = os.path.join(tex_dir, model + ".png")
    if not os.path.exists(base):
        base = os.path.join(tex_dir, "eclipse", model + ".png")
    glow = os.path.join(tex_dir, "eclipse", model + "_glowmask.png")
    return geo, anims, png.read(base)[2], png.read(glow)[2] if os.path.exists(glow) else None


# ------------------------------------------------------------------ Molang + keyframes

ENV = {"m_sin": lambda a: math.sin(math.radians(a)), "m_cos": lambda a: math.cos(math.radians(a)), "m_abs": abs,
       "m_min": min, "m_max": max, "m_sqrt": math.sqrt, "m_pi": math.pi}
_cache = {}


def molang(v, t):
    if isinstance(v, (int, float)):
        return float(v)
    src = _cache.get(v)
    if src is None:
        s = re.sub(r"query\.(anim_time|life_time)", "_t", v).replace("math.", "m_")
        src = _cache[v] = compile(s, "<molang>", "eval")
    return float(eval(src, ENV, {"_t": t}))


def channel(spec, t, life):
    """Value of a channel at clip time t (life = query.life_time)."""
    if isinstance(spec, list):
        return [molang(x, life) for x in spec]
    keys = sorted(((float(k), v) for k, v in spec.items()), key=lambda kv: kv[0])

    def vec(entry):
        e = entry if isinstance(entry, list) else entry.get("post") or entry.get("vector") or entry.get("pre")
        return [molang(x, life) for x in e]
    if t <= keys[0][0]:
        return vec(keys[0][1])
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t <= t1:
            s = (t - t0) / max(1e-6, t1 - t0)
            e = b.get("easing", "linear") if isinstance(b, dict) else "linear"
            s = ease_fn(e)(s)
            va, vb = vec(a), vec(b)
            return [x + (y - x) * s for x, y in zip(va, vb)]
    return vec(keys[-1][1])


def pose_at(anim, t, life=None):
    life = t + 3.0 if life is None else life
    out = {}
    for bone, chans in anim["bones"].items():
        out[bone] = {ch: channel(spec, t, life) for ch, spec in chans.items()}
    return out


# ------------------------------------------------------------------ transforms (mirrored-x world, like the atlas)

def mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mv(a, v):
    return [sum(a[i][k] * v[k] for k in range(3)) for i in range(3)]


def rot(rx, ry, rz):
    """Geo rotation (degrees) -> mirrored-world matrix Rz(rz) Ry(-ry) Rx(-rx)."""
    ax, ay, az = math.radians(-rx), math.radians(-ry), math.radians(rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    X = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mm(Z, mm(Y, X))


class Xf:
    def __init__(self, m=None, t=(0, 0, 0)):
        self.m = m or [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        self.t = list(t)

    def __mul__(self, o):
        return Xf(mm(self.m, o.m), [a + b for a, b in zip(mv(self.m, o.t), self.t)])

    def apply(self, p):
        return [a + b for a, b in zip(mv(self.m, p), self.t)]


def about(pivot, m):
    """Transform rotating/scaling with matrix m around pivot."""
    return Xf(m, [pivot[i] - mv(m, pivot)[i] for i in range(3)])


def bone_xfs(geo, pose):
    bones = {b["name"]: b for b in geo["bones"]}
    xfs = {}

    def get(name):
        if name in xfs:
            return xfs[name]
        b = bones[name]
        p = pose.get(name, {})
        r0 = b.get("rotation", [0, 0, 0])
        r = p.get("rotation", [0, 0, 0])
        o = p.get("position", [0, 0, 0])
        s = p.get("scale", [1, 1, 1])
        piv = [-b["pivot"][0], b["pivot"][1], b["pivot"][2]]
        R = rot(r0[0] + r[0], r0[1] + r[1], r0[2] + r[2])
        RS = mm(R, [[s[0], 0, 0], [0, s[1], 0], [0, 0, s[2]]])
        local = Xf([[1, 0, 0], [0, 1, 0], [0, 0, 1]], [-o[0], o[1], o[2]]) * about(piv, RS)
        xf = get(b["parent"]) * local if b.get("parent") else local
        xfs[name] = xf
        return xf
    for n in bones:
        get(n)
    return xfs


# ------------------------------------------------------------------ rasterising (splats with a z-buffer)

def face_samples(x0, y0, z0, x1, y1, z1, face, fw, fh, sub):
    """(i, j, point, normal) for each sub-texel of a face; i/j texel coords on the face's UV rect."""
    for j in range(fh):
        for i in range(fw):
            for a in range(sub):
                for b in range(sub):
                    u = (i + (a + 0.5) / sub) / fw
                    v = (j + (b + 0.5) / sub) / fh
                    if face == "front":
                        yield i, j, (x1 - u * (x1 - x0), y1 - v * (y1 - y0), z0), (0, 0, -1)
                    elif face == "back":
                        yield i, j, (x0 + u * (x1 - x0), y1 - v * (y1 - y0), z1), (0, 0, 1)
                    elif face == "east":
                        yield i, j, (x1, y1 - v * (y1 - y0), z1 - u * (z1 - z0)), (1, 0, 0)
                    elif face == "west":
                        yield i, j, (x0, y1 - v * (y1 - y0), z0 + u * (z1 - z0)), (-1, 0, 0)
                    elif face == "top":
                        yield i, j, (x1 - u * (x1 - x0), y1, z1 - v * (z1 - z0)), (0, 1, 0)
                    else:
                        yield i, j, (x1 - u * (x1 - x0), y0, z0 + v * (z1 - z0)), (0, -1, 0)


def uv_rects(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


LIGHT = (0.45, 0.8, -0.4)


def render(geo, tex, glow, pose, yaws, sub=2):
    """Returns {yaw: (points)} with points (sx, sy, depth, rgb)."""
    xfs = bone_xfs(geo, pose)
    ln = math.sqrt(sum(c * c for c in LIGHT))
    L = [c / ln for c in LIGHT]
    views = {y: [] for y in yaws}
    rots = {y: (math.cos(math.radians(y)), math.sin(math.radians(y))) for y in yaws}
    for b in geo["bones"]:
        xf = xfs[b["name"]]
        if abs(xf.m[0][0]) + abs(xf.m[1][1]) + abs(xf.m[2][2]) + abs(xf.m[0][1]) + abs(xf.m[1][0]) < 1e-3:
            continue  # scaled to nothing
        for c in b.get("cubes", []):
            o, s = c["origin"], c["size"]
            x0, x1 = -o[0] - s[0], -o[0]
            y0, y1, z0, z1 = o[1], o[1] + s[1], o[2], o[2] + s[2]
            cxf = xf
            if c.get("rotation"):
                cp = c["pivot"]
                cxf = xf * about([-cp[0], cp[1], cp[2]], rot(*c["rotation"]))
            w, h, d = (int(math.ceil(v)) for v in s)
            u0, v0 = c["uv"]
            for face, (fx, fy, fw, fh) in uv_rects(u0, v0, w, h, d).items():
                n = None
                for i, j, p, nrm in face_samples(x0, y0, z0, x1, y1, z1, face, fw, fh, sub):
                    px = tex[fy + j][fx + i]
                    if px[3] == 0:
                        continue
                    if n is None:
                        n = mv(cxf.m, nrm)
                        nl = math.sqrt(sum(q * q for q in n)) or 1
                        n = [q / nl for q in n]
                        lit = 0.5 + 0.5 * max(0.0, sum(a * b for a, b in zip(n, L)))
                    g = glow[fy + j][fx + i] if glow else (0, 0, 0, 0)
                    if g[3]:
                        col = tuple(min(255, int(ch * 1.1 + 35)) for ch in px[:3])
                    else:
                        col = tuple(int(ch * lit) for ch in px[:3])
                    wp = cxf.apply(p)
                    for yaw, (cy, sy) in rots.items():
                        # camera looks along +z (front view), yaw turns the model; screen x = -world x
                        x = wp[0] * cy - wp[2] * sy
                        z = wp[0] * sy + wp[2] * cy
                        views[yaw].append((-x, wp[1], z, col))
    return views


def raster(points, bounds, scale=SCALE):
    x0, y0, x1, y1 = bounds
    w, h = int((x1 - x0) * scale), int((y1 - y0) * scale)
    img = [[BG + (255,)] * w for _ in range(h)]
    zb = [[1e9] * w for _ in range(h)]
    r = max(2, int(scale * 0.75))
    for sx, sy, z, col in points:
        cx, cy = int((sx - x0) * scale), int((y1 - sy) * scale)
        for yy in range(cy - r // 2, cy + r - r // 2):
            if 0 <= yy < h:
                row, zr = img[yy], zb[yy]
                for xx in range(cx - r // 2, cx + r - r // 2):
                    if 0 <= xx < w and z < zr[xx]:
                        zr[xx] = z
                        row[xx] = col + (255,)
    # ground line at y = 0
    gy = int(y1 * scale)
    if 0 <= gy < h:
        for xx in range(w):
            if img[gy][xx][:3] == BG:
                img[gy][xx] = (70, 64, 96, 255)
    return img


def sheet(rows, gap=4):
    """rows: list of lists of images -> one image."""
    hs = [max(len(i) for i in r) for r in rows]
    ws = [sum(len(i[0]) for i in r) + gap * (len(r) - 1) for r in rows]
    W, H = max(ws), sum(hs) + gap * (len(rows) - 1)
    out = [[(10, 10, 16, 255)] * W for _ in range(H)]
    y = 0
    for r, hh in zip(rows, hs):
        x = 0
        for img in r:
            for j, line in enumerate(img):
                out[y + j][x:x + len(line)] = line
            x += len(img[0]) + gap
        y += hh + gap
    return out


DEFAULT = {
    "eclipse": ["idle@0", "idle_shielded@0", "sweep_windup@0.6,0.9", "sweep_strike@0.15,0.3", "lance_windup@0.4,1.3",
                "lance_strike@0.1", "starfall_windup@0.8", "starfall_strike@0.15", "summon_windup@1.0", "summon_strike@0.1",
                "dive_windup@0.3,1.5", "dive_strike@0.3", "dive_recover@0.05", "beam_windup@1.5", "beam_strike@0.2",
                "nova_windup@1.2", "nova_strike@0.1", "grasp_windup@1.0,1.2", "grasp_strike@0.1,0.3", "entrance@1.2,2.6",
                "phase2@1.5", "enrage@0.6", "stagger@1.0", "grounded@0", "death@1.3,3.2,4.6", "blink_windup@0.3"],
    "eclipse_echo": ["idle@0", "attack@0.12,0.25", "death@0.3,0.8"],
    "umbral_pylon": ["idle@0,1.8"],
}
BOUNDS = {"eclipse": (-46, -4, 46, 84), "eclipse_echo": (-14, -6, 14, 30), "umbral_pylon": (-12, -2, 12, 32)}


def main():
    out = sys.argv[1]
    model = sys.argv[2] if len(sys.argv) > 2 else "eclipse"
    clips = sys.argv[3:] or DEFAULT[model]
    os.makedirs(out, exist_ok=True)
    geo, anims, tex, glow = load(model)
    yaws = (0, 90, 35)
    for spec in clips:
        name, _, times = spec.partition("@")
        ts = [float(t) for t in times.split(",")] if times else [0.0]
        rows = []
        for t in ts:
            pose = pose_at(anims[name], t)
            views = render(geo, tex, glow, pose, yaws)
            rows.append([raster(views[y], BOUNDS[model]) for y in yaws])
        path = os.path.join(out, f"{model}_{name}.png")
        png.write(path, sheet(rows))
        print(path)


if __name__ == "__main__":
    main()
