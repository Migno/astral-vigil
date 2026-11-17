# Changelog

All notable changes. Format based on [Keep a Changelog](https://keepachangelog.com/), versions follow SemVer.

## [Unreleased]

### Added
- **Minecraft 26.2 support** (NeoForge 26.2.0.95+, GeckoLib 5.5.6+). Same features as 26.1.2; all 36 GameTests pass.
  Patchouli has no 26.2 build yet, so the Codex Caelestis only shows a hint until it does. JEI and Jade work.
  The mod icon uses `iconFile` on 26.2 (NeoForge 26.2 shows a warning screen for the old `logoFile`).

- **Astrolabe lens (Thaumcraft-style).** Hold use to scan: a ring of starlight fills around the crosshair and the
  scan completes after 1.2 s; it reaches 16 blocks (`scanRange`) and works on blocks, creatures and - pointed at the
  open sky - the biome or structure. Held or **worn on the head**, a panel beside the crosshair names what you look
  at, its alignments and whether you have already researched it.
- **More to research:** every alignment gained 6-10 blocks (cactus, sandstone, froglights, coral, chorus, purpur,
  blackstone, wither roses …), the `c:` ore tags (gold → Sol, lapis → Luna, diamond/emerald → Stella, ancient debris →
  Void) and 2-4 creatures each (armadillo, frog, turtle, breeze, bogged, witch …).

### Changed
- Astrolabe: scanning now needs holding use instead of a single click (clicking a block no longer scans it at once).
- Balance: `attunementThreshold` 12 → 16, because the alignment lists grew. **Upgrade note:** existing servers keep
  their old value in `astralvigil-server.toml`; set it to 16 by hand for the new balance.

### Project
- Third Stonecutter target `26.2-neoforge`. API differences are mostly handled by Stonecutter replacements
  (`EntityType.X` -> `EntityTypes.X`, advancement triggers package, `mainCamera()`); tag datagen goes through
  `compat/TagOut` because 26.2 tags only accept registry keys.
- Minecraft 26.3 port started (not shipped yet): the data generators need a rewrite for 26.3's new datagen API.
- Versioning rule: the version goes up once per release; every release gets a detailed changelog.

## [0.0.1-alpha] - 2026-11-17

First public alpha for multiplayer testing. Everything below is part of it.

### Multiplayer readiness (tested on dedicated servers, both targets)
- **Trials are per player:** Vael's moon targets light up only for the archer who hit them (progress on the action
  bar, the world stays dark for everyone else); Orrin's mirror puzzle turns back to its start when someone begins the
  trial, unless another player is solving it right now (then they solve it together).
- **No more freeze on a realm's first visit:** realms are split into pieces; the arrival shrine appears at once and
  the rest is placed block by block within ~8 ms per tick. An interrupted build resumes on the next visit.
- **Watchers float right beside the bridge** so they can be spoken to from the walkway (they hovered out of reach);
  Watchers that float too high sink back to hover height.

### Models and animations, AAA pass (`docs/ART_GUIDE.md`)
- **The Eclipse**: rebuilt to player-anchored proportions (no more spaghetti fingers), jointed arms, wings and
  tendrils, 275 cubes / 63 bones; every one of its 39 animations re-keyed with anticipation, contact holds and
  follow-through. It now **summons a weapon for each attack** that flares in with a glow during the wind-up and shatters
  in the recovery: Umbral Scythe (Sweep), Void Lance, Starfall Scepter, Echo Orb, Eclipse Claws (Shadow Dive),
  Eclipse Focus (Ray and Nova), Umbral Chain (Grasp), with particle bursts and sounds.
- **Eclipse Echo** and **Umbral Pylon** rebuilt the same way (the Echo gains a glow layer).
- **The Watchers**: rebuilt seven heads tall with six jointed wings, cloth and ribbons; each Watcher carries its own
  attribute (Solenne's sun crown and disc, Vael's crescent, Orrin's hammer and anvil sigil, Ilyra's tome, quill and
  rune tablets). New animations: a living idle, cast, talk, **greet** (when you approach) and **bless** (when you
  complete their trial).
- **Seraph Wings**: regenerated rig with folding idle, gliding spread and a **burst** stroke for the Glide Burst.
- Tools: `gen_eclipse.py`, `gen_watcher.py`, `gen_seraph_wings.py`, `geckorig.py`, previews; the web atlas plays
  GeckoLib easings and Catmull-Rom; Blender key-art pipeline (`tools/marketing/keyart.py`).

