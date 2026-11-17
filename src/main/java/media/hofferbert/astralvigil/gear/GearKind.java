package media.hofferbert.astralvigil.gear;

import java.util.Locale;
import media.hofferbert.astralvigil.research.Alignment;

/** The kinds of living gear, their Watcher alignment and the deed that awakens them. Pure Java. */
public enum GearKind {
    /** Halo Edge – slay undead by daylight. */
    SWORD(Alignment.SOL),
    /** Dawnbreaker – slay undead by daylight. */
    AXE(Alignment.SOL),
    /** Crescent – slay at night (full moon counts thrice). */
    BOW(Alignment.LUNA),
    /** Tidecaller – slay at night (full moon counts thrice). */
    CROSSBOW(Alignment.LUNA),
    /** Starlight Aegis – block attacks. */
    SHIELD(Alignment.STELLA),
    /** Seraph Plate – slay Void-aligned creatures while wearing the full set (tracked on the chestplate). */
    ARMOR(Alignment.STELLA);

    private final Alignment alignment;

    GearKind(Alignment alignment) {
        this.alignment = alignment;
    }

    public Alignment alignment() {
        return alignment;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String deedKey() {
        return "gear.astralvigil.deed." + id();
    }
}
