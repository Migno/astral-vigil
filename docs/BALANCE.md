# Balance

All values are server-config driven (`config/astralvigil-server.toml`). This document explains the defaults.

Guard rails:
- Top tier ≈ Netherite; at most slightly above through utility, never Allthemodium level.
- Abilities cost Starlight **and** have a cooldown – no passive permanent buffs.
- Starfall is rare and cannot be automated into a farm.

## Starfall (`[starfall]`)

| Key | Default | Why |
|---|---|---|
| `checkIntervalTicks` | 1200 (1 min) | Cheap: one roll per player per minute, only at night. |
| `chancePerRoll` | 0.06 | ~8 rolls a night → ≈ 40 % per night per player, ≈ 65 % under a full moon. Raised from 0.035 after the game-loop audit: gear, key and six gate kinds need about a star every other night. |
| `fullMoonMultiplier` | 2.0 | Makes the full moon (1 night in 8) noticeably special. |
| `playerCooldownTicks` | 24000 (1 day) | Hard cap: at most one star per player per day, even when lucky. AFK farms gain nothing. |
| `minDistance` / `maxDistance` | 24 / 64 | Visible in the sky, close enough to walk to, far enough not to hit the player's base by design. |
| `craterRadius` | 3 | Small bowl (≈ 7×7×3), meteoric stone lining; never touches block entities, fluids, unbreakable or `#astralvigil:starfall_immune` blocks. |
| `terrainDamage` | true | Servers that dislike terrain changes set `false` – the star then just lands on the surface. |

**Fallen Star loot:** 2–4 Astral Shards (+ Fortune). Requires an iron pickaxe. Silk Touch keeps the block
(decorative light source) – mining it afterwards still yields shards only once, so shards stay bound to starfall
events and cannot be automated.

## Astrolabe (`[astrolabe]`)

| Key | Default | Why |
|---|---|---|
| `cooldownTicks` | 20 | Prevents chat spam, no gameplay impact. |
| `trackRange` | 512 | Covers the starfall announce radius with margin. |

**Recipe:** 4× `#c:ingots/gold` + 1× `#astralvigil:astral_shards` + 1× `#c:ingots/copper` – first use of the shard, cheap
on purpose (it is an exploration tool).

## Research (`[research]`)

| Key | Default | Why |
|---|---|---|
| `attunementThreshold` | 12 | Roughly 4–8 new discoveries of one alignment (e.g. 3 biomes + 2 creatures + a few blocks) – an evening of exploring, not a grind. |
| `pointsBlock` | 1 | Blocks are plentiful and found without travelling. |
| `pointsEntity` | 2 | Creatures require finding and approaching. |
| `pointsBiome` | 2 | Requires travel and open sky. |
| `pointsStructure` | 4 | Rare, often dangerous. |
| `scanRange` | 16 | Reserved for ranged scanning; current scans use vanilla reach. |

Only the **first** scan of each subject counts, so research cannot be farmed. Quill (Ilyra) needs two attunements
(Luna + Stella) – the chronicler's sign is the reward for broad research.

## Starlight network (`[starlight]`)

| Key | Default | Why |
|---|---|---|
| `collectorRate` | 8 /s at 100 % | A full-moon midnight collector fills a reservoir (20 000) in ~40 min; a typical night with average moon yields ~1 500–2 500 per collector. Several collectors and patience are the progression, not a single block. |
| `collectorCapacity` | 400 | Smooths out short interruptions. |
| `relayCapacity` | 200 | Lenses/mirrors/prisms are pipes, not storage. |
| `reservoirCapacity` | 20 000 | Roughly ten mid-tier altar crafts. |
| `beamRange` | 16 | Forces relays for long distances – building the beam network is part of the fun. |
| `beamThroughput` | 64 /s | Well above collector output; only matters when emptying reservoirs. |
| `mirrorLoss` | 5 % | Turning light costs a little. |
| `prismLoss` | 10 % | Splitting is the expensive step; aligned light is worth more. |

Devices work once per second (not every tick), so even large networks stay cheap.

## Crafting (`[crafting]`)

