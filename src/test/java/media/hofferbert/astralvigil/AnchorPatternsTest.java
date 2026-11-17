package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import media.hofferbert.astralvigil.research.AnchorPatterns;
import media.hofferbert.astralvigil.research.AnchorPatterns.P;
import org.junit.jupiter.api.Test;

class AnchorPatternsTest {
    private static Set<P> place(List<P> pattern, int dx, int dz, int scale, boolean rotate) {
        Set<P> out = new HashSet<>();
        for (P p : pattern) {
            int x = p.x() * scale;
            int z = p.z() * scale;
            if (rotate) {
                int t = x;
                x = -z;
                z = t;
            }
            out.add(new P(x + dx, z + dz));
        }
        return out;
    }

    @Test
    void matchesPlainHalo() {
        Set<P> anchors = place(AnchorPatterns.PATTERNS.get("halo"), 100, -40, 1, false);
        P origin = anchors.iterator().next();
        Optional<AnchorPatterns.Match> m = AnchorPatterns.find(anchors, origin, Set.of("halo"));
        assertTrue(m.isPresent());
        assertEquals("halo", m.get().constellation().id());
        assertEquals(1, m.get().scale());
    }

    @Test
    void matchesRotatedAndScaled() {
        Set<P> anchors = place(AnchorPatterns.PATTERNS.get("anvil"), 7, 9, 2, true);
        Optional<AnchorPatterns.Match> m = AnchorPatterns.find(anchors, anchors.iterator().next(), Set.of("anvil"));
        assertTrue(m.isPresent());
        assertEquals(2, m.get().scale());
    }

    @Test
    void unlearnedOrIncompleteDoesNotMatch() {
        Set<P> anchors = place(AnchorPatterns.PATTERNS.get("crescent"), 0, 0, 1, false);
        P origin = anchors.iterator().next();
        assertFalse(AnchorPatterns.find(anchors, origin, Set.of("halo")).isPresent(), "not learned");
        Set<P> missing = new HashSet<>(anchors);
        missing.remove(new P(3, 6));
        assertFalse(AnchorPatterns.find(missing, origin, Set.of("crescent")).isPresent(), "missing anchor");
    }

    @Test
    void everyConstellationHasAPattern() {
        for (var c : media.hofferbert.astralvigil.research.Constellations.all()) {
            assertTrue(AnchorPatterns.PATTERNS.containsKey(c.id()), c.id());
        }
    }
}
