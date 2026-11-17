package media.hofferbert.astralvigil.firmament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import media.hofferbert.astralvigil.eclipse.EclipseBrain;
import media.hofferbert.astralvigil.eclipse.EclipseMove;
import media.hofferbert.astralvigil.eclipse.EclipseMove.Stage;
import media.hofferbert.astralvigil.eclipse.EclipsePhase;
import org.junit.jupiter.api.Test;

class EclipseBrainTest {
    private static int[] noCooldowns() {
        return new int[EclipseMove.values().length];
    }

    private static Set<EclipseMove> reachable(int phase, boolean shielded, double distance) {
        Set<EclipseMove> seen = EnumSet.noneOf(EclipseMove.class);
        Random r = new Random(7);
        for (int i = 0; i < 2000; i++) {
            seen.add(EclipseBrain.pick(phase, shielded, distance, noCooldowns(), EclipseMove.NONE, r::nextInt));
        }
        return seen;
    }

    @Test
    void phasesUnlockMoves() {
        assertEquals(EnumSet.of(EclipseMove.SWEEP, EclipseMove.LANCE, EclipseMove.STARFALL), reachable(1, false, 4));
        Set<EclipseMove> p2 = reachable(2, false, 4);
        assertTrue(p2.containsAll(EnumSet.of(EclipseMove.SUMMON, EclipseMove.DIVE)));
        assertFalse(p2.contains(EclipseMove.NOVA) || p2.contains(EclipseMove.BEAM) || p2.contains(EclipseMove.BLINK));
        assertTrue(reachable(3, false, 4).containsAll(EnumSet.of(EclipseMove.NOVA, EclipseMove.BEAM, EclipseMove.BLINK)));
    }

    @Test
    void neverPicksSpecialsOrGrasp() {
        for (int phase = 1; phase <= 3; phase++) {
            for (EclipseMove m : reachable(phase, false, 4)) {
                assertEquals(EclipseMove.Kind.ATTACK, m.kind(), m.name());
            }
        }
    }

    @Test
    void shieldedOnlyCastsFromAfar() {
        for (EclipseMove m : reachable(2, true, 4)) {
            assertTrue(m.allowedWhileShielded(), m.name());
        }
        assertFalse(reachable(2, true, 4).contains(EclipseMove.SWEEP));
    }

    @Test
    void sweepNeedsRange() {
        assertFalse(reachable(1, false, 20).contains(EclipseMove.SWEEP));
    }

    @Test
    void noRepeatsAndRespectsCooldowns() {
        Random r = new Random(3);
        int[] cd = noCooldowns();
        cd[EclipseMove.LANCE.ordinal()] = 10;
        EclipseMove last = EclipseMove.SWEEP;
        for (int i = 0; i < 500; i++) {
            EclipseMove m = EclipseBrain.pick(1, false, 4, cd, last, r::nextInt);
            assertNotEquals(EclipseMove.LANCE, m);
            assertNotEquals(last, m);
            last = m;
            if (m == EclipseMove.STARFALL) {
                last = EclipseMove.SWEEP; // keep alternating between the only two options
            }
        }
    }

    @Test
    void nothingFitsReturnsNone() {
        int[] cd = noCooldowns();
        java.util.Arrays.fill(cd, 100);
        assertEquals(EclipseMove.NONE, EclipseBrain.pick(3, false, 4, cd, EclipseMove.NONE, n -> 0));
    }

    @Test
    void timerRunsThroughStagesInOrder() {
        EclipseBrain.Timer t = new EclipseBrain.Timer();
        t.start(EclipseMove.LANCE);
        List<Stage> stages = new ArrayList<>();
        stages.add(t.stage());
        int ticks = 0;
        while (t.running()) {
            if (t.tick()) {
                stages.add(t.stage());
            }
            ticks++;
        }
        assertEquals(List.of(Stage.WINDUP, Stage.ACTIVE, Stage.RECOVERY, Stage.NONE), stages);
        assertEquals(EclipseMove.LANCE.totalTicks(), ticks);
        assertEquals(EclipseMove.NONE, t.move());
    }

    @Test
    void specialsSkipEmptyStages() {
        EclipseBrain.Timer t = new EclipseBrain.Timer();
        t.start(EclipseMove.STAGGER);
        assertEquals(Stage.ACTIVE, t.stage());
        assertEquals(80, t.left());
    }

    @Test
    void burstDamageCannotSkipAPhase() {
        float max = 600;
        float floor1 = EclipseBrain.healthFloor(1, max);
        assertEquals(2, EclipsePhase.of(floor1, max));
        float floor2 = EclipseBrain.healthFloor(2, max);
        assertEquals(3, EclipsePhase.of(floor2, max));
        assertTrue(floor2 > 0);
        assertEquals(0f, EclipseBrain.healthFloor(3, max));
    }

    @Test
    void pacing() {
        assertTrue(EclipseBrain.gap(24, 3, true, 0.7) < EclipseBrain.gap(24, 1, false, 0.7));
        assertTrue(EclipseBrain.gap(1, 3, true, 0.2) >= 6);
        assertTrue(EclipseBrain.shouldReset(600, 600));
        assertFalse(EclipseBrain.shouldReset(599, 600));
    }

    /** The generator's animation lengths must match the enum's stage ticks, and every stage must have an animation. */
    @Test
    void animationsMatchMoves() throws IOException {
        JsonObject anims = JsonParser.parseString(Files.readString(animationFile())).getAsJsonObject().getAsJsonObject("animations");
        for (EclipseMove m : EclipseMove.values()) {
            for (Stage s : Stage.values()) {
                if (m == EclipseMove.NONE || s == Stage.NONE || m.ticks(s) <= 0) {
                    continue;
                }
                String name = m.animation(s);
                assertTrue(anims.has(name), "missing animation " + name);
                if (!EclipseMove.loops(name)) {
                    double length = anims.getAsJsonObject(name).get("animation_length").getAsDouble();
                    assertEquals(m.ticks(s) / 20.0, length, 0.01, name);
                }
            }
        }
        for (String n : new String[] {"idle", "idle_shielded", "idle_total", "idle_enraged", "death"}) {
            assertTrue(anims.has(n), "missing animation " + n);
        }
    }

    private static Path animationFile() {
        Path p = Path.of(System.getProperty("av.generatedResources", "src/generated/resources")).toAbsolutePath();
        for (; p != null; p = p.getParent()) {
            Path f = p.resolve("src/main/resources/assets/astralvigil/animations/entity/eclipse.animation.json");
            if (Files.exists(f)) {
                return f;
            }
        }
        return Path.of("src/main/resources/assets/astralvigil/animations/entity/eclipse.animation.json");
    }
}
