package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import media.hofferbert.astralvigil.starfall.Directions;
import org.junit.jupiter.api.Test;

class DirectionsTest {
    @Test
    void cardinals() {
        assertEquals("east", Directions.cardinal(10, 0));
        assertEquals("west", Directions.cardinal(-10, 0));
        assertEquals("south", Directions.cardinal(0, 10)); // +z = south in Minecraft
        assertEquals("north", Directions.cardinal(0, -10));
        assertEquals("northeast", Directions.cardinal(10, -10));
        assertEquals("southwest", Directions.cardinal(-10, 10));
        assertEquals("here", Directions.cardinal(0, 0));
    }
}
