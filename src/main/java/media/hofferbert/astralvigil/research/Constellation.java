package media.hofferbert.astralvigil.research;

import java.util.List;
import java.util.Set;

/**
 * A constellation: stars on a 0..100 grid and the edges that connect them. Solving one in the Star Chart teaches it;
 * Phase 6 uses learned constellations for Star Anchor rituals.
 *
 * @param requires alignments whose attunement threshold must be reached before the constellation shows up
 * @param decoys extra stars that belong to no edge (make the puzzle a puzzle)
 */
public record Constellation(String id, Set<Alignment> requires, List<Star> stars, List<Edge> edges, List<Star> decoys) {
    public record Star(int x, int y) {}

    /** Undirected edge between two star indices. */
    public record Edge(int a, int b) {
        public Edge {
            if (a > b) {
                int t = a;
                a = b;
                b = t;
            }
        }
    }

    public String translationKey() {
        return "constellation.astralvigil." + id;
    }

    public String advancementId() {
        return "research/constellation_" + id;
    }

    /** Pure check used by the screen and the unit test. */
    public boolean matches(Set<Edge> drawn) {
        return drawn.equals(Set.copyOf(edges));
    }
}
