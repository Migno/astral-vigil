# Changelog

All notable changes. Format based on [Keep a Changelog](https://keepachangelog.com/), versions follow SemVer.

## [Unreleased]

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
