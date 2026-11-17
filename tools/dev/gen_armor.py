"""Generates the three celestial armour sets: equipment layers, emissive seam masks, 3D ornament meshes + textures,
item icons and the matching Java mesh class.

Run: python -I tools/dev/gen_armor.py [--preview DIR]
Outputs (per set seraph / solar / lunar):
  textures/entity/equipment/humanoid/<set>.png, humanoid_leggings/<set>.png   (26.1 equipment layers)
  textures/models/armor/<set>_layer_1.png, <set>_layer_2.png                    (1.21.1 armour layers, same art)
  textures/entity/armor/<set>_glow.png, <set>_leggings_glow.png                 (emissive seams, armour UV layout)
  textures/entity/armor/<set>_ornaments.png, <set>_ornaments_glow.png           (3D ornaments + emissive mask)
  textures/item/<set>_{helmet,chestplate,leggings,boots}.png                    (16x16 icons)
  src/main/java/.../client/armor/ArmorMeshes.java                               (do not edit by hand)
--preview DIR renders orthographic previews (front / three-quarter / side / back, every awakening level) to DIR.

Art rules: vanilla style, <= 8 colours per texture, Void Navy outline, light from the top-left.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "textures")
JAVA = os.path.join(ROOT, "src", "main", "java", "media", "hofferbert", "astralvigil", "client", "armor", "ArmorMeshes.java")
C = png.hexc
CLEAR = (0, 0, 0, 0)
NAVY = C("1E1A2E")

# ------------------------------------------------------------------ palettes (8 colours each, Void Navy outline)

PALETTES = {
    "seraph": {"o": NAVY, "w": C("FFFFFF"), "p": C("D8D8E6"), "s": C("B9B3CC"), "gl": C("F2C94C"), "g": C("C9971E"),
               "gd": C("7A5A12"), "c": C("8FEFFF"), "cd": C("2FA8D0")},
    "solar": {"o": NAVY, "w": C("FFF4C8"), "y": C("FFE27A"), "gl": C("F2C94C"), "g": C("C9971E"), "gd": C("7A5A12"),
              "f": C("E8772E"), "fd": C("A8401A")},
    "lunar": {"o": NAVY, "w": C("FFFFFF"), "p": C("D8D8E6"), "s": C("A9B8D8"), "sd": C("6C7FAE"), "b": C("3B4C80"),
              "bd": C("26305A"), "c": C("8FEFFF")},
}
# seraph uses 9 keys but "s" (shade) and "cd" never share a texture with all others; save() asserts the real count.

# ------------------------------------------------------------------ box UV helpers (vanilla layout)


def faces(u, v, w, h, d):
    """Vanilla box UV: face -> (x, y, width, height). right = the model's -x side, front = -z."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def paint(img, glow, pal, u, v, w, h, d, fn):
    """fn(face, i, j, fw, fh) -> palette key, key + '*' (also emissive), or None (transparent)."""
    for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
        for j in range(fh):
            for i in range(fw):
                k = fn(name, i, j, fw, fh)
                if k is None:
                    continue
                lit = k.endswith("*")
                k = k.rstrip("*")
                img[y0 + j][x0 + i] = pal[k]
                if lit and glow is not None:
                    glow[y0 + j][x0 + i] = pal[k]


def bevel(i, j, fw, fh, light, mid, dark):
    """Top-left light, bottom-right dark (vanilla plate shading)."""
    if j == 0 or i == 0:
        return light
    if j == fh - 1 or i == fw - 1:
        return dark
    return mid


# ------------------------------------------------------------------ armour layers (64x32, vanilla armour UV)
# Layer 1 ("humanoid"): head (0,0) 8x8x8, body (16,16) 8x12x4, arm (40,16) 4x12x4, leg/boot (0,16) 4x12x4.
# Layer 2 ("humanoid_leggings"): body (16,16) belt, legs (0,16).

# --- Seraph Plate: white plate, gold filigree, cyan seams


