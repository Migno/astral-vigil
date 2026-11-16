package media.hofferbert.astralvigil.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code astralvigil-client.toml} – purely visual settings. */
public final class AVClientConfig {
    public enum BeamQuality {
        LOW,
        MEDIUM,
        HIGH
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.EnumValue<BeamQuality> BEAM_QUALITY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        PARTICLE_DENSITY = b.comment("Particle density multiplier (0 = off).")
                .translation("astralvigil.configuration.particle_density")
                .defineInRange("particleDensity", 1.0, 0.0, 2.0);
        SCREEN_SHAKE = b.comment("Screen shake on impacts and boss attacks.")
                .translation("astralvigil.configuration.screen_shake")
                .define("screenShake", true);
        BEAM_QUALITY = b.comment("Render quality of starlight beams.")
                .translation("astralvigil.configuration.beam_quality")
                .defineEnum("beamQuality", BeamQuality.HIGH);
        SPEC = b.build();
    }

    private AVClientConfig() {}

    /** Safe to call before the config is loaded (returns the default). */
    public static double particleFactor() {
        return SPEC.isLoaded() ? PARTICLE_DENSITY.get() : 1.0;
    }
}
