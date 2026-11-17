"""Generates the Watcher GeckoLib model, animations and the four Watcher textures (+ emissive glowmasks).

Run: python -I tools/dev/gen_watcher.py
Outputs (both GeckoLib layouts):
  assets/astralvigil/{geo,geckolib/models}/entity/watcher.geo.json
  assets/astralvigil/{animations,geckolib/animations}/entity/watcher.animation.json
  assets/astralvigil/textures/entity/watcher/<id>.png and <id>_glowmask.png

Design: a floating, faceless seraph - six wings, a sun-disc nimbus behind the head, a spinning halo above it, a
glowing core in the chest, crystal shards orbiting the waist and a robe that dissolves into a trail of light.
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
TW, TH = 256, 128

# palette keys: D outline, R robe, S robe shade, L light, M metal, N metal dark, G glow, W white
PALETTES = {
    "solenne": {"D": "7A5A12", "R": "F4EEDC", "S": "D8CFB4", "L": "FFFFFF", "M": "F2C94C", "N": "C9971E", "G": "FFE58A", "W": "FFF8E0"},
    "vael": {"D": "1E1A2E", "R": "AFC3E0", "S": "7F93B8", "L": "E6EEFA", "M": "D8D8E6", "N": "9AA3B8", "G": "8FEFFF", "W": "FFFFFF"},
    "orrin": {"D": "1E1A2E", "R": "4F6F8F", "S": "33506B", "L": "7FA3C2", "M": "C9971E", "N": "7A5A12", "G": "8FEFFF", "W": "FFFFFF"},
    "ilyra": {"D": "0F0C1A", "R": "2C2A5E", "S": "1E1A2E", "L": "4A4790", "M": "F2C94C", "N": "C9971E", "G": "B9F6FF", "W": "FFFFFF"},
}


# ------------------------------------------------------------------ materials: painter(face, x, y, w, h) -> (base, glow)

def edge(x, y, w, h):
    return x == 0 or y == 0 or x == w - 1 or y == h - 1


def mat_robe(face, x, y, w, h):
    if y == h - 1:
        return "M", None
    if face in ("top", "bottom"):
        return "R", None
    if y == 0:
        return "L", None
    return ("S" if x % 3 == 0 else "R"), None


def mat_armor(face, x, y, w, h):
    if edge(x, y, w, h):
        return "N", None
    return ("L" if (x + y) % 5 == 0 else "M"), None


def mat_trim(face, x, y, w, h):
    return ("N" if y == h - 1 else "M"), None


def mat_helm(face, x, y, w, h):
    if face == "front":
        if y == h // 2 and 0 < x < w - 1:
            return ("W" if x in (1, w - 2) else "G"), ("W" if x in (1, w - 2) else "G")
        if y == h // 2 + 1 and x in (2, w - 3):
            return "G", "G"
        return ("D" if edge(x, y, w, h) else "L"), None
    return ("N" if y == h - 1 else "M" if y == 0 or face == "top" else "L"), None


def mat_glow(face, x, y, w, h):
    c = "W" if (x + y) % 2 == 0 else "G"
    return c, c


def mat_halo(face, x, y, w, h):
    c = "W" if y == 0 and x % 3 == 1 else "G"
    return "M", c


def mat_feather(face, x, y, w, h):
    if face in ("top", "bottom"):
        return "L", None
    if y >= h - 2:
        return "G", ("W" if (x + y) % 2 else "G")
    if y % 3 == 2:
        return "R", None
    return "L", None


def mat_wingbar(face, x, y, w, h):
    if face == "bottom":
        return "N", None
    return ("N" if y == h - 1 else "M"), ("G" if y == 0 and x % 4 == 1 else None)


def mat_tail(face, x, y, w, h):
    return "L", ("W" if y < 2 else "G")


def mat_hem(face, x, y, w, h):
    return "M", ("W" if (x + y) % 3 == 0 else "G")


def mat_crown(face, x, y, w, h):
    if y == 0 or face == "top":
        return "W", "W"
    return ("M" if y < 3 else "N"), ("G" if y == 1 else None)


def mat_covert(face, x, y, w, h):
    if face in ("top", "bottom"):
        return "L", None
    if y == h - 1:
        return ("S" if x % 2 else "L"), None
    return ("L" if y == 0 else "R"), None


def mat_primary(face, x, y, w, h):
    if face in ("top", "bottom"):
        return "L", None
    if y >= h - 3 and (x + (h - y)) % 2 == 0 or y == h - 1:
        return "G", ("W" if y == h - 1 else "G")
    if x % 4 == 3 and y > 1:
        return "R", None
    return "L", None


def mat_stole(face, x, y, w, h):
    if x == 1 and y % 4 == 2:
        return "G", "G"
    return ("N" if x != 1 else "M"), None


def mat_skin(face, x, y, w, h):
    return ("S" if y == h - 1 else "L"), None


MATERIALS = {"robe": mat_robe, "armor": mat_armor, "trim": mat_trim, "helm": mat_helm, "glow": mat_glow,
             "halo": mat_halo, "feather": mat_feather, "wingbar": mat_wingbar, "tail": mat_tail, "skin": mat_skin,
             "hem": mat_hem, "crown": mat_crown, "covert": mat_covert, "primary": mat_primary, "stole": mat_stole}


# ------------------------------------------------------------------ model building

class Model:
    def __init__(self):
        self.bones = []
        self.uv = {}
        self.cursor = [0, 0, 0]
        self.layout = []  # (u, v, w, h, d, material)

    def bone(self, name, parent, pivot, rotation=None):
        b = {"name": name, "pivot": list(pivot), "cubes": []}
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
        if self.cursor[0] + uw > TW:
            self.cursor = [0, self.cursor[1] + self.cursor[2], 0]
        u, v = self.cursor[0], self.cursor[1]
        assert v + uh <= TH, "texture full"
        self.cursor[0] += uw
        self.cursor[2] = max(self.cursor[2], uh)
        self.uv[key] = (u, v)
        self.layout.append((u, v, w, h, d, material))
        return u, v

    def cube(self, bone, origin, size, material, pivot=None, rotation=None, inflate=None):
        w, h, d = size
        u, v = self.alloc(int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d)), material)
        c = {"origin": [round(o, 3) for o in origin], "size": list(size), "uv": [u, v]}
        if rotation:
            c["pivot"] = list(pivot)
            c["rotation"] = list(rotation)
        if inflate:
            c["inflate"] = inflate
        bone["cubes"].append(c)


def build():
    m = Model()
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 34, 0))
    # flared celestial gown with a glowing hem; the figure floats, there are no legs
    m.cube(body, (-4, 34, -2.5), (8, 4, 5), "armor")            # waist
    m.cube(body, (-5, 27, -3.5), (10, 7, 7), "robe")             # gown top
    m.cube(body, (-6, 20, -4), (12, 7, 8), "robe")               # gown middle
    m.cube(body, (-7, 13, -5), (14, 7, 10), "robe")              # gown flare
    m.cube(body, (-7.5, 12, -5.5), (15, 1, 11), "hem")           # glowing hem
    m.cube(body, (-5.5, 33.5, -3.75), (11, 1, 7.5), "trim")      # belt
    m.cube(body, (-5.5, 38, -3.25), (11, 9, 6.5), "armor")       # chest
    m.cube(body, (-1.5, 41, -3.9), (3, 3, 1), "glow")            # core gem
    m.cube(body, (-1.5, 13, -5.4), (3, 21, 0.5), "stole")         # gold stole down the gown front
    m.cube(body, (-4.5, 14, 4.2), (9, 33, 1), "robe")            # mantle hanging down the back
    for sx in (-1, 1):                                            # pauldrons with upswept points
        x = 4.5 if sx > 0 else -9.5
        m.cube(body, (x, 44, -4), (5, 3, 8), "trim")
        m.cube(body, (x + (3 if sx > 0 else 0), 47, -1), (2, 3, 2), "trim")
    m.cube(body, (-4, 47, -3), (8, 2, 6), "trim")                # collar
    tail = m.bone("tail", "body", (0, 12, 0))                     # drips of light under the gown
    for x, z, h in ((-4, -2, 6), (2, 1, 8), (-1, 2.5, 5), (3.5, -3, 4), (-5, 3, 3)):
        m.cube(tail, (x, 12 - h, z), (1, h, 1), "tail")

    head = m.bone("head", "body", (0, 49, 0))
    m.cube(head, (-4, 49, -4), (8, 9, 8), "helm")
    for x, hgt in ((-0.5, 7), (-2.5, 5), (1.5, 5), (-4, 3), (3, 3)):   # sunburst crown
        m.cube(head, (x, 58, -2), (1, hgt, 1), "crown")
    m.cube(head, (-4.5, 57, -4.5), (9, 1, 9), "trim")

    nimbus = m.bone("nimbus", "head", (0, 55, 7))                # sun disc framing the head
    n = 24
    for i in range(n):
        m.cube(nimbus, (-1.75, 55 + 12, 6.5), (3.5, 1.5, 1), "halo", pivot=(0, 55, 7), rotation=(0, 0, i * 360 / n))
    for i in range(12):
        m.cube(nimbus, (-0.5, 55 + 5, 6.75), (1, 6, 0.5), "glow", pivot=(0, 55, 7), rotation=(0, 0, i * 30 + 15))
    halo = m.bone("halo", "head", (0, 66, 0), rotation=(8, 0, 0))   # tilted crown halo above the head
    for i in range(16):
        m.cube(halo, (-1.5, 66, -7), (3, 1, 1), "halo", pivot=(0, 66, 0), rotation=(0, i * 22.5, 0))

    for side, sx in (("right", -1), ("left", 1)):
        arm = m.bone("arm_" + side, "body", (sx * 7, 45, 0))
        def at(x_left, width):  # x origin for the left arm, mirrored for the right
            return x_left if sx > 0 else -x_left - width
        m.cube(arm, (at(5.5, 4), 33, -2), (4, 12, 4), "robe")
        m.cube(arm, (at(5, 5), 30, -2.5), (5, 3, 5), "robe")      # wide sleeve end
        m.cube(arm, (at(4.75, 5.5), 33, -2.75), (5.5, 1, 5.5), "trim")
        m.cube(arm, (at(6, 3), 27, -1.5), (3, 3, 3), "skin")
        m.cube(arm, (at(4.5, 6), 37, -3), (6, 1, 6), "glow")      # floating bracer ring

    # three pairs of layered feather wings: upper raised, middle level, lower lowered; all swept back
    pairs = (("upper", 46, 30, 42), ("middle", 42, 26, 12), ("lower", 38, 20, -18))
    for name, py, length, roll in pairs:
        for side, sx in (("right", -1), ("left", 1)):
            wing = m.bone(f"wing_{name}_{side}", "body", (sx * 2.5, py, 4), rotation=(0, sx * -10, sx * -roll))
            def span(start, ln):  # x origin of a strip [start, start+ln] measured outward from the pivot
                return sx * 2.5 + (start if sx > 0 else -start - ln)
            m.cube(wing, (span(0, length), py - 1, 4), (length, 3, 1), "wingbar")
            m.cube(wing, (span(1, length - 2), py - 6, 4.2), (length - 2, 5, 1), "covert")
            m.cube(wing, (span(3, length - 6), py - 13, 4.4), (length - 6, 7, 1), "feather")
            m.cube(wing, (span(length - 9, 10), py - 17, 4.6), (10, 15, 1), "primary")

    orbit = m.bone("orbit", "root", (0, 38, 0))                  # crystal shards circling the chest
    for i in range(6):
        a = math.radians(i * 60 + 30)
        cx, cz = math.sin(a) * 14, math.cos(a) * 14
        m.cube(orbit, (cx - 1.5, 35, cz - 1.5), (3, 6, 3), "glow", pivot=(cx, 38, cz), rotation=(0, 45, 0))
        m.cube(orbit, (cx - 0.5, 41, cz - 0.5), (1, 2, 1), "glow", pivot=(cx, 38, cz), rotation=(0, 45, 0))
    return m


def geo(m):
    bones = []
    for b in m.bones:
        b = dict(b)
        if not b["cubes"]:
            del b["cubes"]
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.watcher", "texture_width": TW, "texture_height": TH,
                        "visible_bounds_width": 7, "visible_bounds_height": 7, "visible_bounds_offset": [0, 3, 0]},
        "bones": bones}]}


def animations():
    wings = {}
    for name, amp, phase in (("upper", 7, 0), ("middle", 5, 40), ("lower", 4, 80)):
        for side, s in (("right", 1), ("left", -1)):
            wings[f"wing_{name}_{side}"] = {"rotation": [
                "0", f"{s} * math.sin(query.anim_time * 90 + {phase}) * {amp}", f"{s} * math.sin(query.anim_time * 90 + {phase}) * {amp / 2}"]}
    idle = {"loop": True, "animation_length": 8, "bones": {
        "root": {"position": ["0", "4 + math.sin(query.anim_time * 90) * 1.5", "0"]},
        "body": {"rotation": ["math.sin(query.anim_time * 45) * 1.5", "0", "0"]},
        "head": {"rotation": ["math.sin(query.anim_time * 90 + 30) * 2", "math.sin(query.anim_time * 45) * 6", "0"]},
        "nimbus": {"rotation": ["0", "0", "query.anim_time * 22.5"]},
        "halo": {"rotation": ["0", "query.anim_time * 45", "0"], "position": ["0", "math.sin(query.anim_time * 180) * 0.5", "0"]},
        "orbit": {"rotation": ["0", "query.anim_time * -45", "0"]},
        "tail": {"rotation": ["math.sin(query.anim_time * 90 + 60) * 6", "0", "math.sin(query.anim_time * 45) * 4"]},
        "arm_right": {"rotation": ["math.sin(query.anim_time * 90) * 3", "0", "4"]},
        "arm_left": {"rotation": ["math.sin(query.anim_time * 90 + 90) * 3", "0", "-4"]},
        **wings}}
    spread = {f"wing_{n}_{s}": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, (25 if s == "right" else -25), (12 if s == "right" else -12)],
                                              "1.5": [0, 0, 0]}} for n in ("upper", "middle", "lower") for s in ("right", "left")}
    cast = {"animation_length": 1.5, "bones": {
        "root": {"position": {"0.0": [0, 4, 0], "0.5": [0, 7, 0], "1.5": [0, 4, 0]}},
        "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.4": [-150, 0, 20], "1.1": [-150, 0, 20], "1.5": [0, 0, 0]}},
        "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.4": [-150, 0, -20], "1.1": [-150, 0, -20], "1.5": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [-20, 0, 0], "1.5": [0, 0, 0]}},
        "nimbus": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 360]}, "scale": {"0.0": [1, 1, 1], "0.5": [1.3, 1.3, 1.3], "1.5": [1, 1, 1]}},
        "halo": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 720, 0]}},
        "orbit": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, -540, 0]}, "scale": {"0.0": [1, 1, 1], "0.5": [1.4, 1, 1.4], "1.5": [1, 1, 1]}},
        **spread}}
    talk = {"loop": True, "animation_length": 2, "bones": {
        "root": {"position": ["0", "4 + math.sin(query.anim_time * 180) * 0.5", "0"]},
        "head": {"rotation": ["math.sin(query.anim_time * 360) * 4", "0", "0"]},
        "arm_right": {"rotation": ["-30 + math.sin(query.anim_time * 180) * 10", "0", "10"]},
        "arm_left": {"rotation": ["0", "0", "-4"]},
        "nimbus": {"rotation": ["0", "0", "query.anim_time * 45"]}}}
    return {"format_version": "1.8.0", "animations": {"idle": idle, "cast": cast, "talk": talk}, "geckolib_format_version": 2}


FACE_ORDER = ("top", "bottom", "east", "front", "west", "back")


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def textures(m):
    for wid, pal in PALETTES.items():
        colours = {k: png.hexc(c) for k, c in pal.items()}
        base, glow = png.blank(TW, TH), png.blank(TW, TH)
        for u, v, w, h, d, material in m.layout:
            paint = MATERIALS[material]
            for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
                for y in range(fh):
                    for x in range(fw):
                        b, g = paint(face, x, y, fw, fh)
                        base[fy + y][fx + x] = colours[b]
                        if g:
                            glow[fy + y][fx + x] = colours[g]
        for name, img in ((wid, base), (wid + "_glowmask", glow)):
            n = len(png.colours(img))
            assert n <= 8, f"{name}: {n} colours"
            png.write(os.path.join(ASSETS, "textures", "entity", "watcher", name + ".png"), img)


def write_json(rel_dirs, name, data):
    for d in rel_dirs:
        path = os.path.join(ASSETS, *d.split("/"), name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=1)
            f.write("\n")


def main():
    m = build()
    write_json(("geo/entity", "geckolib/models/entity"), "watcher.geo.json", geo(m))
    write_json(("animations/entity", "geckolib/animations/entity"), "watcher.animation.json", animations())
    textures(m)
    print("watcher:", sum(len(b["cubes"]) for b in m.bones), "cubes,", len(m.layout), "uv boxes, cursor", m.cursor)


if __name__ == "__main__":
    main()
