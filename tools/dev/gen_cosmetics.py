"""Generates the developer-cosmetic textures (base + emissive glow) and the matching Java mesh class.

Run: python -I tools/dev/gen_cosmetics.py
Outputs: src/main/resources/assets/astralvigil/textures/entity/cosmetics/*.png
         src/main/java/media/hofferbert/astralvigil/client/cosmetics/CosmeticMeshes.java (do not edit by hand)

Art rules: vanilla style, <= 8 colours per texture, Void Navy outline, light from the top-left.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil", "textures", "entity", "cosmetics")
JAVA = os.path.join(ROOT, "src", "main", "java", "media", "hofferbert", "astralvigil", "client", "cosmetics", "CosmeticMeshes.java")
C = png.hexc
CLEAR = (0, 0, 0, 0)

# ------------------------------------------------------------------ box-UV painting


def faces(u, v, w, h, d):
    """Vanilla box UV: name -> (x, y, width, height) on the texture."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            img[y0 + y][x0 + x] = fn(x, y, w, h)


class Mesh:
    """Collects boxes for one Java method and paints them into a texture as they are added."""

    def __init__(self, name, tex_w=64, tex_h=32):
        self.name, self.parts, self.cursor = name, [], [0, 0, 0]  # cursor: u, v, row height
        self.base, self.glow = png.blank(tex_w, tex_h), png.blank(tex_w, tex_h)
        self.tw = tex_w

    def alloc(self, w, h, d):
        uw, uh = 2 * d + 2 * w, d + h
        if self.cursor[0] + uw > self.tw:
            self.cursor = [0, self.cursor[1] + self.cursor[2], 0]
        u, v = self.cursor[0], self.cursor[1]
        self.cursor[0] += uw
        self.cursor[2] = max(self.cursor[2], uh)
        return u, v

    def box(self, part, x, y, z, w, h, d, side, top, glow_side=None, glow_top=None):
        u, v = self.alloc(w, h, d)
        self.parts.append((part, u, v, x, y, z, w, h, d))
        f = faces(u, v, w, h, d)
        for name in ("east", "front", "west", "back"):
            fill(self.base, f[name], side)
            if glow_side:
                fill(self.glow, f[name], glow_side)
        for name in ("top", "bottom"):
            fill(self.base, f[name], top)
            if glow_top:
                fill(self.glow, f[name], glow_top)


def vertical(colors):
    """Side painter: picks a colour by row (top row = colors[0], bottom row = colors[-1], middle = colors[1])."""
    def fn(x, y, w, h):
        if y == 0:
            return colors[0]
        if y == h - 1:
            return colors[-1]
        return colors[1] if (x + y) % 3 else colors[min(2, len(colors) - 1)]
    return fn


def solid(c):
    return lambda x, y, w, h: c


def tip(tip_c, body):
    """Spike: bright tip pixel row, body below."""
    return lambda x, y, w, h: tip_c if y == 0 else body(x, y, w, h)


def glow_row0(c):
    return lambda x, y, w, h: c if y == 0 else CLEAR


# ------------------------------------------------------------------ crowns

GOLD_L, GOLD, GOLD_D, GOLD_DD = C("F2C94C"), C("C9971E"), C("7A5A12"), C("4A360B")
WHITE, PALE = C("FFFFFF"), C("D8D8E6")
CYAN, CYAN_D = C("8FEFFF"), C("2FA8D0")
NAVY, VIOLET, VIOLET_L, VIOLET_G, VIOLET_W = C("1E1A2E"), C("5B2A86"), C("8C5AC8"), C("B57CFF"), C("E0C8FF")

BAND = [(-5, -10, -5, 10, 2, 1), (-5, -10, 4, 10, 2, 1), (4, -10, -4, 1, 2, 8), (-5, -10, -4, 1, 2, 8)]
GEMS = [(-1, -10, -5.5, 2, 2, 1), (-1, -10, 4.5, 2, 2, 1), (4.5, -10, -1, 1, 2, 2), (-5.5, -10, -1, 1, 2, 2)]


def gem_painter(core, edge):
    return lambda x, y, w, h: core if (x + y) % 2 == 0 else edge


def crown(name, band_cols, spike_tip, spike_body, spikes, gem_core, gem_edge, spike_glow):
    m = Mesh(name)
    for b in BAND:
        m.box("crown", *b, side=vertical(band_cols), top=solid(band_cols[0]))
    for s in spikes:
        m.box("crown", *s, side=tip(spike_tip, solid(spike_body)), top=solid(spike_tip),
              glow_side=glow_row0(spike_glow), glow_top=solid(spike_glow))
    for g in GEMS:
        m.box("crown", *g, side=gem_painter(gem_core, gem_edge), top=solid(gem_edge),
              glow_side=gem_painter(gem_core, gem_edge), glow_top=solid(gem_edge))
    return m


