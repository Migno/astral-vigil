package media.hofferbert.astralvigil.compat;

import media.hofferbert.astralvigil.starlight.StarlightMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
//?}

/** Version adapter for time of day / moon phase (26.1 moved both to world clocks + environment attributes). */
public final class Celestial {
    private Celestial() {}

    /** Day time in [0, 24000). */
    public static long dayTime(Level level) {
        //? if >=26.1 {
        return Math.floorMod(level.getOverworldClockTime(), (long) StarlightMath.DAY_LENGTH);
        //?} else {
        /*return Math.floorMod(level.getDayTime(), (long) StarlightMath.DAY_LENGTH);
        *///?}
    }

    /** Moon phase 0 (full) .. 7, 4 = new moon. */
    public static int moonPhase(Level level, BlockPos pos) {
        //? if >=26.1 {
        return level.environmentAttributes()
                .getValue(EnvironmentAttributes.MOON_PHASE, Vec3.atCenterOf(pos))
                .index();
        //?} else {
        /*return level.getMoonPhase();
        *///?}
    }

    public static boolean hasSkyCycle(Level level) {
        return level.dimensionType().hasSkyLight()
                && !level.dimensionType().hasCeiling()
                && !level.dimensionType().hasFixedTime();
    }

    public static double starlight(Level level, BlockPos pos) {
        if (!hasSkyCycle(level)) {
            return 0.0;
        }
        return StarlightMath.intensity(
                dayTime(level),
                moonPhase(level, pos),
                level.canSeeSky(pos.above()),
                level.isRaining(),
                level.isThundering());
    }
}
