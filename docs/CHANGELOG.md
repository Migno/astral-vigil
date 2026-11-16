# Changelog

All notable changes. Format based on [Keep a Changelog](https://keepachangelog.com/), versions follow SemVer.

## [Unreleased]

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
