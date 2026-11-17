package media.hofferbert.astralvigil.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code astralvigil-server.toml} – every balance value lives here. Defaults are explained in docs/BALANCE.md. */
public final class AVServerConfig {
    public static final ModConfigSpec SPEC;

    // Starfall
    public static final ModConfigSpec.BooleanValue STARFALL_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STARFALL_DIMENSIONS;
    public static final ModConfigSpec.IntValue STARFALL_CHECK_INTERVAL;
    public static final ModConfigSpec.DoubleValue STARFALL_CHANCE;
    public static final ModConfigSpec.DoubleValue STARFALL_FULL_MOON_MULTIPLIER;
    public static final ModConfigSpec.IntValue STARFALL_PLAYER_COOLDOWN;
    public static final ModConfigSpec.IntValue STARFALL_MIN_DISTANCE;
    public static final ModConfigSpec.IntValue STARFALL_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue STARFALL_CRATER_RADIUS;
    public static final ModConfigSpec.BooleanValue STARFALL_TERRAIN_DAMAGE;
    public static final ModConfigSpec.IntValue STARFALL_ANNOUNCE_RADIUS;

    // Astrolabe
    public static final ModConfigSpec.IntValue ASTROLABE_COOLDOWN;
    public static final ModConfigSpec.IntValue ASTROLABE_TRACK_RANGE;

    // Research
    public static final ModConfigSpec.IntValue RESEARCH_ATTUNEMENT;
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_BLOCK;
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_ENTITY;
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_BIOME;
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_STRUCTURE;
    public static final ModConfigSpec.IntValue RESEARCH_SCAN_RANGE;

    // Starlight network
    public static final ModConfigSpec.IntValue COLLECTOR_RATE;
    public static final ModConfigSpec.IntValue COLLECTOR_CAPACITY;
    public static final ModConfigSpec.IntValue RELAY_CAPACITY;
    public static final ModConfigSpec.IntValue RESERVOIR_CAPACITY;
    public static final ModConfigSpec.IntValue BEAM_RANGE;
    public static final ModConfigSpec.IntValue BEAM_THROUGHPUT;
    public static final ModConfigSpec.DoubleValue MIRROR_LOSS;
    public static final ModConfigSpec.DoubleValue PRISM_LOSS;

    // Crafting
    public static final ModConfigSpec.IntValue ALTAR_CAPACITY;
    public static final ModConfigSpec.IntValue FORGE_CAPACITY;
    public static final ModConfigSpec.IntValue ALTAR_RADIUS;
    public static final ModConfigSpec.IntValue STABILITY_BASE;
    public static final ModConfigSpec.IntValue STABILITY_ASYMMETRY_PENALTY;
    public static final ModConfigSpec.IntValue STABILITY_ALIGNED_BONUS;
    public static final ModConfigSpec.IntValue STABILITY_ALIGNED_MAX;
    public static final ModConfigSpec.DoubleValue INSTABILITY_EVENT_FACTOR;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INSTABILITY_MOBS;

    // Living gear
    public static final ModConfigSpec.IntValue GEAR_CHARGE_CAPACITY;
    public static final ModConfigSpec.IntValue GEAR_CHARGE_RATE;
    public static final ModConfigSpec.DoubleValue ABILITY_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ABILITY_COOLDOWN_MULTIPLIER;

