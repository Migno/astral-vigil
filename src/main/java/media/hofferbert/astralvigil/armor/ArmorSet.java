package media.hofferbert.astralvigil.armor;

import java.util.Locale;

/** The celestial armour sets. Pure Java (unit-tested); item lookups live in {@link ArmorSets}. */
public enum ArmorSet {
    /** Seraph Plate – Starmetal, endgame, living gear (ornaments and bonuses grow with the chestplate's awakening). */
    SERAPH,
    /** Solar Regalia – Sunsteel, Solenne's mid-tier set. */
    SOLAR,
    /** Lunar Raiment – Moonsilver, Vael's mid-tier set. */
    LUNAR;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "armor.astralvigil.set." + id();
    }
}
