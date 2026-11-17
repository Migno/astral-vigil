"""Software preview for gen_armor.py: renders a standing player wearing a set (armour layers + 3D ornaments + glow)
from several angles at every awakening level, so ornaments can be judged without starting the game.

Used via: python -I tools/dev/gen_armor.py --preview <dir>
"""
import math
import os

import png

SCALE = 9
SUB = 3
BONES = {"HEAD": (0, 0, 0), "BODY": (0, 0, 0), "RIGHT_ARM": (-5, 2, 0), "LEFT_ARM": (5, 2, 0),
         "RIGHT_LEG": (-1.9, 12, 0), "LEFT_LEG": (1.9, 12, 0)}
SKIN = {"HEAD": (196, 160, 122), "BODY": (58, 120, 160), "RIGHT_ARM": (196, 160, 122), "LEFT_ARM": (196, 160, 122),
        "RIGHT_LEG": (60, 60, 110), "LEFT_LEG": (60, 60, 110)}
BASE_BOXES = {"HEAD": (-4, -8, -4, 8, 8, 8), "BODY": (-4, 0, -2, 8, 12, 4), "RIGHT_ARM": (-3, -2, -2, 4, 12, 4),
              "LEFT_ARM": (-1, -2, -2, 4, 12, 4), "RIGHT_LEG": (-2, 0, -2, 4, 12, 4), "LEFT_LEG": (-2, 0, -2, 4, 12, 4)}


def matmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return matmul(mz, matmul(my, mx))


