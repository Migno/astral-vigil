package media.hofferbert.astralvigil.cosmetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot;
import org.junit.jupiter.api.Test;

class CosmeticTest {
    @Test
    void everySlotHasOptions() {
        for (Slot slot : Slot.values()) {
            assertFalse(Cosmetic.options(slot).isEmpty(), slot.id());
        }
    }

    @Test
    void idsRoundTripAndRespectSlots() {
        for (Cosmetic c : Cosmetic.values()) {
            assertEquals(Optional.of(c), Cosmetic.byId(c.id()));
            assertEquals(Optional.of(c), Cosmetic.byId(c.slot(), c.id()));
            for (Slot other : Slot.values()) {
                if (other != c.slot()) {
                    assertTrue(Cosmetic.byId(other, c.id()).isEmpty());
                }
            }
        }
        assertTrue(Cosmetic.byId("netherite_crown").isEmpty());
    }

    @Test
    void cycleVisitsNoneAndEveryOptionOnce() {
        for (Slot slot : Slot.values()) {
            int size = Cosmetic.options(slot).size() + 1;
            Optional<Cosmetic> current = Optional.empty();
            var seen = new java.util.HashSet<Optional<Cosmetic>>();
            for (int i = 0; i < size; i++) {
                assertTrue(seen.add(current));
                current = Cosmetic.cycle(slot, current, 1);
            }
            assertEquals(Optional.empty(), current);
            assertEquals(Optional.of(Cosmetic.options(slot).getLast()), Cosmetic.cycle(slot, Optional.empty(), -1));
        }
    }
}
