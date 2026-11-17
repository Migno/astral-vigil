"""Generates the Watcher GeckoLib model, animations and the four Watcher textures (+ emissive glowmasks).

Run: python -I tools/dev/gen_watcher.py [--preview <dir>]
Outputs (both GeckoLib layouts):
  assets/astralvigil/{geo,geckolib/models}/entity/watcher.geo.json
  assets/astralvigil/{animations,geckolib/animations}/entity/watcher.animation.json
  assets/astralvigil/textures/entity/watcher/<id>.png and <id>_glowmask.png
--preview renders front/side/back/three-quarter contact sheets (textured + silhouette) of the key poses.

Design (docs/ART_GUIDE.md): a towering seraph, seven heads tall. Silhouette ideas: the tilted halo and six layered
wings. Each Watcher shares the rig but carries its own attribute; the prop bones exist for all four and are painted
fully transparent on the textures of the other three (cutout render type), so nothing needs hiding in code:
  Solenne  sun crown + rayed sun disc                Vael   crescent moon cradle + crescent diadem
  Orrin    starsmith hammer + anvil sigil in a gear  Ilyra  chronicle tome, quill + arc of rune tablets
Joint chains: shoulder > upper arm > forearm > hand > fingers > fingertips; wing root > mid > tip > primary fan;
gown panels, mantle, veil and ribbons in two segments each. 1 texel = 1 unit everywhere, sizes are whole numbers.
Textures: 3-4 tone ramps per material with hue shift (cool shadows, warm lights), light from the top-left, ambient
occlusion at the bottom of every side face, rim light on top edges; glow only on eyes, core, bracers, halo, hem,
feather tips, orbit shards and the attribute's runes (the script asserts <= 15 % emissive surface).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import geckorig as gr  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil")
TW, TH = 256, 128
Y = 4  # the Watcher floats: every part is raised by this much (keeps the float height out of the animations)
MAX_EMISSIVE = 0.15

# ramps: c cloth, m metal, f feathers, s porcelain skin (0 = darkest); g glow, a attribute accent, D outline
PALETTES = {
    "solenne": {"D": "3D2408",
                "c0": "8E7486", "c1": "CDB9A2", "c2": "EFE3CB", "c3": "FFFAEC",
                "m0": "7A5A12", "m1": "C9971E", "m2": "F2C94C", "m3": "FFF0B0",
                "f0": "9A8FAE", "f1": "D6CFE0", "f2": "F3EFF6", "f3": "FFFDF4",
                "s0": "B3907E", "s1": "E6CDB8", "s2": "FFF2E4",
                "g0": "FFC94A", "g1": "FFF6CC",
                "a0": "A8521A", "a1": "EE9330", "a2": "FFD27A"},
    "vael": {"D": "141226",
             "c0": "36406E", "c1": "6F84AE", "c2": "A9BEDE", "c3": "E1EBFA",
             "m0": "525A74", "m1": "959FB8", "m2": "D3D6E6", "m3": "FFFFFF",
             "f0": "66729C", "f1": "AEBAD8", "f2": "DDE6F5", "f3": "FAFCFF",
             "s0": "8A90B2", "s1": "C8D0E6", "s2": "EFF3FF",
             "g0": "8FEFFF", "g1": "E8FDFF",
             "a0": "4A5A8C", "a1": "A6C2EE", "a2": "EAF4FF"},
    "orrin": {"D": "0E1018",
              "c0": "1B2636", "c1": "2F4A63", "c2": "4A6A88", "c3": "7A9EBC",
              "m0": "5A3812", "m1": "985F20", "m2": "D2983A", "m3": "F6D488",
              "f0": "37435A", "f1": "687B96", "f2": "A3B4CA", "f3": "DAE4EE",
              "s0": "7A6A66", "s1": "B6A498", "s2": "E0D2C4",
              "g0": "FF9F43", "g1": "FFE3B0",
              "a0": "353A48", "a1": "687084", "a2": "A6AEC0"},
    "ilyra": {"D": "0B0816",
              "c0": "191436", "c1": "2B285C", "c2": "44428A", "c3": "6C69B6",
              "m0": "7A5A12", "m1": "C9971E", "m2": "F2C94C", "m3": "FFF0B0",
              "f0": "382D64", "f1": "675C9E", "f2": "A49CD4", "f3": "E0DCF8",
              "s0": "837A9C", "s1": "C0B8D6", "s2": "ECE8F6",
              "g0": "B98CFF", "g1": "EEE2FF",
              "a0": "5B2A86", "a1": "B7A07A", "a2": "F2E8CC"},
}
PROP_OWNER = {"sun": "solenne", "moon": "vael", "forge": "orrin", "tome": "ilyra"}


# ------------------------------------------------------------------ materials: painter(Face, watcher) -> (key, glow)

def r(prefix, i, n=4):
    return f"{prefix}{max(0, min(n - 1, i))}"


def m_cloth(f, w, base=2, folds=True):
    t = gr.shade(f, base)
    if folds and f.side and f.w > 3:
        p = (f.x + f.cd + f.cw) % 5
        if p == 0:
            t -= 1
        elif p == 2 and f.y < f.h - 1:
            t += 1
    return r("c", t), None


def m_cloth_dark(f, w):
    return m_cloth(f, w, base=1)


def m_inner(f, w):
    return ("c0" if f.face != "top" else "c1"), None


def hem(f, w, rows=2, glow=True):
    """Gold hem on the last rows of side faces; the very last row glows every other pixel."""
    if f.side and f.y >= f.h - rows:
        if f.y == f.h - 1 and glow and f.face in ("front", "back"):
            return ("g0", "g0") if f.x % 2 == 0 else ("m2", None)
        return ("m3" if f.y == f.h - rows and f.face != "back" else "m1"), None
    return None


def m_panel(f, w):
    if f.side and f.x in (0, f.w - 1) and f.w > 2:
        return ("m2" if f.x == 0 else "m1"), None  # gold piping on the panel edges
    return m_cloth(f, w)


def m_panel_hem(f, w):
    return hem(f, w) or m_panel(f, w)


def m_gown(f, w):
    if f.face == "bottom":
        return "c0", None
    return m_cloth(f, w, base=2)


def m_gown_hem(f, w):
    if f.face == "bottom":
        return ("c0" if 0 < f.x < f.w - 1 and 0 < f.y < f.h - 1 else "m1"), None
    return hem(f, w) or m_cloth(f, w, base=2)


def m_tabard(f, w):
    if f.face == "front" and f.w // 2 - 1 <= f.x <= f.w // 2 and f.y % 4 == 1:
        return "m3", None  # gold stitched sigils
    return m_panel(f, w)


def m_tabard_hem(f, w):
    return hem(f, w) or m_tabard(f, w)


def m_vambrace(f, w):
    if f.side and f.y % 3 == 0:
        return r("m", gr.shade(f, 1)), None  # banded gold plates
    return r("m", gr.shade(f, 2)), None


def m_metal(f, w, base=2):
    t = gr.shade(f, base)
    if f.side and f.edge and f.w > 2 and f.h > 2:
        t = 0 if f.y == f.h - 1 or f.x == f.w - 1 else 3
    elif f.side and (f.x + 2 * f.y) % 7 == 3 and t >= 2:  # filigree glints
        t = 3
    return r("m", t), None


def m_belt(f, w):
    if f.face == "front" and f.w // 2 - 1 <= f.x <= f.w // 2:
        return ("g1", "g1") if f.y == 0 else ("m3", None)  # buckle gem
    return m_metal(f, w)


def m_chest(f, w):
    if f.face == "front":
        cx = f.w // 2
        if f.y == 0:
            return "m3", None
        if f.y == f.h - 1:
            return "m0", None
        if f.x in (0, f.w - 1):
            return ("m1" if f.x == 0 else "m0"), None
        if f.x in (cx - 1, cx) and f.y > 1:
            return "m1", None  # sternum ridge
        if f.y == 4 and f.x not in (cx - 1, cx):
            return "m1", None  # pectoral line
        if f.y < 4:
            return ("m3" if f.x < cx and f.y == 1 else "m2"), None
        return ("c2" if f.y >= 6 else "m2"), None
    return m_metal(f, w)


def m_abdomen(f, w):
    if f.side and f.y % 2 == 1:
        return r("c", gr.shade(f, 1)), None  # wrapped sash bands
    return m_cloth(f, w, folds=False)


def m_skin(f, w):
    t = gr.shade(f, 1, levels=3, rim=False)
    return r("s", t, 3), None


def m_fingers(f, w):
    t = gr.shade(f, 1, levels=3, rim=False)
    if f.face in ("right", "left", "bottom") and f.x % 2 == 1:
        t -= 1  # separate the fingers
    return r("s", t, 3), None


def m_head(f, w):
    if f.face == "front":
        x, y = f.x, f.y
        if y == 0 or x in (0, 7):
            return r("c", 2 if y == 0 else 1), None  # hood frame
        if y == 3 and x in (1, 2, 5, 6):
            return ("g1", "g1") if x in (2, 5) else ("g0", "g0")  # eyes
        if y == 2 and x in (1, 2, 5, 6):
            return "s1", None  # brow shadow
        if y == 4 and x == 4 or y == 5 and x == 4:
            return "s1", None  # nose shadow (light from the left)
        if y == 6 and x in (3, 4):
            return "s1", None  # mouth
        if y == 7:
            return ("s0" if x in (1, 6) else "s1"), None  # jaw in shadow
        return ("s2" if x < 5 or y < 3 else "s1"), None
    if f.face == "top":
        return ("c3" if (f.x + f.y) % 4 == 0 else "c2"), None
    return m_cloth(f, w)


def m_circlet(f, w):
    if f.face == "front" and f.x == f.w // 2:
        return "g1", "g1"
    return m_metal(f, w)


def m_halo(f, w):
    if f.face in ("front", "back", "bottom"):
        return ("g1", "g1") if f.x % 3 == 1 else ("g0", "g0")
    return ("m3" if f.face == "top" else "m2"), None


def m_core(f, w):
    if f.face == "front":
        return ("g1", "g1") if f.y == 1 else ("g0", "g0")
    return "m1", None


def m_glow_ring(f, w):
    if f.side:
        return ("g0", "g0") if f.x % 2 == 0 else ("m2", None)
    return "m1", None


def m_sleeve(f, w):
    if f.side and f.y == f.h - 1:
        return ("m2" if f.face != "back" else "m1"), None
    if f.side and f.x in (0, f.w - 1) and f.w > 2:
        return "m1", None
    return m_cloth(f, w, base=2)


def m_veil(f, w):
    if f.side and f.x in (0, f.w - 1) and f.w > 2:
        return ("m2" if f.x == 0 else "m1"), None
    if f.side and f.y == f.h - 1:
        return "m2", None
    return m_cloth(f, w, base=2)


def m_ribbon(f, w):
    if f.side and f.y >= f.h - 1:
        return "m2", None
    return r("c", gr.shade(f, 3) if f.x == 0 else gr.shade(f, 2)), None


def m_mantle(f, w):
    if f.side and (f.x in (0, f.w - 1)) and f.w > 2:
        return ("m2" if f.x == 0 else "m1"), None
    if f.face == "back" and (f.x * 7 + f.y * 3) % 23 == 0:
        return "c3", None  # embroidered stars
    return m_cloth(f, w, base=1)


def m_wingbar(f, w):
    if f.face == "top" or f.side and f.y == 0:
        return "m3", None
    if f.face == "bottom":
        return "f1", None
    return ("m2" if f.x % 4 else "m1"), None


def feather_mask(f, width, stagger):
    """Pointed, staggered feather tips cut into the lower edge (alpha): returns (opaque, is_tip_row, column in feather)."""
    k, c = divmod(f.x, width)
    centre = width // 2
    cut = abs(c - centre) + (k % stagger if stagger else 0)
    last = f.h - 1 - cut
    return f.y <= last, f.y == last, c


def m_feathers(f, w, width=3, stagger=2, glow_tips=False, base=2):
    if not f.side:
        return "f1", None
    if f.face in ("right", "left"):
        return r("f", gr.shade(f, base)), None
    opaque, tip, c = feather_mask(f, width, stagger)
    if not opaque:
        return None, None
    if tip and glow_tips:
        return "g0", "g0"
    t = base + (1 if f.y < f.h // 3 else 0) - (1 if f.y > 2 * f.h // 3 else 0)
    if f.face == "back":
        t -= 1
    if c == 0:
        t -= 1  # dark gap between feathers
    elif c == width // 2 and f.y < f.h - 2:
        t += 1  # bright rachis
    return r("f", t), None


def m_coverts(f, w):
    if not f.side:
        return "f2", None
    if f.face in ("right", "left"):
        return r("f", gr.shade(f, 2)), None
    band = f.y % 3
    shift = (f.y // 3) % 2
    c = (f.x + shift) % 2
    t = 3 - band - (1 if f.face == "back" else 0) + (0 if c else -0)
    if band == 2 and c == 0:
        t -= 1
    return r("f", t), None


def m_secondaries(f, w):
    return m_feathers(f, w, width=3, stagger=2)


def m_primaries(f, w):
    return m_feathers(f, w, width=3, stagger=3, glow_tips=True)


def m_lead(f, w):
    return m_feathers(f, w, width=3, stagger=0, glow_tips=True, base=2)


def m_trail(f, w):
    return ("g1", "g1") if f.y < f.h // 2 else ("g0", "g0")


def m_shard(f, w):
    if f.face == "top":
        return "g1", "g1"
    if f.side and (f.x + f.y) % 4 == 0:
        return "g1", "g1"
    return ("g0", "g0") if f.face in ("front", "right") else ("m3", None)


def m_pauldron(f, w):
    if f.face == "top":
        return ("m3" if f.y < f.h // 2 else "m2"), None
    if f.side and f.y == f.h - 1:
        return "m0", None
    if f.side and f.y == f.h - 2:
        return ("c3" if f.x % 2 else "c2"), None  # cloth trim under the plate
    return m_metal(f, w)


# attribute props (transparent on the other three Watchers)

def m_sun_ray(f, w):
    if f.y == 0 or f.face == "top":
        return "g1", "g1"
    return r("m", gr.shade(f, 2)), None


def m_sun_ring(f, w):
    if f.face in ("front", "back"):
        if f.y == f.h - 1:
            return "g0", "g0"  # inner edge glows
        return ("a2" if f.x % 3 else "m3"), None
    return r("a", gr.shade(f, 1, levels=3), 3), None


def m_sun_spoke(f, w):
    if f.side and f.y < 2:
        return ("g1", "g1") if f.y == 0 else ("g0", "g0")
    return r("a", 2 - (1 if f.face in ("back", "left") else 0) - (1 if f.y > f.h - 2 else 0), 3), None


def m_moon(f, w):
    if f.face in ("front", "back"):
        if f.y == f.h - 1 and f.h > 1:
            return "g0", "g0"
        return r("a", 2 if f.y == 0 else 1, 3), None
    return r("a", 1 if f.face != "bottom" else 0, 3), None


def m_star(f, w):
    return "g1", "g1"


def m_diadem(f, w):
    if f.y == 0 and f.side:
        return "g1", "g1"
    return r("m", gr.shade(f, 2)), None


def m_handle(f, w):
    return ("c0" if f.y % 2 else "c1"), None


def m_hammer(f, w):
    if f.face in ("right", "left") and 1 <= f.x <= f.w - 2 and f.y == f.h // 2:
        return "g0", "g0"  # star rune on both striking faces
    t = gr.shade(f, 1, levels=3)
    if f.edge and f.side:
        t = 2 if f.y == 0 else 0
    return r("a", t, 3), None


def m_anvil(f, w):
    if f.face in ("front", "back") and f.y == 0:
        return "g0", "g0"
    return r("a", gr.shade(f, 1, levels=3), 3), None


def m_gear(f, w):
    if f.face in ("front", "back") and f.y == 0 and f.x % 2 == 0:
        return "D", None  # teeth gaps
    return r("m", gr.shade(f, 2)), None


def m_tome(f, w):
    if f.face in ("right", "left"):  # covers: leather, gold corners, glowing sigil
        if (f.x in (0, f.w - 1)) and (f.y in (0, f.h - 1)):
            return "m2", None
        if f.x in (f.w // 2 - 1, f.w // 2) and f.y in (f.h // 2 - 1, f.h // 2):
            return "g0", "g0"
        return r("c", gr.shade(f, 1)), None
    if f.face == "front":  # page edges
        return ("a2" if f.y % 2 else "a1"), None
    return r("a", 0, 3) if f.face == "back" else "a2", None


def m_quill(f, w):
    if f.y == 0 and f.side:
        return "g1", "g1"
    return r("f", gr.shade(f, 2) + (1 if f.x == 0 else 0)), None


def m_nib(f, w):
    return r("m", gr.shade(f, 2)), None


def m_tablet(f, w):
    if f.face in ("front", "back"):
        if f.edge:
            return "a1", None
        if f.y % 2 == 1 and f.x == 1:
            return ("g0", "g0") if f.y == 1 else ("a0", None)  # glyph lines, the first one lit
        return "a2", None
    return "a1", None


MATERIALS = {
    "cloth": m_cloth, "cloth_dark": m_cloth_dark, "inner": m_inner, "panel": m_panel, "panel_hem": m_panel_hem,
    "gown": m_gown, "gown_hem": m_gown_hem, "tabard": m_tabard, "tabard_hem": m_tabard_hem, "vambrace": m_vambrace,
    "metal": m_metal, "belt": m_belt, "chest": m_chest, "abdomen": m_abdomen, "skin": m_skin, "fingers": m_fingers,
    "head": m_head, "circlet": m_circlet, "halo": m_halo, "core": m_core, "glow_ring": m_glow_ring,
    "sleeve": m_sleeve, "veil": m_veil, "ribbon": m_ribbon, "mantle": m_mantle, "wingbar": m_wingbar,
    "coverts": m_coverts, "secondaries": m_secondaries, "primaries": m_primaries, "lead": m_lead, "trail": m_trail,
    "shard": m_shard, "pauldron": m_pauldron,
    "sun_ray": m_sun_ray, "sun_ring": m_sun_ring, "sun_spoke": m_sun_spoke,
    "moon_arc": m_moon, "moon_star": m_star, "moon_diadem": m_diadem,
    "forge_handle": m_handle, "forge_head": m_hammer, "forge_pommel": m_nib, "forge_anvil": m_anvil,
    "forge_gear": m_gear,
    "tome_book": m_tome, "tome_quill": m_quill, "tome_nib": m_nib, "tome_tablet": m_tablet,
}


def material(name):
    painter = MATERIALS[name]
    prefix = name.split("_")[0]
    owner = PROP_OWNER.get(prefix)
    if owner is None:
        return painter
    return lambda f, w: painter(f, w) if w == owner else (None, None)


# ------------------------------------------------------------------ model

SIDES = (("right", -1), ("left", 1))
# pair: (shoulder height, segment lengths root/mid/tip, rest yaw (swept back), rest roll (raised), depth)
WING_PAIRS = (("upper", 45, (8, 9, 10), 14, 36, 5.0), ("middle", 42, (7, 8, 9), 20, 8, 6.0),
              ("lower", 39, (6, 7, 7), 26, -22, 7.0))


def mx(s, x0, w):
    """x origin of a box spanning [x0, x0 + w] on the figure's left (+X), mirrored onto the right."""
    return x0 if s > 0 else -x0 - w


