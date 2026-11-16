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
| `chancePerRoll` | 0.035 | A Minecraft night has ~8 rolls → ≈ 25 % chance per night per player, ≈ 45 % under a full moon. A star every 2–4 nights feels like an event, not a farm. |
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

## Starlight formula

`intensity = night(t) × moon(phase) × weather`, 0 without sky access.
- `night`: smoothstep from dusk (12 500) to midnight (18 000) and back to dawn (23 500).
- `moon`: full moon 1.0 … new moon 0.5 (linear).
- `weather`: clear 1.0, rain 0.5, thunder 0.25.
