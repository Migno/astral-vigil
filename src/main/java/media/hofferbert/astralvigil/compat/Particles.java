package media.hofferbert.astralvigil.compat;

import net.minecraft.core.particles.DustParticleOptions;

/** Dust particles take an int colour on 26.1 and a Vector3f on 1.21.1. */
public final class Particles {
    private Particles() {}

    public static DustParticleOptions dust(int rgb, float scale) {
        //? if >=26.1 {
        return new DustParticleOptions(rgb, scale);
        //?} else {
        /*return new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(rgb).toVector3f(), scale);
        *///?}
    }
}
