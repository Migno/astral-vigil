package media.hofferbert.astralvigil.starlight;

import java.util.Locale;

/**
 * Colour of a starlight beam. Collectors gather {@link #PURE} light; a Prism splits it into the three aligned tints
 * that Phase 4 recipes can demand. (Void light is reserved for the Eclipse.)
 */
public enum Tint {
    PURE(0xFF8FEFFF),
    SOL(0xFFF2C94C),
    LUNA(0xFFD8D8E6),
    STELLA(0xFF2FA8D0);

    private final int argb;

    Tint(int argb) {
        this.argb = argb;
    }

    public int argb() {
        return argb;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "tint.astralvigil." + id();
    }

    public static Tint byOrdinal(int ordinal) {
        Tint[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PURE;
    }
}
