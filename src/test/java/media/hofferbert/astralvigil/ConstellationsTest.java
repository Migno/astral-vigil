package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import media.hofferbert.astralvigil.research.Constellation;
import media.hofferbert.astralvigil.research.Constellation.Edge;
import media.hofferbert.astralvigil.research.Constellation.Star;
import media.hofferbert.astralvigil.research.Constellations;
import org.junit.jupiter.api.Test;

class ConstellationsTest {
    @Test
    void definitionsAreValid() {
        Set<String> ids = new HashSet<>();
        for (Constellation c : Constellations.all()) {
            assertTrue(ids.add(c.id()), "duplicate id " + c.id());
            assertFalse(c.requires().isEmpty(), c.id() + " needs at least one alignment");
            assertFalse(c.edges().isEmpty(), c.id() + " has no edges");
            for (Edge e : c.edges()) {
                assertTrue(e.a() >= 0 && e.b() < c.stars().size() && e.a() != e.b(), c.id() + " bad edge " + e);
            }
            assertEquals(c.edges().size(), Set.copyOf(c.edges()).size(), c.id() + " duplicate edge");
            for (Star s : c.stars()) {
                assertTrue(s.x() >= 0 && s.x() <= 100 && s.y() >= 0 && s.y() <= 100, c.id() + " star out of grid");
            }
        }
        assertEquals(4, ids.size());
    }

    @Test
    void edgesAreUndirected() {
        assertEquals(new Edge(1, 3), new Edge(3, 1));
    }

    @Test
    void matchingRequiresExactEdges() {
        Constellation halo = Constellations.HALO;
        Set<Edge> drawn = new HashSet<>();
        for (Edge e : halo.edges()) {
            drawn.add(new Edge(e.b(), e.a())); // drawn in reverse direction
        }
        assertTrue(halo.matches(drawn));
        drawn.add(new Edge(0, halo.stars().size())); // extra line to a decoy
        assertFalse(halo.matches(drawn));
    }
}
