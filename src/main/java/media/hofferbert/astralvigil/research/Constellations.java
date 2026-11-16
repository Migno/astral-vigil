package media.hofferbert.astralvigil.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import media.hofferbert.astralvigil.research.Constellation.Edge;
import media.hofferbert.astralvigil.research.Constellation.Star;

/** The built-in constellations, one per Watcher. Pure data (unit-tested). */
public final class Constellations {
    private static final Map<String, Constellation> ALL = new LinkedHashMap<>();

    /** Solenne's Halo – a ring of six stars around a core. */
    public static final Constellation HALO = add(new Constellation(
            "halo",
            Set.of(Alignment.SOL),
            List.of(
                    new Star(50, 15), new Star(78, 32), new Star(78, 68),
                    new Star(50, 85), new Star(22, 68), new Star(22, 32), new Star(50, 50)),
            List.of(new Edge(0, 1), new Edge(1, 2), new Edge(2, 3), new Edge(3, 4), new Edge(4, 5), new Edge(5, 0)),
            List.of(new Star(90, 88), new Star(10, 10))));

    /** Vael's Crescent – a curved arc. */
    public static final Constellation CRESCENT = add(new Constellation(
            "crescent",
            Set.of(Alignment.LUNA),
            List.of(new Star(62, 10), new Star(38, 22), new Star(28, 48), new Star(36, 74), new Star(60, 88)),
            List.of(new Edge(0, 1), new Edge(1, 2), new Edge(2, 3), new Edge(3, 4)),
            List.of(new Star(70, 50), new Star(85, 20), new Star(15, 85))));

    /** Orrin's Anvil – a flat top on a narrow waist and a broad foot. */
    public static final Constellation ANVIL = add(new Constellation(
            "anvil",
            Set.of(Alignment.STELLA),
            List.of(
                    new Star(15, 25), new Star(85, 25), new Star(60, 45), new Star(40, 45),
                    new Star(30, 80), new Star(70, 80)),
            List.of(new Edge(0, 1), new Edge(1, 2), new Edge(0, 3), new Edge(2, 5), new Edge(3, 4), new Edge(4, 5)),
            List.of(new Star(50, 10), new Star(50, 62))));

    /** Ilyra's Quill – a long shaft with a feathered tip. Needs two attunements: a chronicler reads every sky. */
    public static final Constellation QUILL = add(new Constellation(
            "quill",
            Set.of(Alignment.LUNA, Alignment.STELLA),
            List.of(
                    new Star(20, 85), new Star(40, 65), new Star(58, 45), new Star(75, 25),
                    new Star(88, 12), new Star(62, 22), new Star(80, 42)),
            List.of(new Edge(0, 1), new Edge(1, 2), new Edge(2, 3), new Edge(3, 4), new Edge(3, 5), new Edge(3, 6)),
            List.of(new Star(25, 30), new Star(70, 75))));

    private Constellations() {}

    private static Constellation add(Constellation c) {
        ALL.put(c.id(), c);
        return c;
    }

    public static List<Constellation> all() {
        return List.copyOf(ALL.values());
    }

    public static Optional<Constellation> byId(String id) {
        return Optional.ofNullable(ALL.get(id));
    }

    /** Whether the constellation is unlocked for the given research (attunement threshold for every required alignment). */
    public static boolean available(Constellation c, ResearchData data, int attunementThreshold) {
        return c.requires().stream().allMatch(a -> data.knowledge(a) >= attunementThreshold);
    }
}