def seraph_head(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "front":
        if j == 0:
            return "gl" if i < 7 else "g"
        if i in (3, 4) and j <= 6:
            return "gl" if i == 3 else "g"  # nasal ridge
        if j == 3 and i in (1, 2, 5, 6):
            return "c*"  # eye slits
        if j == 4 and i in (1, 2, 5, 6):
            return "cd"
        if j == 7:
            return "gd"
        return bevel(i, j, fw, fh, "w", "w", "p")
    if f in ("right", "left"):
        if j == 0:
            return "gl"
        if j == 7:
            return "gd"
        # swept feather engraving
        k = i if f == "left" else 7 - i
        if (j == 2 and k >= 3) or (j == 4 and k >= 2) or (j == 6 and k >= 1):
            return "g" if j != 2 else "gl"
        if j == 5 and k == 1:
            return "cd*"
        return "w" if j < 4 else "p"
    if f == "top":
        if i in (3, 4):
            return "gl" if i == 3 else "g"
        return bevel(i, j, fw, fh, "w", "w", "p")
    # back
    if j in (2, 5):
        return "g"
    if j == 7:
        return "gd"
    if i in (3, 4) and j in (3, 4):
        return "c*"
    return bevel(i, j, fw, fh, "w", "w", "p")


def seraph_body(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "top":
        return "gl" if j == 0 else "w"
    if f == "front":
        if j == 0:
            return "gl" if 0 < i < 7 else "g"
        if j in (10, 11):
            if j == 10 and i in (3, 4):
                return "gl"
            return "g" if j == 10 else "gd"
        # diamond-framed core gem with seams running down
        if (i, j) in ((3, 3), (4, 3), (3, 4), (4, 4)):
            return "c*"
        if (i, j) in ((3, 2), (4, 2), (2, 3), (2, 4), (5, 3), (5, 4), (3, 5), (4, 5)):
            return "gl"
        if i in (3, 4) and 6 <= j <= 9:
            return "cd*" if j % 2 == 0 else "c*"
        if (i, j) in ((1, 2), (6, 2), (1, 5), (6, 5)):
            return "g"
        if j in (6, 8) and i not in (0, 7):
            return "s"
        return bevel(i, j, fw, fh, "w", "w" if j < 6 else "p", "p")
    if f == "back":
        if j == 0:
            return "gl"
        if j in (10, 11):
            return "g" if j == 10 else "gd"
        if i in (3, 4) and 1 <= j <= 9:
            return "c*" if j % 3 == 1 else ("gl" if i == 3 else "g")
        if (i, j) in ((1, 2), (6, 2), (1, 3), (6, 3)):
            return "gd"
        return bevel(i, j, fw, fh, "w", "p", "s")
    # sides (4 wide)
    if j == 0:
        return "gl"
    if j >= 10:
        return "g" if j == 10 else "gd"
    if i == 1 and 2 <= j <= 8:
        return "cd*"
    return bevel(i, j, fw, fh, "w", "p", "s")


def seraph_arm(f, i, j, fw, fh):
    if f == "top":
        return "gl"
    if f == "bottom":
        return "p"
    if j == 0:
        return "gl"
    if j in (1, 2):
        return "w" if i < fw - 1 else "p"
    if j == 3:
        return "gd"
    if j in (8, 9):
        return "gl" if j == 8 else "g"
    if j == 11:
        return "s"
    if f == "right" and i in (1, 2) and 4 <= j <= 7:
        return "c*" if i == 1 else "cd"
    return bevel(i, j, fw, fh, "w", "p", "s")


def seraph_boot(f, i, j, fw, fh):
    if f == "top":
        return None
    if f == "bottom":
        return "gd"
    if j < 6:
        return None
    if j == 6:
        return "gl"
    if j == 10:
        return "g"
    if j == 11:
        return "gd"
    if f in ("front", "right") and i == 1 and j in (7, 8, 9):
        return "c*" if j != 9 else "cd*"
    return bevel(i, j - 6, fw, 6, "w", "p", "s")


def seraph_belt(f, i, j, fw, fh):
    if f in ("top", "bottom") or j < 7:
        return None
    if j == 7:
        return "gl"
    if j == 10:
        if f == "front" and i in (3, 4):
            return "c*"
        return "g"
    if j == 11:
        return "gd"
    if f == "front" and i in (3, 4) and j in (8, 9):
        return "gl"
    return "w" if j == 8 else "p"


def seraph_leg(f, i, j, fw, fh):
    if f in ("top", "bottom") or j > 9:
        return None
    if j == 0:
        return "gd"
    if j in (5, 6):
        return "gl" if j == 5 else "g"  # knee cop
    if f == "right" and i == 1 and j not in (5, 6):
        return "c*" if j % 2 else "cd*"
    if f == "front" and i == 1 and j == 4:
        return "gl"
    return bevel(i, j, fw, fh, "w", "p", "s")


# --- Solar Regalia: flame-gold plate, sun emblems

FLAME = [3, 5, 2, 4, 6, 3, 5, 2]


def flame(i, j, fh, top_rows):
    """Flame tongues rising from the bottom: column height from FLAME."""
    hgt = FLAME[i % len(FLAME)]
    if j >= fh - hgt + top_rows:
        return "fd" if j >= fh - 1 - hgt // 3 else "f"
    if j == fh - hgt + top_rows - 1:
        return "y"
    return None


def solar_head(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "front":
        if j == 0:
            return "y" if i < 7 else "gl"
        if (i, j) in ((3, 1), (4, 1), (3, 2), (4, 2)):
            return "w*"
        if j == 3 and 1 <= i <= 6:
            return "y*" if i in (2, 5) else "o"
        if j == 4 and i in (1, 6):
            return "gd"
        if j == 7:
            return "fd"
        if j == 6 and i % 2 == 0:
            return "f"
        return bevel(i, j, fw, fh, "gl", "g", "gd")
    if f in ("right", "left"):
        if j == 0:
            return "y"
        fl = flame(i, j, fh, 2)
        if fl:
            return fl
        return bevel(i, j, fw, fh, "gl", "g", "gd")
    if f == "top":
        if (i + j) % 4 == 0:
            return "y"
        return bevel(i, j, fw, fh, "gl", "g", "g")
    if j == 7:
        return "fd"
    fl = flame(i, j, fh, 3)
    return fl or bevel(i, j, fw, fh, "gl", "g", "gd")


def sun(i, j, cx, cy):
    r = math.hypot(i - cx, j - cy)
    if r < 1.2:
        return "w*"
    if r < 2.0:
        return "y*"
    return None


def solar_body(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "top":
        return "gl" if j == 0 else "g"
    if f in ("front", "back"):
        if j == 0:
            return "y" if f == "front" else "gl"
        if f == "front":
            s = sun(i, j, 3.5, 4.0)
            if s:
                return s
            if (i, j) in ((3, 1), (4, 1), (3, 7), (4, 7), (0, 4), (7, 4), (1, 2), (6, 2), (1, 6), (6, 6)):
                return "y"  # sun rays
        else:
            s = sun(i, j, 3.5, 3.5)
            if s and s.startswith("w"):
                return "y*"
        if j >= 9:
            return flame(i, j, fh, 0) or "g"
        return bevel(i, j, fw, fh, "gl", "g", "gd")
    if j == 0:
        return "y"
    if j >= 9:
        return flame(i, j, fh, 0) or "g"
    return bevel(i, j, fw, fh, "gl", "g", "gd")


def solar_arm(f, i, j, fw, fh):
    if f == "top":
        return "y"
    if f == "bottom":
        return "gd"
    if j == 0:
        return "y"
    if j in (1, 2):
        return "gl" if i < fw - 1 else "g"
    if j == 3:
        return "fd"
    if f == "right" and j == 5 and i in (1, 2):
        return "w*"
    if j >= 8:
        return flame(i, j, fh, 0) or "g"
    return bevel(i, j, fw, fh, "gl", "g", "gd")


def solar_boot(f, i, j, fw, fh):
    if f == "top" or (f != "bottom" and j < 6):
        return None
    if f == "bottom":
        return "gd"
    if j == 6:
        return "y"
    if j == 11:
        return "fd"
    if j >= 9:
        return "f" if (i + j) % 2 else "fd"
    if f == "right" and i in (1, 2) and j == 7:
        return "w*"
    return bevel(i, j - 6, fw, 6, "gl", "g", "gd")


def solar_belt(f, i, j, fw, fh):
    if f in ("top", "bottom") or j < 7:
        return None
    if j == 7:
        return "y"
    if f == "front" and i in (3, 4) and j in (8, 9):
        return "w*"
    if j == 11:
        return "fd"
    return "g" if j != 10 else "f"


def solar_leg(f, i, j, fw, fh):
    if f in ("top", "bottom") or j > 9:
        return None
    if j == 0:
        return "gd"
    if j in (5, 6):
        return "y" if j == 5 else "gl"
    if f == "right" and i in (1, 2) and j in (2, 3):
        return "f"
    if j >= 7:
        return "f" if (i + j) % 3 == 0 else "g"
    return bevel(i, j, fw, fh, "gl", "g", "gd")


# --- Lunar Raiment: midnight cloth, moonsilver plates, moon phases

def lunar_head(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "front":
        if j <= 1:
            if (i, j) in ((3, 1), (4, 1)):
                return "w*"
            return "b" if j == 0 else "bd"
        if j == 2:
            return "s" if i not in (3, 4) else "c*"
        if i in (0, 7):
            return "b" if j < 6 else "bd"
        return None  # the hood frames the face
    if f in ("right", "left"):
        if j == 2:
            return "s"
        if j == 7:
            return "bd"
        return "bd" if (i + (0 if f == "left" else 1)) % 3 == 0 and j > 2 else "b"
    if f == "top":
        if i in (3, 4) and j < 5:
            return "bd"
        return "b"
    if j == 2:
        return "s"
    if i in (2, 5) and j > 3:
        return "bd"
    return "b"


LUNAR_CRESCENT = {(2, 0), (3, 0), (1, 1), (0, 2), (0, 3), (1, 4), (2, 5), (3, 5)}
PHASES = {1: "bd", 2: "sd*", 3: "s*", 4: "w*", 5: "s*", 6: "sd*"}


def lunar_body(f, i, j, fw, fh):
    if f == "bottom":
        return None
    if f == "top":
        return "s" if j == 0 else "b"
    if f == "front":
        if j == 0:
            return PHASES.get(i, "s")  # moon phases on the collar
        if j == 11:
            return "s"
        if 1 <= i <= 6 and 1 <= j <= 6:
            # moonsilver breastplate with a crescent
            if (i - 1, j - 1) in LUNAR_CRESCENT:
                return "w*"
            return bevel(i - 1, j - 1, 6, 6, "p", "s", "sd")
        if j in (8, 10) and i in (1, 5) or (j == 9 and i == 3):
            return "c*"  # embroidered stars
        return "bd" if i in (0, 7) else "b"
    if f == "back":
        if j == 0:
            return "s"
        if j == 11:
            return "s"
        if (i, j) in ((2, 3), (5, 5), (3, 8), (6, 9), (1, 7)):
            return "c*"
        return "bd" if i in (3, 4) and j > 1 else "b"
    if j == 0 or j == 11:
        return "s"
    return "bd" if i == 2 else "b"


def lunar_arm(f, i, j, fw, fh):
    if f == "top":
        return "p"
    if f == "bottom":
        return "bd"
    if j <= 3:
        if j == 3:
            return "sd"
        if f == "right" and j == 1 and i in (1, 2):
            return "w*"
        return bevel(i, j, fw, 4, "p", "s", "sd")
    if j in (9, 10):
        return "s" if j == 9 else "p"
    if j == 11:
        return "sd"
    return "bd" if (i + j) % 4 == 0 else "b"


def lunar_boot(f, i, j, fw, fh):
    if f == "top" or (f != "bottom" and j < 6):
        return None
    if f == "bottom":
        return "sd"
    if j == 6:
        return "p"
    if j == 11:
        return "bd"
    if f == "front" and (i, j) in ((1, 7), (2, 8), (1, 9)):
        return "w*"
    return bevel(i, j - 6, fw, 6, "p", "s", "sd")


def lunar_belt(f, i, j, fw, fh):
    if f in ("top", "bottom") or j < 7:
        return None
    if j == 7:
        return "s"
    if f == "front" and (i, j) in ((3, 8), (4, 9), (3, 9)):
        return "w*"
    return "b" if j < 10 else "bd"


def lunar_leg(f, i, j, fw, fh):
    if f in ("top", "bottom") or j > 9:
        return None
    if j == 9:
        return "s"
    if j == 8 and (i + (1 if f in ("front", "left") else 0)) % 2 == 0:
        return "c*"
    if j == 0:
        return "bd"
    return "bd" if i == 1 and f in ("front", "back") else "b"


ARMOR = {
    "seraph": (seraph_head, seraph_body, seraph_arm, seraph_boot, seraph_belt, seraph_leg),
    "solar": (solar_head, solar_body, solar_arm, solar_boot, solar_belt, solar_leg),
    "lunar": (lunar_head, lunar_body, lunar_arm, lunar_boot, lunar_belt, lunar_leg),
}


def armor_layers(name):
    pal = PALETTES[name]
    head, body, arm, boot, belt, leg = ARMOR[name]
    l1, g1 = png.blank(64, 32), png.blank(64, 32)
    paint(l1, g1, pal, 0, 0, 8, 8, 8, head)
    paint(l1, g1, pal, 16, 16, 8, 12, 4, body)
    paint(l1, g1, pal, 40, 16, 4, 12, 4, arm)
    paint(l1, g1, pal, 0, 16, 4, 12, 4, boot)
    l2, g2 = png.blank(64, 32), png.blank(64, 32)
    paint(l2, g2, pal, 16, 16, 8, 12, 4, belt)
    paint(l2, g2, pal, 0, 16, 4, 12, 4, leg)
    return l1, g1, l2, g2


# ------------------------------------------------------------------ 3D ornaments

class Box:
    def __init__(self, x, y, z, w, h, d, fn, src=None):
        self.x, self.y, self.z, self.w, self.h, self.d, self.fn = x, y, z, w, h, d, fn
        self.u = self.v = 0
        self.src = src  # share the UVs of another box with the same size (mirrored or repeated parts)


class Part:
    """A node of the ornament mesh: pose (offset + rotation in radians) with boxes and children."""

    def __init__(self, name, pose=(0, 0, 0, 0, 0, 0), boxes=(), children=()):
        self.name, self.pose, self.boxes, self.children = name, pose, list(boxes), list(children)


class Piece:
    """One ornament: follows a bone, belongs to an armour slot, appears from an awakening level, animates."""

    def __init__(self, name, bone, slot, level, anim, pose, boxes=(), children=()):
        self.name, self.bone, self.slot, self.level, self.anim = name, bone, slot, level, anim
        self.part = Part(name, pose, boxes, children)


def mirror_part(p, name=None):
    """Mirror across x = 0 (left <-> right): negate x, flip y/z rotations."""
    x, y, z, rx, ry, rz = p.pose
    return Part(name or p.name, (-x, y, z, rx, -ry, -rz),
                [Box(-(b.x + b.w), b.y, b.z, b.w, b.h, b.d, b.fn, b.src or b) for b in p.boxes],
                [mirror_part(c) for c in p.children])


def mirror(piece, name, bone):
    m = Piece(name, bone, piece.slot, piece.level, MIRROR_ANIM.get(piece.anim, piece.anim), (0,) * 6)
    m.part = mirror_part(piece.part, name)
    return m


MIRROR_ANIM = {"FLAP_RIGHT": "FLAP_LEFT", "FLAP_LEFT": "FLAP_RIGHT"}


# --- face painters for ornaments: fn(face, i, j, fw, fh) -> key / key* / None

def plate(light, mid, dark, glow_tip=None):
    def fn(f, i, j, fw, fh):
        if f == "top":
            return light
        if f == "bottom":
            return dark
        if glow_tip and j == 0:
            return glow_tip
        return bevel(i, j, fw, fh, light, mid, dark)
    return fn


def solid(k):
    return lambda f, i, j, fw, fh: k


def tipped(tip, body, rows=1):
    """Spike / feather: emissive tip rows on top, body below."""
    def fn(f, i, j, fw, fh):
        if f == "top":
            return tip
        if f == "bottom":
            return body
        return tip if j < rows else body
    return fn


def feather(quill, vane, edge, tip):
    """Feather lying along +z (length = d): quill on top, vane on the sides, emissive tip at the far end."""
    def fn(f, i, j, fw, fh):
        if f == "back":
            return tip
        if f == "front":
            return quill
        if f in ("left", "right"):
            far = i if f == "left" else fw - 1 - i
            if far >= fw - 1:
                return tip
            return edge if j == fh - 1 else vane
        return quill if f == "top" else edge
    return fn


def vane(light, mid, edge, tip):
    """Hanging wing feather (height = h): light top, darker edge at the bottom, emissive tip pixel row."""
    def fn(f, i, j, fw, fh):
        if f in ("top", "bottom"):
            return light if f == "top" else tip
        if j == fh - 1:
            return tip
        if j == fh - 2:
            return edge
        return light if j == 0 or i == 0 else mid
    return fn


def gem(core, rim):
    return lambda f, i, j, fw, fh: core if (i + j) % 2 == 0 or f in ("front", "back") else rim


def ring_segments(count, radius, seg_len, thick, fn, stud_fn=None, stud_every=3, axis="y", prefix="s"):
    """Ring of `count` boxes around an axis (y: horizontal halo, z: vertical ring behind the back)."""
    out, first = [], {}
    for n in range(count):
        a = 2 * math.pi * n / count
        boxes = []
        if axis == "y":
            boxes.append(Box(-seg_len / 2, -thick / 2, -radius - 0.5, seg_len, thick, 1, fn))
            if stud_fn and n % stud_every == 0:
                boxes.append(Box(-0.5, -thick / 2 - 0.5, -radius - 1.5, 1, thick + 1, 1, stud_fn))
            pose = (0, 0, 0, 0, a, 0)
        else:
            boxes.append(Box(-seg_len / 2, -radius - 0.5, -thick / 2, seg_len, 1, thick, fn))
            if stud_fn and n % stud_every == 0:
                boxes.append(Box(-0.5, -radius - 2.5, -thick / 2, 1, 2, thick, stud_fn))
            pose = (0, 0, 0, 0, 0, a)
        for k, b in enumerate(boxes):
            if k in first:
                b.src = first[k]
            else:
                first[k] = b
        out.append(Part(f"{prefix}{n}", pose, boxes))
    return out


def pixel_rows(rows, z, depth, fn, x0=None, y0=0.0, plane="xy"):
    """Builds boxes from an ASCII bitmap ('#' filled) – one box per horizontal run. plane xy: thin in z."""
    boxes = []
    width = len(rows[0])
    x0 = -width / 2 if x0 is None else x0
    for r, row in enumerate(rows):
        c = 0
        while c < width:
            if row[c] == "#":
                s = c
                while c < width and row[c] == "#":
                    c += 1
                boxes.append(Box(x0 + s, y0 + r, z, c - s, 1, depth, fn))
            else:
                c += 1
    return boxes


# ------------------------------------------------------------------ Seraph Plate ornaments

def seraph_pieces():
    gold = plate("gl", "g", "gd")
    gold_glow = plate("gl*", "g*", "gd")
    white = plate("w", "p", "s")
    cyan = gem("c*", "cd*")
    feather_w = feather("gl", "w", "p", "c*")
    feather_p = feather("g", "p", "s", "cd*")
    pieces = []

    # Halo: tilted, floating, spinning ring with cyan studs
    pieces.append(Piece("halo", "HEAD", "HEAD", 0, "HALO", (0, -14.5, 1.5, -0.22, 0, 0),
                        children=ring_segments(12, 6, 3, 1, gold_glow, cyan, 3)))
    # Inner light ring (Ascended)
    pieces.append(Piece("halo_inner", "HEAD", "HEAD", 2, "HALO_REVERSE", (0, -14.5, 1.5, -0.22, 0, 0),
                        children=ring_segments(8, 4, 3, 1, solid("c*"), prefix="i")))
    # Brow gem + crest ridge
    pieces.append(Piece("crest", "HEAD", "HEAD", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-0.5, -10.0, -4.5, 1, 1, 9, gold),
        Box(-0.5, -12.0, -5.5, 1, 3, 1, tipped("c*", "gl")),
        Box(-1.0, -8.0, -5.6, 2, 2, 1, cyan),
    ]))
    # Feathered crest wings on the helmet sides (three feathers fanning up and back, a fourth when awakened)
    def crest_wing(level, specs, name):
        kids = [Part(f"f{n}", (0, 0, 0, rx, 0, 0), [Box(-0.5, -0.5, 0, 1, h, ln, feather_w if n % 2 == 0 else feather_p)])
                for n, (rx, ln, h) in enumerate(specs)]
        return Piece(name, "HEAD", "HEAD", level, "NONE", (-5.3, -6.0, 1.0, 0, -0.35, 0), children=kids)
    cw = crest_wing(0, [(0.3, 6, 2), (0.7, 7, 2), (1.1, 6, 1)], "crest_wing_right")
    pieces += [cw, mirror(cw, "crest_wing_left", "HEAD")]
    cw2 = crest_wing(1, [(0.5, 9, 1), (0.9, 9, 1), (1.3, 7, 1)], "crest_plume_right")
    pieces += [cw2, mirror(cw2, "crest_plume_left", "HEAD")]

    # Pauldrons with gold filigree ridges and upswept feather spikes
    pr = Piece("pauldron_right", "RIGHT_ARM", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-5.0, -4.0, -3.5, 6, 2, 7, white),
        Box(-5.5, -2.5, -3.5, 2, 3, 7, plate("w", "p", "s")),
        Box(-5.6, -3.6, -3.6, 1, 1, 7, gold),
        Box(-4.0, -5.0, -1.0, 3, 1, 2, gold),
        Box(-5.9, -2.0, -0.5, 1, 1, 1, cyan),
    ], children=[
        Part("spike0", (-4.0, -4.5, -1.5, 0, 0, -0.45), [Box(-0.5, -4, -0.5, 1, 4, 1, tipped("c*", "w"))]),
        Part("spike1", (-4.0, -4.5, 1.5, 0, 0, -0.65), [Box(-0.5, -3, -0.5, 1, 3, 1, tipped("c*", "p"))]),
    ])
    pieces += [pr, mirror(pr, "pauldron_left", "LEFT_ARM")]
    # Cyan core on the breastplate with gold wings
    pieces.append(Piece("core", "BODY", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-1.0, 2.0, -3.7, 2, 2, 1, cyan),
        Box(-4.0, 2.5, -3.4, 3, 1, 1, gold),
        Box(1.0, 2.5, -3.4, 3, 1, 1, gold),
        Box(-0.5, 4.0, -3.4, 1, 3, 1, plate("gl", "g", "gd")),
    ]))
    # Light-wings on the back: leading edge rising up and out, coverts, primaries longest at the tip
    def back_wing(name, level, length, height, count, z):
        boxes = [Box(-length, -1.0, z, length, 2, 1, plate("gl", "w", "g")),
                 Box(-length + 1, 1.0, z + 0.1, length - 1, 2, 1, plate("w", "w", "p"))]
        for k in range(count):
            boxes.append(Box(-length + 2 * k, 1.0, z + 0.2, 2, height - k, 1, vane("w", "p", "gl", "c*")))
        return Piece(name, "BODY", "CHEST", level, "FLAP_RIGHT", (-1.5, 1.5, 3.6, 0.1, 0.55, 0.6), boxes=boxes)
    bw = back_wing("wing_right", 0, 9, 8, 4, 0)
    pieces += [bw, mirror(bw, "wing_left", "BODY")]
    bw2 = back_wing("wing_grand_right", 1, 15, 13, 6, 0.7)
    pieces += [bw2, mirror(bw2, "wing_grand_left", "BODY")]
    # Mandorla of light behind the back (Ascended)
    pieces.append(Piece("mandorla", "BODY", "CHEST", 2, "SPIN_Z", (0, 5.0, 7.0, 0, 0, 0),
                        children=ring_segments(16, 11, 5, 1, solid("c*"), gem("w*", "gl*"), 4, axis="z", prefix="m")))

    # Tassets: hinged plates over the thighs
    tr = Piece("tasset_right", "RIGHT_LEG", "LEGS", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-2.5, -0.5, -3.3, 5, 4, 1, plate("w", "p", "gl")),
        Box(-3.3, -0.5, -2.5, 1, 4, 4, plate("w", "p", "gl")),
        Box(-0.5, 0.5, -3.6, 1, 1, 1, solid("c*")),
    ])
    pieces += [tr, mirror(tr, "tasset_left", "LEFT_LEG")]
    # Winged greaves
    gr = Piece("greave_wing_right", "RIGHT_LEG", "FEET", 0, "NONE", (-3.2, 8.5, 0.5, 0, -0.2, 0), children=[
        Part("f0", (0, 0, 0, 0.35, 0, 0), [Box(-0.5, -0.5, 0, 1, 2, 4, feather_w)]),
        Part("f1", (0, 0, 0, 0.85, 0, 0), [Box(-0.5, -0.5, 0, 1, 1, 4, feather_p)]),
        Part("f2", (0, 0, 0, 1.3, 0, 0), [Box(-0.5, -0.5, 0, 1, 1, 3, feather_w)]),
    ])
    pieces += [gr, mirror(gr, "greave_wing_left", "LEFT_LEG")]
    return pieces


