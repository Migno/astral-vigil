package media.hofferbert.astralvigil.firmament;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the opening of an Astral Gate frame in one vertical plane. Pure (no Minecraft classes) so it can be unit tested:
 * the caller maps plane coordinates (u along the frame's axis, y up) to world positions.
 *
 * <p>The opening is every open cell 4-connected to a start cell next to the clicked frame block. It is valid when it is
 * fully enclosed by frame blocks, at least {@value #MIN_WIDTH} wide and {@value #MIN_HEIGHT} tall, and at most
 * {@value #MAX_SIDE} on each side. Any enclosed shape works, not only rectangles.
 */
public final class GateShape {
    public static final int MIN_WIDTH = 2;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_SIDE = 21;

    @FunctionalInterface
    public interface Cells {
        boolean test(int u, int y);
    }

    private GateShape() {}

    /** @return the opening's cells as {u, y} pairs, or an empty list when the clicked block frames no valid gate */
    public static List<int[]> opening(int u0, int y0, Cells frame, Cells open) {
        int[][] starts = {{u0 + 1, y0}, {u0 - 1, y0}, {u0, y0 + 1}, {u0, y0 - 1}};
        for (int[] s : starts) {
            List<int[]> cells = fill(s[0], s[1], frame, open);
            if (!cells.isEmpty()) {
                return cells;
            }
        }
        return List.of();
    }

    private static List<int[]> fill(int su, int sy, Cells frame, Cells open) {
        if (!open.test(su, sy)) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        List<int[]> cells = new ArrayList<>();
        queue.add(new int[] {su, sy});
        seen.add(key(su, sy));
        int minU = su, maxU = su, minY = sy, maxY = sy;
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            cells.add(c);
            minU = Math.min(minU, c[0]);
            maxU = Math.max(maxU, c[0]);
            minY = Math.min(minY, c[1]);
            maxY = Math.max(maxY, c[1]);
            if (maxU - minU + 1 > MAX_SIDE || maxY - minY + 1 > MAX_SIDE) {
                return List.of(); // leaks out of the frame (or far too big)
            }
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nu = c[0] + d[0], ny = c[1] + d[1];
                if (seen.contains(key(nu, ny)) || frame.test(nu, ny)) {
                    continue;
                }
                if (!open.test(nu, ny)) {
                    return List.of(); // touches something that is neither opening nor frame
                }
                seen.add(key(nu, ny));
                queue.add(new int[] {nu, ny});
            }
        }
        if (maxU - minU + 1 < MIN_WIDTH || maxY - minY + 1 < MIN_HEIGHT) {
            return List.of();
        }
        return cells;
    }

    private static long key(int u, int y) {
        return ((long) u << 32) ^ (y & 0xFFFFFFFFL);
    }
}