SOLAR_SPIKES = [(-1, -13, -5, 2, 3, 1), (-5, -12, -5, 1, 2, 1), (4, -12, -5, 1, 2, 1), (-3, -11, -5, 1, 1, 1),
                (2, -11, -5, 1, 1, 1), (-1, -12, 4, 2, 2, 1), (-5, -12, 4, 1, 2, 1), (4, -12, 4, 1, 2, 1),
                (4, -12, -1, 1, 2, 2), (-5, -12, -1, 1, 2, 2)]
ECLIPSE_SPIKES = [(-1, -15, -5, 2, 5, 1), (-4, -13, -5, 1, 3, 1), (3, -13, -5, 1, 3, 1), (-5, -14, -5, 1, 4, 1),
                  (4, -14, -5, 1, 4, 1), (-1, -14, 4, 2, 4, 1), (-5, -13, 4, 1, 3, 1), (4, -13, 4, 1, 3, 1),
                  (4, -14, -1, 1, 4, 2), (-5, -14, -1, 1, 4, 2), (4, -12, -3, 1, 2, 1), (4, -12, 2, 1, 2, 1),
                  (-5, -12, -3, 1, 2, 1), (-5, -12, 2, 1, 2, 1)]


def halo():
    m = Mesh("seraph_halo")
    seg = lambda x, y, w, h: WHITE if y == 0 and x % 4 == 1 else GOLD_L if y == 0 else GOLD
    for _ in range(1):
        m.box("ring", -1.5, -0.5, -5.5, 3, 1, 1, side=seg, top=solid(GOLD_L),
              glow_side=lambda x, y, w, h: GOLD_L if y == 0 else GOLD, glow_top=solid(GOLD_L))
        m.box("ring", -0.5, -0.5, -6.5, 1, 1, 1, side=solid(WHITE), top=solid(WHITE),
              glow_side=solid(CYAN), glow_top=solid(CYAN))
    return m


# ------------------------------------------------------------------ capes (vanilla 64x32 cape layout)


def cape(outer_fn, inner_fn, edge, glow_fn=None):
    """outer_fn/inner_fn(x, y) for the 10x16 faces; edge colour for the 1px sides/top/bottom."""
    img, glow = png.blank(64, 32), png.blank(64, 32)
    for y in range(16):
        for x in range(10):
            img[1 + y][1 + x] = outer_fn(x, y)
            img[1 + y][12 + x] = inner_fn(x, y)
            if glow_fn:
                glow[1 + y][1 + x] = glow_fn(x, y)
    for y in range(16):
        img[1 + y][0] = img[1 + y][11] = edge
    for x in range(10):
        img[0][1 + x] = img[0][11 + x] = edge
    return img, glow


def seraph_cape():
    sun = {(4, 5), (5, 5), (4, 6), (5, 6), (3, 5), (6, 5), (3, 6), (6, 6), (4, 4), (5, 4), (4, 7), (5, 7)}
    rays = {(4, 2), (5, 2), (1, 5), (8, 5), (4, 9), (5, 9), (2, 3), (7, 3), (2, 8), (7, 8)}

    def outer(x, y):
        if x in (0, 9) or y == 15:
            return GOLD if (x + y) % 2 else GOLD_L
        if y == 0:
            return GOLD_D
        if (x, y) in sun:
            return GOLD_L
        if (x, y) in rays:
            return GOLD
        if y in (12, 13) and x % 2 == y % 2:
            return GOLD_L
        return WHITE if y < 6 else PALE if y < 11 else WHITE

    def inner(x, y):
        return GOLD_D if x in (0, 9) else C("B9B3CC") if (x + y) % 5 == 0 else PALE

    def glow(x, y):
        if (x, y) in sun:
            return GOLD_L
        if (x, y) in rays or (x in (0, 9)):
            return GOLD_D
        return CLEAR
    return cape(outer, inner, GOLD_D, glow)


def starfield_cape():
    rng = random.Random(7)
    stars = {(rng.randrange(1, 9), rng.randrange(1, 15)) for _ in range(16)}
    big = {(2, 3), (7, 5), (4, 9), (6, 12), (3, 13)}
    line = set()
    pts = [(2, 3), (7, 5), (4, 9), (6, 12), (3, 13)]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        for t in range(1, 8):
            line.add((round(x0 + (x1 - x0) * t / 8), round(y0 + (y1 - y0) * t / 8)))
    line -= big

    def outer(x, y):
        if x in (0, 9) or y == 15:
            return GOLD
        if (x, y) in big:
            return WHITE
        if (x, y) in stars:
            return CYAN
        if (x, y) in line:
            return CYAN_D
        return NAVY if y < 8 or (x + y) % 4 else C("2C2942")

    def inner(x, y):
        return C("2C2942") if (x * 3 + y) % 7 else NAVY
    img, glow_a = cape(outer, inner, GOLD_D, lambda x, y: WHITE if (x, y) in big else CYAN if (x, y) in stars and (x + y) % 2 else CLEAR)
    _, glow_b = cape(outer, inner, GOLD_D, lambda x, y: CYAN if (x, y) in big else CYAN if (x, y) in stars and (x + y) % 2 == 0 else CYAN_D if (x, y) in line else CLEAR)
    return img, glow_a, glow_b