### The realms of the Firmament (no more hub)
- Every gate now opens onto a **realm of its own** – a separate dimension with a fixed sky, fog, ambient particles
  and a handcrafted template (`tools/dev/gen_realms.py`), built on the first visit:
  **The Firmament** wilds (Astral Gate; starwood groves, moonlit), **The Sunspire** (eternal dawn, a vast sun ring),
  **The Moonwell** (full-moon midnight, a giant crescent, silver groves), **The Starforge** (deep space: a crystal-vaulted forge hall
  over the mirror bays, a rose window, the Great Forge anvil under a captured star in an armillary, nebulae, an asteroid
  belt, ringed moons), **The Endless Archive** (a grand library around the maze: clerestory, mansard roof, ribbed dome,
  round towers, a portico; floating bookshelf stacks with rope bridges, giant books, an inkwell and quill) and
  **The Umbral Cathedral** (End sky, a broken cathedral with a pillar nave, vault ribs, lancet windows and a rose
  window, the throne in the apse, a shattered halo above).
- Animated floating blocks drift and turn in every realm (vanilla block displays, server-targeted, client-interpolated).
- A realm title card on arrival. Trial realms are sacred ground: no digging, no building, ender pearls fizzle, no
  elytra on the Sun Spire. Return gates and shrines can't be broken anywhere. Falling players **and dropped items**
  are caught and set back on the arrival shrine. The Sun Spire's crown counts only after all four gold pads.
- `/astralvigil realm <id>` builds and visits a realm (admin).

### Celestial masonry: 51 new building blocks
- **Starwood** (a tree of the Firmament): logs, wood, stripped variants, planks, stairs, slab, fence, gate, door,
  trapdoor, button, pressure plate, glowing leaves, sapling, gilded starwood.
- **Celestial Marble** (+ stairs/slab/wall, bricks + stairs/slab/wall, chiseled, pillar, gilded, cracked),
  **Sunstone** and **Moonstone** (+ brick sets), **Umbral Stone** (+ brick set, chiseled, cracked, pillar).
- Lights and glass: Astral, Sunstone, Moonstone and Umbral lamps, Star Lantern, Constellation Tiles, Halo Glass,
  Nebula Glass, Eclipse Glass. Everything craftable (tag inputs, stonecutter variants, smelting for cracked bricks);
  own creative tab.

### The Eclipse reborn
- New model (216 cubes, six tattered wings, black-sun corona, cracked halo) with 36 animations and a glow layer.
- A real boss: one move at a time with wind-up/strike/recovery, synced to the client, telegraphs drawn client-side.
  Umbral Sweep, Void Lance, Falling Stars, Echoes, Shadow Dive, Eclipse Ray, Umbral Nova, Blink, Umbral Grasp.
- Phases: *Fallen Light*, *Shattered Halo* (four Umbral Pylons shield it), *Total Eclipse* (darkness, cover walls,
  enrage below 10 %). Grounded windows for melee, entrance, leash and reset, health scales with players, no phase
  skipping, loot for every participant on the dais, Umbral Sigil refunded on Peaceful.
- New entity `astralvigil:umbral_pylon`; `[eclipse]` config section.

### Celestial armour
- **Solar Regalia** (Sunsteel) and **Lunar Raiment** (Moonsilver) join a reworked **Seraph Plate**: new textures,
  glowing seams and 3D ornaments (halos, crests, wings, sun disc, crescent circlet, mantle) on players and armour
  stands; Seraph ornaments grow with awakening. Full-set bonuses with tooltips (`[armor]`), three advancements,
  Codex entries.

### Game-loop fixes (from a full audit)
- Marks count by advancement: a lost Mark no longer locks the Umbral Gate or the summon; Marks resist fire.
- Guaranteed Void Shards: Astral Forge recipe (crying obsidian + Astral Shard + amethyst → 2). The Umbral Sigil takes
  Void Shards instead of silk-touched Void Crystals.
- Prism Gate corners: diamonds instead of diamond blocks. Starfall `chancePerRoll` 0.035 → 0.06.

### Presentation
- README rewritten for GitHub: pitch, shader gallery (Complementary Reimagined), gate table, journey.
- The Firmament has its own biome (meadow features, no precipitation): no more snow piling up on the trial islands.

### A gate for every challenge
- Six gate kinds, each with its own frame block, portal colour and destination: **Astral** (hub), **Solar** (Sun
  Spire), **Lunar** (Moon Arena), **Prism** (Prism Observatory), **Archive** (Endless Library), **Umbral** (Umbral
  Throne – only lights for a player carrying all four Marks). All lit with the Ascension Key; 4 frames per craft.
