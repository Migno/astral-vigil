package media.hofferbert.astralvigil.research;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Star Anchor layouts (block-grid patterns) for the constellations, and the pure matcher. A layout matches if every
 * pattern point (after translation, any of 4 rotations, optional mirroring and an integer scale 1..3) has an anchor.
 * Unit-tested.
 */
public final class AnchorPatterns {
    public record P(int x, int z) {}

    public record Match(Constellation constellation, int scale, List<P> anchors) {}

    public static final int MAX_SCALE = 3;

    /** Grid patterns; first point is the reference. Kept small so a scale-1 ritual fits a 7×7 area. */
    public static final Map<String, List<P>> PATTERNS = Map.of(
            "halo", List.of(new P(2, 0), new P(4, 1), new P(4, 3), new P(2, 4), new P(0, 3), new P(0, 1)),
            "crescent", List.of(new P(3, 0), new P(1, 1), new P(0, 3), new P(1, 5), new P(3, 6)),
            "anvil", List.of(new P(0, 0), new P(6, 0), new P(4, 2), new P(2, 2), new P(1, 5), new P(5, 5)),
            "quill", List.of(new P(0, 6), new P(2, 4), new P(4, 2), new P(6, 0), new P(4, 0), new P(6, 2)));

    private AnchorPatterns() {}

    /**
     * Finds a learned constellation laid out by {@code anchors} that includes {@code origin}.
     *
     * @param anchors anchor positions (x/z only, same height)
     * @param learned constellation ids the player may use
     */
    public static Optional<Match> find(Set<P> anchors, P origin, Set<String> learned) {
        for (Constellation c : Constellations.all()) {
            if (!learned.contains(c.id()) || !PATTERNS.containsKey(c.id())) {
                continue;
            }
            List<P> pattern = PATTERNS.get(c.id());
            for (int scale = MAX_SCALE; scale >= 1; scale--) {
                for (int rot = 0; rot < 4; rot++) {
                    for (boolean mirror : new boolean[] {false, true}) {
                        List<P> shape = transform(pattern, rot, mirror, scale);
                        // anchor the shape so that each pattern point in turn sits on the origin
                        for (P pivot : shape) {
                            int dx = origin.x() - pivot.x();
                            int dz = origin.z() - pivot.z();
                            List<P> placed = shape.stream().map(p -> new P(p.x() + dx, p.z() + dz)).toList();
                            if (anchors.containsAll(placed)) {
                                return Optional.of(new Match(c, scale, placed));
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    static List<P> transform(List<P> pattern, int rot, boolean mirror, int scale) {
        return pattern.stream()
                .map(p -> {
                    int x = mirror ? -p.x() : p.x();
                    int z = p.z();
                    for (int i = 0; i < rot; i++) {
                        int t = x;
                        x = -z;
                        z = t;
                    }
                    return new P(x * scale, z * scale);
                })
                .toList();
    }
}
