package media.hofferbert.astralvigil.eclipse;

/** Pure Eclipse phase formula (unit-tested). */
public final class EclipsePhase {
    private EclipsePhase() {}

    /** 1, 2 or 3 – from the health fraction (above 2/3, above 1/3, rest). */
    public static int of(float health, float max) {
        float f = max <= 0 ? 0 : health / max;
        return f > 2f / 3f ? 1 : f > 1f / 3f ? 2 : 3;
    }
}