- The bridges are gone: every trial island and the throne has its own arrival platform with a return gate, so its
  gate is the only way in.

### Firmament: built gate, large-scale trials, Umbral Throne
- **Astral Gate:** the Ascension Key no longer teleports. Build an upright, closed frame of **Astral Gate** blocks
  (crafted from Starmetal, Astral Shards and Meteoric Stone; opening 2×3 up to 21×21) and set the key into it to
  light a **Celestial Portal**. Stand in it for 3 seconds to ascend; the return gate stands on the hub.
- **The Firmament gets its own dimension type** (build limit y 448) for the larger structures.
- **Trial islands 240 blocks out**, each a long challenge:
  - **Solenne – Sun Spire:** a 100-block spire with 64 single pads, magma pads that burn, and gold checkpoint pads.
  - **Vael – Moon Arena:** 10 Moon Targets, 4 of them on far floating islets, while Eclipse Echoes keep rising.
  - **Orrin – Prism Observatory:** three bays, mirrors start wrongly turned, all three receptors must glow at once.
  - **Ilyra – Endless Library:** a dark, roofed 70×70 bookshelf maze with the reading room at its heart.
- **The Umbral Throne:** the Eclipse can only be summoned on its own dark arena island 520 blocks west (bridge through
  Vael's arena), with obsidian cover pillars against the Umbral Nova. The Eclipse now has 600 HP and 14 armour.
- Trials last 20 minutes; the sky's safety net sets falling players down at the nearest island.

### Watchers reworked
- The four Watchers are now towering, floating seraphs (≈ 5 blocks): six layered wings, a sun-disc nimbus behind the
  head, a spinning halo, a sunburst crown, faceless helm with glowing eyes, glowing chest core, gold stole, a flared
  gown dissolving into drips of light and crystal shards orbiting the chest. Emissive glowmask on every Watcher.
- New idle (float, wing beats, spinning halos), cast and talk animations.

### Developer cosmetics
- **Celestial Wardrobe** (key **J**): one cosmetic per slot with a turning live preview of your own player.
  Head: **Solar Crown**, **Eclipse Crown** (crackles), **Seraph Halo** (floating, spinning). Cape: **Seraph Cape**,
  **Starfield Cape** (twinkling stars), **Eclipse Cape**. Wings: **Radiant Wings**, **Eclipse Wings**. Aura:
  **Starlight Motes**, **Solar Embers**, **Void Storm** (forked lightning arcs).
- Emissive glow passes with a slow pulse on every cosmetic; vanilla model layers on both targets, visible to everyone.
- Access is decided by the server (built-in team list, `[cosmetics] developers` UUID list, dev environment);
  `[cosmetics] enabled = false` turns them off for a server.
- Debug commands: `/astralvigil cosmetic <id>|clear`, `/astralvigil showcase network|ritual|forge|armory`.
- Full screenshot suite in `docs/screenshots/suite/`.

### Phase 9 – Release readiness
- README rewritten in English and German (journey, integrations, configuration, building).
- Modrinth and CurseForge page drafts in `docs/publishing/` (not published).
- Performance profile (26.1 compat instance with Sodium/Iris/JEI/Jade/Patchouli/Apotheosis/KubeJS, 32 collector
  chains + Eclipse + Echoes): all Astral Vigil block entities together ≈ 0.06 ms/tick, renderers ≈ 0.02 ms/frame,
  server 4.5 ms/tick average.
- Compat check on both targets: no Astral Vigil errors; fixed a Codex icon pointing at an item-less block.

### Phase 8 – Endgame
- **The Eclipse:** a three-phase GeckoLib boss (idle / attack / cast / phase transition / death animations) with a
  purple boss bar and darkened sky. Phase 1: sweeps and telegraphed Void Lances. Phase 2: Eclipse Echo waves.
  Phase 3: blinks and Umbral Novas – take cover behind blocks. Summoned with the **Umbral Sigil** in the Firmament
  while carrying all four Marks.
- **Rewards:** Eclipse Heart → **Halo of Dawn** (cheaper abilities, less Eclipse Gaze), **Seraph Feathers** → toggleable
  **Seraph Wings** cosmetic visible to everyone (spread when falling).
- **Eclipse Echoes** are real creatures now (GeckoLib), also summoned by high Eclipse Gaze.
- Endgame advancements (Umbral Invitation, Dawn After Eclipse, Halo Restored, Feathers of the Fallen), codex entry,
  Eclipse Seal block, `[eclipse]` server config section.
- The sky beneath the Firmament hub catches falling players and returns them to the landing.
- Fixed: impossible-criterion advancements granted from code now award whatever criteria they have.
- JUnit for the phase formula; GameTests for phase transition, drops, echo lifetime and the kill advancement.

### Phase 7 – Firmament
- **GeckoLib integration** on both targets (4.9.3 on 1.21.1, 5.5.2 on 26.1) – required dependency.
- **The Firmament:** a sky dimension of floating islands with low gravity and soft landings, reached with the
  **Ascension Key** (Convergence Altar, 2000 pure starlight, Stella research). Falling off the world returns you to
  the landing.
- **Four Watchers** (Solenne, Vael, Orrin, Ilyra) as animated GeckoLib entities (idle/cast/talk) in their sanctums,
  connected to the landing by cloud-stone bridges.
- **Trials:** Solenne's golden climb, Vael's six Moon Targets (archery), Orrin's beam puzzle (guide Stella light to a
  Trial Receptor with mirrors), Ilyra's library maze. Each grants the Watcher's **Mark**, a challenge advancement and
  **favour**; rival Watchers lose favour.
