package media.hofferbert.astralvigil.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import media.hofferbert.astralvigil.armor.SetBonuses.Bonus;
import media.hofferbert.astralvigil.armor.SetBonuses.Settings;
import media.hofferbert.astralvigil.armor.SetBonuses.Sky;
import org.junit.jupiter.api.Test;

class SetBonusesTest {
    private static final Settings S = Settings.DEFAULTS;
    private static final Sky DAY = new Sky(true, false, false);
    private static final Sky NIGHT = new Sky(false, true, true);

    @Test
    void fullSetNeedsAllFourSlots() {
        assertEquals(Optional.of(ArmorSet.SOLAR), SetBonuses.fullSet(ArmorSet.SOLAR, ArmorSet.SOLAR, ArmorSet.SOLAR, ArmorSet.SOLAR));
        assertTrue(SetBonuses.fullSet(ArmorSet.SOLAR, ArmorSet.SOLAR, ArmorSet.SOLAR, null).isEmpty());
        assertTrue(SetBonuses.fullSet(ArmorSet.LUNAR, ArmorSet.SOLAR, ArmorSet.SOLAR, ArmorSet.SOLAR).isEmpty());
        assertTrue(SetBonuses.fullSet(null, null, null, null).isEmpty());
        assertEquals(3, SetBonuses.count(ArmorSet.SERAPH, ArmorSet.SERAPH, ArmorSet.SERAPH, null, ArmorSet.SERAPH));
    }

    @Test
    void solarIsFireproofAlwaysAndStrongByDay() {
        assertEquals(Set.of(Bonus.FIRE_WARD, Bonus.SOLAR_MIGHT), SetBonuses.active(ArmorSet.SOLAR, 0, DAY, S));
        assertEquals(Set.of(Bonus.FIRE_WARD), SetBonuses.active(ArmorSet.SOLAR, 0, NIGHT, S));
        assertEquals(Set.of(Bonus.FIRE_WARD), SetBonuses.active(ArmorSet.SOLAR, 0, Sky.NONE, S));
        Settings off = new Settings(false, 0, true, 1, 0.5, 1.0, 25);
        assertTrue(SetBonuses.active(ArmorSet.SOLAR, 0, DAY, off).isEmpty());
    }

    @Test
    void lunarSeesAndRunsAtNight() {
        assertEquals(Set.of(Bonus.MOONSIGHT, Bonus.MOONSTRIDE, Bonus.MOONFALL), SetBonuses.active(ArmorSet.LUNAR, 0, NIGHT, S));
        assertEquals(Set.of(Bonus.MOONFALL), SetBonuses.active(ArmorSet.LUNAR, 0, DAY, S));
        assertEquals(0.5f, SetBonuses.fallMultiplier(ArmorSet.LUNAR, 0, S), 1e-6);
        assertEquals(1.0f, SetBonuses.fallMultiplier(ArmorSet.SOLAR, 2, S), 1e-6);
    }

    @Test
    void seraphGrowsWithAwakening() {
        assertEquals(Set.of(Bonus.STARLIGHT_MENDING), SetBonuses.active(ArmorSet.SERAPH, 0, NIGHT, S));
        assertTrue(SetBonuses.active(ArmorSet.SERAPH, 0, DAY, S).isEmpty());
        Set<Bonus> awakened = SetBonuses.active(ArmorSet.SERAPH, 1, DAY, S);
        assertTrue(awakened.contains(Bonus.SERAPH_GLIDE) && awakened.contains(Bonus.GLIDE_BURST));
        assertFalse(awakened.contains(Bonus.FEATHERFALL));
        assertTrue(SetBonuses.active(ArmorSet.SERAPH, 2, DAY, S).contains(Bonus.FEATHERFALL));
        assertEquals(1.0f, SetBonuses.fallMultiplier(ArmorSet.SERAPH, 1, S), 1e-6);
        assertEquals(0.0f, SetBonuses.fallMultiplier(ArmorSet.SERAPH, 2, S), 1e-6);
    }

    @Test
    void glideBurstCostsChargeAndRespectsCooldown() {
        assertFalse(SetBonuses.canBurst(0, 200, 100, 0, S), "dormant plate cannot burst");
        assertFalse(SetBonuses.canBurst(1, S.burstCost() - 1, 100, 0, S), "not enough charge");
        assertFalse(SetBonuses.canBurst(1, 200, 100, 101, S), "cooling down");
        assertTrue(SetBonuses.canBurst(1, S.burstCost(), 100, 100, S));
    }

    @Test
    void everyBonusIsListedOnce() {
        int total = 0;
        for (ArmorSet set : ArmorSet.values()) {
            total += SetBonuses.tiers(set).size();
            for (SetBonuses.Tier t : SetBonuses.tiers(set)) {
                assertTrue(t.level() >= 0 && t.level() <= 2);
                assertTrue(set == ArmorSet.SERAPH || t.level() == 0, "only the living Seraph Plate has level gates");
            }
        }
        assertEquals(Bonus.values().length, total);
    }
}
