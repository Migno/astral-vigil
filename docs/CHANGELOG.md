# Changelog

All notable changes. Format based on [Keep a Changelog](https://keepachangelog.com/), versions follow SemVer.

## [Unreleased]

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
