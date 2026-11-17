package media.hofferbert.astralvigil.firmament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GateShapeTest {
    /** Frame of a w x h opening with its lower-left opening cell at (0, 0). */
    private static Set<Long> frame(int w, int h) {
        Set<Long> f = new HashSet<>();
        for (int u = -1; u <= w; u++) {
            f.add(k(u, -1));
            f.add(k(u, h));
        }
        for (int y = -1; y <= h; y++) {
            f.add(k(-1, y));
            f.add(k(w, y));
        }
        return f;
    }

    private static long k(int u, int y) {
        return ((long) u << 32) ^ (y & 0xFFFFFFFFL);
    }

    @Test
    void findsRectangularOpeningFromAnyFrameBlock() {
        Set<Long> f = frame(3, 4);
        assertEquals(12, GateShape.opening(-1, 1, (u, y) -> f.contains(k(u, y)), (u, y) -> !f.contains(k(u, y))).size());
        assertEquals(12, GateShape.opening(1, -1, (u, y) -> f.contains(k(u, y)), (u, y) -> !f.contains(k(u, y))).size());
    }

    @Test
    void rejectsLeakyFrame() {
        Set<Long> f = frame(3, 4);
        f.remove(k(3, 2)); // gap in the right side -> opening leaks into the world
        assertTrue(GateShape.opening(-1, 1, (u, y) -> f.contains(k(u, y)), (u, y) -> !f.contains(k(u, y))).isEmpty());
    }

    @Test
    void rejectsTooSmallAndObstructedOpenings() {
        Set<Long> small = frame(1, 3);
        assertTrue(GateShape.opening(-1, 0, (u, y) -> small.contains(k(u, y)), (u, y) -> !small.contains(k(u, y))).isEmpty());
        Set<Long> f = frame(3, 4);
        // a solid non-frame block inside the opening
        assertTrue(GateShape.opening(-1, 1, (u, y) -> f.contains(k(u, y)), (u, y) -> !f.contains(k(u, y)) && !(u == 1 && y == 1)).isEmpty());
    }
}
