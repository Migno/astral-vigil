package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import media.hofferbert.astralvigil.starlight.StarlightMath;
import org.junit.jupiter.api.Test;

class StarlightMathTest {
    @Test
    void noStarlightByDay() {
        assertEquals(0.0, StarlightMath.intensity(6000, 0, true, false, false));
        assertEquals(0.0, StarlightMath.intensity(1000, 0, true, false, false));
        assertEquals(0.0, StarlightMath.intensity(StarlightMath.DUSK, 0, true, false, false));
        assertEquals(0.0, StarlightMath.intensity(StarlightMath.DAWN, 0, true, false, false));
    }

    @Test
    void peakAtMidnightUnderFullMoon() {
        assertEquals(1.0, StarlightMath.intensity(StarlightMath.MIDNIGHT, 0, true, false, false), 1e-9);
    }

    @Test
    void nightCurveRisesThenFalls() {
        double prev = -1;
        for (int t = StarlightMath.DUSK; t <= StarlightMath.MIDNIGHT; t += 250) {
            double v = StarlightMath.nightFactor(t);
            assertTrue(v >= prev, "not monotonic rising at " + t);
            prev = v;
        }
        for (int t = StarlightMath.MIDNIGHT; t <= StarlightMath.DAWN; t += 250) {
            double v = StarlightMath.nightFactor(t);
            assertTrue(v <= prev, "not monotonic falling at " + t);
            prev = v;
        }
    }

    @Test
    void dayTimeWrapsAround() {
        assertEquals(
                StarlightMath.nightFactor(StarlightMath.MIDNIGHT),
                StarlightMath.nightFactor(StarlightMath.MIDNIGHT + 24000L * 5));
    }

    @Test
    void moonPhases() {
        assertEquals(1.0, StarlightMath.moonFactor(0));
        assertEquals(0.5, StarlightMath.moonFactor(4));
        assertEquals(StarlightMath.moonFactor(2), StarlightMath.moonFactor(6));
        assertEquals(StarlightMath.moonFactor(1), StarlightMath.moonFactor(9)); // wraps
    }

    @Test
    void weatherAndSky() {
        double clear = StarlightMath.intensity(StarlightMath.MIDNIGHT, 0, true, false, false);
        assertEquals(clear * 0.5, StarlightMath.intensity(StarlightMath.MIDNIGHT, 0, true, true, false), 1e-9);
        assertEquals(clear * 0.25, StarlightMath.intensity(StarlightMath.MIDNIGHT, 0, true, true, true), 1e-9);
        assertEquals(0.0, StarlightMath.intensity(StarlightMath.MIDNIGHT, 0, false, false, false));
    }
}
