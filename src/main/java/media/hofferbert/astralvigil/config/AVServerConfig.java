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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

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