def eclipse_cape():
    cx, cy = 4.5, 5.5

    def ring(x, y):
        return 2.6 <= math.hypot(x - cx, y - cy) <= 3.6

    def core(x, y):
        return math.hypot(x - cx, y - cy) < 2.6

    def outer(x, y):
        if x in (0, 9) or y == 15:
            return VIOLET
        if ring(x, y):
            return VIOLET_G
        if core(x, y):
            return C("0B0912")
        if y > 10 and (x + y) % 3 == 0:
            return VIOLET
        return NAVY

    def inner(x, y):
        return VIOLET if x in (0, 9) else NAVY

    def glow(x, y):
        if ring(x, y):
            return VIOLET_W if (x + y) % 2 else VIOLET_G
        if (x in (0, 9) or y == 15) and (x + y) % 2:
            return VIOLET_G
        return CLEAR
    return cape(outer, inner, VIOLET, glow)


# ------------------------------------------------------------------ wing recolours (from seraph_wings.png)

WING_SRC = {"cyan": (143, 239, 255, 255), "shade": (185, 179, 204, 255), "light": (236, 232, 244, 255),
            "trim": (242, 201, 76, 255), "white": (255, 255, 255, 255)}


def wings(mapping, glow_keys):
    w, h, rows = png.read(os.path.join(os.path.dirname(TEX), "seraph_wings.png"))
    inv = {v: k for k, v in WING_SRC.items()}
    base, glow = png.blank(w, h), png.blank(w, h)
    for y in range(h):
        for x in range(w):
            px = rows[y][x]
            if px[3] == 0:
                continue
            key = inv[px]
            base[y][x] = mapping[key]
            if key in glow_keys:
                glow[y][x] = mapping[key]
    return base, glow


# ------------------------------------------------------------------ output


def save(name, img):
    n = len(png.colours(img))
    assert n <= 8, f"{name}: {n} colours"
    png.write(os.path.join(TEX, name + ".png"), img)


def java(meshes):
    out = ["package media.hofferbert.astralvigil.client.cosmetics;", "",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
           "/** GENERATED by tools/dev/gen_cosmetics.py - do not edit. Box UVs match the generated textures. */",
           "final class CosmeticMeshes {", "    private CosmeticMeshes() {}", ""]
    for m in meshes:
        out.append(f"    static CubeListBuilder {m.name.split('_')[0] + ''.join(p.title() for p in m.name.split('_')[1:])}() {{")
        out.append("        return CubeListBuilder.create()")
        for i, (_, u, v, x, y, z, w, h, d) in enumerate(m.parts):
            end = ";" if i == len(m.parts) - 1 else ""
            out.append(f"                .texOffs({u}, {v}).addBox({x}f, {y}f, {z}f, {w}f, {h}f, {d}f){end}")
        out.append("    }")
        out.append("")
    out.append("    /** Halo: 12 ring segments around the Y axis (each segment a child rotated by 30 degrees). */")
    out.append("    static void haloRing(PartDefinition spin) {")
    out.append("        for (int i = 0; i < 12; i++) {")
    out.append("            spin.addOrReplaceChild(\"segment_\" + i, seraphHalo(), PartPose.rotation(0f, (float) (i * Math.PI / 6), 0f));")
    out.append("        }")
    out.append("    }")
    out.append("}")
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    open(JAVA, "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")


def main():
    os.makedirs(TEX, exist_ok=True)
    solar = crown("solar_crown", [GOLD_L, GOLD, GOLD_D], WHITE, GOLD_L, SOLAR_SPIKES, CYAN, CYAN_D, CYAN)
    eclipse = crown("eclipse_crown", [VIOLET_L, VIOLET, NAVY], VIOLET_W, VIOLET, ECLIPSE_SPIKES, VIOLET_W, VIOLET_G, VIOLET_G)
    hal = halo()
    for m in (solar, eclipse, hal):
        save(m.name, m.base)
        save(m.name + "_glow", m.glow)
    img, glow = seraph_cape()
    save("seraph_cape", img)
    save("seraph_cape_glow", glow)
    img, ga, gb = starfield_cape()
    save("starfield_cape", img)
    save("starfield_cape_glow", ga)
    save("starfield_cape_glow_b", gb)
    img, glow = eclipse_cape()
    save("eclipse_cape", img)
    save("eclipse_cape_glow", glow)
    img, glow = wings({"white": WHITE, "light": GOLD_L, "shade": GOLD, "trim": CYAN, "cyan": WHITE}, {"trim", "cyan"})
    save("radiant_wings", img)
    save("radiant_wings_glow", glow)
    img, glow = wings({"white": VIOLET, "light": C("3A2560"), "shade": NAVY, "trim": VIOLET_G, "cyan": VIOLET_W}, {"trim", "cyan"})
    save("eclipse_wings", img)
    save("eclipse_wings_glow", glow)
    java([solar, eclipse, hal])
    print("cosmetics: textures in", TEX)


if __name__ == "__main__":
    main()