def build():
    m = gr.Rig(TW, TH)

    def B(name, parent, pivot, rot=None):
        return m.bone(name, parent, (pivot[0], pivot[1] + Y, pivot[2]), rot)

    def C(bone, origin, size, mat, pivot=None, rot=None):
        p = (pivot[0], pivot[1] + Y, pivot[2]) if pivot else None
        return m.cube(bone, (origin[0], origin[1] + Y, origin[2]), size, mat, p, rot)

    B("root", None, (0, -Y, 0))
    body = B("body", "root", (0, 30, 0))
    C(body, (-5.5, 26, -3.5), (11, 6, 7), "cloth")                 # hips / gown top
    C(body, (-5, 31, -3.5), (10, 2, 7), "belt")                     # belt with buckle gem
    # gown: a trumpet-shaped skirt in two swaying segments, with a front tabard and a back train over it
    sk = B("skirt", "body", (0, 26, 0))
    C(sk, (-6, 14, -4), (12, 12, 8), "gown")
    sk2 = B("skirt_2", "skirt", (0, 14, 0))
    C(sk2, (-7.5, 2, -5), (15, 12, 10), "gown_hem")
    sf = B("skirt_front", "body", (0, 26, -4), (-2, 0, 0))
    C(sf, (-3, 14, -5), (6, 12, 1), "tabard")
    sf2 = B("skirt_front_2", "skirt_front", (0, 14, -5), (-3, 0, 0))
    C(sf2, (-3.5, 1, -6), (7, 13, 1), "tabard_hem")
    sb = B("skirt_back", "body", (0, 26, 4), (3, 0, 0))
    C(sb, (-4, 14, 4), (8, 12, 1), "panel")
    sb2 = B("skirt_back_2", "skirt_back", (0, 14, 5), (4, 0, 0))
    C(sb2, (-4.5, 0, 5), (9, 14, 1), "panel_hem")
    trail = B("trail", "body", (0, 4, 0))                            # drips of light under the gown
    for x, z, h in ((-3, -2, 5), (2, 1, 7), (-0.5, 2.5, 4), (3.5, -2.5, 3)):
        C(trail, (x, 4 - h, z), (1, h, 1), "trail")

    torso = B("torso", "body", (0, 33, 0))
    C(torso, (-4, 33, -2.5), (8, 6, 5), "abdomen")
    C(torso, (-5, 39, -3), (10, 9, 6), "chest")
    C(torso, (-1, 42, -4), (2, 3, 1), "core")
    C(torso, (-3, 47, -2.5), (6, 2, 5), "metal")                    # gorget
    mantle = B("mantle", "torso", (0, 47, 3))
    C(mantle, (-4, 35, 3), (8, 12, 1), "mantle")
    mantle2 = B("mantle_2", "mantle", (0, 35, 4), (6, 0, 0))
    C(mantle2, (-4.5, 22, 4), (9, 13, 1), "mantle")

    head = B("head", "torso", (0, 48, 0))
    C(head, (-1.5, 48, -1.5), (3, 2, 3), "skin")                    # neck
    C(head, (-4, 50, -4), (8, 8, 8), "head")
    C(head, (-4.5, 55, -4.5), (9, 1, 9), "circlet")
    veil = B("veil", "head", (0, 56, 4))
    C(veil, (-3.5, 50, 4), (7, 6, 1), "veil")
    veil2 = B("veil_2", "veil", (0, 50, 4.5), (8, 0, 0))
    C(veil2, (-3, 44, 4.5), (6, 6, 1), "veil")
    for side, s in SIDES:
        rb = B(f"ribbon_{side}", "head", (s * 4.5, 55.5, 2.5), (6, 0, -s * 8))
        C(rb, (mx(s, 4.5, 1), 49.5, 2), (1, 6, 1), "ribbon")
        rb2 = B(f"ribbon_{side}_2", f"ribbon_{side}", (s * 5, 49.5, 2.5), (6, 0, -s * 4))
        C(rb2, (mx(s, 4.5, 1), 43.5, 2), (1, 6, 1), "ribbon")
    halo = B("halo", "head", (0, 65, 1), (12, 0, 0))
    for i in range(12):
        C(halo, (-2, 64.5, -5.5), (4, 1, 1), "halo", pivot=(0, 65, 1), rot=(0, i * 30, 0))

    # --- Solenne: rayed crown and sun disc
    crown = B("crown_sun", "head", (0, 56, -4))
    for ang, hgt in ((0, 7), (-18, 5), (18, 5), (-36, 4), (36, 4)):
        C(crown, (-0.5, 56, -4.5), (1, hgt, 1), "sun_ray", pivot=(0, 56, -4), rot=(0, 0, ang))
    emblem = B("emblem", "torso", (0, 56, 4.5))
    sun = B("emblem_sun", "emblem", (0, 56, 5))
    for i in range(10):
        C(sun, (-3.5, 56 + 9, 4.5), (7, 2, 1), "sun_ring", pivot=(0, 56, 5), rot=(0, 0, i * 36))
    for i in range(8):
        ln = 6 if i % 2 == 0 else 4
        C(sun, (-0.5, 56 + 11, 5), (1, ln, 1), "sun_spoke", pivot=(0, 56, 5), rot=(0, 0, i * 45 + 22.5))
    # --- Vael: a thick crescent wrapped around the head's right side, horns reaching over the shoulder and above the halo
    moon = B("emblem_moon", "emblem", (0, 58, 5))
    for theta, th in ((62, 1), (92, 2), (122, 3), (152, 4), (182, 4), (212, 3), (242, 2), (272, 1)):
        a = math.radians(theta)
        cx, cy = math.cos(a) * 11.5 + 1.5, math.sin(a) * 11.5
        C(moon, (cx - 2.5, 58 + cy - th / 2, 4.5), (5, th, 1), "moon_arc", pivot=(cx, 58 + cy, 5),
          rot=(0, 0, -(theta - 90)))
    for cx, cy in ((9, 9), (11, 2), (8.5, -4)):
        C(moon, (cx - 0.5, 58 + cy, 4.5), (1, 1, 1), "moon_star")
    diadem = B("crown_moon", "head", (0, 56, -4.5))
    C(diadem, (-1.5, 56, -5), (3, 1, 1), "moon_diadem")
    for s in (-1, 1):
        C(diadem, (s * 2 - 0.5, 56, -5), (1, 3, 1), "moon_diadem", pivot=(s * 1.5, 56, -4.5), rot=(0, 0, -s * 25))
    # --- Orrin: a turning gear ring and an anvil sigil above the halo
    forge = B("emblem_forge", "emblem", (0, 57, 5))
    C(forge, (-4.5, 74, 1), (9, 2, 1), "forge_anvil")                 # anvil sigil floating above the halo: face
    C(forge, (4.5, 74.5, 1), (3, 1, 1), "forge_anvil")                # horn
    C(forge, (-1.5, 72, 1), (3, 2, 1), "forge_anvil")                 # waist
    C(forge, (-3.5, 70, 1), (7, 2, 1), "forge_anvil")                 # foot
    gear = B("emblem_forge_gear", "emblem_forge", (0, 57, 5))
    for i in range(8):
        C(gear, (-3, 57 + 9, 5), (6, 2, 1), "forge_gear", pivot=(0, 57, 5), rot=(0, 0, i * 45))
    # --- Ilyra: an arc of rune tablets above and behind the head
    tome = B("emblem_tome", "emblem", (0, 57, 5))
    for ang in (-64, -32, 0, 32, 64):
        C(tome, (-1.5, 57 + 9, 5), (3, 4, 1), "tome_tablet", pivot=(0, 57, 5), rot=(0, 0, ang))

    for side, s in SIDES:
        arm = B(f"arm_{side}", "torso", (s * 7, 46, 0), (0, 0, -s * 7))
        C(arm, (mx(s, 4, 6), 44, -3.5), (6, 4, 7), "pauldron")
        C(arm, (mx(s, 7.5, 2), 48, -1), (2, 3, 2), "pauldron")       # upswept point
        C(arm, (mx(s, 5, 4), 37, -2), (4, 9, 4), "cloth")
        slv = B(f"sleeve_{side}", f"arm_{side}", (s * 7, 39, 2), (10, 0, -s * 4))
        C(slv, (mx(s, 4.5, 5), 26, 2), (5, 13, 1), "sleeve")           # long angel sleeve hanging from the elbow
        fore = B(f"forearm_{side}", f"arm_{side}", (s * 7, 37, 0), (-18, 0, 0))
        C(fore, (mx(s, 5.5, 3), 28, -1.5), (3, 9, 3), "vambrace")
        C(fore, (mx(s, 5, 4), 29, -2), (4, 1, 4), "glow_ring")        # bracer ring at the wrist
        hand = B(f"hand_{side}", f"forearm_{side}", (s * 7, 28, 0), (-4, 0, 0))
        C(hand, (mx(s, 6, 2), 25, -2), (2, 3, 4), "skin")             # palm
        C(hand, (mx(s, 5.5, 1), 24, -3), (1, 2, 1), "skin")           # thumb
        fing = B(f"fingers_{side}", f"hand_{side}", (s * 7, 25, 0), (0, 0, s * 12))
        C(fing, (mx(s, 6.5, 1), 23, -2), (1, 2, 4), "fingers")
        tips = B(f"fingertips_{side}", f"fingers_{side}", (s * 7, 23, 0), (0, 0, s * 16))
        C(tips, (mx(s, 6.5, 1), 22, -2), (1, 1, 4), "fingers")

    # props in the hands: Orrin's hammer (right), Ilyra's tome (left) and quill (right)
    ham = B("prop_hammer", "hand_right", (-7, 25, 0))
    C(ham, (-7.5, 14, -0.5), (1, 13, 1), "forge_handle")
    C(ham, (-9, 10, -3.5), (3, 4, 7), "forge_head")
    C(ham, (-8, 27, -1), (2, 1, 2), "forge_pommel")
    book = B("prop_tome", "hand_left", (7, 25, 0))
    C(book, (7, 18, -3), (2, 7, 6), "tome_book")
    quill = B("prop_quill", "hand_right", (-7, 26, -1), (62, 0, 0))   # held pointing forward and up
    C(quill, (-7.5, 23, -1.5), (1, 6, 1), "tome_nib")
    C(quill, (-7.5, 29, -2), (1, 7, 2), "tome_quill")

    # three pairs of layered wings: root > mid > tip > primary fan
    for pair, py, (l1, l2, l3), yaw, roll, z in WING_PAIRS:
        for side, s in SIDES:
            n = f"wing_{pair}_{side}"
            x0 = s * 2.5

            def span(start, ln):
                return x0 + (start if s > 0 else -start - ln)

            root = B(n, "torso", (x0, py, z), (0, -s * yaw, -s * roll))
            C(root, (span(0, l1), py - 1.5, z), (l1, 3, 1), "wingbar")
            C(root, (span(0, l1), py - 7, z + 0.5), (l1, 6, 1), "coverts")
            mid = B(n + "_mid", n, (x0 + s * l1, py, z), (0, 0, 0))
            C(mid, (span(l1, l2), py - 1, z), (l2, 2, 1), "wingbar")
            C(mid, (span(l1 - 1, l2 + 1), py - 10, z + 0.5), (l2 + 1, 9, 1), "secondaries")
            tip = B(n + "_tip", n + "_mid", (x0 + s * (l1 + l2), py, z), (0, 0, 0))
            C(tip, (span(l1 + l2, l3), py - 1, z), (l3, 2, 1), "wingbar")
            fan = B(n + "_fan", n + "_tip", (x0 + s * (l1 + l2), py, z), (0, 0, -s * 24))
            C(fan, (span(l1 + l2 - 1, l3 + 2), py - 12, z + 0.5), (l3 + 2, 11, 1), "primaries")
            C(fan, (span(l1 + l2 + l3, 3), py - 15, z + 1), (3, 15, 1), "lead")

    orbit = B("orbit", "root", (0, 16, 0))                           # crystal shards circling the gown
    for i in range(6):
        a = math.radians(i * 60 + 30)
        cx, cz = math.sin(a) * 15, math.cos(a) * 15
        C(orbit, (cx - 1, 13.5, cz - 1), (2, 5, 2), "shard", pivot=(cx, 16, cz), rot=(0, 45, 0))
    return m


