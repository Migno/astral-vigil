package media.hofferbert.astralvigil.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Developer cosmetics. Pure enum (no Minecraft classes) so it can be unit tested. Each cosmetic belongs to exactly one
 * slot; a player wears at most one cosmetic per slot. Visuals live in {@code client/cosmetics}.
 */
public enum Cosmetic {
    SOLAR_CROWN(Slot.HEAD, 0xF2C94C),
    ECLIPSE_CROWN(Slot.HEAD, 0xB57CFF),
    SERAPH_HALO(Slot.HEAD, 0xFFF4C0),
    SERAPH_CAPE(Slot.BACK, 0xF2C94C),
    STARFIELD_CAPE(Slot.BACK, 0x8FEFFF),
    ECLIPSE_CAPE(Slot.BACK, 0xB57CFF),
    RADIANT_WINGS(Slot.WINGS, 0x8FEFFF),
    ECLIPSE_WINGS(Slot.WINGS, 0xB57CFF),
    STARLIGHT_MOTES(Slot.AURA, 0x8FEFFF),
    SOLAR_EMBERS(Slot.AURA, 0xF2C94C),
    VOID_STORM(Slot.AURA, 0xB57CFF);

    public enum Slot {
        HEAD,
        BACK,
        WINGS,
        AURA;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "cosmetic.astralvigil.slot." + id();
        }
    }

    private final Slot slot;
    private final int colour;

    Cosmetic(Slot slot, int colour) {
        this.slot = slot;
        this.colour = colour;
    }

    public Slot slot() {
        return slot;
    }

    /** Signature colour (RGB) used for particles and the screen accent. */
    public int colour() {
        return colour;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "cosmetic.astralvigil." + id();
    }

    public static Optional<Cosmetic> byId(String id) {
        for (Cosmetic c : values()) {
            if (c.id().equals(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** Cosmetic with this id, but only if it belongs to {@code slot}. */
    public static Optional<Cosmetic> byId(Slot slot, String id) {
        return byId(id).filter(c -> c.slot == slot);
    }

    public static List<Cosmetic> options(Slot slot) {
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic c : values()) {
            if (c.slot == slot) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * Cycles through "none" plus every option of the slot. {@code current} empty means none; returns empty for none.
     */
    public static Optional<Cosmetic> cycle(Slot slot, Optional<Cosmetic> current, int step) {
        List<Cosmetic> options = options(slot);
        int size = options.size() + 1;
        int index = current.filter(c -> c.slot == slot).map(c -> options.indexOf(c) + 1).orElse(0);
        int next = Math.floorMod(index + step, size);
        return next == 0 ? Optional.empty() : Optional.of(options.get(next - 1));
    }
}