    // Eclipse Gaze + rituals
    public static final ModConfigSpec.BooleanValue GAZE_ENABLED;
    public static final ModConfigSpec.IntValue GAZE_MAX;
    public static final ModConfigSpec.DoubleValue GAZE_PER_ABILITY_COST;
    public static final ModConfigSpec.IntValue GAZE_PER_RITUAL;
    public static final ModConfigSpec.BooleanValue GAZE_CRYSTALLIZE_BLOCKS;
    public static final ModConfigSpec.DoubleValue GAZE_EVENT_CHANCE;
    public static final ModConfigSpec.IntValue RITUAL_STARLIGHT;
    public static final ModConfigSpec.IntValue RITUAL_GAZE_RELIEF;
    public static final ModConfigSpec.IntValue RITUAL_BASE_SECONDS;
    public static final ModConfigSpec.IntValue RITUAL_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Eclipse Gaze: heavy use of celestial power draws the Eclipse's attention.")
                .translation("astralvigil.configuration.gaze")
                .push("gaze");
        GAZE_ENABLED = b.comment("Enable Eclipse Gaze and its events.")
                .translation("astralvigil.configuration.gaze.enabled")
                .define("enabled", true);
        GAZE_MAX = b.comment("Maximum Gaze.")
                .translation("astralvigil.configuration.gaze.max")
                .defineInRange("max", 1000, 100, 100_000);
        GAZE_PER_ABILITY_COST = b.comment("Gaze gained per point of starlight charge spent on abilities.")
                .translation("astralvigil.configuration.gaze.per_ability_cost")
                .defineInRange("perAbilityCost", 0.25, 0.0, 100.0);
        GAZE_PER_RITUAL = b.comment("Gaze gained per Convergence ritual.")
                .translation("astralvigil.configuration.gaze.per_ritual")
                .defineInRange("perRitual", 15, 0, 10_000);
        GAZE_CRYSTALLIZE_BLOCKS = b.comment("At high Gaze, nearby stone may crystallise into Void Crystal (terrain change).")
                .translation("astralvigil.configuration.gaze.crystallize")
                .define("crystallizeBlocks", true);
        GAZE_EVENT_CHANCE = b.comment("Chance per 10 s check that a Gaze event happens (scaled by Gaze stage).")
                .translation("astralvigil.configuration.gaze.event_chance")
                .defineInRange("eventChance", 0.15, 0.0, 1.0);
        b.pop();

        b.comment("Star Anchor rituals: lay out a learned constellation with Star Anchors.")
                .translation("astralvigil.configuration.ritual")
                .push("ritual");
        RITUAL_STARLIGHT = b.comment("Starlight each anchor must hold (and spends) for a ritual.")
                .translation("astralvigil.configuration.ritual.starlight")
                .defineInRange("starlightPerAnchor", 100, 0, 100_000);
        RITUAL_GAZE_RELIEF = b.comment("Gaze removed by a ritual, multiplied by the layout scale.")
                .translation("astralvigil.configuration.ritual.gaze_relief")
                .defineInRange("gazeRelief", 120, 0, 100_000);
        RITUAL_BASE_SECONDS = b.comment("Blessing duration in seconds, multiplied by the layout scale.")
                .translation("astralvigil.configuration.ritual.base_seconds")
                .defineInRange("baseSeconds", 300, 1, 86_400);
        RITUAL_COOLDOWN = b.comment("Ticks before the same anchor can lead another ritual.")
                .translation("astralvigil.configuration.ritual.cooldown")
                .defineInRange("cooldownTicks", 6000, 0, 1_000_000);
        b.pop();

        b.comment("Living gear: starlight charge, sigil abilities.")
                .translation("astralvigil.configuration.gear")
                .push("gear");
        GEAR_CHARGE_CAPACITY = b.comment("Starlight charge a piece of gear can hold.")
                .translation("astralvigil.configuration.gear.charge_capacity")
                .defineInRange("chargeCapacity", 200, 1, 100_000);
        GEAR_CHARGE_RATE = b.comment("Charge regained every 2 seconds at 100% starlight (held/worn gear only).")
                .translation("astralvigil.configuration.gear.charge_rate")
                .defineInRange("chargeRate", 4, 0, 10_000);
        ABILITY_COST_MULTIPLIER = b.comment("Multiplier for every ability's starlight cost.")
                .translation("astralvigil.configuration.gear.cost_multiplier")
                .defineInRange("abilityCostMultiplier", 1.0, 0.0, 100.0);
        ABILITY_COOLDOWN_MULTIPLIER = b.comment("Multiplier for every ability's cooldown.")
                .translation("astralvigil.configuration.gear.cooldown_multiplier")
                .defineInRange("abilityCooldownMultiplier", 1.0, 0.0, 100.0);
        b.pop();