# ------------------------------------------------------------------ animations

def side_pose(**bones):
    """Expands {name_X: f(s)} entries for both sides: keys ending in '_' get 'right'/'left' appended
    (wing names expand to all pairs if they start with 'wing_*')."""
    out = {}
    for name, chans in bones.items():
        if name.endswith("_"):
            for side, s in SIDES:
                out.update(_expand(name, side, s, chans))
        else:
            out[name] = chans
    return out


def _expand(name, side, s, chans):
    res = {}
    vals = {c: (v(s) if callable(v) else v) for c, v in chans.items()}
    if name.startswith("wing*"):
        seg = name[len("wing*"):-1]  # "", "_mid", "_tip", "_fan"
        for pair, *_ in WING_PAIRS:
            k = {"upper": 1.0, "middle": 0.8, "lower": 0.65}[pair]
            res[f"wing_{pair}_{side}{seg}"] = {c: [x * k for x in v] if c != "scale" else v for c, v in vals.items()}
        return res
    res[name + side] = vals
    return res


P = side_pose  # shorthand


def idle():
    a = gr.Anim(24, loop=True)
    a.osc("root", "position", 6, (0, 1.2, 0))
    a.osc("root", "rotation", 12, (0, 0, 0.8), phase=2)
    a.osc("torso", "position", 4, (0, 0.25, 0))                      # breathing
    a.osc("torso", "rotation", 4, (-1.0, 0, 0), phase=0.3)
    a.osc("head", "rotation", 8, (2.0, 4.0, 1.0), phase=1.0)
    a.osc("halo", "position", 4.8, (0, 0.6, 0), phase=0.6)
    a.keys("halo", "rotation", [(0, (0, 0, 0), "linear"), (24, (0, 360, 0), "linear")])
    a.osc("emblem", "position", 6, (0, 0.5, 0), phase=1.6)
    a.keys("emblem_sun", "rotation", [(0, (0, 0, 0), "linear"), (24, (0, 0, -180), "linear")])
    a.osc("emblem_moon", "rotation", 12, (0, 0, 4), phase=3)
    a.keys("emblem_forge_gear", "rotation", [(0, (0, 0, 0), "linear"), (24, (0, 0, 360), "linear")])
    a.osc("emblem_tome", "rotation", 8, (0, 8, 2), phase=2)
    a.keys("orbit", "rotation", [(0, (0, 0, 0), "linear"), (24, (0, -360, 0), "linear")])
    a.osc("orbit", "position", 6, (0, 0.9, 0), phase=1.4)
    a.osc("trail", "rotation", 24 / 7, (6, 0, 4), phase=0.5)
    for side, s in SIDES:
        d = 0.15 if s < 0 else 0  # slight asymmetry
        a.osc(f"arm_{side}", "rotation", 4, (1.5, 0, -s * 1.2), phase=0.4 + d)
        a.osc(f"forearm_{side}", "rotation", 4, (-2.5, 0, 0), phase=0.8 + d)
        a.osc(f"hand_{side}", "rotation", 6, (3, 0, s * 2), phase=1.2 + d)
        a.osc(f"fingers_{side}", "rotation", 6, (0, 0, s * 4), phase=1.5 + d)
        a.osc(f"fingertips_{side}", "rotation", 6, (0, 0, s * 6), phase=1.8 + d)
        a.osc(f"sleeve_{side}", "rotation", 4.8, (2.5, 0, -s * 1.5), phase=1.0 + d)
        a.osc(f"ribbon_{side}", "rotation", 24 / 9, (4, 0, -s * 3), phase=0.3 + d)
        a.osc(f"ribbon_{side}_2", "rotation", 24 / 9, (6, 0, -s * 5), phase=0.7 + d)
        for pair, period, amp in (("upper", 24 / 7, 4.0), ("middle", 4.0, 3.2), ("lower", 4.8, 2.6)):
            n, ph = f"wing_{pair}_{side}", {"upper": 0, "middle": 1.1, "lower": 2.3}[pair] + 2 * d
            a.osc(n, "rotation", period, (0, -s * amp * 0.4, -s * amp), phase=ph)
            a.osc(n + "_mid", "rotation", period, (0, -s * amp * 0.5, -s * amp * 1.2), phase=ph + 0.25)
            a.osc(n + "_tip", "rotation", period, (0, -s * amp * 0.6, -s * amp * 1.5), phase=ph + 0.5)
            a.osc(n + "_fan", "rotation", period, (0, 0, s * amp * 0.8), phase=ph + 0.75)
    a.osc("skirt", "rotation", 8, (0.8, 0, 0.6), phase=1.2)
    a.osc("skirt_2", "rotation", 8, (1.2, 0, 1.0), phase=1.8)
    a.osc("skirt_front", "rotation", 4.8, (-1.4, 0, 0), phase=0.6)
    a.osc("skirt_front_2", "rotation", 4.8, (-2.2, 0, 0), phase=1.1)
    a.osc("skirt_back", "rotation", 4.8, (1.4, 0, 0), phase=1.3)
    a.osc("skirt_back_2", "rotation", 4.8, (2.4, 0, 0), phase=1.8)
    a.osc("mantle", "rotation", 4.8, (1.5, 0, 0), phase=1.0)
    a.osc("mantle_2", "rotation", 4.8, (2.5, 0, 0), phase=1.5)
    a.osc("veil", "rotation", 6, (2, 0, 1), phase=1.8)
    a.osc("veil_2", "rotation", 6, (3, 0, 1.5), phase=2.3)
    return a


