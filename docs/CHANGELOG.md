# Changelog

All notable changes. Format based on [Keep a Changelog](https://keepachangelog.com/), versions follow SemVer.

## [Unreleased]

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
