package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import media.hofferbert.astralvigil.gear.GearKind;
import media.hofferbert.astralvigil.gear.GearProgress;
import media.hofferbert.astralvigil.gear.Sigil;
import org.junit.jupiter.api.Test;

class GearProgressTest {
    @Test
    void levels() {
        assertEquals(0, GearProgress.level(0));
        assertEquals(0, GearProgress.level(24));
        assertEquals(1, GearProgress.level(25));
        assertEquals(2, GearProgress.level(100));
        assertEquals(2, GearProgress.level(9999));
    }

    @Test
    void slotsGrowWithLevel() {
        assertEquals(1, GearProgress.slots(0));
        assertEquals(2, GearProgress.slots(1));
        assertEquals(3, GearProgress.slots(2));
        assertEquals(3, GearProgress.slots(5));
    }

    @Test
    void toNext() {
        assertEquals(25, GearProgress.toNext(0));
        assertEquals(75, GearProgress.toNext(25));
        assertEquals(0, GearProgress.toNext(100));
    }

    @Test
    void socketingRules() {
        assertNull(GearProgress.socket(List.of(), Sigil.PRECISION, GearKind.SWORD, 0), "wrong gear kind");
        List<String> one = GearProgress.socket(List.of(), Sigil.DAWN, GearKind.SWORD, 0);
        assertNotNull(one);
        assertNull(GearProgress.socket(one, Sigil.FLASH, GearKind.SWORD, 0), "dormant gear has one slot");
        assertNull(GearProgress.socket(one, Sigil.DAWN, GearKind.SWORD, 2), "no duplicates");
        List<String> two = GearProgress.socket(one, Sigil.FLASH, GearKind.SWORD, 1);
        assertEquals(List.of("dawn", "flash"), two);
    }

    @Test
    void everyGearKindHasASigil() {
        for (GearKind kind : GearKind.values()) {
            boolean any = false;
            for (Sigil s : Sigil.values()) {
                any |= s.fits(kind);
            }
            assertTrue(any, kind + " has no sigil");
        }
    }

    @Test
    void stanceCycles() {
        assertEquals(1, GearProgress.nextStance(0, 3));
        assertEquals(0, GearProgress.nextStance(2, 3));
        assertEquals(0, GearProgress.nextStance(5, 0));
    }
}