# lag (s) per bone family for overlapping action, and amplitude factors for the trailing segments
def lags():
    lag, scale = {}, {}
    for side, s in SIDES:
        for pair, *_ in WING_PAIRS:
            n = f"wing_{pair}_{side}"
            lag[n + "_mid"], lag[n + "_tip"], lag[n + "_fan"] = 0.04, 0.08, 0.12
        lag[f"fingers_{side}"], lag[f"fingertips_{side}"] = 0.04, 0.07
        lag[f"sleeve_{side}"], lag[f"ribbon_{side}"], lag[f"ribbon_{side}_2"] = 0.08, 0.06, 0.12
    for n in ("skirt_front_2", "skirt_back_2", "mantle_2", "veil_2", "skirt_2"):
        lag[n] = 0.1
    for n in ("skirt_front", "skirt_back", "mantle", "veil", "halo", "emblem", "orbit", "skirt"):
        lag[n] = 0.05
    return lag, scale


def cloth(k):
    """Gown/mantle/veil response to vertical motion: k > 0 = body rises (cloth trails down and flares)."""
    return P(skirt_front={"rotation": (-3 * k, 0, 0)}, skirt_front_2={"rotation": (-5 * k, 0, 0)},
             skirt_back={"rotation": (3 * k, 0, 0)}, skirt_back_2={"rotation": (6 * k, 0, 0)},
             skirt={"rotation": (-1 * k, 0, 0)}, skirt_2={"rotation": (-1.5 * k, 0, 0)},
             sleeve_={"rotation": lambda s: (6 * k, 0, -s * 5 * k)},
             mantle={"rotation": (4 * k, 0, 0)}, mantle_2={"rotation": (6 * k, 0, 0)},
             veil={"rotation": (3 * k, 0, 0)}, veil_2={"rotation": (5 * k, 0, 0)},
             ribbon_={"rotation": lambda s: (5 * k, 0, -s * 4 * k)})


