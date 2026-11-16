package media.hofferbert.astralvigil.starlight;

/**
 * Pure starlight formulas (no Minecraft classes, unit-tested).
 *
 * <p>Starlight only exists under the open sky. It rises after dusk, peaks at midnight and is stronger the fuller the
 * moon is. Rain halves it, thunderstorms quarter it.
 */
public final class StarlightMath {
    public static final int DAY_LENGTH = 24000;
    /** Day time at which starlight starts to appear (vanilla sunset is about 12000). */
    public static final int DUSK = 12500;
    /** Day time of full darkness (midnight). */
    public static final int MIDNIGHT = 18000;
    /** Day time at which starlight has faded (vanilla sunrise is about 23000). */
    public static final int DAWN = 23500;

    private StarlightMath() {}

    /** Time-of-day factor in [0,1]: 0 by day, smooth rise to 1 at midnight and back to 0 at dawn. */
    public static double nightFactor(long dayTime) {
        int t = (int) Math.floorMod(dayTime, (long) DAY_LENGTH);
        if (t <= DUSK || t >= DAWN) {
            return 0.0;
        }
        double x = t < MIDNIGHT ? (double) (t - DUSK) / (MIDNIGHT - DUSK) : (double) (DAWN - t) / (DAWN - MIDNIGHT);
        return x * x * (3 - 2 * x); // smoothstep
    }

    /** Moon factor in [0.5,1]: full moon (phase 0) = 1, new moon (phase 4) = 0.5. */
    public static double moonFactor(int moonPhase) {
        int p = Math.floorMod(moonPhase, 8);
        int distanceFromNew = Math.abs(p - 4); // 0 (new) .. 4 (full)
        return 0.5 + 0.5 * (distanceFromNew / 4.0);
    }

    public static double weatherFactor(boolean raining, boolean thundering) {
        if (thundering) {
            return 0.25;
        }
        return raining ? 0.5 : 1.0;
    }

    /** Starlight intensity in [0,1]. */
    public static double intensity(long dayTime, int moonPhase, boolean canSeeSky, boolean raining, boolean thundering) {
        if (!canSeeSky) {
            return 0.0;
        }
        return nightFactor(dayTime) * moonFactor(moonPhase) * weatherFactor(raining, thundering);
    }

    public static boolean isNight(long dayTime) {
        return nightFactor(dayTime) > 0.0;
    }
}