        b.comment("Convergence Altar and Astral Forge.")
                .translation("astralvigil.configuration.crafting")
                .push("crafting");
        ALTAR_CAPACITY = b.comment("Starlight the Convergence Altar can hold.")
                .translation("astralvigil.configuration.crafting.altar_capacity")
                .defineInRange("altarCapacity", 4000, 1, 10_000_000);
        FORGE_CAPACITY = b.comment("Starlight the Astral Forge can hold.")
                .translation("astralvigil.configuration.crafting.forge_capacity")
                .defineInRange("forgeCapacity", 2000, 1, 10_000_000);
        ALTAR_RADIUS = b.comment("Horizontal radius in which pedestals belong to an altar.")
                .translation("astralvigil.configuration.crafting.altar_radius")
                .defineInRange("altarRadius", 4, 1, 8);
        STABILITY_BASE = b.comment("Stability of a perfectly symmetric altar before alignment bonuses (100 = no instability).")
                .translation("astralvigil.configuration.crafting.stability_base")
                .defineInRange("stabilityBase", 80, 0, 100);
        STABILITY_ASYMMETRY_PENALTY = b.comment("Stability lost per pedestal without a mirrored partner.")
                .translation("astralvigil.configuration.crafting.asymmetry_penalty")
                .defineInRange("asymmetryPenalty", 15, 0, 100);
        STABILITY_ALIGNED_BONUS = b.comment("Stability gained per nearby block of the recipe's alignment (Void blocks subtract it).")
                .translation("astralvigil.configuration.crafting.aligned_bonus")
                .defineInRange("alignedBonus", 3, 0, 100);
        STABILITY_ALIGNED_MAX = b.comment("Maximum stability gained from aligned blocks.")
                .translation("astralvigil.configuration.crafting.aligned_max")
                .defineInRange("alignedMax", 30, 0, 100);
        INSTABILITY_EVENT_FACTOR = b.comment("Chance per second of an instability event = (100 - stability) / 100 * factor.")
                .translation("astralvigil.configuration.crafting.instability_factor")
                .defineInRange("instabilityFactor", 0.25, 0.0, 1.0);
        INSTABILITY_MOBS = b.comment("Creatures that may tear through an unstable ritual.")
                .translation("astralvigil.configuration.crafting.instability_mobs")
                .defineListAllowEmpty("instabilityMobs", List.of("minecraft:vex"), () -> "minecraft:vex", o -> o instanceof String);
        b.pop();

        b.comment("Starlight network: collectors, lenses, mirrors, prisms and reservoirs.")
                .translation("astralvigil.configuration.starlight")
                .push("starlight");
        COLLECTOR_RATE = b.comment("Starlight a collector gathers per second at 100% starlight (midnight, full moon, clear sky).")
                .translation("astralvigil.configuration.starlight.collector_rate")
                .defineInRange("collectorRate", 8, 0, 10000);
        COLLECTOR_CAPACITY = b.comment("Internal buffer of a collector.")
                .translation("astralvigil.configuration.starlight.collector_capacity")
                .defineInRange("collectorCapacity", 400, 1, 1_000_000);
        RELAY_CAPACITY = b.comment("Internal buffer of lenses, mirrors and prisms.")
                .translation("astralvigil.configuration.starlight.relay_capacity")
                .defineInRange("relayCapacity", 200, 1, 1_000_000);
        RESERVOIR_CAPACITY = b.comment("Capacity of a Starlight Reservoir.")
                .translation("astralvigil.configuration.starlight.reservoir_capacity")
                .defineInRange("reservoirCapacity", 20000, 1, 100_000_000);
        BEAM_RANGE = b.comment("Maximum beam length in blocks.")
                .translation("astralvigil.configuration.starlight.beam_range")
                .defineInRange("beamRange", 16, 2, 64);
        BEAM_THROUGHPUT = b.comment("Starlight a single beam carries per second.")
                .translation("astralvigil.configuration.starlight.beam_throughput")
                .defineInRange("beamThroughput", 64, 1, 1_000_000);
        MIRROR_LOSS = b.comment("Fraction of starlight lost when a mirror turns a beam.")
                .translation("astralvigil.configuration.starlight.mirror_loss")
                .defineInRange("mirrorLoss", 0.05, 0.0, 1.0);
        PRISM_LOSS = b.comment("Fraction of starlight lost when a prism splits a beam.")
                .translation("astralvigil.configuration.starlight.prism_loss")
                .defineInRange("prismLoss", 0.10, 0.0, 1.0);
        b.pop();

