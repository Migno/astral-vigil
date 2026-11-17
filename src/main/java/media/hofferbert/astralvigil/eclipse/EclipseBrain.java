package media.hofferbert.astralvigil.eclipse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Pure decision rules of the Eclipse (unit-tested): which attack comes next, how long it pauses between moves, how far
 * damage may push its health in one phase, and a tiny stage timer. No Minecraft classes, so plain JUnit can test it.
 */
public final class EclipseBrain {
    private EclipseBrain() {}

    /**
     * Picks the next attack: allowed in this phase, off cooldown, in range, allowed while shielded, never the same move
     * twice in a row (unless it is the only option). Weighted random; {@code random.applyAsInt(n)} returns 0..n-1.
     *
     * @return the move, or {@link EclipseMove#NONE} if nothing fits right now (the Eclipse repositions and retries)
     */
    public static EclipseMove pick(int phase, boolean shielded, double distance, int[] cooldowns, EclipseMove last,
            IntUnaryOperator random) {
        List<EclipseMove> options = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (EclipseMove m : EclipseMove.values()) {
            if (m.kind() != EclipseMove.Kind.ATTACK || !m.allowedIn(phase) || cooldowns[m.ordinal()] > 0
                    || distance > m.maxRange() || shielded && !m.allowedWhileShielded()) {
                continue;
            }
            int w = m.weight();
            if (m == EclipseMove.SWEEP && distance <= 5) {
                w *= 2; // in its face: claws first
            }
            if (m == EclipseMove.BLINK && distance > 10) {
                w *= 2; // kiting: close the gap
            }
            options.add(m);
            weights.add(w);
        }
        if (options.size() > 1) {
            int i = options.indexOf(last);
            if (i >= 0) {
                options.remove(i);
                weights.remove(i);
            }
        }
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return EclipseMove.NONE;
        }
        int roll = random.applyAsInt(total);
        for (int i = 0; i < options.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }

    /** Ticks of breathing room between two moves: shorter in phase 3 and when enraged ({@code tempo} &lt; 1). */
    public static int gap(int base, int phase, boolean enraged, double tempo) {
        double g = base * (phase >= 3 ? 0.8 : 1.0) * (enraged ? tempo : 1.0);
        return Math.max(6, (int) Math.round(g));
    }

    /** Cooldown after a move, scaled like {@link #gap}. */
    public static int cooldown(EclipseMove move, int phase, boolean enraged, double tempo) {
        return (int) Math.round(move.cooldown() * (phase >= 3 ? 0.85 : 1.0) * (enraged ? tempo : 1.0));
    }

    /**
     * Lowest health damage may leave in {@code phase}: just past the next phase boundary, so a burst can never skip a
     * phase (excess damage is lost). No floor in the last phase.
     */
    public static float healthFloor(int phase, float max) {
        return switch (phase) {
            case 1 -> max * 2f / 3f - 0.5f;
            case 2 -> max / 3f - 0.5f;
            default -> 0f;
        };
    }

    /** Whether the fight has run idle long enough to reset. */
    public static boolean shouldReset(int ticksWithoutPlayers, int resetTicks) {
        return resetTicks > 0 && ticksWithoutPlayers >= resetTicks;
    }

    /** Stage timer for one running move. */
    public static final class Timer {
        private EclipseMove move = EclipseMove.NONE;
        private EclipseMove.Stage stage = EclipseMove.Stage.NONE;
        private int left;
        private int elapsed;

        public void start(EclipseMove m) {
            move = m;
            enter(EclipseMove.Stage.WINDUP);
        }

        public void stop() {
            move = EclipseMove.NONE;
            stage = EclipseMove.Stage.NONE;
            left = 0;
            elapsed = 0;
        }

        private void enter(EclipseMove.Stage s) {
            while (s != EclipseMove.Stage.NONE && move.ticks(s) <= 0) {
                s = s.next();
            }
            stage = s;
            elapsed = 0;
            left = move.ticks(s);
            if (s == EclipseMove.Stage.NONE) {
                move = EclipseMove.NONE;
            }
        }

        /** Advances one tick. @return true if the stage changed (possibly to NONE, i.e. the move ended) */
        public boolean tick() {
            if (stage == EclipseMove.Stage.NONE) {
                return false;
            }
            elapsed++;
            if (--left <= 0) {
                enter(stage.next());
                return true;
            }
            return false;
        }

        public EclipseMove move() {
            return move;
        }

        public EclipseMove.Stage stage() {
            return stage;
        }

        /** Ticks spent in the current stage. */
        public int elapsed() {
            return elapsed;
        }

        /** Ticks left in the current stage. */
        public int left() {
            return left;
        }

        public boolean running() {
            return stage != EclipseMove.Stage.NONE;
        }
    }
}
