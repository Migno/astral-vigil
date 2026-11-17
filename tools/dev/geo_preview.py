"""Software preview of a GeckoLib geo model (textured, with glowmask) in any animation pose - to judge silhouettes and
key poses without starting the game.

Run: python -I tools/dev/geo_preview.py <geo.json> <texture.png> <out.png> [--glow glow.png] [--anim file.json]
        [--pose clip@seconds ...] [--views 0,90,180,35] [--scale 4] [--silhouette]
Renders a contact sheet: one row per pose, one column per view (yaw in degrees, 0 = front). With --silhouette the
model is drawn flat black on white (the 30-blocks-in-the-dark read).
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402
from geckorig import ease  # noqa: E402


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def rot_geo(rx, ry, rz):
    """Bedrock/GeckoLib rotation (degrees) as a right-handed matrix in geo space: Rz(-rz) Ry(ry) Rx(-rx)."""
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(-rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mat_mul(mz, mat_mul(my, mx))


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


class Xf:
    """Affine transform p -> m p + t."""

    def __init__(self, m=None, t=(0, 0, 0)):
        self.m = m or [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        self.t = list(t)

    def of(self, p):
        q = apply(self.m, p)
        return [q[i] + self.t[i] for i in range(3)]

    def compose(self, other):
        """self after other."""
        return Xf(mat_mul(self.m, other.m), self.of(other.t))


def local(pivot, rot, pos=(0, 0, 0), scale=(1, 1, 1)):
    """T(pos) T(pivot) R S T(-pivot)."""
    r = rot_geo(*rot)
    m = [[r[i][j] * scale[j] for j in range(3)] for i in range(3)]
    mp = apply(m, pivot)
    return Xf(m, [pivot[i] + pos[i] - mp[i] for i in range(3)])


# ------------------------------------------------------------------ animation sampling (GeckoLib keyframes)

def parse_anim(clip):
    out = {}
    for bone, chans in clip.get("bones", {}).items():
        for ch, frames in chans.items():
            if not isinstance(frames, dict):
                continue
            keys = []
            for t, kf in frames.items():
                if isinstance(kf, dict):
                    keys.append((float(t), [float(x) for x in kf["vector"]], kf.get("easing", "linear")))
                else:
                    keys.append((float(t), [float(x) for x in kf], "linear"))
            out.setdefault(bone, {})[ch] = sorted(keys, key=lambda k: k[0])
    return out


def sample(keys, t):
    if t <= keys[0][0]:
        return keys[0][1]
    for (ta, va, _), (tb, vb, e) in zip(keys, keys[1:]):
        if t <= tb:
            k = ease(e, (t - ta) / (tb - ta) if tb > ta else 1)
            return [a + (b - a) * k for a, b in zip(va, vb)]
    return keys[-1][1]


# ------------------------------------------------------------------ geometry

def face_quads(c):
    """Corners (top-left, top-right, bottom-left in UV order) and UV rect for each face of a box-UV cube."""
    (x0, y0, z0), (w, h, d) = c["origin"], c["size"]
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    u, v = c["uv"]
    W, H, D = (math.ceil(s) for s in (w, h, d))
    return [
        ("front", [(x0, y1, z0), (x1, y1, z0), (x0, y0, z0)], (u + D, v + D, W, H)),
        ("back", [(x1, y1, z1), (x0, y1, z1), (x1, y0, z1)], (u + 2 * D + W, v + D, W, H)),
        ("right", [(x0, y1, z1), (x0, y1, z0), (x0, y0, z1)], (u, v + D, D, H)),
        ("left", [(x1, y1, z0), (x1, y1, z1), (x1, y0, z0)], (u + D + W, v + D, D, H)),
        ("top", [(x0, y1, z1), (x1, y1, z1), (x0, y1, z0)], (u + D, v, W, D)),
        ("bottom", [(x0, y0, z1), (x1, y0, z1), (x0, y0, z0)], (u + D + W, v, W, D)),
    ]


def world_faces(geo, pose):
    bones = {b["name"]: b for b in geo["bones"]}
    xf_cache = {}

    def xf(name):
        if name in xf_cache:
            return xf_cache[name]
        b = bones[name]
        p = pose.get(name, {})
        rest = b.get("rotation", [0, 0, 0])
        rot = [rest[i] + p.get("rotation", [0, 0, 0])[i] for i in range(3)]
        lx = local(b["pivot"], rot, p.get("position", (0, 0, 0)), p.get("scale", (1, 1, 1)))
        res = xf(b["parent"]).compose(lx) if "parent" in b else lx
        xf_cache[name] = res
        return res

    out = []
    for b in geo["bones"]:
        bx = xf(b["name"])
        for c in b.get("cubes", []):
            cx = bx
            if "rotation" in c:
                cx = bx.compose(local(c["pivot"], c["rotation"]))
            for face, pts, rect in face_quads(c):
                out.append((face, [cx.of(p) for p in pts], rect))
    return out


# ------------------------------------------------------------------ rasteriser (orthographic, affine per face)

def render(faces, tex, glow, yaw, scale, bounds, silhouette=False, bg=(24, 22, 38, 255)):
    (minx, maxx, miny, maxy) = bounds
    W, H = int((maxx - minx) * scale), int((maxy - miny) * scale)
    img = [[(255, 255, 255, 255) if silhouette else bg] * W for _ in range(H)]
    zbuf = [[1e9] * W for _ in range(H)]
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    light = (-0.45, 0.75, -0.5)
    ln = math.sqrt(sum(x * x for x in light))
    light = [x / ln for x in light]
    tw, th = len(tex[0]), len(tex)

    def view(p):
        x, y, z = p
        # turn the model by yaw (positive = we walk around to its left), camera looks along +Z from the front
        xr, zr = x * cy + z * sy, -x * sy + z * cy
        return ((xr - minx) * scale, (maxy - y) * scale, zr)

    for face, (a, b, c), (fu, fv, fw, fh) in faces:
        if fw == 0 or fh == 0:
            continue
        A, B, C = view(a), view(b), view(c)
        e1 = (B[0] - A[0], B[1] - A[1])
        e2 = (C[0] - A[0], C[1] - A[1])
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-6:
            continue
        # world-space normal for lighting
        u3 = [b[i] - a[i] for i in range(3)]
        v3 = [c[i] - a[i] for i in range(3)]
        n = [u3[1] * v3[2] - u3[2] * v3[1], u3[2] * v3[0] - u3[0] * v3[2], u3[0] * v3[1] - u3[1] * v3[0]]
        nl = math.sqrt(sum(x * x for x in n)) or 1
        n = [x / nl for x in n]
        bright = 0.55 + 0.45 * abs(sum(n[i] * light[i] for i in range(3)))
        D = (A[0] + e1[0] + e2[0], A[1] + e1[1] + e2[1])
        xs = [A[0], B[0], C[0], D[0]]
        ys = [A[1], B[1], C[1], D[1]]
        x0, x1 = max(0, int(min(xs))), min(W - 1, int(max(xs)) + 1)
        y0, y1 = max(0, int(min(ys))), min(H - 1, int(max(ys)) + 1)
        dz1, dz2 = B[2] - A[2], C[2] - A[2]
        for py in range(y0, y1 + 1):
            for px in range(x0, x1 + 1):
                qx, qy = px + 0.5 - A[0], py + 0.5 - A[1]
                s = (qx * e2[1] - qy * e2[0]) / det
                t = (e1[0] * qy - e1[1] * qx) / det
                if s < 0 or s >= 1 or t < 0 or t >= 1:
                    continue
                z = A[2] + s * dz1 + t * dz2
                if z >= zbuf[py][px]:
                    continue
                tx = min(fu + fw - 1, fu + int(s * fw))
                ty = min(fv + fh - 1, fv + int(t * fh))
                if tx >= tw or ty >= th:
                    continue
                col = tex[ty][tx]
                if col[3] < 128:
                    continue
                zbuf[py][px] = z
                if silhouette:
                    img[py][px] = (0, 0, 0, 255)
                    continue
                g = glow[ty][tx] if glow else (0, 0, 0, 0)
                if g[3] > 0:
                    img[py][px] = (g[0], g[1], g[2], 255)
                else:
                    img[py][px] = (int(col[0] * bright), int(col[1] * bright), int(col[2] * bright), 255)
    return img


def bounds_of(face_sets, yaws, pad=4):
    minx = miny = 1e9
    maxx = maxy = -1e9
    for faces in face_sets:
        for _, pts, _ in faces:
            for x, y, z in pts:
                for yaw in yaws:
                    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
                    xr = x * cy + z * sy
                    minx, maxx = min(minx, xr), max(maxx, xr)
                miny, maxy = min(miny, y), max(maxy, y)
    return minx - pad, maxx + pad, miny - pad, maxy + pad


def sheet(geo, tex, glow, anims, poses, yaws, scale, silhouette=False):
    """poses: [(label, clip or None, t)]. Returns rows of pixels (contact sheet)."""
    sets = []
    for _, clip, t in poses:
        pose = {}
        if clip:
            for bone, chans in anims[clip].items():
                pose[bone] = {ch: sample(keys, t) for ch, keys in chans.items()}
        sets.append(world_faces(geo, pose))
    bounds = bounds_of(sets, yaws)
    tiles = [[render(f, tex, glow, yaw, scale, bounds, silhouette) for yaw in yaws] for f in sets]
    th, tw = len(tiles[0][0]), len(tiles[0][0][0])
    gap = 2
    out = [[(60, 60, 70, 255)] * (len(yaws) * (tw + gap)) for _ in range(len(sets) * (th + gap))]
    for r, row in enumerate(tiles):
        for c, tile in enumerate(row):
            for y in range(th):
                out[r * (th + gap) + y][c * (tw + gap):c * (tw + gap) + tw] = tile[y]
    return out


def main(argv):
    geo_path, tex_path, out_path = argv[:3]
    opts = {"--glow": None, "--anim": None, "--views": "0,90,180,35", "--scale": "4"}
    poses_arg, silhouette = [], False
    i = 3
    while i < len(argv):
        if argv[i] == "--pose":
            poses_arg.append(argv[i + 1])
            i += 2
        elif argv[i] == "--silhouette":
            silhouette = True
            i += 1
        else:
            opts[argv[i]] = argv[i + 1]
            i += 2
    geo = json.load(open(geo_path))["minecraft:geometry"][0]
    tex = png.read(tex_path)[2]
    glow = png.read(opts["--glow"])[2] if opts["--glow"] else None
    anims = {}
    if opts["--anim"]:
        for name, clip in json.load(open(opts["--anim"]))["animations"].items():
            anims[name] = parse_anim(clip)
    poses = [("rest", None, 0)]
    for p in poses_arg:
        clip, t = p.split("@")
        poses.append((p, clip, float(t)))
    yaws = [float(y) for y in opts["--views"].split(",")]
    png.write(out_path, sheet(geo, tex, glow, anims, poses, yaws, float(opts["--scale"]), silhouette))
    print("preview:", out_path)


if __name__ == "__main__":
    main(sys.argv[1:])