# ------------------------------------------------------------------ Solar Regalia ornaments

def solar_pieces():
    gold = plate("gl", "g", "gd")
    ray = tipped("w*", "gl", 1)
    ray_y = tipped("y*", "g", 1)
    flame_fn = tipped("y*", "f", 1)
    pieces = []
    # Sun-ray crown: band + 16 rays leaning outwards, tallest in front
    band = [Box(-5.5, -9.5, -5.5, 11, 1, 1, gold), Box(-5.5, -9.5, 4.5, 11, 1, 1, gold),
            Box(-5.5, -9.5, -4.5, 1, 1, 9, gold), Box(4.5, -9.5, -4.5, 1, 1, 9, gold)]
    rays = []
    for n in range(12):
        th = 2 * math.pi * n / 12
        length = round(2 + 5 * (1 + math.cos(th)) / 2)
        r = 5.2
        rays.append(Part(f"r{n}", (round(math.sin(th) * r, 3), -9.5, round(-math.cos(th) * r, 3), 0.6, round(-th, 4), 0),
                         [Box(-0.5, -length, -0.5, 1, length, 1, ray if n % 2 == 0 else ray_y)]))
    pieces.append(Piece("crown", "HEAD", "HEAD", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=band + [
        Box(-1.0, -8.2, -5.6, 2, 2, 1, gem("w*", "y*")),
    ], children=rays))
    # Sun disc on the back with alternating rays, slowly turning
    disc = [Box(-4, -4, 0, 8, 8, 1, gem("w*", "y*")), Box(-5, -3, 0.2, 10, 6, 1, solid("y*")),
            Box(-3, -5, 0.2, 6, 10, 1, solid("y*")), Box(-6, -1, 0.4, 12, 2, 1, solid("gl")),
            Box(-1, -6, 0.4, 2, 12, 1, solid("gl"))]
    disc_rays = []
    for n in range(12):
        boxes = ([Box(-1, -10, 0.3, 2, 4, 1, plate("gl", "g", "gd")), Box(-0.5, -13, 0.3, 1, 3, 1, tipped("w*", "y*"))]
                 if n % 2 == 0 else [Box(-0.5, -9, 0.3, 1, 3, 1, tipped("y*", "f"))])
        disc_rays.append(Part(f"r{n}", (0, 0, 0, 0, 0, round(2 * math.pi * n / 12 + math.pi / 12, 4)), boxes))
    pieces.append(Piece("sun_disc", "BODY", "CHEST", 0, "SPIN_Z", (0, 2.5, 5.0, 0, 0, 0), boxes=disc, children=disc_rays))
    # Flame pauldrons
    pr = Piece("pauldron_right", "RIGHT_ARM", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-5.0, -4.0, -3.5, 6, 2, 7, gold),
        Box(-5.5, -2.5, -3.5, 2, 3, 7, plate("g", "f", "fd")),
        Box(-5.8, -1.5, -1.0, 1, 2, 2, gem("w*", "y*")),
    ], children=[
        Part("flame0", (-4.0, -4.0, -2.0, 0, 0, -0.3), [Box(-0.5, -2, -0.5, 1, 2, 1, flame_fn)]),
        Part("flame1", (-3.5, -4.0, 0.0, 0, 0, -0.4), [Box(-0.5, -4, -0.5, 1, 4, 1, flame_fn)]),
        Part("flame2", (-4.0, -4.0, 2.0, 0, 0, -0.3), [Box(-0.5, -3, -0.5, 1, 3, 1, flame_fn)]),
    ])
    pieces += [pr, mirror(pr, "pauldron_left", "LEFT_ARM")]
    pieces.append(Piece("sun_emblem", "BODY", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-1.0, 3.0, -3.7, 2, 2, 1, gem("w*", "y*")),
        Box(-0.5, 1.5, -3.4, 1, 1, 1, solid("y*")), Box(-0.5, 5.5, -3.4, 1, 1, 1, solid("y*")),
        Box(-2.5, 3.5, -3.4, 1, 1, 1, solid("y*")), Box(1.5, 3.5, -3.4, 1, 1, 1, solid("y*")),
    ]))
    # Tassets with flame fringe
    tr = Piece("tasset_right", "RIGHT_LEG", "LEGS", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-2.5, -0.5, -3.3, 5, 4, 1, plate("gl", "g", "gd")),
        Box(-2.0, 3.5, -3.3, 1, 2, 1, flame_fn), Box(0.5, 3.5, -3.3, 1, 1, 1, flame_fn),
        Box(-3.3, -0.5, -2.5, 1, 4, 5, plate("gl", "g", "gd")),
    ])
    pieces += [tr, mirror(tr, "tasset_left", "LEFT_LEG")]
    # Sun rondel + flame spur at the ankle
    sp = Piece("spur_right", "RIGHT_LEG", "FEET", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-3.5, 8.0, -1.0, 1, 2, 2, gem("w*", "y*")),
    ], children=[
        Part("spur", (-0.5, 10.0, 3.0, -0.5, 0, 0), [Box(-0.5, -0.5, 0, 1, 1, 3, flame_fn)]),
        Part("fin", (-3.2, 8.0, 1.0, 0.6, 0, 0), [Box(-0.5, -3, -0.5, 1, 3, 1, flame_fn)]),
    ])
    pieces += [sp, mirror(sp, "spur_left", "LEFT_LEG")]
    return pieces


