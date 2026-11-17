# Astral Vigil – model and animation guide

The quality bar for every entity model, ornament and animation. Distilled from Blockbench/Minecraft creator guidance,
the Bedrock wiki and the 12 principles of animation as applied to games (sources at the end). Generators in
`tools/dev/` must follow it; reviewers check against it.

## Models

**Silhouette first.** A model must read at 30 blocks in darkness from its outline alone. Pick one or two silhouette
ideas per creature (the Eclipse: black-sun corona + tattered wings; a Watcher: halo + six wings) and let everything
else support them. Shape hierarchy: large forms carry the read (~60 % of the volume), medium forms give structure
(~30 %), small details are accents (~10 %). A rectangle is a weak silhouette – break it with asymmetry, gear and
negative space.

**Proportions anchored to Minecraft.** The player is the yardstick: head 8³, torso 8×12×4, limbs 4×12×4 (= 2 blocks).
Larger beings scale these ratios up, they do not stretch single parts.
- Heroic/celestial figures: head ≈ 1/7 of body height, broad shoulders, tapering waist.
- Hands ≈ 2/3 of the head's width; a finger is at most 1/3 of the forearm's length and has at most two segments.
  Claws may extend a finger by 1–2 px, never by a forearm. No spaghetti limbs.
- Thin parts (feathers, plates, cloth) are 0.5–1 px thick; nothing thinner than 0.5.

**One texel density.** 1 texture pixel = 1 model unit (16 px per block) on every face. No stretched or squashed pixels,
no non-uniform cube scaling; use `inflate` only for overlays (≤ 0.25). Sizes are whole numbers (0.5 only for thin plates).

**Cube economy.** Every cube contributes to the silhouette or to deformation. Merge cubes that never move apart.
A boss lands around 150–250 cubes, a Watcher 100–150, small entities 20–60.

**Bones are joints.** Pivots sit on real joints. Chains for overlapping action: shoulder → upper arm → forearm → hand;
wing root → mid → tip; hair/cloth/tendrils in 2–4 segments. Weapons and props are their own bones (hand-attached).

**Texturing.** Light from the top-left. Per material a 3–4 tone ramp with hue shift (cool shadows, warm highlights),
a darker edge on silhouette faces, ambient occlusion in crevices and under overhangs, a rim highlight on top edges.
Materials must contrast (cloth vs. plate vs. feather vs. glow). Emissive (glowmask) only on focal points – eyes,
cores, weapon edges, runes – at most ~15 % of the visible surface, so the glow means something.

## Animation

**Never still, never mechanical.** Idles layer 2–4 slow motions (breathing 3–4 s, hover bob, secondary sway) with
non-multiple periods so they never sync up. Secondary parts lag the primary motion.

**The 12 principles applied to attacks.**
- *Anticipation*: every attack has a readable wind-up; the bigger the hit, the longer and larger the wind-up
  (≥ 0.4 s, heavy hits 0.8–1.5 s). The body moves *opposite* to the strike first (draw back, crouch, inhale).
- *Timing and spacing*: wind-up eases in slowly, the strike is fast (2–4 frames to contact), recovery eases out.
  Use keyframes with easing (`"easing": "easeInCubic"` / `easeOutBack` / `easeInOutSine`, or `lerp_mode: catmullrom`),
  not single sine waves, for anything that is not a loop.
- *Contact hold*: hold the strike pose 2–4 frames so the hit lands visually.
- *Follow-through and overlapping action*: after the strike, limbs overshoot and settle; wings, cloth, tendrils,
  shards trail by 2–4 frames with smaller amplitude.
- *Arcs*: swings rotate through arcs (shoulder → elbow → wrist offsets), never straight linear slides.
- *Exaggeration and staging*: key poses must read from the silhouette alone – test them from the front and the side.
- *Weight*: big bodies settle; landings compress (scale/position dip) before recovering.
- *Recovery is vulnerability*: recovery poses open the body up and slow down – that is the player's window.

**Props and weapons appear with intent.** A summoned weapon materialises during the wind-up (scale 0 → 1.15 → 1,
glow first, form after), stays through the strike and dissolves during recovery (scale → 0, shards). Never pop.

**Lengths match the game.** A clip's length equals the server stage it accompanies (see `EclipseMove`), so the
animation and the hit stay in sync.

## Sources
- [Minecraft creator docs: entity modeling and animation](https://learn.microsoft.com/en-us/minecraft/creator/documents/entitymodelingandanimation)
- [Bedrock wiki: Blockbench guide](https://wiki.bedrock.dev/guide/blockbench.html) and [Bedrock modeling](https://wiki.bedrock.dev/visuals/bedrock-modeling)
- [Cobblemon wiki: creating a model](https://wiki.cobblemon.com/index.php/Tutorials/Creating_A_Model)
- [Ready Player Me modeling guidelines (silhouette, texel density)](https://docs.readyplayer.me/ready-player-me/customizing-guides/create-custom-assets/modeling-guidelines)
- [ChonkyCraft model bible (one or two silhouette ideas)](https://chonkycraft-wiki.pages.dev/docs/model-bible)
- [Game Developer: the 12 principles of animation in video games](https://www.gamedeveloper.com/production/the-12-principles-of-animation-in-video-games)