def merge(*poses):
    out = {}
    for p in poses:
        for b, chans in p.items():
            if not chans:
                continue
            out.setdefault(b, {}).update(chans)
    return out


def skirt2(k):
    return {f"sleeve_{side}": {"rotation": (12 * k, 0, -s * 8 * k)} for side, s in SIDES} | \
        {f"ribbon_{side}_2": {"rotation": (8 * k, 0, -s * 6 * k)} for side, s in SIDES}


def wings(yaw, roll, fan=0.0, mid=0.7, tip=0.6):
    """Wing pose relative to rest: yaw > 0 swings forward, roll > 0 raises; mid/tip follow with their own share."""
    return P(**{"wing*_": {"rotation": lambda s: (0, s * yaw, -s * roll)},
                "wing*_mid_": {"rotation": lambda s: (0, s * yaw * mid, -s * roll * mid)},
                "wing*_tip_": {"rotation": lambda s: (0, s * yaw * tip, -s * roll * tip * 1.2)},
                "wing*_fan_": {"rotation": lambda s: (0, 0, -s * fan)}})


def one_shot(length, timeline):
    a = gr.Anim(length)
    lag, scale = lags()
    a.pose_track(timeline, lag, scale)
    return a


REST = {}


def cast():
    """Trial start: sink and gather (anticipation 0.55 s), fling both arms up as the wings flare (3 ticks to the
    release), hold, then settle with the wing tips and cloth trailing."""
    gather = merge(
        {"root": {"position": (0, -2.2, 0)}, "torso": {"rotation": (10, 0, 0)}, "head": {"rotation": (14, 0, 0)},
         "halo": {"scale": (0.9, 0.9, 0.9)}, "emblem": {"scale": (0.92, 0.92, 0.92)},
         "orbit": {"scale": (0.7, 1, 0.7), "rotation": (0, 30, 0)}},
        P(arm_={"rotation": lambda s: (30, 0, s * 6)}, forearm_={"rotation": (-38, 0, 0)},
          hand_={"rotation": (22, 0, 0)}, fingers_={"rotation": lambda s: (0, 0, s * 28)},
          fingertips_={"rotation": lambda s: (0, 0, s * 32)}),
        wings(-16, -12, fan=-8), cloth(-0.5))
    release = merge(
        {"root": {"position": (0, 3.2, 0)}, "torso": {"rotation": (-14, 0, 0)}, "head": {"rotation": (-24, 0, 0)},
         "halo": {"scale": (1.35, 1.35, 1.35), "rotation": (0, -60, 0)},
         "emblem": {"scale": (1.22, 1.22, 1.22)}, "orbit": {"scale": (1.5, 1, 1.5), "rotation": (0, -90, 0)}},
        P(arm_={"rotation": lambda s: (-155, 0, -s * 24)}, forearm_={"rotation": (8, 0, 0)},
          hand_={"rotation": (-14, 0, 0)}, fingers_={"rotation": lambda s: (0, 0, -s * 14)},
          fingertips_={"rotation": lambda s: (0, 0, -s * 10)}),
        wings(18, 18, fan=14), cloth(1.0), skirt2(1.0))
    hold = merge(release, {"root": {"position": (0, 3.6, 0)}, "head": {"rotation": (-22, 0, 0)}})
    settle = merge(
        {"root": {"position": (0, 0.6, 0)}, "torso": {"rotation": (3, 0, 0)}, "head": {"rotation": (5, 0, 0)},
         "halo": {"scale": (1.05, 1.05, 1.05), "rotation": (0, -20, 0)}, "emblem": {"scale": (1.03, 1.03, 1.03)},
         "orbit": {"scale": (1.1, 1, 1.1), "rotation": (0, -20, 0)}},
        P(arm_={"rotation": lambda s: (-30, 0, -s * 6)}, forearm_={"rotation": (-10, 0, 0)},
          hand_={"rotation": (6, 0, 0)}, fingers_={"rotation": lambda s: (0, 0, s * 6)},
          fingertips_={"rotation": lambda s: (0, 0, s * 6)}),
        wings(-4, -4, fan=-3), cloth(-0.3))
    return one_shot(2.0, [(0, REST, "linear"), (0.55, gather, "easeInOutSine"), (0.7, release, "easeOutCubic"),
                          (0.95, hold, "linear"), (1.4, settle, "easeInOutSine"), (2.0, REST, "easeInOutSine")])