# ------------------------------------------------------------------ Lunar Raiment ornaments

CRESCENT = ["#.......#",
            "##.....##",
            ".##...##.",
            "..#####.."]


def lunar_pieces():
    silver = plate("p", "s", "sd")
    cloth = plate("b", "b", "bd")
    pieces = []
    # Circlet with a rising crescent and a glowing moonstone
    band = [Box(-5.0, -7.0, -5.5, 10, 1, 1, silver), Box(-5.0, -7.0, 4.5, 10, 1, 1, silver),
            Box(-5.5, -7.0, -5.0, 1, 1, 10, silver), Box(4.5, -7.0, -5.0, 1, 1, 10, silver)]
    cres = pixel_rows(CRESCENT, -5.8, 1, solid("w*"), y0=-11.0)
    pieces.append(Piece("circlet", "HEAD", "HEAD", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=band + cres + [
        Box(-0.5, -7.2, -5.9, 1, 1, 1, solid("c*")),
    ]))
    # Crescent moon-halo floating behind the head (horns up)
    arc = []
    for n in range(9):
        ang = math.pi * (0.55 + 0.9 * n / 8)
        boxes = [Box(-1.5, -7.5, -0.5, 3, 1, 1, solid("w*"))]
        if 2 <= n <= 6:
            boxes.append(Box(-1.5, -6.5, -0.5, 3, 1, 1, solid("c*")))
        arc.append(Part(f"a{n}", (0, 0, 0, 0, 0, round(ang, 4)), boxes))
    pieces.append(Piece("moon_halo", "HEAD", "HEAD", 0, "BOB", (0, -6.0, 6.5, 0, 0, 0), children=arc))
    # Hood drape falling down the back of the head
    pieces.append(Piece("hood", "HEAD", "HEAD", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-4.5, -9.6, 4.3, 9, 10, 1, cloth),
        Box(-1.5, -10.6, 3.3, 3, 2, 2, cloth),
    ]))
    # Mantle: hangs from the shoulders, sways with the stride; star embroidery glows
    def mantle_fn(f, i, j, fw, fh):
        if f in ("top", "bottom"):
            return "s"
        if f in ("left", "right"):
            return "s" if j % 3 == 0 else "bd"
        if j == fh - 1 or (j == fh - 2 and i % 2 == 0):
            return "s"
        if i in (0, fw - 1):
            return "sd"
        if f == "back" and (i * 7 + j * 3) % 17 == 0:
            return "c*"
        if f == "back" and (i * 5 + j) % 23 == 0:
            return "w*"
        return "bd" if (i + 2) % 4 == 0 else "b"
    pieces.append(Piece("mantle", "BODY", "CHEST", 0, "SWAY", (0, 0.0, 3.3, 0, 0, 0), boxes=[
        Box(-5.0, 0.0, 0.0, 10, 19, 1, mantle_fn),
    ]))
    pieces.append(Piece("collar", "BODY", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-5.5, -1.6, -3.6, 11, 2, 1, silver),
        Box(-5.5, -1.6, 2.6, 11, 2, 1, silver),
        Box(-1.0, -0.4, -4.0, 2, 2, 1, gem("w*", "c*")),
    ]))
    # Moon motes circling behind the shoulders (full, half, crescent)
    motes = []
    for n, (fn, size) in enumerate([(solid("w*"), 2), (gem("w*", "s*"), 2), (gem("s*", "sd"), 1)]):
        a = 2 * math.pi * n / 3
        motes.append(Part(f"m{n}", (round(math.sin(a) * 11, 3), round(-math.cos(a) * 11, 3), 0, 0, 0, 0),
                          [Box(-size / 2, -size / 2, -0.5, size, size, 1, fn)]))
    pieces.append(Piece("moon_motes", "BODY", "CHEST", 0, "SPIN_Z_SLOW", (0, 4.0, 6.5, 0, 0, 0), children=motes))
    # Moonsilver pauldrons with a crescent boss
    pr = Piece("pauldron_right", "RIGHT_ARM", "CHEST", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-4.5, -4.0, -3.5, 5, 2, 7, silver),
        Box(-5.2, -2.5, -3.0, 1, 3, 6, plate("p", "s", "sd")),
        # crescent boss on the outer face
        Box(-5.6, -1.6, -1.5, 1, 1, 1, solid("w*")), Box(-5.6, -0.6, -1.0, 1, 1, 2, solid("w*")),
        Box(-5.6, -1.6, 0.5, 1, 1, 1, solid("w*")),
    ])
    pieces += [pr, mirror(pr, "pauldron_left", "LEFT_ARM")]
    # Robe panels over the legs
    def robe_fn(f, i, j, fw, fh):
        if f in ("top", "bottom"):
            return "bd"
        if j == fh - 1:
            return "s"
        if j == fh - 2 and i % 2 == 1:
            return "c*"
        return "bd" if i == 0 or (i + j) % 5 == 0 else "b"
    rr = Piece("robe_right", "RIGHT_LEG", "LEGS", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-2.6, -0.5, -3.2, 5, 9, 1, robe_fn),
        Box(-3.2, -0.5, -2.6, 1, 8, 5, robe_fn),
    ])
    pieces += [rr, mirror(rr, "robe_left", "LEFT_LEG")]
    # Crescent ankle guards
    ag = Piece("ankle_right", "RIGHT_LEG", "FEET", 0, "NONE", (0, 0, 0, 0, 0, 0), boxes=[
        Box(-3.5, 7.5, -2.0, 1, 1, 1, solid("w*")), Box(-3.5, 8.5, -2.5, 1, 2, 1, solid("w*")),
        Box(-3.5, 10.5, -2.0, 1, 1, 1, solid("w*")), Box(-3.3, 8.0, -1.0, 1, 3, 3, silver),
    ])
    pieces += [ag, mirror(ag, "ankle_left", "LEFT_LEG")]
    return pieces