        b.comment("Research: scanning blocks, creatures, biomes and structures with the Astrolabe.")
                .translation("astralvigil.configuration.research")
                .push("research");
        RESEARCH_ATTUNEMENT = b.comment("Knowledge points needed to become attuned to an alignment.")
                .translation("astralvigil.configuration.research.attunement")
                .defineInRange("attunementThreshold", 12, 1, 1000);
        RESEARCH_POINTS_BLOCK = b.comment("Knowledge for the first scan of a block type.")
                .translation("astralvigil.configuration.research.points_block")
                .defineInRange("pointsBlock", 1, 0, 100);
        RESEARCH_POINTS_ENTITY = b.comment("Knowledge for the first scan of a creature type.")
                .translation("astralvigil.configuration.research.points_entity")
                .defineInRange("pointsEntity", 2, 0, 100);
        RESEARCH_POINTS_BIOME = b.comment("Knowledge for the first scan of a biome (under the open sky).")
                .translation("astralvigil.configuration.research.points_biome")
                .defineInRange("pointsBiome", 2, 0, 100);
        RESEARCH_POINTS_STRUCTURE = b.comment("Knowledge for the first scan inside a structure.")
                .translation("astralvigil.configuration.research.points_structure")
                .defineInRange("pointsStructure", 4, 0, 100);
        RESEARCH_SCAN_RANGE = b.comment("Reach of the Astrolabe when scanning blocks.")
                .translation("astralvigil.configuration.research.scan_range")
                .defineInRange("scanRange", 16, 4, 64);
        b.pop();

        b.comment("Starfall: rare night-time meteor events that leave a small crater with a Fallen Star.")
                .translation("astralvigil.configuration.starfall")
                .push("starfall");
        STARFALL_ENABLED = b.comment("Enable starfall events.")
                .translation("astralvigil.configuration.starfall.enabled")
                .define("enabled", true);
        STARFALL_DIMENSIONS = b.comment("Dimensions in which stars may fall.")
                .translation("astralvigil.configuration.starfall.dimensions")
                .defineListAllowEmpty(
                        "dimensions", List.of("minecraft:overworld"), () -> "minecraft:overworld", o -> o instanceof String);
        STARFALL_CHECK_INTERVAL = b.comment("Ticks between starfall rolls (per player, only at night).")
                .translation("astralvigil.configuration.starfall.check_interval")
                .defineInRange("checkIntervalTicks", 1200, 100, 72000);
        STARFALL_CHANCE = b.comment("Chance per roll that a star falls near a player.")
                .translation("astralvigil.configuration.starfall.chance")
                .defineInRange("chancePerRoll", 0.035, 0.0, 1.0);
        STARFALL_FULL_MOON_MULTIPLIER = b.comment("Chance multiplier during a full moon.")
                .translation("astralvigil.configuration.starfall.full_moon_multiplier")
                .defineInRange("fullMoonMultiplier", 2.0, 1.0, 10.0);
        STARFALL_PLAYER_COOLDOWN = b.comment("Minimum ticks between two starfalls near the same player.")
                .translation("astralvigil.configuration.starfall.player_cooldown")
                .defineInRange("playerCooldownTicks", 24000, 0, 1_000_000);
        STARFALL_MIN_DISTANCE = b.comment("Minimum horizontal distance of the impact from the player.")
                .translation("astralvigil.configuration.starfall.min_distance")
                .defineInRange("minDistance", 24, 4, 256);
        STARFALL_MAX_DISTANCE = b.comment("Maximum horizontal distance of the impact from the player.")
                .translation("astralvigil.configuration.starfall.max_distance")
                .defineInRange("maxDistance", 64, 8, 512);
        STARFALL_CRATER_RADIUS = b.comment("Crater radius in blocks.")
                .translation("astralvigil.configuration.starfall.crater_radius")
                .defineInRange("craterRadius", 3, 1, 6);
        STARFALL_TERRAIN_DAMAGE = b.comment("If false, no crater is dug - the Fallen Star simply lands on the surface.")
                .translation("astralvigil.configuration.starfall.terrain_damage")
                .define("terrainDamage", true);
        STARFALL_ANNOUNCE_RADIUS = b.comment("Players within this radius are told where the star fell.")
                .translation("astralvigil.configuration.starfall.announce_radius")
                .defineInRange("announceRadius", 160, 0, 1024);
        b.pop();

        b.comment("Astrolabe: reads starlight and tracks fallen stars.")
                .translation("astralvigil.configuration.astrolabe")
                .push("astrolabe");
        ASTROLABE_COOLDOWN = b.comment("Cooldown in ticks after a reading.")
                .translation("astralvigil.configuration.astrolabe.cooldown")
                .defineInRange("cooldownTicks", 20, 0, 1200);
        ASTROLABE_TRACK_RANGE = b.comment("Range in blocks within which recent fallen stars are tracked.")
                .translation("astralvigil.configuration.astrolabe.track_range")
                .defineInRange("trackRange", 512, 16, 4096);
        b.pop();

        SPEC = b.build();
    }

    private AVServerConfig() {}
}