| Key | Default | Why |
|---|---|---|
| `altarCapacity` / `forgeCapacity` | 4000 / 2000 | Enough for the most expensive current recipe plus headroom. |
| `altarRadius` | 4 | Room for an 8-pedestal ring with walkways. |
| `stabilityBase` | 80 | A symmetric altar alone is *not* perfectly safe – decorating with aligned blocks is part of the ritual. |
| `asymmetryPenalty` | 15 | Two unpaired pedestals already make a ritual risky. |
| `alignedBonus` / `alignedMax` | 3 / 30 | ~7 aligned blocks make a ritual fully stable; Void blocks subtract the same. |
| `instabilityFactor` | 0.25 | At 50 % stability ≈ one event every 8 s. |
| `instabilityMobs` | `minecraft:vex` | Placeholder until the Hollow creature (Phase 7). |

| Recipe | Inputs | Starlight | Time |
|---|---|---|---|
| Sunsteel Ingot (forge) | gold ingot + 2 astral shards | 120 Sol | 5 s |
| Moonsilver Ingot (forge) | iron ingot + astral shard + quartz | 120 Luna | 5 s |
| Starmetal Ingot (forge) | iron ingot + 2 astral shards | 120 Stella | 5 s |
| Sigil Blank ×2 (altar) | starmetal + 2 gold nuggets + 2 amethyst | 400 Stella | 6 s |
| Astral Core (altar) | diamond + starmetal + sunsteel + moonsilver + shard | 1000 pure | 10 s |

Ingot cost ≈ one collector-night of aligned light – metals are a mid-game step, not a grind wall.

## Living gear (`[gear]`)

| Item | Stats | Netherite reference |
|---|---|---|
| Halo Edge | 8 dmg, 1.6 speed, 1800 uses | 8 / 1.6 / 2031 |
| Dawnbreaker | 10 dmg, 1.0 speed, 1800 uses | 10 / 1.0 / 2031 |
| Crescent / Tidecaller | vanilla draw, 640 / 700 uses; arrows +15 % at night, +10 % per level | bow 384 / crossbow 465 uses |
| Starlight Aegis | vanilla shield blocking, 700 uses | 336 |
| Seraph Plate | 3/8/6/3 armour, 3 toughness, 0.1 knockback res., durability factor 35 | 3/8/6/3, 3, 0.1, 37 |

Living-gear bonuses are utility: +15 % damage vs undead per level (Sol blades), glide while sneaking (Seraph,
Awakened), no fall damage (Seraph, Ascended). Deeds: 25 → Awakened, 100 → Ascended.

| Key | Default | Why |
|---|---|---|
| `chargeCapacity` | 200 | 4–10 ability uses. |
| `chargeRate` | 4 per 2 s at 100 % starlight | A clear night refills a piece in ~2 min of real time outdoors; indoors/daytime needs a reservoir. |
| `abilityCostMultiplier` / `abilityCooldownMultiplier` | 1.0 | Pack-level tuning knobs. |

| Sigil | Cost | Cooldown | Effect |
|---|---|---|---|
| Whirl | 30 | 6 s | AoE hit (60 % + 10 %/level of attack damage) in 3.5 blocks |
| Shieldbreaker | 20 | 8 s | Next melee hit +40 % |
| Dawn | 40 | 12 s | Undead in 6 blocks burn + 4 (+2/level) magic damage; others glow |
| Flash | 25 | 4 s | Dash ~6 blocks, fall reset |
| Precision | 20 | 6 s | Next arrow +60 % and glowing |
| Undertow | 25 | 8 s | Next arrow pulls the target |
| Aegis | 40 | 20 s | Absorption II–III 10 s + Resistance I 5 s |

## Eclipse Gaze (`[gaze]`) and rituals (`[ritual]`)

| Key | Default | Why |
|---|---|---|
| `perAbilityCost` | 0.25 | A 40-charge ability adds 10 Gaze – heavy ability use reaches stage 1 after ~20 casts. |
| `perRitual` | 15 | Convergence is powerful; a dozen rituals are noticed. |
| stages | 200 / 400 / 600 / 800 | whispers → echoes (night) → crystallisation → surges |
| `eventChance` | 0.15 × stage per 10 s | Stage 4 ≈ one event every ~17 s – pressure to perform a ritual. |
| `starlightPerAnchor` | 100 | A six-anchor ritual costs 600 light – one good night of one collector. |
| `gazeRelief` | 120 × scale | A scale-3 ritual clears one full stage and more. |
| `baseSeconds` | 300 × scale | 5/10/15 minute blessings – timed, never permanent. |
| `cooldownTicks` | 6000 | Five minutes per anchor; prevents blessing chains. |