ORNAMENTS = {"seraph": seraph_pieces, "solar": solar_pieces, "lunar": lunar_pieces}


class Atlas:
    """Packs box UVs row by row into a texture and paints them."""

    def __init__(self, pal, w=128, h=128):
        self.pal, self.w, self.h = pal, w, h
        self.cursor = [0, 0, 0]
        self.base, self.glow = png.blank(w, h), png.blank(w, h)

    def alloc(self, b):
        if b.src is not None:
            b.u, b.v = b.src.u, b.src.v
            return
        uw, uh = 2 * b.d + 2 * b.w, b.d + b.h
        if self.cursor[0] + uw > self.w:
            self.cursor = [0, self.cursor[1] + self.cursor[2], 0]
        b.u, b.v = self.cursor[0], self.cursor[1]
        assert b.v + uh <= self.h, "ornament atlas full"
        self.cursor[0] += uw
        self.cursor[2] = max(self.cursor[2], uh)
        paint(self.base, self.glow, self.pal, b.u, b.v, b.w, b.h, b.d, b.fn)

    def part(self, p):
        for b in p.boxes:
            self.alloc(b)
        for c in p.children:
            self.part(c)


def build_ornaments(name):
    pieces = ORNAMENTS[name]()
    atlas = Atlas(PALETTES[name])
    for pc in pieces:
        atlas.part(pc.part)
    return pieces, atlas


