package media.hofferbert.astralvigil.firmament;

import java.util.Locale;
import java.util.Optional;
import media.hofferbert.astralvigil.research.Alignment;

/** The four Watchers, their domain and the Watcher whose favour opposes theirs. Pure Java. */
public enum Watcher {
    SOLENNE(Alignment.SOL, "vael", "halo"),
    VAEL(Alignment.LUNA, "solenne", "crescent"),
    ORRIN(Alignment.STELLA, "ilyra", "anvil"),
    ILYRA(Alignment.VOID, "orrin", "quill");

    private final Alignment alignment;
    private final String rival;
    private final String constellation;

    Watcher(Alignment alignment, String rival, String constellation) {
        this.alignment = alignment;
        this.rival = rival;
        this.constellation = constellation;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Alignment alignment() {
        return alignment;
    }

    public Watcher rival() {
        return byId(rival).orElseThrow();
    }

    public String constellation() {
        return constellation;
    }

    public String translationKey() {
        return "watcher.astralvigil." + id();
    }

    public static Optional<Watcher> byId(String id) {
        for (Watcher w : values()) {
            if (w.id().equals(id)) {
                return Optional.of(w);
            }
        }
        return Optional.empty();
    }
}