- New blocks: Firmament Stone, Starlight Font (endless starlight source), Trial Receptor, Trial Beacon, Moon Target.
- Codex category "The Firmament" (en/de); generator script `tools/dev/gen_firmament.py`.
- JUnit for Watcher rivals; GameTests for the Firmament templates and trial rewards.

### Phase 6 – World
- **Fallen Observatories:** ruined round observatories (datapack worldgen: structure, template pool, structure set,
  biome tag `#astralvigil:has_structure/fallen_observatory`) with a telescope, an open chest and a vault sealed by
  **Constellation Locks** that dissolve for players who learned the sign (four variants).
- **Lost Chronicle:** 24 lore fragments (en/de) in observatory loot; reading one unlocks its Codex page.
- **Star Anchors:** lay out a learned constellation (any rotation, mirrored, scale 1–3), fill with starlight and
  touch an anchor → timed Watcher blessing for nearby players, Gaze relief (scales with layout size).
- **Eclipse Gaze:** abilities and convergence rituals raise it; stages bring whispers, Eclipse Echoes, Void Crystal
  crystallisation and eclipse surges; only rituals lower it. All configurable / switchable.
- Void Crystal + Void Shard, generator script `tools/dev/gen_observatory.py` for the templates.
- JUnit for anchor pattern matching; GameTests for rituals, locks and templates.

### Phase 5 – Gear
- **Living gear:** Halo Edge (sword) and Dawnbreaker (battle axe) of Sunsteel, Crescent (bow) and Tidecaller
  (crossbow) of Moonsilver, Starlight Aegis (shield) and Seraph Plate (armour) of Starmetal – forged at the
  Convergence Altar from vanilla diamond gear / bow / crossbow / shield.
- **Deeds awaken gear** (Dormant → Awakened at 25 → Ascended at 100): undead slain by day (Sol blades), night kills
  with full-moon bonus (Luna bows), blocked attacks (Aegis), Void creatures slain in the full Seraph set.
- **Sigils & stances:** 7 sigils (Whirl, Shieldbreaker, Dawn, Flash, Precision, Undertow, Aegis); 1/2/3 sockets by
  level; sneak-use with a sigil in the off hand to socket; keys V (ability) and B (stance).
- **Starlight charge:** abilities cost charge + cooldown; gear recharges under the night sky or from a reservoir.
- Bonuses are utility-first: night/precision arrows, undead bonus per level, Seraph glide (Awakened) and fall immunity
  (Ascended). Raw damage stays at Netherite level.
- Bow/crossbow draw frames on both targets (26.1 item model definitions, 1.21.1 vanilla property functions),
  equipment asset for Seraph Plate on 26.1, armour layers on 1.21.1.
- Codex category "Living Gear"; JUnit for gear rules; GameTests for socketing, deeds and the Whirl ability.

### Phase 4 – Crafting
- **Convergence Altar + Pedestals:** infusion ritual (catalyst + up to 12 offerings), powered by starlight beamed in or
  fed by a touching reservoir. **Stability** from pedestal symmetry, nearby aligned blocks (recipe alignment), Void
  blocks and sky access; instability flings offerings or summons configurable creatures.
- **Astral Forge:** up to four items + starlight of one tint → Sunsteel (Sol), Moonsilver (Luna), Starmetal (Stella).
- New recipe types `astralvigil:convergence` and `astralvigil:astral_forge` (vanilla recipe system, tag inputs,
  KubeJS/datapack friendly); 26.1 syncs them to clients explicitly.