Blessings: Halo – Fire Resistance + Regeneration; Crescent – Night Vision + Speed; Anvil – Haste + Resistance;
Quill – Luck + Hero of the Village.

Fallen Observatories: random spread, spacing 42 / separation 14 chunks (rarer than villages). Vault loot: ingots,
sigil blank, 10 % Astral Core; lore fragments 1–2 (open chest) and 2–3 (vault).

## Firmament (Phase 7)

Ascension Key: 2000 pure starlight + Starmetal, Sunsteel, Moonsilver, Void Shard, 2 feathers around an Astral Core,
Stella research – late-midgame, after the first gear. Trials: 10 min to finish once started. Favour: +50 for a
completed trial, −15 for the rival Watcher, clamped to −100 … 1000 (used by Phase 8). Low gravity: −50 % gravity,
+7 safe fall distance – generous so the parkour trial is about route-finding, not fall damage.

## Endgame (Phase 8)

| | Eclipse | Wither (ref.) | Warden (ref.) |
|---|---|---|---|
| Health / armour | 400 / 10 | 300 / 4 | 500 / 0 |
| Melee | 12, 1.5 s | – | 30 |
| Ranged | Void Lance 14 + Wither I (telegraphed 1 s) | skulls 8 + Wither | sonic boom 10 |
| Area | Umbral Nova 18 (phase 3, blocked by cover) | explosion | – |

Tuned for a group or a single player in full Seraph Plate (≈ netherite) with Sunsteel/Moonsilver gear: every heavy hit
is telegraphed and avoidable, so a skilled solo player can win without potions. Echoes: 14 HP, 4 damage, max 6 alive,
waves of 3 every 11 s from phase 2. Rewards: Eclipse Heart + 3–5 Seraph Feathers per kill; the Umbral Sigil (3000
Stella light, Void materials, echo shard) keeps repeat kills a real investment. Halo of Dawn: −25 % ability cost,
−50 % Gaze gain – convenience, never extra damage.

## Starlight formula

`intensity = night(t) × moon(phase) × weather`, 0 without sky access.
- `night`: smoothstep from dusk (12 500) to midnight (18 000) and back to dawn (23 500).
- `moon`: full moon 1.0 … new moon 0.5 (linear).
- `weather`: clear 1.0, rain 0.5, thunder 0.25.

## Eclipse (`[eclipse]`, reworked in phase 10)

| Key | Default | Why |
|---|---|---|
| `health` / `healthPerExtraPlayer` | 500 / +50 % | The fight now has a shield phase and pauses; 500 keeps it at roughly 4–6 minutes for one netherite-tier player. |
| `armor` | 10 | Top-tier weapons still bite. |
| damage keys | Sweep 14, Lance 14 + Wither, Stars 12, Dive 16 + shockwave 8, Ray 10, Nova 18, Grasp 6 | Every big hit is telegraphed and avoidable; full Seraph Plate survives two mistakes, not three. |
| `enrageThreshold` | 0.1 | ×1.25 damage, ×0.7 tempo; wind-ups are never shortened. |
| `vulnerableDamageMultiplier` | 1.3 | Grounded and stagger windows reward melee. |
| `pylonCount` / `pylonHealth` / `pylonHealPerSecond` | 4 / 40 / 1.0 | Ignoring the pylons costs health, but the phase never turns into a slog. |
| `leashRadius` / `resetRange` / `resetSeconds` | 40 / 64 / 30 | It never drifts over the void; abandoned fights reset fully. |
| feathers | 3–5 per participant | Everyone who fought gets their own reward. |

## Armour sets (`[armor]`)

| Set | Defence | Bonus | Why |
|---|---|---|---|
| Solar Regalia | 18, toughness 2 | Fire Resistance; Strength I by day (`solarStrength`) | Below diamond (20) to pay for the bonus. |
| Lunar Raiment | 17, toughness 1.5 | Night Vision and Speed I at night; −50 % fall damage | A mobility set for night hunters. |
| Seraph Plate | ≈ netherite | Starlight Mending (2 charge per 1 HP every 2 s under an open night sky), Glide Burst (25 charge, 8 s cooldown), no fall damage when ascended | Net-positive only under a clear night sky. |