def talk():
    """Readable conversational gesture: lean in, the right hand opens palm-up toward the listener, the left hand
    rests on the heart, two nods."""
    lean = merge({"torso": {"rotation": (3, 0, 0)}, "head": {"rotation": (5, 0, 0)}},
                 {"arm_right": {"rotation": (12, 0, 0)}, "forearm_right": {"rotation": (-6, 0, 0)}})
    open_ = merge(
        {"torso": {"rotation": (-2, -7, 0)}, "head": {"rotation": (-4, -8, 0)},
         "arm_right": {"rotation": (-52, 12, 14)}, "forearm_right": {"rotation": (-34, 0, 0)},
         "hand_right": {"rotation": (-8, 62, 0)}, "fingers_right": {"rotation": (0, 0, 10)},
         "fingertips_right": {"rotation": (0, 0, 6)},
         "arm_left": {"rotation": (-26, 0, 4)}, "forearm_left": {"rotation": (-78, 0, 0)},
         "hand_left": {"rotation": (0, 30, 0)}},
        wings(4, 5, fan=4))
    nod1 = merge(open_, {"head": {"rotation": (8, -7, 0)}})
    up = merge(open_, {"head": {"rotation": (-3, -6, 0)}, "arm_right": {"rotation": (-44, 12, 12)}})
    nod2 = merge(open_, {"head": {"rotation": (5, -5, 0)}, "arm_right": {"rotation": (-48, 10, 14)},
                         "forearm_right": {"rotation": (-28, 0, 0)}})
    return one_shot(2.4, [(0, REST, "linear"), (0.35, lean, "easeInOutSine"), (0.75, open_, "easeOutBack"),
                          (1.05, nod1, "easeInOutSine"), (1.35, up, "easeInOutSine"), (1.65, nod2, "easeInOutSine"),
                          (2.4, REST, "easeInOutSine")])