class Xf:
    def __init__(self, m=None, t=(0, 0, 0)):
        self.m = m or [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        self.t = t

    def then(self, pose):
        """Child transform: translate by the pose offset, then rotate (ModelPart.translateAndRotate)."""
        x, y, z, rx, ry, rz = pose
        t = self.apply((x, y, z))
        return Xf(matmul(self.m, rot(rx, ry, rz)), t)

    def apply(self, p):
        return tuple(self.t[i] + sum(self.m[i][k] * p[k] for k in range(3)) for i in range(3))

    def dir(self, p):
        return tuple(sum(self.m[i][k] * p[k] for k in range(3)) for i in range(3))


FACE_SWAP = {"right": "left", "left": "right"}


def face_points(x0, y0, z0, x1, y1, z1, face, fw, fh):
    """Yields (i, j, point, normal) samples for a face; i/j are texel coords on the face."""
    for j in range(fh):
        for i in range(fw):
            for a in range(SUB):
                for b in range(SUB):
                    u = (i + (a + 0.5) / SUB) / fw
                    v = (j + (b + 0.5) / SUB) / fh
                    if face == "front":
                        yield i, j, (x0 + u * (x1 - x0), y0 + v * (y1 - y0), z0), (0, 0, -1)
                    elif face == "back":
                        yield i, j, (x1 - u * (x1 - x0), y0 + v * (y1 - y0), z1), (0, 0, 1)
                    elif face == "right":
                        yield i, j, (x0, y0 + v * (y1 - y0), z1 - u * (z1 - z0)), (-1, 0, 0)
                    elif face == "left":
                        yield i, j, (x1, y0 + v * (y1 - y0), z0 + u * (z1 - z0)), (1, 0, 0)
                    elif face == "top":
                        yield i, j, (x0 + u * (x1 - x0), y0, z1 - v * (z1 - z0)), (0, -1, 0)
                    else:
                        yield i, j, (x0 + u * (x1 - x0), y1, z0 + v * (z1 - z0)), (0, 1, 0)


def face_rects(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Canvas:
    def __init__(self, yaw):
        self.yaw = math.radians(yaw)
        self.pts = []  # (sx, sy, depth, colour)

    def view(self, p):
        c, s = math.cos(self.yaw), math.sin(self.yaw)
        x, y, z = p
        return x * c + z * s, y, -x * s + z * c

    def splat(self, p, n, col, glow=False):
        vx, vy, vz = self.view(p)
        nx, ny, nz = self.view(n)
        if not glow:
            light = 0.55 + 0.45 * max(0.0, -0.45 * nx - 0.75 * ny - 0.5 * nz)
            col = tuple(min(255, int(ch * light)) for ch in col[:3])
        else:
            col = tuple(min(255, int(ch * 1.15 + 30)) for ch in col[:3])
        self.pts.append((vx, vy, vz - (0.01 if glow else 0), col))

    def image(self, bounds, bg=(40, 44, 62)):
        x0, y0, x1, y1 = bounds
        w, h = int((x1 - x0) * SCALE), int((y1 - y0) * SCALE)
        img = [[bg + (255,)] * w for _ in range(h)]
        zbuf = [[1e9] * w for _ in range(h)]
        r = int(SCALE / SUB) + 2
        for vx, vy, vz, col in self.pts:
            cx, cy = int((vx - x0) * SCALE), int((vy - y0) * SCALE)
            for yy in range(cy - r // 2, cy + r - r // 2):
                for xx in range(cx - r // 2, cx + r - r // 2):
                    if 0 <= xx < w and 0 <= yy < h and vz < zbuf[yy][xx]:
                        zbuf[yy][xx] = vz
                        img[yy][xx] = col + (255,)
        return img


def draw_box(cv, xf, box, tex, uv, mirror=False, glow_tex=None, flat=None):
    x, y, z, w, h, d = box
    tw, th, td = uv[2:]
    rects = face_rects(uv[0], uv[1], tw, th, td)
    for face in ("top", "bottom", "right", "front", "left", "back"):
        src = FACE_SWAP.get(face, face) if mirror else face
        rx, ry, fw, fh = rects[src]
        for i, j, p, n in face_points(x, y, z, x + w, y + h, z + d, face, fw, fh):
            ii = fw - 1 - i if mirror else i
            wp, wn = xf.apply(p), xf.dir(n)
            if flat is not None:
                cv.splat(wp, wn, flat)
                continue
            px = tex[ry + j][rx + ii]
            if px[3] == 0:
                continue
            if glow_tex is not None and glow_tex[ry + j][rx + ii][3] > 0:
                cv.splat(wp, wn, glow_tex[ry + j][rx + ii], glow=True)
            else:
                cv.splat(wp, wn, px)


def draw_part(cv, xf, part, tex, glow):
    for b in part.boxes:
        draw_box(cv, xf, (b.x, b.y, b.z, b.w, b.h, b.d), tex, (b.u, b.v, b.w, b.h, b.d), glow_tex=glow)
    for c in part.children:
        draw_part(cv, xf.then(c.pose), c, tex, glow)


def scene(cv, l1, g1, l2, g2, pieces, atlas, level):
    for bone, piv in BONES.items():
        xf = Xf().then(piv + (0, 0, 0))
        bx, by, bz, bw, bh, bd = BASE_BOXES[bone]
        draw_box(cv, xf, BASE_BOXES[bone], None, (0, 0, bw, bh, bd), flat=SKIN[bone])
        mirror = bone.startswith("LEFT")

        def inflated(g):
            return bx - g, by - g, bz - g, bw + 2 * g, bh + 2 * g, bd + 2 * g
        if bone == "HEAD":
            draw_box(cv, xf, inflated(1.0), l1, (0, 0, 8, 8, 8), glow_tex=g1)
        elif bone == "BODY":
            draw_box(cv, xf, inflated(1.01), l1, (16, 16, 8, 12, 4), glow_tex=g1)
            draw_box(cv, xf, inflated(0.5), l2, (16, 16, 8, 12, 4), glow_tex=g2)
        elif "ARM" in bone:
            draw_box(cv, xf, inflated(1.0), l1, (40, 16, 4, 12, 4), mirror, glow_tex=g1)
        else:
            draw_box(cv, xf, inflated(0.5), l2, (0, 16, 4, 12, 4), mirror, glow_tex=g2)
            draw_box(cv, xf, inflated(1.0), l1, (0, 16, 4, 12, 4), mirror, glow_tex=g1)
        for pc in pieces:
            if pc.bone == bone and pc.level <= level:
                pivot = xf.then(pc.part.pose)
                draw_part(cv, pivot, pc.part, atlas.base, atlas.glow)


def render(name, l1, g1, l2, g2, pieces, atlas, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    levels = (0, 1, 2) if name == "seraph" else (0,)
    for level in levels:
        row = []
        for yaw in (0, 35, 90, 160, 180):
            cv = Canvas(yaw)
            scene(cv, l1, g1, l2, g2, pieces, atlas, level)
            row.append(cv.image((-17, -24, 17, 32)))
        img = [sum((r[y] for r in row), []) for y in range(len(row[0]))]
        png.write(os.path.join(out_dir, f"{name}_lvl{level}.png"), img)