# ------------------------------------------------------------------ item icons (16x16)
# o outline, h highlight, m mid, d dark, a trim light, A trim dark, g gem, G gem dark, . clear

ICON_KEYS = {
    "seraph": {"o": "o", "h": "w", "m": "p", "d": "s", "a": "gl", "A": "g", "g": "c", "G": "cd"},
    "solar": {"o": "o", "h": "y", "m": "gl", "d": "g", "a": "w", "A": "gd", "g": "w", "G": "f"},
    "lunar": {"o": "o", "h": "p", "m": "s", "d": "sd", "a": "b", "A": "bd", "g": "w", "G": "c"},
}

ICONS = {
    "seraph_helmet": [
        "................",
        "....oooooooo....",
        "...oaAgaaAgao...",
        "....oooooooo....",
        ".o...oooooo...o.",
        "oho.ohhhhhmo.oho",
        "ohhoohhaahmooddo",
        ".ohhohmaamdohdo.",
        "..ohohmaamdohdo.",
        "...oohmaamdoo...",
        "....ohgo.ogdo...",
        "....ohmo.omdo...",
        "....ohmo.omdo...",
        "....oddo.oddo...",
        ".....oo...oo....",
        "................"],
    "seraph_chestplate": [
        "................",
        "..ooo......ooo..",
        ".oaahoooooohaao.",
        "oaahhhhaahhhhAao",
        "oAohhhaggahhdoAo",
        ".ooohhaggadmooo.",
        "...ohhhGGhmdo...",
        "...ohmmGGmmdo...",
        "...ohdmGGmddo...",
        "...ohmmmmmmdo...",
        "...oaaaggaaAo...",
        "...oAAAAAAAAo...",
        "...ohmmmmmmdo...",
        "...oooooooooo...",
        "................",
        "................"],
    "seraph_leggings": [
        "................",
        "...oooooooooo...",
        "...oaaaggaaAo...",
        "...oAAAAAAAAo...",
        "...ohhmoohmdo...",
        "..oohhmoohmdoo..",
        ".ohhhmdo.ohmddo.",
        ".oghmdo..ohmdgo.",
        "..oaado..oaAdo..",
        "..ohhmo..ohmdo..",
        "..ohGmo..ohGdo..",
        "..ohmdo..ohmdo..",
        "..ohmdo..ohmdo..",
        "..oddo....oddo..",
        "...oo......oo...",
        "................"],
    "seraph_boots": [
        "................",
        "................",
        "................",
        "................",
        "..oooo....oooo..",
        "..oaao....oaao..",
        "o.ohmo....ohmo.o",
        "hoogmo....ogmooh",
        ".hohmo....ohmoh.",
        "..ohmdo...ohmdo.",
        ".ohhmmdo.ohhmmdo",
        "ohhmmmdo.ohmmmdo",
        "oaaaaAAo.oaaaAAo",
        "oooooooo.ooooooo",
        "................",
        "................"],
    "solar_helmet": [
        "...h...a...h....",
        "....h..h..h.....",
        ".h...hhahh...h..",
        "..h.oooooooo.h..",
        "...ohhaaaahmo...",
        "..ohmmmggmmmdo..",
        "..ohmAAAAAAmdo..",
        "..ohmmmmmmmmdo..",
        "..ohmoooooomdo..",
        "..ohmo.gg.omdo..",
        "..ohmo....omdo..",
        "..oGGo....oGGo..",
        "..oddo....oddo..",
        "...oo......oo...",
        "................",
        "................"],
    "solar_chestplate": [
        "................",
        ".Go..........oG.",
        ".oGooo....oooGo.",
        "ohhmGhooooodGmdo",
        "ohmmmhhahhmmmmdo",
        ".oooohaggamdooo.",
        "...ohagggga do..",
        "...ohmaggadmdo..",
        "...ohmmaammddo..",
        "...ohmmmmmmmdo..",
        "...oGmGmGmGmGo..",
        "...oGGGGGGGGGo..",
        "...oooooooooo...",
        "................",
        "................",
        "................"],
    "solar_leggings": [
        "................",
        "...oooooooooo...",
        "...ohhhggmmmo...",
        "...oAAAAAAAAo...",
        "...ohhmoohmdo...",
        "..ohhmdo.ohmdo..",
        "..ohmmdo.ohmdo..",
        "..ohaado.ohaao..",
        "..ohmmdo.ohmdo..",
        "..oGmGdo.oGmGo..",
        "..ohmmdo.ohmdo..",
        "..oGGGo...oGGo..",
        "..oooo....oooo..",
        "................",
        "................",
        "................"],
    "solar_boots": [
        "................",
        "................",
        "................",
        "................",
        "..oooo....oooo..",
        "..ohho....ohho..",
        "..ohmo....ohmo..",
        ".Gogmo....ogmoG.",
        "..ohmo....ohmo..",
        "..ohmdo...ohmdo.",
        ".ohGmGdo.ohGmGdo",
        "ohGGGGGo.oGGGGGo",
        "oAAAAAAo.oAAAAAo",
        "oooooooo.ooooooo",
        "................",
        "................"],
    "lunar_helmet": [
        "................",
        "......g..g......",
        "......gggg......",
        ".....oooooo.....",
        "....oaaaaaAo....",
        "...oaAAAAAAAo...",
        "..oaAhmmgmmAAo..",
        "..oaAo.....oAo..",
        "..oaAo.....oAo..",
        "..oaAo.....oAo..",
        "..oaAAo...oAAo..",
        "..oaaAAo.oAAAo..",
        "...oaaAAoAAAo...",
        "....oooooooo....",
        "................",
        "................"],
    "lunar_chestplate": [
        "................",
        "..ooo......ooo..",
        ".ohhmoooooohmdo.",
        "ohhmmghmdgmmmddo",
        "oAoaahmmmmdaaoAo",
        ".oooahmgmmdaooo.",
        "...oahgmmmdAo...",
        "...oahgmmmdAo...",
        "...oaahmmdaAo...",
        "...oaaaaaaaAo...",
        "...oaGaaaGaAo...",
        "...oaaaaGaaAo...",
        "...ommmmmmmmo...",
        "...oooooooooo...",
        "................",
        "................"],
    "lunar_leggings": [
        "................",
        "...oooooooooo...",
        "...ohhmggmmdo...",
        "...oaaaaaaaAo...",
        "...oaaAooaaAo...",
        "..oaaAAo.oaaAo..",
        "..oaaAAo.oaaAo..",
        "..oaGaAo.oaGAo..",
        "..oaaAAo.oaaAo..",
        "..oaaaAo.oaaAo..",
        "..ohmmdo.ohmdo..",
        "..oooooo.ooooo..",
        "................",
        "................",
        "................",
        "................"],
    "lunar_boots": [
        "................",
        "................",
        "................",
        "................",
        "..oooo....oooo..",
        "..ohmo....ohmo..",
        ".gohmo....ohmog.",
        "g.ohmo....ohmo.g",
        "g.ohmo....ohmo.g",
        ".gohmdo...ohmdog",
        "..ohhmdo..ohhmdo",
        ".ohhmmdo.ohhmmdo",
        "oaaaaaAo.oaaaaAo",
        "oooooooo.ooooooo",
        "................",
        "................"],
}


