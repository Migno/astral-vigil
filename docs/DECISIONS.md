# Decisions

Format: date · decision · reason. Decisions that are hard to reverse are marked **(owner)**.

## 2026-11-16 – Phase 0

- **Mod-ID `astralvigil`, package `media.hofferbert.astralvigil`** (owner, given in the brief).
- **Repository public on GitHub** (`Migno/astral-vigil`) (owner, requested).
- **Build:** Gradle 9.9.1 (Kotlin DSL) + ModDevGradle 2.0.148 + Stonecutter 0.10. One codebase, two
  Stonecutter projects `1.21.1-neoforge` and `26.1-neoforge`. NeoForge only, no Architectury.
- **Active/VCS version is 26.1.** Newest API is the source of truth; 1.21.1 lives in Stonecutter
  comments. `ResourceLocation`→`Identifier` handled by a Stonecutter string replacement.
- **Versions** (verified 2026-11-16): NeoForge 21.1.266 / 26.1.2.126, Parchment 1.21.1-2024.11.17,
  GeckoLib 4.9.3 / 5.5.2, BlockPilot 1.3.1.
- **BlockPilot is not on Modrinth** – it ships as GitHub release assets. Resolved through an Ivy
  pattern repository (`dev.blockpilot:blockpilot:<ver>:<mc>-neoforge`), `localRuntime` only, so it
  never ends up in the release jar or POM.
- **Compat instance:** compat mods are a separate non-transitive `compatMods` configuration copied into
  `run/<target>/compat/mods` (`prepareCompatMods`) – production jars work in dev because NeoForge
  runs Mojang names at runtime on both targets. 26.1.2 has no EMI build → JEI there, EMI on 1.21.1.
  Embeddium/Oculus are not needed: Sodium and Iris have official NeoForge builds for both targets.
- **GameTests:** version-neutral catalogue (`AVGameTests.ALL`) + version-specific registration
  (`@GameTestGenerator` on 1.21.1, `TEST_FUNCTION` registry + `RegisterGameTestsEvent` on 26.1).
- **Generated resources are committed** per target (`versions/<target>/src/generated/resources`),
  because data formats differ between 1.21.1 and 26.1 and CI should not need a datagen client.
- **Spotless without a code formatter.** palantir-/google-java-format rewrite `//?` Stonecutter comments
  to `// ?` and sort imports out of version conditionals, which breaks the inactive target. Spotless only
  enforces whitespace hygiene; style comes from `.editorconfig`. It lives on the root project (Stonecutter
  warns that root becomes "buildable" through the `base` plugin – harmless; `spotlessCheck` runs in CI).
- **Project path contains a space** (`D:\dev\minecraft\Astral Vigil`). Gradle/MDG handle it; all
  scripts quote paths. If a tool breaks on it, move to a path without spaces.
- **Windows Defender:** excluding the project folder and `%USERPROFILE%\.gradle` speeds up builds a
  lot. Not applied automatically – owner can run (admin PowerShell):
  `Add-MpPreference -ExclusionPath "D:\dev\minecraft\Astral Vigil","$env:USERPROFILE\.gradle"`

## Open (owner)

- **License.** Placeholder "All Rights Reserved" until decided. Proposal: code under **MIT** (best
  for packmakers/addons, matches ecosystem), assets under **CC BY-NC-SA 4.0** or ARR to protect the
  art. Alternative: LGPL-3.0 for code (forks must stay open).
