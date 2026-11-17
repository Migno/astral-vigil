package media.hofferbert.astralvigil.firmament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WatcherTest {
    @Test
    void rivalsAreSymmetric() {
        for (Watcher w : Watcher.values()) {
            assertNotEquals(w, w.rival());
            assertEquals(w, w.rival().rival());
        }
    }

    @Test
    void alignmentsAreDistinct() {
        var seen = new HashSet<>();
        for (Watcher w : Watcher.values()) {
            assertEquals(true, seen.add(w.alignment()));
            assertEquals(w, Watcher.byId(w.id()).orElseThrow());
        }
    }
}