def greet():
    """A player arrives: a turn and an inhale, then arms and all six wings open wide, chin lifted, held a moment."""
    inhale = merge(
        {"body": {"rotation": (0, 10, 0)}, "torso": {"rotation": (5, 0, 0)}, "head": {"rotation": (9, 6, 0)},
         "root": {"position": (0, -0.8, 0)}},
        P(arm_={"rotation": lambda s: (8, 0, s * 6)}, forearm_={"rotation": (-12, 0, 0)}),
        wings(-6, -5, fan=-4))
    open_ = merge(
        {"body": {"rotation": (0, 0, 0)}, "torso": {"rotation": (-8, 0, 0)}, "head": {"rotation": (-10, 0, 0)},
         "root": {"position": (0, 1.6, 0)}, "halo": {"scale": (1.12, 1.12, 1.12)},
         "orbit": {"scale": (1.25, 1, 1.25)}},
        P(arm_={"rotation": lambda s: (-28, 0, -s * 62)}, forearm_={"rotation": (-24, 0, 0)},
          hand_={"rotation": lambda s: (0, -s * 60, -s * 8)}, fingers_={"rotation": lambda s: (0, 0, -s * 12)},
          fingertips_={"rotation": lambda s: (0, 0, -s * 8)}),
        wings(16, 14, fan=10), cloth(0.6))
    hold = merge(open_, {"torso": {"rotation": (-6, 0, 0)}, "root": {"position": (0, 1.9, 0)}})
    return one_shot(2.6, [(0, REST, "linear"), (0.4, inhale, "easeInOutSine"), (1.0, open_, "easeOutBack"),
                          (1.7, hold, "linear"), (2.6, REST, "easeInOutSine")])


