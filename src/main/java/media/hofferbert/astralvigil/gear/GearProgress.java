package media.hofferbert.astralvigil.gear;

import java.util.ArrayList;
import java.util.List;

/** Pure living-gear rules: awakening thresholds, sigil slots, stance cycling. Unit-tested. */
public final class GearProgress {
    /** Deeds needed for level 1 (Awakened) and level 2 (Ascended). */
    public static final int[] THRESHOLDS = {25, 100};

    private GearProgress() {}

    public static int level(int deeds) {
        int level = 0;
        for (int t : THRESHOLDS) {
            if (deeds >= t) {
                level++;
            }
        }
        return level;
    }

    /** 1 slot when dormant, 2 awakened, 3 ascended. */
    public static int slots(int level) {
        return Math.min(3, 1 + Math.max(0, level));
    }

    /** Deeds still missing for the next level, or 0 when ascended. */
    public static int toNext(int deeds) {
        for (int t : THRESHOLDS) {
            if (deeds < t) {
                return t - deeds;
            }
        }
        return 0;
    }

    /** @return the new sigil list, or {@code null} if the sigil cannot be socketed. */
    public static List<String> socket(List<String> current, Sigil sigil, GearKind kind, int level) {
        if (!sigil.fits(kind) || current.contains(sigil.id()) || current.size() >= slots(level)) {
            return null;
        }
        List<String> out = new ArrayList<>(current);
        out.add(sigil.id());
        return out;
    }

    public static int nextStance(int stance, int sigilCount) {
        return sigilCount <= 0 ? 0 : Math.floorMod(stance + 1, sigilCount);
    }

    public static int scaled(int base, double multiplier) {
        return Math.max(0, (int) Math.round(base * multiplier));
    }
}
