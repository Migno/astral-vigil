package media.hofferbert.astralvigil.eclipse;

import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.eclipse.EclipseMove.Stage;

/**
 * The Eclipse's summoned arsenal (pure, unit-tested). Every weapon is a hidden bone of the Eclipse model that
 * materialises {@code appearTick} ticks into {@code appearStage} of its move (a spark flares, then the weapon grows
 * 0 → 1.15 → 1) and dissolves {@code vanishTick} ticks into {@code vanishStage}. The client plays a burst and a sound at
 * those moments at the weapon's approximate position.
 *
 * <p>Timings must match {@code WEAPON_TIMING} in {@code tools/dev/gen_eclipse.py} – {@code EclipseBrainTest} checks the
 * generated animations. Anchors are in model units (left, up, forward) before the renderer's scale.
 */
public enum EclipseWeapon {
    /** Umbral Sweep: a great scythe whose blade rakes the arc in front. */
    SCYTHE(EclipseMove.SWEEP, "weapon_scythe", Stage.WINDUP, 2, Stage.RECOVERY, 6, false, 0.75f,
            new float[] {-13, 32, 6}, new float[] {6, 38, 22}),
    /** Void Lance: a three-pronged spear, hurled as it fires (it becomes the beam). */
    LANCE(EclipseMove.LANCE, "weapon_lance", Stage.WINDUP, 2, Stage.ACTIVE, 1, false, 1.1f,
            new float[] {12, 40, 6}, new float[] {10, 48, 26}),
    /** Falling Stars: a scepter cradling a captured star. */
    SCEPTER(EclipseMove.STARFALL, "weapon_scepter", Stage.WINDUP, 1, Stage.RECOVERY, 6, false, 1.4f,
            new float[] {-13, 36, 6}, new float[] {-12, 34, 20}),
    /** Echo summons: a caged orb of void glass that shatters into the echoes. */
    ORB(EclipseMove.SUMMON, "weapon_orb", Stage.WINDUP, 2, Stage.RECOVERY, 4, false, 1.2f,
            new float[] {11, 30, 14}, new float[] {12, 60, 12}),
    /** Shadow Dive: twin claws spring from both hands. */
    CLAWS(EclipseMove.DIVE, "weapon_claws_right", Stage.WINDUP, 4, Stage.RECOVERY, 12, true, 0.9f,
            new float[] {13, 30, 10}, new float[] {14, 22, 18}),
    /** Eclipse Ray: the Focus disc forms before the chest and is turned on the floor. */
    RAY_FOCUS(EclipseMove.BEAM, "weapon_focus", Stage.WINDUP, 3, Stage.RECOVERY, 10, false, 0.6f,
            new float[] {0, 46, 16}, new float[] {0, 30, 20}),
    /** Umbral Nova: the Focus disc gathers the nova and breaks with it. */
    NOVA_FOCUS(EclipseMove.NOVA, "weapon_focus", Stage.WINDUP, 4, Stage.RECOVERY, 2, false, 0.5f,
            new float[] {0, 46, 16}, new float[] {0, 54, 18}),
    /** Umbral Grasp: a chain that lashes out and drags the target back. */
    CHAIN(EclipseMove.GRASP, "weapon_chain", Stage.WINDUP, 3, Stage.RECOVERY, 6, false, 0.8f,
            new float[] {-13, 34, 6}, new float[] {-8, 40, 16});

    private final EclipseMove move;
    private final String bone;
    private final Stage appearStage;
    private final int appearTick;
    private final Stage vanishStage;
    private final int vanishTick;
    private final boolean twin;
    private final float pitch;
    private final float[] appearAt;
    private final float[] vanishAt;

    EclipseWeapon(EclipseMove move, String bone, Stage appearStage, int appearTick, Stage vanishStage, int vanishTick,
            boolean twin, float pitch, float[] appearAt, float[] vanishAt) {
        this.move = move;
        this.bone = bone;
        this.appearStage = appearStage;
        this.appearTick = appearTick;
        this.vanishStage = vanishStage;
        this.vanishTick = vanishTick;
        this.twin = twin;
        this.pitch = pitch;
        this.appearAt = appearAt;
        this.vanishAt = vanishAt;
    }

    public EclipseMove move() {
        return move;
    }

    /** The (first) model bone; twin weapons have a mirrored {@code _left} partner. */
    public String bone() {
        return bone;
    }

    public Stage appearStage() {
        return appearStage;
    }

    public int appearTick() {
        return appearTick;
    }

    public Stage vanishStage() {
        return vanishStage;
    }

    public int vanishTick() {
        return vanishTick;
    }

    /** Held in both hands (the anchor is mirrored for the second one). */
    public boolean twin() {
        return twin;
    }

    /** Pitch of the materialise / dissolve sounds (each weapon sounds a little different). */
    public float pitch() {
        return pitch;
    }

    /** Model-space anchor (left, up, forward) where the weapon appears or vanishes. */
    public float[] anchor(boolean appear) {
        return (appear ? appearAt : vanishAt).clone();
    }

    /** Weapons whose materialise ({@code appear}) or dissolve moment falls on this tick of the move. */
    public static List<EclipseWeapon> at(EclipseMove move, Stage stage, int tick, boolean appear) {
        List<EclipseWeapon> out = new ArrayList<>(1);
        for (EclipseWeapon w : values()) {
            if (w.move == move && (appear ? w.appearStage == stage && w.appearTick == tick : w.vanishStage == stage && w.vanishTick == tick)) {
                out.add(w);
            }
        }
        return out;
    }
}
