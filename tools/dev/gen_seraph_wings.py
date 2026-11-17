"""Generates the Seraph Wings: geo + animations (atlas / Blockbench reference), the texture and the vanilla model rig
that client/SeraphWings.java renders (the wings stay a plain vanilla layer so they work with every player renderer).

Run: python -I tools/dev/gen_seraph_wings.py [--preview <dir>]
     then python -I tools/dev/gen_cosmetics.py (radiant/eclipse recolours are derived from seraph_wings.png)
Outputs:
  assets/astralvigil/{geo,geckolib/models}/entity/seraph_wings.geo.json
  assets/astralvigil/{animations,geckolib/animations}/entity/seraph_wings.animation.json
  assets/astralvigil/textures/entity/seraph_wings.png
  src/main/java/.../client/SeraphWingsRig.java (GENERATED: parts + keyframes, played by client/anim/Keyframes)

Rig (per side): wing_<side> (shoulder blade) > _mid (wrist) > _tip > _fan (primary feathers). Bone names are what
SeraphWings.java animates. Clips: idle (folded, breathing, lagging feather tips), glide (spread, rolling
undulation, primaries splayed), burst (Glide Burst: snap up, fast downstroke, tips trail and overshoot, settle into
the glide). Java angles equal the geo angles (geo is the Java model space with Y flipped).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import geckorig as gr  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "astralvigil")
JAVA = os.path.join(ROOT, "src", "main", "java", "media", "hofferbert", "astralvigil", "client", "SeraphWingsRig.java")
TW, TH = 64, 64
BODY_Y = 24  # geo height of the player body's pivot (top of the torso)

# 8 colours; gen_cosmetics.py recolours these keys for the developer wings
PALETTE = {"outline": "1E1A2E", "deep": "8C84A8", "shade": "B9B3CC", "light": "ECE8F4", "white": "FFFFFF",
           "trim_dark": "C9971E", "trim": "F2C94C", "cyan": "8FEFFF"}
FEATHER = ("deep", "shade", "light", "white")
SIDES = (("right", -1), ("left", 1))
PY, PZ, PX = 22.5, 2.5, 1.5
L1, L2, L3 = 4, 6, 5
REST_YAW, REST_ROLL, FAN = 8, 14, 20


def f_tone(i):
    return FEATHER[max(0, min(3, i))]


def m_bar(f, w):
    if f.face == "top" or f.side and f.y == 0:
        return "white", None
    if f.face == "bottom":
        return "shade", None
    return ("trim" if f.x % 3 else "trim_dark"), None


def m_coverts(f, w):
    if not f.side or f.face in ("right", "left"):
        return f_tone(gr.shade(f, 2)), None
    band, c = f.y % 2, (f.x + f.y // 2) % 2
    t = 3 - band - (1 if f.face == "back" else 0) - (1 if band and not c else 0)
    return f_tone(t), None


def feathers(f, width, stagger, glow):
    if not f.side:
        return "shade", None
    if f.face in ("right", "left"):
        return f_tone(gr.shade(f, 2)), None
    k, c = divmod(f.x, width)
    cut = abs(c - width // 2) + (k % stagger if stagger else 0)
    last = f.h - 1 - cut
    if f.y > last:
        return None, None
    if f.y == last and glow:
        return "cyan", None
    t = 2 + (1 if f.y < f.h // 3 else 0) - (1 if f.y > 2 * f.h // 3 else 0) - (1 if f.face == "back" else 0)
    if c == 0:
        t -= 1
    if f.y == last and not glow:
        t = 0
    return (f_tone(t) if t >= 0 else "outline"), None


MATERIALS = {"bar": m_bar, "coverts": m_coverts,
             "secondaries": lambda f, w: feathers(f, 2, 2, False),
             "primaries": lambda f, w: feathers(f, 3, 3, True),
             "lead": lambda f, w: feathers(f, 3, 0, True)}


def build():
    m = gr.Rig(TW, TH)
    m.bone("root", None, (0, BODY_Y, 0))
    for side, s in SIDES:
        x0 = s * PX

        def span(start, ln):
            return x0 + (start if s > 0 else -start - ln)

        n = f"wing_{side}"
        root = m.bone(n, "root", (x0, PY, PZ), (0, -s * REST_YAW, -s * REST_ROLL))
        m.cube(root, (span(0, L1), PY - 1, PZ), (L1, 2, 1), "bar")
        m.cube(root, (span(0, L1 + 1), PY - 5, PZ + 0.5), (L1 + 1, 4, 1), "coverts")
        mid = m.bone(n + "_mid", n, (x0 + s * L1, PY, PZ))
        m.cube(mid, (span(L1, L2), PY - 1, PZ), (L2, 2, 1), "bar")
        m.cube(mid, (span(L1 - 1, L2 + 1), PY - 10, PZ + 0.5), (L2 + 1, 9, 1), "secondaries")
        tip = m.bone(n + "_tip", n + "_mid", (x0 + s * (L1 + L2), PY, PZ))
        m.cube(tip, (span(L1 + L2, L3), PY - 1, PZ), (L3, 2, 1), "bar")
        fan = m.bone(n + "_fan", n + "_tip", (x0 + s * (L1 + L2), PY, PZ), (0, 0, -s * FAN))
        m.cube(fan, (span(L1 + L2 - 1, L3 + 2), PY - 12, PZ + 0.5), (L3 + 2, 11, 1), "primaries")
        m.cube(fan, (span(L1 + L2 + L3, 3), PY - 15, PZ + 1), (3, 15, 1), "lead")
    return m


# ------------------------------------------------------------------ animations (offsets from the rest pose)

def wing_pose(yaw, roll, mid=(0, 0), tip=(0, 0), fan=0):
    """yaw > 0 swings forward, roll > 0 raises; mid/tip are (yaw, roll) of the next joints; fan > 0 splays."""
    out = {}
    for side, s in SIDES:
        n = f"wing_{side}"
        out[n] = {"rotation": (0, s * yaw, -s * roll)}
        out[n + "_mid"] = {"rotation": (0, s * mid[0], -s * mid[1])}
        out[n + "_tip"] = {"rotation": (0, s * tip[0], -s * tip[1])}
        out[n + "_fan"] = {"rotation": (0, 0, -s * fan)}
    return out


FOLD = dict(yaw=-32, roll=-6, mid=(-38, -8), tip=(-26, -10), fan=-10)
GLIDE = dict(yaw=4, roll=-6, mid=(2, 2), tip=(0, -4), fan=10)
# developer wing cosmetics rest half-open and raised, so they read from every angle
DISPLAY = dict(yaw=0, roll=0, mid=(2, 6), tip=(0, 6), fan=12)


def looped(length, base, amps, period, lags):
    a = gr.Anim(length, loop=True)
    pose = wing_pose(**base)
    for side, s in SIDES:
        d = 0.1 if s < 0 else 0
        for seg, (ay, ar) in amps.items():
            n = f"wing_{side}{seg}"
            off = pose[n]["rotation"]
            a.osc(n, "rotation", period, (0, s * ay, -s * ar), phase=lags[seg] + d, offset=off)
    return a


def idle():
    return looped(8, FOLD, {"": (1.5, 2.5), "_mid": (2, 3), "_tip": (2, 4), "_fan": (0, -3)}, 4,
                  {"": 0, "_mid": 0.3, "_tip": 0.6, "_fan": 0.9})


def display():
    """Developer wings at rest: half-spread and raised, a slow majestic beat with the tips trailing."""
    return looped(6, DISPLAY, {"": (2.5, 7), "_mid": (3, 8), "_tip": (3, 11), "_fan": (0, -6)}, 3,
                  {"": 0, "_mid": 0.18, "_tip": 0.36, "_fan": 0.5})


def glide():
    return looped(2.4, GLIDE, {"": (1, 4), "_mid": (1.5, 5), "_tip": (2, 7), "_fan": (0, -4)}, 1.2,
                  {"": 0, "_mid": 0.08, "_tip": 0.16, "_fan": 0.24})


def burst():
    """Glide Burst (0.9 s): wings snap up and back (anticipation), a hard downstroke reaches contact in 3 ticks,
    held for 2, then the tips trail and overshoot while everything settles into the glide spread."""
    a = gr.Anim(0.9)
    fold, up = wing_pose(**FOLD), wing_pose(yaw=-18, roll=40, mid=(-8, 16), tip=(-4, 12), fan=-8)
    down = wing_pose(yaw=16, roll=-34, mid=(6, -14), tip=(4, -16), fan=16)
    hold = wing_pose(yaw=14, roll=-30, mid=(4, -12), tip=(2, -12), fan=18)
    over = wing_pose(yaw=6, roll=-2, mid=(2, 6), tip=(0, 8), fan=12)
    lag = {}
    for side, _ in SIDES:
        lag[f"wing_{side}_mid"], lag[f"wing_{side}_tip"], lag[f"wing_{side}_fan"] = 0.03, 0.06, 0.08
    a.pose_track([(0, fold, "linear"), (0.12, up, "easeOutCubic"), (0.27, down, "easeInCubic"),
                  (0.37, hold, "linear"), (0.6, over, "easeOutBack"), (0.9, wing_pose(**GLIDE), "easeInOutSine")], lag)
    return a


def animations():
    return {"idle": idle(), "display": display(), "glide": glide(), "burst": burst()}


# ------------------------------------------------------------------ Java rig

EASE_IDS = list(gr.EASINGS)  # index = Keyframes easing id


def jf(v):
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    return (s if s not in ("-0", "") else "0") + "f"


def java(m, anims):
    bones = [b for b in m.bones if b["name"] != "root"]
    by = {b["name"]: b for b in m.bones}
    out = ["package media.hofferbert.astralvigil.client;", "",
           "import media.hofferbert.astralvigil.client.anim.Keyframes;",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
           "/** GENERATED by tools/dev/gen_seraph_wings.py - do not edit. Box UVs match seraph_wings.png. */",
           "final class SeraphWingsRig {",
           f"    static final int TEX_W = {TW};", f"    static final int TEX_H = {TH};", "",
           "    /** Bone names in animation order; each bone's parent comes before it. */",
           "    static final String[] BONES = {" + ", ".join(f'"{b["name"]}"' for b in bones) + "};",
           "    /** Parent index into BONES, -1 = attached to the body. */",
           "    static final int[] PARENT = {" + ", ".join(
               str(next((i for i, c in enumerate(bones) if c["name"] == b.get("parent")), -1)) for b in bones) + "};",
           "    /** Rest rotation (radians) per bone; animations add to it. */",
           "    static final float[][] REST = {" + ", ".join(
               "{" + ", ".join(jf(math.radians(r)) for r in b.get("rotation", [0, 0, 0])) + "}" for b in bones) + "};", "",
           "    private SeraphWingsRig() {}", "",
           "    static void addWings(PartDefinition body) {",
           "        PartDefinition[] parts = new PartDefinition[BONES.length];"]
    for i, b in enumerate(bones):
        px, py, pz = b["pivot"]
        parent = by[b["parent"]]
        qx, qy, qz = parent["pivot"]
        cubes = CubeListBuilderLines(b, px, py, pz)
        rot = b.get("rotation", [0, 0, 0])
        pose = (f"PartPose.offsetAndRotation({jf(px - qx)}, {jf(-(py - qy))}, {jf(pz - qz)}, "
                f"REST[{i}][0], REST[{i}][1], REST[{i}][2])")
        holder = "body" if b["parent"] == "root" else f"parts[{[c['name'] for c in bones].index(b['parent'])}]"
        out.append(f"        parts[{i}] = {holder}.addOrReplaceChild(BONES[{i}], {cubes}, {pose});")
        _ = rot
    out += ["    }", ""]
    out.append("    // keyframes: per bone {times...}, {x, y, z per key (degrees)}, {easing per key}")
    for name, a in anims.items():
        out.append(f"    static final Keyframes.Clip {name.upper()} = new Keyframes.Clip({jf(a.length)}, {str(a.loop).lower()}, new Keyframes.Track[] {{")
        for b in bones:
            frames = a.tracks.get(b["name"], {}).get("rotation")
            if not frames:
                out.append("            null,")
                continue
            ts = sorted(frames)
            out.append("            new Keyframes.Track(new float[] {" + ", ".join(jf(t) for t in ts) + "},")
            out.append("                    new float[] {" + ", ".join(jf(v) for t in ts for v in frames[t][0]) + "},")
            out.append("                    new byte[] {" + ", ".join(str(EASE_IDS.index(frames[t][1])) for t in ts) + "}),")
        out.append("    });")
    out.append("}")
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")


def CubeListBuilderLines(b, px, py, pz):
    parts = ["CubeListBuilder.create()"]
    for c in b.get("cubes", []):
        (x0, y0, z0), (w, h, d), (u, v) = c["origin"], c["size"], c["uv"]
        parts.append(f".texOffs({u}, {v}).addBox({jf(x0 - px)}, {jf(-(y0 + h - py))}, {jf(z0 - pz)}, {jf(w)}, {jf(h)}, {jf(d)})")
    return "".join(parts)


def preview(out_dir):
    import json
    import geo_preview as gp
    import png
    os.makedirs(out_dir, exist_ok=True)
    geo = json.load(open(os.path.join(ASSETS, "geo", "entity", "seraph_wings.geo.json")))["minecraft:geometry"][0]
    # a stand-in player body for scale
    geo["bones"].append({"name": "player", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [0, 64]}, {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 64]},
        {"origin": [-4, 0, -2], "size": [8, 12, 4], "uv": [0, 64]}]})
    anims = {k: gp.parse_anim(v) for k, v in
             json.load(open(os.path.join(ASSETS, "animations", "entity", "seraph_wings.animation.json")))["animations"].items()}
    tex = png.read(os.path.join(ASSETS, "textures", "entity", "seraph_wings.png"))[2]
    tex = tex + [[(90, 110, 150, 255)] * TW for _ in range(40)]  # stand-in body texture below the atlas
    poses = [("rest", None, 0), ("idle", "idle", 1), ("display", "display", 0.6), ("glide", "glide", 0.3), ("burst-up", "burst", 0.12),
             ("burst-down", "burst", 0.3), ("burst-over", "burst", 0.62)]
    img = gp.sheet(geo, tex, None, anims, poses, [0, 90, 180, 35], 6)
    png.write(os.path.join(out_dir, "seraph_wings.png"), img)
    print("preview in", out_dir)


def main(argv):
    m = build()
    gr.write_json(ASSETS, ("geo/entity", "geckolib/models/entity"), "seraph_wings.geo.json",
                  m.geo("geometry.seraph_wings", 3, 2.5, 0.75))
    anims = animations()
    gr.write_json(ASSETS, ("animations/entity", "geckolib/animations/entity"), "seraph_wings.animation.json",
                  gr.animation_file(anims))
    mats = {k: (lambda p: (lambda f, w: p(f, w)))(v) for k, v in MATERIALS.items()}
    gr.paint(m, {"seraph_wings": PALETTE}, mats, os.path.join(ASSETS, "textures", "entity"), "{}", max_colours=8)
    os.remove(os.path.join(ASSETS, "textures", "entity", "seraph_wings_glowmask.png"))  # plain wings have no glow pass
    java(m, anims)
    print("seraph wings:", m.count(), "cubes,", len(m.bones), "bones, cursor", m.cursor)
    print("  clips:", ", ".join(f"{k} {a.length}s" for k, a in anims.items()))
    if "--preview" in argv:
        preview(argv[argv.index("--preview") + 1])


if __name__ == "__main__":
    main(sys.argv[1:])
