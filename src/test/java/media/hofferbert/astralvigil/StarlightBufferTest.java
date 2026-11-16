package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import media.hofferbert.astralvigil.starlight.StarlightBuffer;
import media.hofferbert.astralvigil.starlight.Tint;
import org.junit.jupiter.api.Test;

class StarlightBufferTest {
    @Test
    void sharedCapacityAcrossTints() {
        StarlightBuffer b = new StarlightBuffer(100);
        assertEquals(60, b.insert(Tint.SOL, 60, false));
        assertEquals(40, b.insert(Tint.LUNA, 70, false));
        assertEquals(0, b.insert(Tint.PURE, 5, false));
        assertEquals(100, b.total());
        assertEquals(0, b.space());
    }

    @Test
    void simulateDoesNotChange() {
        StarlightBuffer b = new StarlightBuffer(50);
        assertEquals(50, b.insert(Tint.PURE, 80, true));
        assertEquals(0, b.total());
        b.insert(Tint.PURE, 20, false);
        assertEquals(20, b.extract(Tint.PURE, 30, true));
        assertEquals(20, b.get(Tint.PURE));
    }

    @Test
    void extractPerTint() {
        StarlightBuffer b = new StarlightBuffer(100);
        b.insert(Tint.STELLA, 30, false);
        assertEquals(0, b.extract(Tint.SOL, 10, false));
        assertEquals(10, b.extract(Tint.STELLA, 10, false));
        assertEquals(20, b.get(Tint.STELLA));
    }

    @Test
    void roundTripArray() {
        StarlightBuffer b = new StarlightBuffer(100);
        b.insert(Tint.SOL, 7, false);
        b.insert(Tint.LUNA, 3, false);
        StarlightBuffer c = new StarlightBuffer(100);
        c.fromArray(b.toArray());
        assertEquals(7, c.get(Tint.SOL));
        assertEquals(3, c.get(Tint.LUNA));
    }

    @Test
    void comparatorSignal() {
        StarlightBuffer b = new StarlightBuffer(140);
        assertEquals(0, b.signal());
        b.insert(Tint.PURE, 1, false);
        assertEquals(1, b.signal());
        b.insert(Tint.PURE, 139, false);
        assertEquals(15, b.signal());
    }
}
