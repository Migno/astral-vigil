# Astral Vigil

*The Watchers guard the firmament. A rift in the sky rains star fragments upon the world – and the Eclipse awakens.*

**Catch falling stars. Bend starlight. Build the gates to the sky – and face the Watchers in their trials.**

Astral Vigil is a NeoForge progression mod about starlight. It starts with a meteor crashing into your world on a clear
night and ends far above the clouds in the **Firmament**: six realms of the sky, each behind a gate you build
yourself – an eternal dawn, a moonlit midnight, a forge among the stars, an endless archive and the broken cathedral
of the Eclipse, the fallen fifth Watcher.

![The Umbral Cathedral, where the Eclipse waits](docs/screenshots/realms/26.1-neoforge-02-cathedral-nave.png)

| Minecraft | Loader | Java | Requires | Status |
|---|---|---|---|---|
| 1.21.1 | NeoForge 21.1.266+ | 21 | GeckoLib 4.9+ | feature complete, pre-release |
| 26.1.2 | NeoForge 26.1.2.126+ | 25 | GeckoLib 5.5+ | feature complete, pre-release |

[Deutsch weiter unten](#deutsch) · [Changelog](docs/CHANGELOG.md) · [Roadmap](docs/ROADMAP.md) ·
[Balance](docs/BALANCE.md) · [Design decisions](docs/DECISIONS.md)

## Why Astral Vigil?

- **The night sky matters.** Starlight depends on the time, the moon phase, the weather and an open sky. Stars
  literally fall: meteors carve craters and leave Fallen Stars to mine.
- **Light you can see.** Collectors, Lenses, Mirrors, Prisms and Reservoirs throw visible beams across your base.
  Starlight is pushed, not polled: a whole network costs a fraction of a millisecond per tick.
- **Rituals with rules.** Convergence rituals reward symmetry and aligned blocks. Get sloppy and they turn unstable:
  offerings fly and something answers.
- **Gear that grows with you.** Six pieces of living gear awaken through your deeds, take socketed Sigils and unlock
  starlight-powered abilities. Top tier is on par with Netherite, never above it.
- **Realms you have to earn.** Six kinds of gates, each built and lit by hand, each opening onto a realm of its own
  with its own sky: a hundred-block climb at eternal dawn, an archery trial under a full moon, a puzzle of light in
  deep space, a lightless library maze – and the broken cathedral of the Eclipse. Blocks drift in the air around you.
- **A boss that fights like one.** The Eclipse has 36 animations and nine telegraphed moves across three phases: a
  pylon shield, a sweeping ray, a Nova you must hide from, and windows to strike it in melee.
- **Armour that looks the part.** Seraph Plate, Solar Regalia and Lunar Raiment with glowing seams and 3D halos,
  wings, sun discs and mantles – and full-set bonuses.
- **Build with the sky.** Over fifty craftable blocks: glowing starwood, celestial marble, sunstone, moonstone, umbral
  stone, lamps, lanterns and stained glass of nebula and eclipse.
- **Made for modpacks.** Every balance value is in the server config, every recipe uses `c:` tags, worldgen is a
  datapack. Optional JEI, EMI, Jade, Patchouli and KubeJS support. Built with All the Mods 10 and 11 in mind.
- **Fully playable in English and German.**

## Gallery

*Shown with Complementary Reimagined.*

| | |
|---|---|
| ![The Umbral Cathedral from above](docs/screenshots/realms/26.1-neoforge-01-umbral-cathedral.png) | ![The Eclipse](docs/screenshots/realms/26.1-neoforge-03-the-eclipse.png) |
| ![The Sunspire at eternal dawn](docs/screenshots/realms/26.1-neoforge-04-sunspire.png) | ![The Moonwell](docs/screenshots/realms/26.1-neoforge-05-moonwell.png) |
| ![Vael's Moon Arena](docs/screenshots/realms/26.1-neoforge-06-moon-arena.png) | ![The Starforge, its crystal vault and captured star](docs/screenshots/realms/26.1-neoforge-07-starforge.png) |
| ![The Endless Archive](docs/screenshots/realms/26.1-neoforge-08-endless-archive.png) | ![The library portico](docs/screenshots/realms/26.1-neoforge-09-archive-portico.png) |
| ![Seraph Plate in the nave](docs/screenshots/realms/26.1-neoforge-11-seraph-plate-in-the-nave.png) | ![The Firmament shrine](docs/screenshots/realms/26.1-neoforge-10-firmament-shrine.png) |
| ![Vael, Watcher of the Moon](docs/screenshots/shaders/26.1-neoforge-12-watcher-vael.png) | ![Orrin, Watcher of the Forge](docs/screenshots/shaders/26.1-neoforge-13-watcher-orrin.png) |
| ![A starlight network](docs/screenshots/shaders/26.1-neoforge-17-starlight-network.png) | ![A convergence ritual](docs/screenshots/shaders/26.1-neoforge-18-convergence-ritual.png) |

More in [`docs/screenshots/`](docs/screenshots/).

## The journey

1. **Starfall.** On clear nights, fragments of the sky crash into the world. Mine the Fallen Star for Astral Shards
   and craft an **Astrolabe**: it reads the sky, the moon phase and the starlight where you stand, and tracks fallen
   stars.
2. **Research.** Study blocks of the four alignments – *Sol*, *Luna*, *Stella* and *Void* – and connect the
   constellations on the **Star Chart**. The **Codex Caelestis** records what you learn.
3. **Starlight.** Collectors gather light under the open night sky. Guide it with Lenses and Mirrors, split it into
   Sol, Luna and Stella with a Prism and store it in Reservoirs.
4. **Convergence.** Rituals at the **Convergence Altar** and the **Astral Forge** turn offerings and starlight into
   Starmetal, Sunsteel and Moonsilver.
5. **Living gear.** Halo Edge, Dawnbreaker, Crescent, Tidecaller, Starlight Aegis and Seraph Plate awaken through
   deeds, take **Sigils** and unleash abilities that cost starlight.
6. **The world.** Explore **Fallen Observatories**, open their Constellation Locks, collect the 24 pages of the Lost
   Chronicle and hold **Star Anchor** rituals – while the **Eclipse Gaze** grows with every use of celestial power.
7. **The gates.** Forge the **Ascension Key**, build a gate frame and set the key into it. The frame decides where it
   leads:

   | Gate | Realm | The challenge |
   |---|---|---|
   | Astral Gate | **The Firmament** – moonlit sky islands | low gravity, starwood groves to harvest |
   | Solar Gate | **The Sunspire** – eternal dawn | Solenne's 100-block climb on single pads; every gold pad must be reached, magma burns |
   | Lunar Gate | **The Moonwell** – full-moon midnight | Vael's ten moon targets, four on distant islets, while Eclipse Echoes keep rising |
   | Prism Gate | **The Starforge** – deep space | Orrin's three halls of mirrors; all three receptors must glow at once |
   | Archive Gate | **The Endless Archive** – starless dark | Ilyra's roofed maze, 70 × 70 blocks |
   | Umbral Gate | **The Umbral Cathedral** – the Eclipse's sky | opens only for one who has earned all four Watchers' Marks |

   Trial realms are sacred: no digging, no building, no ender pearls. Fall, and the sky sets you back on the shrine.

8. **The Watchers.** Solenne, Vael, Orrin and Ilyra – floating six-winged seraphs, three times your size – wait at the
   way into their trials. Each trial earns a **Mark** and the Watcher's favour, and cools its rival.
9. **The Eclipse.** In the Umbral Cathedral, an **Umbral Sigil** calls down the fallen fifth Watcher: three phases –
   *Fallen Light*, *Shattered Halo* (break its four pylons) and *Total Eclipse* – with telegraphed sweeps, lances,
   falling stars, a shadow dive, a sweeping ray and an Umbral Nova you survive only behind cover. Its heart becomes the
   **Halo of Dawn**; its feathers bind **Seraph Wings** to you.

## Compatibility

Optional integrations, none required: **JEI** and **EMI** (ritual recipes), **Jade** (starlight, rituals,
Watchers), **Patchouli** (Codex Caelestis), **KubeJS** and datapacks (all recipes are data-driven with `c:` and
`astralvigil:` tag inputs). Runs alongside Sodium, Iris, Curios and the Apotheosis family. Designed for All the Mods 10
(1.21.1) and All the Mods 11 (26.1).

## Configuration

- `astralvigil-server.toml` – every balance value: starfall, astrolabe, research, the starlight network, crafting,
  gear, Eclipse Gaze, rituals and the Eclipse fight. The reasoning is in [docs/BALANCE.md](docs/BALANCE.md).
- `astralvigil-client.toml` – particles, screen shake, beam quality.
- Worldgen is a datapack: override `data/astralvigil/worldgen/structure_set/fallen_observatory.json` to retune or
  disable observatories.
- Admin commands: `/astralvigil starfall [here]`, `/astralvigil research <alignment> <points>|reset`,
  `/astralvigil starlight <pos> <tint> <amount>`.

## Building

Requires Temurin JDK 21 and 25. One codebase for both Minecraft versions (Stonecutter + ModDevGradle).

```powershell
.\gradlew.bat build               # both targets + unit tests
.\gradlew.bat runGameTestServer   # all GameTests, both targets
.\gradlew.bat runData             # regenerate models, lang, recipes, loot, advancements, codex
```

Jars land in `versions/<target>/build/libs/`. Contributor notes: [CLAUDE.md](CLAUDE.md).

## License

All Rights Reserved for now – the final license is pending.

---

## Deutsch

*Die Wächter hüten das Firmament. Ein Riss im Himmel lässt Sternensplitter auf die Welt regnen – und die Finsternis
erwacht.*

**Fang fallende Sterne. Lenke Sternenlicht. Baue die Tore zum Himmel – und stelle dich den Prüfungen der Wächter.**

Astral Vigil ist ein Fortschritts-Mod für NeoForge rund um Sternenlicht. Er beginnt mit einem Meteor, der in einer
klaren Nacht in deine Welt stürzt, und endet hoch über den Wolken im **Firmament**: einer schwebenden
Welt aus sechs Himmelsreichen, die du nur durch selbst gebaute Tore erreichst. Behütet von vier
gewaltigen Wächtern – und heimgesucht von ihrem gefallenen fünften Geschwister, der Finsternis. Für Minecraft 1.21.1
und 26.1.2, vollständig auf Deutsch spielbar.

### Was dich erwartet

- **Der Nachthimmel zählt.** Sternenlicht hängt von Uhrzeit, Mondphase, Wetter und freiem Himmel ab. Sterne fallen
  wirklich: Meteore reißen Krater und hinterlassen Gefallene Sterne.
- **Sichtbares Licht.** Sammler, Linsen, Spiegel, Prismen und Speicher werfen sichtbare Strahlen durch deine Basis –
  und kosten dabei kaum Leistung.
- **Rituale mit Regeln.** Konvergenzrituale belohnen Symmetrie und ausgerichtete Blöcke. Wer schlampt, erlebt ein
  instabiles Ritual.
- **Ausrüstung, die mitwächst.** Sechs lebendige Ausrüstungsteile erwachen durch deine Taten, nehmen Siegel auf und
  entfesseln Fähigkeiten. Höchste Stufe ≈ Netherit, nie darüber.
- **Eine Dimension, die man sich verdient.** Sechs Torarten, jede von Hand gebaut und entzündet, jede mit eigenem
  Ziel: ein Aufstieg über hundert Blöcke, eine Bogenprüfung unter Beschuss, ein Rätsel aus Licht, ein lichtloses
  Bibliothekslabyrinth – und der Thron der Finsternis.
- **Für Modpacks gebaut.** Alle Balancewerte in der Serverkonfiguration, alle Rezepte mit `c:`-Tags, Weltgenerierung
  als Datapack; optional JEI, EMI, Jade, Patchouli und KubeJS.

### Die Tore

| Tor | Führt zu | Die Herausforderung |
|---|---|---|
| Astraltor | **Das Firmament** – mondhelle Himmelsinseln | geringe Schwerkraft, Sternholzhaine |
| Sonnentor | **Die Sonnenspitze** – ewige Morgenröte | Solennes Aufstieg über 100 Blöcke; jede goldene Stufe muss erreicht werden |
| Mondtor | **Der Mondbrunnen** – Vollmond um Mitternacht | Vaels zehn Mondzielscheiben, während Echos angreifen |
| Prismentor | **Die Sternenschmiede** – tiefes All | Orrins drei Spiegelhallen; alle drei Empfänger müssen zugleich leuchten |
| Archivtor | **Das Endlose Archiv** – sternloses Dunkel | Ilyras überdachtes Labyrinth, 70 × 70 Blöcke |
| Umbraltor | **Die Umbrale Kathedrale** – der Himmel der Finsternis | öffnet sich nur dem, der alle vier Zeichen der Wächter errungen hat |

In der Umbralen Kathedrale ruft ein **Umbrales Siegel** die Finsternis herab: ein Bosskampf in drei Phasen mit
Pylonenschild, Strahl und Nova, mit 36 Animationen und angekündigten Angriffen. Ihr Herz wird
zum **Heiligenschein der Morgenröte**, ihre Federn binden dir **Seraphsflügel**.

### Lizenz

Vorerst alle Rechte vorbehalten – die endgültige Lizenz steht noch aus.
