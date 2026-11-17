package media.hofferbert.astralvigil.armor;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Pure full-set bonus rules (no Minecraft types, unit-tested). {@link SetBonusEvents} turns the result into effects.
 * Values come from {@code astralvigil-server.toml} via {@link Settings}; the reasoning is in docs/BALANCE.md.
 */
public final class SetBonuses {
    private SetBonuses() {}

    public enum Bonus {
        /** Solar: immune to fire and lava (Fire Resistance). */
        FIRE_WARD,
        /** Solar: Strength by day. */
        SOLAR_MIGHT,
        /** Lunar: Night Vision at night. */
        MOONSIGHT,
        /** Lunar: Speed at night. */
        MOONSTRIDE,
        /** Lunar: reduced fall damage. */
        MOONFALL,
        /** Seraph: heals under the night sky, paid with the chestplate's starlight charge. */
        STARLIGHT_MENDING,
        /** Seraph (Awakened): sneak in mid-air to glide. */
        SERAPH_GLIDE,
        /** Seraph (Awakened): starting a glide flings you forward (charge + cooldown). */
        GLIDE_BURST,
        /** Seraph (Ascended): no fall damage. */
        FEATHERFALL;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "armor.astralvigil.bonus." + id();
        }
    }

    /** A bonus and the awakening level of the Seraph chestplate it needs (0 for the mid-tier sets). */
    public record Tier(Bonus bonus, int level) {}

    /** Current sky: {@code day}/{@code night} only in dimensions with a day cycle; {@code starlit} = under the open night sky. */
    public record Sky(boolean day, boolean night, boolean starlit) {
        public static final Sky NONE = new Sky(false, false, false);
    }

    /** Config snapshot. Strength/speed are effect levels (0 = off, 1 = I, 2 = II). */
    public record Settings(
            boolean fireWard,
            int solarStrength,
            boolean moonsight,
            int moonstride,
            double moonfallReduction,
            double mendingAmount,
            int burstCost) {
        public static final Settings DEFAULTS = new Settings(true, 1, true, 1, 0.5, 1.0, 25);
    }

    private static final List<Tier> SERAPH = List.of(
            new Tier(Bonus.STARLIGHT_MENDING, 0),
            new Tier(Bonus.SERAPH_GLIDE, 1),
            new Tier(Bonus.GLIDE_BURST, 1),
            new Tier(Bonus.FEATHERFALL, 2));
    private static final List<Tier> SOLAR = List.of(new Tier(Bonus.FIRE_WARD, 0), new Tier(Bonus.SOLAR_MIGHT, 0));
    private static final List<Tier> LUNAR =
            List.of(new Tier(Bonus.MOONSIGHT, 0), new Tier(Bonus.MOONSTRIDE, 0), new Tier(Bonus.MOONFALL, 0));

    /** Every bonus of a set in tooltip order. */
    public static List<Tier> tiers(ArmorSet set) {
        return switch (set) {
            case SERAPH -> SERAPH;
            case SOLAR -> SOLAR;
            case LUNAR -> LUNAR;
        };
    }

    /** The set all four slots belong to, if any (nulls = empty slot or foreign armour). */
    public static Optional<ArmorSet> fullSet(ArmorSet head, ArmorSet chest, ArmorSet legs, ArmorSet feet) {
        return head != null && head == chest && chest == legs && legs == feet ? Optional.of(head) : Optional.empty();
    }

    /** How many of the given slots hold a piece of {@code set}. */
    public static int count(ArmorSet set, ArmorSet... slots) {
        int n = 0;
        for (ArmorSet s : slots) {
            if (s == set) {
                n++;
            }
        }
        return n;
    }

    /** Bonuses active right now for a full set. {@code level} = Seraph awakening level (ignored by the other sets). */
    public static Set<Bonus> active(ArmorSet set, int level, Sky sky, Settings s) {
        EnumSet<Bonus> out = EnumSet.noneOf(Bonus.class);
        switch (set) {
            case SOLAR -> {
                if (s.fireWard()) {
                    out.add(Bonus.FIRE_WARD);
                }
                if (sky.day() && s.solarStrength() > 0) {
                    out.add(Bonus.SOLAR_MIGHT);
                }
            }
            case LUNAR -> {
                if (sky.night() && s.moonsight()) {
                    out.add(Bonus.MOONSIGHT);
                }
                if (sky.night() && s.moonstride() > 0) {
                    out.add(Bonus.MOONSTRIDE);
                }
                if (s.moonfallReduction() > 0) {
                    out.add(Bonus.MOONFALL);
                }
            }
            case SERAPH -> {
                if (sky.starlit() && s.mendingAmount() > 0) {
                    out.add(Bonus.STARLIGHT_MENDING);
                }
                if (level >= 1) {
                    out.add(Bonus.SERAPH_GLIDE);
                    out.add(Bonus.GLIDE_BURST);
                }
                if (level >= 2) {
                    out.add(Bonus.FEATHERFALL);
                }
            }
        }
        return out;
    }

    /** Fall damage multiplier for a full set (1 = unchanged). */
    public static float fallMultiplier(ArmorSet set, int level, Settings s) {
        if (set == ArmorSet.SERAPH && level >= 2) {
            return 0f;
        }
        if (set == ArmorSet.LUNAR) {
            return (float) Math.max(0.0, 1.0 - Math.min(1.0, s.moonfallReduction()));
        }
        return 1f;
    }

    /** Glide burst may fire: awakened Seraph, enough charge, cooldown over. */
    public static boolean canBurst(int level, int charge, long now, long cooldownUntil, Settings s) {
        return level >= 1 && charge >= s.burstCost() && now >= cooldownUntil;
    }
}
