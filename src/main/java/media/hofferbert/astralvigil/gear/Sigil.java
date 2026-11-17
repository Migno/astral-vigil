package media.hofferbert.astralvigil.gear;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Sigils socket into living gear and grant a stance with an active ability. Costs are starlight charge, cooldowns in
 * ticks; both are scaled by the server config multipliers. Pure Java (unit-tested).
 */
public enum Sigil {
    WHIRL(30, 120, EnumSet.of(GearKind.AXE)),
    SHIELDBREAKER(20, 160, EnumSet.of(GearKind.AXE, GearKind.SWORD)),
    DAWN(40, 240, EnumSet.of(GearKind.SWORD, GearKind.AXE)),
    FLASH(25, 80, EnumSet.of(GearKind.SWORD)),
    PRECISION(20, 120, EnumSet.of(GearKind.BOW, GearKind.CROSSBOW)),
    UNDERTOW(25, 160, EnumSet.of(GearKind.CROSSBOW, GearKind.BOW)),
    AEGIS(40, 400, EnumSet.of(GearKind.SHIELD, GearKind.ARMOR));

    private final int cost;
    private final int cooldown;
    private final Set<GearKind> fits;

    Sigil(int cost, int cooldown, Set<GearKind> fits) {
        this.cost = cost;
        this.cooldown = cooldown;
        this.fits = fits;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int baseCost() {
        return cost;
    }

    public int baseCooldown() {
        return cooldown;
    }

    public boolean fits(GearKind kind) {
        return fits.contains(kind);
    }

    public String translationKey() {
        return "sigil.astralvigil." + id();
    }

    public static Optional<Sigil> byId(String id) {
        for (Sigil s : values()) {
            if (s.id().equals(id)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