def bless():
    """Trial complete: hands gather the light to the chest (shards contract onto them), then present it forward as
    all wings flare high and the halo blazes; a long hold, a weighty settle."""
    gather = merge(
        {"root": {"position": (0, -1.5, 0)}, "torso": {"rotation": (6, 0, 0)}, "head": {"rotation": (16, 0, 0)},
         "halo": {"scale": (0.9, 0.9, 0.9)}, "emblem": {"scale": (0.95, 0.95, 0.95)},
         "orbit": {"scale": (0.35, 1, 0.35), "position": (0, 12, 0), "rotation": (0, -40, 0)}},
        P(arm_={"rotation": lambda s: (-34, 0, s * 16)}, forearm_={"rotation": (-76, 0, 0)},
          hand_={"rotation": lambda s: (0, -s * 45, 0)}, fingers_={"rotation": lambda s: (0, 0, s * 10)}),
        wings(-14, -14, fan=-8), cloth(-0.4))
    build_ = merge(gather, {"root": {"position": (0, -0.6, 0)}, "halo": {"scale": (1.0, 1.0, 1.0)},
                            "orbit": {"scale": (0.3, 1, 0.3), "position": (0, 12.5, 0), "rotation": (0, -80, 0)}})
    present = merge(
        {"root": {"position": (0, 3.2, 0)}, "torso": {"rotation": (-7, 0, 0)}, "head": {"rotation": (-5, 0, 0)},
         "halo": {"scale": (1.4, 1.4, 1.4)}, "emblem": {"scale": (1.25, 1.25, 1.25)},
         "orbit": {"scale": (1.7, 1.3, 1.7), "position": (0, 5, 0), "rotation": (0, -30, 0)}},
        P(arm_={"rotation": lambda s: (-82, 0, -s * 12)}, forearm_={"rotation": (-18, 0, 0)},
          hand_={"rotation": lambda s: (-10, -s * 70, 0)}, fingers_={"rotation": lambda s: (0, 0, -s * 10)},
          fingertips_={"rotation": lambda s: (0, 0, -s * 6)}),
        wings(22, 24, fan=16), cloth(1.0), skirt2(1.0))
    hold = merge(present, {"root": {"position": (0, 3.6, 0)}, "orbit": {"scale": (1.6, 1.2, 1.6), "position": (0, 5, 0),
                                                                         "rotation": (0, -10, 0)}})
    settle = merge(
        {"root": {"position": (0, -0.6, 0)}, "torso": {"rotation": (3, 0, 0)}, "head": {"rotation": (4, 0, 0)},
         "halo": {"scale": (1.05, 1.05, 1.05)}, "orbit": {"scale": (1.1, 1, 1.1), "position": (0, 1, 0)}},
        P(arm_={"rotation": lambda s: (-20, 0, -s * 4)}, forearm_={"rotation": (-8, 0, 0)}),
        wings(-4, -5, fan=-3), cloth(-0.4))
    return one_shot(3.6, [(0, REST, "linear"), (0.7, gather, "easeInOutSine"), (1.5, build_, "easeInOutSine"),
                          (1.8, present, "easeOutBack"), (2.5, hold, "linear"), (3.0, settle, "easeInOutSine"),
                          (3.6, REST, "easeInOutSine")])


def animations():
    return {"idle": idle(), "cast": cast(), "talk": talk(), "greet": greet(), "bless": bless()}


# ------------------------------------------------------------------ preview

def preview(out_dir):
    import json
    import geo_preview as gp
    import png
    os.makedirs(out_dir, exist_ok=True)
    geo = json.load(open(os.path.join(ASSETS, "geo", "entity", "watcher.geo.json")))["minecraft:geometry"][0]
    anims = {k: gp.parse_anim(v) for k, v in
             json.load(open(os.path.join(ASSETS, "animations", "entity", "watcher.animation.json")))["animations"].items()}
    poses = [("rest", None, 0), ("idle", "idle", 5.3), ("cast-wind", "cast", 0.55), ("cast-release", "cast", 0.8),
             ("talk", "talk", 0.9), ("greet", "greet", 1.2), ("bless-gather", "bless", 1.2),
             ("bless-present", "bless", 2.0)]
    for wid in PALETTES:
        tex = png.read(os.path.join(ASSETS, "textures", "entity", "watcher", wid + ".png"))[2]
        glow = png.read(os.path.join(ASSETS, "textures", "entity", "watcher", wid + "_glowmask.png"))[2]
        sel = poses[:1] if wid != "solenne" else poses
        for sil in (False, True):
            img = gp.sheet(geo, tex, glow, anims, sel, [0, 90, 180, 35], 3 if wid == "solenne" else 4, sil)
            png.write(os.path.join(out_dir, f"watcher_{wid}{'_sil' if sil else ''}.png"), img)
    print("previews in", out_dir)


def main(argv):
    m = build()
    gr.write_json(ASSETS, ("geo/entity", "geckolib/models/entity"), "watcher.geo.json",
                  m.geo("geometry.watcher", 6, 6, 2.5))
    anims = animations()
    gr.write_json(ASSETS, ("animations/entity", "geckolib/animations/entity"), "watcher.animation.json",
                  gr.animation_file(anims))
    mats = {k: material(k) for k in MATERIALS}
    stats = gr.paint(m, PALETTES, mats, os.path.join(ASSETS, "textures", "entity", "watcher"), "{}")
    for wid, e in stats.items():
        assert e <= MAX_EMISSIVE, f"{wid}: {e:.1%} emissive (> {MAX_EMISSIVE:.0%})"
    print("watcher:", m.count(), "cubes,", len(m.bones), "bones,", len(m.layout), "uv boxes, cursor", m.cursor)
    print("  emissive:", ", ".join(f"{k} {v:.1%}" for k, v in stats.items()))
    print("  clips:", ", ".join(f"{k} {a.length}s" for k, a in anims.items()))
    if "--preview" in argv:
        preview(argv[argv.index("--preview") + 1])


if __name__ == "__main__":
    main(sys.argv[1:])