def icon(name):
    s = name.split("_")[0]
    pal, keys = PALETTES[s], ICON_KEYS[s]
    rows = ICONS[name]
    img = png.blank(16, 16)
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, len(row))
        for x, ch in enumerate(row):
            if ch in ". ":
                continue
            img[y][x] = pal[keys[ch]]
    return img


# ------------------------------------------------------------------ Java output

def jf(v):
    v = round(float(v), 4)
    return f"{v:g}f" if v != int(v) else f"{int(v)}f"


def java_part(out, parent, p, var, depth):
    x, y, z, rx, ry, rz = p.pose
    cubes = "CubeListBuilder.create()" + "".join(
        f".texOffs({b.u}, {b.v}).addBox({jf(b.x)}, {jf(b.y)}, {jf(b.z)}, {jf(b.w)}, {jf(b.h)}, {jf(b.d)})" for b in p.boxes)
    if any(abs(a) > 1e-6 for a in (rx, ry, rz)):
        pose = f"PartPose.offsetAndRotation({jf(x)}, {jf(y)}, {jf(z)}, {jf(rx)}, {jf(ry)}, {jf(rz)})"
    elif any(abs(a) > 1e-6 for a in (x, y, z)):
        pose = f"PartPose.offset({jf(x)}, {jf(y)}, {jf(z)})"
    else:
        pose = "PartPose.ZERO"
    if p.children:
        out.append(f"        PartDefinition {var} = {parent}.addOrReplaceChild(\"{p.name}\", {cubes}, {pose});")
        for n, c in enumerate(p.children):
            java_part(out, var, c, f"{var}_{n}", depth + 1)
    else:
        out.append(f"        {parent}.addOrReplaceChild(\"{p.name}\", {cubes}, {pose});")