- New materials: Starmetal/Sunsteel/Moonsilver ingots (`c:ingots/*`), Astral Core, Sigil Blank.
- Floating item renderer for pedestals, altar and forge (both render pipelines).
- **JEI** plugin (both targets) and **EMI** plugin (1.21.1; no EMI build for 26.1 yet), Jade: stability, progress,
  held items.
- `/astralvigil starlight <pos> <tint> <amount>` admin command.
- Blockbench models (altar, pedestal, forge) + five item textures; codex category "Convergence" (en/de).
- GameTests: forge crafting, symmetric ritual, asymmetry lowers stability.

### Phase 3 – Energy
- **Starlight network:** Starlight Collector (gathers under the open sky, scaled by the starlight formula), Lens
  (lossless straight relay), Mirror (turns beams from any side, small loss), Prism (splits pure light into Stella /
  Sol / Luna), Starlight Reservoir (tinted storage, comparator output, feeds touching devices).
- Visible tinted beams (vanilla beacon beam, rotated and thinned) – client-side renderer on both targets; beams pass
  through `#astralvigil:beam_transparent` (glass, panes) and stop at other blocks.
- Devices rotate with an empty hand (sneak = backwards); placement aims away from the player.
- **Jade plugin** (optional): stored starlight per tint, collector rate, aim direction.
- Blockbench models for all five devices; codex category "Starlight" (en/de); recipes via tags.
- Tests: StarlightBuffer JUnit; GameTests for beam transfer, blocking vs. glass, prism split, reservoir feeding.

### Phase 2 – Research
- **Celestial alignments** Sol, Luna, Stella, Void – fully tag-driven (`#astralvigil:alignment/<name>` for blocks,
  entity types, biomes and structures) with sensible vanilla defaults.
- **Scanning** with the Astrolabe: blocks (use on block), creatures (use on entity), biome or structure (use into
  the air). First scans grant alignment knowledge; reaching the threshold attunes the player.
- **Research data** as a persistent, death-surviving player attachment, synced to its owner.
- **Star Chart** item + minigame screen: connect the stars of revealed constellations (Halo, Crescent, Anvil, Quill)
  while avoiding decoy stars; server re-validates.
- **Codex Caelestis** (Patchouli, optional): generated book with four categories, entries unlock with research
  advancements; full en_us + de_de content.
- Research advancements (first scan, four attunements, four constellations).
- `/astralvigil research <alignment> <points>` and `/astralvigil research reset`.
- Compat instance now pins Modrinth **version ids** (`tools/dev/resolve_compat.py`) – version numbers resolved to
  Fabric jars for multi-loader releases. Compat client (26.1) verified: JEI, Jade, Curios, Patchouli, Sodium, Iris,
  Apotheosis, KubeJS, GeckoLib load together with Astral Vigil.

### Phase 1 – Foundation
- Registries: Astral Shard, Astrolabe, Fallen Star, Meteoric Stone; creative tab "Astral Vigil".
- **Starfall:** at night stars may fall near players (server-config driven, full-moon bonus, per-player cooldown);
  particle trail, impact, small crater lined with Meteoric Stone and a glowing Fallen Star (2–4 Astral Shards).
- **Astrolabe:** reads starlight %, moon phase and sky access, and tracks recently fallen stars.
- Server config (`astralvigil-server.toml`) + client config (particles, screen shake, beam quality) with config screen.
- `/astralvigil starfall [here]` operator command.
- Datagen for both targets: block states/models, item models, lang en_us + de_de, loot, recipe, tags, advancements.
- First textures (vanilla style, palette-bound) made in Blockbench via MCP; mod icon.
- Tests: JUnit (starlight formula, compass, de_de completeness + placeholders), GameTests (crater, starfall landing,
  fallen star drops, starlight under roof).

### Phase 0 – Setup
- Multi-version build (Stonecutter 0.10 + ModDevGradle 2.0.148) for Minecraft 1.21.1 (NeoForge 21.1.266)
  and 26.1.2 (NeoForge 26.1.2.126).
- Run configs per target: `runClient`, `runServer`, `runGameTestServer`, `runData`, `runCompatClient`.
- BlockPilot dev-only dependency for scripted in-game testing; Blockbench MCP set up.
- GameTest framework abstraction for both GameTest APIs, first smoke test.
- GitHub Actions matrix build + GameTests, Spotless, EditorConfig, project docs.
