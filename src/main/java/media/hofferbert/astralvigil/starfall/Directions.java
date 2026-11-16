package media.hofferbert.astralvigil.starfall;

/** Eight-way compass directions for chat messages (pure, unit-tested). Minecraft: +x = east, +z = south. */
public final class Directions {
    private static final String[] NAMES = {
        "east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"
    };

    private Directions() {}

    public static String cardinal(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return "here";
        }
        double deg = Math.toDegrees(Math.atan2(dz, dx)); // 0 = east, 90 = south
        int idx = (int) Math.floorMod(Math.round(deg / 45.0), 8L);
        return NAMES[idx];
    }
}
