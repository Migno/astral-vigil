# Astral Vigil – project context

NeoForge-only Minecraft mod. Mod-ID `astralvigil`, package `media.hofferbert.astralvigil`.
Theme: futuristic-angelic (halos, wings, floating rings, gold filigree, cyan cores). Four Watchers
(Solenne/sun, Vael/moon, Orrin/starsmith, Ilyra/chronicler) plus the fallen fifth, the *Eclipse*.

Work in phases (see `docs/ROADMAP.md`). After every phase: build of **all** targets green,
GameTests green, in-game smoke test via BlockPilot with screenshots in `docs/screenshots/<phase>/`,
Conventional-Commit, entry in `docs/CHANGELOG.md`, short status report.
Ask the owner only for hard-to-reverse decisions (mod-ID, license, publishing). Everything else:
decide and record in `docs/DECISIONS.md`.

## Targets (one codebase, Stonecutter)

| Stonecutter project | Minecraft | NeoForge | Java | Pack | GeckoLib |
|---|---|---|---|---|---|
| `1.21.1-neoforge` | 1.21.1 | 21.1.266 | 21 | ATM10 | 4.9.3 (`software.bernie.geckolib`) |
| `26.1-neoforge` (vcs/active) | 26.1.2 | 26.1.2.126 | 25 | ATM11 | 5.5.2 (`com.geckolib`) |

Versions live in `stonecutter.properties.toml` – verify against maven.neoforged.net / Modrinth before bumping.

- Source of truth in `src/` is **26.1** (`stonecutter active "26.1-neoforge"`). 1.21.1 code is in
  Stonecutter comments: `//? if >=26.1 {` … `//?} else {` `/*…*/` `//?}`. No nested `/* */` inside
  commented branches.
- `ResourceLocation` ↔ `Identifier` is swapped automatically (replacement in `stonecutter.gradle.kts`).
- Version-specific imports go **inside** the conditionals (formatter strips unused imports).
- Bigger API gaps: adapter in `compat/` (or a version-switched helper like `gametest/GT.java`).
- 26.1 is unobfuscated (no mappings); 1.21.1 uses Parchment.

## Commands (Windows / PowerShell, always the wrapper)

```powershell
.\gradlew.bat build                          # both targets (+ JUnit)
.\gradlew.bat :26.1-neoforge:runClient       # dev client (BlockPilot included)
.\gradlew.bat :1.21.1-neoforge:runClient -Pav.autojoin=true   # auto-create/join superflat test world
.\gradlew.bat runGameTestServer              # all GameTests, both targets, exits
.\gradlew.bat runData                        # datagen -> versions/<target>/src/generated/resources
                                             #   run it as its OWN invocation before build/runGameTestServer
                                             #   (same invocation may package stale generated resources)
.\gradlew.bat :<target>:runServer            # headless dedicated server
.\gradlew.bat :<target>:runCompatClient      # client + compat mods from Modrinth (see toml)
python -I tools/dev/resolve_compat.py         # refresh compat mod version ids (paste into stonecutter.properties.toml)
.\gradlew.bat spotlessApply                  # whitespace hygiene (spotlessCheck in CI); no code formatter – it breaks //? comments
.\gradlew.bat "Set active project to 1.21.1-neoforge"  # switch src/ to another target (switch back before committing!)
```

Run dirs: `run/<target>/{client,server,gametest,data,compat}` (git-ignored).

## Conventions

- Datagen for everything (models, blockstates, item models, lang en_us + de_de, loot, recipes, tags,
  advancements). Only hand-made assets: GeckoLib `.geo.json`/`.animation.json` from Blockbench.
  Generated output is committed per target in `versions/<target>/src/generated/resources`.
- English first (`en_us`), complete German (`de_de`) from day one – proper fantasy prose, proper
  names (Solenne, Halo Edge …) stay. A JUnit test fails if `de_de` misses keys.
- Recipes data-driven with tag inputs (`c:` + `astralvigil:` tags). No mixins where an event exists.
- All balance values in server config (`astralvigil-server.toml`), reasoning in `docs/BALANCE.md`.
  Client config: particles, screen-shake, beam quality.
- Balance: top tier ≈ netherite, never Allthemodium. Abilities cost Starlight + cooldown.
- Logging via SLF4J (`AstralVigil.LOGGER`), never `System.out`.
- Performance: beams/particles client-side; server only recomputes graphs on change.
- GameTests: add a `Def` to `gametest/AVGameTests.ALL`; structures in
  `src/main/resources/data/astralvigil/structure/gametest/*.nbt`.
- Research: alignments via `#astralvigil:alignment/<sol|luna|stella|void>` tags; player state in the
  `astralvigil:research` attachment; milestones are advancements (`research/*`) granted in `ResearchManager`.
- Codex: edit `datagen/CodexContent.java` (en + de), never the generated Patchouli JSON.
- Debug: `/astralvigil starfall [here]`, `/astralvigil research <alignment> <points>|reset`.
- Commits: Conventional Commits, no AI attribution lines.

## Art rules

Vanilla style, 16×16, ≤ 8 colours per texture, dark outline, light from top-left.
Palette: Seraph White `#FFFFFF`/`#D8D8E6` · Halo Gold `#F2C94C`/`#C9971E`/`#7A5A12` ·
Starlight Cyan `#8FEFFF`/`#2FA8D0` · Void Navy `#1E1A2E` (outline) · Eclipse Violet `#5B2A86`.
`.bbmodel` sources in `art/bbmodel/` (source of truth), exports into `src/main/resources/assets/astralvigil/`.
Emissive layers need a vanilla-RenderType fallback (GeckoLib emissivity is broken on macOS/Linux).

## Tooling

- **Blockbench MCP** (`C:\Tools\blockbench-mcp`, registered as `blockbench`): Blockbench must be running
  with `plugin/blockbench_mcp.js` loaded. Check: `curl http://127.0.0.1:8787/ping`. Bridge can also be
  driven directly: `POST http://127.0.0.1:8787/command {"id","action","params"}`.
- **BlockPilot** 1.3.1 (GitHub release jars via ivy repo, `localRuntime` only, never in release jar).
  After the first client start the launcher is `run/<target>/client/blockpilot/blockpilot.cmd`;
  registered as MCP `blockpilot-121` / `blockpilot-261`. CLI: `blockpilot.cmd --wait-ready 300 "status"`.
  Screenshots into docs: `.\tools\bp-shot.ps1 -Target 26.1-neoforge -Phase phase1 -Name foo -Before "<script>"`.
  `prepareDevOptions` writes `pauseOnLostFocus:false` before every client run.
- Decompiled MC/NeoForge sources: `versions/<target>/build/moddev/artifacts/*-sources.jar` and the
  NeoForge sources jar in the Gradle cache – grep them instead of guessing APIs.