def java(sets):
    out = ["package media.hofferbert.astralvigil.client.armor;", "",
           "import java.util.List;",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.LayerDefinition;",
           "import net.minecraft.client.model.geom.builders.MeshDefinition;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;",
           "import net.minecraft.world.entity.EquipmentSlot;", "",
           "/**",
           " * GENERATED by tools/dev/gen_armor.py - do not edit. Celestial armour ornaments: every piece is a root child",
           " * (the pivot) whose single child {@code body} carries the geometry, so animations can be applied on the pose stack",
           " * without mutating shared model parts. Box UVs match {@code textures/entity/armor/<set>_ornaments.png}.",
           " */",
           "final class ArmorMeshes {",
           "    private ArmorMeshes() {}", ""]
    for name, (pieces, atlas) in sets.items():
        out.append(f"    static final List<ArmorOrnaments.Piece> {name.upper()}_PIECES = List.of(")
        for n, pc in enumerate(pieces):
            end = "" if n == len(pieces) - 1 else ","
            out.append(f"            new ArmorOrnaments.Piece(\"{pc.name}\", ArmorOrnaments.Bone.{pc.bone}, EquipmentSlot.{pc.slot}, "
                       f"{pc.level}, ArmorOrnaments.Anim.{pc.anim}){end}")
        out.append("    );")
        out.append("")
        out.append(f"    static LayerDefinition {name}() {{")
        out.append("        MeshDefinition mesh = new MeshDefinition();")
        out.append("        PartDefinition root = mesh.getRoot();")
        for n, pc in enumerate(pieces):
            x, y, z, rx, ry, rz = pc.part.pose
            pivot = Part(pc.name, pc.part.pose)
            body = Part("body", (0,) * 6, pc.part.boxes, pc.part.children)
            out.append(f"        PartDefinition p{n} = root.addOrReplaceChild(\"{pc.name}\", CubeListBuilder.create(), "
                       + (f"PartPose.offsetAndRotation({jf(x)}, {jf(y)}, {jf(z)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));"))
            java_part(out, f"p{n}", body, f"p{n}b", 1)
            del pivot
        out.append(f"        return LayerDefinition.create(mesh, {atlas.w}, {atlas.h});")
        out.append("    }")
        out.append("")
    out[-1] = "}"
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    open(JAVA, "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")


# ------------------------------------------------------------------ output

def save(path, img, limit=8):
    n = len(png.colours(img))
    assert n <= limit, f"{path}: {n} colours"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    png.write(path, img)


def main():
    preview_dir = None
    if "--preview" in sys.argv:
        preview_dir = sys.argv[sys.argv.index("--preview") + 1]
    built = {}
    for name in ("seraph", "solar", "lunar"):
        l1, g1, l2, g2 = armor_layers(name)
        save(os.path.join(ASSETS, "entity", "equipment", "humanoid", name + ".png"), l1)
        save(os.path.join(ASSETS, "entity", "equipment", "humanoid_leggings", name + ".png"), l2)
        save(os.path.join(ASSETS, "models", "armor", name + "_layer_1.png"), l1)
        save(os.path.join(ASSETS, "models", "armor", name + "_layer_2.png"), l2)
        save(os.path.join(ASSETS, "entity", "armor", name + "_glow.png"), g1)
        save(os.path.join(ASSETS, "entity", "armor", name + "_leggings_glow.png"), g2)
        pieces, atlas = build_ornaments(name)
        save(os.path.join(ASSETS, "entity", "armor", name + "_ornaments.png"), atlas.base)
        save(os.path.join(ASSETS, "entity", "armor", name + "_ornaments_glow.png"), atlas.glow)
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            save(os.path.join(ASSETS, "item", f"{name}_{piece}.png"), icon(f"{name}_{piece}"))
        built[name] = (pieces, atlas)
        if preview_dir:
            import armor_preview  # noqa: E402  (tools/dev/armor_preview.py)
            armor_preview.render(name, l1, g1, l2, g2, pieces, atlas, preview_dir)
    java(built)
    print("armour: textures in", ASSETS, "- mesh class", JAVA)


if __name__ == "__main__":
    main()
