package media.hofferbert.astralvigil.research;

import java.util.Locale;
import java.util.Optional;

/**
 * The four celestial alignments. Membership is data-driven via tags, see {@link AlignmentTags}. Pure Java (no
 * Minecraft classes) so research logic can be unit-tested.
 */
public enum Alignment {
    SOL(0xF2C94C),
    LUNA(0xD8D8E6),
    STELLA(0x8FEFFF),
    VOID(0x9B6AD0);

    private final int color;

    Alignment(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "alignment.astralvigil." + id();
    }

    /** RGB text colour. */
    public int color() {
        return color;
    }

    public static Optional<Alignment> byId(String id) {
        for (Alignment a : values()) {
            if (a.id().equals(id)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }
}
