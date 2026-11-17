package media.hofferbert.astralvigil.eclipse;

/**
 * Every move of the Eclipse (pure, unit-tested). An attack runs wind-up → active → recovery; specials (entrance, phase
 * transitions, shield, enrage, stagger, grounded) only have an active stage. Only one move runs at a time.
 *
 * <p>Tick values must match {@code tools/dev/gen_eclipse.py} (animation lengths) – {@code EclipseMoveTest} checks it.
 */
public enum EclipseMove {
    NONE("idle", Kind.NONE, 0, 0, 0, 0, 1, 3, 0, 0, true),
    /** Umbral Sweep: swoops down beside the target and rakes a 130° arc in front. */
    SWEEP("sweep", Kind.ATTACK, 18, 6, 16, 50, 1, 3, 5, 9.0, false),
    /** Void Lance: an aimed beam, locked 8 ticks before it fires – step sideways. */
    LANCE("lance", Kind.ATTACK, 26, 4, 14, 70, 1, 3, 4, 40.0, true),
    /** Falling Stars: marked circles on the ground that impact 1.5 s later. */
    STARFALL("starfall", Kind.ATTACK, 16, 10, 14, 120, 1, 3, 3, 40.0, true),
    /** Echo summons. */
    SUMMON("summon", Kind.ATTACK, 20, 4, 16, 300, 2, 3, 2, 40.0, true),
    /** Shadow Dive: rises, marks a spot, slams down; a shockwave rolls outward – jump it. */
    DIVE("dive", Kind.ATTACK, 30, 8, 24, 160, 2, 3, 3, 32.0, false),
    /** Eclipse Ray: a ground-level ray sweeps 360° around the arena centre – jump it or hide behind cover. */
    BEAM("beam", Kind.ATTACK, 30, 100, 30, 420, 3, 3, 3, 40.0, false),
    /** Umbral Nova: after a long wind-up, hits everyone with line of sight – hide behind cover. */
    NOVA("nova", Kind.ATTACK, 50, 4, 30, 360, 3, 3, 2, 40.0, false),
    /** Blink: vanishes and reappears behind the target. */
    BLINK("blink", Kind.ATTACK, 8, 2, 8, 90, 3, 3, 3, 40.0, false),
    /** Umbral Grasp (anti-cheese only): drags a hiding or unreachable target back into the arena. */
    GRASP("grasp", Kind.FORCED, 24, 6, 16, 200, 1, 3, 0, 96.0, true),
    ENTRANCE("entrance", Kind.SPECIAL, 0, 70, 0, 0, 1, 3, 0, 0, true),
    TRANSITION_2("phase2", Kind.SPECIAL, 0, 60, 0, 0, 1, 3, 0, 0, true),
    TRANSITION_3("phase3", Kind.SPECIAL, 0, 60, 0, 0, 1, 3, 0, 0, true),
    SHIELD("shield_raise", Kind.SPECIAL, 0, 30, 0, 0, 1, 3, 0, 0, true),
    ENRAGE("enrage", Kind.SPECIAL, 0, 40, 0, 0, 1, 3, 0, 0, true),
    /** Shield broken: helpless and takes extra damage. */
    STAGGER("stagger", Kind.SPECIAL, 0, 80, 0, 0, 1, 3, 0, 0, true),
    /** Exhausted on the floor after a heavy move: the melee window. */
    GROUNDED("grounded", Kind.SPECIAL, 0, 60, 0, 0, 1, 3, 0, 0, true);

    public enum Kind { NONE, ATTACK, FORCED, SPECIAL }

    public enum Stage {
        NONE, WINDUP, ACTIVE, RECOVERY;

        public Stage next() {
            return switch (this) {
                case WINDUP -> ACTIVE;
                case ACTIVE -> RECOVERY;
                default -> NONE;
            };
        }
    }

    private final String animation;
    private final Kind kind;
    private final int windup;
    private final int active;
    private final int recovery;
    private final int cooldown;
    private final int minPhase;
    private final int maxPhase;
    private final int weight;
    private final double maxRange;
    private final boolean allowedWhileShielded;

    EclipseMove(String animation, Kind kind, int windup, int active, int recovery, int cooldown, int minPhase, int maxPhase,
            int weight, double maxRange, boolean allowedWhileShielded) {
        this.animation = animation;
        this.kind = kind;
        this.windup = windup;
        this.active = active;
        this.recovery = recovery;
        this.cooldown = cooldown;
        this.minPhase = minPhase;
        this.maxPhase = maxPhase;
        this.weight = weight;
        this.maxRange = maxRange;
        this.allowedWhileShielded = allowedWhileShielded;
    }

    public Kind kind() {
        return kind;
    }

    public int ticks(Stage stage) {
        return switch (stage) {
            case WINDUP -> windup;
            case ACTIVE -> active;
            case RECOVERY -> recovery;
            case NONE -> 0;
        };
    }

    public int totalTicks() {
        return windup + active + recovery;
    }

    public int cooldown() {
        return cooldown;
    }

    public int weight() {
        return weight;
    }

    public double maxRange() {
        return maxRange;
    }

    public boolean allowedIn(int phase) {
        return phase >= minPhase && phase <= maxPhase;
    }

    public boolean allowedWhileShielded() {
        return allowedWhileShielded;
    }

    /** The GeckoLib animation for a stage: {@code <name>_windup|_strike|_recover} for attacks, the plain name otherwise. */
    public String animation(Stage stage) {
        if (kind != Kind.ATTACK && kind != Kind.FORCED) {
            return animation;
        }
        return animation + switch (stage) {
            case WINDUP -> "_windup";
            case ACTIVE -> "_strike";
            case RECOVERY -> "_recover";
            case NONE -> "";
        };
    }

    /** Animations that loop instead of holding their last frame. */
    public static boolean loops(String animation) {
        return animation.startsWith("idle") || animation.equals("beam_strike") || animation.equals("grounded");
    }

    public static EclipseMove byId(int id) {
        EclipseMove[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }
}
